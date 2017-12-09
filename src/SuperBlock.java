
public class SuperBlock {
    private final int defaultInodeBlocks = 64;
    public int totalBlocks; // the number of disk blocks
    public int totalInodes; // the number of inodes
    public int freeList;    // the block number of the free list's head

    public SuperBlock( int diskSize ) {
        // read superblock from disk
        byte[] superBlock = new byte[Disk.blockSize];
        SysLib.rawread(0, superBlock);
        totalBlocks = SysLib.bytes2int(superBlock,0);
        totalInodes = SysLib.bytes2int(superBlock,4);
        freeList = SysLib.bytes2int(superBlock, 8);

        if(totalBlocks == diskSize && totalInodes > 0 && freeList >= 2) {
            // disk contents are valid
            return;
        }
        else {
            // need to format disk
            totalBlocks = diskSize;
            format(defaultInodeBlocks);
        }

    }

    // format the number of blocks being passes in and replace them with
    // empty or new Inodes, update freeList
    // param-is the number of blocks to format
    public void format(int blocksNumber) {
        if(blocksNumber < 0) {          // check if valid
            return;
        }

        for(short i = 0; i < blocksNumber; i++) {
            Inode temp = new Inode();   // create inode
            temp.flag = 0;              // set flat to unused
            temp.toDisk(i);             // write to disk
        }

        // blockNumber/16 gets block we are in + 1 gets goes to next
        // block and + 1 for the super block
        int nextFreeBlock = blocksNumber/16 + 1 + 1;        // get next free block value
        freeList = nextFreeBlock;       // set next free pointer
        for(int i = nextFreeBlock; i < totalBlocks; i++) {  // loop blocks
            byte[] temp = new byte[Disk.blockSize];         // create temp byte array
            fillBlock(temp);                         // fill array
            SysLib.int2bytes(i + 1, temp, 0);  // turn to byte
            SysLib.rawwrite(i, temp);               // write to disk
        }
        sync();                                     // sync disk
    }
    
    // this function just takes in a byte array and fills it with
    // zeroes
    public void fillBlock(byte[] block) {
        for(int i = 0; i < Disk.blockSize; i++)     // loop array
            block[i] = 0;                           // fill with 0
    }
    
    //Sync updates the disk with any changes made to superblock
    public void sync()
    {
        byte[] data = new byte[Disk.blockSize]; //512 bytes per block
        int offset = 0;
        SysLib.int2bytes(totalBlocks, data, offset); //convert totalBlocks to bytes
        offset += 4; //4 bytes per int
        SysLib.int2bytes(totalInodes, data, offset); //convert totalInodes to bytes
        offset += 4; //4 bytes per int
        SysLib.int2bytes(freeList, data, offset); //convert freeList to bytes
        SysLib.rawwrite(0, data); //Update disk
    }

    // retrives the index of the next free block and replaces the freeList
    // with the index of the next available free block
    public int getFreeBlock() {
        // list not empty but is in range
        if (freeList > 0 && freeList < totalBlocks) {
            byte[] block = new byte[Disk.blockSize];    // store free block
            int freeBlock = freeList;
            SysLib.rawread(freeBlock, block);           // read free block
            freeList = SysLib.bytes2int(block, 0);   // update free list
            SysLib.int2bytes(0, block, 0);       // back to bytes
            SysLib.rawwrite(freeBlock, block);          // write back to disk
            return freeBlock;                           // return index
        }
        return -1;
    }
    //Enqueue the given block to the freelist
    public boolean returnBlock(int blockNumber) {
        if(blockNumber < 0 ||  blockNumber > totalBlocks) //Block number outside of boudns
        {
            return false;
        }
        byte[] data = new byte[Disk.blockSize];
        for(int i = 0; i < data.length; i++) //Clear data
        {
            data[i] = (byte)0;
        }
        SysLib.int2bytes(freeList, data,0);
        SysLib.rawwrite(blockNumber, data);
        freeList = blockNumber; //Freelist head is now parameter block number
        return true;
    }
}
