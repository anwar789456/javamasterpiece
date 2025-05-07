package org.example.controller.projet;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.model.ApplicationTableItem;
import utils.dataSource;

import java.sql.*;
import java.util.Optional;
import java.io.IOException;
import java.io.InputStream;

public class AfficherPostulerAdminController {

    @FXML
    private TableView<ApplicationTableItem> tableView;
    @FXML
    private TableColumn<ApplicationTableItem, Integer> projectIdColumn;
    @FXML
    private TableColumn<ApplicationTableItem, String> firstNameColumn;
    @FXML
    private TableColumn<ApplicationTableItem, String> emailColumn;
    @FXML
    private TableColumn<ApplicationTableItem, String> joiningReasonColumn;
    @FXML
    private TableColumn<ApplicationTableItem, String> numTelColumn;
    @FXML
    private TableColumn<ApplicationTableItem, String> statusColumn;
    @FXML
    private TableColumn<ApplicationTableItem, Void> actionsColumn;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> filterComboBox;

    private ObservableList<ApplicationTableItem> applications;
    private FilteredList<ApplicationTableItem> filteredApplications;

    @FXML
    private void initialize() {
        applications = FXCollections.observableArrayList();
        filteredApplications = new FilteredList<>(applications);

        // Enable multiple selection
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        setupTableColumns();
        setupFilterComboBox();
        setupSearch();
        loadApplications();
    }

