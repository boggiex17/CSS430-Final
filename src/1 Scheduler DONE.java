// Completed ===================================================================

import java.util.*;

public class Scheduler extends Thread
{
    private Vector queue;
    private int timeSlice;
    private static final int DEFAULT_TIME_SLICE = 1000;

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
            for ( int i = 0; i < queue.size( ); i++ ) {
            TCB tcb = ( TCB )queue.elementAt( i );
            Thread thread = tcb.getThread( );
            if ( thread == myThread ) // if this is my TCB, return it
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
        initTid( DEFAULT_MAX_THREADS );
    }

    public Scheduler( int quantum ) {
        timeSlice = quantum;
        queue = new Vector( );
        initTid( DEFAULT_MAX_THREADS );
    }

    // A new feature added to p161 
    // A constructor to receive the max number of threads to be spawned
    public Scheduler( int quantum, int maxThreads ) {
        timeSlice = quantum;
        queue = new Vector( );
        initTid( maxThreads );
    }

    private void schedulerSleep( ) {
        try {
            Thread.sleep( timeSlice );
        } catch ( InterruptedException e ) { }
    }

    // A modified addThread of p161 example
    public TCB addThread( Thread t ) {
        t.setPriority( 2 );
        TCB parentTcb = getMyTcb( ); // get my TCB and find my TID
        int pid = ( parentTcb != null ) ? parentTcb.getTid( ) : -1;
        int tid = getNewTid( ); // get a new TID
        if ( tid == -1)
            return null;
        TCB tcb = new TCB( t, tid, pid ); // create a new TCB
    
        // the following if and for statements are for file system.
        if ( parentTcb != null ) {
            for ( int i = 0; i < 32; i++ ) {
                tcb.ftEnt[i] = parentTcb.ftEnt[i];
                // JFM added 2012-12-01
                // increment the count for any file table entries inherited from parent
                if ( tcb.ftEnt[i] != null )
                    tcb.ftEnt[i].count++;
            }
        }
    
        queue.add( tcb );
        return tcb;
    }

    // A new feature added to p161
    // Removing the TCB of a terminating thread
    public boolean deleteThread( ) {
        TCB tcb = getMyTcb( ); 
        if ( tcb == null )
            return false;
        else {
            // JFM added 2012-12-01
            // if any file table entries are still open, decrement their count
            for ( int i = 3; i < 32; i++ )
                if ( tcb.ftEnt[i] != null )
                    // JFM changed 2012-12-13
                    // close any open file descriptors rather than decrement the counts
                    // to ensure that system-wide file table entries are removed
                    // when no longer needed
                    SysLib.close( i );
            return tcb.setTerminated( );
        }
    }

    public void sleepThread( int milliseconds ) {
        try {
            sleep( milliseconds );
        } catch ( InterruptedException e ) { }
    }
    
    // A modified run of p161
    public void run( ) {
        Thread current = null;
    
        this.setPriority( 6 );
        
        while ( true ) {
            try {
                // get the next TCB and its thrad
                if ( queue.size( ) == 0 )
                    continue;
                TCB currentTCB = (TCB)queue.firstElement( );
                if ( currentTCB.getTerminated( ) == true ) {
                    queue.remove( currentTCB );
                    returnTid( currentTCB.getTid( ) );
                    continue;
                }
                current = currentTCB.getThread( );
                if ( current != null ) {
                    if ( current.isAlive( ) )
                        current.setPriority( 4 );
                    else {
                        // Spawn must be controlled by Scheduler
                        // Scheduler must start a new thread
                        current.start( ); 
                        current.setPriority( 4 );
                    }
                }
                
                schedulerSleep( );
                // System.out.println("* * * Context Switch * * * ");
                
                synchronized ( queue ) {
                    if ( current != null && current.isAlive( ) )
                        current.setPriority( 2 );
                    queue.remove( currentTCB ); // rotate this TCB to the end
                    queue.add( currentTCB );
                }
            } catch ( NullPointerException e3 ) { };
        }
    }
}

/*
import java.util.Vector;

public class FileTable {

    private Vector table;          // the actual entity of this file table
    private Directory dir;         // the root directory

    public FileTable( Directory directory ) { // constructor
        table = new Vector<FileTableEntry>();     // instantiate a file (structure) table
        dir = directory;           // receive a reference to the Director
    }                              // from the file system


    // falloc
    // allocate a new file (structure) table entry for this file name
    // allocate/retrieve and register the corresponding inode using dir
    // increment this inode's count
    // immediately write back this inode to the disk
    // return a reference to this file (structure) table entry
    // takes in a filename and a mode and allocates a new file table entry
    // to the file descriptor vector
    public synchronized FileTableEntry falloc( String filename, String mode ) {
        short iNode = dir.namei(filename);  // retrieve INode
        if (iNode == -1)                    // iNode not found
            iNode = dir.ialloc(filename);   // allocate a new inode

        byte[] block = new byte[Disk.blockSize];    // temporary buffer

        // get block number
        int blockNumber = (iNode % 16 != 0) ? 3 + iNode / 16 : 2 + iNode / 16;
        SysLib.rawread(blockNumber, block);                 // read block

        Inode temp = new Inode(iNode);  // temporary inode
        FileTableEntry Entry = new FileTableEntry(temp, iNode, mode);
        temp.count++;                   // add to inode count
        temp.toDisk(iNode);             // write to disk

        table.add(Entry);               // add entry to vector
        return Entry;                   // return
    }

    // Recieve a file table entry reference as a parameter and
    // free this file table entry. Save corresponding inode to the
    // disk. Return true if file table entry was in table
    public synchronized boolean ffree( FileTableEntry e ) {
       if(!table.contains(e))
           return false;
       else {
           Inode temp = e.inode;
           temp.count--;

           if (temp.flag == 1)
               e.inode.flag = 0;
           else if (temp.flag == 2)
               e.inode.flag = 0;

           temp.toDisk(e.iNumber);      // write to disk
           return true;
       }
    }

    public synchronized boolean fempty( ) {
        return table.isEmpty( );  // return if table is empty
    }                             // should be called before starting a format
}
*/
