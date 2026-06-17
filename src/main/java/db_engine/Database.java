package db_engine;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Database {

    private Map<String, Table> tables = new HashMap<>();
    private final File file = new File("db.json");
    private final ObjectMapper mapper = new ObjectMapper();

    public void addTable(String name, Table table) {
        tables.put(name, table);
    }

    public Table getTable(String name) {
        return tables.get(name);
    }

    @JsonIgnore
    public Set<String> getTableName(){
        return tables.keySet();
    }

    public void load() throws IOException {
        if (file.exists()) {
            Database loaded = mapper.readValue(file, Database.class);
            if (loaded.tables != null) {
                this.tables = loaded.tables;
            }
        }
    }

    public void save(){
        try{
            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(file, this);
        } catch (IOException e) {
            System.out.println("Database cannot be saved");
        }
    }

    public Map<String, Table> getTables() {
        return tables;
    }
}