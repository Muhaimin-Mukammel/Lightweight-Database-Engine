package db_engine;

public class RowNotFoundException extends RuntimeException {

    public RowNotFoundException(String pk, String tableName) {
        super("No row with key '" + pk + "' in table " + tableName);
    }
}