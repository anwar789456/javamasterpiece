package org.example.controller.user;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.example.controller.login.SessionManager;
import org.example.models.TeacherApplication;
import org.example.models.user.User;
import org.example.services.TeacherApplicationService;
import org.example.services.TeacherService;

import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class TeacherApplicationController implements Initializable {

    @FXML
    private TextArea bioTextArea;

    @FXML
    private TextField skillTextField;

    @FXML
    private Button addSkillButton;

    @FXML
    private FlowPane skillsFlowPane;

    @FXML
    private TextArea experienceTextArea;

    @FXML
    private TextField portfolioUrlTextField;

    @FXML
    private TextArea motivationTextArea;

    @FXML
    private Button submitButton;

    @FXML
    private Button cancelButton;

    private TeacherApplicationService applicationService;
    private TeacherService teacherService;
    private User currentUser;
    private List<String> skills = new ArrayList<>();
    private UserFrontController parentController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize services
        applicationService = new TeacherApplicationService();
        teacherService = new TeacherService();

        // Get the current user from session
        currentUser = SessionManager.getInstance().getCurrentUser();

        // Check if user has already applied
        if (currentUser != null) {
            try {
                if (applicationService.hasApplied(currentUser.getId())) {
                    showInfo("You have already submitted a teacher application.");
                    disableForm();
                }
            } catch (SQLException e) {
                showError("Error checking application status: " + e.getMessage());
            }
        }
    }

    /**
     * Set the parent controller for navigation
     */
    public void setParentController(UserFrontController controller) {
        this.parentController = controller;
    }

    /**
     * Handle adding a skill
     */
    @FXML
    private void handleAddSkill() {
        String skill = skillTextField.getText().trim();
        if (skill.isEmpty()) {
            showError("Please enter a skill");
            return;
        }

        // Add skill to the list
        skills.add(skill);

        // Create a skill tag
        HBox skillTag = createSkillTag(skill);
        skillsFlowPane.getChildren().add(skillTag);

        // Clear the text field
        skillTextField.clear();
        skillTextField.requestFocus();
    }

    /**
     * Create a skill tag with remove button
     */
    private HBox createSkillTag(String skill) {
        HBox tagBox = new HBox();
        tagBox.setStyle("-fx-background-color: #e0e6ed; -fx-background-radius: 15; -fx-padding: 5 10 5 10; -fx-spacing: 5;");
        tagBox.setPadding(new Insets(5, 10, 5, 10));

        Label tagLabel = new Label(skill);
        tagLabel.setStyle("-fx-text-fill: #333333;");

        Button removeButton = new Button("×");
        removeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #666666; -fx-padding: 0; -fx-cursor: hand;");
        removeButton.setOnAction(e -> {
            skills.remove(skill);
            skillsFlowPane.getChildren().remove(tagBox);
        });

        tagBox.getChildren().addAll(tagLabel, removeButton);
        return tagBox;
    }

    /**
     * Handle form submission
     */
    @FXML
    private void handleSubmit() {
        // Validate form
        if (!validateForm()) {
            return;
        }

        // Create application object
        TeacherApplication application = new TeacherApplication();
        application.setUserId(currentUser.getId());
        application.setBio(bioTextArea.getText().trim());
        application.setSkills(String.join(",", skills));
        application.setExperience(experienceTextArea.getText().trim());
        application.setPortfolioUrl(portfolioUrlTextField.getText().trim());
        application.setMotivation(motivationTextArea.getText().trim());

        try {
            // Submit application
            if (applicationService.submitApplication(application)) {
                // Register as teacher
                teacherService.addTeacher(currentUser.getId());
                
                showSuccess("Your application has been submitted successfully!");
                
                // Return to the profile page with a slight delay to ensure success message is seen
                javafx.application.Platform.runLater(() -> {
                    try {
                        // Small delay to ensure user sees the success message
                        Thread.sleep(800);
                        
                        // Close and return to profile
                        closeWindow();
                    } catch (InterruptedException e) {
                        // Just proceed with closing if interrupted
                        closeWindow();
                    }
                });
            } else {
                showError("You have already submitted an application.");
            }
        } catch (SQLException e) {
            showError("Error submitting application: " + e.getMessage());
        }
    }

    /**
     * Handle cancel button
     */
    @FXML
    private void handleCancel() {
        closeWindow();
    }

    /**
     * Validate the form
     */
    private boolean validateForm() {
        if (bioTextArea.getText().trim().isEmpty()) {
            showError("Please enter your bio");
            bioTextArea.requestFocus();
            return false;
        }

        if (skills.isEmpty()) {
            showError("Please add at least one skill");
            skillTextField.requestFocus();
            return false;
        }

        if (experienceTextArea.getText().trim().isEmpty()) {
            showError("Please enter your experience");
            experienceTextArea.requestFocus();
            return false;
        }

        if (portfolioUrlTextField.getText().trim().isEmpty()) {
            showError("Please enter your portfolio URL");
            portfolioUrlTextField.requestFocus();
            return false;
        }

        if (motivationTextArea.getText().trim().isEmpty()) {
            showError("Please enter your motivation");
            motivationTextArea.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * Close the form window
     */
    private void closeWindow() {
        if (parentController != null) {
            parentController.showMainContent();
        } else {
            // Close the window if not embedded in parent
            Stage stage = (Stage) cancelButton.getScene().getWindow();
            stage.close();
        }
    }

    /**
     * Disable the form
     */
    private void disableForm() {
        bioTextArea.setDisable(true);
        skillTextField.setDisable(true);
        addSkillButton.setDisable(true);
        experienceTextArea.setDisable(true);
        portfolioUrlTextField.setDisable(true);
        motivationTextArea.setDisable(true);
        submitButton.setDisable(true);
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

    /**
     * Show a success message dialog
     */
    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
} 