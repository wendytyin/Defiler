package Tests;

import java.io.UnsupportedEncodingException;

import common.DFileID;
import dfs.DFSd;

public class Test {
	
	
	public static void main(String[] args) throws UnsupportedEncodingException{
		
		DFSd Filer = DFSd.instance();
		Filer.init();
		
		
		Client c1 = new Client("1",Filer);
		//Client c2 = new Client("2",Filer);
		
		//Client c3 = new Client("3",Filer);
		new Thread(c1).start();
		//new Thread(c2).start();
		//new Thread(c3).start();
		
		//c1.createDFile();
		//c2.createDFile();
		//c1.createDFile();
		//c3.createDFile();
		//c1.sync();
		c1.listAllDFiles();
		
		
		DFileID id = new DFileID(2);
		
	//	DFileID id1 = new DFileID(3);
	//	DFileID id2 = new DFileID(90);

	//	c1.destroyFile(id);
	//	c2.destroyFile(id);
		
	//	c1.sync();
	//	c1.listAllDFiles();
		byte[] buffer = new byte[10];
		String msg = "hello";
		buffer = msg.getBytes();
		c1.write(id, buffer, 0, buffer.length);
		c1.sync();
		
		System.out.println("after write, size of filer is: " + Filer.sizeDFile(id));
		
		
		byte[] buf2 = new byte[10];
		c1.read(id, buf2, 0,buf2.length);
		
		
		String str = new String(buf2,"UTF-8");
		
		System.out.println("file content is = " + str);
		
		
		
		
	}

}
