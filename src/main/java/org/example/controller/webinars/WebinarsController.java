package org.example.controller.webinars;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import org.example.controller.login.SessionManager;
import org.example.models.user.User;

public class WebinarsController {
    @FXML
    private VBox webinarsContainer;
    
    @FXML
    private Button refreshButton;
    
    @FXML
    private Label noWebinarsLabel;
    
    @FXML
    private void initialize() {
        // Set up button actions
        refreshButton.setOnAction(event -> loadWebinars());
        
        // Initial load
        loadWebinars();
    }
    
    /**
     * Sets the current user ID from the session
     * This method is kept for compatibility with the FrontMenu controller
     * but now gets the user directly from the SessionManager
     * @param userId The user ID (not used anymore)
     */
    public void setCurrentUserId(int userId) {
        // Instead of storing the passed ID, we'll get the current user from the session
        // and reload webinars
        loadWebinars();
    }
    
    /**
     * Load webinars from the database or service
     */
    private void loadWebinars() {
        // Clear existing content
        webinarsContainer.getChildren().clear();
        
        try {
            // Get the current user from the session
            User currentUser = SessionManager.getInstance().getCurrentUser();
            
            if (currentUser == null) {
                showAlert(AlertType.WARNING, "Warning", "No user is currently logged in.");
                return;
            }
            
            // Log the current user email for debugging
            System.out.println("Initialized with user email: " + currentUser.getEmail());
            
            // TODO: Implement actual webinar loading from database
            // For now, just show the "no webinars" message
            noWebinarsLabel.setVisible(true);
            
            // Example of future implementation:
            // List<Webinar> webinars = webinarService.getWebinarsForUser(currentUser.getId());
            // if (webinars.isEmpty()) {
            //     noWebinarsLabel.setVisible(true);
            // } else {
            //     noWebinarsLabel.setVisible(false);
            //     for (Webinar webinar : webinars) {
            //         webinarsContainer.getChildren().add(createWebinarCard(webinar));
            //     }
            // }
            
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Error", "Failed to load webinars: " + e.getMessage());
        }
    }
    
    /**
     * Helper method to show alerts
     */
    private void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
} 