package Combiner;

import com.fasterxml.jackson.databind.ObjectMapper;
import db_engine.Column;
import db_engine.Database;
import db_engine.Row;
import db_engine.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public class combiner {
    public void integrate() throws Exception{
        Scanner sc = new Scanner(System.in);
        Database db = new Database();

        db.load();
        Set<String> tablenames = db.getTableName();
        System.out.println(tablenames);

        System.out.println("Select Table: ");
        String Name = sc.nextLine();
        if(tablenames.contains(Name)){
            Table table = db.getTable(Name);
            List<Column> columns = table.getColumns();
            Row row = new Row();
            for(Column col : columns){
                String columnName = col.getName();
                System.out.println("Enter " + columnName);
                String userinput = sc.nextLine();

                Object value;

                switch (col.getType()){
                    case "INT":
                        value = Integer.parseInt(userinput);
                        break;
                    case "STRING":
                        value = userinput;
                        break;
                    default:
                        throw new IllegalStateException("Value is Null : " + col.getType());
                }

                row.put(columnName, value);
            }

            System.out.println(row.getValues());
            table.insert("2", row);
            db.save();

        }else {
            List<Column> columns = new ArrayList<>();
            System.out.print("Enter the number of Column in the Table : ");
            int max = sc.nextInt();
            sc.nextLine();
            for(int i = 0 ; i < max ; i++){
                System.out.print("Enter Name of the column : ");
                String name = sc.nextLine();
                System.out.print("Enter Type of the column : ");
                String type = sc.nextLine();
                Column column;
                if(i == 0) {
                    column = new Column(name, type, true);
                }else{
                    column = new Column(name, type, false);
                }

                columns.add(column);
            }

            Table table = new Table(Name, columns);
            db.addTable(Name, table);
            Row row = new Row();
            Object pk = null;
            for(Column col : table.getColumns()){
                System.out.print("Enter " + col.getName() + ":");
                String input = sc.nextLine();

                Object value;
                switch (col.getType()){
                    case "INT":
                        value = Integer.parseInt(input);
                        break;
                    case "STRING":
                        value = input;
                        break;
                    default:
                        throw new IllegalStateException("Value is Null : " + col.getType());
                }
                row.put(col.getName(), value);
                if(col.isPrimaryKey()){
                    pk = row.get(col.getName());
                }

            }

            table.insert(pk.toString(), row);

            System.out.println("\n--- ROW ---");
            System.out.println(row);

            ObjectMapper mapper = new ObjectMapper();

            String json = mapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(db);

            System.out.println("\n--- DATABASE JSON ---");
            System.out.println(json);

            db.save();
        }
    }
}
