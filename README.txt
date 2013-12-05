/**********************************************
 * Please DO NOT MODIFY the format of this file
 **********************************************/

/*************************
 * Team Info & Time spent
 *************************/

	Name1: Wendy Yin	// Edit this accordingly
	NetId1: wty3	 	// Edit
	Time spent: 9 hours 	// Edit 

	Name2: Helena Wu 	// Edit this accordingly
	NetId2: hw87	 	// Edit
	Time spent: 9 hours 	// Edit 

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
CONSTANTS
	INODE_SIZE must be a whole number multiple (>=3) of size of integer in bytes (4 bytes).
	Valid DFileIDs span [1-MAX_DFILES]. The inode region is fixed according to Constants.MAX_DFILES.
	BlockIDs span [(((Constants.MAX_DFILES-1)/inode_per_block)+2) - (NUM_OF_CACHE_BLOCKS-1)].
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
 	If we assume a MAX_FILE_SIZE of 500 blocks, this structure imposes a lower limit of ~88 bytes per block. (x^2+x-500 ; x~=22 ; 22 ints/block * 4 bytes/int = 88)
	
DFS
	SPECIAL CASE: if an inode has size==1 but its block map is all 0s, it is a newly created DFile that is empty.
	This is important for different programs using the same VDF during initialization.
	Inodes must be zeroed when their corresponding files are destroyed. An inode with a size of 0 is free. A blockID==0 in an inode is free. 
	
	RULES FOR VDF/FILE SYSTEM VAILIDITY:
	- DFileIDs are calculated from blockID and inode offset, so an inode can never store an invalid DFileID.
	However, a user can request an invalid DFileID through read() or write(). DFS will return -1.
	- The size of a file does not include the number of bytes of metadata. It only records size of the data. 
	- If the size recorded in an inode is smaller than the total number of blocks allotted, DFS will ignore the later blocks, recording them as "free" blocks which may be overwritten. The recorded size does not change unless a single write to the file is larger than the current size. 
	- If the size recorded in an inode is larger than the total number of blocks allotted, DFS will round the file size to (BLOCK_SIZE*number of blocks used). DFS.sizeDFile() will likely return an incorrect number (that multiple of BLOCK_SIZE) until read() is called, at which point DFS add up the number of bytes read into each buffer and updates the file size. 
	- If a blockID appears in more than one inode, the inode with lower DFileID retains the block and the later inode is truncated by changing the size to (BLOCK_SIZE*number of blocks up to contended block). E.g. the 3rd direct block in inode 6 is already used in inode 5; inode 6 size=BLOCK_SIZE*2.
	- If a blockID is invalid (outside of data blocks region), the file is truncated (see previous).


/********************* CODE DETAILS ***************************/


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