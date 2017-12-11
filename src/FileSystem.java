
public class FileSystem {
    private SuperBlock superblock;
    private Directory directory;
    private FileTable filetable;

    // Construnctor
    // takes in the number of disk blocks and uses it to initialize superblock
    // directory and file table
    public FileSystem(int diskBlocks) {
        // create superblock, and format disk with 64 inodes in default
        superblock = new SuperBlock(diskBlocks);
        // create directory, and register "/" in directory entry 0
        directory = new Directory(superblock.totalInodes);
        // file table is created, and store directory in the file table
        filetable = new FileTable(directory);

        // directory reconstruction
        FileTableEntry dirEnt = open("/", "r");
        int dirSize = fsize(dirEnt);
        if (dirSize > 0) {
            byte[] dirData = new byte[dirSize];
            read(dirEnt, dirData);
            directory.bytes2directory(dirData);
        }
        close(dirEnt);
    }

    // sync
    void sunc() {
        FileTableEntry var1 = open("/", "w");
        byte[] var2 = directory.directory2bytes();
        write(var1, var2);
        close(var1);
        superblock.sync();
    }


    // format
    boolean format(int files){

        if(files < 0)
            return false;

        superblock.format(files);

        //dir = new Directory(sb.totalInodes);
        //ftb = new FileTable(dir);
        return true;
        //return false;
    }

    // Open
    // takes in a filename string and a mode string and
    // creates a fileTableEntry sending these values to the filetable constuctor
    // to create a filetableEntry object that will act as a file descripor for the file
    // returns an instance of fileTableEnry
    FileTableEntry open(String filename, String mode) {
        FileTableEntry openFile = filetable.falloc(filename, mode); // makes entry
        return openFile;                                            // return fileTableEntry
    }

    // Close
    // takes in a file entry and closes the file by removing the file enlty
    // from the table vector
    boolean close(FileTableEntry ftEnt) {
        if (filetable.ffree(ftEnt))         // if ffree is successful
            return true;                    //
        return false;                       // not successful
    }

    // Fsize
    // takes in a file entry and returns the size of the file that entry is pointing to in bytes
    int fsize(FileTableEntry ftEnt) {
        return ftEnt.inode.length;
    }

    // read
    // takes in a file entry and a buffer to store the information read
    // the file entry is used to locate and access the location on the disk of where
    // this buffer will retrieve information from
    // returns -1 if invalid or the amount of data read in bytes
    public int read(FileTableEntry ftEnt, byte[] buffer)
    {
        // the mode is not readable so data cannot be read
        if (ftEnt.mode == "w" || ftEnt.mode == "a")
            return  -1;

        int buffLen = buffer.length;        // bytes to read

        if (buffLen == 0)                   // 0 bytes to read
            return -1;

        int bufLeft = 0;
        int readLeft = 0;
        int bufRead = 0;
        int leftToRead = 0;
        int offset = 0;
        int blocksLeft = 0;

        // while still need to read and seek pointer is not at the end and buffer has room to read into
        while(buffLen != 0 && ftEnt.seekPtr < fsize(ftEnt) && buffLen > 0)
        {

            int current = ftEnt.inode.findBlock(ftEnt.seekPtr); // find where to read
            if (current == -1)                                  // if nothing to read
                break;
            else
            {
                byte[] readData = new byte[Disk.blockSize];     // to read data from block
                SysLib.rawread(current, readData);              // read from disk

                offset = ftEnt.seekPtr % Disk.blockSize;        // get dsik offset
                readLeft = fsize(ftEnt) - ftEnt.seekPtr;        // how many bytes left
                blocksLeft = Disk.blockSize - offset;

                int smaller = Math.min(readLeft, blocksLeft);   // get the smaller value
                leftToRead = Math.min(smaller, bufLeft);        // get samller value

                // copy/read data from the disk into the buffer from limited bytes to
                // limited bytes a limited amount of bytes
                System.arraycopy(readData, offset, buffer, bufRead, leftToRead);
            }
            bufLeft = bufRead - leftToRead;         // update bytes left
            bufRead = bufRead + leftToRead;         // update number of bytes read
            ftEnt.seekPtr += leftToRead;            // updtate seek pointer
        }

        return bufRead;                             // return number of bytes read
    }

