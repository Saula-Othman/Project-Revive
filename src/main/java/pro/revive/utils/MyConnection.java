package pro.revive.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Thread-safe DB access via HikariCP connection pool.
 *
 * Each call to getCnx() borrows a connection from the pool and MUST be
 * used inside a try-with-resources block so it is returned automatically:
 *
 *   try (Connection c = MyConnection.getInstance().getCnx()) { ... }
 *
 * For transactional operations use runInTransaction().
 */
public class MyConnection {

    private static MyConnection instance;
    private final HikariDataSource dataSource;

    private MyConnection() {
        // ── Configure HikariCP ────────────────────────────────────
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost:3306/revive");
        config.setUsername("root");
        config.setPassword("");

        // Pool sizing: small app, keep it lean
        config.setMinimumIdle(2);
        config.setMaximumPoolSize(10);
        config.setConnectionTimeout(30_000);   // 30 s
        config.setIdleTimeout(600_000);        // 10 min
        config.setMaxLifetime(1_800_000);      // 30 min

        // Keep connections alive (MySQL drops idle connections after 8 h)
        config.setKeepaliveTime(60_000);       // ping every 60 s
        config.setConnectionTestQuery("SELECT 1");

        config.setPoolName("RevivePool");

        dataSource = new HikariDataSource(config);
        System.out.println("Pool de connexions initialise (HikariCP).");
    }

    public static synchronized MyConnection getInstance() {
        if (instance == null) {
            instance = new MyConnection();
        }
        return instance;
    }

    /**
     * Borrow a connection from the pool.
     * ALWAYS use inside try-with-resources — the pool reclaims it on close().
     */
    public Connection getCnx() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException("Impossible d'obtenir une connexion: " + e.getMessage(), e);
        }
    }

    /**
     * Executes a transactional block on a single borrowed connection.
     * Commits on success, rolls back on any exception.
     */
    public void runInTransaction(TransactionBlock block) throws Exception {
        try (Connection c = getCnx()) {
            c.setAutoCommit(false);
            try {
                block.execute(c);
                c.commit();
            } catch (Exception e) {
                try { c.rollback(); } catch (SQLException ignored) {}
                throw e;
            } finally {
                try { c.setAutoCommit(true); } catch (SQLException ignored) {}
            }
        }
    }

    /** Shut down the pool cleanly (call from App.stop() if needed). */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("Pool de connexions ferme.");
        }
    }

    @FunctionalInterface
    public interface TransactionBlock {
        void execute(Connection c) throws Exception;
    }
}
