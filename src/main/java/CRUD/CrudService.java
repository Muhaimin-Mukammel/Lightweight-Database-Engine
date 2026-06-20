package CRUD;

import db_engine.HashIndex;
import db_engine.Page;
import db_engine.PageManager;
import db_engine.Row;
import db_engine.RowDeserializer;
import db_engine.RowSerializer;
import db_engine.StorageEngine;
import db_engine.TableMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CrudService {

    private final StorageEngine engine;
    private final PageManager pageManager;

    public CrudService(StorageEngine engine) {
        this.engine = engine;
        this.pageManager = engine.pages();
    }

    public TableMeta getTable(String name) {
        if (name == null) {
            return null;
        }
        return engine.catalog().getTables().get(name);
    }

    public Map<String, TableMeta> listTables() {
        return engine.catalog().getTables();
    }

    public void createTable(TableMeta table) throws Exception {
        engine.catalog().addTable(table);
        engine.catalog().save();
    }

    public void insert(String tableName, Object pk, Row row) throws Exception {
        TableMeta table = getTable(tableName);
        if (table == null) {
            throw new RuntimeException("Table not found: " + tableName);
        }

        byte[] record = RowSerializer.serialize(row, table);

        if (record.length > db_engine.Constants.PAGE_SIZE - db_engine.Constants.PAGE_HEADER_SIZE - db_engine.Constants.SLOT_ENTRY_SIZE) {
            throw new RuntimeException("Record too large for a single page");
        }

        Page targetPage = null;
        int pageCount = engine.pageCount();

        for (int pageId = 0; pageId < pageCount; pageId++) {
            Page candidate = engine.readPage(pageId);
            if (pageManager.canFit(candidate, record.length)) {
                targetPage = candidate;
                break;
            }
        }

        if (targetPage == null) {
            int newPageId = engine.allocatePage();
            targetPage = new Page(newPageId);
        }

        int slotId = pageManager.insert(targetPage, record);
        engine.writePage(targetPage);

        table.getIndex().put(
                pk.toString(),
                new HashIndex.Location(targetPage.getPageId(), slotId)
        );
    }

    public Row get(String tableName, Object pk) throws Exception {
        TableMeta table = getTable(tableName);
        if (table == null) {
            return null;
        }

        HashIndex.Location loc = table.getIndex().get(pk.toString());
        if (loc == null) {
            return null;
        }

        Page page = engine.readPage(loc.getPageId());
        byte[] data = pageManager.read(page, loc.getSlotId());
        if (data == null) {
            return null;
        }
        return RowDeserializer.deserialize(data, table);
    }

    public void delete(String tableName, Object pk) throws Exception {
        TableMeta table = getTable(tableName);
        if (table == null) {
            return;
        }

        HashIndex.Location loc = table.getIndex().get(pk.toString());
        if (loc == null) {
            return;
        }

        Page page = engine.readPage(loc.getPageId());
        pageManager.delete(page, loc.getSlotId());
        engine.writePage(page);
        table.getIndex().remove(pk.toString());
    }

    public List<Row> getAll(String tableName) throws Exception {
        TableMeta table = getTable(tableName);
        if (table == null) {
            return new ArrayList<>();
        }

        List<Row> result = new ArrayList<>();
        for (String pk : table.getIndex().raw().keySet()) {
            Row r = get(tableName, pk);
            if (r != null) {
                result.add(r);
            }
        }
        return result;
    }
}