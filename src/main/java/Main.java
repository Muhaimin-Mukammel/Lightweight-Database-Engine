import Combiner.combiner;
import db_engine.Database;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        Database database = new Database();
        Scanner scanner = new Scanner(System.in);
        combiner combiner1 = new combiner(database, scanner);
        combiner1.integrate();
    }
}