package db_engine;

public class Column {
    private String name;
    private String type;
    private boolean primaryKey;


    public Column(){}
    public Column(String name, String type, boolean primaryKey) {
        this.name = name;
        this.type = type;
        this.primaryKey = primaryKey;
    }

    public String getName() { return name; }
    public String getType() { return type; }
    public boolean isPrimaryKey() { return primaryKey; }

    public void setName(String name) { this.name = name; }
    public void setType(String type) { this.type = type; }
    public void setPrimaryKey(boolean primaryKey) { this.primaryKey = primaryKey; }

}
