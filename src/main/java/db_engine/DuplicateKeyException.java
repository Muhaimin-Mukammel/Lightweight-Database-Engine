package db_engine;

public class DuplicateKeyException extends RuntimeException {

    public DuplicateKeyException(String pk, String tableName) {
        super("Primary key '" + pk + "' already exists in table " + tableName);
    }
}