package nachos.threads;

import nachos.machine.*;
import nachos.threads.Scheduler;

import java.util.TreeSet;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A scheduler that chooses threads using a lottery.
 *
 * <p>
 * A lottery scheduler associates a number of tickets with each thread. When a
 * thread needs to be dequeued, a random lottery is held, among all the tickets
 * of all the threads waiting to be dequeued. The thread that holds the winning
 * ticket is chosen.
 *
 * <p>
 * Note that a lottery scheduler must be able to handle a lot of tickets
 * (sometimes billions), so it is not acceptable to maintain state for every
 * ticket.
 *
 * <p>
 * A lottery scheduler must partially solve the priority inversion problem; in
 * particular, tickets must be transferred through locks, and through joins.
 * Unlike a priority scheduler, these tickets add (as opposed to just taking
 * the maximum).
 */
public class LotteryScheduler extends Scheduler{
    /**
     * Allocate a new lottery scheduler.
     */
    public LotteryScheduler() {
    }
    
    /**
     * Allocate a new lottery thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer tickets from waiting threads
     *					to the owning thread.
     * @return	a new lottery thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferTickets) {
	// implement me
	return new LotteryQueue(transferTickets);
    }
    
    public int getTickets(KThread thread) {
    	Lib.assertTrue(Machine.interrupt().disabled());
    		       
    	return getThreadState(thread).getPriority();
        }

        public int getEffectivePriority(KThread thread) {
    	Lib.assertTrue(Machine.interrupt().disabled());
    		       
    	return getThreadState(thread).getEffectivePriority();
        }

        public void setPriority(KThread thread, int priority) {
    	Lib.assertTrue(Machine.interrupt().disabled());
    		       
    	Lib.assertTrue(priority >= 0 &&
    		   priority <= Integer.MAX_VALUE);
    	
    	getThreadState(thread).setPriority(priority);
        }

        public boolean increasePriority() {
    	boolean intStatus = Machine.interrupt().disable();
    		       
    	KThread thread = KThread.currentThread();

    	int priority = getPriority(thread);
    	if (priority == Integer.MAX_VALUE)
    	    return false;

    	setPriority(thread, priority+1);

    	Machine.interrupt().restore(intStatus);
    	return true;
        }

        public boolean decreasePriority() {
    	boolean intStatus = Machine.interrupt().disable();
    		       
    	KThread thread = KThread.currentThread();

    	int priority = getPriority(thread);
    	if (priority == 0)
    	    return false;

    	setPriority(thread, priority-1);

    	Machine.interrupt().restore(intStatus);
    	return true;
        }
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
        protected class LotteryQueue extends ThreadQueue {
    	LotteryQueue(boolean transferPriority) {
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
    	    return getThreadState((KThread) waitingQueue.peek());
		}
		
		protected ThreadState chooseThread(int i) {
			KThread p = new KThread();
			Iterator it = waitingQueue.iterator();
			for(int j = 0; j < i; j++) {
				if(it.hasNext())
					p = (KThread)it.next();
			}
			return getThreadState(p);
		}
    	
    	public void print() {
    	    Lib.assertTrue(Machine.interrupt().disabled());
    	    // implement me (if you want)
    	    
    	    Iterator it = waitingQueue.iterator();
    		  int i = 0;
    		  while (it.hasNext()) {
    		    KThread thread = (KThread)it.next();
    		   // System.out.print(thread.getName()+"->");
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
    			if(getThreadState(x).getEffectivePriority() > getThreadState(y).getEffectivePriority()) {
    				return 1;
    			}else if(getThreadState(y).getEffectivePriority() > getThreadState(x).getEffectivePriority()) {
    				return -1;
    			}else
    				return 0;
    		
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
    	    
    	    setPriority(1);
    	    
    	    this.queue = new LinkedList();
    	}
    	

    	public void addToQueue(LotteryQueue t) {
    		this.queue.add(t);
    		
    	}
    	
    	public void removeFromQueue(LotteryQueue t) {
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
    		int total = this.priority;
    		if(!this.queue.isEmpty()) {
    			for(int i = 0; i <= this.queue.lastIndexOf(this.queue.getLast()); i++) {
    				
    				if(this.queue.get(i).transferPriority && !this.queue.get(i).waitingQueue.isEmpty()) {
    					//System.out.print(this.queue.get(i)+"->");
    					if(this.queue.get(i+1).chooseThread(i+1).getEffectivePriority(i+1) > total) {
    						
    						total += this.queue.get(i+1).chooseThread(i+1).getEffectivePriority(i+1);
    					}
    				}
    			}//System.out.println();
    		}
    	    return total;
		}
		
		public int getEffectivePriority(int i) {
			// implement me
			int total = this.priority;
			if(!this.queue.isEmpty()) {
				for(; i <= this.queue.lastIndexOf(this.queue.getLast()); i++) {
					
					if(this.queue.get(i).transferPriority){ 
						if(!this.queue.get(i).waitingQueue.isEmpty()) {
						//System.out.print(this.queue.get(i)+"->");
						if(this.queue.get(i).chooseThread(i+1).getEffectivePriority(i+1) > total) {
							
							total += this.queue.get(i).chooseThread(i+1).getEffectivePriority(i+1);
							
						}
					}
				}
				}//System.out.println();
			}
			return total;
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
    	public void waitForAccess(LotteryQueue waitQueue) {
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
    	public void acquire(LotteryQueue waitQueue) {
    	    // implement me
    		if(queue.isEmpty()) {
    		this.addToQueue(waitQueue);
    		
    		//say the owner is this thread
    		waitQueue.owner = this.thread;
    		
    		
    		//System.out.println("acquired " + this.thread);
    		}//else {
    		//	System.out.println(" not available "+ this.thread);
    		//}
    		
    	}	
    	
    	/** The thread with which this object is associated. */	   
    	protected KThread thread;
    	/** The priority of the associated thread. */
    	protected int priority;
    	
    	/** Priority Queue's list of threads**/
    	protected LinkedList<LotteryQueue> queue;
    	
    	
    	
        } 
}
