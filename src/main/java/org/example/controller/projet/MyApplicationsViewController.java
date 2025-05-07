package org.example.controller.projet;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import utils.dataSource;
import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.control.ButtonType;

public class MyApplicationsViewController {
    @FXML private TableView<ApplicationItem> applicationsTable;
    @FXML private TableColumn<ApplicationItem, String> colProjectName;
    @FXML private TableColumn<ApplicationItem, String> colStatus;
    @FXML private TableColumn<ApplicationItem, String> colCancel;
    @FXML private Button closeButton;

    private String userEmail;
    private ObservableList<ApplicationItem> applications = FXCollections.observableArrayList();

    public void setUserEmail(String email) {
        this.userEmail = email;
        System.out.println("Loading applications for email: " + userEmail);
        loadApplications();
    }

    @FXML
    private void initialize() {
        System.out.println("Initializing MyApplicationsViewController");
        colProjectName.setCellValueFactory(new PropertyValueFactory<>("projectName"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        applicationsTable.setItems(applications);

        // Status badge styling
        colStatus.setCellFactory(column -> new TableCell<ApplicationItem, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Text badge = new Text(status);
                    badge.getStyleClass().add("status-badge");
                    switch (status.toLowerCase()) {
                        case "active": badge.setFill(Color.web("#43e97b")); break;
                        case "completed": badge.setFill(Color.web("#2196F3")); break;
                        case "on hold": badge.setFill(Color.web("#FFC107")); break;
                        case "cancelled": badge.setFill(Color.web("#F44336")); break;
                        default: badge.setFill(Color.web("#888888")); break;
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        // Cancel button column
        colCancel.setCellFactory(col -> new TableCell<ApplicationItem, String>() {
            private final Button btn = new Button("Cancel");
            {
                btn.getStyleClass().add("cancel-btn");
                btn.setOnAction(e -> {
                    ApplicationItem item = getTableView().getItems().get(getIndex());
                    if (item != null) {
                        boolean confirmed = confirmCancel(item);
                        if (confirmed) {
                            cancelApplication(item);
                        }
                    }
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btn);
                }
            }
        });
    }

    private void loadApplications() {
        applications.clear();
        try {
            Connection conn = dataSource.getInstance().getConnection();
            String query;
            PreparedStatement stmt;
            
            System.out.println("Current user email: " + userEmail);
            
            if (userEmail == null || userEmail.isEmpty()) {
                System.out.println("Warning: No user email set, loading all applications for troubleshooting.");
                query = "SELECT p.project_name, a.status, a.email FROM postulerr a JOIN projet p ON a.id_projet = p.id ORDER BY a.id_projet DESC";
                stmt = conn.prepareStatement(query);
            } else {
                query = "SELECT p.project_name, a.status, a.email FROM postulerr a JOIN projet p ON a.id_projet = p.id WHERE a.email = ? ORDER BY a.id_projet DESC";
                stmt = conn.prepareStatement(query);
                stmt.setString(1, userEmail);
            }
            
            System.out.println("Executing query: " + query);
            ResultSet rs = stmt.executeQuery();
            
            int count = 0;
            while (rs.next()) {
                count++;
                String projectName = rs.getString("project_name");
                String status = rs.getString("status");
                String email = rs.getString("email");
                System.out.println("Found application - Project: " + projectName + ", Status: " + status + ", Email: " + email);
                
                applications.add(new ApplicationItem(
                    projectName,
                    status != null ? status : "Pending"
                ));
            }
            System.out.println("Total applications found: " + count);
            
            if (count == 0) {
                System.out.println("No applications found for user: " + userEmail);
                showAlert(Alert.AlertType.INFORMATION, "No Applications", 
                         "You haven't applied to any projects yet.");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", 
                     "Failed to load applications: " + e.getMessage());
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) applicationsTable.getScene().getWindow();
        stage.close();
    }

    private boolean confirmCancel(ApplicationItem item) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Cancel Application");
        alert.setHeaderText("Are you sure you want to cancel your application?");
        alert.setContentText("Project: " + item.getProjectName());
        return alert.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    private void cancelApplication(ApplicationItem item) {
        try {
            Connection conn = dataSource.getInstance().getConnection();
            String query = "DELETE FROM postulerr WHERE email = ? AND id_projet = (SELECT id FROM projet WHERE project_name = ? LIMIT 1)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, userEmail);
            stmt.setString(2, item.getProjectName());
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                applications.remove(item);
            } else {
                showError("Failed to cancel application.");
            }
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        }
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static class ApplicationItem {
        private final String projectName;
        private final String status;
        public ApplicationItem(String projectName, String status) {
            this.projectName = projectName;
            this.status = status;
        }
        public String getProjectName() { return projectName; }
        public String getStatus() { return status; }
    }
} 