
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
        int blockOffSet = (iNumber % 16) *iNodeSize; //Location of inode in block
        int blockNum = 1 + iNumber / 16; //Location of block on disk
        byte[] nodeData = new byte[iNodeSize]; //32 Bytes in an inode, 16 inodes per block
        byte[] blockData = new byte[Disk.blockSize]; //For reading from disk

        SysLib.int2bytes(length, nodeData, offset); //Store length
        offset += 4; //4 bytes per int
        SysLib.short2bytes(count, nodeData, offset); //Store count
        offset += 2; //2 bytes for short
        SysLib.short2bytes(flag, nodeData, offset); //Store flag
        offset += 2; //2 bytes for short

        for(int i = 0; i < directSize; i++)  //Start writing direct pointers
        {
            SysLib.short2bytes(direct[i],nodeData, offset); //Convert short entries to bytes, store with offset
            offset += 2; //Each short entry is 2 bytes
        }
        SysLib.short2bytes(indirect, nodeData, offset); //Write indirect block
        SysLib.rawread(blockNum,blockData); //Read old data from disk
        System.arraycopy(nodeData, 0, blockData, blockOffSet, iNodeSize);
        SysLib.rawwrite(blockNum, blockData); //Write to disk
        return 0; 
    }
}
