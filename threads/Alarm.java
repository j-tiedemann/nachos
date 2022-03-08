package nachos.threads;

import nachos.machine.*;
import java.util.PriorityQueue;
import nachos.threads.KThread;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
	Machine.timer().setInterruptHandler(new Runnable() {
		public void run() { timerInterrupt(); }
	    });
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {
        //timerInterrupt Data Fields
        long currentTime = Machine.timer().getTime();
        boolean interrupt = Machine.interrupt().disable();

        //check the PriorityQueue for waitThreads that have a finished wait time and ready them
        while(!waitQueue.isEmpty() && (waitQueue.peek().getWakeTime() <= currentTime)){
            waitQueue.poll().getWaitThread().ready();
        }

        Machine.interrupt().restore(interrupt)
        KThread.yield();
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long waitTime) {
        //store the interrupt status
        boolean interrupt = Machine.interrupt().disable();
        //calculate the time the thread needs to wake
        long wakeTime = Machine.timer().getTime() + waitTime;
        //use custom waitThread object to store KThread and calculated wakeTime
        waitThread waiter = new waitThread(KThread.currentThread(), wakeTime);

        waitQueue.add(waiter);
        KThread.sleep();

        Machine.interrupt().restore(interrupt);
    }

    private class waitThread implement Comparable<waitThread>{
        //waitThread data fields
        private long wakeTime;
        private KThread waitThread;

        //waitThread class constructor
        public waitThread(KThread thread, long time){
            this.waitThread = thread;
            this.wakeTime = time;
        }

        //wakeTime accessor
        public long getWakeTime(){
            return wakeTime;
        }

        //waitThread accessor
        public Kthread getWaitThread(){
            return waitThread;
        }

        //Comparable Interface Implementation
        public int compareTo(waitThread otherThread){
            if(wakeTime > other.wakeTime)
                return 1;
            else if(wakeTime == other.wakeTime)
                return 0;
            else
                return -1;
        }
    }

    //Priority queue object implementation using the custom object waitThread to store and sort threads according
    //to their timeToWait
    private PriorityQueue<waitThread> waitQueue = new PriorityQueue<waitThread>();

    /**
     * selfTest() method tests to make sure that threads are put to sleep and awoken in the proper order. The order
     * is determined by the timeToWait rather than the order in which the threads were put to sleep.
     */
    private static final char AlarmTestChar = 'a';
    public static void selftest(){
        Lib.debug(AlarmTestChar, "Alarm.selfTest(): Starting self test.");

        //Tests go here
        Lib.debug(AlarmTestChar, "Alarm.selfTest(): Finished selfTest(), passed.");
    }
}
