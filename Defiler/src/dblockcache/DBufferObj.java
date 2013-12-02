package dblockcache;

import common.Constants.DiskOperationType;

public class DBufferObj {
	private DBuffer buf;
	private DiskOperationType op;
	
	public DBufferObj(DBuffer buf,DiskOperationType operation){
		this.buf = buf;	
		this.op = operation;
	}
	public DiskOperationType getOp(){
		return this.op;
	}
		
	public DBuffer getBuf(){
		return this.buf;
	}
	
}
