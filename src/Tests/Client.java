package Tests;

import java.util.List;

import common.DFileID;
import dfs.DFSd;

public class Client implements Runnable {
	private String name;
	private DFSd Filer;
	
	
	public Client(String name, DFSd Filer){
		this.name = name;
		this.Filer = Filer;
	}
	
	//wrapper functions
	public DFileID createDFile(){
		return Filer.createDFile();
	}
	
	public void destroyFile(DFileID dFID){
		Filer.destroyDFile(dFID);
	}
	
	public int read(DFileID dFID, byte[] buffer, int startOffset, int count){
		return Filer.read(dFID, buffer, startOffset, count);
	}
	public int write(DFileID dFID, byte[] buffer, int startOffset, int count){
		return Filer.write(dFID, buffer, startOffset, count);
	}
	
	public List<DFileID> listAllDFiles(){
		System.out.println("Client " + this.name + " is listing files");
		return Filer.listAllDFiles();
	}
	public void sync(){
		Filer.sync();
	}
	
	@Override
	public void run() {
		while(true){
		}
		
	}

}
