
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

    public int read(FileTableEntry ftEnt, byte[] buffer)
    {

        int sizeLeftToRead = 0;
        int trackDataRead = 0;
        int size = buffer.length;
        // Could not read if the File Table Entry has mode write or append
        if (ftEnt.mode.equals("w") || ftEnt.mode.equals("a")) return -1;
        // Check for invalid passed in parameter
        if (buffer == null)
        {
            return -1;
        }

        synchronized (ftEnt)
        {
            // Only stop when the seek pointer is still within the range
            // And the buffer still have place to read data into
            while (ftEnt.seekPtr < fsize(ftEnt) && buffer.length > 0)
            {

                // FIND BLOCK NUMBER
                int blockNum = ftEnt.inode.findBlock(ftEnt.seekPtr);
                //
                if (blockNum != -1)
                {

                    byte[] tempRead = new byte[Disk.blockSize];
                    // Know the block location to read from, now load the data from disk
                    SysLib.rawread(blockNum, buffer);

                    // How far we go in to


                    int dataGetInto = ftEnt.seekPtr % Disk.blockSize;
                    int remainingBlocks = Disk.blockSize - dataGetInto;
                    int remaining = fsize(ftEnt) - ftEnt.seekPtr;


                    int smallerBetweenBlockandData = Math.min(remainingBlocks, size);
                    // Check to see how much left we can read versus the size remaining
                    sizeLeftToRead = Math.min(smallerBetweenBlockandData, remaining);


                    System.arraycopy(tempRead, dataGetInto, buffer, trackDataRead, sizeLeftToRead);
                    // Update the varaible to read into the byte array
                    trackDataRead += sizeLeftToRead;
                    // Update the Seek Pointer to read at new position
                    ftEnt.seekPtr += sizeLeftToRead;
                    // Update the size total.
                    size -= sizeLeftToRead;
                } else
                {
                    // Invalid block location
                    break;
                }

            }
            return trackDataRead;

        }
        // Default return value, if reached here, then no success


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

    private boolean deallocAllBlocks(FileTableEntry ftEnt) {
        return false;
    }

    boolean delete(String filename) {
        return false;
    }

    private final int SEEK_SET = 0;
    private final int SEEK_CUR = 1;
    private final int SEEK_END = 2;

    int seek(FileTableEntry ftEnt, int offset, int whence) {
        return 0;
    }

}


