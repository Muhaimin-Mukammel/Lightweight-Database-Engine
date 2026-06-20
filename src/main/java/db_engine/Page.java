package db_engine;

public class Page {

    private final int pageId;
    private final byte[] data;

    public static final int PAGE_SIZE = 4096;

    public Page(int pageId) {
        this.pageId = pageId;
        this.data = new byte[PAGE_SIZE];
    }

    public int getPageId() {
        return pageId;
    }

    public byte[] getData() {
        return data;
    }
}