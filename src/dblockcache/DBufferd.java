package dblockcache;

import common.Constants;

/*
 * EACH BUFFER HAS AT MOST ONE I/O OPERATION PENDING AT ONE TIME 
 * So we must have synchronization on waitClean/waitValid... //wty3
 * 
 */
public class DBufferd extends DBuffer {
	
	private byte[] myBuffer;
	private int BID; //block id
	private boolean clean;
	private boolean busy;
	private Constants.DiskOperationType op;
	

	public DBufferd(int blockid){
		BID=blockid;
		clean=true;
		busy=false;
	}
	
	@Override
	public void startFetch() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void startPush() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean checkValid() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean waitValid() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean checkClean() {
		return clean;
	}

	@Override
	public boolean waitClean() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isBusy() {
		return busy;
	}

	@Override
	public int read(byte[] buffer, int startOffset, int count) {
		return 0;
	}

	@Override
	public int write(byte[] buffer, int startOffset, int count) {
		return 0;
	}

	@Override
	public void ioComplete() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getBlockID() {
		return BID;
	}

	@Override
	public byte[] getBuffer() {
		return myBuffer;
	}
	
	@Override
	public boolean equals(Object o){
		//TODO compare block ids. Note each dbuffer must map 1:1 with blockid //wty3
		return false;
	}
	
	@Override
	public String toString(){
		return BID+" ";
	}

}
