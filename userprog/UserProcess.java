package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.userprog.UserKernel.InsufficientFreePagesException;

import java.io.EOFException;

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see	nachos.vm.VMProcess
 * @see	nachos.network.NetProcess
 */
public class UserProcess{
	/**
	 * Allocate a new process.
	 */
	public UserProcess() {
		int numPhysPages = Machine.processor().getNumPhysPages();
		pageTable = new TranslationEntry[numPhysPages];
		for (int i=0; i<numPhysPages; i++)
			pageTable[i] = new TranslationEntry(i,i, true,false,false,false);

		//Task 1
		processID = numProcesses;
		++numProcesses;
		localFileTable = new OpenFile[16];
		globalFileTable = new FileReference[16];

		localFileTable[0] = UserKernel.console.openForReading();
		localFileTable[1] = UserKernel.console.openForWriting();
		globalFileTable[0] = new FileReference(localFileTable[0].getName());
		globalFileTable[1] = new FileReference(localFileTable[1].getName());
    
		//Task 2
		lock = new Lock();
	}

	/**
	 * Allocate and return a new process of the correct class. The class name
	 * is specified by the <tt>nachos.conf</tt> key
	 * <tt>Kernel.processClassName</tt>.
	 *
	 * @return	a new process of the correct class.
	 */
	public static UserProcess newUserProcess() {
		return (UserProcess)Lib.constructObject(Machine.getProcessClassName());
	}

	/**
	 * Execute the specified program with the specified arguments. Attempts to
	 * load the program, and then forks a thread to run it.
	 *
	 * @param	name	the name of the file containing the executable.
	 * @param	args	the arguments to pass to the executable.
	 * @return	<tt>true</tt> if the program was successfully executed.
	 */
	public boolean execute(String name, String[] args) {
		if (!load(name, args))
			return false;

		new UThread(this).setName(name).fork();

		return true;
	}

	/**
	 * Save the state of this process in preparation for a context switch.
	 * Called by <tt>UThread.saveState()</tt>.
	 */
	public void saveState() {
	}

	/**
	 * Restore the state of this process after a context switch. Called by
	 * <tt>UThread.restoreState()</tt>.
	 */
	public void restoreState() {
		Machine.processor().setPageTable(pageTable);
	}
  
  	public int accessMemory(int vaddr, byte[] data, int offset, int length, boolean read){
		//Get the virtual page number and virtual offset
		int vPageNum= vaddr / pageSize;
		int vOffset = vaddr % pageSize;
        
		TranslationEntry entry = pageTable[vPageNum];
		entry.used = true;
            
		//Calculate the physical address and memory available
		int addr = entry.ppn * pageSize + vOffset;
		byte[] mem= Machine.processor().getMemory();
                
		//If the physical address is out of bounds return 0
		if(addr < 0 || addr > mem.length || !entry.valid)
			return 0;
                    
		//Set the amount of bytes accessed
		int amount = Math.min(length, mem.length - addr);
        
		if(read) {
			System.arraycopy(mem, addr, data, offset, amount);
		}else {
			if(!entry.readOnly) {
				//Copy into memory from data
  				System.arraycopy(data, offset, mem, addr, amount);
			}else{
				return 0;
        	}
		}
        //Finally return the amount of bytes accessed
		return amount;
	}
  

	/**
	 * Read a null-terminated string from this process's virtual memory. Read
	 * at most <tt>maxLength + 1</tt> bytes from the specified address, search
	 * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
	 * without including the null terminator. If no null terminator is found,
	 * returns <tt>null</tt>.
	 *
	 * @param	vaddr	the starting virtual address of the null-terminated
	 *			string.
	 * @param	maxLength	the maximum number of characters in the string,
	 *				not including the null terminator.
	 * @return	the string read, or <tt>null</tt> if no null terminator was
	 *		found.
	 */
	public String readVirtualMemoryString(int vaddr, int maxLength) {
		Lib.assertTrue(maxLength >= 0);

		byte[] bytes = new byte[maxLength+1];

		int bytesRead = readVirtualMemory(vaddr, bytes);

		for (int length=0; length<bytesRead; length++) {
			if (bytes[length] == 0)
				return new String(bytes, 0, length);
		}

		return null;
	}

