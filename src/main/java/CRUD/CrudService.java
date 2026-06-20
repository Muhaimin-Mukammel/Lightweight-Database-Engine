package CRUD;

import db_engine.*;

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

    // ---------------- TABLE ----------------

    public TableMeta getTable(String name) {
        if (name == null) return null;
        return engine.catalog().getTables().get(name);
    }

    public Map<String, TableMeta> listTables() {
        return engine.catalog().getTables();
    }

    // ---------------- CREATE ----------------

    public void createTable(TableMeta table) throws Exception {
        engine.catalog().addTable(table);
        engine.catalog().save();
    }

    // ---------------- INSERT ----------------

    public void insert(String tableName, Object pk, Row row) throws Exception {

        TableMeta table = getTable(tableName);
        if (table == null) throw new RuntimeException("Table not found: " + tableName);

        byte[] record = RowSerializer.serialize(row, table);

        // ⚠️ TEMP FIX: still using page 0 (your system limitation)
        Page page = engine.readPage(0);

        int slotId = pageManager.insert(page, record);

        engine.writePage(page);

        table.getIndex().put(
                pk.toString(),
                new HashIndex.Location(page.getPageId(), slotId)
        );
    }

    // ---------------- SELECT ----------------

    public Row get(String tableName, Object pk) throws Exception {

        TableMeta table = getTable(tableName);
        if (table == null) return null;

        HashIndex.Location loc = table.getIndex().get(pk.toString());

        if (loc == null) return null;

        Page page = engine.readPage(loc.getPageId());

        byte[] data = pageManager.read(page, loc.getSlotId());

        return RowDeserializer.deserialize(data, table);
    }

    // ---------------- DELETE ----------------

    public void delete(String tableName, Object pk) throws Exception {

        TableMeta table = getTable(tableName);
        if (table == null) return;

        HashIndex.Location loc = table.getIndex().get(pk.toString());

        if (loc == null) return;

        Page page = engine.readPage(loc.getPageId());

        pageManager.delete(page, loc.getSlotId());

        engine.writePage(page);

        table.getIndex().remove(pk.toString());
    }

    // ---------------- GET ALL ----------------

    public List<Row> getAll(String tableName) throws Exception {

        TableMeta table = getTable(tableName);
        if (table == null) return new ArrayList<>();

        List<Row> result = new ArrayList<>();

        for (String pk : table.getIndex().raw().keySet()) {

            Row r = get(tableName, pk);

            if (r != null) result.add(r);
        }

        return result;
    }
}