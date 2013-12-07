package Tests;

import common.DFileID;

import dfs.DFSd;

public class Test {
	
	
	public static void main(String[] args){
		
		DFSd Filer = DFSd.instance();
		Filer.init();
		
		
		Client c1 = new Client("1",Filer);
		//Client c2 = new Client("2",Filer);
		
		//Client c3 = new Client("3",Filer);
		new Thread(c1).start();
		//new Thread(c2).start();
		//new Thread(c3).start();
		
		//c1.createDFile();
		//c1.createDFile();
		//c1.createDFile();
		//c3.createDFile();
		//c1.sync();
		c1.listAllDFiles();
		
		
		DFileID id = new DFileID(2);
		c1.destroyFile(id);
		c1.sync();
		c1.listAllDFiles();
		
		
	}

}
