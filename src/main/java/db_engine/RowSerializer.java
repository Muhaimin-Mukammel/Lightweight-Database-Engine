package db_engine;

import java.io.*;

public class RowSerializer {

    public static byte[] serialize(Row row, TableMeta meta) throws IOException {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bos);

        for (Column col : meta.getColumns()) {

            Object val = row.get(col.getName());

            switch (col.getType().toUpperCase()) {

                case "INT":
                    out.writeInt((Integer) val);
                    break;

                case "STRING":
                    String s = (String) val;
                    out.writeInt(s.length());
                    out.writeBytes(s);
                    break;
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
            throw new RuntimeException(e);
        }

        return null;
    }
}