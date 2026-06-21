package db_engine;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class RowSerializer {

    public static byte[] serialize(Row row, TableMeta meta) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bos);

        for (Column col : meta.getColumns()) {
            Object val = row.get(col.getName());

            switch (col.getType().toUpperCase()) {
                case "INT":
                    if (val != null && !(val instanceof Integer)) {
                        throw new IOException("Column '" + col.getName() + "' expects INT, got " + val.getClass().getSimpleName());
                    }
                    out.writeInt(val == null ? 0 : (Integer) val);
                    break;
                case "STRING":
                    if (val != null && !(val instanceof String)) {
                        throw new IOException("Column '" + col.getName() + "' expects STRING, got " + val.getClass().getSimpleName());
                    }
                    String s = val == null ? "" : (String) val;
                    byte[] bytes = s.getBytes();
                    out.writeInt(bytes.length);
                    out.write(bytes);
                    break;
                default:
                    throw new IOException("Unknown column type: " + col.getType());
            }
        }

        return bos.toByteArray();
    }

    public static String extractPrimaryKey(byte[] record, TableMeta table) {
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(record));
            for (Column col : table.getColumns()) {
                if (col.getType().equalsIgnoreCase("INT")) {
                    int v = in.readInt();
                    if (col.isPrimaryKey()) {
                        return String.valueOf(v);
                    }
                } else if (col.getType().equalsIgnoreCase("STRING")) {
                    int len = in.readInt();
                    byte[] buf = new byte[len];
                    in.readFully(buf);
                    String s = new String(buf);
                    if (col.isPrimaryKey()) {
                        return s;
                    }
                }
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }
}