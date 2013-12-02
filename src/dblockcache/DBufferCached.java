package dblockcache;

import java.util.HashMap;
import java.util.LinkedList;

public class DBufferCached extends DBufferCache {
	
	private static DBufferCached _instance;
	
	private HashMap<Integer,DBuffer> bufmap=new HashMap<Integer,DBuffer>();  //map blockid->dbuffer
	private LinkedList<DBuffer> lru=new LinkedList<DBuffer>();
	
	protected DBufferCached(int cacheSize) {
		super(cacheSize);
		for (int i=0;i<cacheSize;i++){
		}
	}
	
	public static void init(int cacheSize){
		_instance=new DBufferCached(cacheSize);
	}
	
	public static DBufferCached instance(){
		return _instance;
	}
	

	@Override
	synchronized public DBuffer getBlock(int blockID) {
		// TODO Auto-generated method stub
		
		return null;
	}

	@Override
	synchronized public void releaseBlock(DBuffer buf) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sync() {
		// TODO Auto-generated method stub
		
	}

}
