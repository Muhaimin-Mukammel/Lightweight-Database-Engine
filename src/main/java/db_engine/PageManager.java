package db_engine;

import java.nio.ByteBuffer;

public class PageManager {

    public Slot readSlot(Page page, int slotId) {
        ByteBuffer buf = ByteBuffer.wrap(page.getData());
        int dirOffset = page.slotDirectoryOffset(slotId);
        int offset = buf.getInt(dirOffset);
        int size = buf.getInt(dirOffset + 4);
        boolean deleted = buf.get(dirOffset + 8) != 0;
        return new Slot(offset, size, deleted);
    }

    private void writeSlot(Page page, int slotId, int offset, int size, boolean deleted) {
        ByteBuffer buf = ByteBuffer.wrap(page.getData());
        int dirOffset = page.slotDirectoryOffset(slotId);
        buf.putInt(dirOffset, offset);
        buf.putInt(dirOffset + 4, size);
        buf.put(dirOffset + 8, (byte) (deleted ? 1 : 0));
    }

    private int freeSpaceForNewSlot(Page page) {
        int slotCount = page.getSlotCount();
        int dirEndAfterNewSlot = page.slotDirectoryOffset(slotCount + 1);
        int freeOffset = page.getFreeOffset();
        if (dirEndAfterNewSlot > freeOffset) {
            return 0;
        }
        return freeOffset - dirEndAfterNewSlot;
    }

    public boolean canFit(Page page, int recordSize) {
        int slotCount = page.getSlotCount();
        for (int i = 0; i < slotCount; i++) {
            Slot slot = readSlot(page, i);
            if (slot.isDeleted() && slot.getSize() >= recordSize) {
                return true;
            }
        }
        return freeSpaceForNewSlot(page) >= recordSize;
    }

    public int insert(Page page, byte[] record) {
        int slotCount = page.getSlotCount();

        for (int i = 0; i < slotCount; i++) {
            Slot slot = readSlot(page, i);
            if (slot.isDeleted() && slot.getSize() >= record.length) {
                System.arraycopy(record, 0, page.getData(), slot.getOffset(), record.length);
                writeSlot(page, i, slot.getOffset(), record.length, false);
                return i;
            }
        }

        if (freeSpaceForNewSlot(page) < record.length) {
            throw new RuntimeException("Page full");
        }

        int writeAt = page.getFreeOffset() - record.length;

        System.arraycopy(record, 0, page.getData(), writeAt, record.length);
        writeSlot(page, slotCount, writeAt, record.length, false);
        page.setSlotCount(slotCount + 1);
        page.setFreeOffset(writeAt);

        return slotCount;
    }

    public byte[] read(Page page, int slotId) {
        if (slotId < 0 || slotId >= page.getSlotCount()) {
            return null;
        }
        Slot slot = readSlot(page, slotId);
        if (slot.isDeleted()) {
            return null;
        }
        byte[] result = new byte[slot.getSize()];
        System.arraycopy(page.getData(), slot.getOffset(), result, 0, slot.getSize());
        return result;
    }

    public void delete(Page page, int slotId) {
        if (slotId < 0 || slotId >= page.getSlotCount()) {
            return;
        }
        Slot slot = readSlot(page, slotId);
        writeSlot(page, slotId, slot.getOffset(), slot.getSize(), true);
    }
}