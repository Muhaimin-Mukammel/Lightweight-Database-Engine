package db_engine;

import java.nio.ByteBuffer;

public class Page {

    private final int pageId;
    private final byte[] data;

    public static final int PAGE_SIZE = Constants.PAGE_SIZE;

    public Page(int pageId) {
        this.pageId = pageId;
        this.data = new byte[PAGE_SIZE];
        ByteBuffer buf = ByteBuffer.wrap(data);
        buf.putInt(0, 0);
        buf.putInt(4, PAGE_SIZE);
    }

    public int getPageId() {
        return pageId;
    }

    public byte[] getData() {
        return data;
    }

    public int getSlotCount() {
        return ByteBuffer.wrap(data).getInt(0);
    }

    public void setSlotCount(int count) {
        ByteBuffer.wrap(data).putInt(0, count);
    }

    public int getFreeOffset() {
        return ByteBuffer.wrap(data).getInt(4);
    }

    public void setFreeOffset(int offset) {
        ByteBuffer.wrap(data).putInt(4, offset);
    }

    public int slotDirectoryOffset(int slotId) {
        return Constants.PAGE_HEADER_SIZE + slotId * Constants.SLOT_ENTRY_SIZE;
    }

    public int slotDirectoryEnd() {
        return slotDirectoryOffset(getSlotCount());
    }
}