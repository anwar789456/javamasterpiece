package org.example.controller.user;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import org.example.controller.login.SessionManager;
import org.example.models.user.User;
import org.example.models.Badge;
import utils.dataSource;
import org.example.service.BadgeService;
import javafx.scene.layout.FlowPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.collections.FXCollections;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.List;

/**
 * Controller for the client account display screen
 */
public class UserFrontController implements Initializable {

    @FXML
    private Label nameLabel;

    @FXML
    private Label roleLabel;

    @FXML
    private Label memberSinceLabel;

    @FXML
    private Label emailLabel;

    @FXML
    private Label phoneLabel;

    @FXML
    private Label registrationDateLabel;

    @FXML
    private Label lastLoginLabel;

    @FXML
    private Label loginCountLabel;

    @FXML
    private Button editProfileButton;

    @FXML
    private Button changePasswordButton;

    @FXML
    private ToggleButton notificationsToggle;

    @FXML
    private Button logoutButton;

    @FXML
    private Button deactivateAccountButton;

    @FXML
    private Label cinLabel;

    @FXML
    private Label averageRatingLabel;

    @FXML
    private FlowPane badgesFlowPane;

    @FXML
    private Button giveBadgeButton;

    @FXML
    private BorderPane mainContent;

    // Date formatter for display
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");

    // Current logged-in user
    private User currentUser;

    // Session manager instance
    private SessionManager sessionManager;

    // User ID passed from FrontMenu
    private int userId;

    private final BadgeService badgeService = new BadgeService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Get the session manager instance
        sessionManager = SessionManager.getInstance();

        // Get the current logged-in user from session
        currentUser = sessionManager.getCurrentUser();

        userId= currentUser.getId();

        // If no user is logged in, show an error and return
        if (currentUser == null) {
            showError("No user logged in!");
            return;
        }

        // Load user data
        loadUserData();

        // Setup event handlers
        setupEventHandlers();

        // Set toggle button style change based on selection
        styleToggleButton();

        // Setup badge button handler
        giveBadgeButton.setOnAction(event -> handleGiveBadge());
        
        // Show admin button if user is an admin
        if (currentUser.getRoles().contains("ROLE_ADMIN")) {
            Button adminButton = new Button("Admin Functions");
            adminButton.setStyle("-fx-background-color: #673AB7; -fx-text-fill: white;");
            adminButton.setOnAction(event -> showAdminSection());
            // Add the button to the UI (you'll need to adjust this based on your layout)
            if (badgesFlowPane.getParent() instanceof VBox) {
                VBox container = (VBox) badgesFlowPane.getParent();
                container.getChildren().add(adminButton);
            }
        }
        
