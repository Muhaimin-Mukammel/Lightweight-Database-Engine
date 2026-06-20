package db_engine;

import java.util.*;

public class Row {

    private Map<String, Object> values = new HashMap<>();

    public void put(String k, Object v) {
        values.put(k, v);
    }

    public Object get(String k) {
        return values.get(k);
    }

    public Map<String, Object> getValues() {
        return values;
    }
}