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
        printBanner();

        while (true) {
            System.out.println();
            System.out.println("---------------------------------------------");
            System.out.println("Tables: " + describeTableList());

            if (currentTable == null) {
                System.out.println("Mode: GLOBAL");
                System.out.println("Type HELP for a list of commands.");
            } else {
                System.out.println("Mode: TABLE -> " + currentTable);
                System.out.println("Type HELP for a list of commands.");
            }

            System.out.print("> ");

            if (!sc.hasNextLine()) {
                System.out.println("\nInput closed. Goodbye.");
                return;
            }

            String input = sc.nextLine().trim();
            if (input.isEmpty()) {
                continue;
            }

            try {
                handle(input);
            } catch (Exception e) {
                // Every command path funnels its failures here. We print a clean message
                // instead of a stack trace so a bad command never looks like a crash.
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private void printBanner() {
        System.out.println("===============================================");
        System.out.println("   Lightweight Database Engine -- Interactive Shell");
        System.out.println("===============================================");
        System.out.println("Type HELP at any time to see what you can do.");
    }

    private String describeTableList() {
        if (crud.listTables().isEmpty()) {
            return "(none yet -- try CREATE)";
        }
        return crud.listTables().keySet().toString();
    }

    private void handle(String input) throws Exception {
        String upper = input.toUpperCase();

        if (upper.equals("EXIT") || upper.equals("QUIT")) {
            System.out.println("Goodbye.");
            System.exit(0);
        }

        if (upper.equals("HELP")) {
            printHelp();
            return;
        }

        if (upper.equals("BACK")) {
            if (currentTable == null) {
                System.out.println("Already in global mode.");
            } else {
                System.out.println("Leaving table: " + currentTable);
                currentTable = null;
            }
            return;
        }

        if (upper.equals("LIST TABLES") || upper.equals("TABLES")) {
            listTables();
            return;
        }

        if (currentTable == null) {
            handleGlobal(input, upper);
        } else {
            handleTable(input, upper);
        }
    }

    private void printHelp() {
        System.out.println();
        if (currentTable == null) {
            System.out.println("Global mode commands:");
            System.out.println("  CREATE              start the table creation wizard");
            System.out.println("  USE <table>         switch into a table");
            System.out.println("  LIST TABLES         show every table that exists");
            System.out.println("  DROP <table>        delete a table and its schema (asks to confirm)");
            System.out.println("  HELP                show this message");
            System.out.println("  EXIT                quit the program");
        } else {
            System.out.println("Table mode commands (current table: " + currentTable + "):");
            System.out.println("  INSERT                  add a new row, guided field by field");
            System.out.println("  SELECT *                show every row in the table");
            System.out.println("  SELECT <pk>              show one row by primary key");
            System.out.println("  SELECT * WHERE col=val   show rows where a column matches a value");
            System.out.println("  UPDATE <pk>              edit an existing row's fields");
            System.out.println("  DELETE <pk>              remove a row (asks to confirm)");
            System.out.println("  COUNT                    show how many rows are in the table");
            System.out.println("  DESCRIBE                 show the table's schema");
            System.out.println("  BACK                     return to global mode");
            System.out.println("  HELP                     show this message");
            System.out.println("  EXIT                     quit the program");
        }
    }

    private void listTables() {
        if (crud.listTables().isEmpty()) {
            System.out.println("No tables yet. Try CREATE to make one.");
            return;
        }
        System.out.println("Tables:");
        for (TableMeta t : crud.listTables().values()) {
            System.out.println("  " + t.getName() + "  (" + t.getColumns().size() + " columns, pk=" + t.getPrimaryKey() + ")");
        }
    }

    // ---------- global mode ----------

    private void handleGlobal(String input, String upper) throws Exception {
        if (upper.equals("CREATE")) {
            createTable();
            return;
        }
        if (upper.startsWith("USE ")) {
            useTable(input.substring(4).trim());
            return;
        }
        if (upper.startsWith("DROP ")) {
            dropTable(input.substring(5).trim());
            return;
        }
        System.out.println("Unknown command. Type HELP to see what's available in global mode.");
    }

    private void useTable(String rawName) {
        String tableName = rawName.toUpperCase();
        if (tableName.isEmpty()) {
            System.out.println("Usage: USE <table name>");
            return;
        }
        if (!crud.listTables().containsKey(tableName)) {
            System.out.println("No table named '" + tableName + "'. Available: " + crud.listTables().keySet());
            return;
        }
        currentTable = tableName;
        System.out.println("Now using table: " + currentTable);
    }

    private void dropTable(String rawName) throws Exception {
        String tableName = rawName.toUpperCase();
        if (tableName.isEmpty()) {
            System.out.println("Usage: DROP <table name>");
            return;
        }
        if (!crud.listTables().containsKey(tableName)) {
            System.out.println("No table named '" + tableName + "'.");
            return;
        }

        System.out.print("This will permanently delete table '" + tableName + "' and its schema. Type YES to confirm: ");
        String confirm = sc.nextLine().trim();
        if (!confirm.equalsIgnoreCase("YES")) {
            System.out.println("Cancelled.");
            return;
        }

        crud.dropTable(tableName);
        if (tableName.equals(currentTable)) {
            currentTable = null;
        }
        System.out.println("Table dropped: " + tableName);
        System.out.println("Note: existing rows on disk for this table are not reclaimed, only the schema is removed.");
    }

    private void createTable() throws Exception {
        System.out.print("Table name: ");
        String name = sc.nextLine().trim().toUpperCase();
        if (name.isEmpty()) {
            System.out.println("Table name cannot be empty. Cancelled.");
            return;
        }
        if (crud.listTables().containsKey(name)) {
            System.out.println("A table named '" + name + "' already exists. Cancelled.");
            return;
        }

        int n = readPositiveInt("Number of columns: ");
        if (n <= 0) {
            System.out.println("A table needs at least one column. Cancelled.");
            return;
        }

        List<Column> cols = new ArrayList<>();
        String pk = null;

        for (int i = 0; i < n; i++) {
            System.out.println();
            System.out.println("Column " + (i + 1) + " of " + n);

            String colName = readNonEmptyLine("  Name: ");
            String type = readValidType("  Type (INT/STRING): ");

            System.out.print("  Primary key? (y/n): ");
            boolean isPk = sc.nextLine().trim().equalsIgnoreCase("y");
            if (isPk && pk != null) {
                System.out.println("  A primary key is already set on '" + pk + "'. Ignoring this one.");
                isPk = false;
            }
            if (isPk) {
                pk = colName;
            }

            cols.add(new Column(colName, type, isPk));
        }

        if (pk == null) {
            System.out.println("No primary key was chosen. Cancelled -- every table needs exactly one.");
            return;
        }

        TableMeta table = new TableMeta(name, cols, pk);
        crud.createTable(table);
        System.out.println("Table created: " + name);
    }

    private int readPositiveInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String raw = sc.nextLine().trim();
            try {
                return Integer.parseInt(raw);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a whole number.");
            }
        }
    }

    private String readNonEmptyLine(String prompt) {
        while (true) {
            System.out.print(prompt);
            String raw = sc.nextLine().trim();
            if (!raw.isEmpty()) {
                return raw;
            }
            System.out.println("This can't be empty, try again.");
        }
    }

    private String readValidType(String prompt) {
        while (true) {
            System.out.print(prompt);
            String raw = sc.nextLine().trim().toUpperCase();
            if (Column.isSupportedType(raw)) {
                return raw;
            }
            System.out.println("Unsupported type. Choose one of: " + Column.SUPPORTED_TYPES);
        }
    }

    // ---------- table mode ----------

    private void handleTable(String input, String upper) throws Exception {
        TableMeta table = crud.getTable(currentTable);
        if (table == null) {
            System.out.println("That table no longer exists. Returning to global mode.");
            currentTable = null;
            return;
        }

        if (upper.equals("INSERT")) {
            insert(table);
            return;
        }
        if (upper.equals("SELECT *")) {
            selectAll();
            return;
        }
        if (upper.startsWith("SELECT * WHERE ")) {
            selectWhere(input.substring(15).trim());
            return;
        }
        if (upper.startsWith("SELECT ")) {
            select(input.substring(7).trim());
            return;
        }
        if (upper.startsWith("UPDATE ")) {
            update(table, input.substring(7).trim());
            return;
        }
        if (upper.startsWith("DELETE ")) {
            delete(input.substring(7).trim());
            return;
        }
        if (upper.equals("COUNT")) {
            System.out.println("Row count: " + crud.count(currentTable));
            return;
        }
        if (upper.equals("DESCRIBE") || upper.equals("DESC")) {
            describe(table);
            return;
        }

        System.out.println("Unknown command. Type HELP to see what's available in table mode.");
    }

    private void describe(TableMeta table) {
        System.out.println();
        System.out.println("Table: " + table.getName());
        System.out.println("Primary key: " + table.getPrimaryKey());
        System.out.println("Columns:");
        for (Column col : table.getColumns()) {
            String marker = col.isPrimaryKey() ? " [PK]" : "";
            System.out.println("  " + col.getName() + " : " + col.getType() + marker);
        }
        System.out.println("Row count: " + crud.count(currentTable));
    }

    private void selectAll() throws Exception {
        List<Row> rows = crud.getAll(currentTable);
        if (rows.isEmpty()) {
            System.out.println("Table is empty.");
            return;
        }
        for (Row r : rows) {
            System.out.println(r.getValues());
        }
        System.out.println("(" + rows.size() + " row" + (rows.size() == 1 ? "" : "s") + ")");
    }

    private void selectWhere(String clause) throws Exception {
        int eq = clause.indexOf('=');
        if (eq < 0) {
            System.out.println("Usage: SELECT * WHERE column=value");
            return;
        }
        String column = clause.substring(0, eq).trim();
        String value = clause.substring(eq + 1).trim();

        List<Row> rows = crud.filter(currentTable, column, value);
        if (rows.isEmpty()) {
            System.out.println("No rows match " + column + "=" + value);
            return;
        }
        for (Row r : rows) {
            System.out.println(r.getValues());
        }
        System.out.println("(" + rows.size() + " row" + (rows.size() == 1 ? "" : "s") + ")");
    }

    private void insert(TableMeta table) throws Exception {
        Row row = new Row();
        System.out.println();
        System.out.println("INSERT INTO " + table.getName());

        for (Column col : table.getColumns()) {
            Object parsed = readTypedValue(col);
            row.put(col.getName(), parsed);
        }

        String pk = readNonEmptyLine("Primary key value: ");
        crud.insert(table.getName(), pk, row);
        System.out.println("Inserted.");
    }

    /**
     * Prompts for a single column's value and keeps retrying until it parses
     * cleanly as the column's declared type, instead of letting a bad number
     * blow up the whole INSERT flow.
     */
    private Object readTypedValue(Column col) {
        while (true) {
            System.out.print(col.getName() + " (" + col.getType() + "): ");
            String val = sc.nextLine();
            try {
                return parse(val, col.getType());
            } catch (NumberFormatException e) {
                System.out.println("'" + val + "' is not a valid " + col.getType() + ". Try again.");
            }
        }
    }

    private void select(String pk) throws Exception {
        if (pk.isEmpty()) {
            System.out.println("Usage: SELECT <primary key>");
            return;
        }
        Row row = crud.get(currentTable, pk);
        if (row == null) {
            System.out.println("No row with primary key '" + pk + "'.");
            return;
        }
        System.out.println(row.getValues());
    }

    private void update(TableMeta table, String pk) throws Exception {
        if (pk.isEmpty()) {
            System.out.println("Usage: UPDATE <primary key>");
            return;
        }

        Row existing = crud.get(currentTable, pk);
        if (existing == null) {
            System.out.println("No row with primary key '" + pk + "'.");
            return;
        }

        System.out.println();
        System.out.println("Editing row " + pk + " in " + table.getName());
        System.out.println("Press Enter on any field to keep its current value.");

        Row updated = new Row();
        for (Column col : table.getColumns()) {
            if (col.isPrimaryKey()) {
                // The primary key identifies the row -- it isn't something UPDATE
                // can change. Changing it would mean inserting a different row.
                updated.put(col.getName(), existing.get(col.getName()));
                continue;
            }

            Object current = existing.get(col.getName());
            Object parsed = readTypedValueOrKeep(col, current);
            updated.put(col.getName(), parsed);
        }

        crud.update(currentTable, pk, updated);
        System.out.println("Updated.");
    }

    private Object readTypedValueOrKeep(Column col, Object current) {
        while (true) {
            System.out.print(col.getName() + " (" + col.getType() + ") [" + current + "]: ");
            String val = sc.nextLine();
            if (val.isBlank()) {
                return current;
            }
            try {
                return parse(val, col.getType());
            } catch (NumberFormatException e) {
                System.out.println("'" + val + "' is not a valid " + col.getType() + ". Try again.");
            }
        }
    }

    private void delete(String pk) throws Exception {
        if (pk.isEmpty()) {
            System.out.println("Usage: DELETE <primary key>");
            return;
        }
        if (crud.get(currentTable, pk) == null) {
            System.out.println("No row with primary key '" + pk + "'.");
            return;
        }

        System.out.print("Delete row '" + pk + "'? Type YES to confirm: ");
        String confirm = sc.nextLine().trim();
        if (!confirm.equalsIgnoreCase("YES")) {
            System.out.println("Cancelled.");
            return;
        }

        crud.delete(currentTable, pk);
        System.out.println("Deleted.");
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