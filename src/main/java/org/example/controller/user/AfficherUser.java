package org.example.controller.user;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

import org.example.models.user.User;
import org.example.controller.login.SessionManager;
import utils.dataSource;

public class AfficherUser implements Initializable {

    @FXML private ImageView userImageView;
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField phoneField;
    @FXML private TextField roleField;

    @FXML private Label nameErrorLabel;
    @FXML private Label emailErrorLabel;
    @FXML private Label passwordErrorLabel;
    @FXML private Label phoneErrorLabel;
    @FXML private Label statusLabel;

    @FXML private Button uploadImageButton;
    @FXML private Button updateButton;
    @FXML private Button deleteButton;
    @FXML private Button backButton;

    private User currentUser;
    private String newImagePath;

    // Validation patterns
    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\p{L} '-]{1,30}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{8}$");

    @Override
    public void initialize(URL location, ResourceBundle resources) {


        currentUser = SessionManager.getInstance().getCurrentUser();

        if (currentUser == null) {
            showGlobalError("No user is logged in!");
            return;
        }

        // Set up UI elements with user data
        loadUserData();

        // Set up event handlers for buttons
        setupEventHandlers();

        // Add listeners for input validation
        setupInputValidation();
    }

    private void loadUserData() {
        // Populate form fields with user data
        nameField.setText(currentUser.getName());
        emailField.setText(currentUser.getEmail());
        // Don't set password for security reasons
        passwordField.setText("");
        phoneField.setText(currentUser.getNumTel());
        roleField.setText(currentUser.getRoles());

        // Load user profile image if available
        if (currentUser.getImageUrl() != null && !currentUser.getImageUrl().isEmpty()) {
            try {
                Image image = new Image(new File(currentUser.getImageUrl()).toURI().toString());
                userImageView.setImage(image);
            } catch (Exception e) {
                System.err.println("Error loading user image: " + e.getMessage());
                // Set a default image
                userImageView.setImage(new Image(getClass().getResourceAsStream("/images/default_user.png")));
            }
        } else {
            // Set a default image
            userImageView.setImage(new Image(getClass().getResourceAsStream("/images/default_user.png")));
        }
    }

    private void setupEventHandlers() {
        // Handle upload image button
        uploadImageButton.setOnAction(event -> handleImageUpload());

        // Handle update information button
        updateButton.setOnAction(event -> handleUpdateUser());

        // Handle delete account button
        deleteButton.setOnAction(event -> handleDeleteAccount());

        // Handle back button
        backButton.setOnAction(event -> handleBackButton());
    }

