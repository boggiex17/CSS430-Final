// Completed =========================================================================

import java.util.Arrays;

public class Directory {
    private static int maxChars = 30; // max characters of each file name

    // Directory entries
    private int fsize[];        // each element stores a different file size.
    private char fnames[][];    // each element stores a different file name.

    public Directory( int maxInumber ) {  // directory constructor
        fsize = new int[maxInumber];      // maxInumber = max files
        for ( int i = 0; i < maxInumber; i++ )
            fsize[i] = 0;                 // all file size initialized to 0
        fnames = new char[maxInumber][maxChars];
        for (int i = 0; i < maxChars; i++) {
            for (int j = 0; j < maxInumber; j++) {
                fnames[i][j] = '\0';
            }
        }
        String root = "/";                // entry(inode) 0 is "/"
        fsize[0] = root.length( );        // fsize[0] is the size of "/".
        root.getChars( 0, fsize[0], fnames[0], 0 ); // fnames[0] includes "/"
    }

    // assumes data[] received directory information from disk
    // initializes the Directory instance with this data[]
    public int bytes2directory( byte data[] ) {

        if (data.length == 0)             // no data
            return -1;

        int offset = 0;
        for(int i = 0; i < fsize.length; i++) {
            fsize[i] = SysLib.bytes2int(data, offset);
            offset += 4;
        }

        for(int i = 0; i < fnames.length; i++){
            String fname = new String(data, offset, maxChars*2);
            fname.getChars(0, fsize[i], fnames[i], 0);
            offset += maxChars * 2;
        }
        return 1;                       // ran successfully
    }

    // converts and return Directory information into a plain byte array
    // this byte array will be written back to disk
    // note: only meaningfull directory information should be converted
    // into bytes.
    public byte[] directory2bytes( ) {
        int intSize = fsize.length * 4; //Fsize is an int array, with .length entries, 4 bytes each
        int charSize = fsize.length * 2 * maxChars; //Up to 30 characters per entry, .length entries, 2 bytes each
        byte [] toByte = new byte[intSize + charSize ]; //Initialize with proper size
        int offset = 0;

        for(int i = 0; i < fsize.length; i++)
        {
            SysLib.int2bytes(fsize[i], toByte, offset); //SysLib int2bytes does calculation for us
            offset += 4; //4 byte offset because of int size
        }
        for(int i = 0; i < fnames.length; i++)
        {
            String entryName = new String(fnames[i],0, fsize[i]);
            byte [] stSize = entryName.getBytes(); //Returns the sequence of bytes representing this file name
            System.arraycopy(stSize,0, toByte, offset, stSize.length);
            offset += maxChars * 2; //Offset is the maximum number of characters * 2 bytes per character
        }
        return toByte;
    }

    // filename is the one of a file to be created.
    // allocates a new inode number for this filename
    public short ialloc( String filename ) {
        //char[] file = filename.toCharArray();

        for(short i = 0; i < fsize.length; i++) {   // loop fsize to find slot
            if(fnames[0][i] == '\0') {              // slot found
                if(filename.length() > maxChars)    // filename is bigger than maxChars (30)
                    fsize[i] = maxChars;            // set 30 as size
                else
                    fsize[i] = filename.length();   // filename size is <= maxChars

                // Copy characters into fnames
                filename.getChars(0, fsize[i], fnames[i],0);
                return i;                           // return index
            }
        }
        return -1;
    }

    // deallocates this inumber (inoe number)
    // deletes the corresponding file
    public boolean ifree( short iNumber ) {
       if (iNumber < 0 || iNumber > fsize.length)    // iNumber is out of bounds
           return false;

       if(fnames[iNumber][0] == '\0') {              // empty
           return false;
       }
       else {
           for(int i = 0; i < maxChars; i++) {       // resets fnames
               fnames[iNumber][i] = '\0';
           }
       }
       fsize[iNumber] = 0;                   // set to be deleted
           for(int i = 0; i < maxChars; i++)     // loop name
               fnames[iNumber][i] = 0;           // reset name
           return true;
    }

