/**********************************************
 * Please DO NOT MODIFY the format of this file
 **********************************************/

/*************************
 * Team Info & Time spent
 *************************/

	Name1: Wendy Yin	// Edit this accordingly
	NetId1: wty3	 	// Edit
	Time spent: 25 hours 	// Edit 

	Name2: Helena Wu 	// Edit this accordingly
	NetId2: hw87	 	// Edit
	Time spent: 25 hours 	// Edit 

/******************
 * Files to submit
 ******************/

	lab4.jar // An executable jar including all the source files and test cases.
	README	// This file filled with the lab implementation details
        DeFiler.log   // (optional) auto-generated log on execution of jar file

/************************
 * Implementation details
 *************************/

/* 
 * This section should contain the implementation details and a overview of the
 * results. You are required to provide a good README document along with the
 * implementation details. In particular, you can pseudocode to describe your
 * implementation details where necessary. However that does not mean to
 * copy/paste your Java code. Rather, provide clear and concise text/pseudocode
 * describing the primary algorithms (for e.g., scheduling choices you used)
 * and any special data structures in your implementation. We expect the design
 * and implementation details to be 3-4 pages. A plain textfile is encouraged.
 * However, a pdf is acceptable. No other forms are permitted.
 *
 * In case of lab is limited in some functionality, you should provide the
 * details to maximize your partial credit.  
 * */
 
 
 
/********************* RULES ***************************/
ALL CONCRETE CLASSES ARE NAMED <ABSTRACT SUPERCLASS NAME>d.java.

CONSTANTS
	INODE_SIZE must be a whole number multiple (>=3) of size of integer in bytes (4 bytes).
	Valid DFileIDs span [1-MAX_DFILES]. The inode region is fixed according to Constants.MAX_DFILES.
	BlockIDs span [(((Constants.MAX_DFILES-1)/INODES_PER_BLOCK)+2) - (NUM_OF_CACHE_BLOCKS-1)].
	BID 0 is left empty, and is available for future metadata use.
	
	DFileIDs are calculated using this equation:
------
FID = file ID
BID = block ID
offset = offset of inode within block
 
FID = (BID-1) * inode_per_block + offset + 1
 
BID = ((FID-1) / inode_per_block) + 1
offset = (FID-1) % inode_per_block
------
 
 	MAX_FILE_SIZE (in bytes) should be approximately equal to or less than:
------
BLOCK_SIZE * ((ints_per_inode-3) + (ints_per_block) + (ints_per_block)^2)
 
	where
ints_per_inode = INODE_SIZE/4
ints_per_block = BLOCK_SIZE/4
------
 	given that each inode contains:
 	one int size, 
 	any number of direct blocks (number = ints_per_inode - 3) 
 	one indirect block, located at the second-to-last integer in the inode, and 
 	one doubly indirect block, located at the last integer in the inode.
 	This is also why the inode must be at least 3*Int.size bytes, to store the size field and two blocks.
 	If we assume a MAX_FILE_SIZE of 500 blocks, this structure imposes a lower limit of ~88 bytes per block. (x^2+x=500 ; x~=22 ; 22 ints/block * 4 bytes/int = 88)
 	
 	The maximum size of VDF is limited by the size of an int, since we are using ints to index into the byte array.
	
