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
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.time.LocalDate;
import java.util.Optional;

public class AfficherProjet {

    @FXML
    private TableView<ProjetTableItem> tableProjet;

    @FXML
    private TableColumn<ProjetTableItem, Integer> colId;

    @FXML
    private TableColumn<ProjetTableItem, Integer> colOwnerId;

    @FXML
    private TableColumn<ProjetTableItem, String> colProjectName;

    @FXML
    private TableColumn<ProjetTableItem, String> colDescription;

    @FXML
    private TableColumn<ProjetTableItem, Date> colStartDate;

    @FXML
    private TableColumn<ProjetTableItem, Date> colEndDate;

    @FXML
    private TableColumn<ProjetTableItem, String> colStatus;

    @FXML
    private Button btnAdd;

    @FXML
    private Button btnUpdate;

    @FXML
    private Button btnDelete;

    @FXML
    private TextField tfSearch;

    @FXML
    private Button btnSearch;

    @FXML
    private ComboBox<String> cbStatusFilter;

    @FXML
    private DatePicker dpStartDate;

    @FXML
    private DatePicker dpEndDate;

    @FXML
    private Button btnFilterByDate;

    private ObservableList<ProjetTableItem> projetList = FXCollections.observableArrayList();
    
    private int currentUserId = 1; // Default user ID

