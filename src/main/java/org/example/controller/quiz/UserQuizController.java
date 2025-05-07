package org.example.controller.quiz;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import org.example.model.quiz.Quiz;
import org.example.model.quiz.QuestionQuiz;
import org.example.models.user.User;
import org.example.controller.login.SessionManager;
import utils.dataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class UserQuizController {
    @FXML
    private VBox quizzesContainer;
    @FXML
    private Button historyButton;
    @FXML
    private Button quizButton;
    @FXML
    private Button questionQuizButton;
    @FXML
    private Button competitionButton;

    private User currentUser;

    @FXML
    private void initialize() {
        // Get the current user from the session
        currentUser = SessionManager.getInstance().getCurrentUser();
        loadQuizzes();
    }

    /**
     * Sets the current user
     * @param user The user to set
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadQuizzes(); // Reload quizzes with the new user
    }
    
    /**
     * Sets the current user ID from the session
     * This method is maintained for compatibility with FrontMenu
     * but now gets the user directly from SessionManager
     * @param userId The user ID (not used directly)
     */
    public void setCurrentUserId(int userId) {
        // Get the current user from the session instead of loading from DB
        this.currentUser = SessionManager.getInstance().getCurrentUser();
        
        if (currentUser == null) {
            showAlert(Alert.AlertType.WARNING, "Warning", "No user is currently logged in.");
            return;
        }
        
        // Log the current user for debugging
        System.out.println("Quiz controller initialized with user ID: " + currentUser.getId() + 
                           ", email: " + currentUser.getEmail());
        
        loadQuizzes();
    }

    @FXML
    private void handleShowHistory() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/quiz/QuizHistorique.fxml"));
            Parent root = loader.load();
            
            // Make sure we have a current user
            if (currentUser == null) {
                currentUser = SessionManager.getInstance().getCurrentUser();
                if (currentUser == null) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Vous devez être connecté pour voir l'historique.");
                    return;
                }
            }
            
            // Récupérer le contrôleur et définir l'ID utilisateur
            QuizHistoriqueController controller = loader.getController();
            controller.setUserId(currentUser.getId()); // Utiliser l'ID de l'utilisateur actuel
            
            Stage stage = new Stage();
            stage.setTitle("Historique des Quiz");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir l'historique: " + e.getMessage());
        }
    }

    @FXML
    private void handleQuizzes() {
        loadQuizzes(); // Reload quizzes when the quiz button is clicked
    }

    @FXML
    private void handleQuestions() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/quiz/AfficherQuestionQuizUser.fxml"));
            Parent root = loader.load();
            
            // You can add any specific setup here if needed
            
            Stage stage = new Stage();
            stage.setTitle("Questions de Quiz");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'afficher les questions: " + e.getMessage());
        }
    }

    @FXML
    private void handleCompetitions() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/quiz/CompetitionQuiz.fxml"));
            Parent root = loader.load();
            
            // You can add any specific setup here if needed
            
            Stage stage = new Stage();
            stage.setTitle("Competitions");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'afficher les competitions: " + e.getMessage());
        }
    }

    private void loadQuizzes() {
        quizzesContainer.getChildren().clear();
        List<Quiz> quizzes = new ArrayList<>();
        try (Connection conn = dataSource.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM Quiz")) {
            while (rs.next()) {
                Quiz quiz = new Quiz(
                        rs.getInt("id"),
                        rs.getString("titre"),
                        rs.getString("description"),
                        rs.getTimestamp("date_creation").toLocalDateTime(),
                        rs.getTimestamp("datedebut").toLocalDateTime(),
                        rs.getTimestamp("datefin").toLocalDateTime(),
                        rs.getString("statut")
                );
                quizzes.add(quiz);
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les quiz: " + e.getMessage());
        }

        for (Quiz quiz : quizzes) {
            List<QuestionQuiz> questions = loadQuestionsForQuiz(quiz.getId());
            VBox quizBlock = createQuizBlock(quiz, questions);
            quizzesContainer.getChildren().add(quizBlock);
        }
    }

    private List<QuestionQuiz> loadQuestionsForQuiz(int quizId) {
        List<QuestionQuiz> questions = new ArrayList<>();
        try (Connection conn = dataSource.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM QuestionQuiz WHERE id_quiz = ?")) {
            stmt.setInt(1, quizId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                QuestionQuiz question = new QuestionQuiz(
                        rs.getInt("id"),
                        rs.getInt("id_quiz"),
                        rs.getString("question"),
                        rs.getString("option_1"),
                        rs.getString("option_2"),
                        rs.getString("option_3"),
                        rs.getString("option_4"),
                        rs.getInt("bonne_reponse"),
                        rs.getString("explication"),
                        rs.getTimestamp("date_creation") != null ? rs.getTimestamp("date_creation").toLocalDateTime() : null,
                        rs.getString("type_question")
                );
                questions.add(question);
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les questions: " + e.getMessage());
        }
        return questions;
    }

    private VBox createQuizBlock(Quiz quiz, List<QuestionQuiz> questions) {
        VBox quizBlock = new VBox(10);
        quizBlock.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #ddd; -fx-border-width: 1; -fx-border-radius: 5;");
        quizBlock.setPadding(new Insets(15));

        Label titleLabel = new Label(quiz.getTitre());
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Label dateLabel = new Label("Du " + quiz.getDateDebut().toLocalDate() + " au " + quiz.getDateFin().toLocalDate());
        dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #888;");

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

        Button startButton = new Button("Commencer");
        startButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        startButton.setOnAction(event -> startQuiz(quiz, questions));
        if (!"Actif".equalsIgnoreCase(statut)) {
            startButton.setDisable(true);
        }

        quizBlock.getChildren().addAll(titleLabel, dateLabel, statutLabel, startButton);
        return quizBlock;
    }

    private void startQuiz(Quiz quiz, List<QuestionQuiz> questions) {
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Veuillez vous connecter pour passer le quiz");
            return;
        }
        
        try {
            QuizPassageController.openQuiz(quiz, questions, currentUser);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le quiz: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Suppression d'un quiz et de ses questions liées
    public void deleteQuizWithQuestions(int quizId) {
        try (Connection conn = dataSource.getInstance().getConnection()) {
            // 1. Supprimer les questions liées
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM QuestionQuiz WHERE id_quiz = ?")) {
                stmt.setInt(1, quizId);
                stmt.executeUpdate();
            }
            // 2. Supprimer le quiz
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM Quiz WHERE id = ?")) {
                stmt.setInt(1, quizId);
                stmt.executeUpdate();
            }
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Quiz supprimé avec succès");
            loadQuizzes();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de supprimer le quiz: " + e.getMessage());
        }
    }
} 