package org.example.controller.projet;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.util.NavigationUtils;
import utils.dataSource;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.application.Platform;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class UserProjectViewController implements Initializable {
    @FXML private TilePane projectsGrid;
    @FXML private TextField searchField;
    private Connection connection;
    private List<Project> allProjects = new ArrayList<>();
    private String currentUserEmail;
    private boolean isDarkMode = false;
    private VBox root;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            connection = dataSource.getInstance().getConnection();
            loadAllProjects();
            showProjects(allProjects);
            
            // Set current user email from UserSession
            currentUserEmail = org.example.util.UserSession.getInstance().getUserEmail();
            System.out.println("Initialized with user email: " + currentUserEmail);
            
            // Wait for scene to be set before adding dark mode toggle
            projectsGrid.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    Platform.runLater(() -> {
                        // Get the root VBox by traversing up from projectsGrid
                        Node current = projectsGrid;
                        while (current != null && !(current instanceof VBox)) {
                            current = current.getParent();
                        }
                        root = (VBox) current;
                        
                        if (root != null) {
                            setupDarkModeToggle();
                        }
                    });
                }
            });

            // Show onboarding wizard if needed
            if (ProjectOnboardingWizardController.shouldShowOnboarding()) {
                Platform.runLater(() -> {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/projet/ProjectOnboardingWizard.fxml"));
                        Parent root = loader.load();
                        Stage wizardStage = new Stage();
                        wizardStage.setTitle("Welcome to Project Management");
                        wizardStage.initModality(Modality.APPLICATION_MODAL);
                        wizardStage.setScene(new Scene(root));
                        wizardStage.show();
                    } catch (IOException e) {
                        System.out.println("Could not load onboarding wizard: " + e.getMessage());
                    }
                });
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to connect to database: " + e.getMessage());
        }
    }

    private void setupDarkModeToggle() {
        // Add dark mode toggle button at the top
        HBox topBar = new HBox(10);
        topBar.setAlignment(Pos.CENTER_RIGHT);
        topBar.setPadding(new Insets(10));
        
        // Create a more professional looking toggle button
        Button darkModeBtn = new Button("🌙 Dark Mode");
        darkModeBtn.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white; -fx-font-size: 14px; " +
                           "-fx-padding: 8 15; -fx-background-radius: 5; -fx-cursor: hand; " +
                           "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 2);");
        darkModeBtn.setOnAction(e -> toggleDarkMode(darkModeBtn));
        
        // Add hover effect
        darkModeBtn.setOnMouseEntered(e -> 
            darkModeBtn.setStyle("-fx-background-color: #34495e; -fx-text-fill: white; -fx-font-size: 14px; " +
                               "-fx-padding: 8 15; -fx-background-radius: 5; -fx-cursor: hand; " +
                               "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 6, 0, 0, 3);"));
        darkModeBtn.setOnMouseExited(e -> 
            darkModeBtn.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white; -fx-font-size: 14px; " +
                               "-fx-padding: 8 15; -fx-background-radius: 5; -fx-cursor: hand; " +
                               "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 2);"));
        
        topBar.getChildren().add(darkModeBtn);
        
        // Add the top bar as the first child of the root VBox
        if (!root.getChildren().isEmpty() && root.getChildren().get(0) instanceof HBox) {
            return;
        }
        root.getChildren().add(0, topBar);
    }

    private void loadAllProjects() {
        allProjects.clear();
        try {
            String query = "SELECT * FROM projet ORDER BY start_date DESC";
            PreparedStatement pst = connection.prepareStatement(query);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                allProjects.add(new Project(
                        rs.getInt("id"),
                        rs.getString("project_name"),
                        rs.getString("description"),
                        rs.getDate("start_date").toLocalDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                        rs.getDate("end_date").toLocalDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                        rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load projects: " + e.getMessage());
        }
    }

    private void showProjects(List<Project> projects) {
        projectsGrid.getChildren().clear();
        if (projects.isEmpty()) {
            Label noProjects = new Label("No active projects found.");
            noProjects.setStyle("-fx-font-size: 18px; -fx-text-fill: #888; -fx-padding: 40;");
            projectsGrid.getChildren().add(noProjects);
            return;
        }
        for (Project project : projects) {
            VBox card = createProjectCard(project, searchField.getText());
            projectsGrid.getChildren().add(card);
        }
    }

    @FXML
    private void handleSearch() {
        String searchText = searchField.getText().toLowerCase(Locale.ROOT).trim();
        if (searchText.isEmpty()) {
            showProjects(allProjects);
            return;
        }
        List<Project> filtered = new ArrayList<>();
        for (Project p : allProjects) {
            if (p.name.toLowerCase(Locale.ROOT).contains(searchText) ||
                p.description.toLowerCase(Locale.ROOT).contains(searchText)) {
                filtered.add(p);
            }
        }
        showProjects(filtered);
    }

    private VBox createProjectCard(Project project, String searchText) {
        VBox card = new VBox(10);
        String cardStyle = isDarkMode ? 
            "-fx-background-color: #1e1e1e; -fx-background-radius: 10; -fx-padding: 20; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 8, 0, 0, 3); -fx-border-color: #333333; -fx-border-width: 1; -fx-border-radius: 10;" :
            "-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 20; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 3); -fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-border-radius: 10;";
        card.setStyle(cardStyle);
        card.setPrefWidth(320);
        card.setPrefHeight(220);

        // Add hover effect
        card.setOnMouseEntered(e -> {
            if (isDarkMode) {
                card.setStyle(cardStyle + "-fx-background-color: #252525;");
            } else {
                card.setStyle(cardStyle + "-fx-background-color: #f8f9fa;");
            }
        });
        card.setOnMouseExited(e -> card.setStyle(cardStyle));

        Label title = new Label(highlightText(project.name, searchText));
        String titleStyle = "-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + 
                          (isDarkMode ? "#e0e0e0" : "#2c3e50") + ";";
        title.setStyle(titleStyle);
        title.setWrapText(true);

        Label desc = new Label(highlightText(project.description, searchText));
        String descStyle = "-fx-text-fill: " + (isDarkMode ? "#9e9e9e" : "#666") + 
                         "; -fx-font-size: 14px; -fx-line-spacing: 5;";
        desc.setStyle(descStyle);
        desc.setWrapText(true);
        desc.setMaxHeight(70);

        Label dateLabel = new Label(project.startDate + " - " + project.endDate);
        String dateStyle = "-fx-text-fill: " + (isDarkMode ? "#757575" : "#888") + 
                         "; -fx-font-size: 13px; -fx-font-style: italic;";
        dateLabel.setStyle(dateStyle);

        Label status = new Label(project.status);
        status.setStyle("-fx-background-color: " + getStatusColor(project.status) + 
                       "; -fx-text-fill: white; -fx-padding: 6 12; -fx-background-radius: 15; " +
                       "-fx-font-size: 13px; -fx-font-weight: bold;");

        Button applyBtn = new Button("Apply Now");
        String btnStyle = isDarkMode ?
            "-fx-background-color: #2e7d32; -fx-text-fill: white; -fx-background-radius: 5; " +
            "-fx-padding: 8 20; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 4, 0, 0, 2);" :
            "-fx-background-color: #43e97b; -fx-text-fill: white; -fx-background-radius: 5; " +
            "-fx-padding: 8 20; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 2);";
        applyBtn.setStyle(btnStyle);
        
        // Add hover effect for apply button
        applyBtn.setOnMouseEntered(e -> {
            if (isDarkMode) {
                applyBtn.setStyle("-fx-background-color: #1b5e20; -fx-text-fill: white; -fx-background-radius: 5; " +
                                "-fx-padding: 8 20; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 6, 0, 0, 3);");
            } else {
                applyBtn.setStyle("-fx-background-color: #38d16b; -fx-text-fill: white; -fx-background-radius: 5; " +
                                "-fx-padding: 8 20; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 6, 0, 0, 3);");
            }
        });
        applyBtn.setOnMouseExited(e -> applyBtn.setStyle(btnStyle));
        
        applyBtn.setOnAction(e -> openApplyModal(project.id));

        card.setOnMouseClicked(e -> openProjectView(project.id));
        card.getChildren().addAll(title, desc, dateLabel, status, applyBtn);
        return card;
    }

    private String highlightText(String text, String highlight) {
        if (highlight == null || highlight.isEmpty()) return text;
        String lower = text.toLowerCase(Locale.ROOT);
        String lowerHighlight = highlight.toLowerCase(Locale.ROOT);
        int idx = lower.indexOf(lowerHighlight);
        if (idx < 0) return text;
        // Simple HTML-style highlight (JavaFX Label supports basic HTML)
        return text.substring(0, idx) + "[" + text.substring(idx, idx + highlight.length()) + "]" + text.substring(idx + highlight.length());
    }

    private void openApplyModal(int projectId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/projet/Postuler.fxml"));
            Parent root = loader.load();
            Object controller = loader.getController();
            if (controller instanceof org.example.controller.projet.PostulerController) {
                ((org.example.controller.projet.PostulerController) controller).setProjectId(projectId);
            }
            Stage stage = new Stage();
            stage.setTitle("Apply to Project");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to open application form: " + e.getMessage());
        }
    }

    private void openProjectView(int projectId) {
        try {
            System.out.println("Opening project view for ID: " + projectId);
            
            if (projectId <= 0) {
                showAlert(Alert.AlertType.ERROR, "Error", "Invalid project ID");
                return;
            }
            
            if (currentUserEmail == null || currentUserEmail.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Error", "User email not set");
                return;
            }
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/projet/ProjectCollabHub.fxml"));
            Parent root = loader.load();
            
            ProjectCollabHubController controller = loader.getController();
            controller.setProjectData(projectId, currentUserEmail);
            
            Stage stage = new Stage();
            stage.setTitle("Project Collaboration Hub");
            Scene scene = new Scene(root);
            
            // Add dark mode support
            if (isDarkMode) {
                scene.getRoot().setStyle("-fx-background-color: #121212;");
            }
            
            stage.setScene(scene);
            stage.show();
            
            // Clean up when window is closed
            stage.setOnCloseRequest(e -> {
                if (controller != null) {
                    controller.cleanup();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not open project collaboration hub: " + e.getMessage());
        }
    }

    @FXML
    private void filterByStatus() { loadAllProjects(); showProjects(allProjects); }
    @FXML
    private void sortByDate() { loadAllProjects(); showProjects(allProjects); }
    @FXML
    private void sortByPopularity() { loadAllProjects(); showProjects(allProjects); }

    @FXML
    private void showCalendar() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/projet/ProjectCalendarView.fxml"));
            Parent root = loader.load();
            ProjectCalendarViewController controller = loader.getController();

            // Use French locale for parsing
            java.time.format.DateTimeFormatter frenchFormatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy", java.util.Locale.FRENCH);

            // Convert your project list to the calendar's Project format
            List<ProjectCalendarViewController.Project> calendarProjects = new ArrayList<>();
            for (Project p : allProjects) {
                calendarProjects.add(
                    new ProjectCalendarViewController.Project(
                        p.name,
                        java.time.LocalDate.parse(p.startDate, frenchFormatter),
                        java.time.LocalDate.parse(p.endDate, frenchFormatter),
                        p.status,
                        p.description
                    )
                );
            }
            controller.setProjects(calendarProjects);

            Stage stage = new Stage();
            stage.setTitle("Calendrier des Projets");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to open calendar: " + e.getMessage());
        }
    }

    @FXML
    private void showMyApplications() {
        try {
            // Get email from UserSession if not set directly
            if (currentUserEmail == null || currentUserEmail.isEmpty()) {
                currentUserEmail = org.example.util.UserSession.getInstance().getUserEmail();
            }
            
            System.out.println("Opening My Applications view with user email: " + currentUserEmail);
            
            if (currentUserEmail == null || currentUserEmail.isEmpty()) {
                // For testing purposes, use a default email
                currentUserEmail = "test@example.com";
                org.example.util.UserSession.getInstance().setUserEmail(currentUserEmail);
                System.out.println("Using default test email: " + currentUserEmail);
            }
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/projet/MyApplicationsView.fxml"));
            Parent root = loader.load();
            MyApplicationsViewController controller = loader.getController();
            controller.setUserEmail(currentUserEmail);
            
            Stage stage = new Stage();
            stage.setTitle("My Applications");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to open My Applications: " + e.getMessage());
        }
    }

    @FXML
    private void openAIAssistant() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/projet/AIProjectAssistant.fxml"));
            Parent root = loader.load();
            
            Stage stage = new Stage();
            stage.setTitle("AI Project Assistant");
            Scene scene = new Scene(root);
            
            // Add dark mode support
            if (isDarkMode) {
                scene.getRoot().setStyle("-fx-background-color: #121212;");
            }
            
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not open AI Assistant: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void setCurrentUserId(int userId) {
        // Optionally store or use the userId for user-specific features
    }

    public void setCurrentUserEmail(String email) {
        if (email != null && !email.isEmpty()) {
            this.currentUserEmail = email;
            System.out.println("User email set in UserProjectViewController: " + email);
            // Also update the UserSession
            org.example.util.UserSession.getInstance().setUserEmail(email);
        } else {
            System.out.println("Warning: Attempted to set null or empty user email in UserProjectViewController");
        }
    }

    private String getStatusColor(String status) {
        if (status == null) return "#888888";
        
        return switch (status.toLowerCase()) {
            case "active" -> "#4CAF50";  // Green
            case "completed" -> "#2196F3";  // Blue
            case "on hold" -> "#FFC107";  // Yellow
            case "cancelled" -> "#F44336";  // Red
            case "open" -> "#4CAF50";  // Green
            case "closed" -> "#9E9E9E";  // Grey
            default -> "#888888";  // Default grey
        };
    }

    private void toggleDarkMode(Button darkModeBtn) {
        isDarkMode = !isDarkMode;
        if (root == null) return;
        
        if (isDarkMode) {
            // Switch to dark mode with darker colors
            root.setStyle("-fx-background-color: #121212;"); // Darker background
            searchField.setStyle("-fx-background-color: #1e1e1e; -fx-text-fill: #e0e0e0; " +
                               "-fx-prompt-text-fill: #666666; -fx-background-radius: 5; " +
                               "-fx-border-color: #333333; -fx-border-radius: 5;");
            darkModeBtn.setText("☀️ Light Mode");
            darkModeBtn.setStyle("-fx-background-color: #1e1e1e; -fx-text-fill: #e0e0e0; -fx-font-size: 14px; " +
                               "-fx-padding: 8 15; -fx-background-radius: 5; -fx-cursor: hand; " +
                               "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 6, 0, 0, 3); " +
                               "-fx-border-color: #333333; -fx-border-radius: 5;");
            
            // Update all project cards
            for (Node node : projectsGrid.getChildren()) {
                if (node instanceof VBox) {
                    VBox card = (VBox) node;
                    card.setStyle("-fx-background-color: #1e1e1e; -fx-background-radius: 10; -fx-padding: 20; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 8, 0, 0, 3); " +
                                "-fx-border-color: #333333; -fx-border-width: 1; -fx-border-radius: 10;");
                    
                    // Update labels in the card
                    for (Node cardNode : card.getChildren()) {
                        if (cardNode instanceof Label) {
                            Label label = (Label) cardNode;
                            if (label.getStyle().contains("-fx-font-weight: bold")) {
                                label.setStyle(label.getStyle().replace("-fx-text-fill: #333", "-fx-text-fill: #e0e0e0"));
                            } else if (!label.getStyle().contains("-fx-background-color")) {
                                label.setStyle(label.getStyle().replace("-fx-text-fill: #666", "-fx-text-fill: #9e9e9e"));
                            }
                        } else if (cardNode instanceof Button) {
                            Button btn = (Button) cardNode;
                            if (btn.getText().equals("Apply Now")) {
                                btn.setStyle("-fx-background-color: #2e7d32; -fx-text-fill: white; -fx-background-radius: 5; " +
                                           "-fx-padding: 8 20; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand; " +
                                           "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 4, 0, 0, 2);");
                            }
                        }
                    }
                }
            }
        } else {
            // Switch back to light mode (keep existing light mode code)
            root.setStyle("-fx-background-color: #f8f9fa;");
            searchField.setStyle("-fx-background-color: white; -fx-text-fill: black; " +
                               "-fx-prompt-text-fill: #757575; -fx-background-radius: 5; " +
                               "-fx-border-color: #e0e0e0; -fx-border-radius: 5;");
            darkModeBtn.setText("🌙 Dark Mode");
            darkModeBtn.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white; -fx-font-size: 14px; " +
                               "-fx-padding: 8 15; -fx-background-radius: 5; -fx-cursor: hand; " +
                               "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 2);");
            
            // Update all project cards (keep existing light mode card code)
            for (Node node : projectsGrid.getChildren()) {
                if (node instanceof VBox) {
                    VBox card = (VBox) node;
                    card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 20; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 3); " +
                                "-fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-border-radius: 10;");
                    
                    // Update labels in the card
                    for (Node cardNode : card.getChildren()) {
                        if (cardNode instanceof Label) {
                            Label label = (Label) cardNode;
                            if (label.getStyle().contains("-fx-font-weight: bold")) {
                                label.setStyle(label.getStyle().replace("-fx-text-fill: white", "-fx-text-fill: #2c3e50"));
                            } else if (!label.getStyle().contains("-fx-background-color")) {
                                label.setStyle(label.getStyle().replace("-fx-text-fill: #9e9e9e", "-fx-text-fill: #666"));
                            }
                        } else if (cardNode instanceof Button) {
                            Button btn = (Button) cardNode;
                            if (btn.getText().equals("Apply Now")) {
                                btn.setStyle("-fx-background-color: #43e97b; -fx-text-fill: white; -fx-background-radius: 5; " +
                                           "-fx-padding: 8 20; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand; " +
                                           "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 2);");
                            }
                        }
                    }
                }
            }
        }
    }

    // Simple project data class
    private static class Project {
        int id;
        String name;
        String description;
        String startDate;
        String endDate;
        String status;
        Project(int id, String name, String description, String startDate, String endDate, String status) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.startDate = startDate;
            this.endDate = endDate;
            this.status = status;
        }
    }
} 