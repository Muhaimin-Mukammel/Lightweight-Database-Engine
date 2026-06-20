package DashBoard;

import CRUD.CrudService;
import db_engine.Column;
import db_engine.Row;
import db_engine.TableMeta;

import java.util.*;

public class Dashboard {

    private final CrudService crud;
    private final Scanner sc = new Scanner(System.in);

    private String currentTable = null;

    public Dashboard(CrudService crud) {
        this.crud = crud;
    }

    public void run() throws Exception {

        while (true) {

            System.out.println("\n================ DB SHELL ================");

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

            handle(input);
        }
    }

    private void handle(String input) throws Exception {

        input = input.trim();

        if (input.equalsIgnoreCase("EXIT")) {
            System.exit(0);
        }

        if (input.equalsIgnoreCase("BACK")) {
            currentTable = null;
            System.out.println("✔ Back to global mode");
            return;
        }

        // ---------------- GLOBAL MODE ----------------
        if (currentTable == null) {

            if (input.equalsIgnoreCase("CREATE")) {
                createTable();
                return;
            }

            if (input.startsWith("USE ")) {

                String tableName = input.substring(4).trim().toUpperCase();

                if (!crud.listTables().containsKey(tableName)) {
                    System.out.println("❌ Table not found");
                    System.out.println("Available: " + crud.listTables().keySet());
                    return;
                }

                currentTable = tableName;
                System.out.println("✔ Using table: " + currentTable);
                return;
            }

            System.out.println("❌ Unknown command (global mode)");
            return;
        }

        // ---------------- TABLE MODE ----------------
        handleTable(input);
    }

    private void handleGlobal(String input) throws Exception {

        switch (input.toUpperCase()) {

            case "CREATE" -> createTable();

            default -> System.out.println("Unknown command");
        }
    }

    private void createTable() throws Exception {

        System.out.print("Table name: ");
        String name = sc.nextLine().trim().toUpperCase();

        System.out.print("Number of columns: ");
        int n = Integer.parseInt(sc.nextLine());

        List<Column> cols = new ArrayList<>();
        String pk = null;

        for (int i = 0; i < n; i++) {

            System.out.println("\nColumn " + (i + 1));

            System.out.print("Name: ");
            String colName = sc.nextLine();

            System.out.print("Type (INT/STRING): ");
            String type = sc.nextLine();

            System.out.print("Primary Key? (y/n): ");
            boolean isPk = sc.nextLine().equalsIgnoreCase("y");

            if (isPk) pk = colName;

            cols.add(new Column(colName, type, isPk));
        }

        TableMeta table = new TableMeta(name, cols, pk);

        crud.createTable(table);

        System.out.println("✔ Table created: " + name);
    }

    private void handleTable(String input) throws Exception {

        TableMeta table = crud.getTable(currentTable);

        if (table == null) {
            System.out.println("❌ Table missing. Resetting state.");
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

        if (input.startsWith("SELECT ")) {
            select(input.substring(7).trim());
            return;
        }

        if (input.startsWith("DELETE ")) {
            delete(input.substring(7).trim());
            return;
        }

        System.out.println("❌ Unknown table command");
    }

    private void selectAll() throws Exception {

        if (currentTable == null) {
            System.out.println("❌ No table selected");
            return;
        }

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

        System.out.println("\nINSERT INTO " + table.getName());

        for (Column col : table.getColumns()) {

            System.out.print(col.getName() + " (" + col.getType() + "): ");
            String val = sc.nextLine();

            Object parsed = parse(val, col.getType());

            row.put(col.getName(), parsed);
        }

        System.out.print("Primary Key value: ");
        String pk = sc.nextLine().trim();

        if (pk.isEmpty()) {
            System.out.println("❌ PK cannot be empty");
            return;
        }

        crud.insert(table.getName(), pk, row);

        System.out.println("✔ Inserted");
    }

    private void select(String pk) throws Exception {

        if (currentTable == null) {
            System.out.println("❌ No table selected");
            return;
        }

        Row row = crud.get(currentTable, pk);

        if (row == null) {
            System.out.println("❌ Not found");
            return;
        }

        System.out.println(row.getValues());
    }

    private void delete(String pk) throws Exception {

        if (currentTable == null) {
            System.out.println("❌ No table selected");
            return;
        }

        crud.delete(currentTable, pk);

        System.out.println("✔ Deleted");
    }

    private Object parse(String val, String type) {

        switch (type.toUpperCase()) {

            case "INT":
                return Integer.parseInt(val);

            case "STRING":
                return val;

            default:
                throw new RuntimeException("Unknown type");
        }
    }
}


