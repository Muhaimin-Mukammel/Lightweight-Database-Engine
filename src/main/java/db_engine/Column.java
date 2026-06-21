package db_engine;

import java.util.Arrays;
import java.util.List;

public class Column {

    public static final List<String> SUPPORTED_TYPES = Arrays.asList("INT", "STRING");

    private String name;
    private String type;
    private boolean primaryKey;

    public Column() {
    }

    public Column(String name, String type, boolean primaryKey) {
        this.name = name;
        this.type = type;
        this.primaryKey = primaryKey;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setPrimaryKey(boolean primaryKey) {
        this.primaryKey = primaryKey;
    }

    public static boolean isSupportedType(String type) {
        return type != null && SUPPORTED_TYPES.contains(type.toUpperCase());
    }
}