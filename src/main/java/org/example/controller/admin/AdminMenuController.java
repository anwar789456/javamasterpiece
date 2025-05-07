package org.example.controller.admin;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import java.util.HashMap;
import java.util.Map;

public class AdminMenuController {
    @FXML
    private Button quizManagementButton;
    
    @FXML
    private Button questionManagementButton;
    
    @FXML
    private Pane contentPane;
    
    private Button selectedButton;
    private final Map<Button, String> buttonPathMap = new HashMap<>();
    
    @FXML
    private void handleButtonClick(javafx.event.ActionEvent event) {
        Button clickedButton = (Button) event.getSource();
        String fxmlPath = buttonPathMap.get(clickedButton);
        if (fxmlPath != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                Parent content = loader.load();
                contentPane.getChildren().setAll(content);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    @FXML
    private void initialize() {
        // Store button-path mappings
        buttonPathMap.put(quizManagementButton, "/quiz/AfficherQuiz.fxml");
        buttonPathMap.put(questionManagementButton, "/quiz/AfficherQuestionQuiz.fxml");
        
        // Set up button actions
        quizManagementButton.setOnAction(event -> handleButtonClick(quizManagementButton));
        questionManagementButton.setOnAction(event -> handleButtonClick(questionManagementButton));
    }
    
    @FXML
    public void handleButtonClick(Button clickedButton) {
        if (clickedButton == selectedButton) {
            return;
        }
        
        if (selectedButton != null) {
            selectedButton.getStyleClass().remove("menu-button-selected");
        }
        
        clickedButton.getStyleClass().add("menu-button-selected");
        selectedButton = clickedButton;
        
        String fxmlPath = buttonPathMap.get(clickedButton);
        loadContent(fxmlPath);
    }
    
    private void loadContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent content = loader.load();
            
            AnchorPane wrapper = new AnchorPane();
            wrapper.getChildren().add(content);
            AnchorPane.setTopAnchor(content, 0.0);
            AnchorPane.setRightAnchor(content, 0.0);
            AnchorPane.setBottomAnchor(content, 0.0);
            AnchorPane.setLeftAnchor(content, 0.0);
            
            contentPane.getChildren().clear();
            contentPane.getChildren().add(wrapper);
            
            wrapper.prefWidthProperty().bind(contentPane.widthProperty());
            wrapper.prefHeightProperty().bind(contentPane.heightProperty());
            
        } catch (Exception e) {
            System.err.println("Error loading content: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 