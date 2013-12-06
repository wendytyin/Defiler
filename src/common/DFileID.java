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
	    
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + _dFID;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DFileID other = (DFileID) obj;
		if (_dFID != other._dFID)
			return false;
		return true;
	}
	
	    
	public String toString(){
		return _dFID+"";
	}
}
