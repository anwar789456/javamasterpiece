package org.example.controller.projet;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import utils.dataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

public class ProjectController {
    @FXML
    private Label projectNameLabel;
    @FXML
    private Label descriptionLabel;
    @FXML
    private Label startDateLabel;
    @FXML
    private Label endDateLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Label creatorNameLabel;
    @FXML
    private ImageView creatorAvatar;
    @FXML
    private FlowPane teamMembersFlowPane;
    @FXML
    private Label teamMembersLabel;
    @FXML
    private Button darkModeToggle;

    private int projectId;
    private String userEmail;
    private boolean isDarkMode = false;

    @FXML
    public void initialize() {
        System.out.println("ProjectController initializing...");
        System.out.println("Dark mode button: " + (darkModeToggle != null ? "found" : "not found"));
    }

    @FXML
    private void toggleDarkMode() {
        System.out.println("Toggle dark mode clicked");
        isDarkMode = !isDarkMode;
        VBox root = (VBox) darkModeToggle.getScene().getRoot();
        
        if (isDarkMode) {
            darkModeToggle.setText("Light Mode");
            root.setStyle("-fx-background-color: #1a1a1a;");
            darkModeToggle.setStyle("-fx-background-color: #64b5f6; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8 15; -fx-background-radius: 5; -fx-cursor: hand;");
        } else {
            darkModeToggle.setText("Dark Mode");
            root.setStyle("-fx-background-color: #f8f9fa;");
            darkModeToggle.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8 15; -fx-background-radius: 5; -fx-cursor: hand;");
        }
    }

    public void setProjectId(int projectId) {
        System.out.println("Setting project ID: " + projectId);
        this.projectId = projectId;
        loadProjectDetails();
        
        // Check if user is accepted before showing team members
        System.out.println("Checking if user is accepted. User email: " + userEmail);
        boolean isAccepted = isUserAccepted();
        System.out.println("User accepted status: " + isAccepted);
        
        if (isAccepted) {
            System.out.println("User is accepted, showing team members section");
            teamMembersLabel.setVisible(true);
            teamMembersFlowPane.setVisible(true);
            loadTeamMembers();
        } else {
            System.out.println("User is not accepted, hiding team members section");
            teamMembersLabel.setVisible(false);
            teamMembersFlowPane.setVisible(false);
        }
    }

    public void setUserEmail(String email) {
        if (email != null && !email.isEmpty()) {
            this.userEmail = email;
            System.out.println("User email set to: " + email);
        } else {
            System.out.println("Warning: Attempted to set null or empty user email");
        }
    }

