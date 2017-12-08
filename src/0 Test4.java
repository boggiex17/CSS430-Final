import java.util.*;

public class Test4 extends Thread {
    private boolean enabled;
    int testType;               // stores test type
    int dataBlocks = 200;       // stores number of blocks
    double readTime;            // stores total read time
    double writeTime;           // stores total write time
    private byte[] wbytes;      // stores write bytes
    private byte[] rbytes;      // stores read bytes
    private Random rand;        // stores random
    //DecimalFormat readTime = new DecimalFormat("#.00");

    // Constructor for Test4 to check that arguments passed in are valid
    public Test4(String[] args) {
        testType = 0;               // initialize test type
        if (args.length < 2) {      // check if less than 2 arguments
            SysLib.cout("\nToo few arguments...");      // print error message
            SysLib.exit();
        }
        if (args.length > 2) {      // check if more than 2 arguments
            SysLib.cout("\nNot enough arguments...");   // print error message
            SysLib.exit();
        }

        if(args[0].equals("enabled")) {          // check if 2nd argument is equal to enabled
            enabled = true;                 // set enabled
        }
        else if (args[0].equals("disabled")) {   // check if 2nd argument is equal to disabled
            enabled = false;                // sed disabled
        }
        else {                              // not 'enabled' or 'disabled'
            // print error
            SysLib.cout("\nImproper argument, must be 'enabled' or 'disabled')");
            SysLib.exit();
        }

        int test = Integer.parseInt(args[1]);
        if(test < 5 || test > 0) {          // sets the testType
            switch (test) {
                case 1:
                    testType = 1;           // case for test type 1
                    break;
                case 2:
                    testType = 2;           // case for test type 2
                    break;
                case 3:
                    testType = 3;           // case for test type 3
                    break;
                case 4:
                    testType = 4;           // case for test type 4
                    break;
            }
        }
        else {                              // test not (1 - 4)
            SysLib.cout("\nImproper test case...");     // error
            SysLib.exit();
        }
        wbytes = new byte[512];             // initialize write bytes
        rbytes = new byte[512];             // initialize read bytes
        rand = new Random();                // initialize random
        readTime = 0;
        writeTime = 0;
    }

    private void read(int blockId, byte[] buffer) {     // read
        if (enabled) {
            SysLib.cread(blockId, buffer);              // cache read
        } else {
            SysLib.rawread(blockId, buffer);            // raw read
        }
    }

    private void write(int blockId, byte[] buffer) {    // write
        if (enabled) {
            SysLib.cwrite(blockId, buffer);             // cache write
        } else {
            SysLib.rawwrite(blockId, buffer);           // raw write
        }
    }

    // function for Random Access (Read and Write many blocks randomly across
    // the disk. Verify the correctness of your disk cache.)
    private void randomAccess() {
        int[] blockIDs = new int[dataBlocks];

        for(int i = 0; i < dataBlocks; i++) {
            blockIDs[i] = Math.abs(rand.nextInt() % 512);   // create blockIDs
        }

        for(int i = 0; i < dataBlocks; i++) {           // loop blocks
            for(int j= 0; j < 512; j++) {               // loop wbytes array
                wbytes[j] = (byte)j;                    // set bytes
            }
            double startTime = new Date().getTime();    // start timer
            write(blockIDs[i], wbytes);                 // write bytes
            double endTime = new Date().getTime();      // end timer
            writeTime += (endTime - startTime);          // add to timer

        }
        for(int i = 0; i < dataBlocks; i++) {           // loop blocks
            double startTime = new Date().getTime();    // start timer
            read(blockIDs[i], rbytes);                  // read bytes
            double endTime = new Date().getTime();      // end timer
            readTime += (endTime - startTime);          // add to timer

            for(int j = 0; j < 512; j++) {              // loop through bytes
                if (rbytes[j] != wbytes[j]) {           // make sure data is same
                    SysLib.cerr("ERROR\n");          // error
                    SysLib.exit();
                }
            }
        }
    }

    // function for Localized Access (Read and Write a small selection of
    // blocks many times to get a high ration of cache hits.)
    private void localizedAccess() {
        for(int i = 0; i < (dataBlocks/10); i++) {
            for(int j = 0; j < 512; j++) {
                wbytes[j] = (byte)(i + j);                  // fill array
            }

            for(int j = 0; j < 1000; j += 100) {
                double startTime = new Date().getTime();    // start timer
                write(j, wbytes);                           // write
                double endTime = new Date().getTime();      // end timer
                writeTime += (endTime - startTime);          // add to timer
            }

            for(int j = 0; j < 1000; j += 100) {
                double startTime = new Date().getTime();    // start timer
                read(j, rbytes);                            // read
                double endTime = new Date().getTime();      // end timer
                readTime += (endTime - startTime);          // add to timer

                for(int k = 0; k < 512; k++) {              // loop through bytes
                    if (this.rbytes[k] != this.wbytes[k]) { // compare data
                        SysLib.cerr("ERROR\n");          // error
                        SysLib.exit();
                    }
                }
            }
        }
    }