/*
public class FileSystem {
    private SuperBlock sb;
    private Directory dir;
    private FileTable ftb;
    private final int SEEK_SET = 0;
    private final int SEEK_CUR = 1;
    private final int SEEK_END = 2;

    // CONSTRUCTOR
    // takes in a int as a variable which is the disk size and uses it to initialize
    // superblock, directory and Filetable
    public FileSystem(int var1) {
        sb = new SuperBlock(var1);
        dir = new Directory(sb.totalInodes);
        ftb = new FileTable(dir);
        FileTableEntry var2 = open("/", "r");
        int var3 = fsize(var2);
        if(var3 > 0) {
            byte[] var4 = new byte[var3];
            read(var2, var4);
            dir.bytes2directory(var4);
        }

        close(var2);
    }

    // SYNC
    // Syncs FilTableEntry to disk
    // and closes the FTE
    // calls superblocks sync method

    void sync() {
        FileTableEntry var1 = open("/", "w");
        byte[] var2 = dir.directory2bytes();
        write(var1, var2);
        close(var1);
        sb.sync();
    }

    // FORMAT
    // Calls superblocks format method
    // and assigns dir and ftb variables new instances of their objects
    boolean format(int var1) {
        while(!ftb.fempty()) {
            ;
        }

        sb.format(var1);
        dir = new Directory(sb.totalInodes);
        ftb = new FileTable(dir);
        return true;
    }

    // OPEN
    // creates a FileTable Entry
    // returns FileTable Entry
    FileTableEntry open(String var1, String var2) {
        FileTableEntry var3 = ftb.falloc(var1, var2);
        return var2 == "w" && !deallocAllBlocks(var3)?null:var3;
    }

    // CLOSE
    // calls FileTable's ffree method
    boolean close(FileTableEntry var1) {
        synchronized(var1) {
            --var1.count;
            if(var1.count > 0) {
                return true;
            }
        }

        return ftb.ffree(var1);
    }

    // FSIZE
    // Returns length of file
    int fsize(FileTableEntry var1) {
        synchronized(var1) {
            return var1.inode.length;
        }
    }

    // READ
    // Reads the file and returns an int depending on success of the method
    int read(FileTableEntry var1, byte[] var2) {
        if(var1.mode != "w" && var1.mode != "a") {
            int var3 = 0;
            int var4 = var2.length;
            synchronized(var1) {
                while(var4 > 0 && var1.seekPtr < fsize(var1)) {
                    int var6 = var1.inode.findTargetBlock(var1.seekPtr);
                    if(var6 == -1) {
                        break;
                    }

                    byte[] var7 = new byte[512];
                    SysLib.rawread(var6, var7);
                    int var8 = var1.seekPtr % 512;
                    int var9 = 512 - var8;
                    int var10 = fsize(var1) - var1.seekPtr;
                    int var11 = Math.min(Math.min(var9, var4), var10);
                    System.arraycopy(var7, var8, var2, var3, var11);
                    var1.seekPtr += var11;
                    var3 += var11;
                    var4 -= var11;
                }

                return var3;
            }
        } else {
            return -1;
        }
    }

    // WRITE
    // writes to disk
    // Parametes given a FileTableEntry and a byteArray to write to
    // return an int corresponding on the status of the method at the end
    int write(FileTableEntry var1, byte[] var2) {
        if(var1.mode == "r") {
            return -1;
        } else {
            synchronized(var1) {
                int var4 = 0;
                int var5 = var2.length;

                while(var5 > 0) {
                    int var6 = var1.inode.findTargetBlock(var1.seekPtr);
                    if(var6 == -1) {
                        short var7 = (short)sb.getFreeBlock();
                        switch(var1.inode.registerTargetBlock(var1.seekPtr, var7)) {
                            case -3:
                                short var8 = (short)sb.getFreeBlock();
                                if(!var1.inode.registerIndexBlock(var8)) {
                                    SysLib.cerr("ThreadOS: panic on write\n");
                                    return -1;
                                }

                                if(var1.inode.registerTargetBlock(var1.seekPtr, var7) != 0) {
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
                    if(SysLib.rawread(var6, var13) == -1) {
                        System.exit(2);
                    }

                    int var14 = var1.seekPtr % 512;
                    int var9 = 512 - var14;
                    int var10 = Math.min(var9, var5);
                    System.arraycopy(var2, var4, var13, var14, var10);
                    SysLib.rawwrite(var6, var13);
                    var1.seekPtr += var10;
                    var4 += var10;
                    var5 -= var10;
                    if(var1.seekPtr > var1.inode.length) {
                        var1.inode.length = var1.seekPtr;
                    }
                }

                var1.inode.toDisk(var1.iNumber);
                return var4;
            }
        }
    }


    // DEALLOCALLBLOCKS
    // Goes though all of the blocks and deallocates them
    // returns a boolean true if success false otherwise
    private boolean deallocAllBlocks(FileTableEntry var1) {
        if(var1.inode.count != 1) {
            return false;
        } else {
            byte[] var2 = var1.inode.unregisterIndexBlock();
            if(var2 != null) {
                byte var3 = 0;

                short var4;
                while((var4 = SysLib.bytes2short(var2, var3)) != -1) {
                    sb.returnBlock(var4);
                }
            }

            int var5 = 0;

            while(true) {
                Inode var10001 = var1.inode;
                if(var5 >= 11) {
                    var1.inode.toDisk(var1.iNumber);
                    return true;
                }

                if(var1.inode.direct[var5] != -1) {
                    sb.returnBlock(var1.inode.direct[var5]);
                    var1.inode.direct[var5] = -1;
                }

                ++var5;
            }
        }
    }

    // DELETE
    // Calls directory's ifree method to delete the file
    // returns a boolean depending on the success of the method
    boolean delete(String var1) {
        FileTableEntry var2 = open(var1, "w");
        short var3 = var2.iNumber;
        return close(var2) && dir.ifree(var3);
    }

    // SEEK
    // Seek finds the location in the file
    // returns an int where the seekPtr is looking at
    int seek(FileTableEntry var1, int var2, int var3) {
        synchronized(var1) {
            switch(var3) {
                case 0:
                    if(var2 >= 0 && var2 <= fsize(var1)) {
                        var1.seekPtr = var2;
                        break;
                    }

                    return -1;
                case 1:
                    if(var1.seekPtr + var2 >= 0 && var1.seekPtr + var2 <= fsize(var1)) {
                        var1.seekPtr += var2;
                        break;
                    }

                    return -1;
                case 2:
                    if(fsize(var1) + var2 < 0 || fsize(var1) + var2 > fsize(var1)) {
                        return -1;
                    }

                    var1.seekPtr = fsize(var1) + var2;
            }

            return var1.seekPtr;
        }
    }
}
*/





