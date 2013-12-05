package dblockcache;

import java.io.IOException;

import virtualdisk.VirtualDisk;
import common.Constants;
import common.Constants.DiskOperationType;

public class DBufferd extends DBuffer {
	
	private static VirtualDisk vd; //to be initialized in DBufferCache
	
	private byte[] myBuffer;
	public final int BID; //block id
	private boolean clean;
	private boolean busy;
	private boolean valid;

	public DBufferd(int blockid, VirtualDisk vd){
		BID=blockid;
		DBufferd.vd=vd;
		clean=true;
		busy=true;
		valid=false;
		myBuffer=new byte[Constants.BLOCK_SIZE];
	}
	
	/*
	 * Rules: you must hold the buffer to call any of the methods
	 */
	public void hold(){
		busy=true;
	}
	public void release(){
		busy=false;
	}
	
	//Asynchronous return from vd
	@Override
	public void startFetch() {
		try {
			vd.startRequest(this, DiskOperationType.READ);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	//Asynchronous return from vd
	@Override
	public void startPush() {
		try {
			vd.startRequest(this, DiskOperationType.WRITE);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean checkValid() {
		return valid;
	}

	@Override
	public synchronized boolean waitValid() {
		while (!valid){
			startFetch();
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return valid;
	}

	@Override
	public boolean checkClean() {
		return clean;
	}

	@Override
	public synchronized boolean waitClean() {
		while (!clean){
			startPush();
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return clean;
	}

	@Override
	public boolean isBusy() {
		return busy;
	}

	@Override
	public int read(byte[] buffer, int startOffset, int count) {
		if (valid){
			if (startOffset>=Constants.BLOCK_SIZE){return -1;}
			if (count>=Constants.BLOCK_SIZE){count=Constants.BLOCK_SIZE;}
			for (int i=0;i<count;i++){
				buffer[i+startOffset]=myBuffer[i]; //copy into buffer
			}
		} else {return -1;} //invalid data
		return count; 
	}

	@Override
	public int write(byte[] buffer, int startOffset, int count) {
		if (clean){
			if (startOffset>=Constants.BLOCK_SIZE){return -1;}
			if (count>=Constants.BLOCK_SIZE){count=Constants.BLOCK_SIZE;}
			for (int i=0;i<count;i++){
				myBuffer[i]=buffer[i+startOffset]; //copy into buffer
			}
		} else {return -1;} //was dirty, need to push first
		clean=false;
		return count;
	}

	@Override
	public synchronized void ioComplete() {
		valid=true;
		clean=true;
		
		notifyAll();
	}

	@Override
	public int getBlockID() {
		return BID;
	}

	@Override
	public byte[] getBuffer() {
		return myBuffer;
	}
	
	
	//TODO: NECESSARY?
	@Override
	public boolean equals(Object o){
		return this.BID==((DBufferd)o).BID;
	}
	
	@Override
	public String toString(){
		return BID+" ";
	}

}
