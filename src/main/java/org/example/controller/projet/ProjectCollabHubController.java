package org.example.controller.projet;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import utils.dataSource;

import java.io.File;
import java.net.URL;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class ProjectCollabHubController implements Initializable {
    @FXML private Text projectTitle;
    @FXML private Text projectDescription;
    @FXML private Label statusLabel;
    @FXML private VBox chatArea;
    @FXML private TextField messageField;
    @FXML private VBox taskBoard;
    @FXML private ListView<String> teamMembersList;
    @FXML private ListView<String> filesList;

    private int projectId;
    private String currentUserEmail;
    private ObservableList<String> teamMembers = FXCollections.observableArrayList();
    private ObservableList<String> files = FXCollections.observableArrayList();

    private volatile boolean isRunning = true;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("Initializing ProjectCollabHubController");
        teamMembersList.setItems(teamMembers);
        filesList.setItems(files);
        
        // Create required tables if they don't exist
        createRequiredTables();
        
        // Start chat update thread with a flag to control it
        Thread chatUpdateThread = new Thread(this::updateChatPeriodically);
        chatUpdateThread.setDaemon(true);
        chatUpdateThread.start();
    }

    public void setProjectData(int projectId, String currentUserEmail) {
        System.out.println("Setting project data - ID: " + projectId + ", Email: " + currentUserEmail);
        this.projectId = projectId;
        this.currentUserEmail = currentUserEmail;
        
        if (projectId <= 0) {
            showError("Invalid project ID");
            return;
        }
        
        if (currentUserEmail == null || currentUserEmail.isEmpty()) {
            showError("Invalid user email");
            return;
        }
        
        loadProjectDetails();
        loadTeamMembers();
        loadTasks();
        loadFiles();
        loadChatHistory();
    }

    private void loadProjectDetails() {
        System.out.println("Loading project details for ID: " + projectId);
        try (Connection conn = dataSource.getInstance().getConnection()) {
            String query = "SELECT project_name, description, status FROM projet WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, projectId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Platform.runLater(() -> {
                    try {
                        projectTitle.setText(rs.getString("project_name"));
                        projectDescription.setText(rs.getString("description"));
                        String status = rs.getString("status");
                        statusLabel.setText(status);
                        statusLabel.setStyle(getStatusStyle(status));
                    } catch (Exception e) {
                        System.out.println("Error updating UI: " + e.getMessage());
                    }
                });
            } else {
                System.out.println("No project found with ID: " + projectId);
                showError("Project not found");
            }
        } catch (SQLException e) {
            System.out.println("Database error loading project details: " + e.getMessage());
            showError("Error loading project details: " + e.getMessage());
        }
    }

    private void loadTeamMembers() {
        System.out.println("Loading team members for project: " + projectId);
        try (Connection conn = dataSource.getInstance().getConnection()) {
            String query = "SELECT DISTINCT p.email, p.first_name FROM postulerr p WHERE p.id_projet = ? AND p.status = 'Accepted'";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, projectId);
            ResultSet rs = stmt.executeQuery();

            // Store results in a temporary list
            List<String> tempMembers = new ArrayList<>();
            while (rs.next()) {
                tempMembers.add(rs.getString("first_name") + " (" + rs.getString("email") + ")");
            }

            // Update UI with collected data
            Platform.runLater(() -> {
                teamMembers.clear();
                teamMembers.addAll(tempMembers);
            });
        } catch (SQLException e) {
            System.out.println("Database error loading team members: " + e.getMessage());
            showError("Error loading team members: " + e.getMessage());
        }
    }

    @FXML
    private void sendMessage() {
        String message = messageField.getText().trim();
        if (message.isEmpty()) return;

        System.out.println("Sending message for project: " + projectId);
        try (Connection conn = dataSource.getInstance().getConnection()) {
            String query = "INSERT INTO project_messages (project_id, sender_email, message_text) VALUES (?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, projectId);
            stmt.setString(2, currentUserEmail);
            stmt.setString(3, message);
            stmt.executeUpdate();

            Platform.runLater(() -> {
                try {
                    messageField.clear();
                    loadChatHistory(); // Reload chat to show new message
                } catch (Exception e) {
                    System.out.println("Error updating chat UI: " + e.getMessage());
                }
            });
        } catch (SQLException e) {
            System.out.println("Database error sending message: " + e.getMessage());
            showError("Error sending message: " + e.getMessage());
        }
    }

    private void loadChatHistory() {
        System.out.println("Loading chat history for project: " + projectId);
        try (Connection conn = dataSource.getInstance().getConnection()) {
            String query = """
                SELECT m.sender_email, p.first_name, m.message_text, m.sent_time 
                FROM project_messages m 
                LEFT JOIN postulerr p ON m.sender_email = p.email 
                WHERE m.project_id = ? 
                ORDER BY m.sent_time DESC 
                LIMIT 50
                """;
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, projectId);
            ResultSet rs = stmt.executeQuery();

            // Store results in a temporary list
            List<ChatMessage> messages = new ArrayList<>();
            while (rs.next()) {
                messages.add(new ChatMessage(
                    rs.getString("sender_email"),
                    rs.getString("first_name"),
                    rs.getString("message_text"),
                    rs.getTimestamp("sent_time")
                ));
            }

            // Update UI with collected data
            Platform.runLater(() -> {
                chatArea.getChildren().clear();
                for (ChatMessage msg : messages) {
                    VBox messageBox = createMessageBox(
                        msg.senderName != null ? msg.senderName : msg.senderEmail,
                        msg.messageText,
                        msg.sentTime,
                        msg.senderEmail.equals(currentUserEmail)
                    );
                    chatArea.getChildren().add(0, messageBox);
                }
            });
        } catch (SQLException e) {
            System.out.println("Database error loading chat history: " + e.getMessage());
            showError("Error loading chat history: " + e.getMessage());
        }
    }

    private void updateChatPeriodically() {
        while (isRunning) {
            try {
                Thread.sleep(5000); // Update every 5 seconds
                if (projectId > 0) { // Only update if we have a valid project ID
                    loadChatHistory();
                }
            } catch (InterruptedException e) {
                System.out.println("Chat update thread interrupted");
                break;
            } catch (Exception e) {
                System.out.println("Error in chat update: " + e.getMessage());
            }
        }
    }

    @FXML
    private void addTask() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Add New Task");
        dialog.setHeaderText("Enter task details");

        TextField taskField = new TextField();
        dialog.getDialogPane().setContent(taskField);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return taskField.getText();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(task -> {
            try (Connection conn = dataSource.getInstance().getConnection()) {
                String query = "INSERT INTO project_tasks (project_id, task_description, created_by, status) VALUES (?, ?, ?, 'Pending')";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setInt(1, projectId);
                stmt.setString(2, task);
                stmt.setString(3, currentUserEmail);
                stmt.executeUpdate();

                loadTasks(); // Reload tasks to show new task
            } catch (SQLException e) {
                showError("Error adding task: " + e.getMessage());
            }
        });
    }

    private void loadTasks() {
        System.out.println("Loading tasks for project: " + projectId);
        try (Connection conn = dataSource.getInstance().getConnection()) {
            String query = "SELECT * FROM project_tasks WHERE project_id = ? ORDER BY created_at DESC";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, projectId);
            ResultSet rs = stmt.executeQuery();

            // Store results in a temporary list
            List<Task> tasks = new ArrayList<>();
            while (rs.next()) {
                tasks.add(new Task(
                    rs.getInt("id"),
                    rs.getString("task_description"),
                    rs.getString("status")
                ));
            }

            // Update UI with collected data
            Platform.runLater(() -> {
                taskBoard.getChildren().clear();
                for (Task task : tasks) {
                    HBox taskBox = createTaskBox(
                        task.id,
                        task.description,
                        task.status
                    );
                    taskBoard.getChildren().add(taskBox);
                }
            });
        } catch (SQLException e) {
            System.out.println("Database error loading tasks: " + e.getMessage());
            showError("Error loading tasks: " + e.getMessage());
        }
    }

    @FXML
    private void uploadFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File to Upload");
        File file = fileChooser.showOpenDialog(null);
        
        if (file != null) {
            try (Connection conn = dataSource.getInstance().getConnection()) {
                String query = "INSERT INTO project_files (project_id, file_name, uploaded_by) VALUES (?, ?, ?)";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setInt(1, projectId);
                stmt.setString(2, file.getName());
                stmt.setString(3, currentUserEmail);
                stmt.executeUpdate();

                loadFiles(); // Reload files list
            } catch (SQLException e) {
                showError("Error uploading file: " + e.getMessage());
            }
        }
    }

    private void loadFiles() {
        System.out.println("Loading files for project: " + projectId);
        try (Connection conn = dataSource.getInstance().getConnection()) {
            String query = "SELECT * FROM project_files WHERE project_id = ? ORDER BY uploaded_at DESC";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, projectId);
            ResultSet rs = stmt.executeQuery();

            // Store results in a temporary list
            List<String> tempFiles = new ArrayList<>();
            while (rs.next()) {
                tempFiles.add(rs.getString("file_name"));
            }

            // Update UI with collected data
            Platform.runLater(() -> {
                files.clear();
                files.addAll(tempFiles);
            });
        } catch (SQLException e) {
            System.out.println("Database error loading files: " + e.getMessage());
            showError("Error loading files: " + e.getMessage());
        }
    }

    private VBox createMessageBox(String sender, String message, Timestamp time, boolean isCurrentUser) {
        VBox messageBox = new VBox(5);
        messageBox.setPadding(new Insets(10));
        messageBox.setStyle("-fx-background-color: " + (isCurrentUser ? "#e3f2fd" : "#f5f5f5") + "; -fx-background-radius: 10;");
        messageBox.setMaxWidth(400);

        Label senderLabel = new Label(sender);
        senderLabel.setStyle("-fx-font-weight: bold;");

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);

        Label timeLabel = new Label(time.toLocalDateTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");

        messageBox.getChildren().addAll(senderLabel, messageLabel, timeLabel);
        messageBox.setAlignment(isCurrentUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        return messageBox;
    }

    private HBox createTaskBox(int taskId, String description, String status) {
        HBox taskBox = new HBox(10);
        taskBox.setAlignment(Pos.CENTER_LEFT);
        taskBox.setPadding(new Insets(10));
        taskBox.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-radius: 5;");

        CheckBox checkBox = new CheckBox();
        checkBox.setSelected(status.equals("Completed"));
        checkBox.setOnAction(e -> updateTaskStatus(taskId, checkBox.isSelected()));

        Label descLabel = new Label(description);
        descLabel.setWrapText(true);
        HBox.setHgrow(descLabel, Priority.ALWAYS);

        taskBox.getChildren().addAll(checkBox, descLabel);
        return taskBox;
    }

    private void updateTaskStatus(int taskId, boolean completed) {
        try (Connection conn = dataSource.getInstance().getConnection()) {
            String query = "UPDATE project_tasks SET status = ? WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, completed ? "Completed" : "Pending");
            stmt.setInt(2, taskId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            showError("Error updating task status: " + e.getMessage());
        }
    }

    private String getStatusStyle(String status) {
        String color = switch (status.toLowerCase()) {
            case "active" -> "#4CAF50";
            case "completed" -> "#2196F3";
            case "on hold" -> "#FFC107";
            case "cancelled" -> "#F44336";
            default -> "#757575";
        };
        return String.format("-fx-background-color: %s; -fx-text-fill: white;", color);
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void createRequiredTables() {
        try (Connection conn = dataSource.getInstance().getConnection()) {
            // Create project_messages table
            String createMessagesTable = """
                CREATE TABLE IF NOT EXISTS project_messages (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    project_id INT NOT NULL,
                    sender_email VARCHAR(255) NOT NULL,
                    message_text TEXT NOT NULL,
                    sent_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;
            
            // Create project_tasks table
            String createTasksTable = """
                CREATE TABLE IF NOT EXISTS project_tasks (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    project_id INT NOT NULL,
                    task_description TEXT NOT NULL,
                    status VARCHAR(50) DEFAULT 'Pending',
                    created_by VARCHAR(255) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    completed_at TIMESTAMP NULL
                )
                """;
            
            // Create project_files table
            String createFilesTable = """
                CREATE TABLE IF NOT EXISTS project_files (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    project_id INT NOT NULL,
                    file_name VARCHAR(255) NOT NULL,
                    file_path VARCHAR(1000),
                    uploaded_by VARCHAR(255) NOT NULL,
                    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;

            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createMessagesTable);
                stmt.execute(createTasksTable);
                stmt.execute(createFilesTable);
                
                // Add foreign key constraints if they don't exist
                try {
                    stmt.execute("""
                        ALTER TABLE project_messages 
                        ADD CONSTRAINT fk_messages_project 
                        FOREIGN KEY (project_id) REFERENCES projet(id) ON DELETE CASCADE
                    """);
                } catch (SQLException e) {
                    // Ignore if constraint already exists
                }
                
                try {
                    stmt.execute("""
                        ALTER TABLE project_tasks 
                        ADD CONSTRAINT fk_tasks_project 
                        FOREIGN KEY (project_id) REFERENCES projet(id) ON DELETE CASCADE
                    """);
                } catch (SQLException e) {
                    // Ignore if constraint already exists
                }
                
                try {
                    stmt.execute("""
                        ALTER TABLE project_files 
                        ADD CONSTRAINT fk_files_project 
                        FOREIGN KEY (project_id) REFERENCES projet(id) ON DELETE CASCADE
                    """);
                } catch (SQLException e) {
                    // Ignore if constraint already exists
                }
            }
        } catch (SQLException e) {
            showError("Error creating required tables: " + e.getMessage());
        }
    }

    // Helper classes to store data
    private static class ChatMessage {
        final String senderEmail;
        final String senderName;
        final String messageText;
        final Timestamp sentTime;

        ChatMessage(String senderEmail, String senderName, String messageText, Timestamp sentTime) {
            this.senderEmail = senderEmail;
            this.senderName = senderName;
            this.messageText = messageText;
            this.sentTime = sentTime;
        }
    }

    private static class Task {
        final int id;
        final String description;
        final String status;

        Task(int id, String description, String status) {
            this.id = id;
            this.description = description;
            this.status = status;
        }
    }

    public void cleanup() {
        isRunning = false;
    }
} 