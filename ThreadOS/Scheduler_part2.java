import java.util.Vector;

public class Scheduler_part2 extends Thread
{
	private Vector queue;
	private Vector queue1;   // 1st queue
	private Vector queue2;   // 2nd queue
    private int timeSlice;
    private static final int DEFAULT_TIME_SLICE = 500;  // for 0th queue

    // New data added to p161
    private boolean[] tids; // Indicate which ids have been used
    private static final int DEFAULT_MAX_THREADS = 10000;

    // A new feature added to p161
    // Allocate an ID array, each element indicating if that id has been used
    private int nextId = 0;
    private void initTid( int maxThreads ) {
	tids = new boolean[maxThreads];
	for ( int i = 0; i < maxThreads; i++ )
	    tids[i] = false;
    }

    // A new feature added to p161
    // Search an available thread ID and provide a new thread with this ID
    private int getNewTid( ) {
	for ( int i = 0; i < tids.length; i++ ) {
	    int tentative = ( nextId + i ) % tids.length;
	    if ( tids[tentative] == false ) {
		tids[tentative] = true;
		nextId = ( tentative + 1 ) % tids.length;
		return tentative;
	    }
	}
	return -1;
    }

    // A new feature added to p161
    // Return the thread ID and set the corresponding tids element to be unused
    private boolean returnTid( int tid ) {
	if ( tid >= 0 && tid < tids.length && tids[tid] == true ) {
	    tids[tid] = false;
	    return true;
	}
	return false;
    }

    // A new feature added to p161
    // Retrieve the current thread's TCB from the queue
    public TCB getMyTcb( ) {
	Thread myThread = Thread.currentThread( ); // Get my thread object
		synchronized( queue ) {
			for (int i = 0; i < queue.size(); i++) {
				TCB tcb = (TCB) queue.elementAt(i);
				Thread thread = tcb.getThread();
				if (thread == myThread) // if this is my TCB, return it
					return tcb;
			}
		}
	    synchronized( queue1 ) {      // search queue 1
			for (int i = 0; i < queue1.size(); i++) {
				TCB tcb = (TCB) queue1.elementAt(i);
				Thread thread = tcb.getThread();
				if (thread == myThread) // if this is my TCB, return it
					return tcb;
			}
		}
		synchronized( queue2 ) {      // search queue 2
			for (int i = 0; i < queue2.size(); i++) {
				TCB tcb = (TCB) queue2.elementAt(i);
				Thread thread = tcb.getThread();
				if (thread == myThread) // if this is my TCB, return it
					return tcb;
			}
		}
	return null;
    }

    // A new feature added to p161
    // Return the maximal number of threads to be spawned in the system
    public int getMaxThreads( ) {
	return tids.length;
    }

    public Scheduler_part2( ) {
	timeSlice = DEFAULT_TIME_SLICE;
	queue = new Vector( );
	queue1 = new Vector( );   // 1st queue
	queue2 = new Vector( );	  // 2nd queue
	initTid( DEFAULT_MAX_THREADS );
    }

    public Scheduler_part2(int quantum ) {
	timeSlice = quantum;
	queue = new Vector( );
	queue1 = new Vector( );  // 1st queue
	queue2 = new Vector( );  // 2nd queue
	initTid( DEFAULT_MAX_THREADS );
    }

    // A new feature added to p161
    // A constructor to receive the max number of threads to be spawned
    public Scheduler_part2(int quantum, int maxThreads ) {
	timeSlice = quantum;
	queue = new Vector( );
	queue1 = new Vector( );  // 1st queue
	queue2 = new Vector( );  // 2nd queue
	initTid( maxThreads );
    }

    private void schedulerSleep( ) {
	try {
	    Thread.sleep( timeSlice );
	} catch ( InterruptedException e ) {
	}
    }

    // A modified addThread of p161 example
    public TCB addThread( Thread t ) {
	// t.setPriority( 2 );
	TCB parentTcb = getMyTcb( ); // get my TCB and find my TID
	int pid = ( parentTcb != null ) ? parentTcb.getTid( ) : -1;
	int tid = getNewTid( ); // get a new TID
	if ( tid == -1)
	    return null;
	TCB tcb = new TCB( t, tid, pid ); // create a new TCB

		queue.add( tcb );
	return tcb;
    }

