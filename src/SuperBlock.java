
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

    public void format(int blocksNumber) {

    }

    public void sync() {

    }

    public int getFreeBlock() {
        // list not empty but is in range
        if (freeList > 0 && freeList < totalBlocks) {
            byte[] block = new byte[Disk.blockSize];
            SysLib.rawread(freeList, blockInfo);

        }
    }

    public boolean returnBlock(int blockNumber) {

    }
}