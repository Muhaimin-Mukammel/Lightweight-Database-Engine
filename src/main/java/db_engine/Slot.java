package db_engine;

public class Slot {

    int offset;
    int size;
    boolean deleted;

    public Slot(int offset, int size, boolean deleted) {
        this.offset = offset;
        this.size = size;
        this.deleted = deleted;
    }

    public int getOffset() {
        return offset;
    }

    public int getSize() {
        return size;
    }

    public boolean isDeleted() {
        return deleted;
    }
}