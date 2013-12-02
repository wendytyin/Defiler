package virtualdisk;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;

import common.Constants.DiskOperationType;
import dblockcache.DBuffer;
import dblockcache.DBufferObj;


public class VirtualDiskd extends VirtualDisk implements Runnable {
	
	private static VirtualDiskd _instance;
	
	private LinkedList<DBufferObj> q = new LinkedList<DBufferObj>();
	private Thread t;

	//Single instance of virtualdisk, single thread running within the instance
	protected VirtualDiskd(String volName, boolean format)
			throws FileNotFoundException, IOException {
		super(volName, format);
		t=new Thread(this);
		t.start();
		
	}
	protected VirtualDiskd(boolean format)
			throws FileNotFoundException, IOException {
		super(format);
		t=new Thread(this);
		t.start();
	}
	protected VirtualDiskd()
			throws FileNotFoundException, IOException {
		super();
		t=new Thread(this);
		t.start();
	}
	
	public static void init(String volName, boolean format)
			throws FileNotFoundException, IOException {
		_instance=new VirtualDiskd(volName,format);
	}
	public static void init(boolean format)
			throws FileNotFoundException, IOException {
		_instance=new VirtualDiskd(format);
	}
	public static void init()
			throws FileNotFoundException, IOException {
		_instance=new VirtualDiskd();
	}
	
	public static VirtualDiskd instance(){
		return _instance;
	}
	
	
	//multiple producers, one consumer
	synchronized private DBufferObj getRequest() throws InterruptedException {
		while (q.size()==0){
			wait();
		}
		DBufferObj tmp=q.poll();
		return tmp;
	}

	private void doRequest(DBufferObj request){
		assert(request!=null);
		try {
			if (request.getOp()==DiskOperationType.READ){
				int i;
				i = readBlock(request.getBuf());
				if (i==-1){
					System.err.println("EOF reached, read failed");
				}
			} else if (request.getOp()==DiskOperationType.WRITE){
				writeBlock(request.getBuf());
			} else {
				System.err.println(" wut");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.err.println("Disk op failed");
			e.printStackTrace();
		}
		request.getBuf().ioComplete();
	}	
	
	@Override
	synchronized public void startRequest(DBuffer buf, DiskOperationType operation)
			throws IllegalArgumentException, IOException {
		{
			DBufferObj bufObj = new DBufferObj(buf, operation);
			q.add(bufObj);
			notifyAll(); //wake up virtualdisk thread if it is sleeping
		}
	}
	
	
	@Override
	public void run() {
		while (true){
			DBufferObj tmp;
			try {
				tmp = getRequest();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				break;
			}
			doRequest(tmp);
		}
	}

}
