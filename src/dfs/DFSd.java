package dfs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import virtualdisk.VirtualDisk;
import virtualdisk.VirtualDiskd;

import common.Constants;
import common.DFileID;
import dblockcache.DBuffer;
import dblockcache.DBufferCache;
import dblockcache.DBufferCached;


/*
 * 
 * BID 0 is left empty
 * FID spans 1-Constants.MAX_DFILES
 * 
 * FID = file ID
 * BID = block ID
 * offset = offset of inode within block
 * 
 * FID = (BID-1) * inode_per_block + offset + 1
 * 
 * BID = ((FID-1) / inode_per_block) + 1
 * offset = (FID-1) % inode_per_block
 * 
 */

public class DFSd extends DFS {
	
	private static DFSd _instance;
	private DBufferCache cache;
	private VirtualDisk disk;
	
	private HashMap<DFileID,int[]> fs;
	private TreeSet<Integer> free_data_blocks; //block ids 
	private TreeSet<Integer> free_inodes; // file ids
	
	private final static byte[] empty_buf=new byte[Constants.BLOCK_SIZE];
	
	protected DFSd(String volName, boolean format) {
		super(volName,format);
	}
	protected DFSd(boolean format) {
		super(format);
	}
	protected DFSd() {
		super();
	}

	public static DFSd instance(String volName, boolean format){
		if (_instance==null){ _instance=new DFSd(volName,format); }
		return _instance;
	}
	public static DFSd instance(boolean format){
		if (_instance==null){ _instance=new DFSd(format); }
		return _instance;
	}
	public static DFSd instance(){
		if (_instance==null){ _instance=new DFSd(); }
		return _instance;
	}

	
	//this method should only be called once within a program. It is not thread-safe.
	@Override
	public void init() {
		System.out.println("Initializing file system...");
		//get Singletons of other layers
		try {
			disk=VirtualDiskd.instance(_volName,_format);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		cache=DBufferCached.instance(Constants.NUM_OF_CACHE_BLOCKS, disk);
		
		//initialize metadata
		fs=new HashMap<DFileID,int[]>();
		free_data_blocks=new TreeSet<Integer>();
		free_inodes=new TreeSet<Integer>();
		for (int i=Constants.DATA_REGION;i<Constants.NUM_OF_BLOCKS;i++){
			free_data_blocks.add(i);
		}
		for (int i=1;i<Constants.MAX_DFILES;i++){
			free_inodes.add(i);
		}
		
		buildMetadata();
		System.out.println("File system initialized.");

	}
	
	private void buildMetadata() {
		//loop through blocks that contain inodes
		for (int BID=1;BID<Constants.DATA_REGION;BID++){
			//test
			System.out.println("entered buildmetadata, BID = " + BID);
			IntBuffer ib=getBlockAsInts(BID);
							
			//loop through inodes
			for (int i=0;i<Constants.INODES_PER_BLOCK;i++){
				
				if (ib.get((i*Constants.INTS_PER_INODE))>0){ //used inode
					
					//test	
					System.out.println("At block " + BID + " inode " + i);
					
					
					
					//remove file id from free list
					int FID=getFID(BID,i);
					free_inodes.remove(FID);

					//get the block map, put it in our map
					int[] iblocks=new int[Constants.INTS_PER_INODE];
					
					//test
					System.out.println("at this get block, offset is " + (i*Constants.INTS_PER_INODE));
					
					ib.get(iblocks, 0, Constants.INTS_PER_INODE); //read out inode into int array
					fs.put(new DFileID(FID), iblocks);

					int fileBlocks=0;
					int expectedBlocks=getBlockCount(iblocks[0]);

					//special case of empty file
					if (iblocks[0]==1 && iblocks[1]==0){ 
						continue;
					}
					
					//loop through blockIDs belonging to inode, stop at invalid blocks or end of recorded file size
					for (int j=1;j<Constants.INTS_PER_INODE;j++){
						if (fileBlocks>=expectedBlocks){
							break;
						}
						
						if (j==(Constants.INTS_PER_INODE-2)){
							//singly
							int tmp=removeUsedBlock(iblocks[j],(expectedBlocks-fileBlocks), 1);
							fileBlocks+=tmp;
							if (tmp<Constants.INTS_PER_BLOCK){
								//file ended at a sub-block
								break;
							}
						}
						else if (j==(Constants.INTS_PER_INODE-1)){
							//doubly
							int tmp=removeUsedBlock(iblocks[j],(expectedBlocks-fileBlocks), 2);
							if (tmp==0){ //should be impossible
								break;
							}
							fileBlocks+=tmp;
						}
						else {
							//singly
							int tmp=removeUsedBlock(iblocks[j], (expectedBlocks-fileBlocks), 0);
							if (tmp==-1){
								break;
							}
							fileBlocks+=tmp;
						}
					} // end block map loop
					
					if (expectedBlocks>fileBlocks && iblocks[1]!=0){
						iblocks[0]=fileBlocks*Constants.BLOCK_SIZE;
						ib.put(iblocks,(i*Constants.INTS_PER_INODE), Constants.INTS_PER_INODE); //make sure consistent inode
					}
				} // end used inodes
				
			} // end inode loop
			//update disk data of inode
			ByteBuffer bb=ByteBuffer.allocate(Constants.BLOCK_SIZE);
			bb.asIntBuffer().put(ib);
			writeBlock(BID,bb.array(),0,Constants.BLOCK_SIZE);
		} // end block loop
	}
	
	/*
	 * Recursively mark data blocks in from inode as used
	 */
	private int removeUsedBlock(int BID, int limit, int depth){
		if (BID<Constants.DATA_REGION || BID>=Constants.NUM_OF_BLOCKS){ //invalid blocks, truncate file
			return -1;
		}
		if (limit==0){
			return -1;
		}
		boolean freed=free_data_blocks.remove(BID);
		if (!freed){ //block already used elsewhere
			return -1;
		}
		if (depth==0){ //direct
			return 1;
		} 
		else if (depth==1){ //singly
			IntBuffer ib=getBlockAsInts(BID);
			int subCt=0;
			for (int i=0;i<Constants.INTS_PER_BLOCK;i++){
				int tmp=removeUsedBlock(ib.get(i), limit, 0);
				if (tmp==-1){
					return subCt;
				}
				limit-=tmp;
				subCt+=tmp;
			}
			return subCt;
		}
		else if (depth==2){ //doubly
			IntBuffer ib=getBlockAsInts(BID);
			int subCt=0;
			for (int i=0;i<Constants.INTS_PER_BLOCK;i++){
				int tmp=removeUsedBlock(ib.get(i), limit, 1);
				limit-=tmp;
				subCt+=tmp;
				if (tmp<Constants.INTS_PER_BLOCK){ //file ends at one of sub-blocks
					return subCt;
				}
			}
			return subCt;
		}
		return 0; //should be impossible
	}
	
	@Override
	public synchronized DFileID createDFile() {
		//get free DFileID
		Integer FID=free_inodes.ceiling(1);
		if (FID==null){ System.err.println("maxed out files"); return null;}
		
		//update DFS data structures
		free_inodes.remove(FID);
		DFileID did=new DFileID(FID);
		int [] arr = new int[Constants.INTS_PER_INODE];
		fs.put(did, arr);
		
		//get new inode from disk
		int BID = getBID(FID);
		int offset = getInodeOffset(FID);
		
		byte[] buffer=new byte[Constants.BLOCK_SIZE];
		readBlock(BID,buffer,0,Constants.BLOCK_SIZE);

		ByteBuffer bb=ByteBuffer.wrap(buffer);
		
		//set inode size=1
		bb.putInt((offset*Constants.INTS_PER_INODE*Constants.intBytes),1);
		
//		buffer=bb.array(); 
		
		writeInode(FID,buffer);
		return did;
	}

	/*
	 * Recursively add data blocks from inode to free list, and zeroes them out
	 * Inodes may list invalid blocks as part of blockmap, code takes that into account
	 * 
	 * returns the number of blocks freed
	 */
	private int destroyDFile(int BID, int limit, int depth){
		if (BID<Constants.DATA_REGION || BID>=Constants.NUM_OF_BLOCKS){
			return -1;
		}
		if (limit==0){
			return -1;
		}
		if (depth==0){ //direct
			zeroBlock(BID);
			free_data_blocks.add(BID);
			return 1;
		}
		else if (depth==1){ //singly
			IntBuffer ib=getBlockAsInts(BID);
			int subCt=0;
			for (int j=0;j<Constants.INTS_PER_BLOCK;j++){
				int tmp=destroyDFile(ib.get(j),limit,0);
				if (tmp<1){
					return subCt;
				}
				limit-=tmp;
				subCt+=tmp;
			}
			zeroBlock(BID);
			free_data_blocks.add(BID);
			return subCt;
		}
		else if (depth==2){ //doubly
			IntBuffer ib=getBlockAsInts(BID);
			int subCt=0;
			for (int j=0;j<Constants.INTS_PER_BLOCK;j++){
				int tmp=destroyDFile(ib.get(j),limit,1);
				limit-=tmp;
				subCt+=tmp;
			}
			zeroBlock(BID);
			free_data_blocks.add(BID);
			return subCt;
		}
		return 0;
	}
	
	@Override
	public synchronized void destroyDFile(DFileID dFID) {
		if (dFID.getDFileID()<1 || dFID.getDFileID()>=Constants.DATA_REGION){return;} //invalid dFID
		int[] iblocks=fs.get(dFID);
		if (iblocks==null){return;} //no such fileID in use
		
		int fileBlocks=0;
		int expectedBlocks=getBlockCount(iblocks[0]);
		
		iblocks[0]=0; //clear out inode size
		
		for (int i=1;i<Constants.INTS_PER_INODE;i++){
			if (fileBlocks>=expectedBlocks){
				break;
			}
			if (i==(Constants.INTS_PER_INODE-2)){
				//singly indirect
				int tmp=destroyDFile(iblocks[i],(expectedBlocks-fileBlocks),1);
				fileBlocks+=tmp;
			}
			else if (i==(Constants.INTS_PER_INODE-1)){
				//doubly indirect
				int tmp=destroyDFile(iblocks[i],(expectedBlocks-fileBlocks),2);
				fileBlocks+=tmp;
			}
			else {
				//direct
				int tmp=destroyDFile(iblocks[i],(expectedBlocks-fileBlocks),0);
				fileBlocks+=tmp;
			}
			iblocks[i]=0; //clear out inode data
		}
		//if number of blocks freed < expected number to be freed, there is a memory leak somewhere
		//but this will be fixed when the program quits/re-initializes.
		
		//write inode back to memory
		IntBuffer ib=IntBuffer.wrap(iblocks);
		ByteBuffer bb=ByteBuffer.allocate(Constants.INODE_SIZE);
		bb.asIntBuffer().put(ib);
		writeInode(dFID.getDFileID(),bb.array()); //zero out the inode in memory
		fs.remove(dFID);
		free_inodes.add(dFID.getDFileID());
	}
	
	@Override
	public int read(DFileID dFID, byte[] buffer, int startOffset, int count) {
		if (dFID.getDFileID()<1 || dFID.getDFileID()>=Constants.DATA_REGION){return -1;} //invalid dFID
		int[] iblocks=fs.get(dFID);
		if (iblocks==null){return -1;} //no such fileID in use
		
		int iSize=iblocks[0];
		int expectedBlocks=getBlockCount(iSize);
		
		
		//TODO: LOOP THRU BLOCKS BELONGING TO FILE, READ THEM OUT.
		//q: IS THERE A WAY TO DETERMINE ACTUAL SIZE OF FILE FROM INTS RETURNED?
		return 0;
	}

	@Override
	public int write(DFileID dFID, byte[] buffer, int startOffset, int count) {
		if (dFID.getDFileID()<1 || dFID.getDFileID()>=Constants.DATA_REGION){return -1;} //invalid dFID
		int[] iblocks=fs.get(dFID);
		if (iblocks==null){return -1;} //no such fileID in use
		
		if (count>iblocks[0]){ //need to increase size of file and change inode
			int oldSize=iblocks[0];
			int remainingSpace=Constants.BLOCK_SIZE-(oldSize%Constants.BLOCK_SIZE);
			int tmp=count-oldSize;
			int increase=tmp/Constants.BLOCK_SIZE;
			int partial=tmp%Constants.BLOCK_SIZE;
			if (partial>remainingSpace){
				increase+=1;
			}
			
			//TODO: ADD BLOCKS TO INODE IF INCREASE>0
			
			iblocks[0]=count;
			//TODO: WRITE BACK INODE TO DISK
		}
		//TODO
		return 0;
	}

	private int readBlock(Integer BID, byte[] buffer, int startOffset, int count){
		DBuffer d=cache.getBlock(BID);
		
		if (!d.checkValid()){
			d.waitValid(); //wait for fetch to finish
		}
		int bytes=d.read(buffer, startOffset, count);
		cache.releaseBlock(d);
		return bytes;
		
	}
	
	private void writeBlock(Integer BID, byte[] buffer, int startOffset, int count){
		DBuffer d=cache.getBlock(BID);
		
		if (!d.checkClean()){
			d.waitClean(); //wait for fetch to finish
		}
		d.write(buffer, startOffset, count);
		cache.releaseBlock(d);
	}
	
	private void writeInode(Integer FID, byte[] buffer){
		int BID = getBID(FID);
		int offset = getInodeOffset(FID);
		
		writeBlock(BID,buffer,0,((offset+1)*Constants.INODE_SIZE));
	}

	@Override
	public int sizeDFile(DFileID dFID) {
		return fs.get(dFID)[0];
	}

	private void zeroBlock(int BID){
		writeBlock(BID,empty_buf,0,Constants.BLOCK_SIZE);
	}
	
	@Override
	public List<DFileID> listAllDFiles() {
		Set<DFileID> tmp=fs.keySet();
		
		//test...to del
		for(DFileID id:tmp){
			System.out.print(id.getDFileID() + ", ");
		}
		System.out.println();
		
			
		return new ArrayList<DFileID>(tmp);
	}

	//the number of blocks something of size byteNumber would take up
	private int getBlockCount(int byteNumber){
		int tmp=byteNumber/Constants.BLOCK_SIZE;
		if ((byteNumber%Constants.BLOCK_SIZE)>0){
			tmp+=1;
		}
		return tmp;
	}
	private IntBuffer getBlockAsInts(int BID){
		byte[] buffer=new byte[Constants.BLOCK_SIZE];
		readBlock(BID,buffer,0,Constants.BLOCK_SIZE);
		
		ByteBuffer bb=ByteBuffer.wrap(buffer);
		IntBuffer ib=bb.asIntBuffer();
		return ib;
	}
	private int getFID(int BID, int iOffset){
		return (((BID-1)*Constants.INODES_PER_BLOCK)+iOffset+1);
	}
	private int getBID(int FID){
		return ((FID-1) / Constants.INODES_PER_BLOCK) + 1;
	}
	//inode offset in block (in terms of inodes, not bytes)
	private int getInodeOffset(int FID){
		return (FID-1) % Constants.INODES_PER_BLOCK;
	}


	@Override
	public synchronized void sync() {
		cache.sync();
	}

}