	/**
	 * Transfer data from this process's virtual memory to all of the specified
	 * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 *
	 * @param	vaddr	the first byte of virtual memory to read.
	 * @param	data	the array where the data will be stored.
	 * @return	the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data) {
		return readVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from this process's virtual memory to the specified array.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no
	 * data could be copied).
	 *
	 * @param	vaddr	the first byte of virtual memory to read.
	 * @param	data	the array where the data will be stored.
	 * @param	offset	the first byte to write in the array.
	 * @param	length	the number of bytes to transfer from virtual memory to
	 *			the array.
	 * @return	the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data, int offset,
								 int length) {
		lock.acquire();

		//get the first page and the length for the first bytes
		int pages = ((length + vaddr%pageSize)/pageSize)+1;
		int firstLength = Math.min(length, pageSize - vaddr%pageSize);
            
		// have the read boolean set to true so it reads the bytes
		int amount = accessMemory(vaddr, data, offset, firstLength, true);
                    
		// if there’s more then one page do it for each page
		if(pages > 1){
			for(int i = 1; i < pages; ++i){
				amount += accessMemory((vaddr/pageSize +i*pageSize), data, offset+amount, Math.min(length-amount, pageSize), true);
			}
		}
		lock.release();
		return amount;
    }

	/**
	 * Transfer all data from the specified array to this process's virtual
	 * memory.
	 * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 *
	 * @param	vaddr	the first byte of virtual memory to write.
	 * @param	data	the array containing the data to transfer.
	 * @return	the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data) {
		return writeVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from the specified array to this process's virtual memory.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no
	 * data could be copied).
	 *
	 * @param	vaddr	the first byte of virtual memory to write.
	 * @param	data	the array containing the data to transfer.
	 * @param	offset	the first byte to transfer from the array.
	 * @param	length	the number of bytes to transfer from the array to
	 *			virtual memory.
	 * @return	the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data, int offset,
								  int length) {
		lock.acquire();

		//get the first page and the length for the first bytes
		int pages = ((length + vaddr%pageSize)/pageSize)+1;
		int firstLength = Math.min(length, pageSize - vaddr%pageSize);

		// have the read boolean set to false so the bytes are written
		int amount = accessMemory(vaddr, data, offset, firstLength, false);
		if(pages > 1){
			for(int i = 1; i < pages; ++i){
				amount += accessMemory((vaddr/pageSize +i*pageSize), data, offset+amount, Math.min(length-amount, pageSize), false);
			}
		}
		lock.release();
		return amount;
    }

	public int sysClose(int fileDescriptor){

		if(fileDescriptor < 0 || fileDescriptor > 15)
			return -1;

		if(localFileTable[fileDescriptor] == null)
			return -1;

		else {
			localFileTable[fileDescriptor].close();
			localFileTable[fileDescriptor] = null;
		}
		return 0;
	}

	public int unlink(int nameAddress) {
		if(nameAddress < 0)
			return -1;

		String file = readVirtualMemoryString(nameAddress, 256);
		if(file == null)
			return -1;

		int result = -1;
		for(int index = 0; index < 15; index++){
			if(globalFileTable[index] != null && globalFileTable[index].fileName == file){
				result = index;
				break;
			}
		}
		if(result != -1)
			return -1;

		if(ThreadedKernel.fileSystem.remove(file))
			return 0;

		else
			return -1;
	}

	public int Syswrite(int fileDescriptor, int bufferAddress, int count){

		if(count < 0)
			return -1;

		if(fileDescriptor < 0 || fileDescriptor > 15)
			return -1;

		if(localFileTable[fileDescriptor] == null)
			return -1;

		OpenFile writeFile = localFileTable[fileDescriptor];
		int bytesRemaining = count;
		byte [] writing = new byte[count];
		int increment = 0;
		int written = 0;
		int toWrite;
		int total = 0;

		while(bytesRemaining > 0){
			if(bytesRemaining < pageSize){
				increment = bytesRemaining; }
			else {
				increment = pageSize; }

			toWrite = readVirtualMemory(bufferAddress, writing, 0, increment);
			written = writeFile.write(writing, 0, toWrite);


			if(written == -1)
				return -1;

			if(toWrite != written || written != increment)
				return -1;

			total += written;
			bytesRemaining -= written;
			bufferAddress += written;
		}

		return total;
	}

	/**
	 * Load the executable with the specified name into this process, and
	 * prepare to pass it the specified arguments. Opens the executable, reads
	 * its header information, and copies sections and arguments into this
	 * process's virtual memory.
	 *
	 * @param	name	the name of the file containing the executable.
	 * @param	args	the arguments to pass to the executable.
	 * @return	<tt>true</tt> if the executable was successfully loaded.
	 */
	private boolean load(String name, String[] args) {
		Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");

		OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
		if (executable == null) {
			Lib.debug(dbgProcess, "\topen failed");
			return false;
		}

		try {
			coff = new Coff(executable);
		}
		catch (EOFException e) {
			executable.close();
			Lib.debug(dbgProcess, "\tcoff load failed");
			return false;
		}

		// make sure the sections are contiguous and start at page 0
		numPages = 0;
		for (int s=0; s<coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);
			if (section.getFirstVPN() != numPages) {
				coff.close();
				Lib.debug(dbgProcess, "\tfragmented executable");
				return false;
			}
			numPages += section.getLength();
		}

