package CRUD;

import db_engine.Column;
import db_engine.DuplicateKeyException;
import db_engine.HashIndex;
import db_engine.Page;
import db_engine.PageManager;
import db_engine.Row;
import db_engine.RowDeserializer;
import db_engine.RowNotFoundException;
import db_engine.RowSerializer;
import db_engine.SchemaException;
import db_engine.Slot;
import db_engine.StorageEngine;
import db_engine.TableMeta;
import db_engine.TableNotFoundException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private TableMeta requireTable(String name) {
        TableMeta table = getTable(name);
        if (table == null) {
            throw new TableNotFoundException(name);
        }
        return table;
    }

    public Map<String, TableMeta> listTables() {
        return engine.catalog().getTables();
    }

    public void createTable(TableMeta table) throws Exception {
        validateSchema(table);
        if (engine.catalog().tableExists(table.getName())) {
            throw new SchemaException("Table already exists: " + table.getName());
        }
        engine.catalog().addTable(table);
    }

    public void dropTable(String tableName) throws Exception {
        requireTable(tableName);
        engine.catalog().dropTable(tableName);
    }

    private void validateSchema(TableMeta table) {
        if (table.getName() == null || table.getName().isBlank()) {
            throw new SchemaException("Table name cannot be empty");
        }
        if (table.getColumns() == null || table.getColumns().isEmpty()) {
            throw new SchemaException("Table must have at least one column");
        }

        Set<String> seenNames = new HashSet<>();
        boolean hasPk = false;

        for (Column col : table.getColumns()) {
            if (col.getName() == null || col.getName().isBlank()) {
                throw new SchemaException("Column name cannot be empty");
            }
            if (!seenNames.add(col.getName().toUpperCase())) {
                throw new SchemaException("Duplicate column name: " + col.getName());
            }
            if (!Column.isSupportedType(col.getType())) {
                throw new SchemaException("Unsupported type '" + col.getType() + "' for column " + col.getName()
                        + ". Supported types: " + Column.SUPPORTED_TYPES);
            }
            if (col.isPrimaryKey()) {
                hasPk = true;
            }
        }

        if (!hasPk) {
            throw new SchemaException("Table must declare exactly one primary key column");
        }
    }

    public void insert(String tableName, Object pk, Row row) throws Exception {
        TableMeta table = requireTable(tableName);

        String pkStr = pk == null ? null : pk.toString();
        if (pkStr == null || pkStr.isBlank()) {
            throw new SchemaException("Primary key value cannot be empty");
        }
        if (table.getIndex().get(pkStr) != null) {
            throw new DuplicateKeyException(pkStr, tableName);
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

        table.getIndex().put(pkStr, new HashIndex.Location(targetPage.getPageId(), slotId));
    }

    public Row get(String tableName, Object pk) throws Exception {
        TableMeta table = requireTable(tableName);

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

    public void update(String tableName, Object pk, Row newValues) throws Exception {
        TableMeta table = requireTable(tableName);
        String pkStr = pk.toString();

        HashIndex.Location loc = table.getIndex().get(pkStr);
        if (loc == null) {
            throw new RowNotFoundException(pkStr, tableName);
        }

        // The primary key column itself is never editable through update;
        // carry the existing value over so identity can't silently drift.
        if (table.getPrimaryKey() != null) {
            newValues.put(table.getPrimaryKey(), get(tableName, pk).get(table.getPrimaryKey()));
        }

        byte[] record = RowSerializer.serialize(newValues, table);

        Page page = engine.readPage(loc.getPageId());
        Slot existing = pageManager.readSlot(page, loc.getSlotId());

        if (record.length <= existing.getSize()) {
            // Fits in place: overwrite without touching the slot directory size,
            // keeping the original capacity reserved for future updates.
            pageManager.overwrite(page, loc.getSlotId(), record);
            engine.writePage(page);
        } else {
            // Doesn't fit: free the old slot and re-insert elsewhere, then refresh the index.
            pageManager.delete(page, loc.getSlotId());
            engine.writePage(page);

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
            table.getIndex().put(pkStr, new HashIndex.Location(targetPage.getPageId(), slotId));
        }
    }

    public void delete(String tableName, Object pk) throws Exception {
        TableMeta table = requireTable(tableName);
        String pkStr = pk.toString();

        HashIndex.Location loc = table.getIndex().get(pkStr);
        if (loc == null) {
            throw new RowNotFoundException(pkStr, tableName);
        }

        Page page = engine.readPage(loc.getPageId());
        pageManager.delete(page, loc.getSlotId());
        engine.writePage(page);
        table.getIndex().remove(pkStr);
    }

    public List<Row> getAll(String tableName) throws Exception {
        TableMeta table = requireTable(tableName);

        List<Row> result = new ArrayList<>();
        for (String pk : table.getIndex().raw().keySet()) {
            Row r = get(tableName, pk);
            if (r != null) {
                result.add(r);
            }
        }
        return result;
    }

    public int count(String tableName) {
        TableMeta table = requireTable(tableName);
        return table.getIndex().raw().size();
    }

    /**
     * Simple full-scan filter: returns every row where the given column equals the given value.
     * No query planner here -- it's a linear scan over the in-memory index, same cost as SELECT *.
     */
    public List<Row> filter(String tableName, String columnName, String value) throws Exception {
        TableMeta table = requireTable(tableName);

        boolean knownColumn = table.getColumns().stream()
                .anyMatch(c -> c.getName().equalsIgnoreCase(columnName));
        if (!knownColumn) {
            throw new SchemaException("No such column: " + columnName);
        }

        List<Row> result = new ArrayList<>();
        for (Row row : getAll(tableName)) {
            Object actual = row.get(findActualColumnName(table, columnName));
            if (actual != null && actual.toString().equals(value)) {
                result.add(row);
            }
        }
        return result;
    }

    private String findActualColumnName(TableMeta table, String name) {
        for (Column c : table.getColumns()) {
            if (c.getName().equalsIgnoreCase(name)) {
                return c.getName();
            }
        }
        return name;
    }
}