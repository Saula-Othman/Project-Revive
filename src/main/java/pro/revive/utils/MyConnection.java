package pro.revive.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyConnection {

    private static final String URL   = "jdbc:mysql://localhost:3306/revive";
    private static final String LOGIN = "root";
    private static final String PWD   = "";

    private static MyConnection instance;
    private Connection cnx;

    private MyConnection() {
        try {
            cnx = DriverManager.getConnection(URL, LOGIN, PWD);
            System.out.println("Connexion etablie!");
        } catch (SQLException e) {
            System.err.println("ERREUR CONNEXION DB: " + e.getMessage());
        }
    }

    /** Returns the singleton instance, reconnecting if the connection was lost. */
    public static MyConnection getInstance() {
        try {
            if (instance == null || instance.cnx == null || instance.cnx.isClosed()) {
                instance = new MyConnection();
            }
        } catch (SQLException e) {
            instance = new MyConnection();
        }
        return instance;
    }

    public Connection getCnx() {
        return cnx;
    }
}
