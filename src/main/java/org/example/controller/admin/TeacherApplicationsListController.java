package org.example.controller.admin;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Callback;

import org.example.models.TeacherApplication;
import org.example.models.user.User;
import org.example.services.TeacherApplicationService;
import org.example.services.TeacherService;
import org.example.services.UserService;
import org.example.controller.login.SessionManager;
import utils.dataSource;
import org.example.services.DeclinedApplicationsService;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for displaying and managing teacher applications
 */
public class TeacherApplicationsListController implements Initializable {

    @FXML
    private TableView<TeacherApplicationDisplay> applicationsTable;

    @FXML
    private TableColumn<TeacherApplicationDisplay, String> userNameColumn;

    @FXML
    private TableColumn<TeacherApplicationDisplay, String> bioColumn;

    @FXML
    private TableColumn<TeacherApplicationDisplay, String> skillsColumn;

    @FXML
    private TableColumn<TeacherApplicationDisplay, String> experienceColumn;

    @FXML
    private TableColumn<TeacherApplicationDisplay, String> portfolioColumn;

    @FXML
    private TableColumn<TeacherApplicationDisplay, String> motivationColumn;

    @FXML
    private TableColumn<TeacherApplicationDisplay, Void> actionsColumn;

    private TeacherApplicationService applicationService;
    private TeacherService teacherService;
    private UserService userService;
    private DeclinedApplicationsService declinedApplicationsService;

    /**
     * Model class for displaying teacher applications in the table
     */
    public static class TeacherApplicationDisplay {
        private final int id;
        private final int userId;
        private final String userName;
        private final String bio;
        private final String skills;
        private final String experience;
        private final String portfolioUrl;
        private final String motivation;
        private boolean isDeclined = false;
        private String declineReason;

        public TeacherApplicationDisplay(int id, int userId, String userName, String bio, String skills, 
                                        String experience, String portfolioUrl, String motivation) {
            this.id = id;
            this.userId = userId;
            this.userName = userName;
            this.bio = bio;
            this.skills = skills;
            this.experience = experience;
            this.portfolioUrl = portfolioUrl;
            this.motivation = motivation;
        }

        public int getId() { return id; }
        public int getUserId() { return userId; }
        public String getUserName() { return userName; }
        public String getBio() { return bio; }
        public String getSkills() { return skills; }
        public String getExperience() { return experience; }
        public String getPortfolioUrl() { return portfolioUrl; }
        public String getMotivation() { return motivation; }
        
        public boolean isDeclined() { return isDeclined; }
        public void setDeclined(boolean declined) { isDeclined = declined; }
        
        public String getDeclineReason() { return declineReason; }
        public void setDeclineReason(String reason) { declineReason = reason; }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        applicationService = new TeacherApplicationService();
        teacherService = new TeacherService();
        userService = new UserService();
        declinedApplicationsService = new DeclinedApplicationsService();

