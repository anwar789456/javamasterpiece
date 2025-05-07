package org.example.controller.quiz;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.example.model.quiz.Quiz;
import utils.dataSource;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FrontQuiz {
    @FXML
    private Button returnButton;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> statusFilter;
    @FXML
    private FlowPane quizGrid;

    private List<Quiz> allQuizzes = new ArrayList<>();

    @FXML
    private void initialize() {
        // Initialize status filter
        statusFilter.getItems().addAll("Tous", "Actif", "Terminé", "En attente");
        statusFilter.setValue("Tous");

        // Add listeners for search and filter
        searchField.textProperty().addListener((observable, oldValue, newValue) -> filterQuizzes());
        statusFilter.valueProperty().addListener((observable, oldValue, newValue) -> filterQuizzes());

        // Load quizzes
        loadQuizzes();
    }

    private void loadQuizzes() {
        allQuizzes.clear();
        quizGrid.getChildren().clear();

        try (Connection conn = dataSource.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, titre, datedebut, datefin, statut FROM Quiz")) {

            while (rs.next()) {
                Quiz quiz = new Quiz(
                    rs.getInt("id"),
                    rs.getString("titre"),
                    null, // description non utilisée
                    LocalDateTime.now(), // date_creation non utilisée
                    rs.getTimestamp("datedebut").toLocalDateTime(),
                    rs.getTimestamp("datefin").toLocalDateTime(),
                    rs.getString("statut")
                );
                allQuizzes.add(quiz);
            }

            displayQuizzes(allQuizzes);

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les quiz: " + e.getMessage());
        }
    }

    private void displayQuizzes(List<Quiz> quizzes) {
        quizGrid.getChildren().clear();
        
        for (Quiz quiz : quizzes) {
            VBox card = createQuizCard(quiz);
            quizGrid.getChildren().add(card);
        }
    }

    private VBox createQuizCard(Quiz quiz) {
        VBox card = new VBox(10);
        card.getStyleClass().add("quiz-card");
        card.setPrefWidth(300);
        card.setPrefHeight(150);

        // Titre du quiz avec icône
        HBox titleBox = new HBox(5);
        Label title = new Label(quiz.getTitre());
        title.getStyleClass().add("quiz-title");
        titleBox.getChildren().addAll(title);
        titleBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Dates
        HBox dateBox = new HBox(5);
        Label dateRange = new Label("Du " + quiz.getDateDebut().toLocalDate() + 
                                  " au " + quiz.getDateFin().toLocalDate());
        dateRange.getStyleClass().add("quiz-date");
        dateBox.getChildren().addAll(dateRange);
        dateBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Statut depuis la base
        String statut = quiz.getStatut();
        Label statutLabel = new Label("Statut : " + statut);
        switch (statut) {
            case "Actif":
                statutLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #4CAF50;");
                break;
            case "Terminé":
                statutLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #F44336;");
                break;
            case "Inactif":
                statutLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #9E9E9E;");
                break;
            default:
                statutLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2196F3;");
        }

        card.getChildren().addAll(titleBox, dateBox, statutLabel);
        return card;
    }

    private void filterQuizzes() {
        String searchText = searchField.getText().toLowerCase();
        String selectedStatus = statusFilter.getValue();

        List<Quiz> filteredQuizzes = allQuizzes.stream()
            .filter(quiz -> quiz.getTitre().toLowerCase().contains(searchText))
            .filter(quiz -> {
                if (selectedStatus.equals("Tous")) return true;
                return quiz.getStatut() != null && quiz.getStatut().equalsIgnoreCase(selectedStatus);
            })
            .collect(Collectors.toList());

        displayQuizzes(filteredQuizzes);
    }

    @FXML
    private void handleReturn() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Menu/menu.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) returnButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", 
                     "Impossible de retourner au menu: " + e.getMessage());
        }
    }

    @FXML
    private void handleShowHistory() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("QuizHistorique.fxml"));
            Parent root = loader.load();
            
            // Récupérer le contrôleur et définir l'ID utilisateur
            QuizHistoriqueController controller = loader.getController();
            controller.setUserId(16); // Utiliser l'ID utilisateur actuel
            
            Stage stage = new Stage();
            stage.setTitle("Historique des Quiz");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir l'historique: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
} 