		// make sure the argv array will fit in one page
		byte[][] argv = new byte[args.length][];
		int argsSize = 0;
		for (int i=0; i<args.length; i++) {
			argv[i] = args[i].getBytes();
			// 4 bytes for argv[] pointer; then string plus one for null byte
			argsSize += 4 + argv[i].length + 1;
		}
		if (argsSize > pageSize) {
			coff.close();
			Lib.debug(dbgProcess, "\targuments too long");
			return false;
		}

		// program counter initially points at the program entry point
		initialPC = coff.getEntryPoint();

		// next comes the stack; stack pointer initially points to top of it
		numPages += stackPages;
		initialSP = numPages*pageSize;

		// and finally reserve 1 page for arguments
		numPages++;

		if (!loadSections())
			return false;

		// store arguments in last page
		int entryOffset = (numPages-1)*pageSize;
		int stringOffset = entryOffset + args.length*4;

		this.argc = args.length;
		this.argv = entryOffset;

		for (int i=0; i<argv.length; i++) {
			byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
			Lib.assertTrue(writeVirtualMemory(entryOffset,stringOffsetBytes) == 4);
			entryOffset += 4;
			Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
					argv[i].length);
			stringOffset += argv[i].length;
			Lib.assertTrue(writeVirtualMemory(stringOffset,new byte[] { 0 }) == 1);
			stringOffset += 1;
		}

		return true;
	}

	/**
	 * Allocates memory for this process, and loads the COFF sections into
	 * memory. If this returns successfully, the process will definitely be
	 * run (this is the last step in process initialization that can fail).
	 *
	 * @return	<tt>true</tt> if the sections were successfully loaded.
	 */
	protected boolean loadSections() {
		if (numPages > Machine.processor().getNumPhysPages()) {
			coff.close();
			Lib.debug(dbgProcess, "\tinsufficient physical memory");
			return false;
		}
		// load sections
		try{
			//Call the UserKernel.getPages method to allocate free pages
			pageTable = ((UserKernel)Kernel.kernel).getPages(numPages);
		}
		//Catch the exception if there aren't enough pages to satisfy the request
		catch(InsufficientFreePagesException e){
			//Close the file and return false
			coff.close();
			return false;
		}

		//Populate the pageTable's vpn with numbers from 0 to length-1
		for(int i = 0; i < pageTable.length; i++)
			pageTable[i].vpn = i;

		//Iterate through each section
		for (int s=0; s<coff.getNumSections(); s++) {
			//Get the section
			CoffSection section = coff.getSection(s);

			Lib.debug(dbgProcess, "\tinitializing "
			+ section.getName() +
			" section (" + section.getLength() + " pages)");

			//Load the pages
			for (int i=0; i<section.getLength(); i++) {
				int vpn = section.getFirstVPN()+i;
				section.loadPage(i, pageTable[vpn].ppn);
			}
		}

		//Return true since the operation was successful
		return true;
    }

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
    ((UserKernel)Kernel.kernel).releasePageTable(pageTable);
	}

	/**
	 * Initialize the processor's registers in preparation for running the
	 * program loaded into this process. Set the PC register to point at the
	 * start function, set the stack pointer register to point at the top of
	 * the stack, set the A0 and A1 registers to argc and argv, respectively,
	 * and initialize all other registers to 0.
	 */
	public void initRegisters() {
		Processor processor = Machine.processor();

		// by default, everything's 0
		for (int i=0; i<processor.numUserRegisters; i++)
			processor.writeRegister(i, 0);

		// initialize PC and SP according
		processor.writeRegister(Processor.regPC, initialPC);
		processor.writeRegister(Processor.regSP, initialSP);

		// initialize the first two argument registers to argc and argv
		processor.writeRegister(Processor.regA0, argc);
		processor.writeRegister(Processor.regA1, argv);
	}

	/**
	 * Handle the halt() system call.
	 */
	private int handleHalt() {

		Machine.halt();

		Lib.assertNotReached("Machine.halt() did not halt machine!");
		return 0;
	}
  
  public void selfTest(){

		byte[] data = {'I','T',' ','W','O','R','K'};
		byte[] buffer = new byte[7];
		
		//Write to memory, then read the same section
		//What was read should be what was written
		int bytesWritten = writeVirtualMemory(0, data, 0, 7);
		int bytesRead = readVirtualMemory(0,buffer,0,7);

		String message = new String(buffer);
		System.out.println("Read Write Test: " + message);

		//Write more than a pages worth of bytes to memory
		byte[] overFlow = new byte[pageSize + 4];

		for(int i = 0; i < pageSize; ++i)
			overFlow[i] = (byte)(i%255);

		overFlow[pageSize] = 'D';
		overFlow[pageSize+1] = 'o';
		overFlow[pageSize+2] = 'g';
		overFlow[pageSize+3] = 's';

		bytesWritten = writeVirtualMemory(0, overFlow,0, overFlow.length);

		System.out.println("Bytes Written: " + bytesWritten);
		System.out.println("Write OverFlow Test: Dogs");

		for(int i = 0; i < overFlow.length; ++i)
			overFlow[i] = 0;

		//Read more than a pages worth of bytes from memory
		bytesRead = readVirtualMemory(0,overFlow,0,overFlow.length);

		byte[] last4 = new byte[4];
		last4[0] = overFlow[pageSize];
		last4[1] = overFlow[pageSize+1];
		last4[2] = overFlow[pageSize+2];
		last4[3] = overFlow[pageSize+3];
		
		System.out.println("Bytes Read: " + bytesRead);
		System.out.println("Read OverFlow Test: " + new String(last4));

		for(int i = 0; i < last4.length; ++i)
			last4[i] = 0;

		//Read the first 4 bytes of vpn 1, should read GOOD		
		bytesRead = readVirtualMemory(pageSize, last4, 0, last4.length);
		System.out.println("OverFlow Test: " + new String(last4));
	}


	private static final int
			syscallHalt = 0,
			syscallExit = 1,
			syscallExec = 2,
			syscallJoin = 3,
			syscallCreate = 4,
			syscallOpen = 5,
			syscallRead = 6,
			syscallWrite = 7,
			syscallClose = 8,
			syscallUnlink = 9;

	/**
	 * Handle a syscall exception. Called by <tt>handleException()</tt>. The
	 * <i>syscall</i> argument identifies which syscall the user executed:
	 *
	 * <table>
	 * <tr><td>syscall#</td><td>syscall prototype</td></tr>
	 * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
	 * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
	 * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
	 * 								</tt></td></tr>
	 * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
	 * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
	 * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
	 * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
	 *								</tt></td></tr>
	 * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
	 *								</tt></td></tr>
	 * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
	 * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
	 * </table>
	 *
	 * @param	syscall	the syscall number.
	 * @param	a0	the first syscall argument.
	 * @param	a1	the second syscall argument.
	 * @param	a2	the third syscall argument.
	 * @param	a3	the fourth syscall argument.
	 * @return	the value to be returned to the user.
	 */
	public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
		switch (syscall) {
			case syscallHalt:
				return handleHalt();
			case syscallCreate:
				return handleCreate(a0);
			case syscallOpen:
				return handleOpen(a0);
			case syscallRead:
				return handleRead(a0, a1, a2);


			default:
				Lib.debug(dbgProcess, "Unknown syscall " + syscall);
				Lib.assertNotReached("Unknown system call!");
		}
		return 0;
	}

	/**
	 * Method to handle the open system call. Uses the fileOpen method
	 */
	private int handleOpen(int fileNamePointer) {
		return fileOpen(fileNamePointer, false);
	}

	/**
	 * Method to handle the create system call. Uses the fileOpen method
	 */
	private int handleCreate(int fileNamePointer) {
		return fileOpen(fileNamePointer, true);
	}

	private int fileOpen(int fileNamePointer, boolean isCreate) {
		//Validate the file name address and retrieve the file name
		addressChecker(fileNamePointer);
		String fileName = readVirtualMemoryString(fileNamePointer, 256);
		int globalFileIndex = -1;
		int nullIndex = -1;

		//Find file or free space within Global File Table
		for(int index = 2; index < globalFileTable.length; ++index){
			//Store the location of the file and the last null index
			if(globalFileTable[index] == null)
				nullIndex = index;
			else
			if(globalFileTable[index].fileName.equals(fileName))
				globalFileIndex = index;

		}

		//Global Table holds file with same name
		if(globalFileIndex != -1){
			//file must not be marked to unlink
			if(!globalFileTable[globalFileIndex].markedForDeath)
				//increase referances by 1
				++globalFileTable[globalFileIndex].numReferences;
		}else{
			if(nullIndex == -1)
				//There is no space within the global file table
				return -1;
		}

		//Find free space within Local File Table
		int localFileIndex = -1;
		for(int index = 0; index < localFileTable.length; ++index)
			if(localFileTable[index] == null)
				localFileIndex = index;
		if(localFileIndex == -1)
			return -1;
		localFileTable[localFileIndex] = UserKernel.fileSystem.open(fileName, isCreate);
		//ensure file was properly opened
		//and add to global file table if needed
		if(localFileTable[localFileIndex] != null){
			if(globalFileIndex == -1)
				globalFileTable[nullIndex] = new FileReference(fileName);
			return localFileIndex;
		}else
			//file wasn't opened successfully
			return -1;

	}

	public int handleRead(int fileIndex, int bufferPointer, int size){
		addressChecker(bufferPointer);
		//Ensure valid index and that a file exists at that index in table
		if(fileIndex < 0 || fileIndex > 15 || localFileTable[fileIndex] == null)
			return -1;

		byte[] storage = new byte[size];
		//read the data from file into storage
		int bytesRead = localFileTable[fileIndex].read(storage, 0, size);
		if(bytesRead == -1)
			return -1;
		//write the storage to buffer
		int bytesWritten = writeVirtualMemory(bufferPointer, storage, 0, bytesRead);
		if(bytesWritten != bytesRead)
			return -1;

		return bytesRead;
	}

	/**
	 * Ensure that a memory address is valid. The address must be within defined memory.
	 * If it is is an invalid location handleExit is called with an exit code -1.
	 * @param address the address to be verified
	 */
	private void addressChecker(int address) {
		int pageNum = Processor.pageFromAddress(address);
		if(pageNum >= numPages || pageNum < 0)
			handleExit(-1);
	}

	/**
	 * Handles the exit system call by acquiring the joinLock. Notifies the parent that the child is going to exit
	 * if the parent exists to avoid it from joining later. Will disown each child by making the child's parent null.
	 * It then opens the files and closes them to release any referances to them using the handleClose method and wake
	 * up any processes that may be assleep using waitingToJoin condition. unloadSections is then used to release the
	 * virtual memory, finally releasing the lock
	 * @param i
	 */
	private void handleExit(int i) {
	}

	/**
	 * Handle a user exception. Called by
	 * <tt>UserKernel.exceptionHandler()</tt>. The
	 * <i>cause</i> argument identifies which exception occurred; see the
	 * <tt>Processor.exceptionZZZ</tt> constants.
	 *
	 * @param	cause	the user exception that occurred.
	 */
	public void handleException(int cause) {
		Processor processor = Machine.processor();

		switch (cause) {
			case Processor.exceptionSyscall:
				int result = handleSyscall(processor.readRegister(Processor.regV0),
						processor.readRegister(Processor.regA0),
						processor.readRegister(Processor.regA1),
						processor.readRegister(Processor.regA2),
						processor.readRegister(Processor.regA3)
				);
				processor.writeRegister(Processor.regV0, result);
				processor.advancePC();
				break;

			default:
				Lib.debug(dbgProcess, "Unexpected exception: " +
						Processor.exceptionNames[cause]);
				Lib.assertNotReached("Unexpected exception");
		}
	}

	/** The program being run by this process. */
	protected Coff coff;

	/** This process's page table. */
	protected TranslationEntry[] pageTable;
	/** The number of contiguous pages occupied by the program. */
	protected int numPages;

	/** The number of pages in the program's stack. */
	protected final int stackPages = 8;

	private int initialPC, initialSP;
	private int argc, argv;

	private static final int pageSize = Processor.pageSize;
	private static final char dbgProcess = 'a';

	//Task 1 Variables
	private static int numProcesses = 0;
	public int processID;

	OpenFile[] localFileTable;
	static FileReference[] globalFileTable;

	public class FileReference{
		//integer tracking the number of references to the file
		public int numReferences;

		//boolean to check if the file is set to be unlinked
		public boolean markedForDeath;

		//String storing the name of hte file
		public String fileName;

		public FileReference(String inFileName){
			//A created file must start with a reference
			numReferences = 1;
			markedForDeath = false;
			fileName = inFileName;
		}
  	}

    /** The program being run by this process. */
    protected Coff coff;

    /** This process's page table. */
    protected TranslationEntry[] pageTable;
    /** The number of contiguous pages occupied by the program. */
    protected int numPages;

    /** The number of pages in the program's stack. */
    protected final int stackPages = 8;
    
    private int initialPC, initialSP;
    private int argc, argv;
	
    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';

    Lock lock;


}