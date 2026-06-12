package db_engine;

import java.util.HashMap;
import java.util.Map;

public class Row {

    private Map<String, Object> values = new HashMap<>();

    public Row() {}

    public void put(String key, Object value) {
        values.put(key, value);
    }
    public Object get(String key){
        return values.get(key);
    }
    public Map<String, Object> getValues() {
        return values;
    }
}