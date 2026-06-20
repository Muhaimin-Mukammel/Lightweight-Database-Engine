package db_engine;

import java.io.*;

public class SuperBlock {

    public long catalogOffset;
    public long catalogSize;
    public long pageStartOffset;
    public int nextPageId;

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