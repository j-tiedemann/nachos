package nachos.threads;


import java.util.concurrent.TimeUnit;

import nachos.machine.*;

public class ReactWater{

    private static final char ReactWaterTestChar = 'r';
    private Lock lock; 
    private Condition hydrogen; 
    private Condition oxygen;
    private int numHydrogen;
    private int numOxygen;
    private Boolean madeWater;
    /** 
     *   Constructor of ReactWater
     **/
    public ReactWater() {
        lock = new Lock();
        hydrogen = new Condition(lock);
        oxygen = new Condition(lock);
        numHydrogen = 0;
        numOxygen = 0;
        madeWater = false;

    } // end of ReactWater()

    /** 
     *   When H element comes, if there already exist another H element 
     *   and an O element, then call the method of Makewater(). Or let 
     *   H element wait in line. 
     **/ 
    public void hReady() {
        lock.acquire(); //Atomically acquire the lock
        ++numHydrogen;

        while(numHydrogen < 2 || numOxygen < 1){
        //Test madeWater flag, flip if true and release the lock
            if(madeWater){
                madeWater = false;
                lock.release();
                return;
            } else //otherwise sleep the hydrogen thread
                hydrogen.sleep();
            }
        //We have enough atom threads
        makeWater();
        oxygen.wake();
        lock.release();
    
    } // end of hReady()
 
    /** 
     *   When O element comes, if there already exist another two H
     *   elements, then call the method of Makewater(). Or let O element
     *   wait in line. 
     **/ 
    public void oReady() {

        lock.acquire();
	    ++numOxygen;
	    //Waking a hydrogen atom will initiate a check for
	    //the number of readied atoms
	    hydrogen.wake();
	    oxygen.sleep();
	    //when water is made the oxygen atom will wake the second
	    //hydrogen atom
	    hydrogen.wake();
	    lock.release();
    } // end of oReady()
    
    /** 
     *   Print out the message of "water was made!".
     **/
    public void makeWater() {
        numHydrogen -= 2;
        --numOxygen;
        madeWater = true;
     //   System.out.println("Water was made!");
        Lib.debug(ReactWaterTestChar, "Water was made");
        

    } // end of Makewater()

    public void terminate() {
    	lock.acquire();
    	//hydrogen.wake();
		lock.release();
    }

    public static void selfTest() {
        Lib.debug(ReactWaterTestChar, "------------------");
        Lib.debug(ReactWaterTestChar, "Testing for react water");

        basicTest();
        manyWater();
        noWater();

        Lib.debug(ReactWaterTestChar, "------------------");
      }

    public static void basicTest() {

        Lib.debug(ReactWaterTestChar, "Test Case 1: Basic test");
        final ReactWater MotherNature = new ReactWater();


        KThread h1 = new KThread(new Runnable(){
			public void run(){
				Lib.debug(ReactWaterTestChar, "hydrogen 1 ready");
				MotherNature.hReady();
			}
		});

        KThread h2 = new KThread(new Runnable(){
			public void run(){
				Lib.debug(ReactWaterTestChar, "hydrogen 2 ready");
				MotherNature.hReady();
			}
		});

        KThread o1 = new KThread(new Runnable(){
			public void run(){
				Lib.debug(ReactWaterTestChar, "oyygen1 ready ");
				MotherNature.oReady();
			}
		});

        h1.fork();
        h2.fork();
        o1.fork();
        h1.join();
        Lib.debug(ReactWaterTestChar, "Test case 1 successful ");
        Lib.debug(ReactWaterTestChar, "------------------");
    }

    public static void noWater() {
    	
    	Lib.debug(ReactWaterTestChar, "Test Case 2: 1o and 1h");
        final ReactWater MotherNature = new ReactWater();
        KThread h1 = new KThread(new Runnable(){
			public void run(){
				Lib.debug(ReactWaterTestChar, "hydrogen 1 ready");
				MotherNature.hReady();
				KThread.finish();
			}
		});

        KThread o1 = new KThread(new Runnable(){
			public void run(){
				Lib.debug(ReactWaterTestChar, "oyygen1 ready ");
				MotherNature.oReady();
				KThread.finish();
			}
		});
        
        KThread test = new KThread(new Runnable(){
			public void run(){
		
				//MotherNature.terminate();
			}
		});

        h1.fork();
        
        o1.fork();
        test.fork();
       
     
        test.join();
       
        Lib.debug(ReactWaterTestChar, "No water made");
        Lib.debug(ReactWaterTestChar, "Test case 3 successful ");
        
        
    }
    
    public static void manyWater() {
    	Lib.debug(ReactWaterTestChar, "Test Case 3: Many hydrogen ");
        final ReactWater MotherNature = new ReactWater();
        KThread h1 = new KThread(new Runnable(){
			public void run(){
				Lib.debug(ReactWaterTestChar, "hydrogen 1 ready");
				MotherNature.hReady();
			}
		});

        KThread h2 = new KThread(new Runnable(){
			public void run(){
				Lib.debug(ReactWaterTestChar, "hydrogen 2 ready");
				MotherNature.hReady();
			}
		});

        KThread h3 = new KThread(new Runnable(){
			public void run(){
				Lib.debug(ReactWaterTestChar, "hydrogen 2 ready");
				MotherNature.hReady();
			}
		});

        KThread h4 = new KThread(new Runnable(){
			public void run(){
				Lib.debug(ReactWaterTestChar, "hydrogen 2 ready");
				MotherNature.hReady();
			}
		});

        KThread o1 = new KThread(new Runnable(){
			public void run(){
				Lib.debug(ReactWaterTestChar, "oyygen1 ready ");
				MotherNature.oReady();
			}
		});
    	
        h1.fork();
        h2.fork();
        h3.fork();
        h4.fork();
        o1.fork();
        o1.join();
        Lib.debug(ReactWaterTestChar, "Test case 2 successful ");
        Lib.debug(ReactWaterTestChar, "------------------");
    }
    

} // end of class ReactWater
 