    private boolean isUserAccepted() {
        if (userEmail == null || userEmail.isEmpty()) {
            System.out.println("User email is null or empty");
            return false;
        }

        String sql = """
            SELECT status
            FROM postulerr
            WHERE id_projet = ? AND email = ? AND status = 'Accepted'
            """;
            
        try (Connection conn = dataSource.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, projectId);
            stmt.setString(2, userEmail);
            System.out.println("Checking acceptance status for project: " + projectId + ", user: " + userEmail);
            var rs = stmt.executeQuery();
            
            boolean isAccepted = rs.next();
            System.out.println("User acceptance status: " + isAccepted);
            return isAccepted;
        } catch (SQLException e) {
            System.out.println("Error checking user acceptance: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void loadProjectDetails() {
        String sql = """
            SELECT p.project_name, p.description, p.start_date, p.end_date, p.status, 
                   u.email
            FROM projet p
            LEFT JOIN user u ON p.owner_id = u.id
            WHERE p.id = ?
            """;
            
        try (Connection conn = dataSource.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, projectId);
            var rs = stmt.executeQuery();
            
            if (rs.next()) {
                // Set project details
                projectNameLabel.setText(rs.getString("project_name"));
                descriptionLabel.setText(rs.getString("description"));
                
                // Format dates
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
                startDateLabel.setText(rs.getDate("start_date").toLocalDate().format(formatter));
                endDateLabel.setText(rs.getDate("end_date").toLocalDate().format(formatter));
                
                // Set status with color
                String status = rs.getString("status");
                statusLabel.setText(status);
                statusLabel.setTextFill(getStatusColor(status));
                
                // Set creator details
                String email = rs.getString("email");
                creatorNameLabel.setText(email);
                
                // Set default avatar
                creatorAvatar.setImage(new Image(getClass().getResourceAsStream("/images/default-avatar.png")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not load project details: " + e.getMessage());
        }
    }

    private void ensureTeamMembersTable() {
        String checkTableSql = "SHOW TABLES LIKE 'team_members'";
        try (Connection conn = dataSource.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(checkTableSql)) {
            
            var rs = stmt.executeQuery();
            if (!rs.next()) {
                System.out.println("Creating team_members table...");
                String createTableSql = """
                    CREATE TABLE team_members (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        project_id INT NOT NULL,
                        user_id INT NOT NULL,
                        role VARCHAR(50) NOT NULL,
                        join_date DATE NOT NULL,
                        FOREIGN KEY (project_id) REFERENCES projet(id),
                        FOREIGN KEY (user_id) REFERENCES user(id)
                    )
                    """;
                
                try (PreparedStatement createStmt = conn.prepareStatement(createTableSql)) {
                    createStmt.executeUpdate();
                    System.out.println("team_members table created successfully");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error checking/creating team_members table: " + e.getMessage());
        }
    }

    private void ensureTestTeamMembers() {
        // First ensure the table exists
        ensureTeamMembersTable();
        
        String checkSql = "SELECT COUNT(*) as count FROM team_members WHERE project_id = ?";
        try (Connection conn = dataSource.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(checkSql)) {
            
            stmt.setInt(1, projectId);
            var rs = stmt.executeQuery();
            rs.next();
            int count = rs.getInt("count");
            
            if (count == 0) {
                System.out.println("Adding test team members...");
                String insertSql = """
                    INSERT INTO team_members (project_id, user_id, role, join_date)
                    SELECT ?, id, 'Developer', CURRENT_DATE
                    FROM user
                    WHERE email = ?
                    """;
                
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setInt(1, projectId);
                    insertStmt.setString(2, userEmail);
                    int rows = insertStmt.executeUpdate();
                    System.out.println("Added " + rows + " test team member(s)");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error adding test team members: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void checkDatabaseState() {
        try (Connection conn = dataSource.getInstance().getConnection()) {
            // Check if user table exists and has data
            String checkUserSql = "SELECT COUNT(*) as count FROM user";
            try (PreparedStatement stmt = conn.prepareStatement(checkUserSql)) {
                var rs = stmt.executeQuery();
                rs.next();
                int userCount = rs.getInt("count");
                System.out.println("Users in database: " + userCount);
                
                if (userCount == 0) {
                    System.out.println("No users found in database!");
                    return;
                }
                
                // List all users
                String listUsersSql = "SELECT id, email FROM user";
                try (PreparedStatement listStmt = conn.prepareStatement(listUsersSql)) {
                    var userRs = listStmt.executeQuery();
                    System.out.println("User list:");
                    while (userRs.next()) {
                        System.out.println("ID: " + userRs.getInt("id") + ", Email: " + userRs.getString("email"));
                    }
                }
            }
            
            // Check if team_members table exists
            String checkTableSql = "SHOW TABLES LIKE 'team_members'";
            try (PreparedStatement stmt = conn.prepareStatement(checkTableSql)) {
                var rs = stmt.executeQuery();
                if (!rs.next()) {
                    System.out.println("team_members table does not exist!");
                    return;
                }
                System.out.println("team_members table exists");
            }
            
            // Check if there are any team members
            String checkMembersSql = "SELECT COUNT(*) as count FROM team_members";
            try (PreparedStatement stmt = conn.prepareStatement(checkMembersSql)) {
                var rs = stmt.executeQuery();
                rs.next();
                int memberCount = rs.getInt("count");
                System.out.println("Total team members: " + memberCount);
                
                if (memberCount > 0) {
                    // List all team members
                    String listMembersSql = """
                        SELECT tm.*, u.email 
                        FROM team_members tm 
                        JOIN user u ON tm.user_id = u.id
                        """;
                    try (PreparedStatement listStmt = conn.prepareStatement(listMembersSql)) {
                        var memberRs = listStmt.executeQuery();
                        System.out.println("Team members list:");
                        while (memberRs.next()) {
                            System.out.println("Project ID: " + memberRs.getInt("project_id") + 
                                             ", User ID: " + memberRs.getInt("user_id") + 
                                             ", Email: " + memberRs.getString("email"));
                        }
                    }
                }
            }
            
            // Check if there are team members for this project
            String checkProjectSql = "SELECT COUNT(*) as count FROM team_members WHERE project_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(checkProjectSql)) {
                stmt.setInt(1, projectId);
                var rs = stmt.executeQuery();
                rs.next();
                int projectMemberCount = rs.getInt("count");
                System.out.println("Team members for project " + projectId + ": " + projectMemberCount);
            }
            
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadTeamMembers() {
        System.out.println("Loading team members for project: " + projectId);
        teamMembersFlowPane.getChildren().clear();
        
        String sql = """
            SELECT p.email, p.status
            FROM postulerr p
            WHERE p.id_projet = ? AND p.status = 'Accepted'
            ORDER BY p.email
            """;
            
        try (Connection conn = dataSource.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, projectId);
            System.out.println("Executing SQL query for team members...");
            var rs = stmt.executeQuery();
            
            if (!rs.next()) {
                System.out.println("No team members found");
                Label noMembers = new Label("No team members yet");
                noMembers.setStyle("-fx-font-size: 16px; -fx-text-fill: #666;");
                teamMembersFlowPane.getChildren().add(noMembers);
                return;
            }
            
            System.out.println("Found team members, creating cards...");
            do {
                String email = rs.getString("email");
                System.out.println("Creating card for member: " + email);
                
                VBox memberCard = createMemberCard(
                    email, // Using email as name since first_name might not exist
                    email,
                    "" // Phone number might not be available
                );
                teamMembersFlowPane.getChildren().add(memberCard);
            } while (rs.next());
            
        } catch (SQLException e) {
            System.out.println("Error loading team members: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not load team members: " + e.getMessage());
        }
    }

    private VBox createMemberCard(String firstName, String email, String phone) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 10; -fx-padding: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        card.setPrefWidth(200);
        
        // Member avatar and name
        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        ImageView avatar = new ImageView();
        avatar.setFitWidth(40);
        avatar.setFitHeight(40);
        avatar.setPreserveRatio(true);
        avatar.setImage(new Image(getClass().getResourceAsStream("/images/default-avatar.png")));
        
        Label nameLabel = new Label(firstName);
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        header.getChildren().addAll(avatar, nameLabel);
        
        // Member details
        Label emailLabel = new Label(email);
        emailLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");
        
        Label phoneLabel = new Label(phone);
        phoneLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");
        
        card.getChildren().addAll(header, emailLabel, phoneLabel);
        return card;
    }

    private Color getStatusColor(String status) {
        if (status == null) return Color.web("#888888");
        
        return switch (status.toLowerCase()) {
            case "active" -> Color.web("#4CAF50");  // Green
            case "completed" -> Color.web("#2196F3");  // Blue
            case "on hold" -> Color.web("#FFC107");  // Yellow
            case "cancelled" -> Color.web("#F44336");  // Red
            case "open" -> Color.web("#4CAF50");  // Green
            case "closed" -> Color.web("#9E9E9E");  // Grey
            default -> Color.web("#888888");  // Default grey
        };
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
} 