package utils;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class dataSource {
    private String url="jdbc:mysql://localhost:3306/pidevversion2";
    private String username="root";
    private String pwd="";
    private static dataSource instance;
    private final ConcurrentLinkedQueue<Connection> connectionPool = new ConcurrentLinkedQueue<>();
    private static final int INITIAL_POOL_SIZE = 5;
    
    private dataSource() {
        try {
            for (int i = 0; i < INITIAL_POOL_SIZE; i++) {
                Connection conn = DriverManager.getConnection(url, username, pwd);
                connectionPool.add(conn);
            }
            System.out.println("✅ Connection pool initialized with " + INITIAL_POOL_SIZE + " connections!");
        } catch (SQLException e) {
            System.err.println("❌ Error initializing connection pool!");
            throw new RuntimeException(e);
        }
    }
    
    public static dataSource getInstance() {
        if (instance == null) {
            instance = new dataSource();
        }
        return instance;
    }
    
    public Connection getConnection() throws SQLException {
        Connection connection = connectionPool.poll();
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(url, username, pwd);
            System.out.println("Created new database connection");
        }
        return connection;
    }
    
    public void releaseConnection(Connection connection) {
        try {
            if (connection != null && !connection.isClosed()) {
                connectionPool.add(connection);
            }
        } catch (SQLException e) {
            System.err.println("Error releasing connection: " + e.getMessage());
        }
    }
    
    public void closeAllConnections() {
        int closedCount = 0;
        Connection conn;
        while ((conn = connectionPool.poll()) != null) {
            try {
                conn.close();
                closedCount++;
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
        System.out.println("Closed " + closedCount + " connections");
    }
}