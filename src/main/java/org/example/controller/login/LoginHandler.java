package org.example.controller.login;
import org.example.models.user.EmailSender;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.controller.menu.FrontMenu;
import org.example.controller.menu.menu;
import org.example.models.user.User;
import org.example.controller.login.SessionManager;
import utils.dataSource;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * Handles the login process and directs users to the appropriate menu based on their role
 */
public class LoginHandler {

    /**
     * Handle login with email and password
     *
     * @param email User's email
     * @param password User's password
     * @param stage Primary stage
     * @return true if login was successful
     */
    public static boolean handleLogin(String email, String password, Stage stage) {
        try {
            Connection conn = dataSource.getInstance().getConnection();
            String query = "SELECT * FROM user WHERE email = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString("password");
                boolean isAuthenticated = false;

                // Try BCrypt verification first
                try {
                    isAuthenticated = BCrypt.checkpw(password, storedPassword);
                } catch (IllegalArgumentException e) {
                    // If BCrypt verification fails, check if passwords match directly (legacy case)
                    isAuthenticated = password.equals(storedPassword);
                    
                    // If login successful with plain password, update to BCrypt hash
                    if (isAuthenticated) {
                        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
                        updatePasswordHash(conn, email, hashedPassword);
                    }
                }

                if (isAuthenticated) {
                    System.out.println("✅ Login successful!");

                    // Create user object from database result
                    int id = rs.getInt("id");
                    String userEmail = rs.getString("email");
                    String roles = rs.getString("roles");
                    String name = rs.getString("name");
                    int loginCount = rs.getInt("login_count");
                    String imageUrl = rs.getString("image_url");
                    String numTel = rs.getString("numTel");
                    String cin = rs.getString("cin");

                    // Handle the LocalDateTime conversion
                    LocalDateTime penalizedUntil = null;
                    if (rs.getTimestamp("penalized_until") != null) {
                        penalizedUntil = rs.getTimestamp("penalized_until").toLocalDateTime();
                    }

                    // Create the user with all constructor parameters
                    User currentUser = new User(
                            id,
                            userEmail,
                            storedPassword,
                            roles,
                            name,
                            loginCount,
                            imageUrl,
                            numTel,
                            cin,
                            penalizedUntil
                    );

                    // Start session by storing the current user
                    SessionManager.getInstance().setCurrentUser(currentUser);

                    System.out.println("✅ Session started for user ID: " + currentUser.getId());

                    // Show the appropriate menu based on user role
                    showMenuForUser(currentUser, stage);

                    return true;
                }
            }
            System.out.println("❌ Login failed: Invalid credentials");
            return false;
            
        } catch (SQLException e) {
            System.err.println("❌ Database error: " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            System.err.println("❌ Error during login: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Handle login with CIN
     *
     * @param cin User's CIN number
     * @param stage Primary stage
     * @return true if login was successful
     */
    public static boolean handleCINLogin(String cin, Stage stage) {
        try {
            Connection conn = dataSource.getInstance().getConnection();
            String query = "SELECT * FROM user WHERE cin = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, cin);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                System.out.println("✅ CIN Login successful!");

                // Create user object from database result
                int id = rs.getInt("id");
                String userEmail = rs.getString("email");
                String storedPassword = rs.getString("password");
                String roles = rs.getString("roles");
                String name = rs.getString("name");
                int loginCount = rs.getInt("login_count");
                String imageUrl = rs.getString("image_url");
                String numTel = rs.getString("numTel");
                String userCin = rs.getString("cin");

                // Handle the LocalDateTime conversion
                LocalDateTime penalizedUntil = null;
                if (rs.getTimestamp("penalized_until") != null) {
                    penalizedUntil = rs.getTimestamp("penalized_until").toLocalDateTime();
                }

                // Create the user with all constructor parameters
                User currentUser = new User(
                        id,
                        userEmail,
                        storedPassword,
                        roles,
                        name,
                        loginCount,
                        imageUrl,
                        numTel,
                        userCin,
                        penalizedUntil
                );

                // Start session by storing the current user
                SessionManager.getInstance().setCurrentUser(currentUser);

                System.out.println("✅ Session started for user ID: " + currentUser.getId());

                // Show the appropriate menu based on user role
                showMenuForUser(currentUser, stage);

                return true;
            }
            System.out.println("❌ CIN Login failed: No user found with CIN: " + cin);
            return false;
            
        } catch (SQLException e) {
            System.err.println("❌ Database error: " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            System.err.println("❌ Error during CIN login: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Show the appropriate menu based on user role
     *
     * @param user The authenticated user
     * @param stage The primary stage to show the menu on
     */
    private static void showMenuForUser(User user, Stage stage) {
        try {
            FXMLLoader loader;

            if (isAdminUser(user)) {
                // Admin users see the admin menu
                loader = new FXMLLoader(LoginHandler.class.getResource("/Menu/menu.fxml"));
                Parent root = loader.load();
                menu adminMenu = loader.getController();

                // Set up stage
                Scene scene = new Scene(root);
                stage.setScene(scene);
                stage.setTitle("Esprit Admin Panel");
                stage.show();

                System.out.println("✅ Loaded admin menu for user: " + user.getId());
            } else {
                // Regular users see the front-end menu
                loader = new FXMLLoader(LoginHandler.class.getResource("/Menu/front_menu.fxml"));
                Parent root = loader.load();
                FrontMenu frontMenu = loader.getController();

                // Set up stage
                Scene scene = new Scene(root);
                stage.setScene(scene);
                stage.setTitle("Esprit Learning Platform");
                stage.show();

                System.out.println("✅ Loaded front menu for user: " + user.getId());
            }
        } catch (Exception e) {
            System.err.println("❌ Error loading menu: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Determine if a user is an admin based on their role
     *
     * @param user The user object
     * @return true if the user is an admin, false otherwise
     */
    private static boolean isAdminUser(User user) {
        // Check if the roles field contains "admin"
        return user.getRoles() != null && user.getRoles().toLowerCase().contains("admin");
    }

    /**
     * Update user's password hash in the database
     */
    private static void updatePasswordHash(Connection conn, String email, String hashedPassword) {
        try {
            String updateQuery = "UPDATE user SET password = ? WHERE email = ?";
            PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
            updateStmt.setString(1, hashedPassword);
            updateStmt.setString(2, email);
            updateStmt.executeUpdate();
            System.out.println("✅ Updated password hash for user: " + email);
        } catch (SQLException e) {
            System.err.println("❌ Failed to update password hash: " + e.getMessage());
        }
    }
}