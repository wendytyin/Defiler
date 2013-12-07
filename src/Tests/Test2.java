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
		
			Client_auto c1 = new Client_auto(Filer,"src/Tests/c1_act");
			new Thread(c1).start();
			
			
		}
}
