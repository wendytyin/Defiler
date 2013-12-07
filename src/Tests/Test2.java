package Tests;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.Scanner;

import common.DFileID;
import dfs.DFSd;

public class Test2 {
		public static void main(String[] args) throws UnsupportedEncodingException, FileNotFoundException{
			
			DFSd Filer = DFSd.instance();
			Filer.init();
//		
//			Client_auto c1 = new Client_auto(Filer,"src/Tests/c1_act");//c1 and c2 for interaction
//			new Thread(c1).start();
//			
//			Client_auto c2 = new Client_auto(Filer,"src/Tests/c2_act");
//			new Thread(c2).start();
//			

//			Client_auto c3 = new Client_auto(Filer,"src/Tests/c3_act"); //c3 tests max file size
//			new Thread(c3).start();
			
			Client_auto c4 = new Client_auto(Filer,"src/Tests/c4_act"); 
			new Thread(c4).start();
			
		}
}
