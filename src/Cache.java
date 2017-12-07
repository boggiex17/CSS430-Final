import java.util.*;

public class Cache {

    private Entry[] pageTable = null;                   // stores all entries in an entry array
    int lastVictim = 0;                                 // keep track of last victim
    int bufSize = 0;                                    // keeps track of block size

    public Cache(int blockSize, int cacheBlocks) {      // constructor

        bufSize = blockSize;
        pageTable = new Entry[cacheBlocks];             // create a table
        for(int i = 0; i < pageTable.length; i++) {     // loop through table
            pageTable[i] = new Entry(blockSize);        // initialize entries
        }
    }

    private class Entry {
        int blockID;                                // stores ID of entry
        byte[] data;                                // stores data of entry
        int referenceBit;                           // keeps track of reference bit
        int modifyBit;                              // keeps track of modify bit

        public Entry(int blockSize){
            blockID = -1;                           // initialize blockID
            data = new byte[blockSize];             // initialize data block
            referenceBit = 0;                       // initialize reference bit
            modifyBit = 0;                          // initialize modify bit
        }
    }

    // finds the next available free page in the table
    private int findFreePage() {
        for(int i = 0; i < pageTable.length; i++) { // loop page table
            if(pageTable[i].blockID == -1) {        // if page index is free
                return i;                           // return index
            }
        }
        return -1;                                  // free page not found
    }

    // finds a victim that can be replaced in the page table
    // uses resetBits function to makes sure reference bits are reset
    private int nextVictim() {
        int victim = 0;                         // store current victim
        int cur;                                // stores current array index
        int priority = -1;                      // holds priority of replacements

        // loop through all array elements starting at last victim + 1
        for (int i = lastVictim; i < lastVictim + pageTable.length; i++){
            cur = i % pageTable.length;         // current array index

            // neither recently used nor modified ================================================
            if(pageTable[cur].referenceBit == 0 && pageTable[cur].modifyBit == 0){
                resetBits(cur);
                lastVictim = cur;
                return lastVictim;
            }
            // not recently used but modified ====================================================
            else if (pageTable[cur].referenceBit == 0 && pageTable[cur].modifyBit == 1 && priority > 2) {
                priority = 2;
                victim = cur;
            }
            // recently used but not modified ====================================================
            else if (pageTable[cur].referenceBit == 1 && pageTable[cur].modifyBit == 0 && priority > 3) {
                priority = 3;
                victim = cur;
            }
            // recently used and modified ========================================================
            else if (priority == -1) {
                priority = 4;
                victim = cur;
            }
        }

        resetBits(victim);                  // reset necessary bits
        lastVictim = victim;                // set last victim
        return lastVictim;                  // return victim
    }

    // makes sure that all the reference bits that have to be set to 0 because the
    // entry was considered a victim are set to 0
    private void resetBits(int endIndex) {
        int stopLoop = endIndex;            // know when to stop the loop

        if (endIndex <= lastVictim) {       // if index of array started over from beginning
            stopLoop = endIndex + pageTable.length;
        }

        // loops all the entries with from last victim to current victim setting reference bits to 0
        for(int i = lastVictim; i < stopLoop; i++) {
            if(pageTable[i % pageTable.length].referenceBit == 1) {
                pageTable[i % pageTable.length].referenceBit = 0;
            }
        }
    }

    // writes back a block that will be replaced to disk
    private void writeBack(int victimEntry) {
        if(pageTable[victimEntry].modifyBit == 1) { // if modify bit is set
            // write back to Disk
            SysLib.rawwrite(pageTable[victimEntry].blockID, pageTable[victimEntry].data);
            pageTable[victimEntry].modifyBit = 0;   // sets modify bit to 0
        }
        pageTable[victimEntry].referenceBit = 0;
    }

