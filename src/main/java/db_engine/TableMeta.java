package db_engine;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

public class TableMeta {

    private String name;
    private List<Column> columns;
    private String primaryKey;

    @JsonIgnore
    private HashIndex index = new HashIndex();

    public TableMeta() {
    }

    public TableMeta(String name, List<Column> columns, String primaryKey) {
        this.name = name;
        this.columns = columns;
        this.primaryKey = primaryKey;
        this.index = new HashIndex();
    }

    public String getName() {
        return name;
    }

    public List<Column> getColumns() {
        return columns;
    }

    public String getPrimaryKey() {
        return primaryKey;
    }

    public HashIndex getIndex() {
        return index;
    }
}