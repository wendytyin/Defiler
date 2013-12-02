package dblockcache;

public class DBufferCached extends DBufferCache {
	
	private static DBufferCached _instance;
	
	//TODO: KEEP FIXED SIZE of _cacheSize LIST OF DBUFFERS
	
	protected DBufferCached(int cacheSize) {
		super(cacheSize);
	}
	
	public static void init(int cacheSize){
		_instance=new DBufferCached(cacheSize);
	}
	
	public static DBufferCached instance(){
		return _instance;
	}
	

	@Override
	public DBuffer getBlock(int blockID) {
		// TODO Auto-generated method stub
		
		//TODO: LRU
		
		return null;
	}

	@Override
	public void releaseBlock(DBuffer buf) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sync() {
		// TODO Auto-generated method stub
		
	}

}
