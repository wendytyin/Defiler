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
	}

	private void buildMetadata() {
		//loop through blocks that contain inodes
		for (int BID=1;BID<Constants.DATA_REGION;BID++){
			IntBuffer ib=getBlockAsInts(BID);

			//loop through inodes
			for (int i=0;i<Constants.INODES_PER_BLOCK;i++){
				if (ib.get((i*Constants.INTS_PER_BLOCK))>0){ //used inode

					//remove file id from free list
					int FID=getFID(BID,i);
					free_inodes.remove(FID);

					//get the block map, put it in our map
					int[] iblocks=new int[Constants.INTS_PER_BLOCK];
					ib.get(iblocks, (i*Constants.INTS_PER_BLOCK), Constants.INTS_PER_BLOCK); //read out inode into int array
					fs.put(new DFileID(FID), iblocks);

					int fileBlocks=0;
					boolean valid=true;

					//special case of empty file
					if (iblocks[0]==1 && iblocks[1]==0){ 
						continue;
					}
					
					//TODO: MAKE THESE LOOPS PRETTIER AND LESS REPETITIVE
					
					//loop through blockIDs belonging to inode
					for (int j=1;j<Constants.INTS_PER_INODE;j++){
						if (valid==false) { break; } //reached end of dFile
						if (iblocks[j]<Constants.DATA_REGION || iblocks[j]>=Constants.NUM_OF_BLOCKS){ //invalid blocks, truncate file
							valid=false;
							break;
						}

						boolean freed=free_data_blocks.remove(iblocks[j]);

						if (!freed){ //block already used elsewhere
							valid=false;
							break;
						}

						if (j==(Constants.INTS_PER_INODE-2)){
							//singly indirect block
							IntBuffer ib2=getBlockAsInts(iblocks[j]);
							for (int k=0;k<Constants.INTS_PER_BLOCK;k++){
								if (valid==false){break;}
								int b=ib2.get(k);
								if (b<Constants.DATA_REGION || b>=Constants.NUM_OF_BLOCKS){
									valid=false;
									break;
								}
								freed=free_data_blocks.remove(ib2.get(k));
								if (!freed){
									valid=false;
									break;
								}
								fileBlocks++;
							}
						}
						else if (j==(Constants.INTS_PER_INODE-1)){
							//doubly indirect block
							IntBuffer ib2=getBlockAsInts(iblocks[j]);
							for (int k=0;k<Constants.INTS_PER_BLOCK;k++){
								if (valid==false){break;}
								int b=ib2.get(k);
								if (b<Constants.DATA_REGION || b>=Constants.NUM_OF_BLOCKS){
									valid=false;
									break;
								}
								freed=free_data_blocks.remove(ib2.get(k));
								if (!freed){
									valid=false;
									break;
								}
								IntBuffer ib3=getBlockAsInts(ib2.get(k));
								for (int l=0;l<Constants.INTS_PER_BLOCK;l++){
									if (valid==false){break;}
									int b2=ib3.get(l);
									if (b2<Constants.DATA_REGION || b2>=Constants.NUM_OF_BLOCKS){
										valid=false;
										break;
									}
									freed=free_data_blocks.remove(ib3.get(l));
									if (!freed){
										valid=false;
										break;
									}
									fileBlocks++;
								}
							}
						}
						else {
							//direct block
							fileBlocks++;
						}
					} // end block map loop
					int expectedBlocks=((iblocks[0]-1)/Constants.BLOCK_SIZE)+1;
					if (valid==false || expectedBlocks>fileBlocks){
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
	public synchronized DFileID createDFile() {
		//get free DFileID
		Integer FID=free_inodes.ceiling(1);
		if (FID==null){ System.err.println("maxed out files"); return null;}
		
		//update DFS data structures
		free_inodes.remove(FID);
		DFileID did=new DFileID(FID);
		fs.put(did, null);
		
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

	@Override
	public synchronized void destroyDFile(DFileID dFID) {
		if (dFID.getDFileID()<1 || dFID.getDFileID()>=Constants.DATA_REGION){return;} //invalid dFID
		int[] iblocks=fs.get(dFID);
		if (iblocks==null){return;} //no such fileID in use
		iblocks[0]=0;
		
		for (int i=1;i<Constants.INTS_PER_INODE;i++){
			if (iblocks[i]<Constants.DATA_REGION || iblocks[i]>=Constants.NUM_OF_BLOCKS){continue;}
			
			if (i==(Constants.INTS_PER_INODE-2)){
				//singly indirect
				IntBuffer ib2=getBlockAsInts(iblocks[i]);
				for (int j=0;j<Constants.INTS_PER_BLOCK;j++){
					int b=ib2.get(j);
					if (b<Constants.DATA_REGION || b>=Constants.NUM_OF_BLOCKS){continue;}
					free_data_blocks.add(b);
				}
			}
			else if (i==(Constants.INTS_PER_INODE-1)){
				//doubly indirect
				IntBuffer ib2=getBlockAsInts(iblocks[i]);
				for (int j=0;j<Constants.INTS_PER_BLOCK;j++){
					IntBuffer ib3=getBlockAsInts(ib2.get(j));
					for (int k=0;k<Constants.INTS_PER_BLOCK;k++){
						int b=ib3.get(k);
						if (b<Constants.DATA_REGION || b>=Constants.NUM_OF_BLOCKS){continue;}
						free_data_blocks.add(b);
					}
				}
			}
			else {
				//direct
				int b=iblocks[i];
				if (b<Constants.DATA_REGION || b>=Constants.NUM_OF_BLOCKS){continue;}
				free_data_blocks.add(b);
			}
			iblocks[i]=0;
		}
		IntBuffer ib=IntBuffer.wrap(iblocks);
		ByteBuffer bb=ByteBuffer.allocate(Constants.INODE_SIZE);
		bb.asIntBuffer().put(ib);
		writeInode(dFID.getDFileID(),bb.array()); //zero out the inode in memory
		fs.remove(dFID);
		free_inodes.add(dFID.getDFileID());
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
	public int read(DFileID dFID, byte[] buffer, int startOffset, int count) {
		if (dFID.getDFileID()<1 || dFID.getDFileID()>=Constants.DATA_REGION){return -1;} //invalid dFID
		int[] iblocks=fs.get(dFID);
		if (iblocks==null){return -1;} //no such fileID in use
		
		int isize=iblocks[0];
		//TODO
		return 0;
	}

	@Override
	public int write(DFileID dFID, byte[] buffer, int startOffset, int count) {
		if (dFID.getDFileID()<1 || dFID.getDFileID()>=Constants.DATA_REGION){return -1;} //invalid dFID
		int[] iblocks=fs.get(dFID);
		if (iblocks==null){return -1;} //no such fileID in use
		//TODO
		return 0;
	}

	@Override
	public int sizeDFile(DFileID dFID) {
		return fs.get(dFID)[0];
	}

	@Override
	public List<DFileID> listAllDFiles() {
		Set<DFileID> tmp=fs.keySet();
		return new ArrayList<DFileID>(tmp);
	}

	@Override
	public void sync() {
		// TODO Auto-generated method stub
		
	}

}
