package db_engine;

import java.util.List;

public class Table {
        private String name;
        private List<Column> columns;

        public Table(String name, List<Column> columns) {
            this.name = name;
            this.columns = columns;
        }

        public String getName() {
            return name;
        }

        public List<Column> getColumns() {
            return columns;
        }
}