    private void setupInputValidation() {
        // Validate name field - only characters, max 30 chars
        nameField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!NAME_PATTERN.matcher(newValue).matches()) {
                nameErrorLabel.setText("Name must contain only letters, spaces, hyphens or apostrophes (max 30 chars)");
                nameErrorLabel.setVisible(true);
            } else {
                nameErrorLabel.setVisible(false);
            }
        });

        // Validate email field - proper email format, max 30 chars
        emailField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > 30) {
                emailErrorLabel.setText("Email cannot be longer than 30 characters");
                emailErrorLabel.setVisible(true);
            } else if (!EMAIL_PATTERN.matcher(newValue).matches()) {
                emailErrorLabel.setText("Please enter a valid email address");
                emailErrorLabel.setVisible(true);
            } else {
                emailErrorLabel.setVisible(false);
            }
        });

        // Validate phone field - 8 digits only
        phoneField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!PHONE_PATTERN.matcher(newValue).matches()) {
                phoneErrorLabel.setText("Phone number must be exactly 8 digits");
                phoneErrorLabel.setVisible(true);
            } else {
                phoneErrorLabel.setVisible(false);
            }
        });

        // Additional listener to enforce digits-only in phone field
        phoneField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                phoneField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
    }

    private void handleImageUpload() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File selectedFile = fileChooser.showOpenDialog(uploadImageButton.getScene().getWindow());
        if (selectedFile != null) {
            newImagePath = selectedFile.getAbsolutePath();
            userImageView.setImage(new Image(selectedFile.toURI().toString()));
            showSuccess("New image selected. Click Save Changes to update your profile.");
        }
    }

    private boolean validateAllInputs() {
        boolean isValid = true;

        // Reset all error messages
        nameErrorLabel.setVisible(false);
        emailErrorLabel.setVisible(false);
        passwordErrorLabel.setVisible(false);
        phoneErrorLabel.setVisible(false);

        // Validate name
        if (nameField.getText().isEmpty()) {
            nameErrorLabel.setText("Name is required");
            nameErrorLabel.setVisible(true);
            isValid = false;
        } else if (!NAME_PATTERN.matcher(nameField.getText()).matches()) {
            nameErrorLabel.setText("Name must contain only letters, spaces, hyphens or apostrophes (max 30 chars)");
            nameErrorLabel.setVisible(true);
            isValid = false;
        }

        // Validate email
        if (emailField.getText().isEmpty()) {
            emailErrorLabel.setText("Email is required");
            emailErrorLabel.setVisible(true);
            isValid = false;
        } else if (emailField.getText().length() > 30) {
            emailErrorLabel.setText("Email cannot be longer than 30 characters");
            emailErrorLabel.setVisible(true);
            isValid = false;
        } else if (!EMAIL_PATTERN.matcher(emailField.getText()).matches()) {
            emailErrorLabel.setText("Please enter a valid email address");
            emailErrorLabel.setVisible(true);
            isValid = false;
        }

        // Validate password if provided
        if (!passwordField.getText().isEmpty()) {
            if (passwordField.getText().equals(currentUser.getPassword())) {
                passwordErrorLabel.setText("New password must be different from current password");
                passwordErrorLabel.setVisible(true);
                isValid = false;
            }
        }

        // Validate phone number
        if (!PHONE_PATTERN.matcher(phoneField.getText()).matches()) {
            phoneErrorLabel.setText("Phone number must be exactly 8 digits");
            phoneErrorLabel.setVisible(true);
            isValid = false;
        }

        return isValid;
    }

    private void handleUpdateUser() {
        // Validate all input fields
        if (!validateAllInputs()) {
            showGlobalError("Please correct the errors before updating your profile");
            return;
        }

        try {
            Connection conn = dataSource.getInstance().getConnection();

            // Prepare SQL query - only update password if one is provided
            StringBuilder sqlBuilder = new StringBuilder("UPDATE user SET name = ?, email = ?, numTel = ?");

            if (newImagePath != null && !newImagePath.isEmpty()) {
                sqlBuilder.append(", image_url = ?");
            }

            if (!passwordField.getText().isEmpty()) {
                sqlBuilder.append(", password = ?");
            }

            sqlBuilder.append(" WHERE id = ?");

            PreparedStatement stmt = conn.prepareStatement(sqlBuilder.toString());

            // Set parameters
            stmt.setString(1, nameField.getText());
            stmt.setString(2, emailField.getText());
            stmt.setString(3, phoneField.getText());

            int paramIndex = 4;

            if (newImagePath != null && !newImagePath.isEmpty()) {
                stmt.setString(paramIndex++, newImagePath);
            }

            if (!passwordField.getText().isEmpty()) {
                stmt.setString(paramIndex++, passwordField.getText());
            }

            stmt.setInt(paramIndex, currentUser.getId());

            // Execute update
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                // Update user object with new values
                currentUser.setName(nameField.getText());
                currentUser.setEmail(emailField.getText());
                currentUser.setNumTel(phoneField.getText());

                if (newImagePath != null && !newImagePath.isEmpty()) {
                    currentUser.setImageUrl(newImagePath);
                }

                if (!passwordField.getText().isEmpty()) {
                    currentUser.setPassword(passwordField.getText());
                }

                // Update the session
                SessionManager.getInstance().setCurrentUser(currentUser);

                showSuccess("Profile updated successfully!");
            } else {
                showGlobalError("Failed to update profile");
            }

        } catch (SQLException e) {
            showGlobalError("Database error: " + e.getMessage());
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleDeleteAccount() {
        // Show confirmation dialog
        Alert confirmAlert = new Alert(AlertType.CONFIRMATION);
        confirmAlert.setTitle("Delete Account");
        confirmAlert.setHeaderText("Are you sure you want to delete your account?");
        confirmAlert.setContentText("This action cannot be undone.");

        // Customize the buttons
        ((Button) confirmAlert.getDialogPane().lookupButton(ButtonType.OK)).setText("Delete");
        ((Button) confirmAlert.getDialogPane().lookupButton(ButtonType.OK)).setStyle("-fx-background-color: #FF5C58; -fx-text-fill: white;");
        ((Button) confirmAlert.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Cancel");

        Optional<ButtonType> result = confirmAlert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                Connection conn = dataSource.getInstance().getConnection();
                String query = "DELETE FROM user WHERE id = ?";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setInt(1, currentUser.getId());

                int rowsAffected = stmt.executeUpdate();

                if (rowsAffected > 0) {
                    // Clear session
                    SessionManager.getInstance().clearSession();

                    // Show success message
                    Alert successAlert = new Alert(AlertType.INFORMATION);
                    successAlert.setTitle("Account Deleted");
                    successAlert.setHeaderText("Your account has been deleted successfully");
                    successAlert.setContentText("You will now be redirected to the login page.");
                    successAlert.showAndWait();

                    // Redirect to login page
                    loadLoginPage();
                } else {
                    showGlobalError("Failed to delete account");
                }

            } catch (SQLException e) {
                showGlobalError("Database error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void handleBackButton() {
        try {
            // Determine which page to load based on user role
            String fxmlPath = "/Menu/front_menu.fxml";
            if (currentUser.getRoles() != null && currentUser.getRoles().toLowerCase().contains("admin")) {
                fxmlPath = "/Menu/menu.fxml";
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            showGlobalError("Error loading page: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadLoginPage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) deleteButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Login");
            stage.show();

        } catch (Exception e) {
            System.err.println("Error loading login page: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showGlobalError(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: #FF5C58;");
    }

    private void showSuccess(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: #4CAF50;");
    }


}