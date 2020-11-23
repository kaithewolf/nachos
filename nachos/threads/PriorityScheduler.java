package nachos.threads;

import nachos.machine.*;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Comparator;
/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
    /**
     * Allocate a new priority scheduler.
     */
    public PriorityScheduler() {
    }
    
    /**
     * Allocate a new priority thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer priority from waiting threads
     *					to the owning thread.
     * @return	a new priority thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
	return new PriorityQueue(transferPriority);
    }

    public int getPriority(KThread thread) {
	Lib.assertTrue(Machine.interrupt().disabled());
		       
	return getThreadState(thread).getPriority();
    }

    public int getEffectivePriority(KThread thread) {
	Lib.assertTrue(Machine.interrupt().disabled());
		       
	return getThreadState(thread).getEffectivePriority();
    }

    public void setPriority(KThread thread, int priority) {
	Lib.assertTrue(Machine.interrupt().disabled());
		       
	Lib.assertTrue(priority >= priorityMinimum &&
		   priority <= priorityMaximum);
	
	getThreadState(thread).setPriority(priority);
    }

    public boolean increasePriority() {
	boolean intStatus = Machine.interrupt().disable();
		       
	KThread thread = KThread.currentThread();

	int priority = getPriority(thread);
	if (priority == priorityMaximum)
	    return false;

	setPriority(thread, priority+1);

	Machine.interrupt().restore(intStatus);
	return true;
    }

    public boolean decreasePriority() {
	boolean intStatus = Machine.interrupt().disable();
		       
	KThread thread = KThread.currentThread();

	int priority = getPriority(thread);
	if (priority == priorityMinimum)
	    return false;

	setPriority(thread, priority-1);

	Machine.interrupt().restore(intStatus);
	return true;
    }

    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static final int priorityDefault = 1;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMinimum = 0;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMaximum = 7;    

    /**
     * Return the scheduling state of the specified thread.
     *
     * @param	thread	the thread whose scheduling state to return.
     * @return	the scheduling state of the specified thread.
     */
    protected ThreadState getThreadState(KThread thread) {
	if (thread.schedulingState == null)
	    thread.schedulingState = new ThreadState(thread);

	return (ThreadState) thread.schedulingState;
    }

    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority.
     */
    protected class PriorityQueue extends ThreadQueue {
	PriorityQueue(boolean transferPriority) {
	    this.transferPriority = transferPriority;
	}

	public void waitForAccess(KThread thread) {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    getThreadState(thread).waitForAccess(this);
	}

	public void acquire(KThread thread) {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    getThreadState(thread).acquire(this);
	}

	public KThread nextThread() {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    // implement me
	    
	    //owner of this thread releases thread
	    if(this.owner != null)
	    getThreadState(this.owner).removeFromQueue(this);
	    
	    //if no one is waiting for this thread, there is no owner.
	    if(this.waitingQueue.isEmpty()) {
	    	this.owner = null;
	    }else {
	    	//else pick the next owner of this thread
	    	this.owner = (KThread)waitingQueue.peek();
	    }
	    
	    return this.owner;
	}

	/**
	 * Return the next thread that <tt>nextThread()</tt> would return,
	 * without modifying the state of this queue.
	 *
	 * @return	the next thread that <tt>nextThread()</tt> would
	 *		return.
	 */
	protected ThreadState pickNextThread() {
	    // implement me
		//picks the next highest thread in waiting queue
		System.out.println("returning KThread waitingQueue.peek");
	    return getThreadState((KThread) waitingQueue.poll());
	}
	
	public void print() {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    // implement me (if you want)
	    
	    Iterator it = waitingQueue.iterator();
		  int i = 0;
		  while (it.hasNext()) {
		    KThread thread = (KThread)it.next();
		    System.out.print(thread.getName()+"->");
		    i++;
		  }
		  System.out.println("\n"+i);
	        }
	        
	

	/**
	 * <tt>true</tt> if this queue should transfer priority from waiting
	 * threads to the owning thread.
	 */
	public boolean transferPriority;

	private KThread owner = null;
	
	private java.util.PriorityQueue waitingQueue = new java.util.PriorityQueue<KThread>(11, new sortByPriority());

	
    }
    public class sortByPriority implements Comparator<KThread> {
    	
		public int compare(KThread x, KThread y){
			int X = getThreadState(x).getEffectivePriority();
			int Y = getThreadState(y).getEffectivePriority();
				return X-Y;
		}
	}

    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see	nachos.threads.KThread#schedulingState
     */
    protected class ThreadState {
	/**
	 * Allocate a new <tt>ThreadState</tt> object and associate it with the
	 * specified thread.
	 *
	 * @param	thread	the thread this state belongs to.
	 */
	public ThreadState(KThread thread) {
	    this.thread = thread;
	    
	    setPriority(priorityDefault);
	    
	    this.queue = new LinkedList();
	}
	

	public void addToQueue(PriorityQueue t) {
		this.queue.add(t);
		t.waitingQueue.add(this.thread);   //===** this had Typecast issues **===
		
		
	}
	
	public void removeFromQueue(PriorityQueue t) {
		this.queue.remove(t);
		t.waitingQueue.remove(this);
	}
	
	public void isEmpty() {
		this.queue.isEmpty();
		
	}

	/**
	 * Return the priority of the associated thread.
	 *
	 * @return	the priority of the associated thread.
	 */
	public int getPriority() {
	    return priority;
	}

	/**
	 * Return the effective priority of the associated thread.
	 *
	 * @return	the effective priority of the associated thread.
	 */
	public int getEffectivePriority() {
	    // implement me
		int highest = this.priority;
		if(!this.queue.isEmpty()) {
			for(int i = 0; i <= this.queue.lastIndexOf(this.queue.getLast()); i++) {
				
				if(this.queue.get(i).transferPriority){ 
					if(!this.queue.get(i).waitingQueue.isEmpty()) {
					//System.out.print(this.queue.get(i)+"->");
					if(this.queue.get(i).pickNextThread().getEffectivePriority() > highest) {
						
						highest = this.queue.get(i).pickNextThread().getEffectivePriority();
					}
				}
				
			}
			}//System.out.println();
		}
	    return highest;
	}

	/**
	 * Set the priority of the associated thread to the specified value.
	 *
	 * @param	priority	the new priority.
	 */
	public void setPriority(int priority) {
	    if (this.priority == priority)
		return;
	    
	    this.priority = priority;
	    
	    // implement me
	    //I think it's ok like this
	}

	/**
	 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
	 * the associated thread) is invoked on the specified priority queue.
	 * The associated thread is therefore waiting for access to the
	 * resource guarded by <tt>waitQueue</tt>. This method is only called
	 * if the associated thread cannot immediately obtain access.
	 *
	 * @param	waitQueue	the queue that the associated thread is
	 *				now waiting on.
	 *
	 * @see	nachos.threads.ThreadQueue#waitForAccess
	 */
	public void waitForAccess(PriorityQueue waitQueue) {
	    // implement me
		//put the thing on waitQueue. 
		//I also wanted to put it on the thread's waitingQueue, but had typecast issues.
		Lib.assertTrue(Machine.interrupt().disabled());
		this.addToQueue(waitQueue);
		//System.out.println("waiting on "+this.thread);
	}

	/**
	 * Called when the associated thread has acquired access to whatever is
	 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
	 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
	 * <tt>thread</tt> is the associated thread), or as a result of
	 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
	 *
	 * @see	nachos.threads.ThreadQueue#acquire
	 * @see	nachos.threads.ThreadQueue#nextThread
	 */
	public void acquire(PriorityQueue waitQueue) {
	    // implement me
		if(queue.isEmpty()) {
		this.addToQueue(waitQueue);
		
		//say the owner is this thread
		waitQueue.owner = this.thread;
		
		
		System.out.println("acquired " + this.thread);
		}else {
			System.out.println(" not available "+ this.thread);
		}
		
	}	
	
	/** The thread with which this object is associated. */	   
	protected KThread thread;
	/** The priority of the associated thread. */
	protected int priority;
	
	/** Priority Queue's list of threads**/
	protected LinkedList<PriorityQueue> queue;
	
	
	
    } 
    
    
    public static void selfTest() {
		System.out.println("---------PriorityScheduler test---------------------");
		PriorityScheduler s = new PriorityScheduler();
		ThreadQueue queue = s.newThreadQueue(true);
		ThreadQueue queue2 = s.newThreadQueue(true);
		ThreadQueue queue3 = s.newThreadQueue(true);
		
		KThread thread1 = new KThread();
		KThread thread2 = new KThread();
		KThread thread3 = new KThread();
		KThread thread4 = new KThread();
		KThread thread5 = new KThread();
		thread1.setName("thread1");
		thread2.setName("thread2");
		thread3.setName("thread3");
		thread4.setName("thread4");
		thread5.setName("thread5");

		
		boolean intStatus = Machine.interrupt().disable();
		
		queue3.acquire(thread1);		//3: 1
		queue.acquire(thread1); 		//1: 1 <- (3, 1)
		queue.waitForAccess(thread2);	//1: 2 <- (1)
		queue.acquire(thread1);			//1: 1 <-(3, 1)
		
		queue2.acquire(thread4);		//2: 4 <-(2)
		queue2.waitForAccess(thread1);	//2: 1 <- (3, 2, 1)
		
		queue2.print(); //4, 1;
		
		System.out.println("thread1 EP="+s.getThreadState(thread1).getEffectivePriority());
		System.out.println("thread2 EP="+s.getThreadState(thread2).getEffectivePriority());
		System.out.println("thread4 EP="+s.getThreadState(thread4).getEffectivePriority());
		
		queue.print(); // 2, 1;
		
		
		s.getThreadState(thread2).setPriority(3); //(3, 1, 2)
		
		System.out.println("After setting thread2's EP=3:");
		System.out.println("thread1 EP="+s.getThreadState(thread1).getEffectivePriority()); // 3
		System.out.println("thread2 EP="+s.getThreadState(thread2).getEffectivePriority()); // 3
		System.out.println("thread4 EP="+s.getThreadState(thread4).getEffectivePriority()); // 1
	
		
		queue.waitForAccess(thread3);
		s.getThreadState(thread3).setPriority(5);
		
		System.out.println("After adding thread3 with EP=5:");
		System.out.println("thread1 EP="+s.getThreadState(thread1).getEffectivePriority()); // 5
		System.out.println("thread2 EP="+s.getThreadState(thread2).getEffectivePriority()); // 5
		System.out.println("thread3 EP="+s.getThreadState(thread3).getEffectivePriority()); // 5
		System.out.println("thread4 EP="+s.getThreadState(thread4).getEffectivePriority()); // 1
		
		s.getThreadState(thread3).setPriority(2);
		
		System.out.println("After setting thread3 EP=2:");
		System.out.println("thread1 EP="+s.getThreadState(thread1).getEffectivePriority()); // 3
		System.out.println("thread2 EP="+s.getThreadState(thread2).getEffectivePriority()); // 3
		System.out.println("thread3 EP="+s.getThreadState(thread3).getEffectivePriority()); // 3
		System.out.println("thread4 EP="+s.getThreadState(thread4).getEffectivePriority()); // 1
		
		System.out.println("Thread1 acquires queue and queue3");
		
		Machine.interrupt().restore(intStatus);
		System.out.println("--------End PriorityScheduler test------------------");
	}
    
    
}
