package nachos.threads;
import java.util.Timer;

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
        wordSpoken = false;
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

        while(numListners == 0 || wordSpoken) {
            speaker.sleep();
    
        }

        wordSpoken = true;
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
        wordSpoken = false;

        numListners--;
        speaker.wake();
        lock.release();
        return wordTransfer;

    }

    public static void selfTest(){
        Lib.debug(CommunicatorTestChar, "-----------------------");
        Lib.debug(CommunicatorTestChar, "Comuincator test case");

		basicTest();
        multipleSpeakers();
        multipleListeners();
        performanceTest();

        Lib.debug(CommunicatorTestChar, "-----------------------");
    
    }

    public static void basicTest() {
        final Communicator comTest = new Communicator();

		//Test case 1
		KThread speakerThread = new KThread(new Runnable(){
			public void run(){
				Lib.debug(CommunicatorTestChar, "Test Case 1: basic test one speaker and lisitner.");
				Lib.debug(CommunicatorTestChar, "Sending message 15");
				comTest.speak(15);
			}
		});

		speakerThread.fork();

		KThread listenThread = new KThread(new Runnable(){
			public void run(){
				int result = comTest.listen();
				Lib.debug(CommunicatorTestChar, "" + result);
				if(result == 15)
					Lib.debug(CommunicatorTestChar, "Test 1 Successful! Message " + result + " received.\n");
				else
					Lib.debug(CommunicatorTestChar, "Test 1 Failed.\n");
			}
		});

		listenThread.fork();
		listenThread.join();
		speakerThread.join();
		Lib.debug(CommunicatorTestChar, "-----------------------");
    }
    
    public static void multipleSpeakers() {

        Lib.debug(CommunicatorTestChar, "Test 2: multiple speakers ");
    	//Create a Communicator object
    	final Communicator multiSpeakerCom = new Communicator();
       
    	
    	//Create 5 speakers that each will speak a word (1-5) when they are forked
    	KThread speaker1 = new KThread(new Runnable(){
			public void run(){
				Lib.debug(CommunicatorTestChar, "Speaker 1: sending 1");
				multiSpeakerCom.speak(1);
			}
		});

        KThread speaker2 = new KThread(new Runnable(){
			public void run(){
				Lib.debug(CommunicatorTestChar, "Speaker 2: sending 2");
				multiSpeakerCom.speak(2);
			}
		});

        KThread speaker3 = new KThread(new Runnable(){
			public void run(){
				Lib.debug(CommunicatorTestChar, "Speaker 1: sending 3");
				multiSpeakerCom.speak(3);
			}
		});
    	
        KThread listen1 = new KThread(new Runnable(){
			public void run(){
				Lib.debug(CommunicatorTestChar, "listen 1: recived:" + multiSpeakerCom.listen());
			}
		});

        KThread listen2 = new KThread(new Runnable(){
			public void run(){
				Lib.debug(CommunicatorTestChar, "listen 2: recived:" + multiSpeakerCom.listen());
			}
		});

        KThread listen3 = new KThread(new Runnable(){
			public void run(){
				Lib.debug(CommunicatorTestChar, "listen 3: recived:" + multiSpeakerCom.listen());
			}
		});

        speaker1.fork();
        speaker2.fork();
        speaker3.fork();
        listen1.fork();
        listen2.fork();
        listen3.fork();

        speaker1.join();
        speaker2.join();
        speaker3.join();
     
        Lib.debug(CommunicatorTestChar, "Test 2 Successful");
        Lib.debug(CommunicatorTestChar, "-----------------------");

    }
    
    public static void multipleListeners() {

        Lib.debug(CommunicatorTestChar, "Test 3: multiple listners ");

        final Communicator multiSpeakerCom = new Communicator();

        KThread listen1 = new KThread(new Runnable(){
			public void run(){
				Lib.debug(CommunicatorTestChar, "listener 1: ready to listen ");
    			Lib.debug(CommunicatorTestChar, "listener1 recived word: " 
    												+ multiSpeakerCom.listen() + ".");
			}
		});

        KThread listen2 = new KThread(new Runnable(){
			public void run(){
				Lib.debug(CommunicatorTestChar, "listener 2: ready to listen ");
    			Lib.debug(CommunicatorTestChar, "listener2 recived word: " 
    												+ multiSpeakerCom.listen() + ".");
			}
		});

        KThread listen3 = new KThread(new Runnable(){
			public void run(){
				Lib.debug(CommunicatorTestChar, "listener 2: ready to listen ");
    			Lib.debug(CommunicatorTestChar, "listener2 recived word: " 
    												+ multiSpeakerCom.listen() + ".");
			}
		});


        KThread speaker1 = new KThread(new Runnable(){
			public void run(){
				Lib.debug(CommunicatorTestChar, "Speaker 1: sending 1");
				multiSpeakerCom.speak(1);
			}
		});

        KThread speaker2 = new KThread(new Runnable(){
			public void run(){
				Lib.debug(CommunicatorTestChar, "Speaker 2: sending 2");
				multiSpeakerCom.speak(2);
			}
		});

        KThread speaker3 = new KThread(new Runnable(){
			public void run(){
				Lib.debug(CommunicatorTestChar, "Speaker 1: sending 3");
				multiSpeakerCom.speak(3);
			}
		});

        listen1.fork();
        listen2.fork();
        listen3.fork();
        speaker1.fork();
        speaker2.fork();
        speaker3.fork();
        
        listen1.join();
     
        listen2.join();
        
        listen3.join();
        Lib.debug(CommunicatorTestChar, "Test 3 Successful");
        Lib.debug(CommunicatorTestChar, "-----------------------");

    }
    public static void performanceTest() {
    	
    	long start = System.currentTimeMillis();
    	
        Lib.debug(CommunicatorTestChar, "Test 4: Performance test ");
        Lib.debug(CommunicatorTestChar, "Starting timer");
   
    	//Create a Communicator object
    	final Communicator multiSpeakerCom = new Communicator();
        
    	
    	//Create 5 speakers that each will speak a word (1-5) when they are forked
    	KThread speaker1 = new KThread(new Runnable(){
			public void run(){
				Lib.debug(CommunicatorTestChar, "Speaker 1: sending 1");
				multiSpeakerCom.speak(1);
			}
		});

        KThread speaker2 = new KThread(new Runnable(){
			public void run(){
				Lib.debug(CommunicatorTestChar, "Speaker 2: sending 2");
				multiSpeakerCom.speak(2);
			}
		});

        KThread speaker3 = new KThread(new Runnable(){
			public void run(){
				Lib.debug(CommunicatorTestChar, "Speaker 1: sending 3");
				multiSpeakerCom.speak(3);
			}
		});


        KThread listner = new KThread(new Runnable(){
			public void run(){
				for(int i = 0; i < 3; i++) {
                    Lib.debug(CommunicatorTestChar, "listner recived word: " 
    												+ multiSpeakerCom.listen() + ".");
                }

			}
		});


        speaker1.fork();
        speaker2.fork();
        speaker3.fork();
        listner.fork();
        
        listner.join();
        Lib.debug(CommunicatorTestChar, "timer ending");
        long end = System.currentTimeMillis();
     
        
        long elapsedTime = end - start;
        Lib.debug(CommunicatorTestChar, "Test took: " + Long.toString(elapsedTime) + " millisecond");
    }
}