DFS
	SPECIAL CASE: if an inode has size==1 but its block map is all 0s, it is a newly created DFile that is empty.
	This is important for different programs using the same VDF during initialization, so they recognize empty DFiles without the aid of a DFileID field in the disk's inode.
	Inodes are zeroed when their corresponding files are destroyed. An inode with a size of 0 is free. A blockID==0 in an inode is free. 
	
	RULES FOR VDF/FILE SYSTEM VAILIDITY:
	- DFileIDs are calculated from blockID and inode offset, so an inode can never store an invalid DFileID.
	However, a user can request an invalid DFileID through read() or write(). DFS will return -1.
	- The size of a file does not include the number of bytes of metadata. It only records size of the data. 
	- If the size recorded in an inode is smaller than the total number of blocks allotted, DFS will ignore the later blocks, recording them as "free" blocks during initialization which may be overwritten. The recorded size does not change unless a single write to the file is larger than the current size. 
	- If the size recorded in an inode is larger than the total number of blocks allotted, DFS will round the file size to (BLOCK_SIZE*number of blocks used).
	- If a blockID appears in more than one inode, the inode with lower DFileID retains the block and the later inode is truncated by changing the size to (BLOCK_SIZE*number of blocks up to contended block). E.g. the 3rd direct block in inode 6 is already used in inode 5; inode 6 size=BLOCK_SIZE*2.
	- If a blockID is invalid (outside of data blocks region), the file is truncated (see previous).

/********************* CODE DETAILS ***************************/

A Client program (Tests folder) gets a Singleton instance of DFSd and calls DFS.init before creating threads. 
DFSd stores details of each file's block map, its size, the free data blocks, and the available DFileIDs.
DFS enforces two areas of synchronization. 
Read and write synchronize on the DFileID that was passed in, so multiple threads may access different files concurrently, but read and write on a single file is serialized.
Any operation in DFS that changes the set of free data blocks, free inodes, or mapping of DFileIDs to block maps, synchronizes on the DFS object. 

In order to read/write a file, DFS gets the block map from its internal mapping of {DFileID->block map}.
It then loops through the block map and requests each block from DBufferCache. 
DBufferCache.getBlock(blockID) returns a DBuffer object, which that thread now holds. 

DBufferCache also has two areas of synchronization. 
The code synchronizes on a DBuffer object to check if that DBuffer is busy (ie the DBuffer is doing an IO or held by another thread).
If the DBuffer is busy, the current thread waits until the other thread calls Cache.releaseBlock() or the thread inside VirtualDisk calls ioComplete() on the DBuffer.
The code synchronizes on the DBufferCache if it needs to update the mapping of {block ID->DBuffer} or the linked list used for LRU calculation.

LRU is implemented with a linked list. The head of the linked list is the least recently accessed DBuffer. If no DBuffer contains the data we want and we've filled the cache to Constants.NUM_OF_CACHE_BLOCKS, Cache scans through the linked list starting from the head. 
It selects the first DBuffer in the linked list that is not busy, deletes that DBuffer, creates a new one, and adds it to the tail of the LRU linked list.

We debated whether to use this current method of discarding DBuffers, which gives more work to the garbage collector, versus resetting each DBuffer to a new blockID and byte[] buffer. 
We ended up choosing this method to change the API given in the lab4.pdf as little as possible (there is no "reset" function listed), and because reconstructing objects ensured the DBuffers all started in a similar state with similar clean/busy/valid flags.

The Client receives and holds the DBuffer from Cache, and at this point it knows it is the only thread holding that particular DBuffer, and if it is reading/writing, the only thread allowed to read/write that file at that point, thanks to the layers of synchronized statements.
It can then call the methods in DBuffer. 
DBuffer submits the necessary changes to VirtualDisk through startRequest(), which puts the io request on a queue. 
This is a single consumer-multiple producer synchronization problem. We have a single thread that exclusively runs VirtualDisk. 
That thread waits on the queue for requests, and when a request is submitted by the thread running a DBuffer (using the synchronized method VirtualDisk.startRequest), the VirtualDisk thread wakes up from the synchronized method getRequest() and removes a request, then runs it. 
We separated the portion that runs the request from the portion that removes the request, so that DBuffers may continue submitting requests to the queue while the VirtualDisk thread does file operations. 


/************************
 * Feedback on the lab
 ************************/

/*
 * Any comments/questions/suggestions/experiences that you would help us to
 * improve the lab.
 * */

/************************
 * References
 ************************/

/*
 * List of collaborators involved including any online references/citations.
 * */

 
 GoF book (Design Patterns: Elements of Reusable Object-Oriented Software)