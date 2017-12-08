
public class Inode {
    private final static int iNodeSize = 32;       // fix to 32 bytes
    private final static int directSize = 11;      // # direct pointers
    private final static int size = 16;
    
    public int length;                             // file size in bytes
    public short count;                            // # file-table entries pointing to this
    public short flag;                             // 0 = unused, 1 = used, ...
    public short direct[] = new short[directSize]; // direct pointers
    public short indirect;                         // a indirect pointer

    Inode( ) {                                     // a default constructor
        length = 0;
        count = 0;
        flag = 1;
        for ( int i = 0; i < directSize; i++ )
            direct[i] = -1;
        indirect = -1;
    }

    Inode( short iNumber )
    {   // retrieving inode from disk
        // design it by yourself.
        int blockNum = 1 + iNumber / size;

        byte[] iData = new byte[Disk.blockSize];
        SysLib.rawread(blockNum, iData);
        int offset = (iNumber % 16) * iNodeSize; //Calculate offset
        length = SysLib.bytes2int(iData,offset);
        offset += 4;
        count = SysLib.bytes2short(iData,offset);
        offset += 2;
        flag = SysLib.bytes2short(iData,offset);
        offset += 2;

        for(int i = 0; i <directSize; i++)
        {
            direct[i] = SysLib.bytes2short(iData, offset);
            offset += 2;
        }
        indirect = SysLib.bytes2short(iData,offset);
    }

    int toDisk( short iNumber ) {                  // save to disk as the i-th inode
        // save to disk as the i-th inode
        // design it by yourself.
        int offset = 0;
        int inodeOffset = (iNumber % 16) * iNodeSize; // Location of inode in block
        int blockOffset = 1 + iNumber / 16;           // Location of block on disk
        byte[] inodeData = new byte[iNodeSize];       // 32 Bytes in an inode, 16 inodes per block
        byte[] blockData = new byte[Disk.blockSize];  // For reading from disk

        SysLib.int2bytes(length, inodeData, offset);  // Store length
        offset += 4; // 4 bytes for int
        SysLib.short2bytes(count, inodeData, offset); // Store count
        offset += 2; // 2 bytes for short
        SysLib.short2bytes(flag, inodeData, offset);   // Store flag
        offset += 2; // 2 bytes for short

        for(int i = 0; i < directSize; i++)  // Start writing direct pointers
        {
            // Convert short entries to bytes, store with offset
            SysLib.short2bytes(direct[i], inodeData, offset);
            offset += 2; // Each short entry is 2 bytes
        }
        SysLib.short2bytes(indirect, inodeData, offset); // Write indirect block
        // Read whole block so you only rewrite 1 inode (32 bytes) of data instead
        // of overwriting whole block (512 bytes of data)
        SysLib.rawread(blockOffset, blockData);          // Read old data from disk
        System.arraycopy(inodeData, 0, blockData, inodeOffset, iNodeSize);
        SysLib.rawwrite(blockOffset, blockData);         // Write to disk
        return 0;
    }
    
    int getBlock(int index, short offset)
    {
        int block = index / Disk.blockSize; //Divide by 512 to find entry
        if(block < directSize) //Block is direct
        {
            if(direct[block] >= 0) //Block already exists
            {
                return -1;
            }
            if(direct[block -1 ] == -1 && block > 0)
            {
                return -2;
            }
            direct[block] = offset;
            return 0;
        }
        else //Indirect block
        {
            if(indirect < 0) //No indirect block allocated
            {
                return -3;
            }
            byte [] blockData = new byte[Disk.blockSize];
            SysLib.rawread(indirect,blockData);
            short indiBlock = SysLib.bytes2short(blockData,(block - directSize )* 2);
            if(indiBlock > 0)
            {
                return -1;
            }
            else
            {
                SysLib.short2bytes(offset,blockData, (block - directSize )* 2);
                SysLib.rawwrite(indirect, blockData);
            }
        }
        return 0;
    }
    
    // finds the block that the seek pointer is pointing too
    // takes in a pointer and return the index of the block
    int findBlock(int index) {
        int block = index/Disk.blockSize;

        if(block < directSize) {        // in direct block of inode
            return direct[block];
        }
        else if(indirect == -1) {       // indirect no pointer
            return -1;
        }

        // not in direct or inderect
        byte[] readData = new byte[Disk.blockSize]; // to read
        SysLib.rawread(indirect, readData);         // read

        // find offset (*2 is because pointers are short (2 bytes))
        int offset = (block - directSize) * 2;      // find offset
        return SysLib.bytes2short(readData, offset);// return short
    }
    
    // writes a block back to the disk
    // takes in a block number that needs to be writen to disk
    // return true or false based on succession 
    boolean setBlock(short blockNumber) {
        for(int i = 0; i < directSize; i++) {       // loop indirect
            if (direct[i] == -1)                    // if block is invalid
                return false;                       // can't update
        }

        if (indirect != -1)                         // no need to write back
            return false;
        else {
            indirect = blockNumber;                 // set indirect
            byte[] loadData = new byte[Disk.blockSize]; // temporary 

            // fill with default data 
            for(int i = 0; i < (Disk.blockSize/2); i++) {
                SysLib.short2bytes((short)-1, loadData, i*2);
            }
            // write to disl
            SysLib.rawwrite(blockNumber, loadData);
            return true;                            // success
        }
    }
    byte[] releaseBlock()
    {
        if (indirect >= 0)  //Indirect block points to something
        {
            byte[] blockData = new byte[Disk.blockSize]; //512 bytes
            SysLib.rawread(indirect, blockData);
            indirect = -1; //Indirect block doesn't point to anything anymore
            return blockData;
        }
        return null; //Indirect block did not point to anything
    }
}
