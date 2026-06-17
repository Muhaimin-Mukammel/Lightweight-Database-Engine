package Combiner;

import db_engine.Column;
import db_engine.Database;
import db_engine.Row;
import db_engine.Table;

import java.util.Scanner;

public class AddColumn {

    private Table table;
    private final Database db;
    private final Scanner sc;

    public AddColumn(Database database, Scanner sc){
        this.db = database;
        this.sc = sc;
    }

    public void addColumn(Table table, int max){

        if(table == null){
            System.out.println("Table not found");
            return;
        }

        for(int i = 0; i < max; i++){
            add(table);
        }
    }

    private void add(Table table){
        System.out.print("Enter the name of the column : ");
        String name = sc.nextLine().toLowerCase();

        for(Column c : table.getColumns()){
            if(c.getName().equals(name)){
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

        Column column = new Column(name, type.toUpperCase(), false);
        table.getColumns().add(column);

        Object defaultValue;
        switch(type.toUpperCase()){
            case "INT": defaultValue = 0; break;
            case "STRING": defaultValue = ""; break;
            default: defaultValue = null;
        }

        for(Row row : table.getRows().values()){
            row.getValues().put(name, defaultValue);
        }
    }

    public void addPk(Table table){
        System.out.print("Enter primary key column name : ");
        String name = sc.nextLine().toLowerCase();

        for(Column c : table.getColumns()){
            if(c.getName().equals(name)){
                System.out.println("Column already exists");
                return;
            }
        }

        for(Column c : table.getColumns()){
            if(c.isPrimaryKey()){
                System.out.println("Primary key already exists");
                return;
            }
        }

        if(!table.getRows().isEmpty()){
            System.out.println("Cannot add primary key after rows exist");
            return;
        }

        System.out.print("Enter type of primary key (INT only): ");
        String type = sc.nextLine();

        if(!type.equalsIgnoreCase("INT")){
            System.out.println("Primary key must be INT");
            return;
        }

        Column column = new Column(name, type.toUpperCase(), true);
        table.getColumns().add(column);

        System.out.println("Primary key added");
    }
}