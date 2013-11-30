package virtualdisk;

import java.io.FileNotFoundException;
import java.io.IOException;

import common.Constants.DiskOperationType;

import dblockcache.DBuffer;

public class VirtualDiskd extends VirtualDisk {
	
	private static VirtualDiskd _instance;
	
	protected VirtualDiskd(String volName, boolean format)
			throws FileNotFoundException, IOException {
		super(volName, format);
	}
	protected VirtualDiskd(boolean format)
			throws FileNotFoundException, IOException {
		super(format);
	}
	protected VirtualDiskd()
			throws FileNotFoundException, IOException {
		super();
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
	
	
	
	@Override
	public void startRequest(DBuffer buf, DiskOperationType operation)
			throws IllegalArgumentException, IOException {
		// TODO Auto-generated method stub
		
		//TODO: STORE THAT DBUFFER REFERENCE, CALLS UP GETBUFFER, GETBUFFERID, IOCOMPLETE
		
	}

}
