package db_engine;

public class Slot {

    int offset;
    int size;
    boolean deleted;

    public Slot(int offset, int size) {
        this.offset = offset;
        this.size = size;
        this.deleted = false;
    }
}