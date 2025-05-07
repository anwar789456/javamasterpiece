package org.example.controller.login;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Controller for the login screen
 */
public class LoginController {
    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    private Button loginButton;

    @FXML
    private Button signUpButton;

    @FXML
    private Button scanCINButton;

    /**
     * Handle login button click event
     */
    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        // Validate inputs
        if (email.isEmpty()) {
            showError("Please enter your email");
            return;
        }

        if (password.isEmpty()) {
            showError("Please enter your password");
            return;
        }

        // Use the LoginHandler to process the login
        Stage stage = (Stage) loginButton.getScene().getWindow();
        boolean success = LoginHandler.handleLogin(email, password, stage);

        if (!success) {
            showError("Invalid email or password");
        }
    }

    /**
     * Handle CIN scanner login
     */
    @FXML
    private void handleScanCINLogin() {
        try {
            // Get the absolute path to the Python script
            String scriptPath = new File("src/main/resources/IntelligentScanner/cinScanner.py").getAbsolutePath();
            System.out.println("Attempting to run Python script at: " + scriptPath);

            // Create the process builder with proper error handling
            ProcessBuilder processBuilder = new ProcessBuilder(
                "python",
                scriptPath
            );
            
            // Redirect error stream to standard output
            processBuilder.redirectErrorStream(true);
            
            // Start the process
            Process process = processBuilder.start();
            
            // Read both standard output and error streams
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            StringBuilder output = new StringBuilder();
            boolean missingPackages = false;
            String packageInstallCommand = "";
            
            // Read all output
            while ((line = reader.readLine()) != null) {
                System.out.println("Python output: " + line);
                output.append(line).append("\n");
                
                // Check for missing packages message
                if (line.contains("Missing required Python packages")) {
                    missingPackages = true;
                }
                if (line.contains("pip install")) {
                    packageInstallCommand = line;
                }
            }
            
            // Wait for the process to complete
            int exitCode = process.waitFor();
            System.out.println("Python process exited with code: " + exitCode);
            
            if (missingPackages) {
                showError("Required Python packages are missing. Please run: " + packageInstallCommand);
                return;
            }
            
            // Extract the CIN number from the output
            String cinNumber = null;
            for (String outputLine : output.toString().split("\n")) {
                if (outputLine.matches("\\d{7,10}")) {
                    cinNumber = outputLine.trim();
                    break;
                }
            }

            if (cinNumber != null && !cinNumber.trim().isEmpty()) {
                System.out.println("Found CIN number: " + cinNumber);
                // Use the LoginHandler to process the CIN login
                Stage stage = (Stage) scanCINButton.getScene().getWindow();
                boolean success = LoginHandler.handleCINLogin(cinNumber.trim(), stage);

                if (!success) {
                    showError("No account found with this CIN");
                }
            } else {
                System.out.println("No valid CIN number found in output");
                showError("Could not scan CIN. Please try again or use email login.");
            }
        } catch (Exception e) {
            System.err.println("Error during CIN scanning: " + e.getMessage());
            e.printStackTrace();
            showError("Error scanning CIN: " + e.getMessage());
        }
    }

    /**
     * Show an error message
     *
     * @param message The error message to display
     */
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    /**
     * Initialize the controller
     */
    @FXML
    private void initialize() {
        // Add any initialization code here

        // Add listeners for when enter is pressed in the text fields
        emailField.setOnAction(event -> {
            if (emailField.getText().trim().isEmpty()) {
                showError("Please enter your email");
            } else {
                passwordField.requestFocus();
            }
        });

        passwordField.setOnAction(event -> {
            if (!emailField.getText().trim().isEmpty() && !passwordField.getText().isEmpty()) {
                handleLogin();
            } else {
                showError("Please fill in all fields");
            }
        });

        // Add event handler for the signup button
        signUpButton.setOnAction(event -> openSignUpForm());
    }

    /**
     * Open the signup form when the signup button is clicked
     */
    private void openSignUpForm() {
        try {
            // Load the signup FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/SignUp.fxml"));
            Parent signUpRoot = loader.load();

            // Get the controller
            org.example.controller.user.SignUpController signUpController = loader.getController();

            // Create a new stage for the signup form
            Stage signUpStage = new Stage();
            signUpStage.initModality(Modality.APPLICATION_MODAL);
            signUpStage.initStyle(StageStyle.DECORATED);
            signUpStage.setTitle("Create Account");
            signUpStage.setScene(new Scene(signUpRoot));

            // Set the close button action
            signUpController.setStage(signUpStage);

            // Show the signup form and wait for it to close
            signUpStage.showAndWait();

            // If the user was created successfully, pre-fill the login form
            if (signUpController.isUserCreated()) {
                String email = signUpController.getCreatedUserEmail();
                emailField.setText(email);
                passwordField.requestFocus();
                errorLabel.setVisible(false);
            }

        } catch (IOException e) {
            e.printStackTrace();
            showError("Could not open sign up form");
        }
    }

    /**
     * Handle forgot password link click
     */
    @FXML
    private void handleForgotPassword() {
        try {
            // Load the password reset FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/PasswordReset.fxml"));
            Parent resetRoot = loader.load();

            // Get the controller
            PasswordResetController resetController = loader.getController();

            // Create a new stage for the password reset form
            Stage resetStage = new Stage();
            resetStage.initModality(Modality.APPLICATION_MODAL);
            resetStage.initStyle(StageStyle.DECORATED);
            resetStage.setTitle("Reset Password");
            resetStage.setScene(new Scene(resetRoot));

            // Set the stage in the controller
            resetController.setStage(resetStage);

            // Show the password reset form
            resetStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Could not open password reset form");
        }
    }
}