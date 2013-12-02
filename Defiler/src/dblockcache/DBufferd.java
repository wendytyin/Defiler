package dblockcache;

/*
 * EACH BUFFER HAS AT MOST ONE I/O OPERATION PENDING AT ONE TIME
 * 
 */
public class DBufferd extends DBuffer {
	
	private byte[] myBuffer;

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
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean waitClean() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isBusy() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int read(byte[] buffer, int startOffset, int count) {
		// TODO ASYNCHRON0US TO REQUEST QUEUE ON VIRTUALDISK 
		return 0;
	}

	@Override
	public int write(byte[] buffer, int startOffset, int count) {
		// TODO ASYNCHRONOUS TO REQUEST QUEUE ON VIRTUALDISK
		return 0;
	}

	@Override
	public void ioComplete() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getBlockID() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public byte[] getBuffer() {
		// TODO Auto-generated method stub
		return null;
	}

}
