package db_engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Table {

    public Table() {}

    private String name;
    private List<Column> columns = new ArrayList<>();
    private Map<String, Row> rows = new HashMap<>();

    public Table(String name, List<Column> columns) {
        this.name = name;
        this.columns = columns;
    }

    public void insert(String pk, Row row) {
        validate(row);
        rows.put(pk, row);
    }

    public void validate(Row row){
        for( Column col : columns){
            Object value = row.get(col.getName());
            if(value == null){
                throw new RuntimeException("Missing column : " + col.getName());
            }
            switch (col.getType().toUpperCase()){
                case "INT":
                    if(!(value instanceof Integer)){
                        throw new RuntimeException("Type Mismatch : " + col.getName());
                    }
                    break;
                case "STRING":
                    if (!(value instanceof String)) {
                        throw new RuntimeException("Type mismatch : " + col.getName());
                    }
                    break;
            }
        }
    }

    public Map<String, Row> getRows() {
        return rows;
    }
    public List<Column> getColumns(){
        return columns;
    }
    public String getName(){
        return name;
    }
}