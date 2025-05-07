package org.example.controller.projet;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import utils.dataSource;
import org.example.model.ApplicationTableItem;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.IOException;
import javafx.collections.ObservableList;

public class AjouterPostulerAdminController {
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
    private ComboBox<String> cbStatus;
    @FXML
    private Button btnSubmit;
    @FXML
    private Button btnCancel;
    @FXML
    private TableView<ApplicationTableItem> tableView;

    private boolean isEditMode = false;
    private ApplicationTableItem editingApplication;

    @FXML
    private void initialize() {
        // Set up status options
        cbStatus.setItems(FXCollections.observableArrayList("Pending", "Accepted", "Rejected"));
        cbStatus.setValue("Pending");

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

        tfEmail.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("[A-Za-z0-9+_.-@]*")) {
                tfEmail.setText(oldVal);
            }
        });
    }

    public void setEditMode(ApplicationTableItem application) {
        isEditMode = true;
        editingApplication = application;
        
        // Populate fields with existing data
        tfProjectId.setText(String.valueOf(application.getProjectId()));
        tfFirstName.setText(application.getFirstName());
        tfEmail.setText(application.getEmail());
        taJoiningReason.setText(application.getJoiningReason());
        tfNumTel.setText(application.getNumTel());
        cbStatus.setValue(application.getStatus());
        
        // Disable project ID and email fields in edit mode as they are the primary keys
        tfProjectId.setDisable(true);
        tfEmail.setDisable(true);
        
        // Update button text
        btnSubmit.setText("Update");
    }

    @FXML
    private void handleSubmit() {
        if (!validateInputs()) {
            return;
        }

        try (Connection conn = dataSource.getInstance().getConnection()) {
            if (!isEditMode) {
                // Check if project exists
                if (!checkProjectExists(conn, Integer.parseInt(tfProjectId.getText()))) {
                    showErrorAlert("Project with ID " + tfProjectId.getText() + " does not exist.");
                    return;
                }

                // Check if application already exists
                if (checkApplicationExists(conn)) {
                    showErrorAlert("An application with this email for this project already exists.");
                    return;
                }

                // Insert new application
                String query = "INSERT INTO postulerr (id_projet, first_name, email, joining_reason, num_tel, status) " +
                              "VALUES (?, ?, ?, ?, ?, ?)";
                
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setInt(1, Integer.parseInt(tfProjectId.getText().trim()));
                stmt.setString(2, tfFirstName.getText().trim());
                stmt.setString(3, tfEmail.getText().trim());
                stmt.setString(4, taJoiningReason.getText().trim());
                stmt.setString(5, tfNumTel.getText().trim());
                stmt.setString(6, cbStatus.getValue());

                int rowsInserted = stmt.executeUpdate();
                if (rowsInserted > 0) {
                    showSuccessAlert("Application added successfully!");
                    closeWindow();
                } else {
                    showErrorAlert("Failed to add application.");
                }
            } else {
                // Update existing application
                String query = "UPDATE postulerr SET first_name = ?, joining_reason = ?, num_tel = ?, status = ? " +
                             "WHERE id_projet = ? AND email = ?";
                
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setString(1, tfFirstName.getText().trim());
                stmt.setString(2, taJoiningReason.getText().trim());
                stmt.setString(3, tfNumTel.getText().trim());
                stmt.setString(4, cbStatus.getValue());
                stmt.setInt(5, Integer.parseInt(tfProjectId.getText().trim()));
                stmt.setString(6, tfEmail.getText().trim());

                int rowsUpdated = stmt.executeUpdate();
                if (rowsUpdated > 0) {
                    showSuccessAlert("Application updated successfully!");
                    closeWindow();
                } else {
                    showErrorAlert("Failed to update application.");
                }
            }
        } catch (SQLException e) {
            showErrorAlert("Database error: " + e.getMessage());
        }
    }

    private boolean checkProjectExists(Connection conn, int projectId) throws SQLException {
        String query = "SELECT COUNT(*) FROM projet WHERE id = ?";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setInt(1, projectId);
        ResultSet rs = stmt.executeQuery();
        return rs.next() && rs.getInt(1) > 0;
    }

    private boolean checkApplicationExists(Connection conn) throws SQLException {
        String query = "SELECT COUNT(*) FROM postulerr WHERE id_projet = ? AND email = ?";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setInt(1, Integer.parseInt(tfProjectId.getText().trim()));
        stmt.setString(2, tfEmail.getText().trim());
        ResultSet rs = stmt.executeQuery();
        return rs.next() && rs.getInt(1) > 0;
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

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }

    private void showSuccessAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        try {
            InputStream imageStream = getClass().getResourceAsStream("/images/success.png");
            if (imageStream != null) {
                Image image = new Image(imageStream);
                if (!image.isError()) {
                    ImageView imageView = new ImageView(image);
                    imageView.setFitWidth(48);
                    imageView.setFitHeight(48);
                    alert.setGraphic(imageView);
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not load success image: " + e.getMessage());
        }
        alert.showAndWait();
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        try {
            InputStream imageStream = getClass().getResourceAsStream("/images/error.png");
            if (imageStream != null) {
                Image image = new Image(imageStream);
                if (!image.isError()) {
                    ImageView imageView = new ImageView(image);
                    imageView.setFitWidth(48);
                    imageView.setFitHeight(48);
                    alert.setGraphic(imageView);
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not load error image: " + e.getMessage());
        }
        alert.showAndWait();
    }

    @FXML
    private void handleAcceptSelected() {
        ObservableList<ApplicationTableItem> selectedItems = tableView.getSelectionModel().getSelectedItems();
        if (selectedItems.isEmpty()) {
            showErrorAlert("Please select at least one application to accept.");
            return;
        }

        try (Connection conn = dataSource.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE postulerr SET status = 'Accepted' WHERE id_projet = ? AND email = ?")) {
            
            int successCount = 0;
            for (ApplicationTableItem item : selectedItems) {
                stmt.setInt(1, item.getProjectId());
                stmt.setString(2, item.getEmail());
                successCount += stmt.executeUpdate();
                // Send status update email
                sendStatusUpdateEmail(item.getEmail(), item.getFirstName(), getProjectName(item.getProjectId()), "Accepted");
            }
            
            if (successCount > 0) {
                showSuccessAlert(successCount + " application(s) accepted successfully");
                refreshTable();
            } else {
                showErrorAlert("Failed to accept selected applications");
            }
        } catch (SQLException e) {
            showErrorAlert("Error accepting applications: " + e.getMessage());
        }
    }

    @FXML
    private void handleRejectSelected() {
        ObservableList<ApplicationTableItem> selectedItems = tableView.getSelectionModel().getSelectedItems();
        if (selectedItems.isEmpty()) {
            showErrorAlert("Please select at least one application to reject.");
            return;
        }

        try (Connection conn = dataSource.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE postulerr SET status = 'Rejected' WHERE id_projet = ? AND email = ?")) {
            
            int successCount = 0;
            for (ApplicationTableItem item : selectedItems) {
                stmt.setInt(1, item.getProjectId());
                stmt.setString(2, item.getEmail());
                successCount += stmt.executeUpdate();
                // Send status update email
                sendStatusUpdateEmail(item.getEmail(), item.getFirstName(), getProjectName(item.getProjectId()), "Rejected");
            }
            
            if (successCount > 0) {
                showSuccessAlert(successCount + " application(s) rejected successfully");
                refreshTable();
            } else {
                showErrorAlert("Failed to reject selected applications");
            }
        } catch (SQLException e) {
            showErrorAlert("Error rejecting applications: " + e.getMessage());
        }
    }

    private void sendStatusUpdateEmail(String recipientEmail, String firstName, String projectName, String newStatus) {
        System.out.println("[DEBUG] sendStatusUpdateEmail called with: " + recipientEmail + ", " + firstName + ", " + projectName + ", " + newStatus);
        try {
            String pythonPath = "C:\\Users\\USER\\AppData\\Local\\Programs\\Python\\Python313\\python.exe";
            String currentDir = System.getProperty("user.dir");
            String scriptPath = currentDir + "\\src\\main\\python\\email_sender.py";

            System.out.println("[DEBUG] Running: " + pythonPath + " " + scriptPath + " status_update " + recipientEmail + " " + firstName + " " + projectName + " " + newStatus);

            ProcessBuilder processBuilder = new ProcessBuilder(
                pythonPath,
                scriptPath,
                "status_update",
                recipientEmail,
                firstName,
                projectName,
                newStatus
            );
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                StringBuilder output = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                int exitCode = process.waitFor();
                System.out.println("[DEBUG] Email sender process exited with code: " + exitCode);
                if (exitCode != 0) {
                    System.err.println("Status update email sender failed with output:");
                    System.err.println(output.toString());
                } else {
                    System.out.println("Status update email sender output:");
                    System.out.println(output.toString());
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            System.err.println("Failed to execute status update email sender: " + e.getMessage());
        }
    }

    private String getProjectName(int projectId) {
        try (Connection conn = dataSource.getInstance().getConnection()) {
            String query = "SELECT name FROM projet WHERE id = ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, projectId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Unknown Project";
    }

    private void refreshTable() {
        loadApplications();
    }

    private void loadApplications() {
        tableView.getItems().clear();
        try (Connection conn = dataSource.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT id_projet, first_name, email, joining_reason, num_tel, COALESCE(status, 'Pending') as status FROM postulerr ORDER BY id_projet DESC")) {

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                tableView.getItems().add(new ApplicationTableItem(
                        rs.getInt("id_projet"),
                        rs.getString("first_name"),
                        rs.getString("email"),
                        rs.getString("joining_reason"),
                        rs.getString("num_tel"),
                        rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            showErrorAlert("Error loading applications: " + e.getMessage());
        }
    }
} 