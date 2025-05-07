package utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Test class to verify database connection and authentication
 */
public class DBTest {
    
    public static void main(String[] args) {
        testConnection();
        testUserAuthentication("test@example.com", "password");
    }
    
    public static void testConnection() {
        System.out.println("Testing database connection...");
        Connection conn = null;
        try {
            conn = utils.dataSource.getInstance().getConnection();
            if (conn != null && !conn.isClosed()) {
                System.out.println("Database connection successful!");
                
                // Test a simple query
                try {
                    String testQuery = "SHOW TABLES";
                    PreparedStatement stmt = conn.prepareStatement(testQuery);
                    ResultSet rs = stmt.executeQuery();
                    
                    System.out.println("\nTables in database:");
                    int tableCount = 0;
                    while (rs.next()) {
                        System.out.println(" - " + rs.getString(1));
                        tableCount++;
                    }
                    System.out.println("Total tables: " + tableCount);
                    
                    // Check if user table exists
                    testQuery = "SHOW COLUMNS FROM user";
                    stmt = conn.prepareStatement(testQuery);
                    rs = stmt.executeQuery();
                    
                    System.out.println("\nColumns in user table:");
                    while (rs.next()) {
                        System.out.println(" - " + rs.getString(1) + " (" + rs.getString(2) + ")");
                    }
                    
                    // Count users
                    testQuery = "SELECT COUNT(*) FROM user";
                    stmt = conn.prepareStatement(testQuery);
                    rs = stmt.executeQuery();
                    
                    if (rs.next()) {
                        int count = rs.getInt(1);
                        System.out.println("\nTotal users in database: " + count);
                    }
                    
                    rs.close();
                    stmt.close();
                } catch (SQLException e) {
                    System.err.println("ERROR: Test query failed: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("ERROR: Database connection test failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    utils.dataSource.getInstance().releaseConnection(conn);
                } catch (Exception e) {
                    System.err.println("ERROR: Error releasing connection: " + e.getMessage());
                }
            }
        }
    }
    
    public static void testUserAuthentication(String email, String password) {
        System.out.println("\nTesting user authentication...");
        System.out.println("Email: " + email);
        System.out.println("Password: " + password);
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = utils.dataSource.getInstance().getConnection();
            
            // First query to get all users for debugging
            String listQuery = "SELECT id, email, password, roles FROM user LIMIT 5";
            stmt = conn.prepareStatement(listQuery);
            rs = stmt.executeQuery();
            
            System.out.println("\nSample user accounts:");
            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("id") + 
                                   ", Email: " + rs.getString("email") + 
                                   ", Password: " + rs.getString("password") +
                                   ", Roles: " + rs.getString("roles"));
            }
            rs.close();
            stmt.close();
            
            // Test authentication
            String query = "SELECT * FROM user WHERE email = ?";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, email);
            
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                String dbPassword = rs.getString("password");
                System.out.println("\nFound user with email: " + email);
                System.out.println("Database password: " + dbPassword);
                
                if (password.equals(dbPassword)) {
                    System.out.println("SUCCESS: Password matches - Login would be successful!");
                } else {
                    System.out.println("ERROR: Password does not match - Login would fail!");
                }
            } else {
                System.out.println("ERROR: No user found with email: " + email);
            }
            
        } catch (SQLException e) {
            System.err.println("ERROR: SQL error during authentication test: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) utils.dataSource.getInstance().releaseConnection(conn);
            } catch (SQLException e) {
                System.err.println("ERROR: Error closing resources: " + e.getMessage());
            }
        }
    }
} 