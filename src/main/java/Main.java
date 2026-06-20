import CRUD.CrudService;
import DashBoard.Dashboard;
import db_engine.StorageEngine;

public class Main {

    public static void main(String[] args) throws Exception {

        StorageEngine engine = new StorageEngine("data.db");

        CrudService crud = new CrudService(engine);

        Dashboard dashboard = new Dashboard(crud);

        dashboard.run();
    }
}