    // A new feature added to p161
    // Removing the TCB of a terminating thread
    public boolean deleteThread( ) {
	TCB tcb = getMyTcb( ); 
	if ( tcb!= null )
	    return tcb.setTerminated( );
	else
	    return false;
    }

    public void sleepThread( int milliseconds ) {
	try {
	    sleep( milliseconds );
	} catch ( InterruptedException e ) { }
    }
    
    // A modified run of p161
    public void run( ) {
		Thread current = null;

		// this.setPriority( 6 );

		int execLeft1 = 2;
		int execLeft2 = 4;

		while ( true ) {

	    	try {
				// get the next TCB and its thread
				if ( queue.size( ) == 0 && queue1.size( ) == 0 && queue2.size( ) == 0 )
		    		continue;

				// case if queue in not empty
				if ( queue.size( ) != 0 ) {
					TCB currentTCB = (TCB)queue.firstElement( );   // retrieve first element of queue
					current = currentTCB.getThread( );

					if ( current != null )                       // if not null start executing
						current.start( );						 // start thread

					schedulerSleep( );						     // run for 500 ms

					if( currentTCB.getTerminated( ) ) {          // if terminated
						queue.remove( currentTCB );              // remove from queue
						returnTid( currentTCB.getTid( ) );       // get Tid
						continue;								 // go to top
					}
					else {									 	 // if not terminated
						current.suspend();						 // suspend
						queue.remove( currentTCB );				 // remove from queue
						queue1.add( currentTCB );				 // add to queue1
					}
					continue;									 // go to top
				}

				// case if queue is empty but queue1 is not empty
				if ( queue1.size( ) != 0 ) {
					TCB currentTCB = (TCB)queue1.firstElement( );  // get first element in queue1
					current = currentTCB.getThread( );

					if ( execLeft1 == 0 ) {						 // thread executed full time quantum
						if ( current != null && current.isAlive( ) )	// check if valid
							current.suspend();					 // suspend
						queue1.remove( currentTCB ); 			 // remove from queue1
						queue2.add( currentTCB );				 // add to queue2
						execLeft1 = 2;							 // reset exec counter
						continue;								 // go to top
					}
					else {
						if ( current != null && current.isAlive( ) )	// check if valid
							current.resume();					 // resume thread

						schedulerSleep( );						 // run for 500 ms
						execLeft1--;							 // subtract from exec counter

						if ( currentTCB.getTerminated( ) ) {	 // if terminated
							execLeft1 = 2; 						 // reset exec counter
							queue1.remove( currentTCB );  		 // remove from queue1
							returnTid( currentTCB.getTid( ) );	 // return Tid
						}
						continue;          						 // go to top
					}
				}

				// case if queue and queue1 is empty but queue 2 is not
				if ( queue2.size( ) != 0 ) {
					TCB currentTCB = (TCB)queue2.firstElement( );  // get first element in queue2
					current = currentTCB.getThread( );

					if( execLeft2 == 0 ) {                       // thread executed full time quantum
						synchronized ( queue2 ) {
							if (current != null && current.isAlive())
								current.suspend();				 // suspend
							queue2.remove(currentTCB);           // remove from queue2
							queue2.add(currentTCB);				 // put at the end of queue2
							execLeft2 = 4;                       // reset execution counter
							continue;							 // go to top
						}
					}
					else {                                       // thread not fully executed
						if( current != null && current.isAlive( ) )
							current.resume();                    // resume thread

						schedulerSleep();   				 	 // run for 500 ms
						execLeft2--;                             // remove from exec counter

						if( currentTCB.getTerminated( ) ) {      // check if terminated
							execLeft2 = 4;                       // reset exec counter
							queue2.remove(currentTCB);           // remove thread from queue2
							returnTid(currentTCB.getTid());      // return Tid
						}
						continue;                                // go to top
					}
				}


	    	} catch ( NullPointerException e3 ) { };
		}
    }
}