    int write(FileTableEntry ftEnt, byte[] buffer) {
        if (ftEnt.mode == "r") {
            return -1;
        }
        else {

            int numberOfBytes = buffer.length;
            int var4 = 0;
            while(numberOfBytes > 0) {
                int var6 = ftEnt.inode.findBlock(ftEnt.seekPtr);
                if (var6 == -1) {
                    short var7 = (short)this.superblock.getFreeBlock();
                    switch(ftEnt.inode.recordBlock(ftEnt.seekPtr, var7)) {
                        case -3:
                            short var8 = (short)this.superblock.getFreeBlock();
                            if (!ftEnt.inode.setBlock(var8)) {
                                SysLib.cerr("ThreadOS: panic on write\n");
                                return -1;
                            }
                            if (ftEnt.inode.recordBlock(ftEnt.seekPtr, var7) != 0) {
                                SysLib.cerr("ThreadOS: panic on write\n");
                                return -1;
                            }
                        case 0:
                        default:
                            var6 = var7;
                            break;
                        case -2:
                        case -1:
                            SysLib.cerr("ThreadOS: filesystem panic on write\n");
                            return -1;
                    }
                }

                byte[] var13 = new byte[512];
                if (SysLib.rawread(var6, var13) == -1) {
                    System.exit(2);
                }
                int var14 = ftEnt.seekPtr % 512;
                int var9 = 512 - var14;
                int var10 = Math.min(var9, numberOfBytes);
                System.arraycopy(buffer, var4, var13, var14, var10);
                SysLib.rawwrite(var6, var13);
                ftEnt.seekPtr += var10;
                var4 += var10;
                numberOfBytes -= var10;
                if (ftEnt.seekPtr > ftEnt.inode.length) {
                    ftEnt.inode.length = ftEnt.seekPtr;
                }
                ftEnt.inode.toDisk(ftEnt.iNumber);
                return var4;
            }
        }
        return -1;
    }

    // deallocAllBlock
    // takes in a file table entry and goes through all the pointers deallocating the blocks
    // return true or false based on successful execution
    private boolean deallocAllBlocks(FileTableEntry ftEnt) {
        if (ftEnt.inode.count != 1)                 // if not valid
            return  false;
        for (int i = 0; i < 11; i++) {              // loop indirect block pointers
            if (ftEnt.inode.direct[i] == -1)        // if pointing to nothing
                continue;
            else {                                  // pointing to data
                superblock.returnBlock(i);          // get superblock
                ftEnt.inode.direct[i] = -1;         // set pointer to nothing
            }
        }

        byte[] data = ftEnt.inode.unregisterIndexBlock(); // temporary read data

        if (data != null) {                         // data exists
            short index;
            while((index = SysLib.bytes2short(data, 0)) != -1) {    //
                superblock.returnBlock(index);
            }
        }
        return true;
    }

    // delete
    // takes in a filename and deletes that file name from the system
    boolean delete(String filename) {
        FileTableEntry Entry = open(filename, "w");      // check if file exists
        //short location = Entry.iNumber;
        if (directory.ifree(Entry.iNumber))                     // remove from dirrectory
            if (close(Entry))                                   // close
                return true;                                    // success

        return false;
    }
    private final int SEEK_SET = 0;
    private final int SEEK_CUR = 1;
    private final int SEEK_END = 2;

    // seek
    // takes in 3 arguments, a file table entry and offset int and an location int
    // and used these arguments to set a new seek pointer to the entry that is
    // passed in. Returns -1 if failed or the new seek pointer if success
    int seek(FileTableEntry ftEnt, int offset, int whence) {

        if(ftEnt != null) {          // if valid entry and offset
            if (offset < 0)          // if offset is invalid make it valid
                offset = 0;

            if (whence == SEEK_SET) {           // if at beggining
                ftEnt.seekPtr = offset;         // set pointer
            } else if (whence == SEEK_CUR) {    // if at current
                int newOffset = ftEnt.seekPtr + offset; // make new pointer
                ftEnt.seekPtr = newOffset;              // set pointer
            } else if (whence == SEEK_END) {    // if at end
                int newOffset = ftEnt.inode.length + offset; // make new pointer
                ftEnt.seekPtr = newOffset;                   // set pointer
            } else
                return -1;                      // not a case
            return  ftEnt.seekPtr;              // return seek pointer
        }
        return -1;                              // error
    }
}






