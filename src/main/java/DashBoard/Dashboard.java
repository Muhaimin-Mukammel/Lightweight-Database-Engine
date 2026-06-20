package DashBoard;

import CRUD.CrudService;
import db_engine.Column;
import db_engine.Row;
import db_engine.TableMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Dashboard {

    private final CrudService crud;
    private final Scanner sc = new Scanner(System.in);
    private String currentTable = null;

    public Dashboard(CrudService crud) {
        this.crud = crud;
    }

    public void run() throws Exception {
        while (true) {
            System.out.println();
            System.out.println("================ DB SHELL ================");
            System.out.println("Tables: " + crud.listTables().keySet());

            if (currentTable == null) {
                System.out.println("Mode: GLOBAL");
                System.out.println("Commands: CREATE, USE <table>, EXIT");
            } else {
                System.out.println("Mode: TABLE -> " + currentTable);
                System.out.println("Commands: INSERT, SELECT <pk>, SELECT *, DELETE <pk>, BACK");
            }

            System.out.print("> ");
            String input = sc.nextLine().trim();

            try {
                handle(input);
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private void handle(String input) throws Exception {
        input = input.trim();

        if (input.equalsIgnoreCase("EXIT")) {
            System.exit(0);
        }

        if (input.equalsIgnoreCase("BACK")) {
            currentTable = null;
            System.out.println("Back to global mode");
            return;
        }

        if (currentTable == null) {
            if (input.equalsIgnoreCase("CREATE")) {
                createTable();
                return;
            }
            if (input.toUpperCase().startsWith("USE ")) {
                String tableName = input.substring(4).trim().toUpperCase();
                if (!crud.listTables().containsKey(tableName)) {
                    System.out.println("Table not found");
                    System.out.println("Available: " + crud.listTables().keySet());
                    return;
                }
                currentTable = tableName;
                System.out.println("Using table: " + currentTable);
                return;
            }
            System.out.println("Unknown command (global mode)");
            return;
        }

        handleTable(input);
    }

    private void createTable() throws Exception {
        System.out.print("Table name: ");
        String name = sc.nextLine().trim().toUpperCase();

        System.out.print("Number of columns: ");
        int n = Integer.parseInt(sc.nextLine().trim());

        List<Column> cols = new ArrayList<>();
        String pk = null;

        for (int i = 0; i < n; i++) {
            System.out.println();
            System.out.println("Column " + (i + 1));

            System.out.print("Name: ");
            String colName = sc.nextLine().trim();

            System.out.print("Type (INT/STRING): ");
            String type = sc.nextLine().trim();

            System.out.print("Primary Key? (y/n): ");
            boolean isPk = sc.nextLine().trim().equalsIgnoreCase("y");
            if (isPk) {
                pk = colName;
            }

            cols.add(new Column(colName, type, isPk));
        }

        TableMeta table = new TableMeta(name, cols, pk);
        crud.createTable(table);
        System.out.println("Table created: " + name);
    }

    private void handleTable(String input) throws Exception {
        TableMeta table = crud.getTable(currentTable);
        if (table == null) {
            System.out.println("Table missing. Resetting state.");
            currentTable = null;
            return;
        }

        input = input.trim();

        if (input.equalsIgnoreCase("INSERT")) {
            insert(table);
            return;
        }
        if (input.equalsIgnoreCase("SELECT *")) {
            selectAll();
            return;
        }
        if (input.toUpperCase().startsWith("SELECT ")) {
            select(input.substring(7).trim());
            return;
        }
        if (input.toUpperCase().startsWith("DELETE ")) {
            delete(input.substring(7).trim());
            return;
        }

        System.out.println("Unknown table command");
    }

    private void selectAll() throws Exception {
        List<Row> rows = crud.getAll(currentTable);
        if (rows == null || rows.isEmpty()) {
            System.out.println("Empty table");
            return;
        }
        for (Row r : rows) {
            System.out.println(r.getValues());
        }
    }

    private void insert(TableMeta table) throws Exception {
        Row row = new Row();
        System.out.println();
        System.out.println("INSERT INTO " + table.getName());

        for (Column col : table.getColumns()) {
            System.out.print(col.getName() + " (" + col.getType() + "): ");
            String val = sc.nextLine();
            Object parsed = parse(val, col.getType());
            row.put(col.getName(), parsed);
        }

        System.out.print("Primary Key value: ");
        String pk = sc.nextLine().trim();
        if (pk.isEmpty()) {
            System.out.println("Primary key cannot be empty");
            return;
        }

        crud.insert(table.getName(), pk, row);
        System.out.println("Inserted");
    }

    private void select(String pk) throws Exception {
        Row row = crud.get(currentTable, pk);
        if (row == null) {
            System.out.println("Not found");
            return;
        }
        System.out.println(row.getValues());
    }

    private void delete(String pk) throws Exception {
        crud.delete(currentTable, pk);
        System.out.println("Deleted");
    }

    private Object parse(String val, String type) {
        switch (type.toUpperCase()) {
            case "INT":
                return Integer.parseInt(val.trim());
            case "STRING":
                return val;
            default:
                throw new RuntimeException("Unknown type: " + type);
        }
    }
}