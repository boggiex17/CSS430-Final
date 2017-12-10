import java.util.Vector;

public class FileTable {

    private Vector table;          // the actual entity of this file table
    private Directory dir;         // the root directory

    public FileTable( Directory directory ) { // constructor
        table = new Vector( );     // instantiate a file (structure) table
        dir = directory;           // receive a reference to the Director
    }                              // from the file system

   //Allocates an entry into the file table
    //Parameters used to retreive  Inode number
    public synchronized FileTableEntry falloc( String filename, String mode ) {
        short iNodeNum = dir.namei( filename );  //Acquire inode number

        if ( iNodeNum == -1 )                //No number for inode
        {
            iNodeNum = dir.ialloc( filename ); //Allocate inode
        }
        byte [] iNodeBlock = new byte [Disk.blockSize];
        //Make sure blockNumber is in range
        int blockNum;
        if(iNodeNum % 16 != 0)
        {
            blockNum = 1 + iNodeNum/16;
        }
        else
        {
            blockNum = iNodeNum/16;
        }
        SysLib.rawread(blockNum, iNodeBlock ); //Read from disk
        Inode temp = new Inode( iNodeNum ); //Create inode with proper index
        FileTableEntry entry = new FileTableEntry( temp, iNodeNum, mode ); //File entry holds inode
        temp.count++;
        temp.toDisk( iNodeNum ); //Write to disk
        table.add(entry);
        return entry;
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
           else if (temp.flag == 3)
               e.inode.flag = 3;
           else if (temp.flag == 4)
               e.inode.flag = 3;

           temp.toDisk(e.iNumber);      // write to disk
           return true;
       }
    }

    public synchronized boolean fempty( ) {
        return table.isEmpty( );  // return if table is empty
    }                             // should be called before starting a format
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
