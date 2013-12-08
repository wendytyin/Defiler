package Tests;

import java.io.FileNotFoundException;

import dfs.DFSd;

public class DemoTest {

	public static void main (String[] args) throws FileNotFoundException, InterruptedException{
		DFSd Filer = DFSd.instance(true);
		Filer.init();
		
		
		Client_auto c = new Client_auto(Filer, "src/Tests/init_disk_act"); //run this first to initialize testable file state
		Client_auto c1 = new Client_auto(Filer, "src/Tests/demo_test_1");
		Client_auto c2 = new Client_auto(Filer,"src/Tests/demo_test_2");
		Client_auto c3 = new Client_auto(Filer,"src/Tests/demo_test_3");
		
		
		new Thread(c).start();
		Thread.sleep(500);
		
		new Thread(c1).start();
		new Thread(c2).start();
		new Thread(c3).start();
		
	}
	
	
}
