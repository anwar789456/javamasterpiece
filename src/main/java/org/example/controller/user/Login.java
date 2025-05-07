package org.example.controller.user;


import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.example.models.user.User;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import utils.dataSource;

public class Login {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;
    
    @FXML
    private Label errorMessageLabel;
    
    // For testing database connectivity
    private boolean isDataSourceWorking = false;
    private boolean isDebugMode = true; // Set to true to see debug logs
    
    // Reference to the stage for UI updates
    private Stage currentStage;

    @FXML
    private void initialize() {
        // Handle button click event
        loginButton.setOnAction(event -> handleLogin());
        
        // Initialize error message label if it exists
        if (errorMessageLabel != null) {
            errorMessageLabel.setVisible(false);
            errorMessageLabel.setTextFill(Color.RED);
        } else {
            debugLog("Warning: errorMessageLabel not found in FXML");
        }
        
        // Store reference to current stage
        currentStage = (Stage) loginButton.getScene().getWindow();
        
        // Test database connection
        testDbConnection();
    }
    
    private void debugLog(String message) {
        if (isDebugMode) {
            System.out.println("[DEBUG] " + message);
        }
    }
    
    private void testDbConnection() {
        Connection conn = null;
        try {
            debugLog("Testing database connection...");
            conn = dataSource.getInstance().getConnection();
            if (conn != null && !conn.isClosed()) {
                isDataSourceWorking = true;
                debugLog("✅ Database connection is working");
                
                // Test a simple query to ensure we can actually query the database
                try {
                    String testQuery = "SELECT COUNT(*) FROM user";
                    PreparedStatement stmt = conn.prepareStatement(testQuery);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        int count = rs.getInt(1);
                        debugLog("✅ Database query successful. Found " + count + " users in the database.");
                    } else {
                        debugLog("⚠️ Database query returned no results, but didn't error.");
                    }
                    rs.close();
                    stmt.close();
                } catch (SQLException e) {
                    debugLog("❌ Test query failed: " + e.getMessage());
                    isDataSourceWorking = false;
                }
            }
        } catch (Exception e) {
            isDataSourceWorking = false;
            debugLog("❌ Database connection test failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    dataSource.getInstance().releaseConnection(conn);
                } catch (Exception e) {
                    debugLog("❌ Error releasing connection: " + e.getMessage());
                }
            }
        }
    }
    
    private void showErrorMessage(String message) {
        if (errorMessageLabel != null) {
            errorMessageLabel.setText(message);
            errorMessageLabel.setVisible(true);
        }
        // Always print to console for debugging
        System.out.println("❌ " + message);
    }

    private void handleLogin() {
        // Clear any previous error message
        if (errorMessageLabel != null) {
            errorMessageLabel.setVisible(false);
        }
        
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        debugLog("Login attempt with email: " + email);
        
        // Simple validation logic
        if (email.isEmpty() || password.isEmpty()) {
            showErrorMessage("Please fill out both fields.");
            return;
        }
        
        // Default to authentication failure
        boolean authSuccess = false;
        User user = null;
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            if (!isDataSourceWorking) {
                showErrorMessage("Database connection is not available. Please check your configuration.");
                return;
            }
            
            conn = dataSource.getInstance().getConnection();
            String query = "SELECT * FROM user WHERE email = ?";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, email);
            debugLog("Executing query: " + query + " with email: " + email);
            
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                // User exists, now check password
                String dbPassword = rs.getString("password");
                debugLog("Found user with email: " + email);
                
                if (password.equals(dbPassword)) {
                    debugLog("✅ Password matches for user: " + email);
                    
                    // Create User object from database result
                    user = new User();
                    user.setId(rs.getInt("id"));
                    user.setEmail(rs.getString("email"));
                    user.setPassword(rs.getString("password"));
                    user.setRoles(rs.getString("roles"));
                    user.setName(rs.getString("name"));
                    user.setLoginCount(rs.getInt("login_count"));
                    user.setImageUrl(rs.getString("image_url"));
                    user.setNumTel(rs.getString("numtel"));
                    
                    // Get penalized_until if not null
                    java.sql.Timestamp penalizedTimestamp = rs.getTimestamp("penalized_until");
                    if (penalizedTimestamp != null) {
                        user.setPenalizedUntil(penalizedTimestamp.toLocalDateTime());
                    }
                    
                    // Check if user is penalized
                    if (user.isPenalized()) {
                        showErrorMessage("Your account is suspended until " + user.getPenalizedUntil());
                        return;
                    }
                    
                    // Authentication successful
                    authSuccess = true;
                } else {
                    // Password doesn't match
                    debugLog("❌ Password doesn't match for user: " + email);
                    debugLog("  Entered: " + password);
                    debugLog("  Expected: " + dbPassword);
                    showErrorMessage("Incorrect password. Please try again.");
                    return; // Return early to prevent proceeding with login
                }
            } else {
                // User doesn't exist
                debugLog("❌ No user found with email: " + email);
                showErrorMessage("No account found with this email. Please check your email or register.");
                return; // Return early to prevent proceeding with login
            }
            
        } catch (SQLException e) {
            debugLog("SQL Exception: " + e.getMessage());
            showErrorMessage("Database error: " + e.getMessage());
            e.printStackTrace();
            return; // Return early to prevent proceeding with login
        } catch (Exception e) {
            debugLog("Exception: " + e.getMessage());
            showErrorMessage("Error: " + e.getMessage());
            e.printStackTrace();
            return; // Return early to prevent proceeding with login
        } finally {
            // Close resources in finally block
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) dataSource.getInstance().releaseConnection(conn);
            } catch (SQLException e) {
                debugLog("Error closing resources: " + e.getMessage());
            }
        }
        
        // Proceed with login ONLY if authentication was successful and we have a user object
        if (authSuccess && user != null) {
            debugLog("✅ Authentication successful, proceeding with login for user: " + user.getEmail());
            try {
                // Increment login count
                user.incrementLoginCount();
                
                // Update login count in database
                updateLoginCount(user.getId(), user.getLoginCount());
                
                FXMLLoader loader;
                Parent root;
                Stage stage = new Stage();
                
                // Check if user is admin and load appropriate menu
                if (user.isAdmin()) {
                    // Load admin dashboard
                    debugLog("Loading admin dashboard for user: " + user.getEmail());
                    loader = new FXMLLoader(getClass().getResource("/Menu/menu.fxml"));
                    root = loader.load();
                    stage.setTitle("Admin Dashboard");
                } else {
                    // Load regular user interface
                    debugLog("Loading user interface for user: " + user.getEmail());
                    loader = new FXMLLoader(getClass().getResource("/Menu/front_menu.fxml"));
                    root = loader.load();
                    stage.setTitle("User Interface");
                    
                    // If front_menu controller has a method to set user
                    try {
                        org.example.controller.menu.FrontMenu frontMenu = loader.getController();
                        frontMenu.setCurrentUserId(user.getId());
                    } catch (Exception e) {
                        debugLog("Error setting user ID: " + e.getMessage());
                    }
                }
                
                stage.setScene(new Scene(root));
                stage.show();

                // Close the login window
                loginButton.getScene().getWindow().hide();
            } catch (Exception e) {
                debugLog("Error in UI loading: " + e.getMessage());
                showErrorMessage("Error loading application: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            debugLog("❌ Authentication failed or user is null");
            showErrorMessage("Login failed. Please try again.");
        }
    }
    
    /**
     * Update user's login count in the database
     */
    private void updateLoginCount(int userId, int loginCount) {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = dataSource.getInstance().getConnection();
            String query = "UPDATE user SET login_count = ? WHERE id = ?";
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, loginCount);
            stmt.setInt(2, userId);
            int updatedRows = stmt.executeUpdate();
            debugLog("Updated login count for user ID " + userId + ": " + updatedRows + " rows affected");
        } catch (SQLException e) {
            debugLog("Error updating login count: " + e.getMessage());
        } finally {
            try {
                if (stmt != null) stmt.close();
                if (conn != null) dataSource.getInstance().releaseConnection(conn);
            } catch (SQLException e) {
                debugLog("Error closing resources: " + e.getMessage());
            }
        }
    }
}