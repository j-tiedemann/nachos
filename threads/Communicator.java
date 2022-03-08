package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    
    private static final char CommunicatorTestChar = 'c';
    private int message;
    private Boolean wordSpoken;
    private Condition speaker;
    private Condition listener;
    private Lock lock;
    private int numListners;
    /**
     * Allocate a new communicator.
     */
    public Communicator() {

        lock = new Lock();
        speaker = new Condition(lock);
        listener = new Condition(lock);
        wordSpoken = true;
        numListners = 0;
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {
        lock.acquire();

        while(numListners == 0 || (!wordSpoken)) {
            speaker.sleep();
    
        }

        wordSpoken = false;
        message = word;
        listener.wake();
        lock.release();
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    public int listen() {
        lock.acquire();

        numListners++;
        speaker.wake();

        listener.sleep();

        int wordTransfer = message;
        wordSpoken = true;

        numListners--;
        lock.release();
        return wordTransfer;

    }

    public static void selfTest(){
        Lib.debug(CommunicatorTestChar, "Comuincator test case");

		basicTest();
        testTest();
    
    }

    public static void basicTest() {
        final Communicator cTest = new Communicator();

		//Test case 1
		KThread test1a = new KThread(new Runnable(){
			public void run(){
				Lib.debug(CommunicatorTestChar, "Test Case 1: basic test one speaker and lisitner.");
				Lib.debug(CommunicatorTestChar, "Sending message 15");
				cTest.speak(15);
			}
		});

		test1a.fork();

		KThread test1b = new KThread(new Runnable(){
			public void run(){
				int result = cTest.listen();
				Lib.debug(CommunicatorTestChar, "" + result);
				if(result == 15)
					Lib.debug(CommunicatorTestChar, "Test 1 Successful! Message " + result + " received.\n");
				else
					Lib.debug(CommunicatorTestChar, "Test 1 Failed.\n");
			}
		});

		test1b.fork();
		test1b.join();
		test1a.join();
    }
    
    public static void testTest(){
        final Communicator cTest = new Communicator();
      
		//Test case 1
		KThread test1a = new KThread(new Runnable(){
			public void run(){
                System.out.println("Test Case 1: basic test one speaker and lisitner.");
                System.out.println("Sending message 15");
				cTest.speak(15);
			}
		});

		test1a.fork();

		KThread test1b = new KThread(new Runnable(){
			public void run(){
				int result = cTest.listen();
                System.out.println(result);
				if(result == 15)
                    System.out.println("Test 1 Successful! Message " + result + " received.\n");
				else
                    System.out.println("Test 1 Failed.\n");
			}
		});

		test1b.fork();
		test1b.join();
		test1a.join();
    }
}
