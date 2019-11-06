package win.oakcsclub;

import org.jdbi.v3.core.Jdbi;

import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {

    public static Jdbi get(){
        return connection;
    }

    private static Jdbi connection = null;

    static {
        try {
            connection = Jdbi.create(DriverManager.getConnection("jdbc:sqlite:database.db"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


}