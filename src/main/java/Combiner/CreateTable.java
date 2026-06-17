package Combiner;

import db_engine.Column;
import db_engine.Database;
import db_engine.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class CreateTable {
    private final Database database;
    private final Scanner sc;
    public CreateTable(Database db, Scanner sc){
        this.database = db;
        this.sc = sc;
    }

    public Table createTable(){
        List<Column> columns = new ArrayList<>();

        System.out.print("Name of the Table : ");
        String name = sc.nextLine();

        Table table = new Table(name, columns);
        database.addTable(name, table);

        return table;
    }
}
