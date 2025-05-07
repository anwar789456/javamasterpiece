package org.example.controller.login;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.models.user.EmailSender;
import utils.dataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class PasswordResetController {
    @FXML
    private TextField emailField;

    @FXML
    private Label messageLabel;

    private Stage stage;
    private EmailSender emailSender;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void initialize() {
        emailSender = new EmailSender();
    }

    @FXML
    private void handleResetPassword() {
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            showMessage("Please enter your email address", true);
            return;
        }

        try {
            Connection conn = dataSource.getInstance().getConnection();
            String query = "SELECT * FROM user WHERE email = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String password = rs.getString("password");
                String name = rs.getString("name");

                // Send email with password
                String subject = "Password Recovery - Esprit Learning Platform";
                String content = String.format("""
                    Dear %s,
                    
                    You have requested your password for the Esprit Learning Platform.
                    Your password is: %s
                    
                    For security reasons, we recommend changing your password after logging in.
                    
                    Best regards,
                    Esprit E-Learning Platform Team
                    """, name, password);

                try {
                    emailSender.sendEmail(email, subject, content);
                    showMessage("Password sent to your email address. Please check your inbox.", false);
                    
                    // Close the window after a delay
                    new Thread(() -> {
                        try {
                            Thread.sleep(3000);
                            javafx.application.Platform.runLater(() -> stage.close());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }).start();
                } catch (Exception e) {
                    showMessage("Error sending email: " + e.getMessage(), true);
                    e.printStackTrace();
                }
            } else {
                showMessage("No account found with this email address", true);
            }
        } catch (Exception e) {
            showMessage("Error retrieving password: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    private void showMessage(String message, boolean isError) {
        messageLabel.setText(message);
        messageLabel.setStyle(isError ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
        messageLabel.setVisible(true);
    }
} 