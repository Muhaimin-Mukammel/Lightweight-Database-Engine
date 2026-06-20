package db_engine;

import java.io.IOException;
import java.io.RandomAccessFile;

public class SuperBlock {

    public long catalogOffset;
    public long catalogSize;
    public long pageStartOffset;
    public int nextPageId;

    public SuperBlock() {
        this.catalogOffset = Constants.SUPERBLOCK_SIZE;
        this.catalogSize = 0;
        this.pageStartOffset = Constants.SUPERBLOCK_SIZE + Constants.CATALOG_RESERVED_SIZE;
        this.nextPageId = 0;
    }

    public void write(RandomAccessFile raf) throws IOException {
        raf.seek(0);
        raf.writeLong(catalogOffset);
        raf.writeLong(catalogSize);
        raf.writeLong(pageStartOffset);
        raf.writeInt(nextPageId);
    }

    public static SuperBlock read(RandomAccessFile raf) throws IOException {
        SuperBlock sb = new SuperBlock();
        raf.seek(0);
        sb.catalogOffset = raf.readLong();
        sb.catalogSize = raf.readLong();
        sb.pageStartOffset = raf.readLong();
        sb.nextPageId = raf.readInt();
        return sb;
    }
}