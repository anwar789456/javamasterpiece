package org.example.controller.projet;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import utils.dataSource;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AfficherPostulerController {
    @FXML
    private TableView<PostulerTableItem> tablePostuler;
    @FXML
    private TableColumn<PostulerTableItem, Integer> colProjectId;
    @FXML
    private TableColumn<PostulerTableItem, String> colFirstName;
    @FXML
    private TableColumn<PostulerTableItem, String> colEmail;
    @FXML
    private TableColumn<PostulerTableItem, String> colJoiningReason;
    @FXML
    private TableColumn<PostulerTableItem, String> colNumTel;
    @FXML
    private Button btnAdd;
    @FXML
    private Button btnDelete;
    @FXML
    private Button btnUpdate;
    @FXML
    private TextField tfSearch;
    @FXML
    private Button btnSearch;

    private ObservableList<PostulerTableItem> postulerList = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        // Set up table columns
        colProjectId.setCellValueFactory(new PropertyValueFactory<>("projectId"));
        colFirstName.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colJoiningReason.setCellValueFactory(new PropertyValueFactory<>("joiningReason"));
        colNumTel.setCellValueFactory(new PropertyValueFactory<>("numTel"));

        // Set button actions
        btnAdd.setOnAction(event -> openAddApplicationForm());
        btnDelete.setOnAction(event -> {
            try {
                deleteSelectedApplication();
            } catch (Exception e) {
                showErrorAlert("Failed to delete application: " + e.getMessage());
            }
        });
        btnUpdate.setOnAction(event -> {
            try {
                updateSelectedApplication();
            } catch (Exception e) {
                showErrorAlert("Failed to update application: " + e.getMessage());
            }
        });
        btnSearch.setOnAction(event -> {
            try {
                searchApplications();
            } catch (Exception e) {
                showErrorAlert("Search failed: " + e.getMessage());
            }
        });

        // Add search field validation
        tfSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.length() > 50) {
                tfSearch.setText(oldVal);
            }
        });

        // Load all applications initially
        loadApplications();
    }

    private void loadApplications() {
        postulerList.clear();
        try {
            Connection conn = dataSource.getInstance().getConnection();
            String query = "SELECT * FROM postulerr";
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                PostulerTableItem postuler = new PostulerTableItem(
                    rs.getInt("id_projet"),
                    rs.getString("first_name"),
                    rs.getString("email"),
                    rs.getString("joining_reason"),
                    rs.getString("num_tel")
                );
                postulerList.add(postuler);
            }

            tablePostuler.setItems(postulerList);
        } catch (SQLException e) {
            showErrorAlert("Database error: " + e.getMessage());
        }
    }

    private void deleteSelectedApplication() {
        PostulerTableItem selected = tablePostuler.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showErrorAlert("Please select an application to delete");
            return;
        }

        // Show confirmation dialog with more details
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirm Delete");
        confirmDialog.setHeaderText("Delete Application");
        confirmDialog.setContentText("Are you sure you want to delete this application?\n\n" +
                                   "Project ID: " + selected.getProjectId() + "\n" +
                                   "Name: " + selected.getFirstName() + "\n" +
                                   "Email: " + selected.getEmail());
        confirmDialog.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/images/warning.png"))));
        
        if (confirmDialog.showAndWait().get() == ButtonType.OK) {
            try {
                Connection conn = dataSource.getInstance().getConnection();
                
                // First check if the application still exists
                String checkQuery = "SELECT COUNT(*) FROM postulerr WHERE id_projet = ? AND email = ?";
                PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
                checkStmt.setInt(1, selected.getProjectId());
                checkStmt.setString(2, selected.getEmail());
                ResultSet rs = checkStmt.executeQuery();
                rs.next();
                if (rs.getInt(1) == 0) {
                    showErrorAlert("This application no longer exists in the database");
                    loadApplications(); // Refresh the table
                    return;
                }

                String deleteQuery = "DELETE FROM postulerr WHERE id_projet = ? AND email = ?";
                PreparedStatement pstmt = conn.prepareStatement(deleteQuery);
                pstmt.setInt(1, selected.getProjectId());
                pstmt.setString(2, selected.getEmail());

                int rowsDeleted = pstmt.executeUpdate();
                if (rowsDeleted > 0) {
                    showSuccessAlert("Application deleted successfully");
                    loadApplications();
                } else {
                    showErrorAlert("Failed to delete application. Please try again.");
                }
            } catch (SQLException e) {
                showErrorAlert("Database error: " + e.getMessage());
            }
        }
    }

    private void updateSelectedApplication() {
        PostulerTableItem selected = tablePostuler.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showErrorAlert("Please select an application to update");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/projet/ModifierPostuler.fxml"));
            Parent root = loader.load();

            ModifierPostulerController controller = loader.getController();
            controller.setApplicationData(selected);

            Stage stage = new Stage();
            stage.setTitle("Update Application");
            stage.setScene(new Scene(root));
            stage.show();

            // Refresh the table when the update window is closed
            stage.setOnHidden(event -> loadApplications());

        } catch (IOException e) {
            showErrorAlert("Error loading update form: " + e.getMessage());
        }
    }

    private void searchApplications() {
        String searchText = tfSearch.getText().trim();
        if (searchText.isEmpty()) {
            loadApplications();
            return;
        }

        postulerList.clear();
        try {
            Connection conn = dataSource.getInstance().getConnection();
            String query = "SELECT * FROM postulerr WHERE first_name LIKE ? OR email LIKE ? OR joining_reason LIKE ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            String searchPattern = "%" + searchText + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setString(3, searchPattern);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                PostulerTableItem postuler = new PostulerTableItem(
                    rs.getInt("id_projet"),
                    rs.getString("first_name"),
                    rs.getString("email"),
                    rs.getString("joining_reason"),
                    rs.getString("num_tel")
                );
                postulerList.add(postuler);
            }

            if (postulerList.isEmpty()) {
                showInfoAlert("No applications found matching your search criteria");
            }

            tablePostuler.setItems(postulerList);
        } catch (SQLException e) {
            showErrorAlert("Database error: " + e.getMessage());
        }
    }

    private void openAddApplicationForm() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/projet/Postuler.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Add Application");
            stage.setScene(new Scene(root));
            stage.show();

            // Refresh the table when the add window is closed
            stage.setOnHidden(event -> loadApplications());

        } catch (IOException e) {
            showErrorAlert("Error loading add form: " + e.getMessage());
        }
    }

    private void showErrorAlert(String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Operation Failed");
        alert.setContentText(content);
        alert.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/images/error.png"))));
        alert.showAndWait();
    }

    private void showSuccessAlert(String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText("Operation Successful");
        alert.setContentText(content);
        alert.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/images/success.png"))));
        alert.showAndWait();
    }

    private void showInfoAlert(String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText("Search Results");
        alert.setContentText(content);
        alert.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/images/info.png"))));
        alert.showAndWait();
    }

    public static class PostulerTableItem {
        private int projectId;
        private String firstName;
        private String email;
        private String joiningReason;
        private String numTel;

        public PostulerTableItem(int projectId, String firstName, String email, String joiningReason, String numTel) {
            this.projectId = projectId;
            this.firstName = firstName;
            this.email = email;
            this.joiningReason = joiningReason;
            this.numTel = numTel;
        }

        // Getters
        public int getProjectId() { return projectId; }
        public String getFirstName() { return firstName; }
        public String getEmail() { return email; }
        public String getJoiningReason() { return joiningReason; }
        public String getNumTel() { return numTel; }
    }
} 