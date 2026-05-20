package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL      = "jdbc:mysql://localhost:3306/pos_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&useUnicode=true";
    private static final String USER     = "root";
    private static final String PASSWORD = "0312";

    // One connection per server thread — avoids shared-state concurrency bugs
    private static final ThreadLocal<Connection> THREAD_CONN = new ThreadLocal<>();

    private DatabaseConnection() {}

    public static Connection getInstance() throws SQLException {
        Connection c = THREAD_CONN.get();
        if (c == null || c.isClosed()) {
            c = DriverManager.getConnection(URL, USER, PASSWORD);
            THREAD_CONN.set(c);
        }
        return c;
    }

    public static void close() {
        Connection c = THREAD_CONN.get();
        try {
            if (c != null && !c.isClosed()) c.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            THREAD_CONN.remove();
        }
    }
}
