package Tests;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import dfs.DFSd;

public class AutomateTest {
		public static void main(String[] args) throws UnsupportedEncodingException, FileNotFoundException{
			
			DFSd Filer = DFSd.instance();
			Filer.init();
			
//Previous tests...will not work for an empty VirtualDisk	
//			Client_auto c1 = new Client_auto(Filer,"src/Tests/c1_act");//c1 and c2 for concurrency interactions
//			new Thread(c1).start();
//			
//			Client_auto c2 = new Client_auto(Filer,"src/Tests/c2_act");
//			new Thread(c2).start();
//			

//			Client_auto c3 = new Client_auto(Filer,"src/Tests/c3_act"); // tests max file size
//			new Thread(c3).start();
			
//			Client_auto c4 = new Client_auto(Filer,"src/Tests/c4_act"); // tests writes and reads of 500KB file
//			new Thread(c4).start();
			
			
			
			
		}
}
