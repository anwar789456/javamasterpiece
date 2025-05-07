package org.example.controller.projet;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import utils.dataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;

public class EditProjectController {

    @FXML
    private TextField projectNameField;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private ComboBox<String> statusComboBox;

    @FXML
    private Button saveButton;

    @FXML
    private Button cancelButton;

    private int projectId;
    private boolean updateSuccessful = false;

    @FXML
    public void initialize() {
        // Initialize status options
        statusComboBox.getItems().addAll("Active", "Completed", "On Hold", "Cancelled");
        
        // Set up button handlers
        saveButton.setOnAction(e -> handleSave());
        cancelButton.setOnAction(e -> handleCancel());
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
        loadProjectDetails();
    }

    public void setProjectData(AfficherProjet.ProjetTableItem project) {
        this.projectId = project.getId();
        projectNameField.setText(project.getProjectName());
        descriptionArea.setText(project.getDescription());
        startDatePicker.setValue(project.getStartDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate());
        endDatePicker.setValue(project.getEndDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate());
        statusComboBox.setValue(project.getStatus());
    }

    private void loadProjectDetails() {
        String sql = "SELECT project_name, description, start_date, end_date, status FROM projet WHERE id = ?";
        try (Connection conn = dataSource.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, projectId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                projectNameField.setText(rs.getString("project_name"));
                descriptionArea.setText(rs.getString("description"));
                startDatePicker.setValue(rs.getDate("start_date").toLocalDate());
                endDatePicker.setValue(rs.getDate("end_date").toLocalDate());
                statusComboBox.setValue(rs.getString("status"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not load project details: " + e.getMessage());
        }
    }

    @FXML
    private void handleSave() {
        if (!validateInput()) {
            return;
        }

        String sql = "UPDATE projet SET project_name = ?, description = ?, start_date = ?, end_date = ?, status = ? WHERE id = ?";
        try (Connection conn = dataSource.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, projectNameField.getText());
            stmt.setString(2, descriptionArea.getText());
            stmt.setDate(3, java.sql.Date.valueOf(startDatePicker.getValue()));
            stmt.setDate(4, java.sql.Date.valueOf(endDatePicker.getValue()));
            stmt.setString(5, statusComboBox.getValue());
            stmt.setInt(6, projectId);
            
            int result = stmt.executeUpdate();
            if (result > 0) {
                updateSuccessful = true;
                showAlert(Alert.AlertType.INFORMATION, "Success", "Project updated successfully!");
                closeWindow();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not update project: " + e.getMessage());
        }
    }

    private boolean validateInput() {
        if (projectNameField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Project name cannot be empty");
            return false;
        }

        if (descriptionArea.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Description cannot be empty");
            return false;
        }

        if (startDatePicker.getValue() == null) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Start date must be selected");
            return false;
        }

        if (endDatePicker.getValue() == null) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "End date must be selected");
            return false;
        }

        if (endDatePicker.getValue().isBefore(startDatePicker.getValue())) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "End date cannot be before start date");
            return false;
        }

        if (statusComboBox.getValue() == null) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Status must be selected");
            return false;
        }

        return true;
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public boolean isUpdateSuccessful() {
        return updateSuccessful;
    }
} 