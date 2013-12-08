package Tests;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Scanner;

import common.DFileID;
import dfs.DFSd;

public class Client_auto implements Runnable {
	private String name;
	private DFSd Filer;
	private BufferedReader Br;
	
	public Client_auto(DFSd Filer, String fileName) throws FileNotFoundException{
		this.Br = new BufferedReader(new FileReader(fileName));
		this.Filer = Filer;
	}
	
	//wrapper functions
	public DFileID createDFile(){
		DFileID fid = Filer.createDFile();
		System.out.println(this.name + " creates file: " + fid.getDFileID() );

		return fid;
	}
	
	public void destroyFile(DFileID dFID){
		System.out.println(this.name + " destroys file: " + dFID.getDFileID());
		Filer.destroyDFile(dFID);
	}
	
	public int read(DFileID dFID, byte[] buffer, int startOffset, int count){

		return Filer.read(dFID, buffer, startOffset, count);
	}
	public int write(DFileID dFID, byte[] buffer, int startOffset, int count){
		return Filer.write(dFID, buffer, startOffset, count);
	}
	
	public List<DFileID> listAllDFiles(){
		System.out.println(this.name + " is listing files");
		return Filer.listAllDFiles();
	}
	public void sync(){
		Filer.sync();
	}

	
	@Override
	public void run() {
		String line;
		String[] actions;
		Integer fid, sizeBuf, offset;
		byte[] buf;
		
		try {
			line = Br.readLine();
			this.name = line;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			while((line=Br.readLine())!=null && line.length()>0){
				char first = line.charAt(0);	
				switch(first){
					case 'c':
							createDFile();
							break;
					case 'd':
						actions = line.split(" ");
						fid = Integer.parseInt(actions[1]); 
						destroyFile(new DFileID(fid));
						break;
					case 'l':
						listAllDFiles();
						break;
					case 's':
						sync();
						break;
					case 'r':
						actions = line.split(" ");
						fid= Integer.parseInt(actions[1]); 
						sizeBuf= Integer.parseInt(actions[2]);
						offset = Integer.parseInt(actions[3]);
						String file = actions[4];
						
						buf = new byte[sizeBuf];
						read(new DFileID(fid),buf,offset,buf.length-offset);
						String content = new String(buf);
						
						PrintWriter writer = new PrintWriter(new String("src/Tests/Results/").concat(file).concat(".txt"), "UTF-8"); //change to outFile name
						writer.println(content);
						writer.close();
						
						break;
						
					case 'w':
						actions = line.split(" ");
						fid= Integer.parseInt(actions[1]); 
						offset = Integer.parseInt(actions[2]);
						String from_file = actions[3];
						String path = new String("src/Tests/").concat(from_file);
						String msg = new Scanner(new File(path), "UTF-8").useDelimiter("\\A").next();
						buf = new byte[msg.length()];
												
						buf=msg.getBytes();
						System.out.println(this.name + " writes to file " + fid);
						int status = write(new DFileID(fid),buf,offset,buf.length);
						if(status<0){
							System.out.println("read failed, file doesn't exist?");
						}
						break;
					case '\r': //carriage return
						break;
					default:
						System.out.println("command not recognized: "+ line + " test will exit.");
						System.exit(1);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
