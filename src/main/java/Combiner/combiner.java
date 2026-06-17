package Combiner;

import com.fasterxml.jackson.databind.ObjectMapper;
import db_engine.Column;
import db_engine.Database;
import db_engine.Row;
import db_engine.Table;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public class combiner {

    private final Database db;
    private final Scanner sc;
    private AddRow ar;
    private AddColumn ac;
    private AddColumnExist ace;
    private CreateTable ct;

    public combiner(Database database, Scanner scanner){
        this.db = database;
        this.sc = scanner;

        this.ar = new AddRow(db, sc);
        this.ac = new AddColumn(db, sc);
        this.ace = new AddColumnExist(db, sc);
        this.ct = new CreateTable(db, sc);
    }

    public void integrate(){

        try{
            db.load();
        } catch (IOException e) {
            System.out.println("Loading failed");
        }

        Set<String> tablenames = db.getTableName();

        System.out.println("Existing tables -> " + tablenames);

        System.out.print("Enter Table name : ");
        String name = sc.nextLine();

        if(tablenames.contains(name)){
            Table table = db.getTable(name);

            if(table == null){
                System.out.println("Table not found");
                return;
            }

            System.out.println("Table exists....");

            while(true){
                System.out.println("""
                        1. Add row 
                        2. Add column
                        3. Exit table 
                        """);

                if (!sc.hasNextInt()) {
                    System.out.println("Invalid input");
                    sc.nextLine();
                    continue;
                }

                int key = sc.nextInt();
                sc.nextLine();

                switch (key) {
                    case 1:
                        ar.addRow(table);
                        try {
                            db.save();
                        } catch (IOException e) {
                            System.out.println("Database cannot be saved..");
                        }
                        break;
                    case 2:
                        ace.addColumn(table);
                        try {
                            db.save();
                        } catch (IOException e) {
                            System.out.println("Database cannot be saved..");
                        }
                        break;
                    case 3:
                        System.out.println("Exiting table...");
                        return;
                    default:
                        System.out.println("Invalid input");
                }
            }
        } else {

            Table table = null;

            while(true){
                System.out.println("""
            Table does not exist in Database.
            1. Create Table
            2. Exit
            """);

                if (!sc.hasNextInt()) {
                    System.out.println("Invalid input");
                    sc.nextLine();
                    continue;
                }

                int key = sc.nextInt();
                sc.nextLine();

                switch (key) {
                    case 1:
                        table = ct.createTable();
                    try{
                        db.save();
                    } catch (IOException e) {
                        System.out.println("Database cannot be saved..");
                    }
                        break;

                    case 2:
                        System.out.println("Exiting table..");
                        return;

                    default:
                        System.out.println("Invalid Input");
                        continue;
                }

                if(table != null){
                    System.out.println("Table Created..");
                    ac.addPk(table);
                    try {
                        db.save();
                    } catch (IOException e) {
                        System.out.println("Database cannot be saved..");
                    }

                    System.out.print("Add columns now? (y/n): ");
                    String ans = sc.nextLine();

                    if(ans.equalsIgnoreCase("y")){
                        System.out.print("Enter number of columns: ");
                        int max = sc.nextInt();
                        sc.nextLine();

                        ac.addColumn(table, max);
                        try {
                            db.save();
                        } catch (IOException e) {
                            System.out.println("Database cannot be saved..");
                        }
                    }

                    break;
                } else {
                    System.out.println("Table creation failed");
                }
            }
        }
    }
}
