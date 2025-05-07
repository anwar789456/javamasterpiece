package org.example.controller.projet;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import utils.dataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ModifierPostulerController {

    @FXML
    private TextField tfProjectId;
    @FXML
    private TextField tfFirstName;
    @FXML
    private TextField tfEmail;
    @FXML
    private TextArea taJoiningReason;
    @FXML
    private TextField tfNumTel;
    @FXML
    private Button btnUpdate;
    @FXML
    private Button btnCancel;

    private String originalEmail; // Store original email for update query

    @FXML
    private void initialize() {
        // Set button actions
        btnUpdate.setOnAction(event -> updateApplication());
        btnCancel.setOnAction(event -> closeWindow());

        // Add input validation listeners
        tfProjectId.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                tfProjectId.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });

        tfFirstName.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("[a-zA-Z\\s]*")) {
                tfFirstName.setText(newVal.replaceAll("[^a-zA-Z\\s]", ""));
            }
        });

        tfNumTel.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("[0-9+\\s-]*")) {
                tfNumTel.setText(newVal.replaceAll("[^0-9+\\s-]", ""));
            }
        });

        // Add email validation listener
        tfEmail.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("[A-Za-z0-9+_.-@]*")) {
                tfEmail.setText(oldVal);
            }
        });
    }

    public void setApplicationData(AfficherPostulerController.PostulerTableItem application) {
        tfProjectId.setText(String.valueOf(application.getProjectId()));
        tfFirstName.setText(application.getFirstName());
        tfEmail.setText(application.getEmail());
        taJoiningReason.setText(application.getJoiningReason());
        tfNumTel.setText(application.getNumTel());
        originalEmail = application.getEmail(); // Store original email
    }

    private void updateApplication() {
        // Validate inputs
        if (!validateInputs()) {
            return;
        }

        try {
            Connection conn = dataSource.getInstance().getConnection();
            String query = "UPDATE postulerr SET first_name = ?, email = ?, " +
                          "joining_reason = ?, num_tel = ? WHERE id_projet = ? AND email = ?";
            
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, tfFirstName.getText().trim());
            stmt.setString(2, tfEmail.getText().trim());
            stmt.setString(3, taJoiningReason.getText().trim());
            stmt.setString(4, tfNumTel.getText().trim());
            stmt.setInt(5, Integer.parseInt(tfProjectId.getText().trim()));
            stmt.setString(6, originalEmail); // Use original email for WHERE clause

            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                showSuccessAlert("Application updated successfully!");
                closeWindow();
            } else {
                showErrorAlert("Failed to update application.");
            }

        } catch (SQLException e) {
            showErrorAlert("Database error: " + e.getMessage());
        }
    }

    private boolean validateInputs() {
        // Validate Project ID
        if (tfProjectId.getText().isEmpty()) {
            showErrorAlert("Project ID is required");
            return false;
        }
        try {
            int projectId = Integer.parseInt(tfProjectId.getText());
            if (projectId <= 0) {
                showErrorAlert("Project ID must be a positive number");
                return false;
            }
        } catch (NumberFormatException e) {
            showErrorAlert("Project ID must be a valid number");
            return false;
        }

        // Validate First Name
        if (tfFirstName.getText().isEmpty()) {
            showErrorAlert("First name is required");
            return false;
        }
        if (!tfFirstName.getText().matches("[a-zA-Z\\s]+")) {
            showErrorAlert("First name can only contain letters and spaces");
            return false;
        }
        if (tfFirstName.getText().length() < 2) {
            showErrorAlert("First name must be at least 2 characters long");
            return false;
        }

        // Validate Email
        if (tfEmail.getText().isEmpty()) {
            showErrorAlert("Email is required");
            return false;
        }
        if (!tfEmail.getText().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showErrorAlert("Please enter a valid email address");
            return false;
        }

        // Validate Joining Reason
        if (taJoiningReason.getText().isEmpty()) {
            showErrorAlert("Joining reason is required");
            return false;
        }
        if (taJoiningReason.getText().length() < 10) {
            showErrorAlert("Joining reason must be at least 10 characters long");
            return false;
        }

        // Validate Phone Number
        if (tfNumTel.getText().isEmpty()) {
            showErrorAlert("Phone number is required");
            return false;
        }
        if (!tfNumTel.getText().matches("^[0-9+\\s-]{8,15}$")) {
            showErrorAlert("Please enter a valid phone number (8-15 digits, may include +, -, or spaces)");
            return false;
        }

        return true;
    }

    private void showSuccessAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText("Operation Successful");
        alert.setContentText(message);
        alert.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/images/success.png"))));
        alert.showAndWait();
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Operation Failed");
        alert.setContentText(message);
        alert.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/images/error.png"))));
        alert.showAndWait();
    }

    private void closeWindow() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }
} 