/*
import java.util.Vector;

public class Scheduler extends Thread
{
	private Vector queue;
	private Vector queue1;   // 1st queue
	private Vector queue2;   // 2nd queue
	private int timeSlice;
	private static final int DEFAULT_TIME_SLICE = 500;  // for 0th queue

	// New data added to p161
	private boolean[] tids; // Indicate which ids have been used
	private static final int DEFAULT_MAX_THREADS = 10000;

	// A new feature added to p161
	// Allocate an ID array, each element indicating if that id has been used
	private int nextId = 0;
	private void initTid( int maxThreads ) {
		tids = new boolean[maxThreads];
		for ( int i = 0; i < maxThreads; i++ )
			tids[i] = false;
	}

	// A new feature added to p161
	// Search an available thread ID and provide a new thread with this ID
	private int getNewTid( ) {
		for ( int i = 0; i < tids.length; i++ ) {
			int tentative = ( nextId + i ) % tids.length;
			if ( tids[tentative] == false ) {
				tids[tentative] = true;
				nextId = ( tentative + 1 ) % tids.length;
				return tentative;
			}
		}
		return -1;
	}

	// A new feature added to p161
	// Return the thread ID and set the corresponding tids element to be unused
	private boolean returnTid( int tid ) {
		if ( tid >= 0 && tid < tids.length && tids[tid] == true ) {
			tids[tid] = false;
			return true;
		}
		return false;
	}

	// A new feature added to p161
	// Retrieve the current thread's TCB from the queue
	public TCB getMyTcb( ) {
		Thread myThread = Thread.currentThread( ); // Get my thread object
		synchronized( queue ) {
			for (int i = 0; i < queue.size(); i++) {
				TCB tcb = (TCB) queue.elementAt(i);
				Thread thread = tcb.getThread();
				if (thread == myThread) // if this is my TCB, return it
					return tcb;
			}
		}
		synchronized( queue1 ) {      // search queue 1
			for (int i = 0; i < queue1.size(); i++) {
				TCB tcb = (TCB) queue1.elementAt(i);
				Thread thread = tcb.getThread();
				if (thread == myThread) // if this is my TCB, return it
					return tcb;
			}
		}
		synchronized( queue2 ) {      // search queue 2
			for (int i = 0; i < queue2.size(); i++) {
				TCB tcb = (TCB) queue2.elementAt(i);
				Thread thread = tcb.getThread();
				if (thread == myThread) // if this is my TCB, return it
					return tcb;
			}
		}
		return null;
	}

	// A new feature added to p161
	// Return the maximal number of threads to be spawned in the system
	public int getMaxThreads( ) {
		return tids.length;
	}

	public Scheduler( ) {
		timeSlice = DEFAULT_TIME_SLICE;
		queue = new Vector( );
		queue1 = new Vector( );   // 1st queue
		queue2 = new Vector( );	  // 2nd queue
		initTid( DEFAULT_MAX_THREADS );
	}

	public Scheduler(int quantum ) {
		timeSlice = quantum;
		queue = new Vector( );
		queue1 = new Vector( );  // 1st queue
		queue2 = new Vector( );  // 2nd queue
		initTid( DEFAULT_MAX_THREADS );
	}

	// A new feature added to p161
	// A constructor to receive the max number of threads to be spawned
	public Scheduler(int quantum, int maxThreads ) {
		timeSlice = quantum;
		queue = new Vector( );
		queue1 = new Vector( );  // 1st queue
		queue2 = new Vector( );  // 2nd queue
		initTid( maxThreads );
	}

	private void schedulerSleep( ) {
		try {
			Thread.sleep( timeSlice );
		} catch ( InterruptedException e ) {
		}
	}

	// A modified addThread of p161 example
	public TCB addThread( Thread t ) {
		// t.setPriority( 2 );
		TCB parentTcb = getMyTcb( ); // get my TCB and find my TID
		int pid = ( parentTcb != null ) ? parentTcb.getTid( ) : -1;
		int tid = getNewTid( ); // get a new TID
		if ( tid == -1)
			return null;
		TCB tcb = new TCB( t, tid, pid ); // create a new TCB

		queue.add( tcb );
		return tcb;
	}

	// A new feature added to p161
	// Removing the TCB of a terminating thread
	public boolean deleteThread( ) {
		TCB tcb = getMyTcb( );
		if ( tcb!= null )
			return tcb.setTerminated( );
		else
			return false;
	}

	public void sleepThread( int milliseconds ) {
		try {
			sleep( milliseconds );
		} catch ( InterruptedException e ) { }
	}

	// A modified run of p161
	public void run( ) {
		Thread current = null;

		// this.setPriority( 6 );

		int execLeft1 = 2;
		int execLeft2 = 4;

		while ( true ) {

			try {
				// get the next TCB and its thread
				if ( queue.size( ) == 0 && queue1.size( ) == 0 && queue2.size( ) == 0 )
					continue;

				// case if queue in not empty
				if ( queue.size( ) != 0 ) {
					TCB currentTCB = (TCB)queue.firstElement( );   // retrieve first element of queue
					current = currentTCB.getThread( );

					if ( current != null )                       // if not null start executing
						current.start( );						 // start thread

					schedulerSleep( );						     // run for 500 ms

					if( currentTCB.getTerminated( ) ) {          // if terminated
						queue.remove( currentTCB );              // remove from queue
						returnTid( currentTCB.getTid( ) );       // get Tid
						continue;								 // go to top
					}
					else {									 	 // if not terminated
						current.suspend();						 // suspend
						queue.remove( currentTCB );				 // remove from queue
						queue1.add( currentTCB );				 // add to queue1
					}
					continue;									 // go to top
				}

				// case if queue is empty but queue1 is not empty
				if ( queue1.size( ) != 0 ) {
					TCB currentTCB = (TCB)queue1.firstElement( );  // get first element in queue1
					current = currentTCB.getThread( );

					if ( execLeft1 == 0 ) {						 // thread executed full time quantum
						if ( current != null && current.isAlive( ) )	// check if valid
							current.suspend();					 // suspend
						queue1.remove( currentTCB ); 			 // remove from queue1
						queue2.add( currentTCB );				 // add to queue2
						execLeft1 = 2;							 // reset exec counter
						continue;								 // go to top
					}
					else {
						if ( current != null && current.isAlive( ) )	// check if valid
							current.resume();					 // resume thread

						schedulerSleep( );						 // run for 500 ms
						execLeft1--;							 // subtract from exec counter

						if ( currentTCB.getTerminated( ) ) {	 // if terminated
							execLeft1 = 2; 						 // reset exec counter
							queue1.remove( currentTCB );  		 // remove from queue1
							returnTid( currentTCB.getTid( ) );	 // return Tid
						}
						continue;          						 // go to top
					}
				}

				// case if queue and queue1 is empty but queue 2 is not
				if ( queue2.size( ) != 0 ) {
					TCB currentTCB = (TCB)queue2.firstElement( );  // get first element in queue2
					current = currentTCB.getThread( );

					if( execLeft2 == 0 ) {                       // thread executed full time quantum
						synchronized ( queue2 ) {
							if (current != null && current.isAlive())
								current.suspend();				 // suspend
							queue2.remove(currentTCB);           // remove from queue2
							queue2.add(currentTCB);				 // put at the end of queue2
							execLeft2 = 4;                       // reset execution counter
							continue;							 // go to top
						}
					}
					else {                                       // thread not fully executed
						if( current != null && current.isAlive( ) )
							current.resume();                    // resume thread

						schedulerSleep();   				 	 // run for 500 ms
						execLeft2--;                             // remove from exec counter

						if( currentTCB.getTerminated( ) ) {      // check if terminated
							execLeft2 = 4;                       // reset exec counter
							queue2.remove(currentTCB);           // remove thread from queue2
							returnTid(currentTCB.getTid());      // return Tid
						}
						continue;                                // go to top
					}
				}


			} catch ( NullPointerException e3 ) { };
		}
	}
}

*/
