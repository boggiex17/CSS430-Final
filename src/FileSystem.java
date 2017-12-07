public class FileSystem {
    private SuperBlock superblock;
    private Directory directory;
    private FileTable filetable;

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

    void sunc() {

    }

    boolean format(int files){
        return false;
    }

    FileTableEntry open(String filename, String mode) {
        FileTableEntry test = filetable.falloc(filename, mode);

        return test;
    }

    boolean close(FileTableEntry ftEnt) {
        return false;
    }

    int fsize(FileTableEntry ftEnt) {
        return 0;
    }

    int read(FileTableEntry ftEnt, byte[] buffer) {
        return 0;
    }

    int writ(FileTableEntry ftEnt, byte[] buffer) {
        return 0;
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
