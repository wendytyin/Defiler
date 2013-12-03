package dblockcache;

import java.util.HashMap;
import java.util.LinkedList;

import virtualdisk.VirtualDisk;

public class DBufferCached extends DBufferCache {
	
	private static DBufferCached _instance;
	private static VirtualDisk vd;
	
	
	private HashMap<Integer,DBuffer> bufmap=new HashMap<Integer,DBuffer>();  //map blockid -> dbuffer
	private LinkedList<Integer> lru=new LinkedList<Integer>(); // lru of blockids
	
	protected DBufferCached(int cacheSize) {
		super(cacheSize);
	}
	
	public static DBufferCached instance(int cacheSize, VirtualDisk disk){
		if (_instance==null){_instance=new DBufferCached(cacheSize);}
		DBufferCached.vd=disk;
		return _instance;
	}
	

	@Override
	public DBuffer getBlock(int blockID) {
		DBuffer d=null;
		if (bufmap.containsKey(blockID)){  //we have a buffer with blockid
			d=bufmap.get(blockID);
			synchronized(d){
				while (d.isBusy()){
					try {
						wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				((DBufferd) d).hold();
			}
			synchronized(this){ //put buffer at mru position
				lru.remove(blockID);
				lru.add(blockID);
			}
			
		} else {    	//no buffer with blockid
			if (lru.size()<_cacheSize){ 	//room for more buffers, create new buffer
				d=new DBufferd(blockID, vd);
				synchronized(this){
					lru.add(blockID);
					bufmap.put(blockID, d);
				}
			}
			else {		//no room for more buffers, kick out old buffer and create new one
				synchronized(this){
					for (int i=0;i<lru.size();i++){
						Integer tmp=lru.get(i);
						d=bufmap.get(tmp);
						if (!d.isBusy()){
							if (!d.checkClean()){d.waitClean();} //push dirty block to disk
							lru.remove(tmp);
							bufmap.remove(tmp);
							d=new DBufferd(blockID,vd);
							lru.add(blockID);
							bufmap.put(blockID, d);
						}
					} //all buffers are busy
					if (d==null){
						//TODO
					}
				}
			}
		}
		return d;
	}

	@Override
	public void releaseBlock(DBuffer buf) {
		synchronized(buf){
			((DBufferd) buf).release();
			notifyAll();
		}
	}

	@Override
	public void sync() {
		for (DBuffer d: bufmap.values()){
			synchronized(d){
				while (d.isBusy()){
					try {
						wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				((DBufferd) d).hold();
			}
			if (d.checkValid()){
				if (!d.checkClean()){
					d.waitClean();
				}
			}
			releaseBlock(d);
		}
	}

}
