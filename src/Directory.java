
public class Directory {
    private static int maxChars = 30; // max characters of each file name

    // Directory entries
    private int fsize[];        // each element stores a different file size.
    private char fnames[][];    // each element stores a different file name.

    public Directory( int maxInumber ) { // directory constructor
        fsize = new int[maxInumber];     // maxInumber = max files
        for ( int i = 0; i < maxInumber; i++ )
            fsize[i] = 0;                 // all file size initialized to 0
        fnames = new char[maxInumber][maxChars];
        String root = "/";                // entry(inode) 0 is "/"
        fsize[0] = root.length( );        // fsize[0] is the size of "/".
        root.getChars( 0, fsize[0], fnames[0], 0 ); // fnames[0] includes "/"
    }

    public int bytes2directory( byte data[] ) {
        // assumes data[] received directory information from disk
        // initializes the Directory instance with this data[]

        if (data.length == 0)           // no data
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

    public byte[] directory2bytes( ) {
        // converts and return Directory information into a plain byte array
        // this byte array will be written back to disk
        // note: only meaningfull directory information should be converted
        // into bytes.
    }

    public short ialloc( String filename ) {
        // filename is the one of a file to be created.
        // allocates a new inode number for this filename
        short inodeNum = -1; //Default to -1 in case no free slot is found
        for(short i = 0; i < fsize.length; i++) //Loop through fsize until free slot is found
        {
            if(fsize[i] == 0) //Free slot
            {
                if(filename.length() > maxChars) //Filename is bigger than maxChars (30)
                {
                    fsize[i] = maxChars; //Copy 30 characters of filename
                }
                else
                {
                    fsize[i] = filename.length(); //filename size is <= maxChars
                }
                inodeNum = i;
                filename.getChars(0, fsize[i], fnames[i],0); //Copy characters into fnames
                break; //break out of for loop and return index
            }
        }
        return inodeNum;
    }

    // deallocates this inumber (inoe number)
    // deletes the corresponding file
    public boolean ifree( short iNumber ) {
       if(fsize[iNumber] > 0 && iNumber > 0) {      // if valid
           fsize[iNumber] = 0;                      // set to be deleted
           for(int i = 0; i < maxChars; i++)        // loop name
               fnames[iNumber][i] = 0;              // reset name
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
