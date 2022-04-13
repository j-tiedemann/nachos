package nachos.userprog;

import java.util.LinkedList;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

/**
 * A kernel that can support multiple user processes.
 */
public class UserKernel extends ThreadedKernel {

    public LinkedList<TranslationEntry> freePage;
    public Lock lock;

    /**
     * Allocate a new user kernel.
     */
    public UserKernel() {
        super();

        freePage= new LinkedList<TranslationEntry>();

	
    }

    /**
     * Initialize this kernel. Creates a synchronized console and sets the
     * processor's exception handler.
     */
    public void initialize(String[] args) {
	super.initialize(args);

	console = new SynchConsole(Machine.console());
	
	Machine.processor().setExceptionHandler(new Runnable() {
		public void run() { exceptionHandler(); }
	    });

        //get number of pages available
        int pageNum = Machine.processor().getNumPhysPages();
        for(int i=0; i<pageNum; ++i)
        	freePage.add(new TranslationEntry(0, i, false, false, false, false));
		
		//Initialize the pageLock
		lock = new Lock();
    }

    /**
     * Test the console device.
     */	
    public void selfTest() {
	super.selfTest();

	UserProcess userProcessTest = null;	
	if(Lib.test(vmTestChar)){
		userProcessTest = new UserProcess();
		userProcessTest.selfTest();
	}

	System.out.println("Testing the console device. Typed characters");
	System.out.println("will be echoed until q is typed.");

	char c;

	do {
	    c = (char) console.readByte(true);
	    console.writeByte(c);
	}
	while (c != 'q');

	System.out.println("");
    }

    /**
     * Returns the current process.
     *
     * @return	the current process, or <tt>null</tt> if no process is current.
     */
    public static UserProcess currentProcess() {
	if (!(KThread.currentThread() instanceof UThread))
	    return null;
	
	return ((UThread) KThread.currentThread()).process;
    }

    /**
     * The exception handler. This handler is called by the processor whenever
     * a user instruction causes a processor exception.
     *
     * <p>
     * When the exception handler is invoked, interrupts are enabled, and the
     * processor's cause register contains an integer identifying the cause of
     * the exception (see the <tt>exceptionZZZ</tt> constants in the
     * <tt>Processor</tt> class). If the exception involves a bad virtual
     * address (e.g. page fault, TLB miss, read-only, bus error, or address
     * error), the processor's BadVAddr register identifies the virtual address
     * that caused the exception.
     */
    public void exceptionHandler() {
	Lib.assertTrue(KThread.currentThread() instanceof UThread);

	UserProcess process = ((UThread) KThread.currentThread()).process;
	int cause = Machine.processor().readRegister(Processor.regCause);
	process.handleException(cause);
    }

    /**
     * Start running user programs, by creating a process and running a shell
     * program in it. The name of the shell program it must run is returned by
     * <tt>Machine.getShellProgramName()</tt>.
     *
     * @see	nachos.machine.Machine#getShellProgramName
     */
    public void run() {
	super.run();

	UserProcess process = UserProcess.newUserProcess();
	
	String shellProgram = Machine.getShellProgramName();	
	Lib.assertTrue(process.execute(shellProgram, new String[] { }));

	KThread.currentThread().finish();
    }

    /**
     * Terminate this kernel. Never returns.
     */
    public void terminate() {
	super.terminate();
    }

    public TranslationEntry[] getPages(int amount) throws InsufficientFreePagesException{
    	//Acquire the lock
    	lock.acquire();
    	
    	//If there are enough pages left to accomodate the request
    	if(!freePage.isEmpty() && freePage.size() >= amount){
    		//Create a new array of the requested pages
    		TranslationEntry[] requestedPages = new TranslationEntry[amount];
    		
    		//Cycle through the pages to remove them from the free pages and validate them
    		for(int i=0; i<requestedPages.length; ++i){
    			requestedPages[i] = freePage.remove();
    			requestedPages[i].valid = true;
    		}
    		
    		//Release the lock and return the requested pages
    		lock.release();
    		return requestedPages;
    	}
    	//If there are not enough pages left to accomodate the reques
    	else{
    		//Release the lock and throw an InsufficientFreePagesException
    		lock.release();
    		throw new InsufficientFreePagesException();
    	}
    }

    public void releasePageTable(TranslationEntry[] pageTable){
    	lock.acquire();
    	
    	for(int i=0; i<pageTable.length; ++i){
    		pageTable[i].valid = false;
    		freePage.add(pageTable[i]);
    	}
    	
    	lock.release();
    }
    
    
    public class InsufficientFreePagesException extends Exception{}
    /** Globally accessible reference to the synchronized console. */
    public static SynchConsole console;

    // dummy variables to make javac smarter
    private static Coff dummy1 = null;
    
    public static final char vmTestChar = 'j';
}