    // returns the inumber corresponding to this filename
    public short namei( String filename ) {
        char[] temp = filename.toCharArray();       // convert to char array
        for(short i = 0; i < fnames.length; i++) {  // loop names
            if(Arrays.equals(temp, fnames[i]))      // if match
                return i;
        }
        return -1;
    }
}
/*
// Completed =========================================================================

import java.util.Arrays;

public class Directory {
    private static int maxChars = 30; // max characters of each file name

    // Directory entries
    private int fsize[];        // each element stores a different file size.
    private char fnames[][];    // each element stores a different file name.

    public Directory( int maxInumber ) {  // directory constructor
        fsize = new int[maxInumber];      // maxInumber = max files
        for ( int i = 0; i < maxInumber; i++ )
            fsize[i] = 0;                 // all file size initialized to 0
        fnames = new char[maxInumber][maxChars];
        String root = "/";                // entry(inode) 0 is "/"
        fsize[0] = root.length( );        // fsize[0] is the size of "/".
        root.getChars( 0, fsize[0], fnames[0], 0 ); // fnames[0] includes "/"
    }

    // assumes data[] received directory information from disk
    // initializes the Directory instance with this data[]
    public int bytes2directory( byte data[] ) {

        if (data.length == 0)             // no data
            return -1;

        int offset = 0;
        for(int i = 0; i < fsize.length; i++) {
            fsize[i] = SysLib.bytes2int(data, offset);
            offset += 4;
        }

        for(int i = 0; i < fnames.length; i++){
            String fname = new String(data, offset, maxChars*2);
            fname.getChars(0, fsize[i], fnames[i], 0);
            offset += maxChars * 2;
        }
        return 1;                       // ran successfully
    }

    // converts and return Directory information into a plain byte array
    // this byte array will be written back to disk
    // note: only meaningfull directory information should be converted
    // into bytes.
    public byte[] directory2bytes( ) {
        int intSize = fsize.length * 4; //Fsize is an int array, with .length entries, 4 bytes each
        int charSize = fsize.length * 2 * maxChars; //Up to 30 characters per entry, .length entries, 2 bytes each
        byte [] toByte = new byte[intSize + charSize ]; //Initialize with proper size
        int offset = 0;

        for(int i = 0; i < fsize.length; i++)
        {
            SysLib.int2bytes(fsize[i], toByte, offset); //SysLib int2bytes does calculation for us
            offset += 4; //4 byte offset because of int size
        }
        for(int i = 0; i < fnames.length; i++)
        {
            String entryName = new String(fnames[i],0, fsize[i]);
            byte [] stSize = entryName.getBytes(); //Returns the sequence of bytes representing this file name
            System.arraycopy(stSize,0, toByte, offset, stSize.length);
            offset += maxChars * 2; //Offset is the maximum number of characters * 2 bytes per character
        }
        return toByte;
    }

    // filename is the one of a file to be created.
    // allocates a new inode number for this filename
    public short ialloc( String filename ) {
        for(short i = 0; i < fsize.length; i++) {   // loop fsize to find slot
            if(fsize[i] == 0) {                     // slot found
                if(filename.length() > maxChars)    // filename is bigger than maxChars (30)
                    fsize[i] = maxChars;            // set 30 as size
                else
                    fsize[i] = filename.length();   // filename size is <= maxChars

                // Copy characters into fnames
                filename.getChars(0, fsize[i], fnames[i],0);
                return i;                           // return index
            }
        }
        return -1;
    }

    // deallocates this inumber (inoe number)
    // deletes the corresponding file
    public boolean ifree( short iNumber ) {
       if(fsize[iNumber] > 0 && iNumber > 0) {   // if valid
           fsize[iNumber] = 0;                   // set to be deleted
           for(int i = 0; i < maxChars; i++)     // loop name
               fnames[iNumber][i] = 0;           // reset name
           return true;
       }
       else
           return false;
    }

    // returns the inumber corresponding to this filename
    public short namei( String filename ) {
        char[] temp = filename.toCharArray();       // convert to char array
        for(short i = 0; i < fnames.length; i++) {  // loop names
            if(Arrays.equals(temp, fnames[i]))      // if match
                return i;
        }
        return -1;
    }
}
*/
