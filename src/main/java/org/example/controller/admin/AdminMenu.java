package org.example.controller.admin;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class AdminMenu {
    @FXML
    private void handleUsers() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/views/admin/user/AfficherUser.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((javafx.scene.Node) loader.getController()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleFormations() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/views/admin/formation/AfficherFormation.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((javafx.scene.Node) loader.getController()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleQuiz() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/views/admin/quiz/AfficherQuiz.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((javafx.scene.Node) loader.getController()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleQuestions() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/views/admin/quiz/AfficherQuestionQuiz.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((javafx.scene.Node) loader.getController()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/views/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((javafx.scene.Node) loader.getController()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
} 