    // function for Mixed Access (90% of the total disk operations should be
    // localized accesses and 10% should be random accesses.)
    private void mixedAccess() {
        int[] blockIDs = new int[dataBlocks];

        for(int i = 0; i < dataBlocks; i++) {           // loop blocks
            if (Math.abs(this.rand.nextInt() % 10) > 8) {   // if 10%
                blockIDs[i] = Math.abs(rand.nextInt() % 512);  // set blockID
            } else {                                        // 90%
                blockIDs[i] = Math.abs(rand.nextInt() % 10);   // set blockID
            }
        }

        for(int i = 0; i < dataBlocks; i++) {           // loop blocks
            for(int j = 0; j < 512; j++) {              // loop bytes
                wbytes[j] = (byte)j;                    // set wbytes
            }

            double startTime = new Date().getTime();    // start timer
            write(blockIDs[i], wbytes);                 // write
            double endTime = new Date().getTime();      // end timer
            writeTime += (endTime - startTime);          // add to timer
        }

        for(int i = 0; i < dataBlocks; i++) {           // loop blocks
            double startTime = new Date().getTime();    // start timer
            read(blockIDs[i], rbytes);                  // read
            double endTime = new Date().getTime();      // end timer
            readTime += (endTime - startTime);          // add to timer

            for(int j = 0; j < 512; j++) {              // loop bytes
                if (rbytes[j] != wbytes[j]) {           // compare data
                    SysLib.cerr("ERROR\n");          // error
                    SysLib.exit();
                }
            }
        }
    }

    // function for adversaryAccess (Generate disk accesses that do not make
    // good use of the disk cache at all i.e., purposely accessing blocks to
    // create cache misses)
    private void adversaryAccess() {
        for(int i = 0; i < (dataBlocks/10); i++) {          // loop 10% of blocks
            for(int j = 0; j < 512; j++) {                  // loop bytes
                wbytes[j] = (byte)j;                        // set wbytes
            }

            for(int j = 0; j < (dataBlocks/20); j++) {      // loop 5% of blocks
                double startTime = new Date().getTime();    // start timer
                write(i * 10 + j, wbytes);          // write
                double endTime = new Date().getTime();      // end timer
                writeTime += (endTime - startTime);         // add to timer
            }
        }

        for(int i = 0; i < (dataBlocks/10); i++) {          // loop 10% of blocks
            for(int j = 0; j < (dataBlocks/20); j++) {      // loop 5% of blocks
                double startTime = new Date().getTime();    // start timer
                read(i * (dataBlocks/20) + j, rbytes);  // read
                double endTime = new Date().getTime();      // end timer
                readTime += (endTime - startTime);          // add to timer

                for(int k = 0; k < 512; k++) {              // loop bytes
                    if (rbytes[k] != wbytes[k]) {           // compare data
                        SysLib.cerr("ERROR\n");          // error
                        SysLib.exit();
                    }
                }
            }
        }
    }

    // run the test by calling the corresponding function of the test case
    public void run() {
        SysLib.flush();

        switch (testType) {
            case 1:                     // Random access case
                randomAccess();
                SysLib.cout("\nRandom access ");
                break;
            case 2:                     // Localized access case
                localizedAccess();
                SysLib.cout("\nLocalized access ");
                break;
            case 3:                     // Mixed access case
                mixedAccess();
                SysLib.cout("\nMixed access ");
                break;
            case 4:                     // Adversary access case
                adversaryAccess();
                SysLib.cout("\nAdversary access ");
                break;
            default:                    // Error case
                SysLib.cout( "\nAn error occurred...");
                SysLib.exit();
        }
        getPerformance();
    }

    private void getPerformance() {
        // prints out results
        SysLib.cout("with cache " + (enabled ? "enabled" : "disabled"));
        SysLib.cout( "\n    Average read time = " + String.format("%.3f", readTime/dataBlocks) + " msec");
        SysLib.cout( "\n    Average write time = " + String.format("%.3f", writeTime/dataBlocks) + " msec\n");
        SysLib.exit();
    }
}
