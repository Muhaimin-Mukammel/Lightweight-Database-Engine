package db_engine;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class RowDeserializer {

    public static Row deserialize(byte[] data, TableMeta meta) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
        Row row = new Row();

        for (Column col : meta.getColumns()) {
            switch (col.getType().toUpperCase()) {
                case "INT":
                    row.put(col.getName(), in.readInt());
                    break;
                case "STRING":
                    int len = in.readInt();
                    byte[] buf = new byte[len];
                    in.readFully(buf);
                    row.put(col.getName(), new String(buf));
                    break;
                default:
                    throw new IOException("Unknown column type: " + col.getType());
            }
        }

        return row;
    }
}