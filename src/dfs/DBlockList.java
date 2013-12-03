package dfs;

import common.Constants;

//TODO: TO STORE INODE BLOCK MAPS
public class DBlockList {
	
	private int size;
	private Integer[] blocks=new Integer[(Constants.BLOCK_SIZE/Constants.INODE_SIZE)-1];
	
	public DBlockList(int size, Integer[] blocks){
		this.size=size;
		//TODO: COPY BLOCKS IN
	}

}
