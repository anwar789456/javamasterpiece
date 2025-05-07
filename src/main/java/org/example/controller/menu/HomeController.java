package org.example.controller.menu;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.controller.login.SessionManager;
import org.example.controller.user.TeacherCoursesController;
import org.example.models.user.User;
import org.example.services.TeacherService;

import java.io.IOException;
import java.sql.SQLException;

public class HomeController {
    
    @FXML
    private Button applyTeacherButton;
    
    @FXML
    private Button manageCoursesButton;
    
    @FXML
    private VBox teacherActionsContainer;
    
    @FXML
    private VBox teacherApplicationContainer;
    
    @FXML
    private VBox teacherButtonsContainer;
    
    private TeacherService teacherService;
    private User currentUser;
    private int currentUserId = -1;
    
    @FXML
    private void initialize() {
        teacherService = new TeacherService();
        
        // Get the current user from the session manager
        currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getId();
            checkTeacherStatus();
        }
        
        // Set up Apply as Teacher button action
        applyTeacherButton.setOnAction(event -> applyAsTeacher());
        
        // Set up Manage Courses button action
        manageCoursesButton.setOnAction(event -> openManageCourses());
    }
    
    /**
     * Set the current user ID from an external source
     * 
     * @param userId User ID to set
     */
    public void setCurrentUserId(int userId) {
        this.currentUserId = userId;
        checkTeacherStatus();
    }
    
    /**
     * Check if the current user is a teacher and update UI accordingly
     */
    private void checkTeacherStatus() {
        if (currentUserId <= 0) {
            // Hide the entire teacher buttons section if no valid user
            teacherButtonsContainer.setVisible(false);
            return;
        }
        
        // Show the container since we have a valid user
        teacherButtonsContainer.setVisible(true);
        
        try {
            boolean isTeacher = teacherService.isTeacher(currentUserId);
            
            // Show/hide appropriate sections based on teacher status
            teacherActionsContainer.setVisible(isTeacher);
            teacherApplicationContainer.setVisible(!isTeacher);
            
            if (isTeacher) {
                applyTeacherButton.setText("Already a Teacher");
                applyTeacherButton.setDisable(true);
            } else {
                applyTeacherButton.setText("Apply as Teacher");
                applyTeacherButton.setDisable(false);
            }
        } catch (SQLException e) {
            showError("Error checking teacher status: " + e.getMessage());
        }
    }
    
    /**
     * Apply as a teacher
     */
    private void applyAsTeacher() {
        try {
            if (teacherService.addTeacher(currentUserId)) {
                showInfo("Congratulations! You are now registered as a teacher.");
                
                // Update UI to show teacher actions
                teacherActionsContainer.setVisible(true);
                teacherApplicationContainer.setVisible(false);
                
                applyTeacherButton.setText("Already a Teacher");
                applyTeacherButton.setDisable(true);
            } else {
                showInfo("You are already registered as a teacher.");
            }
        } catch (SQLException e) {
            showError("Error applying as teacher: " + e.getMessage());
        }
    }
    
    /**
     * Open the Manage Courses screen while preserving the navigation bar
     */
    private void openManageCourses() {
        try {
            // Get the parent FrontMenu controller
            Parent currentParent = applyTeacherButton.getScene().getRoot();
            FrontMenu frontMenu = null;
            
            // Find the FrontMenu controller
            if (currentParent.getProperties().containsKey("controller")) {
                Object controller = currentParent.getProperties().get("controller");
                if (controller instanceof FrontMenu) {
                    frontMenu = (FrontMenu) controller;
                }
            }
            
            if (frontMenu == null) {
                // If we can't find the controller, we'll reload the FrontMenu
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Menu/front_menu.fxml"));
                Parent root = loader.load();
                frontMenu = loader.getController();
                frontMenu.setCurrentUserId(currentUserId);
                
                // Replace the entire scene
                Scene scene = new Scene(root);
                Stage stage = (Stage) applyTeacherButton.getScene().getWindow();
                stage.setScene(scene);
            }
            
            // Load the teacher courses content
            FXMLLoader contentLoader = new FXMLLoader(getClass().getResource("/Teacher/manage_courses.fxml"));
            Parent coursesContent = contentLoader.load();
            
            // Set the teacher ID in the controller
            TeacherCoursesController coursesController = contentLoader.getController();
            coursesController.setTeacherId(currentUserId);
            
            // Load the content into the FrontMenu
            frontMenu.loadCustomContent(coursesContent);
        } catch (IOException e) {
            showError("Error opening Manage Courses: " + e.getMessage());
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