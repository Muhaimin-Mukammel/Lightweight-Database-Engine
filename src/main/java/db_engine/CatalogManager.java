package db_engine;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

public class CatalogManager {

    private final RandomAccessFile raf;
    private final SuperBlock sb;
    private Map<String, TableMeta> tables = new HashMap<>();

    public CatalogManager(RandomAccessFile raf, SuperBlock sb) {
        this.raf = raf;
        this.sb = sb;
    }

    public void load() throws IOException {
        if (sb.catalogSize <= 0) {
            return;
        }
        long available = raf.length() - sb.catalogOffset;
        int size = (int) Math.min(sb.catalogSize, available);
        if (size <= 0) {
            return;
        }
        byte[] data = new byte[size];
        raf.seek(sb.catalogOffset);
        raf.readFully(data);
        String json = new String(data).trim();
        if (!json.isEmpty()) {
            Map<String, TableMeta> loaded = Json.deserialize(
                    json,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, TableMeta>>() {
                    }
            );
            if (loaded != null) {
                tables = loaded;
            }
        }
    }

    public void save() throws IOException {
        String json = Json.serialize(tables);
        byte[] data = json.getBytes();

        if (data.length > Constants.CATALOG_RESERVED_SIZE) {
            throw new IOException("Catalog size exceeds reserved space");
        }

        raf.seek(sb.catalogOffset);
        raf.write(data);
        sb.catalogSize = data.length;
        sb.write(raf);
    }

    public void addTable(TableMeta t) throws IOException {
        tables.put(t.getName(), t);
        save();
    }

    public void dropTable(String name) throws IOException {
        tables.remove(name);
        save();
    }

    public boolean tableExists(String name) {
        return tables.containsKey(name);
    }

    public Map<String, TableMeta> getTables() {
        return tables;
    }
}