package db_engine;

import java.io.RandomAccessFile;
import java.util.Map;

public class StorageEngine {

    private final RandomAccessFile raf;
    private final SuperBlock superBlock;
    private final CatalogManager catalog;
    private final PageManager pageManager;

    public StorageEngine(String file) throws Exception {
        this.raf = new RandomAccessFile(file, "rw");
        boolean isNew = raf.length() == 0;

        if (isNew) {
            this.superBlock = new SuperBlock();
            this.superBlock.write(raf);
        } else {
            this.superBlock = SuperBlock.read(raf);
        }

        this.pageManager = new PageManager();
        this.catalog = new CatalogManager(raf, superBlock);
        catalog.load();
        rebuildIndex();
    }

    public Page readPage(int pageId) throws Exception {
        long offset = superBlock.pageStartOffset + (long) pageId * Constants.PAGE_SIZE;
        Page page = new Page(pageId);
        if (offset < raf.length()) {
            raf.seek(offset);
            raf.readFully(page.getData());
        }
        return page;
    }

    public void writePage(Page page) throws Exception {
        long offset = superBlock.pageStartOffset + (long) page.getPageId() * Constants.PAGE_SIZE;
        raf.seek(offset);
        raf.write(page.getData());
    }

    public int allocatePage() throws Exception {
        int pageId = superBlock.nextPageId;
        superBlock.nextPageId++;
        superBlock.write(raf);
        return pageId;
    }

    public int pageCount() {
        return superBlock.nextPageId;
    }

    public PageManager pages() {
        return pageManager;
    }

    public CatalogManager catalog() {
        return catalog;
    }

    public void rebuildIndex() throws Exception {
        for (Map.Entry<String, TableMeta> entry : catalog.getTables().entrySet()) {
            TableMeta table = entry.getValue();
            table.getIndex().clear();

            int maxPages = superBlock.nextPageId;
            for (int pageId = 0; pageId < maxPages; pageId++) {
                Page page = readPage(pageId);
                int slotCount = page.getSlotCount();

                for (int slot = 0; slot < slotCount; slot++) {
                    byte[] record = pageManager.read(page, slot);
                    if (record == null) {
                        continue;
                    }
                    String pk = RowSerializer.extractPrimaryKey(record, table);
                    if (pk == null || pk.isEmpty()) {
                        continue;
                    }
                    table.getIndex().put(pk, new HashIndex.Location(pageId, slot));
                }
            }
        }
    }
}