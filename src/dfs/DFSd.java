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
 * TODO: 
 * -DEAL WITH SYNCHRONIZATION ISSUES ON SETS AND MAP
 * -UPDATE EVERYTHING THAT CALLS WRITEBLOCK and WRITEINODE TO CHECK FOR ERRORS (RETURN -1)
 * 
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
		for (int i=0;i<Constants.BLOCK_SIZE;i++){
			empty_buf[i]=0;
		}
	}
	protected DFSd(boolean format) {
		super(format);
		for (int i=0;i<Constants.BLOCK_SIZE;i++){
			empty_buf[i]=0;
		}
	}
	protected DFSd() {
		super();
		for (int i=0;i<Constants.BLOCK_SIZE;i++){
			empty_buf[i]=0;
		}
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
				if ((ib.get(i*Constants.INTS_PER_INODE))>0){ //used inode
					
					//test	
					System.out.println("At block " + BID + " inode " + i);
					
					
					
					//remove file id from free list
					int FID=getFID(BID,i);
					free_inodes.remove(FID);

					//get the block map, put it in our map
					int[] iblocks=new int[Constants.INTS_PER_INODE];
					
					//test
					System.out.println("at this get block, offset is " + (i*Constants.INTS_PER_INODE));
					
					ib.position((i*Constants.INTS_PER_INODE));
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
							//TODO: CHECK TMP VALUE>0
							fileBlocks+=tmp;
							if (tmp<Constants.INTS_PER_BLOCK){
								//file ended at a sub-block
								break;
							}
						}
						else if (j==(Constants.INTS_PER_INODE-1)){
							//doubly
							int tmp=removeUsedBlock(iblocks[j],(expectedBlocks-fileBlocks), 2);
							//TODO: CHECK TMP VALUE>0
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
						ib.position(i*Constants.INTS_PER_INODE);
						ib.put(iblocks,0, Constants.INTS_PER_INODE); //make sure consistent inode
					}
				} // end used inodes
			} // end inode loop
			//update disk data of inode
			ByteBuffer bb=ByteBuffer.allocate(Constants.BLOCK_SIZE);
			ib.reset(); //put position of buffer back at zero
			bb.asIntBuffer().put(ib);
			writeBlock(BID,bb.array(),0,Constants.BLOCK_SIZE);
		} // end block loop
	}
	
	/**
	 * Recursively mark data blocks in from inode as used, for use in building metadata during DFS.init
	 * @param limit maximum number of blocks to mark as used (in case file size<blocks in block map)
	 * @return the number of blocks marked as used and removed from free_data_blocks. -1 if block ID is invalid or can't remove any more blocks from free_data_blocks.
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

		int[] blocks=new int[Constants.INTS_PER_INODE];
		
		fs.put(did, blocks);

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

	/**
	 * Recursively add data blocks from inode to free list, and zeroes them out. 
	 * For use in public destroyDFile
	 * Inodes may list invalid blocks as part of blockmap, code takes that into account by returning -1
	 * 
	 * @return the number of blocks freed
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

//		DFileID key = new DFileID(dFID.getDFileID());//'0' for initialization
		
//		int[] iblocks=fs.get((DFileID)key);
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
				//TODO: CHECK TMP VALUE>0
				fileBlocks+=tmp;
			}
			else if (i==(Constants.INTS_PER_INODE-1)){
				//doubly indirect
				int tmp=destroyDFile(iblocks[i],(expectedBlocks-fileBlocks),2);
				//TODO: CHECK TMP VALUE>0
				fileBlocks+=tmp;
			}
			else {
				//direct
				int tmp=destroyDFile(iblocks[i],(expectedBlocks-fileBlocks),0);

				if(tmp<0){
					break;
				}
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
	
	/**
	 * Recursively read block data
	 * @return number of bytes read
	 */
	private int readBlocks(int BID, byte[] buffer, int offset, int count, int depth){
		if (BID<Constants.DATA_REGION || BID>=Constants.NUM_OF_BLOCKS){ //invalid blocks, truncate file
			return -1;
		}
		if (count==0){
			return 0;
		}
		if (depth==0){
			//direct
			int tmp=readBlock(BID,buffer,offset,count);
			return tmp;
		}
		else if (depth==1){
			//singly
			IntBuffer ib=getBlockAsInts(BID);
			int subCt=0;
			int tmp=0;
			for (int i=0;i<Constants.INTS_PER_BLOCK;i++){
				tmp=readBlocks(ib.get(i),buffer,offset,count,0);
				if (tmp<1){
					return subCt;
				}
				count-=tmp;
				offset+=tmp;
				subCt+=tmp;
			}
			return subCt;
		}
		else if (depth==2){
			//doubly
			IntBuffer ib=getBlockAsInts(BID);
			int subCt=0;
			int tmp=0;
			for (int i=0;i<Constants.INTS_PER_BLOCK;i++){
				tmp=readBlocks(ib.get(i),buffer,offset,count,1);
				if (tmp<1){
					return subCt;
				}
				count-=tmp;
				offset+=tmp;
				subCt+=tmp;
			}
			return subCt;
		}
		return 0; //should be impossible unless inodes changed
	}
	
	//assumes metadata is all valid
	@Override
	public int read(DFileID dFID, byte[] buffer, int startOffset, int count) {
		if (dFID.getDFileID()<1 || dFID.getDFileID()>=Constants.DATA_REGION){return -1;} //invalid dFID

		int countCpy=count;
		
		synchronized(dFID){
		int[] iblocks=fs.get(dFID);
		if (iblocks==null){return -1;} //no such fileID in use
		
		int iSize=iblocks[0];
		
		if (count>iSize){count=iSize;}
		
		for (int i=1;i<Constants.INTS_PER_INODE;i++){
			if (count<=0){
				break;
			}
			//DBuffer.read takes into account counts larger than Constants.BLOCK_SIZE
			if (i==(Constants.INTS_PER_INODE-2)){
				//singly indirect
				int tmp=readBlocks(iblocks[i],buffer,startOffset,count,1);
				if (tmp<1){
					break;
				}
				count-=tmp;
				startOffset+=tmp;
			}
			else if (i==(Constants.INTS_PER_INODE-1)){
				//doubly indirect
				int tmp=readBlocks(iblocks[i],buffer,startOffset,count,2);
				if (tmp<1){
					break;
				}
				count-=tmp;
				startOffset+=tmp;
			}
			else {
				//direct
				int tmp=readBlocks(iblocks[i],buffer,startOffset,count,0);
				if (tmp<1){
					break; //stop reading
				}
				count-=tmp;	//decrease number of bytes we still have to read
				startOffset+=tmp; //advance buffer
				
			}
		}
		}
		return countCpy-count;
	}
	
	/**
	 * Each data block in a file is indexed by 1-number of blocks used.
	 * @param bix block index
	 * @return path to reach that data block [inode level, indirect level, doubly indirect level]
	 */
	private int[] calculateIndices(int bix){
		int[] index=new int[3];
		if (bix<(Constants.INTS_PER_INODE-2)){ //direct
			index[0]=bix;
			index[1]=-1;
			index[2]=-1;
		}
		else if (bix<(Constants.INTS_PER_INODE-2+Constants.INTS_PER_BLOCK)){ //singly
			index[0]=(Constants.INTS_PER_INODE-2);
			bix-=(Constants.INTS_PER_INODE-2);
			index[1]=bix%Constants.INTS_PER_BLOCK;
			index[2]=-1;
		}
		else { //doubly
			index[0]=(Constants.INTS_PER_INODE-1);
			bix-=(Constants.INTS_PER_INODE-2);
			index[1]=(bix/Constants.INTS_PER_BLOCK)-1;
			index[2]=bix%Constants.INTS_PER_BLOCK;
		}
		return index;
	}
	
	private boolean putNewDirectBlockPtr(DFileID dFID, int[] iblocks, int ix){
		//iblocks contains entire inode data
		Integer data=free_data_blocks.ceiling(Constants.DATA_REGION);
		free_data_blocks.remove(data);
		iblocks[ix]=data;

		IntBuffer ib=IntBuffer.wrap(iblocks);
		ByteBuffer bb=ByteBuffer.allocate(Constants.INODE_SIZE);
		bb.asIntBuffer().put(ib); 
		int suc=writeInode(dFID.getDFileID(),bb.array()); //rewrite inode
		if (suc<1){return false;}
		fs.put(dFID, iblocks);
		return true;
	}
	
	private boolean putNewIndirectBlockPtr(int blockID, int ix){
		Integer data=free_data_blocks.ceiling(Constants.DATA_REGION);
		free_data_blocks.remove(data);
		IntBuffer ib=getBlockAsInts(blockID);
		ib.put(ix, data);

		ByteBuffer bb=ByteBuffer.allocate(Constants.INODE_SIZE);
		bb.asIntBuffer().put(ib); 
		int suc=writeBlock(blockID, bb.array(), 0, Constants.BLOCK_SIZE);
		if (suc<1){return false;}
		return true;
	}
	/**
	 * Recursively write block data
	 * @return number of bytes read
	 */
	private int writeBlocks(int BID, byte[] buffer, int offset, int count, int depth){
		if (BID<Constants.DATA_REGION || BID>=Constants.NUM_OF_BLOCKS){ //invalid blocks, truncate file
			return -1;
		}
		if (count==0){
			return 0;
		}
		if (depth==0){
			//direct
			int tmp=writeBlock(BID,buffer,offset,count);
			return tmp;
		}
		else if (depth==1){
			//singly
			IntBuffer ib=getBlockAsInts(BID);
			int subCt=0;
			int tmp=0;
			for (int i=0;i<Constants.INTS_PER_BLOCK;i++){
				tmp=writeBlocks(ib.get(i),buffer,offset,count,0);
				if (tmp<1){
					return subCt;
				}
				count-=tmp;
				offset+=tmp;
				subCt+=tmp;
			}
			return subCt;
		}
		else if (depth==2){
			//doubly
			IntBuffer ib=getBlockAsInts(BID);
			int subCt=0;
			int tmp=0;
			for (int i=0;i<Constants.INTS_PER_BLOCK;i++){
				tmp=writeBlocks(ib.get(i),buffer,offset,count,1);
				if (tmp<1){
					return subCt;
				}
				count-=tmp;
				offset+=tmp;
				subCt+=tmp;
			}
			return subCt;
		}
		return 0; //should be impossible unless inodes changed
	}
	
	
	@Override
	public int write(DFileID dFID, byte[] buffer, int startOffset, int count) {
		if (dFID.getDFileID()<1 || dFID.getDFileID()>=Constants.DATA_REGION){return -1;} //invalid dFID

		//TODO: PROPER SYNCHRONIZATION so fs.get cannot give back for same dfileid

		int[] iblocks=fs.get(dFID); 
		if (iblocks==null){return -1;} //no such fileID in use

		if (count>Constants.MAX_FILE_SIZE){ //upper limit to file
			count=Constants.MAX_FILE_SIZE;
		}

		if (count>iblocks[0]){ //may need to increase size of file and change inode

			//calculate the number of blocks we will need to add to file
			int oldSize=iblocks[0];
			int remainingSpace=Constants.BLOCK_SIZE-(oldSize%Constants.BLOCK_SIZE);
			if (remainingSpace==Constants.BLOCK_SIZE){remainingSpace=0;}
			int tmp=count-oldSize;
			int increase=tmp/Constants.BLOCK_SIZE;
			int partial=tmp%Constants.BLOCK_SIZE;
			if (partial>remainingSpace){
				increase+=1;
			}
			int oldBlockSize=getBlockCount(oldSize);

			//TODO: MORE SMARTER SUCCESS CHECK (add up amounts, write partial)

			//add blocks to expand file
			if (increase>0){
				for (int i=0;i<increase;i++){
					oldBlockSize+=1;
					int[] ix=calculateIndices(oldBlockSize);
					boolean suc=true;
					if (ix[1]==-1){ //direct, need to update inode
						suc=putNewDirectBlockPtr(dFID,iblocks,ix[0]);
						if (!suc){return -1;}
					}
					else {
						if (ix[2]==-1){ //singly indirect
							if (ix[1]==0){ //need to update inode by adding branch
								suc=putNewDirectBlockPtr(dFID,iblocks,ix[0]);
								if (!suc){return -1;}
							}
							suc=putNewIndirectBlockPtr(iblocks[ix[0]], ix[1]);
							if (!suc){return -1;}
						}
						else { //doubly indirect
							if (ix[1]==0 && ix[2]==0){ //update inode
								suc=putNewDirectBlockPtr(dFID,iblocks,ix[0]);
								if (!suc){return -1;}
							}
							if (ix[2]==0){ //update first level
								suc=putNewIndirectBlockPtr(iblocks[ix[0]], ix[1]);
								if (!suc){return -1;}
							}
							IntBuffer ib=getBlockAsInts(iblocks[ix[0]]);
							suc=putNewIndirectBlockPtr(ib.get(ix[1]), ix[2]);
							if (!suc){return -1;}
						}
					}
				} //end for loop
				//write inode back to disk
				iblocks=fs.get(dFID);
				iblocks[0]=count;
				IntBuffer ib=IntBuffer.wrap(iblocks);
				ByteBuffer bb=ByteBuffer.allocate(Constants.INODE_SIZE);
				bb.asIntBuffer().put(ib);
				writeInode(dFID.getDFileID(),bb.array());
				fs.put(dFID, iblocks);
			} //end increase
		}
		//write data to file

		int countCpy=count;

		for (int i=1;i<Constants.INTS_PER_INODE;i++){
			if (count<=0){
				break;
			}
			//DBuffer.read takes into account counts larger than Constants.BLOCK_SIZE
			if (i==(Constants.INTS_PER_INODE-2)){
				//singly indirect
				int tmp=writeBlocks(iblocks[i],buffer,startOffset,count,1);
				if (tmp<1){
					break;
				}
				count-=tmp;
				startOffset+=tmp;
			}
			else if (i==(Constants.INTS_PER_INODE-1)){
				//doubly indirect
				int tmp=writeBlocks(iblocks[i],buffer,startOffset,count,2);
				if (tmp<1){
					break;
				}
				count-=tmp;
				startOffset+=tmp;
			}
			else {
				//direct
				int tmp=writeBlocks(iblocks[i],buffer,startOffset,count,0);
				if (tmp<1){
					break; //stop writing
				}
				count-=tmp;	
				startOffset+=tmp; //advance buffer
			}
		}
		return countCpy-count;
	}

	/**
	 * 
	 * @param BID block ID (where the data is stored)
	 * @param buffer destination for bytes
	 * @param startOffset offset for buffer
	 * @param count number of bytes to read into buffer
	 * @return number of bytes read into buffer
	 */
	private int readBlock(Integer BID, byte[] buffer, int startOffset, int count){
		DBuffer d=cache.getBlock(BID);
		
		if (!d.checkValid()){
			d.waitValid(); //wait for fetch to finish
		}
		int bytes=d.read(buffer, startOffset, count);
		cache.releaseBlock(d);
		return bytes;
		
	}
	
	private int writeBlock(Integer BID, byte[] buffer, int startOffset, int count){
		DBuffer d=cache.getBlock(BID);
		
		if (!d.checkClean()){
			d.waitClean(); //wait for fetch to finish
		}
		int bytes=d.write(buffer, startOffset, count);
		cache.releaseBlock(d);
		return bytes;
	}
	
	/**
	 * @param FID file ID
	 * @param buffer information for that inode only
	 */
	private int writeInode(Integer FID, byte[] buffer){
		int BID = getBID(FID);
		int offset = getInodeOffset(FID);
		
		byte[] block=new byte[Constants.BLOCK_SIZE];
		

		int read=readBlock(BID,block,0,Constants.BLOCK_SIZE);
		if (read<0){return -1;}
		
		//rewrite inode into block
		int inode_offset_bytes=offset*Constants.INTS_PER_INODE*Constants.intBytes;
		for (int i=0;i<Constants.INODE_SIZE;i++){
			block[inode_offset_bytes+i]=buffer[i];
		}
		
		int write=writeBlock(BID,block,0,(inode_offset_bytes+Constants.INODE_SIZE));
		return write;
	}

	@Override
	public int sizeDFile(DFileID dFID) {
		int[] prop=fs.get(dFID);
		if (prop==null){return -1;}
		return prop[0];
	}

	
	@Override
	public List<DFileID> listAllDFiles() {
		Set<DFileID> tmp=fs.keySet();

		//test...to del
		for(DFileID id:tmp){
			System.out.print(id + ", ");
		}
		System.out.println();
				
			
		return new ArrayList<DFileID>(tmp);
	}
	
	//wrapping the 
	private synchronized int[] getInodeProp(DFileID dFID){
		//TODO: fs.get, then deep copy that array and return it
		
		return null;
	}
	private synchronized boolean removeInode(DFileID dFID){
		//TODO:
		return false;
	}

	private void zeroBlock(int BID){
		writeBlock(BID,empty_buf,0,Constants.BLOCK_SIZE);
	}
	/**the number of blocks something of size byteNumber would take up
	 * 
	 * @param byteNumber number of bytes (aka file size)
	 * @return number of blocks that number of bytes uses
	 */
	private int getBlockCount(int byteNumber){
		int tmp=byteNumber/Constants.BLOCK_SIZE;
		if ((byteNumber%Constants.BLOCK_SIZE)>0){
			tmp+=1;
		}
		return tmp;
	}
	/**
	 * 
	 * @param BID block ID
	 * @return the data in the block interpreted as ints
	 */
	private IntBuffer getBlockAsInts(int BID){
		byte[] buffer=new byte[Constants.BLOCK_SIZE];
		readBlock(BID,buffer,0,Constants.BLOCK_SIZE);
		
		ByteBuffer bb=ByteBuffer.wrap(buffer);
		IntBuffer ib=bb.asIntBuffer();
		ib.mark(); //mark index 0
		return ib;
	}
	/**
	 * Calculates DFileID number from the inode's block id and offset within the block
	 * @param BID block ID
	 * @param iOffset inode offset
	 * @return file ID
	 */
	private int getFID(int BID, int iOffset){
		return (((BID-1)*Constants.INODES_PER_BLOCK)+iOffset+1);
	}
	/**
	 * @param FID file ID
	 * @return block ID (offset within byte array in terms of blocks, not bytes)
	 */
	private int getBID(int FID){
		return ((FID-1) / Constants.INODES_PER_BLOCK) + 1;
	}
	/**
	 * @param FID file ID
	 * @return inode offset within the block in terms of inodes (not bytes)
	 */
	private int getInodeOffset(int FID){
		return (FID-1) % Constants.INODES_PER_BLOCK;
	}

	@Override
	public synchronized void sync() {
		cache.sync();
	}

}
