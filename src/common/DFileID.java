package common;

/* typedef DFileID to int */
public class DFileID {

	private int _dFID;

	public DFileID(int dFID) {
		_dFID = dFID;
	}

	public int getDFileID() {
		return _dFID;
	}
	    
	public boolean equals(Object other){
		DFileID otherID =  (DFileID) other;
		if(otherID.getDFileID() == _dFID){
			return true;
		}
		return false;
	}
	    
	public String toString(){
		return _dFID+"";
	}
}
