import java.util.Vector;

public class FileTable {

    private Vector table;          // the actual entity of this file table
    private Directory dir;         // the root directory

    public FileTable( Directory directory ) { // constructor
        table = new Vector( );     // instantiate a file (structure) table
        dir = directory;           // receive a reference to the Director
    }                              // from the file system

    // major public methods
    public synchronized FileTableEntry falloc( String filename, String mode ) {
        // allocate a new file (structure) table entry for this file name
        // allocate/retrieve and register the corresponding inode using dir
        // increment this inode's count
        // immediately write back this inode to the disk
        // return a reference to this file (structure) table entry
        short iNumber = -1;
        Inode inode = null;

        while(true) {
            iNumber = (filename.equals("/") ? 0 : dir.namei(filename));

            if (iNumber >= 0) {
                inode = new Inode(iNumber);
                if (mode.equals("r")) {


                } else if (mode.equals("w")) {

                }
            }
            // did not find the file that the system wants to read
            else {
                if (mode.equals("r")) {
                    return null;
                }
            }
        }
        FileTableEntry test = new FileTableEntry();
        return  test;
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
