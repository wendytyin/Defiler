package common;

/* typedef DFileID to int */
public class DFileID {

	private int _dFID;

	public DFileID(int dFID) {
		_dFID = dFID;
	}

	public int getDFileID() {
		return _dFID;
	}
	    
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + _dFID;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DFileID other = (DFileID) obj;
		if (_dFID != other._dFID)
			return false;
		return true;
	}
	
	    
	public String toString(){
		return _dFID+"";
	}

	/**
	 * Calculates DFileID number from the inode's block id and offset within the block
	 * @param BID block ID
	 * @param iOffset inode offset
	 * @return file ID
	 */
	public static int getFID(int BID, int iOffset){
		return (((BID-1)*Constants.INODES_PER_BLOCK)+iOffset+1);
	}
	
	/**
	 * @param FID file ID
	 * @return block ID (offset within byte array in terms of blocks, not bytes)
	 */
	public static int getBID(int FID){
		return ((FID-1) / Constants.INODES_PER_BLOCK) + 1;
	}
	/**
	 * @param FID file ID
	 * @return inode offset within the block in terms of inodes (not bytes)
	 */
	public static int getInodeOffset(int FID){
		return (FID-1) % Constants.INODES_PER_BLOCK;
	}

}