    @FXML
    private void initialize() {
        // Set up table columns
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colOwnerId.setCellValueFactory(new PropertyValueFactory<>("ownerId"));
        colProjectName.setCellValueFactory(new PropertyValueFactory<>("projectName"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colStartDate.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        colEndDate.setCellValueFactory(new PropertyValueFactory<>("endDate"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Set up status filter
        cbStatusFilter.getItems().addAll("All", "Active", "Completed", "On Hold", "Cancelled");
        cbStatusFilter.setValue("All");
        cbStatusFilter.setOnAction(event -> filterByStatus());

        // Set button actions
        btnAdd.setOnAction(event -> openAddProjectForm());
        btnUpdate.setOnAction(event -> updateSelectedProject());
        btnDelete.setOnAction(event -> deleteSelectedProject());
        btnSearch.setOnAction(event -> searchProjects());

        // Set up date filter
        btnFilterByDate.setOnAction(event -> filterByDateRange());

        // Load all projects initially
        loadProjects();
    }

    private void loadProjects() {
        projetList.clear();

        try {
            Connection conn = dataSource.getInstance().getConnection();
            // Modified to select all projects instead of filtering by owner_id
            String query = "SELECT * FROM projet";
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                ProjetTableItem projet = new ProjetTableItem(
                    rs.getInt("id"),
                    rs.getInt("owner_id"),
                    rs.getString("project_name"),
                    rs.getString("description"),
                    rs.getDate("start_date"),
                    rs.getDate("end_date"),
                    rs.getString("status")
                );
                projetList.add(projet);
            }

            tableProjet.setItems(projetList);

            if (projetList.isEmpty()) {
                showAlert(Alert.AlertType.INFORMATION, "Information", "Aucun projet",
                         "Aucun projet n'a été trouvé dans la base de données.", "info.png");
            }

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de chargement",
                     "Erreur lors du chargement des projets: " + e.getMessage(), "error.png");
        }
    }

    private ImageView createAlertIcon(String imageName) {
        try {
            String imagePath = "/images/" + imageName;
            if (getClass().getResourceAsStream(imagePath) != null) {
                return new ImageView(new Image(getClass().getResourceAsStream(imagePath)));
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not load image " + imageName);
        }
        return null;
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content, String imageName) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        
        ImageView icon = createAlertIcon(imageName);
        if (icon != null) {
            alert.setGraphic(icon);
        }
        
        alert.showAndWait();
    }

    @FXML
    private void openAddProjectForm() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/projet/AjouterProjet.fxml"));
            if (loader.getLocation() == null) {
                throw new IOException("Fichier FXML non trouvé");
            }
            Parent root = loader.load();
            
            AjouterProjetController controller = loader.getController();
            if (controller == null) {
                throw new IOException("Contrôleur non trouvé");
            }
            
            // Set the default owner ID but allow it to be changed
            TextField ownerIdField = (TextField) root.lookup("#tfOwnerId");
            if (ownerIdField != null) {
                ownerIdField.setText(String.valueOf(currentUserId));
            }
            
            Stage stage = new Stage();
            stage.setTitle("Ajouter un projet");
            stage.setScene(new Scene(root));
            stage.show();

            stage.setOnHidden(event -> {
                loadProjects();
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Opération réussie", 
                         "Le projet a été ajouté avec succès!", "success.png");
            });

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de chargement",
                     "Erreur lors du chargement du formulaire: " + e.getMessage(), "error.png");
        }
    }

    @FXML
    private void updateSelectedProject() {
        ProjetTableItem selectedProject = tableProjet.getSelectionModel().getSelectedItem();
        if (selectedProject == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Aucun projet sélectionné",
                     "Veuillez sélectionner un projet à modifier.", "warning.png");
            return;
        }

        try {
            System.out.println("Attempting to load ModifierProjet.fxml...");
            URL resourceUrl = getClass().getResource("/projet/ModifierProjet.fxml");
            System.out.println("Resource URL: " + resourceUrl);
            
            FXMLLoader loader = new FXMLLoader(resourceUrl);
            if (loader.getLocation() == null) {
                System.err.println("FXML location is null!");
                throw new IOException("Fichier FXML non trouvé");
            }
            System.out.println("FXML loader created successfully");
            
            Parent root = loader.load();
            System.out.println("FXML loaded successfully");

            EditProjectController controller = loader.getController();
            if (controller == null) {
                System.err.println("Controller is null!");
                throw new IOException("Contrôleur non trouvé");
            }
            System.out.println("Controller obtained successfully");
            
            controller.setProjectData(selectedProject);
            System.out.println("Project data set successfully");

            Stage stage = new Stage();
            stage.setTitle("Modifier le projet");
            stage.setScene(new Scene(root));
            
            // Only show success message if update was successful
            stage.setOnHidden(event -> {
                if (controller.isUpdateSuccessful()) {
                    loadProjects();
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Opération réussie",
                             "Le projet a été modifié avec succès!", "success.png");
                }
            });
            
            stage.show();
            System.out.println("Edit form window opened successfully");

        } catch (IOException e) {
            System.err.println("Error loading form: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de chargement",
                     "Erreur lors du chargement du formulaire: " + e.getMessage(), "error.png");
        }
    }

    @FXML
    private void deleteSelectedProject() {
        ProjetTableItem selectedProject = tableProjet.getSelectionModel().getSelectedItem();
        if (selectedProject == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Aucun projet sélectionné",
                     "Veuillez sélectionner un projet à supprimer.", "warning.png");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation de suppression");
        confirmation.setHeaderText("Supprimer le projet");
        confirmation.setContentText("Êtes-vous sûr de vouloir supprimer ce projet ?");
        ImageView icon = createAlertIcon("warning.png");
        if (icon != null) {
            confirmation.setGraphic(icon);
        }

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                Connection conn = dataSource.getInstance().getConnection();
                String query = "DELETE FROM projet WHERE id = ?";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setInt(1, selectedProject.getId());

                int rowsDeleted = stmt.executeUpdate();
                if (rowsDeleted > 0) {
                    loadProjects();
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Suppression réussie",
                             "Le projet a été supprimé avec succès!", "success.png");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de la suppression",
                             "Le projet n'a pas pu être supprimé.", "error.png");
                }
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de base de données",
                         "Erreur lors de la suppression: " + e.getMessage(), "error.png");
            }
        }
    }

    @FXML
    private void searchProjects() {
        String searchText = tfSearch.getText().trim();
        if (searchText.isEmpty()) {
            loadProjects();
            return;
        }

        projetList.clear();
        try {
            Connection conn = dataSource.getInstance().getConnection();
            String query = "SELECT * FROM projet WHERE project_name LIKE ? OR description LIKE ? OR status LIKE ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            String searchPattern = "%" + searchText + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setString(3, searchPattern);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                ProjetTableItem projet = new ProjetTableItem(
                    rs.getInt("id"),
                    rs.getInt("owner_id"),
                    rs.getString("project_name"),
                    rs.getString("description"),
                    rs.getDate("start_date"),
                    rs.getDate("end_date"),
                    rs.getString("status")
                );
                projetList.add(projet);
            }

            tableProjet.setItems(projetList);

            if (projetList.isEmpty()) {
                showAlert(Alert.AlertType.INFORMATION, "Information", "Aucun résultat",
                         "Aucun projet ne correspond à votre recherche.", "info.png");
            }

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de recherche",
                     "Erreur lors de la recherche: " + e.getMessage(), "error.png");
        }
    }

    @FXML
    private void filterByStatus() {
        String selectedStatus = cbStatusFilter.getValue();
        if (selectedStatus.equals("All")) {
            loadProjects();
            return;
        }

        projetList.clear();
        try {
            Connection conn = dataSource.getInstance().getConnection();
            String query = "SELECT * FROM projet WHERE status = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, selectedStatus);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                ProjetTableItem projet = new ProjetTableItem(
                    rs.getInt("id"),
                    rs.getInt("owner_id"),
                    rs.getString("project_name"),
                    rs.getString("description"),
                    rs.getDate("start_date"),
                    rs.getDate("end_date"),
                    rs.getString("status")
                );
                projetList.add(projet);
            }

            tableProjet.setItems(projetList);

            if (projetList.isEmpty()) {
                showAlert(Alert.AlertType.INFORMATION, "Information", "Aucun projet",
                         "Aucun projet avec le statut: " + selectedStatus, "info.png");
            }

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de filtrage",
                     "Erreur lors du filtrage des projets: " + e.getMessage(), "error.png");
        }
    }

    @FXML
    private void filterByDateRange() {
        LocalDate startDate = dpStartDate.getValue();
        LocalDate endDate = dpEndDate.getValue();

        if (startDate == null || endDate == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Date Range Error");
            alert.setHeaderText("Missing Date");
            alert.setContentText("Please select both start and end dates");
            alert.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/images/warning.png"))));
            alert.showAndWait();
            return;
        }

        if (startDate.isAfter(endDate)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Date Range Error");
            alert.setHeaderText("Invalid Date Range");
            alert.setContentText("Start date cannot be after end date");
            alert.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/images/warning.png"))));
            alert.showAndWait();
            return;
        }

        projetList.clear();
        try {
            Connection conn = dataSource.getInstance().getConnection();
            String query = "SELECT * FROM projet WHERE start_date >= ? AND end_date <= ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setDate(1, java.sql.Date.valueOf(startDate));
            stmt.setDate(2, java.sql.Date.valueOf(endDate));
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                ProjetTableItem projet = new ProjetTableItem(
                    rs.getInt("id"),
                    rs.getInt("owner_id"),
                    rs.getString("project_name"),
                    rs.getString("description"),
                    rs.getDate("start_date"),
                    rs.getDate("end_date"),
                    rs.getString("status")
                );
                projetList.add(projet);
            }

            if (projetList.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Filter Results");
                alert.setHeaderText("No Projects Found");
                alert.setContentText("No projects found in the selected date range");
                alert.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/images/info.png"))));
                alert.showAndWait();
            }

            tableProjet.setItems(projetList);
        } catch (SQLException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Filter Failed");
            alert.setContentText("Database error: " + e.getMessage());
            alert.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/images/error.png"))));
            alert.showAndWait();
        }
    }

    public void setCurrentUserId(int userId) {
        this.currentUserId = userId;
        // Reload projects to reflect the current user's projects
        loadProjects();
    }

    // Inner class for TableView items
    public static class ProjetTableItem {
        private int id;
        private int ownerId;
        private String projectName;
        private String description;
        private Date startDate;
        private Date endDate;
        private String status;

        public ProjetTableItem(int id, int ownerId, String projectName, String description,
                               Date startDate, Date endDate, String status) {
            this.id = id;
            this.ownerId = ownerId;
            this.projectName = projectName;
            this.description = description;
            this.startDate = startDate;
            this.endDate = endDate;
            this.status = status;
        }

        // Getters
        public int getId() { return id; }
        public int getOwnerId() { return ownerId; }
        public String getProjectName() { return projectName; }
        public String getDescription() { return description; }
        public Date getStartDate() { return startDate; }
        public Date getEndDate() { return endDate; }
        public String getStatus() { return status; }

        // Setters
        public void setId(int id) { this.id = id; }
        public void setOwnerId(int ownerId) { this.ownerId = ownerId; }
        public void setProjectName(String projectName) { this.projectName = projectName; }
        public void setDescription(String description) { this.description = description; }
        public void setStartDate(Date startDate) { this.startDate = startDate; }
        public void setEndDate(Date endDate) { this.endDate = endDate; }
        public void setStatus(String status) { this.status = status; }
    }
} 