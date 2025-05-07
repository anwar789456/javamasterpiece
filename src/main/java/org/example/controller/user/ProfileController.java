package org.example.controller.user;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.example.controller.login.SessionManager;
import org.example.models.user.User;
import org.example.services.ProfileUserService;

import java.sql.SQLException;

public class ProfileController {
    
    @FXML
    private Label nameLabel;
    
    @FXML
    private Label emailLabel;
    
    @FXML
    private Label cinLabel;
    
    @FXML
    private Label phoneLabel;
    
    @FXML
    private ImageView profileImage;
    
    private ProfileUserService userService;
    private User currentUser;
    private int currentUserId = -1;
    
    @FXML
    private void initialize() {
        userService = new ProfileUserService();
        
        // Get the current user from the session manager
        currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getId();
            loadUserProfile();
        }
    }
    
    /**
     * Set the current user ID from the menu controller
     */
    public void setCurrentUserId(int userId) {
        this.currentUserId = userId;
        
        // If the user ID is set after initialization, load the profile
        if (currentUserId > 0 && currentUser == null) {
            try {
                currentUser = userService.getUserById(currentUserId);
                loadUserProfile();
            } catch (SQLException e) {
                showError("Error loading user profile: " + e.getMessage());
            }
        }
    }
    
    /**
     * Load the user profile data into the view
     */
    private void loadUserProfile() {
        if (currentUser == null) return;
        
        // Set labels with user data
        nameLabel.setText(currentUser.getName());
        emailLabel.setText(currentUser.getEmail());
        cinLabel.setText(currentUser.getCin());
        phoneLabel.setText(currentUser.getNumTel());
        
        // Load profile image if available
        if (currentUser.getImageUrl() != null && !currentUser.getImageUrl().isEmpty()) {
            try {
                Image image = new Image(currentUser.getImageUrl());
                profileImage.setImage(image);
            } catch (Exception e) {
                System.err.println("Error loading profile image: " + e.getMessage());
            }
        }
    }
    
    /**
     * Show an error message dialog
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Show an information message dialog
     */
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
} 