package Combiner;

import db_engine.Column;
import db_engine.Database;
import db_engine.Row;
import db_engine.Table;
import java.io.IOException;
import java.util.Scanner;

public class AddRow {
    private final Database db;
    private final Scanner sc;

    public AddRow(Database database, Scanner sc){
        this.db = database;
        this.sc = sc;
    }

    public void addRow(Table table){

        Row row = new Row();
        Object pk = null;

        for(Column col : table.getColumns()){

            String input;

            while(true){
                System.out.print("Enter " + col.getName() + " : ");
                input = sc.nextLine();

               try {
                    Object value;

                    switch (col.getType().toUpperCase()) {
                        case "INT":
                            value = Integer.parseInt(input);
                            break;
                        case "STRING":
                            value = input;
                            break;
                        default:
                            System.out.println("Unsupported type: " + col.getType());
                            continue;
                    }

                    row.put(col.getName(), value);

                    if (col.isPrimaryKey()) {
                        pk = row.get(col.getName());
                    }
                    break;
                } catch (NumberFormatException e){
                   System.out.println("Invalid INT value");
               }
            }
        }

        if (pk == null) {
            System.out.println("Primary key missing");
            return;
        }

        if(table.getRows().containsKey(pk.toString())){
            System.out.println("Duplicate primary key");
            return;
        }

        table.insert(pk.toString(), row);

        try{
            db.save();
        }catch (IOException e){
            System.out.println("save failed");
        }
    }
}