        // Initialize table columns
        userNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getUserName()));
        bioColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getBio()));
        skillsColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getSkills()));
        experienceColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getExperience()));
        portfolioColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPortfolioUrl()));
        motivationColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getMotivation()));

        // Setup action buttons column
        setupActionsColumn();

        // Set row factory to style declined applications
        applicationsTable.setRowFactory(tv -> {
            TableRow<TeacherApplicationDisplay> row = new TableRow<>();
            
            row.itemProperty().addListener((obs, oldItem, newItem) -> {
                if (newItem != null && newItem.isDeclined()) {
                    row.setStyle("-fx-background-color: #EEEEEE;");
                } else {
                    row.setStyle("");
                }
            });
            
            return row;
        });

        // Load the applications data
        loadApplications();
        
        // Format the table
        formatTable();
    }
    
    /**
     * Format the table for better display
     */
    private void formatTable() {
        // Set column resize policy
        applicationsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Set row height
        applicationsTable.setFixedCellSize(80);
        
        // Set wrap text for text columns
        bioColumn.setCellFactory(tc -> {
            TableCell<TeacherApplicationDisplay, String> cell = new TableCell<>();
            Text text = new Text();
            cell.setGraphic(text);
            cell.setPrefHeight(Control.USE_COMPUTED_SIZE);
            text.wrappingWidthProperty().bind(bioColumn.widthProperty());
            text.textProperty().bind(cell.itemProperty());
            return cell;
        });
        
        experienceColumn.setCellFactory(tc -> {
            TableCell<TeacherApplicationDisplay, String> cell = new TableCell<>();
            Text text = new Text();
            cell.setGraphic(text);
            cell.setPrefHeight(Control.USE_COMPUTED_SIZE);
            text.wrappingWidthProperty().bind(experienceColumn.widthProperty());
            text.textProperty().bind(cell.itemProperty());
            return cell;
        });
        
        motivationColumn.setCellFactory(tc -> {
            TableCell<TeacherApplicationDisplay, String> cell = new TableCell<>();
            Text text = new Text();
            cell.setGraphic(text);
            cell.setPrefHeight(Control.USE_COMPUTED_SIZE);
            text.wrappingWidthProperty().bind(motivationColumn.widthProperty());
            text.textProperty().bind(cell.itemProperty());
            return cell;
        });
    }

    /**
     * Load all teacher applications from the database
     */
    private void loadApplications() {
        try {
            // Get all applications
            Connection connection = dataSource.getInstance().getConnection();
            String query = "SELECT a.*, u.name FROM applications a JOIN user u ON a.user_id = u.id";
            PreparedStatement pst = connection.prepareStatement(query);
            ResultSet rs = pst.executeQuery();

            ObservableList<TeacherApplicationDisplay> applications = FXCollections.observableArrayList();

            while (rs.next()) {
                TeacherApplicationDisplay app = new TeacherApplicationDisplay(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    rs.getString("name"), // User name from joined user table
                    rs.getString("bio"),
                    rs.getString("skills"),
                    rs.getString("experience"),
                    rs.getString("portfolio_url"),
                    rs.getString("motivation")
                );
                applications.add(app);
            }

            rs.close();
            pst.close();
            
            // Check which applications have been declined and load decline reasons
            String declinedQuery = "SELECT application_id, decline_reason FROM declined_applications";
            PreparedStatement declinedPst = connection.prepareStatement(declinedQuery);
            ResultSet declinedRs = declinedPst.executeQuery();
            
            // Map of application IDs to decline reasons
            java.util.Map<Integer, String> declineReasons = new java.util.HashMap<>();
            while (declinedRs.next()) {
                declineReasons.put(
                    declinedRs.getInt("application_id"), 
                    declinedRs.getString("decline_reason")
                );
            }
            
            declinedRs.close();
            declinedPst.close();
            dataSource.getInstance().releaseConnection(connection);
            
            // Mark declined applications and set reasons
            for (TeacherApplicationDisplay app : applications) {
                if (declineReasons.containsKey(app.getId())) {
                    app.setDeclined(true);
                    app.setDeclineReason(declineReasons.get(app.getId()));
                }
            }

            applicationsTable.setItems(applications);
        } catch (SQLException e) {
            showError("Error loading applications: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle accepting a teacher application
     * 
     * @param application The application to accept
     */
    private void handleAccept(TeacherApplicationDisplay application) {
        try {
            // Display confirmation dialog
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirm Accept");
            alert.setHeaderText("Accept Teacher Application");
            alert.setContentText("Are you sure you want to accept " + application.getUserName() + " as a teacher?");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                // Add the user as a teacher
                if (teacherService.addTeacher(application.getUserId())) {
                    // Delete the application after accepting
                    Connection connection = dataSource.getInstance().getConnection();
                    String query = "DELETE FROM applications WHERE id = ?";
                    PreparedStatement pst = connection.prepareStatement(query);
                    pst.setInt(1, application.getId());
                    pst.executeUpdate();
                    dataSource.getInstance().releaseConnection(connection);

                    // Show success message
                    showInfo(application.getUserName() + " has been accepted as a teacher!");
                    
                    // Refresh the table
                    loadApplications();
                } else {
                    showError("Failed to add teacher. They may already be a teacher.");
                }
            }
        } catch (SQLException e) {
            showError("Error accepting application: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle rejecting a teacher application
     * 
     * @param application The application to reject
     */
    private void handleReject(TeacherApplicationDisplay application) {
        // Create a custom dialog for entering decline reason
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Decline Application");
        dialog.setHeaderText("Please provide a reason for declining " + application.getUserName() + "'s application");

        // Set the button types
        ButtonType declineButtonType = new ButtonType("Decline", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = ButtonType.CANCEL;
        dialog.getDialogPane().getButtonTypes().addAll(declineButtonType, cancelButtonType);

        // Create the text area for the decline reason
        TextArea declineReasonText = new TextArea();
        declineReasonText.setPromptText("Enter the reason for declining this application...");
        declineReasonText.setPrefWidth(400);
        declineReasonText.setPrefHeight(200);
        declineReasonText.setWrapText(true);
        
        // Style the text area
        declineReasonText.setStyle("-fx-font-size: 14px; -fx-background-radius: 5px;");

        // Create the layout for the dialog
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(20, 20, 20, 20));
        vbox.getChildren().add(new Label("Reason for declining:"));
        vbox.getChildren().add(declineReasonText);
        dialog.getDialogPane().setContent(vbox);
        
        // Style the dialog
        dialog.getDialogPane().setPrefWidth(500);
        dialog.getDialogPane().getStyleClass().add("modern-dialog");
        
        // Add custom CSS stylesheet
        String cssPath = getClass().getResource("/css/dialog.css").toExternalForm();
        dialog.getDialogPane().getStylesheets().add(cssPath);
        
        // Style the buttons
        Button declineButton = (Button) dialog.getDialogPane().lookupButton(declineButtonType);
        declineButton.getStyleClass().add("decline-button");
        
        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(cancelButtonType);
        cancelButton.getStyleClass().add("cancel-button");
        
        // Request focus on the text area by default
        Platform.runLater(declineReasonText::requestFocus);

        // Convert the result to a string when the decline button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == declineButtonType) {
                return declineReasonText.getText();
            }
            return null;
        });

        // Show the dialog and wait for a response
        Optional<String> result = dialog.showAndWait();

        result.ifPresent(declineReason -> {
            if (declineReason.trim().isEmpty()) {
                showError("Please provide a reason for declining the application.");
                return;
            }
            
            try {
                // Get current admin ID from session
                int adminId = SessionManager.getInstance().getCurrentUserId();
                
                // Save to declined_applications table using the service
                boolean savedDeclined = declinedApplicationsService.saveDeclinedApplication(
                    application.getId(), adminId, declineReason);
                
                if (!savedDeclined) {
                    showError("Error saving decline reason to database.");
                    return;
                }
                
                // Mark the application as declined in the UI and set reason
                application.setDeclined(true);
                application.setDeclineReason(declineReason);
                
                // Show success message
                showInfo(application.getUserName() + "'s application has been declined.");
                
                // Refresh the table to update the UI
                applicationsTable.refresh();
                
            } catch (SQLException e) {
                showError("Error declining application: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Show an error message
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show an information message
     */
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Setup the actions column with accept and reject buttons
     */
    private void setupActionsColumn() {
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button acceptButton = new Button("Accept");
            private final Button rejectButton = new Button("Reject");
            private final Label declinedLabel = new Label("DECLINED");
            private final HBox buttonPane = new HBox(5, acceptButton, rejectButton);

            {
                // Style the buttons
                acceptButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 5;");
                rejectButton.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; -fx-background-radius: 5;");
                declinedLabel.setStyle("-fx-background-color: #9E9E9E; -fx-text-fill: white; -fx-padding: 5 10; " +
                                      "-fx-background-radius: 3; -fx-font-weight: bold;");
                
                buttonPane.setPadding(new Insets(5));
                buttonPane.setSpacing(10);

                // Set button actions
                acceptButton.setOnAction(event -> {
                    TeacherApplicationDisplay app = getTableView().getItems().get(getIndex());
                    handleAccept(app);
                });

                rejectButton.setOnAction(event -> {
                    TeacherApplicationDisplay app = getTableView().getItems().get(getIndex());
                    handleReject(app);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    TeacherApplicationDisplay app = getTableView().getItems().get(getIndex());
                    if (app.isDeclined()) {
                        // Set tooltip to show the decline reason
                        Tooltip tooltip = new Tooltip("Reason: " + app.getDeclineReason());
                        Tooltip.install(declinedLabel, tooltip);
                        setGraphic(declinedLabel);
                    } else {
                        setGraphic(buttonPane);
                    }
                }
            }
        });
    }
} 