    // Scans table to see if data is in memory, if in memory reads in the contents
    // of buffer, if not in memory reads corresponding disk block from the disk
  w  // and loads it into memory and adds to page table
    public synchronized boolean read(int blockId, byte buffer[]) {

        if(blockId < 0)                 // ID outside of limit
            return false;               // error

        for(int i = 0; i < pageTable.length; i++) {   // search table for blockID
            if(blockId == pageTable[i].blockID) {     // if found
                // read data
                System.arraycopy(pageTable[i].data, 0, buffer, 0, bufSize);

                pageTable[i].referenceBit = 1;        // set reference bit to 1
                return true;                          // no errors
            }
        }

        int page = findFreePage();              // finds index of free page
        if(page != -1) {                        // check if free page exists
            // load data
            SysLib.rawread(blockId, pageTable[page].data);
            // read data
            System.arraycopy(pageTable[page].data, 0, buffer, 0, bufSize);

            pageTable[page].referenceBit = 1;
            return true;                              // success
        }
        else {                                        // no free pages
            int found = nextVictim();                 // find victim
            writeBack(found);                         // write victim back
            // load data
            SysLib.rawread(blockId, pageTable[found].data);
            // read data
            System.arraycopy(pageTable[found].data, 0, buffer, 0,bufSize);
            pageTable[found].referenceBit = 1;        // reset reference bit
            pageTable[found].modifyBit = 0;           // reset modify bit
            return true;                              // success
        }
    }

    public synchronized boolean write(int blockId, byte buffer[]) {

        if(blockId < 0)                                 // if block id illegal
            return false;                               // out of bounds

        for(int i = 0; i < pageTable.length; i++) {     // loop through table
            if(blockId == pageTable[i].blockID) {       // if block id match
                // write data
                System.arraycopy(buffer, 0, pageTable[i].data, 0, bufSize);

                pageTable[i].referenceBit = 1;          // set reference bit
                pageTable[i].modifyBit = 1;             // set modify bit to 1
                return true;                            // no errors
            }
        }

        int page = findFreePage();                      // find free page
        if(page != -1) {                                // if free page exists
            pageTable[page].blockID = blockId;          // set block id
            // write data
            System.arraycopy(buffer, 0, pageTable[page].data, 0, bufSize);
            pageTable[page].referenceBit = 1;              // set reference bit
            pageTable[page].modifyBit = 1;                 // set modify bit to 1
            return true;                                // no errors
        }
        else {                                          // no free page
            int found = nextVictim();                   // victim found
            writeBack(found);
            pageTable[found].blockID = blockId;          // set block id
            // write data
            System.arraycopy(buffer, 0, pageTable[found].data, 0, bufSize);
            pageTable[found].referenceBit = 1;          // set reference bit
            pageTable[found].modifyBit = 1;             // set modify bit
            return true;                                // no errors
        }
    }

    // writes back all dirty blocks to Disk.java and forces Disk.java to write back
    // all contents to the DISK file
    public synchronized void sync() {

        for(int i = 0; i < pageTable.length; i++) {     // loops page table
            if(pageTable[i].modifyBit == 1) {           // if modify bit is set
                // write to disk
                SysLib.rawwrite(pageTable[i].blockID, pageTable[i].data);
            }
        }
        SysLib.sync();                                  // sync disk
    }

    // writes back all dirty blocks to Disk.java, forces Disk.java to write back
    // all contents to DISK file and wipes all cached blocks
    public synchronized void flush() {

        for(int i = 0; i < pageTable.length; i++) {     // loops page table
            if(pageTable[i].modifyBit == 1) {           // if modify bit is set
                // write to disk
                SysLib.rawwrite(pageTable[i].blockID, pageTable[i].data);

                pageTable[i].blockID = -1;          // reset blockId
                pageTable[i].referenceBit = 0;      // reset reference bit to default value
                pageTable[i].modifyBit = 0;         // reset modify bit to default value
            }
        }
        SysLib.sync();                              // sync disk
    }
}
