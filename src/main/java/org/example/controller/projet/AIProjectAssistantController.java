package org.example.controller.projet;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import utils.dataSource;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class AIProjectAssistantController implements Initializable {
    @FXML private VBox chatArea;
    @FXML private TextField userInput;

    private List<Project> projects = new ArrayList<>();
    private Map<String, String> knowledgeBase = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadProjects();
        initializeKnowledgeBase();
        showWelcomeMessage();
    }

    private void loadProjects() {
        try (Connection conn = dataSource.getInstance().getConnection()) {
            String query = "SELECT * FROM projet ORDER BY start_date DESC";
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                projects.add(new Project(
                    rs.getInt("id"),
                    rs.getString("project_name"),
                    rs.getString("description"),
                    rs.getString("status"),
                    rs.getDate("start_date"),
                    rs.getDate("end_date")
                ));
            }
        } catch (SQLException e) {
            System.out.println("Error loading projects: " + e.getMessage());
        }
    }

    private void initializeKnowledgeBase() {
        // General responses
        knowledgeBase.put("how to join", """
            To join a project, follow these steps:
            1. Browse available projects
            2. Check project requirements
            3. Click "Apply Now" on the project card
            4. Fill out the application form
            5. Submit your application
            
            Tips for success:
            - Read project description carefully
            - Highlight relevant skills
            - Be clear about your availability
            - Explain your interest in the project
            """);

        knowledgeBase.put("requirements", """
            Common project requirements include:
            • Technical skills relevant to the project
            • Time commitment (usually specified in hours/week)
            • Communication skills
            • Team collaboration experience
            • Required certifications (if any)
            
            Specific requirements vary by project. Would you like to know about a specific project?
            """);

        knowledgeBase.put("application tips", """
            Here are some tips for a strong application:
            1. Customize your application for each project
            2. Highlight relevant experience
            3. Show enthusiasm and initiative
            4. Be specific about your skills
            5. Explain why you're a good fit
            6. Provide examples of past work
            7. Be honest about your availability
            
            Would you like more specific tips for a particular project?
            """);
    }

    private void showWelcomeMessage() {
        String welcome = """
            👋 Hello! I'm your AI Project Assistant.
            
            I can help you with:
            • Finding suitable projects
            • Understanding requirements
            • Application process
            • Project-specific questions
            
            Feel free to ask anything or use the quick action buttons below!
            """;
        addAssistantMessage(welcome);
    }

    @FXML
    private void sendMessage() {
        String message = userInput.getText().trim();
        if (message.isEmpty()) return;

        addUserMessage(message);
        userInput.clear();

        // Process the message asynchronously
        CompletableFuture.runAsync(() -> {
            String response = generateResponse(message);
            Platform.runLater(() -> addAssistantMessage(response));
        });
    }

    private String generateResponse(String message) {
        message = message.toLowerCase();

        // Check for project-specific questions
        if (message.contains("project") && message.contains("show")) {
            return generateProjectList();
        }

        // Check knowledge base for responses
        for (Map.Entry<String, String> entry : knowledgeBase.entrySet()) {
            if (message.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        // Generate contextual response based on message content
        if (message.contains("deadline") || message.contains("date")) {
            return "Project deadlines vary. Would you like to see the current open projects with their deadlines?";
        } else if (message.contains("skill") || message.contains("requirement")) {
            return knowledgeBase.get("requirements");
        } else if (message.contains("apply") || message.contains("join")) {
            return knowledgeBase.get("how to join");
        } else if (message.contains("tip") || message.contains("advice")) {
            return knowledgeBase.get("application tips");
        }

        // Default response
        return """
            I'm here to help! You can ask me about:
            • How to join projects
            • Project requirements
            • Application tips
            • Current projects
            • Specific project details
            
            What would you like to know more about?
            """;
    }

    private String generateProjectList() {
        StringBuilder response = new StringBuilder("Here are the current projects:\n\n");
        
        for (Project project : projects) {
            if (project.status.equalsIgnoreCase("active") || project.status.equalsIgnoreCase("open")) {
                response.append("📌 ").append(project.name).append("\n");
                response.append("   Status: ").append(project.status).append("\n");
                response.append("   Description: ").append(project.description).append("\n\n");
            }
        }
        
        response.append("\nWould you like more details about any specific project?");
        return response.toString();
    }

    @FXML
    private void askHowToJoin() {
        addUserMessage("How do I join a project?");
        addAssistantMessage(knowledgeBase.get("how to join"));
    }

    @FXML
    private void askShowProjects() {
        addUserMessage("Show me available projects");
        addAssistantMessage(generateProjectList());
    }

    @FXML
    private void askRequirements() {
        addUserMessage("What are the project requirements?");
        addAssistantMessage(knowledgeBase.get("requirements"));
    }

    @FXML
    private void askApplicationTips() {
        addUserMessage("Can you give me application tips?");
        addAssistantMessage(knowledgeBase.get("application tips"));
    }

    private void addUserMessage(String message) {
        HBox messageBox = new HBox();
        messageBox.setAlignment(Pos.CENTER_RIGHT);
        messageBox.setPadding(new Insets(5, 5, 5, 50));

        TextFlow textFlow = new TextFlow();
        textFlow.setStyle(
            "-fx-background-color: #2196F3;" +
            "-fx-background-radius: 20;" +
            "-fx-padding: 10;" +
            "-fx-text-fill: white;"
        );

        Text text = new Text(message);
        text.setStyle("-fx-fill: white;");
        textFlow.getChildren().add(text);

        messageBox.getChildren().add(textFlow);
        Platform.runLater(() -> {
            chatArea.getChildren().add(messageBox);
            scrollToBottom();
        });
    }

    private void addAssistantMessage(String message) {
        HBox messageBox = new HBox();
        messageBox.setAlignment(Pos.CENTER_LEFT);
        messageBox.setPadding(new Insets(5, 50, 5, 5));

        TextFlow textFlow = new TextFlow();
        textFlow.setStyle(
            "-fx-background-color: #f5f5f5;" +
            "-fx-background-radius: 20;" +
            "-fx-padding: 10;"
        );

        Text text = new Text(message);
        text.setStyle("-fx-fill: #333333;");
        textFlow.getChildren().add(text);

        messageBox.getChildren().add(textFlow);
        Platform.runLater(() -> {
            chatArea.getChildren().add(messageBox);
            scrollToBottom();
        });
    }

    private void scrollToBottom() {
        if (chatArea.getParent() instanceof ScrollPane) {
            ScrollPane scrollPane = (ScrollPane) chatArea.getParent();
            scrollPane.setVvalue(1.0);
        }
    }

    private static class Project {
        final int id;
        final String name;
        final String description;
        final String status;
        final Date startDate;
        final Date endDate;

        Project(int id, String name, String description, String status, Date startDate, Date endDate) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.status = status;
            this.startDate = startDate;
            this.endDate = endDate;
        }
    }
} 