package dblockcache;

import common.Constants.DiskOperationType;

public class DBufferObj {
	private DBuffer buf;
	private DiskOperationType op;
	
	public DBufferObj(DBuffer buf, DiskOperationType op){
		this.buf=buf;
		this.op=op;
	}
	
	public DBuffer getBuf(){
		return buf;
	}

	public DiskOperationType getOp() {
		return op;
	}

}