        // Load badges
        loadBadges();
    }

    /**
     * Set the current user ID from FrontMenu
     * This method is required for compatibility with the FrontMenu controller
     * @param userId The ID of the current user
     */
    public void setCurrentUserId(int userId) {
        this.userId = userId;

        // If the user ID changes and we've already initialized, reload user data
        if (currentUser != null && currentUser.getId() != userId) {
            // In a real application, you would fetch the user with this ID from the database
            // For now, if we already have a session user, we'll just verify the ID matches
            if (sessionManager.getCurrentUserId() != userId) {
                System.out.println("Warning: User ID mismatch between session and menu!");
                // Here you might want to reload the user data from a database based on userId
            }
        }
    }

    /**
     * Load user data into the UI components
     */
    private void loadUserData() {
        try {
            Connection conn = dataSource.getInstance().getConnection();
            String query = "SELECT id, name, email, numTel, cin FROM user WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, currentUser.getId());
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                currentUser.setName(rs.getString("name"));
                currentUser.setEmail(rs.getString("email"));
                currentUser.setNumTel(rs.getString("numTel"));
                currentUser.setCin(rs.getString("cin"));
                
                // Update UI elements
                nameLabel.setText(currentUser.getName());
                
                // Format role for display (remove ROLE_ prefix and capitalize)
                String role = currentUser.getRoles().replace("ROLE_", "");
                role = role.substring(0, 1) + role.substring(1).toLowerCase();
                roleLabel.setText(role);
                
                // Set member since date
                memberSinceLabel.setText("Member since: " + LocalDate.now().format(dateFormatter));
                
                emailLabel.setText(currentUser.getEmail());
                phoneLabel.setText(currentUser.getNumTel().isEmpty() ? "Not provided" : currentUser.getNumTel());
                cinLabel.setText(currentUser.getCin() != null ? currentUser.getCin() : "Not provided");
                
                // Set registration date
                registrationDateLabel.setText(LocalDate.now().format(dateFormatter));
                
                // Set last login
                lastLoginLabel.setText("Today at " + LocalDateTime.now().format(timeFormatter));
                
                // Set login count
                loginCountLabel.setText(String.valueOf(currentUser.getLoginCount()));
            }
            
            rs.close();
            stmt.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Error loading user data: " + e.getMessage());
        }
    }

    /**
     * Setup event handlers for buttons and controls
     */
    private void setupEventHandlers() {
        // Edit Profile button - modified to not handle images
        editProfileButton.setOnAction(event -> handleEditProfile());

        // Change Password button
        changePasswordButton.setOnAction(event -> handleChangePassword());

        // Notifications Toggle
        notificationsToggle.setOnAction(event -> handleToggleNotifications());

        // Logout button
        logoutButton.setOnAction(event -> handleLogout());

        // Deactivate Account button
        deactivateAccountButton.setOnAction(event -> handleDeactivateAccount());
    }

    /**
     * Style the toggle button based on its state
     */
    private void styleToggleButton() {
        // Set initial state
        updateToggleButtonStyle();

        // Add listener for state changes
        notificationsToggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
            updateToggleButtonStyle();
        });
    }

    private void updateToggleButtonStyle() {
        if (notificationsToggle.isSelected()) {
            notificationsToggle.setText("ON");
            notificationsToggle.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white; -fx-background-radius: 15;");
        } else {
            notificationsToggle.setText("OFF");
            notificationsToggle.setStyle("-fx-background-color: #9e9e9e; -fx-text-fill: white; -fx-background-radius: 15;");
        }
    }

    /**
     * Handle Edit Profile button click - modified to handle profile editing without images
     */
    private void handleEditProfile() {
        // Create a dialog for editing profile
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Profile");
        dialog.setHeaderText("Update your profile information");

        // Set the button types
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create the form fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField(currentUser.getName());
        nameField.setPromptText("Full Name");
        TextField emailField = new TextField(currentUser.getEmail());
        emailField.setPromptText("Email");
        TextField phoneField = new TextField(currentUser.getNumTel());
        phoneField.setPromptText("Phone Number");
        TextField cinField = new TextField(currentUser.getCin());
        cinField.setPromptText("CIN");

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Email:"), 0, 1);
        grid.add(emailField, 1, 1);
        grid.add(new Label("Phone:"), 0, 2);
        grid.add(phoneField, 1, 2);
        grid.add(new Label("CIN:"), 0, 3);
        grid.add(cinField, 1, 3);

        dialog.getDialogPane().setContent(grid);

        // Request focus on the name field by default
        nameField.requestFocus();

        // Show the dialog and wait for a response
        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == saveButtonType) {
            // Validate inputs
            if (nameField.getText().isEmpty() || emailField.getText().isEmpty()) {
                showError("Name and email are required fields!");
                return;
            }

            // Update user information
            currentUser.setName(nameField.getText());
            currentUser.setEmail(emailField.getText());
            currentUser.setNumTel(phoneField.getText());
            currentUser.setCin(cinField.getText());

            try {
                // Save the updated user to the database
                if (saveUser(currentUser)) {
                    // Update the UI
                    loadUserData();
                    showSuccess("Profile updated successfully!");
                } else {
                    showError("Failed to update profile. Please try again.");
                }
            } catch (Exception e) {
                showError("Error updating profile: " + e.getMessage());
            }
        }
    }

    private boolean saveUser(User user) {
        try {
            // Get database connection
            Connection conn = dataSource.getInstance().getConnection();
            
            // Prepare the update statement with correct column names
            String query = "UPDATE user SET name = ?, email = ?, numTel = ?, cin = ? WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            
            // Set parameters
            stmt.setString(1, user.getName());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getNumTel());
            stmt.setString(4, user.getCin());
            stmt.setInt(5, user.getId());
            
            // Execute update
            int rowsAffected = stmt.executeUpdate();
            
            // Close resources
            stmt.close();
            conn.close();
            
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Handle Change Password button click
     */
    private void handleChangePassword() {
        // Create a dialog for password change
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Change Password");
        dialog.setHeaderText("Enter your current password and a new password");

        // Set the button types
        ButtonType changeButtonType = new ButtonType("Change", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(changeButtonType, ButtonType.CANCEL);

        // Create the password fields
        PasswordField currentPassword = new PasswordField();
        currentPassword.setPromptText("Current Password");
        PasswordField newPassword = new PasswordField();
        newPassword.setPromptText("New Password");
        PasswordField confirmPassword = new PasswordField();
        confirmPassword.setPromptText("Confirm New Password");

        // Create a grid for the fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        grid.add(new Label("Current Password:"), 0, 0);
        grid.add(currentPassword, 1, 0);
        grid.add(new Label("New Password:"), 0, 1);
        grid.add(newPassword, 1, 1);
        grid.add(new Label("Confirm New Password:"), 0, 2);
        grid.add(confirmPassword, 1, 2);

        dialog.getDialogPane().setContent(grid);

        // Request focus on the current password field by default
        currentPassword.requestFocus();

        // Show the dialog and wait for a response
        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == changeButtonType) {
            // Validate inputs
            if (currentPassword.getText().isEmpty() || newPassword.getText().isEmpty() || confirmPassword.getText().isEmpty()) {
                showError("All fields are required!");
                return;
            }

            // Check if current password is correct
            if (!currentPassword.getText().equals(currentUser.getPassword())) {
                showError("Current password is incorrect!");
                return;
            }

            // Check if new passwords match
            if (!newPassword.getText().equals(confirmPassword.getText())) {
                showError("New passwords do not match!");
                return;
            }

            // Check if new password meets requirements
            if (newPassword.getText().length() < 6) {
                showError("New password must be at least 6 characters!");
                return;
            }

            // Update password in database
            try {
                Connection conn = dataSource.getInstance().getConnection();
                String query = "UPDATE user SET password = ? WHERE id = ?";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setString(1, newPassword.getText());
                stmt.setInt(2, currentUser.getId());

                int rowsAffected = stmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    // Update the current user's password in memory
                    currentUser.setPassword(newPassword.getText());
                    showSuccess("Password changed successfully!");
                } else {
                    showError("Failed to update password. Please try again.");
                }

                stmt.close();
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
                showError("Database error: " + e.getMessage());
            }
        }
    }

    /**
     * Handle Toggle Notifications button click
     */
    private void handleToggleNotifications() {
        boolean enabled = notificationsToggle.isSelected();
        System.out.println("Notifications " + (enabled ? "enabled" : "disabled"));

        // In a real application, you would save this preference to the user's profile
    }

    /**
     * Handle Logout button click
     */
    private void handleLogout() {
        try {
            // Clear the session
            sessionManager.clearSession();

            // Load the login page
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Parent root = loader.load();

            // Get the current stage
            Stage stage = (Stage) logoutButton.getScene().getWindow();

            // Set the new scene
            stage.setScene(new Scene(root));
            stage.setTitle("Login");
        } catch (IOException e) {
            e.printStackTrace();
            showError("Error during logout: " + e.getMessage());
        }
    }

    /**
     * Handle Deactivate Account button click
     */
    private void handleDeactivateAccount() {
        // Show confirmation dialog with extra warning
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Deactivate Account");
        alert.setHeaderText("Are you sure you want to deactivate your account?");
        alert.setContentText("This action cannot be undone. All your data will be permanently deleted.");

        // Create custom buttons
        ButtonType deactivateButton = new ButtonType("Deactivate", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(cancelButton, deactivateButton);

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == deactivateButton) {
            try {
                // Delete the user from the database
                Connection conn = dataSource.getInstance().getConnection();
                String query = "DELETE FROM user WHERE id = ?";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setInt(1, currentUser.getId());

                int rowsAffected = stmt.executeUpdate();

                if (rowsAffected > 0) {
                    // Clear the session
                    sessionManager.clearSession();

                    // Show success message
                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("Account Deactivated");
                    successAlert.setHeaderText("Your account has been deactivated successfully");
                    successAlert.setContentText("You will now be redirected to the login page.");
                    successAlert.showAndWait();

                    // Redirect to login page
                    loadLoginPage();
                } else {
                    showError("Failed to deactivate account");
                }

                stmt.close();
                conn.close();
            } catch (SQLException e) {
                showError("Database error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Load the login page
     */
    private void loadLoginPage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) deactivateAccountButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Login");
        } catch (IOException e) {
            e.printStackTrace();
            showError("Error loading login page: " + e.getMessage());
        }
    }

    /**
     * Show an error message
     *
     * @param message The error message to display
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show a success message
     *
     * @param message The success message to display
     */
    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void loadBadges() {
        // Clear existing badges
        badgesFlowPane.getChildren().clear();
        
        // Get user's badges
        List<Badge> badges = badgeService.getUserBadges(currentUser.getId());
        
        // Calculate and display average rating
        double averageStars = badgeService.getUserAverageStars(currentUser.getId());
        averageRatingLabel.setText(String.format("%.1f", averageStars));
        
        // Add badge images to the flow pane
        for (Badge badge : badges) {
            ImageView badgeImage = new ImageView();
            badgeImage.setFitWidth(40);
            badgeImage.setFitHeight(40);
            badgeImage.setPreserveRatio(true);
            
            // Set the appropriate badge image based on type
            String imagePath = badge.getTypeBadge().equals("gold") ? 
                "/images/gold_badge.png" : "/images/silver_badge.png";
            
            try {
                badgeImage.setImage(new Image(getClass().getResourceAsStream(imagePath)));
            } catch (Exception e) {
                System.err.println("Error loading badge image: " + e.getMessage());
            }
            
            // Add tooltip with number of stars
            Tooltip tooltip = new Tooltip(badge.getNbrStars() + " stars");
            Tooltip.install(badgeImage, tooltip);
            
            badgesFlowPane.getChildren().add(badgeImage);
        }
    }

    private void handleGiveBadge() {
        // Create a dialog for giving a badge
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Give Badge");
        dialog.setHeaderText("Select a user and give them a badge");

        // Create the form fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField emailField = new TextField();
        emailField.setPromptText("User Email");
        ComboBox<Integer> starsComboBox = new ComboBox<>();
        starsComboBox.getItems().addAll(1, 2, 3, 4, 5);
        starsComboBox.setValue(3);

        grid.add(new Label("User Email:"), 0, 0);
        grid.add(emailField, 1, 0);
        grid.add(new Label("Stars:"), 0, 1);
        grid.add(starsComboBox, 1, 1);

        dialog.getDialogPane().setContent(grid);

        // Add buttons
        ButtonType giveButton = new ButtonType("Give Badge", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(giveButton, ButtonType.CANCEL);

        // Show the dialog and wait for a response
        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == giveButton) {
            try {
                String email = emailField.getText();
                int stars = starsComboBox.getValue();

                if (badgeService.addBadgeByEmail(email, stars)) {
                    showSuccess("Badge given successfully!");
                } else {
                    showError("Failed to give badge. Please try again.");
                }
            } catch (Exception e) {
                showError("Error: " + e.getMessage());
            }
        }
    }

    private void showBadgeList() {
        try {
            List<Badge> badges = badgeService.getAllBadges();
            
            // Create a dialog to display the badge list
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Badge List");
            dialog.setHeaderText("All Badges in the System");

            // Create a table to display badges
            TableView<Badge> table = new TableView<>();
            
            // Create columns
            TableColumn<Badge, String> emailColumn = new TableColumn<>("User Email");
            emailColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getUserEmail()));
            
            TableColumn<Badge, Integer> starsColumn = new TableColumn<>("Stars");
            starsColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getNbrStars()).asObject());
            
            TableColumn<Badge, String> dateColumn = new TableColumn<>("Date Awarded");
            dateColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDateAwarded().toString()));

            // Add columns to table
            table.getColumns().addAll(emailColumn, starsColumn, dateColumn);
            
            // Set items
            table.setItems(FXCollections.observableArrayList(badges));
            
            // Set table properties
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            table.setPrefHeight(400);
            
            // Add table to dialog
            dialog.getDialogPane().setContent(table);
            
            // Add close button
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            
            // Show dialog
            dialog.showAndWait();
        } catch (Exception e) {
            showError("Error loading badge list: " + e.getMessage());
        }
    }

    private void showAdminSection() {
        // Create admin section content
        VBox adminContent = new VBox(10);
        adminContent.setPadding(new Insets(20));
        adminContent.setAlignment(Pos.CENTER);

        // Add buttons for admin actions
        Button giveBadgeButton = new Button("Give Badge");
        Button viewBadgeListButton = new Button("View Badge List");
        Button backButton = new Button("Back");

        // Style buttons
        giveBadgeButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        viewBadgeListButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        backButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");

        // Add action handlers
        giveBadgeButton.setOnAction(e -> handleGiveBadge());
        viewBadgeListButton.setOnAction(e -> showBadgeList());
        backButton.setOnAction(e -> showMainContent());

        // Add buttons to admin content
        adminContent.getChildren().addAll(giveBadgeButton, viewBadgeListButton, backButton);

        // Set the admin content as the main content
        mainContent.getChildren().clear();
        mainContent.getChildren().add(adminContent);
    }

    private void showMainContent() {
        // This method will be called to return to the main user view
        if (mainContent != null) {
            // Simply reload the current screen
            loadUserData();
            loadBadges();
        }
    }
}