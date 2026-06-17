package Combiner;

import db_engine.Column;
import db_engine.Database;
import db_engine.Row;
import db_engine.Table;
import java.util.Scanner;

public class AddColumnExist {
    private Table table;
    private final Database db;
    private final Scanner sc;

    public AddColumnExist(Database database, Scanner sc){
        this.db = database;
        this.sc = sc;
    }

    public void addColumn(Table table){
        if(table == null){
            System.out.println("Table not found");
            return;
        }

        System.out.print("Enter the number of additional column : ");
        int nocl = sc.nextInt();
        sc.nextLine();

        for(int i = 0; i < nocl; i++){
            add(table);
        }
    }

    private void add(Table table){
        System.out.print("Enter the name of the column : ");
        String name = sc.nextLine().toLowerCase();

        for(Column c : table.getColumns()){
            if(c.getName().equalsIgnoreCase(name)){
                System.out.println("Column already exists");
                return;
            }
        }

        System.out.print("Enter the type of the column : ");
        String type = sc.nextLine();

        if(!type.equalsIgnoreCase("INT") &&
                !type.equalsIgnoreCase("STRING")){
            System.out.println("Unsupported type");
            return;
        }

        Column column;
        column = new Column(name, type, false);
        table.getColumns().add(column);

        Object defaultValue;

        switch(type.toUpperCase()) {
            case "INT": defaultValue = 0; break;
            case "STRING": defaultValue = ""; break;
            default: defaultValue = null;
        }

        for (Row row : table.getRows().values()) {
            row.getValues().put(name, defaultValue);
        }
    }
}
