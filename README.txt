/**********************************************
 * Please DO NOT MODIFY the format of this file
 **********************************************/

/*************************
 * Team Info & Time spent
 *************************/

	Name1: Wendy Yin	// Edit this accordingly
	NetId1: wty3	 	// Edit
	Time spent: 10 hours 	// Edit 

	Name2: Helena Wu 	// Edit this accordingly
	NetId2: hw87	 	// Edit
	Time spent: 10 hours 	// Edit 

	Name3: Full Name 	// Edit this accordingly
	NetId3: fn	 	// Edit
	Time spent: 10 hours 	// Edit 

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

/*** RULES ***/
CONSTANTS
	INODE_SIZE must be a whole number multiple of size of integer in bytes (4 bytes)
	
DFS
	SPECIAL CASE: if an inode has size==1 but its block map is all 0s, it is a newly created DFile that is empty.
	This is important for different programs using the same VDF during initialization
	Inodes must be zeroed when destroyed.




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
