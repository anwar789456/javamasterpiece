package org.example.controller.menu;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;

public class menu {
    @FXML
    private Button forumButton;
    
    @FXML
    private Button userForumButton;
    
    @FXML
    private Pane contentPane;
    
    @FXML
    private void initialize() {
        forumButton.setOnAction(event -> handleForumButton());
        userForumButton.setOnAction(event -> handleUserForumButton());
    }
    
    private void handleForumButton() {
        loadContentIntoPane("/Forum/forum.fxml", "Error loading admin forum");
    }
    
    private void handleUserForumButton() {
        loadContentIntoPane("/Forum/user_forum.fxml", "Error loading user forum");
    }
    
    /**
     * Loads content from the specified FXML file into the main content pane.
     * 
     * @param fxmlPath The path to the FXML file
     * @param errorMessage The error message to display if loading fails
     */
    private void loadContentIntoPane(String fxmlPath, String errorMessage) {
        try {
            // Load the content
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent content = loader.load();
            
            // Wrap the content in an AnchorPane to make it fill the contentPane
            AnchorPane wrapper = new AnchorPane();
            wrapper.getChildren().add(content);
            AnchorPane.setTopAnchor(content, 0.0);
            AnchorPane.setRightAnchor(content, 0.0);
            AnchorPane.setBottomAnchor(content, 0.0);
            AnchorPane.setLeftAnchor(content, 0.0);
            
            // Clear the contentPane and add the wrapper
            contentPane.getChildren().clear();
            contentPane.getChildren().add(wrapper);
            
            // Make the wrapper fill the contentPane
            wrapper.prefWidthProperty().bind(contentPane.widthProperty());
            wrapper.prefHeightProperty().bind(contentPane.heightProperty());
            
        } catch (Exception e) {
            System.err.println(errorMessage + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
