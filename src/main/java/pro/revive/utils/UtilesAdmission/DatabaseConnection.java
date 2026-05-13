package pro.revive.utils.UtilesAdmission;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL =
        "jdbc:mysql://localhost:3306/revive" +
        "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" +
        "&characterEncoding=UTF-8&useUnicode=true";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private static Connection connection;

    public static Connection getConnection() throws SQLException {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
            }
        } catch (SQLException e) {
            throw new SQLException("Impossible de se connecter a la base de donnees.\n" +
                "Verifiez que WAMP est demarree et que la base 'urgence_db' existe.\n" +
                "Erreur: " + e.getMessage(), e);
        }
        return connection;
    }

    public static void closeConnection() {
        if (connection != null) {
            try { connection.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }
}
