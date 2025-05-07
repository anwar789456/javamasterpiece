package org.example.controller.user;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class Signup {
    @FXML
    private TextField nameField;
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Button createAccountButton;
    @FXML
    private Button backToLoginButton;

    @FXML
    private void initialize() {
        // Add event handlers
        createAccountButton.setOnAction(event -> handleCreateAccount());
        backToLoginButton.setOnAction(event -> handleBackToLogin());
    }

    private void handleCreateAccount() {

    }

    private void handleBackToLogin() {
        try {
            // Load the login form
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Parent root = loader.load();
            
            // Get the current stage
            Stage stage = (Stage) backToLoginButton.getScene().getWindow();
            
            // Set the new scene
            stage.setScene(new Scene(root));
            stage.setTitle("Login");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
} 