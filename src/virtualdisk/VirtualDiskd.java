package virtualdisk;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;

import common.Constants.DiskOperationType;
import dblockcache.DBuffer;
import dblockcache.DBufferObj;


public class VirtualDiskd extends VirtualDisk {
	
	private static VirtualDiskd _instance;
	private LinkedList<DBufferObj> q = new LinkedList<DBufferObj>();

	protected VirtualDiskd(String volName, boolean format)
			throws FileNotFoundException, IOException {
		super(volName, format);
		runRequest();
	}
	protected VirtualDiskd(boolean format)
			throws FileNotFoundException, IOException {
		super(format);
		runRequest();
	}
	protected VirtualDiskd()
			throws FileNotFoundException, IOException {
		super();
		runRequest();
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
	
	
	public void runRequest() throws IOException{
		while(q.size()!=0){
			DBufferObj tmp=q.poll();
			if (tmp.getOp()==DiskOperationType.READ){
				int i=readBlock(tmp.getBuf());
				if (i==-1){
					System.err.println("read failed");
				}
				tmp.getBuf().ioComplete();
			} else if (tmp.getOp()==DiskOperationType.WRITE){
				writeBlock(tmp.getBuf());
				tmp.getBuf().ioComplete();
			} else {
				System.err.println(" wut");
			}
		}	
	}
	@Override
	synchronized public void startRequest(DBuffer buf, DiskOperationType operation)
			throws IllegalArgumentException, IOException {
		//TODO: STORE THAT DBUFFER REFERENCE, IOCOMPLETE
		{
			DBufferObj bufObj = new DBufferObj(buf, operation);
			q.add(bufObj);			
		}
			
	}

}
