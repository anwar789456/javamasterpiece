package org.example.controller.projet;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.TilePane;
import utils.dataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AfficherProjetUserController {
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private TilePane projectsGrid;
    
    private List<ProjectData> projects = new ArrayList<>();
    
    @FXML
    private void initialize() {
        // Initialize status filter
        statusFilter.getItems().addAll("All", "Active", "Completed", "On Hold", "Cancelled");
        statusFilter.setValue("All");
        
        // Load initial projects
        loadProjects();
    }
    
    @FXML
    private void handleSearch() {
        String searchText = searchField.getText().toLowerCase();
        String statusFilterValue = statusFilter.getValue();
        
        // Clear current projects
        projectsGrid.getChildren().clear();
        
        // Filter and display projects
        for (ProjectData project : projects) {
            if (matchesSearch(project, searchText, statusFilterValue)) {
                addProjectCard(project);
            }
        }
    }
    
    private boolean matchesSearch(ProjectData project, String searchText, String status) {
        boolean matchesSearch = project.name.toLowerCase().contains(searchText) ||
                              project.description.toLowerCase().contains(searchText);
        
        boolean matchesStatus = status.equals("All") || project.status.equals(status);
        
        return matchesSearch && matchesStatus;
    }
    
    private void loadProjects() {
        try {
            Connection conn = dataSource.getInstance().getConnection();
            if (conn == null) {
                showError("Database connection failed. Please check your database configuration.");
                return;
            }
            
            // Try with a more general query first to see if there are any projects
            String query = "SELECT * FROM projet";
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            
            projects.clear();
            projectsGrid.getChildren().clear();
            
            int projectCount = 0;
            while (rs.next()) {
                projectCount++;
                ProjectData project = new ProjectData();
                project.id = rs.getInt("id");
                project.name = rs.getString("project_name");
                project.description = rs.getString("description");
                project.dates = formatDates(rs.getDate("start_date"), rs.getDate("end_date"));
                project.status = rs.getString("status");
                
                projects.add(project);
                addProjectCard(project);
            }
            
            if (projectCount == 0) {
                showInfo("No projects found in the database. Please add some projects first.");
            } else {
                showInfo("Loaded " + projectCount + " projects successfully.");
            }
        } catch (SQLException e) {
            showError("Error loading projects: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void addProjectCard(ProjectData project) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/projet/ProjectCard.fxml"));
            Node card = loader.load();
            ProjectCardController cardController = loader.getController();
            
            // Convert ProjectData to Project
            org.example.model.ProjectData projectModel = new org.example.model.ProjectData(
                project.id,
                project.name,
                project.description,
                null, // startDate
                null, // endDate
                project.status,
                null  // imageUrl
            );
            
            cardController.setProject(projectModel);
            projectsGrid.getChildren().add(card);
        } catch (Exception e) {
            showError("Error creating project card: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private String formatDates(java.sql.Date startDate, java.sql.Date endDate) {
        if (startDate == null || endDate == null) {
            return "Dates not specified";
        }
        
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
            return startDate.toLocalDate().format(formatter) + " - " + 
                   endDate.toLocalDate().format(formatter);
        } catch (Exception e) {
            return "Invalid date format";
        }
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // Project data class
    public static class ProjectData {
        public int id;
        public String name;
        public String description;
        public String dates;
        public String status;
    }
} 