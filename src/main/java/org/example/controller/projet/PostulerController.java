package org.example.controller.projet;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import utils.dataSource;
import org.example.util.NavigationUtils.DataInitializable;
import javafx.event.ActionEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.IOException;

public class PostulerController implements DataInitializable {
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
    private Button btnApply;
    @FXML
    private Button btnCancel;

    @Override
    public void initializeWithData(Object data) {
        if (data instanceof Integer) {
            setProjectId((Integer) data);
        }
    }

    @FXML
    private void initialize() {
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

    @FXML
    private void handleSubmit(ActionEvent event) {
        handleSubmit();
    }

    private void handleSubmit() {
        // Validate Project ID
        if (tfProjectId.getText().isEmpty()) {
            showErrorAlert("Project ID is required");
            return;
        }
        try {
            int projectId = Integer.parseInt(tfProjectId.getText());
            if (projectId <= 0) {
                showErrorAlert("Project ID must be a positive number");
                return;
            }
            // Check if project exists
            if (!projectExists(projectId)) {
                showErrorAlert("Project ID does not exist in the database");
                return;
            }
            // Check if project is active
            if (!isProjectActive(projectId)) {
                showErrorAlert("This project is not active. You cannot apply for it.");
                return;
            }
        } catch (NumberFormatException e) {
            showErrorAlert("Project ID must be a valid number");
            return;
        }

        // Validate First Name
        if (tfFirstName.getText().isEmpty()) {
            showErrorAlert("First name is required");
            return;
        }
        if (!tfFirstName.getText().matches("[a-zA-Z\\s]+")) {
            showErrorAlert("First name can only contain letters and spaces");
            return;
        }
        if (tfFirstName.getText().length() < 2) {
            showErrorAlert("First name must be at least 2 characters long");
            return;
        }

        // Validate Email
        if (tfEmail.getText().isEmpty()) {
            showErrorAlert("Email is required");
            return;
        }
        if (!tfEmail.getText().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showErrorAlert("Please enter a valid email address");
            return;
        }
        // Check if email is already used for this specific project
        if (emailExistsForProject(tfEmail.getText(), Integer.parseInt(tfProjectId.getText()))) {
            showErrorAlert("You have already applied to this project. Please choose a different project.");
            return;
        }

        // Validate Joining Reason
        if (taJoiningReason.getText().isEmpty()) {
            showErrorAlert("Joining reason is required");
            return;
        }
        if (taJoiningReason.getText().length() < 10) {
            showErrorAlert("Joining reason must be at least 10 characters long");
            return;
        }

        // Validate Phone Number
        if (tfNumTel.getText().isEmpty()) {
            showErrorAlert("Phone number is required");
            return;
        }
        if (!tfNumTel.getText().matches("^[0-9+\\s-]{8,15}$")) {
            showErrorAlert("Please enter a valid phone number (8-15 digits, may include +, -, or spaces)");
            return;
        }

        try {
            Connection conn = dataSource.getInstance().getConnection();
            String query = "INSERT INTO postulerr (id_projet, first_name, email, joining_reason, num_tel) VALUES (?, ?, ?, ?, ?)";
            
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, Integer.parseInt(tfProjectId.getText()));
            pstmt.setString(2, tfFirstName.getText().trim());
            pstmt.setString(3, tfEmail.getText().trim());
            pstmt.setString(4, taJoiningReason.getText().trim());
            pstmt.setString(5, tfNumTel.getText().trim());

            int rowsInserted = pstmt.executeUpdate();
            if (rowsInserted > 0) {
                // Get project details for email
                String[] details = getProjectDetails(Integer.parseInt(tfProjectId.getText()));
                sendConfirmationEmail(tfEmail.getText(), tfFirstName.getText(), details[0], details[1], details[2], details[3]);
                showSuccessAlert("Application submitted successfully!");
                clearForm();
            } else {
                showErrorAlert("Failed to submit application");
            }
        } catch (SQLException e) {
            showErrorAlert("Database error: " + e.getMessage());
        }
    }

    private boolean projectExists(int projectId) {
        try {
            Connection conn = dataSource.getInstance().getConnection();
            String query = "SELECT COUNT(*) FROM projet WHERE id = ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, projectId);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    private boolean isProjectActive(int projectId) {
        try {
            Connection conn = dataSource.getInstance().getConnection();
            String query = "SELECT status FROM projet WHERE id = ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, projectId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String status = rs.getString("status");
                return "Active".equals(status);
            }
            return false;
        } catch (SQLException e) {
            return false;
        }
    }

    private boolean emailExistsForProject(String email, int projectId) {
        try {
            Connection conn = dataSource.getInstance().getConnection();
            String query = "SELECT COUNT(*) FROM postulerr WHERE email = ? AND id_projet = ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, email);
            pstmt.setInt(2, projectId);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    private void clearForm() {
        tfProjectId.clear();
        tfFirstName.clear();
        tfEmail.clear();
        taJoiningReason.clear();
        tfNumTel.clear();
    }

    private void showErrorAlert(String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Validation Error");
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showSuccessAlert(String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText("Operation Successful");
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    private void closeWindow() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }
    
    @FXML
    private void closeWindow(ActionEvent event) {
        closeWindow();
    }
    
    public void setProjectId(int projectId) {
        tfProjectId.setText(String.valueOf(projectId));
        tfProjectId.setEditable(false);
    }

    @FXML
    public void handleApply() {
        handleSubmit();
    }

    @FXML
    public void handleCancel() {
        closeWindow();
    }

    private String[] getProjectDetails(int projectId) {
        try (Connection conn = dataSource.getInstance().getConnection()) {
            String query = "SELECT project_name, description, start_date, end_date FROM projet WHERE id = ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, projectId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String name = rs.getString("project_name");
                String description = rs.getString("description");
                String startDate = rs.getString("start_date");
                String endDate = rs.getString("end_date");
                return new String[]{name, description, startDate, endDate};
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new String[]{"Unknown Project", "No description", "N/A", "N/A"};
    }

    private String getProjectName(int projectId) {
        try (Connection conn = dataSource.getInstance().getConnection()) {
            String query = "SELECT project_name FROM projet WHERE id = ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, projectId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("project_name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Unknown Project";
    }

    private void sendConfirmationEmail(String recipientEmail, String firstName, String projectName, String projectDesc, String startDate, String endDate) {
        try {
            String pythonPath = "C:\\Users\\USER\\AppData\\Local\\Programs\\Python\\Python313\\python.exe";
            String currentDir = System.getProperty("user.dir");
            String scriptPath = currentDir + "\\src\\main\\python\\email_sender.py";

            ProcessBuilder processBuilder = new ProcessBuilder(
                pythonPath,
                scriptPath,
                "application_confirmation",
                recipientEmail,
                firstName,
                projectName,
                projectDesc,
                startDate,
                endDate
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
                if (exitCode != 0) {
                    System.err.println("Email sender failed with output:");
                    System.err.println(output.toString());
                } else {
                    System.out.println("Email sender output:");
                    System.out.println(output.toString());
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            System.err.println("Failed to execute email sender: " + e.getMessage());
        }
    }
} 