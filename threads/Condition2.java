package nachos.threads;

import java.util.LinkedList;

import nachos.machine.Lib;
import nachos.machine.Machine;

public class Condition2 {

  private static final char dbgC2 = 'f';
  private Lock conditionLock;
  private LinkedList<KThread> waitList;
  
	public Condition2(Lock conditionLock) {
		this.conditionLock = conditionLock;
		Lib.debug(dbgC2, Machine.timer().getTime() + "Cnd2 start");
		this.waitList = new LinkedList<KThread>();
		Lib.debug(dbgC2, Machine.timer().getTime() + "Cnd2 end");
		}

	//Threads and interrupts must be suspended to maintain atomicity
	//Lock disabled during operation to prevent thread from being //usable by concurrent method (this is also done for wake()) 
	//sleep() method adds threads to our waitList for future use 
		
	public void sleep() {
		Lib.debug(dbgC2, Machine.timer().getTime() + "Cnd2 sleep start");
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		//Lib.assertTrue(KThread.currentThread().status != statusFinished);
		
		
		waitList.add(KThread.currentThread());
		boolean intSatus = Machine.interrupt().disable(); 
		Lib.debug(dbgC2, Machine.timer().getTime() + "Cnd2 sleep: interrupts disabled");
		conditionLock.release();
		Lib.debug(dbgC2, Machine.timer().getTime() + "Cnd2 sleep: lock released");
		KThread.currentThread().sleep();
		Lib.debug(dbgC2, Machine.timer().getTime() + "Cnd2 lock slept");
		conditionLock.acquire();
		Lib.debug(dbgC2, Machine.timer().getTime() + "Cnd2 sleep: lock acquired");
		Machine.interrupt().restore(intSatus);
		Lib.debug(dbgC2, Machine.timer().getTime() + "Cnd2 sleep: interrupts enabled");
	   }

	//wake() uses a waker thread to pull a thread out of the waitList //for use
	public void wake() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		boolean intSatus = Machine.interrupt().disable();
		if (waitList.peek() != null) {
			
			Lib.debug(dbgC2, Machine.timer().getTime() + "Cnd2 wake: interrupts disabled");
			try {
				((KThread)waitList.poll()).ready();
			}
			catch(Exception e) {
				
			}
			
			
		}
		
		Machine.interrupt().restore(intSatus);
		Lib.debug(dbgC2, Machine.timer().getTime() + "Cnd2 wake: interrupts disabled");
		}
	public void wakeAll() {
			Lib.assertTrue(conditionLock.isHeldByCurrentThread());
			while (!waitList.isEmpty()) {
				wake();
	}
		
	}
	
	
	public static void selfTest()
	{
		final Lock sTLock = new Lock();
		final Condition2 sTCond = new Condition2(sTLock);
		KThread sleepThread1 = new KThread(new Runnable(){
			public void run(){
				sTLock.acquire();
				System.out.println("sleepThread1 Lock acquired. Commencing Condition2 test cases...");
				sTCond.sleep();
				System.out.println("sleepThread1: slept");
				System.out.println("sleepThread1: test complete");
				sTLock.release();
				System.out.println("sleepThread1: Lock released.");
		}});
		sleepThread1.setName("Sleep Test 1");
			System.out.println("Forking sleepThread 1");
			sleepThread1.fork();
			KThread sleepThread2 = new KThread(new Runnable(){
				
				public void run() {
					sTLock.acquire();
					System.out.println("sleepThread2 Lock acquired. Sleeping...");
					sTCond.sleep();
					
					System.out.println("sleepThread2: slept");
					System.out.println("sleepThread2: test complete");
					sTLock.release();
					System.out.println("sleepThread2: Lock released.");
	
				}
			});
			
			sleepThread2.setName("Sleep Test 2");
			System.out.println("Forking sleepThread 2");
			sleepThread2.fork();
				
			KThread sleepThread3 = new KThread(new Runnable(){
				
							
			public void run() {
				sTLock.acquire();
				System.out.println("sleepThread3 Lock acquired. Sleeping...");
				sTCond.sleep();
					
				System.out.println("sleepThread3 slept");
				System.out.println("sleepThread3: test complete");
				sTLock.release();
				System.out.println("sleepThread3: Lock released.");
					
				}
			});
		
			sleepThread3.setName("Wake test 1");
				System.out.println("Forking sleepThread3");
				sleepThread3.fork();
				
				KThread wakeThread1 = new KThread(new Runnable(){
					
					public void run() {
					sTLock.acquire();
					System.out.println("wakeThread1 Lock acquired. Waking...");
					sTCond.wake();
					
					System.out.println("wakeThread1 woken");
					System.out.println("wakeThread1: test complete");
					sTLock.release();
					System.out.println("wakeThread1: Lock released.");
					
				}
				});
				wakeThread1.setName("Wake Test 2");
			System.out.println("Forking wakeThread1");
				wakeThread1.fork();
				
				KThread wakeThread2 = new KThread(new Runnable(){
					
					public void run() {
					sTLock.acquire();
					System.out.println("wakeThread2 Lock acquired. Waking all...");
					sTCond.wakeAll();
					
					System.out.println("wakeThread2 woken all");
					System.out.println("wakeThread2 test complete");
					sTLock.release();
					System.out.println("wakeThread2 Lock released.");
					}
				});
				wakeThread2.setName("Wake Test 3");
			System.out.println("Forking wakeThread1");
			wakeThread2.fork();
				
			KThread wakeThread3 = new KThread(new Runnable(){
					
					public void run() {
					sTLock.acquire();
					System.out.println("wakeThread2 Lock acquired. Waking all with no wakeable threads...");
					sTCond.wakeAll();
					System.out.println("No threads to wake");
					System.out.println("wakeThread2 test complete");
					sTLock.release();
					System.out.println("wakeThread2 Lock released.");
				}
			});
	}
}
	


				
					
				