    private void setupTableColumns() {
        projectIdColumn.setCellValueFactory(new PropertyValueFactory<>("projectId"));
        firstNameColumn.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        joiningReasonColumn.setCellValueFactory(new PropertyValueFactory<>("joiningReason"));
        numTelColumn.setCellValueFactory(new PropertyValueFactory<>("numTel"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        setupActionsColumn();
        
        tableView.setItems(filteredApplications);
    }

    private void setupFilterComboBox() {
        filterComboBox.setItems(FXCollections.observableArrayList("All", "Pending", "Accepted", "Rejected"));
        filterComboBox.setValue("All");
        filterComboBox.setOnAction(e -> applyFilters());
    }

    private void setupSearch() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
    }

    private void applyFilters() {
        String searchText = searchField.getText().toLowerCase();
        String statusFilter = filterComboBox.getValue();

        filteredApplications.setPredicate(application -> {
            boolean matchesSearch = searchText.isEmpty() ||
                    application.getFirstName().toLowerCase().contains(searchText) ||
                    application.getEmail().toLowerCase().contains(searchText) ||
                    String.valueOf(application.getProjectId()).contains(searchText);

            boolean matchesStatus = statusFilter.equals("All") ||
                    application.getStatus().equals(statusFilter);

            return matchesSearch && matchesStatus;
        });
    }

    private void setupActionsColumn() {
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button viewBtn = new Button("View");
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox buttons = new HBox(5, viewBtn, editBtn, deleteBtn);

            {
                viewBtn.setOnAction(event -> handleView(getTableRow().getItem()));
                editBtn.setOnAction(event -> handleEdit(getTableRow().getItem()));
                deleteBtn.setOnAction(event -> handleDelete(getTableRow().getItem()));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(buttons);
                }
            }
        });
    }

    @FXML
    private void refreshTable() {
        loadApplications();
    }

    private void loadApplications() {
        applications.clear();
        try (Connection conn = dataSource.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT id_projet, first_name, email, joining_reason, num_tel, " +
                     "COALESCE(status, 'Pending') as status FROM postulerr ORDER BY id_projet DESC")) {

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                applications.add(new ApplicationTableItem(
                        rs.getInt("id_projet"),
                        rs.getString("first_name"),
                        rs.getString("email"),
                        rs.getString("joining_reason"),
                        rs.getString("num_tel"),
                        rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            // Check if the error is about missing column
            if (e.getMessage().contains("status")) {
                // Try to add the status column
                try (Connection conn = dataSource.getInstance().getConnection();
                     Statement alterStmt = conn.createStatement()) {
                    alterStmt.execute("ALTER TABLE postulerr ADD COLUMN status VARCHAR(20) DEFAULT 'Pending'");
                    // Try loading again after adding the column
                    loadApplications();
                    return;
                } catch (SQLException e2) {
                    showErrorAlert("Error adding status column: " + e2.getMessage());
                }
            }
            showErrorAlert("Error loading applications: " + e.getMessage());
        }
    }

    private void handleView(ApplicationTableItem application) {
        if (application == null) return;
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Application Details");
        alert.setHeaderText("Application for Project ID: " + application.getProjectId());
        alert.setContentText(String.format("""
                First Name: %s
                Email: %s
                Phone: %s
                Status: %s
                
                Joining Reason:
                %s""",
                application.getFirstName(),
                application.getEmail(),
                application.getNumTel(),
                application.getStatus(),
                application.getJoiningReason()));
        alert.showAndWait();
    }

    @FXML
    private void handleAdd() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/projet/AjouterPostulerAdmin.fxml"));
            Parent root = loader.load();
            
            Stage stage = new Stage();
            stage.setTitle("Add Application");
            stage.setScene(new Scene(root));
            
            // Set the owner to ensure proper modal behavior
            stage.initOwner(tableView.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            
            stage.showAndWait();
            refreshTable(); // Refresh the table after adding
        } catch (IOException e) {
            showErrorAlert("Error opening add form: " + e.getMessage());
        }
    }

    private void handleEdit(ApplicationTableItem application) {
        if (application == null) return;
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/projet/AjouterPostulerAdmin.fxml"));
            Parent root = loader.load();
            
            AjouterPostulerAdminController controller = loader.getController();
            controller.setEditMode(application); // Set the form in edit mode with existing data
            
            Stage stage = new Stage();
            stage.setTitle("Edit Application");
            stage.setScene(new Scene(root));
            
            stage.initOwner(tableView.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            
            stage.showAndWait();
            refreshTable(); // Refresh the table after editing
        } catch (IOException e) {
            showErrorAlert("Error opening edit form: " + e.getMessage());
        }
    }

    private void handleDelete(ApplicationTableItem application) {
        if (application == null) return;

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Deletion");
        confirmation.setHeaderText("Delete Application");
        confirmation.setContentText("Are you sure you want to delete this application?");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try (Connection conn = dataSource.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "DELETE FROM postulerr WHERE id_projet = ? AND email = ?")) {
                
                stmt.setInt(1, application.getProjectId());
                stmt.setString(2, application.getEmail());
                
                int rowsDeleted = stmt.executeUpdate();
                if (rowsDeleted > 0) {
                    showSuccessAlert("Application deleted successfully");
                    refreshTable();
                } else {
                    showErrorAlert("Failed to delete application");
                }
            } catch (SQLException e) {
                showErrorAlert("Error deleting application: " + e.getMessage());
            }
        }
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

    @FXML
    private void handleDeleteSelected() {
        ObservableList<ApplicationTableItem> selectedItems = tableView.getSelectionModel().getSelectedItems();
        if (selectedItems.isEmpty()) {
            showErrorAlert("Please select at least one application to delete.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Deletion");
        confirmation.setHeaderText("Delete Selected Applications");
        confirmation.setContentText("Are you sure you want to delete " + selectedItems.size() + " selected application(s)?");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try (Connection conn = dataSource.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "DELETE FROM postulerr WHERE id_projet = ? AND email = ?")) {
                
                int successCount = 0;
                for (ApplicationTableItem item : selectedItems) {
                    stmt.setInt(1, item.getProjectId());
                    stmt.setString(2, item.getEmail());
                    successCount += stmt.executeUpdate();
                }
                
                if (successCount > 0) {
                    showSuccessAlert(successCount + " application(s) deleted successfully");
                    refreshTable();
                } else {
                    showErrorAlert("Failed to delete selected applications");
                }
            } catch (SQLException e) {
                showErrorAlert("Error deleting applications: " + e.getMessage());
            }
        }
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
} 