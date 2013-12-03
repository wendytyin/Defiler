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
	
	private HashMap<DFileID,DBlockList> fs;
	private TreeSet<Integer> free_data_blocks; //block ids 
	private TreeSet<Integer> free_inodes; // file ids
	
	
	private static final int intBytes=4;
	
	private static final int inode_per_block=Constants.BLOCK_SIZE/Constants.INODE_SIZE;
	private static final int data_region=((Constants.MAX_DFILES-1)/inode_per_block)+2;
	private static final int ints_per_inode=Constants.INODE_SIZE/intBytes;
	
	
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


	@Override
	public void init() {
		try {
			disk=VirtualDiskd.instance(_volName,_format);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		cache=DBufferCached.instance(Constants.NUM_OF_CACHE_BLOCKS, disk);
		
		fs=new HashMap<DFileID,DBlockList>();
		free_data_blocks=new TreeSet<Integer>();
		free_inodes=new TreeSet<Integer>();
		for (int i=data_region;i<Constants.NUM_OF_BLOCKS;i++){
			free_data_blocks.add(i);
		}
		for (int i=1;i<Constants.MAX_DFILES;i++){
			free_inodes.add(i);
		}
		
		
		
		//TODO
		
		
		
		
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
		byte[] buffer=new byte[Constants.BLOCK_SIZE];
		readInode(FID,buffer);

		ByteBuffer bb=ByteBuffer.wrap(buffer);
		
		//inode offset in block (in terms of inodes, not bytes)
		int offset = (FID-1) % inode_per_block;
		
		//set inode size=1
		bb.putInt(1,(offset*ints_per_inode*intBytes));
		
//		buffer=bb.array(); 
		
		writeInode(FID,buffer);
		return did;
	}

	@Override
	public synchronized void destroyDFile(DFileID dFID) {

//		//get inode properties
//		int offset = (FID-1) % inode_per_block;
//		ByteBuffer bb=ByteBuffer.wrap(buffer);
//		IntBuffer ib=bb.asIntBuffer();
//		
//		int[] ibuffer=new int[ints_per_inode]; //will contain single inode data
//		ib.get(ibuffer, offset*ints_per_inode, ints_per_inode);
		
		//TODO
	}
	
	private void readInode(Integer FID, byte[] buffer){
		int BID = ((FID-1) / inode_per_block) + 1;
		
		DBuffer d=cache.getBlock(BID);
		
		if (!d.checkValid()){
			d.waitValid();
		}
		d.read(buffer, 0, Constants.BLOCK_SIZE);
		cache.releaseBlock(d);
		
	}
	
	private void writeInode(Integer FID, byte[] buffer){
		int BID = ((FID-1) / inode_per_block) + 1;
		int offset = (FID-1) % inode_per_block;
		
		DBuffer d=cache.getBlock(BID);
		
		if (!d.checkClean()){
			d.waitClean();
		}
		d.write(buffer, 0, ((offset+1)*Constants.INODE_SIZE));
		cache.releaseBlock(d);
	}

	@Override
	public int read(DFileID dFID, byte[] buffer, int startOffset, int count) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int write(DFileID dFID, byte[] buffer, int startOffset, int count) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int sizeDFile(DFileID dFID) {
		// TODO Auto-generated method stub
		return 0;
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
