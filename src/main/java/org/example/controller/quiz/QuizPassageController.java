package org.example.controller.quiz;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.example.model.quiz.QuestionQuiz;
import org.example.model.quiz.Quiz;
import utils.dataSource;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import javafx.stage.StageStyle;
import javafx.animation.PauseTransition;
import org.json.JSONObject;
import java.io.IOException;
import java.net.URL;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.example.models.quiz.emailing;
import org.example.models.user.User;

public class QuizPassageController {
    private emailing emailSender;
    @FXML
    private VBox root;
    @FXML
    private Label quizTitle;
    @FXML
    private Label timerLabel;
    @FXML
    private VBox questionsContainer;
    @FXML
    private VBox resultsContainer;
    @FXML
    private Label scoreLabel;
    @FXML
    private Label percentageLabel;
    @FXML
    private TextField emailField;
    @FXML
    private Button sendEmailButton;
    @FXML
    private Button submitButton;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label progressLabel;
    @FXML
    private VBox emailContainer;

    private Quiz quiz;
    private Timeline timer;
    private int timeRemaining;
    private Map<QuestionQuiz, String> userAnswers = new HashMap<>();
    private User currentUser;
    private double lastScore;

    public static void openQuiz(Quiz quiz, List<QuestionQuiz> questions, User user) {
        try {
            // Charger le FXML avec le chemin absolu
            String fxmlPath = "/org/example/quiz/QuizPassage.fxml";
            System.out.println("Tentative de chargement du FXML: " + fxmlPath);
            
            FXMLLoader loader = new FXMLLoader(QuizPassageController.class.getResource(fxmlPath));
            if (loader.getLocation() == null) {
                // Si le premier chemin échoue, essayer un autre chemin
                fxmlPath = "/quiz/QuizPassage.fxml";
                System.out.println("Premier chemin échoué, tentative avec: " + fxmlPath);
                loader = new FXMLLoader(QuizPassageController.class.getResource(fxmlPath));
            }
            
            if (loader.getLocation() == null) {
                throw new IOException("Impossible de trouver le fichier FXML. Chemins essayés: /org/example/quiz/QuizPassage.fxml et /quiz/QuizPassage.fxml");
            }

            System.out.println("FXML trouvé à: " + loader.getLocation());
            Parent root = loader.load();
            
            QuizPassageController controller = loader.getController();
            if (controller == null) {
                throw new IOException("Le contrôleur n'a pas été correctement initialisé");
            }
            
            Stage stage = new Stage();
            stage.setTitle("Quiz: " + quiz.getTitre());
            Scene scene = new Scene(root);
            stage.setScene(scene);
            
            // Initialiser le quiz après que la scène est créée
            controller.initializeQuiz(quiz, questions, user);
            
            stage.show();
        } catch (Exception e) {
            System.err.println("Erreur détaillée lors du chargement du quiz:");
            e.printStackTrace();
            
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Impossible d'ouvrir le quiz");
            alert.setContentText("Une erreur est survenue lors du chargement du quiz: " + e.getMessage() + 
                               "\nVérifiez les logs pour plus de détails.");
            alert.showAndWait();
        }
    }

    @FXML
    public void initialize() {
        System.out.println("Initialisation du contrôleur QuizPassage");
        System.out.println("État des composants FXML:");
        System.out.println("- quizTitle: " + (quizTitle != null ? "OK" : "null"));
        System.out.println("- timerLabel: " + (timerLabel != null ? "OK" : "null"));
        System.out.println("- questionsContainer: " + (questionsContainer != null ? "OK" : "null"));
        System.out.println("- emailField: " + (emailField != null ? "OK" : "null"));
        System.out.println("- sendEmailButton: " + (sendEmailButton != null ? "OK" : "null"));
        emailSender = new emailing();
        // Désactiver le champ email et le bouton d'envoi jusqu'à la soumission
        if (emailField != null) {
            emailField.setDisable(true);
        }
        if (sendEmailButton != null) {
            sendEmailButton.setDisable(true);
        }
        
        // Ajouter un écouteur pour valider l'email
        if (emailField != null) {
            emailField.textProperty().addListener((obs, oldVal, newVal) -> {
                boolean isValidEmail = newVal != null && newVal.matches("^[A-Za-z0-9+_.-]+@(.+)$");
                if (sendEmailButton != null) {
                    sendEmailButton.setDisable(!isValidEmail);
                }
            });
        }
    }

    public void initializeQuiz(Quiz quiz, List<QuestionQuiz> questions, User user) {
        if (quiz == null) {
            throw new IllegalArgumentException("Le quiz ne peut pas être null");
        }
        if (questions == null) {
            throw new IllegalArgumentException("Les questions ne peuvent pas être null");
        }
        if (user == null) {
            throw new IllegalArgumentException("L'utilisateur ne peut pas être null");
        }
        
        this.quiz = quiz;
        this.currentUser = user;
        quiz.setQuestions(questions);
        
        // S'assurer que tous les composants FXML sont initialisés
        if (quizTitle == null || questionsContainer == null || timerLabel == null) {
            throw new IllegalStateException("Les composants FXML n'ont pas été correctement injectés");
        }
        
        // Initialiser l'interface
        quizTitle.setText(quiz.getTitre());
        displayQuestions();
        startTimer();
    }

    private void displayQuestions() {
        if (questionsContainer == null) {
            System.err.println("Erreur: questionsContainer est null");
            return;
        }
        if (quiz == null || quiz.getQuestions() == null) {
            System.err.println("Erreur: Le quiz ou les questions sont null");
            return;
        }
        questionsContainer.getChildren().clear();
        int questionNumber = 1;
        for (QuestionQuiz question : quiz.getQuestions()) {
            VBox questionBox = new VBox(10);
            questionBox.getStyleClass().add("question-box");
            
            // Numéro et texte de la question
            Label questionNumberLabel = new Label("Question " + questionNumber + ":");
            questionNumberLabel.getStyleClass().add("question-number");
            Label questionLabel = new Label(question.getQuestion());
            questionLabel.getStyleClass().add("question-text");
            questionLabel.setWrapText(true);
            
            questionBox.getChildren().addAll(questionNumberLabel, questionLabel);

            if (question.getType().equals("QCM")) {
                ToggleGroup group = new ToggleGroup();
                VBox optionsBox = new VBox(8);
                optionsBox.getStyleClass().add("options-box");
                
                for (String option : question.getOptions()) {
                    RadioButton radioButton = new RadioButton(option);
                    radioButton.setToggleGroup(group);
                    radioButton.setWrapText(true);
                    radioButton.setOnAction(e -> {
                        userAnswers.put(question, option);
                        updateProgress();
                    });
                    optionsBox.getChildren().add(radioButton);
                }
                questionBox.getChildren().add(optionsBox);
            } else {
                TextField answerField = new TextField();
                answerField.getStyleClass().add("answer-field");
                answerField.setPromptText("Votre réponse ici");
                answerField.textProperty().addListener((obs, oldVal, newVal) -> {
                    userAnswers.put(question, newVal);
                    updateProgress();
                });
                questionBox.getChildren().add(answerField);
            }

            // Conteneur pour le feedback
            HBox feedbackBox = new HBox(10);
            feedbackBox.getStyleClass().add("feedback-box");
            feedbackBox.setVisible(false);
            questionBox.getChildren().add(feedbackBox);

            // Animation de fade in
            questionBox.setOpacity(0);
            PauseTransition pause = new PauseTransition(Duration.millis(100 * questionNumber));
            pause.setOnFinished(e -> {
                javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(Duration.millis(500), questionBox);
                ft.setFromValue(0);
                ft.setToValue(1);
                ft.play();
            });
            pause.play();

            questionsContainer.getChildren().add(questionBox);
            questionNumber++;
        }
        updateProgress();
    }

    private void updateProgress() {
        int totalQuestions = quiz.getQuestions().size();
        int answeredQuestions = userAnswers.size();
        double progress = (double) answeredQuestions / totalQuestions;
        
        progressBar.setProgress(progress);
        progressLabel.setText(String.format("Question %d/%d", answeredQuestions, totalQuestions));
        
        // Animation de la barre de progression
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
            new KeyFrame(Duration.ZERO, new javafx.animation.KeyValue(progressBar.progressProperty(), progressBar.getProgress())),
            new KeyFrame(Duration.millis(250), new javafx.animation.KeyValue(progressBar.progressProperty(), progress))
        );
        timeline.play();
    }

    private void showFeedback(VBox questionBox, boolean isCorrect, String correctAnswer) {
        HBox feedbackBox = (HBox) questionBox.getChildren().get(questionBox.getChildren().size() - 1);
        feedbackBox.getChildren().clear();
        
        Label iconLabel = new Label(isCorrect ? "✓" : "✗");
        iconLabel.getStyleClass().add(isCorrect ? "correct-icon" : "incorrect-icon");
        
        Label messageLabel = new Label(isCorrect ? "Correct !" : "Incorrect. La bonne réponse était : " + correctAnswer);
        messageLabel.getStyleClass().add(isCorrect ? "correct-message" : "incorrect-message");
        
        feedbackBox.getChildren().addAll(iconLabel, messageLabel);
        feedbackBox.setVisible(true);
        
        // Animation du feedback
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(Duration.millis(300), feedbackBox);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private void startTimer() {
        timeRemaining = 60; // 60 secondes par défaut
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            timeRemaining--;
            timerLabel.setText("Temps restant: " + timeRemaining + " secondes");
            
            // Ajouter un log pour le débogage
            System.out.println("Temps restant: " + timeRemaining);
            
            if (timeRemaining <= 0) {
                System.out.println("Temps écoulé - Arrêt du timer");
                timer.stop();
                
                // Utiliser Platform.runLater pour s'assurer que l'alerte s'affiche dans le thread JavaFX
                javafx.application.Platform.runLater(() -> {
                    System.out.println("Affichage de l'alerte");
                    showTimeoutAlert();
                });
            }
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
        System.out.println("Timer démarré");
    }

    private void showTimeoutAlert() {
        try {
            System.out.println("Création de l'alerte");
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Temps écoulé");
            alert.setHeaderText(null);
            alert.setContentText("Le temps est écoulé. Veuillez réessayer plus tard.");
            
            // Ajouter un bouton personnalisé
            ButtonType closeButton = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(closeButton);
            
            // Gérer la fermeture de la fenêtre
            alert.setOnCloseRequest(event -> {
                System.out.println("Fermeture de la fenêtre du quiz");
                Stage stage = (Stage) root.getScene().getWindow();
                stage.close();
            });
            
            // Afficher l'alerte et attendre la réponse
            System.out.println("Affichage de l'alerte");
            alert.showAndWait();
            
            // Fermer la fenêtre du quiz
            System.out.println("Fermeture de la fenêtre du quiz");
            Stage stage = (Stage) root.getScene().getWindow();
            stage.close();
        } catch (Exception e) {
            System.err.println("Erreur lors de l'affichage de l'alerte: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSubmit() {
        if (timer != null) {
            timer.stop();
        }
        
        // Vérifier si toutes les questions ont une réponse
        boolean allQuestionsAnswered = quiz.getQuestions().stream()
                .allMatch(q -> userAnswers.containsKey(q));
                
        if (!allQuestionsAnswered) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Questions non répondues");
            alert.setHeaderText("Certaines questions n'ont pas de réponse");
            alert.setContentText("Voulez-vous vraiment soumettre le quiz ?");
            
            if (alert.showAndWait().get() != ButtonType.OK) {
                return;
            }
        }

        // Désactiver les contrôles
        questionsContainer.getChildren().forEach(node -> {
            if (node instanceof VBox) {
                node.setDisable(true);
            }
        });
        submitButton.setDisable(true);

        // Afficher les corrections avec animation
        PauseTransition initialPause = new PauseTransition(Duration.millis(500));
        initialPause.setOnFinished(e -> {
            int questionIndex = 0;
            for (QuestionQuiz question : quiz.getQuestions()) {
                VBox questionBox = (VBox) questionsContainer.getChildren().get(questionIndex);
                String userAnswer = userAnswers.getOrDefault(question, "");
                boolean isCorrect = question.getReponseCorrecte().equals(userAnswer);
                
                PauseTransition feedbackPause = new PauseTransition(Duration.millis(500 * questionIndex));
                feedbackPause.setOnFinished(f -> showFeedback(questionBox, isCorrect, question.getReponseCorrecte()));
                feedbackPause.play();
                
                questionIndex++;
            }
        });
        initialPause.play();

        calculateScore();
        
        // Activer le champ email après la soumission avec animation
        PauseTransition emailPause = new PauseTransition(Duration.seconds(2));
        emailPause.setOnFinished(e -> {
            emailField.setDisable(false);
            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(Duration.millis(500), emailContainer);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        });
        emailPause.play();
    }

    private void calculateScore() {
        int score = 0;
        for (Map.Entry<QuestionQuiz, String> entry : userAnswers.entrySet()) {
            if (entry.getKey().getReponseCorrecte().equals(entry.getValue())) {
                score++;
            }
        }
        lastScore = (double) score / quiz.getQuestions().size() * 100;
        showResult(score, lastScore);
        saveQuizResult(score, lastScore);
    }

    private void saveQuizResult(int score, double percentage) {
        Connection conn = null;
        try {
            conn = dataSource.getInstance().getConnection();
            
            // Sauvegarder chaque réponse individuellement
            String query = "INSERT INTO reponsequiz (id_utilisateur, id_question, reponse_choisie, date_reponse, points) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement pst = conn.prepareStatement(query);
            
            LocalDateTime now = LocalDateTime.now();
            
            for (Map.Entry<QuestionQuiz, String> entry : userAnswers.entrySet()) {
                QuestionQuiz question = entry.getKey();
                String userAnswer = entry.getValue();
                
                // Truncate the answer if it's too long - assuming VARCHAR(255) in database
                if (userAnswer != null && userAnswer.length() > 250) {
                    userAnswer = userAnswer.substring(0, 250);
                    System.out.println("Réponse tronquée pour la question " + question.getId() + " car trop longue");
                }
                
                boolean isCorrect = question.getReponseCorrecte().equals(entry.getValue()); // Compare with original answer
                
                pst.setInt(1, currentUser.getId()); // Utiliser l'ID de l'utilisateur connecté
                pst.setInt(2, question.getId());
                pst.setString(3, userAnswer != null ? userAnswer : ""); // Ensure no null values are inserted
                pst.setTimestamp(4, java.sql.Timestamp.valueOf(now));
                pst.setInt(5, isCorrect ? 1 : 0); // 1 point si correct, 0 sinon
                
                try {
                    pst.executeUpdate();
                } catch (SQLException e) {
                    System.err.println("Erreur lors de l'insertion de la réponse pour la question " + question.getId() + ": " + e.getMessage());
                    System.err.println("Réponse problématique: '" + userAnswer + "', longueur: " + (userAnswer != null ? userAnswer.length() : 0));
                    // Continue with next answer
                }
            }
            
            // Sauvegarder le score total du quiz dans quiz_history
            String quizHistoryQuery = "INSERT INTO quiz_history (id_utilisateur, id_quiz, score, total_questions, date_passage) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement quizHistoryStmt = conn.prepareStatement(quizHistoryQuery);
            
            quizHistoryStmt.setInt(1, currentUser.getId());
            quizHistoryStmt.setInt(2, quiz.getId());
            quizHistoryStmt.setInt(3, score);
            quizHistoryStmt.setInt(4, quiz.getQuestions().size());
            quizHistoryStmt.setTimestamp(5, java.sql.Timestamp.valueOf(now));
            
            quizHistoryStmt.executeUpdate();
            
            showAlert("Succès", "Vos réponses ont été enregistrées avec succès!");
            
        } catch (SQLException e) {
            System.err.println("Erreur SQL lors de la sauvegarde des réponses: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Impossible de sauvegarder les réponses: " + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    dataSource.getInstance().releaseConnection(conn);
                } catch (Exception e) {
                    System.err.println("Erreur lors de la libération de la connexion: " + e.getMessage());
                }
            }
        }
    }

    private void showResult(int score, double percentage) {
        resultsContainer.setVisible(true);
        scoreLabel.setText("Score: " + score + " / " + quiz.getQuestions().size());
        percentageLabel.setText("Pourcentage: " + String.format("%.2f%%", percentage));

        // Créer une alerte détaillée avec les résultats
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Résultats du Quiz");
        alert.setHeaderText("Votre score");
        
        // Construire le contenu détaillé
        StringBuilder content = new StringBuilder();
        content.append(String.format("Vous avez obtenu %d bonnes réponses sur %d (%.2f%%)\n\n", 
            score, quiz.getQuestions().size(), percentage));
        
        content.append("Détail des réponses:\n");
        for (QuestionQuiz question : quiz.getQuestions()) {
            String userAnswer = userAnswers.getOrDefault(question, "Non répondu");
            content.append("\nQuestion: ").append(question.getQuestion()).append("\n");
            content.append("Votre réponse: ").append(userAnswer).append("\n");
            content.append("Réponse correcte: ").append(question.getReponseCorrecte()).append("\n");
        }
        
        alert.setContentText(content.toString());
        alert.showAndWait();
    }

    @FXML
    private void handleSendEmail() {
        String email = emailField.getText();
        if (email == null || email.trim().isEmpty()) {
            showAlert("Erreur", "Veuillez entrer une adresse email valide");
            return;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showAlert("Erreur", "Format d'email invalide");
            return;
        }

        String subject = "Résultats de votre Quiz: " + quiz.getTitre();
        String content = String.format("""
                    Résultats de votre quiz
                    
                    Score final: %.2f%%
                    
                    Détail de vos réponses:
                    %s
                    
                    Merci d'avoir participé au quiz!
                    Esprit E-Learning Platform Team
                    """, 
                    lastScore,
                    generateDetailedResults());

        emailSender.sendEmail(email, subject, content);
    }

    private String generateDetailedResults() {
        StringBuilder details = new StringBuilder();
        int questionNumber = 1;
        
        for (QuestionQuiz question : quiz.getQuestions()) {
            String userAnswer = userAnswers.getOrDefault(question, "Non répondu");
            boolean isCorrect = question.getReponseCorrecte().equals(userAnswer);
            
            details.append(String.format("""
                    
                    Question %d: %s
                    Votre réponse: %s
                    Réponse correcte: %s
                    Résultat: %s
                    """,
                    questionNumber,
                    question.getQuestion(),
                    userAnswer,
                    question.getReponseCorrecte(),
                    isCorrect ? "✓ Correct" : "✗ Incorrect"
            ));
            
            questionNumber++;
        }
        
        return details.toString();
    }

    private void sendResultsByEmail(String email) {
        try {
            if (email == null || email.trim().isEmpty()) {
                throw new IllegalArgumentException("L'adresse email ne peut pas être vide");
            }

            // Préparer les données pour l'email
            JSONObject emailData = new JSONObject();
            emailData.put("email", email);
            
            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append("Résultats de votre quiz:\n\n");
            messageBuilder.append(String.format("Score: %.2f%%\n\n", lastScore));
            messageBuilder.append("Détail des réponses:\n");
            
            for (QuestionQuiz question : quiz.getQuestions()) {
                String userAnswer = userAnswers.getOrDefault(question, "Non répondu");
                messageBuilder.append("\nQuestion: ").append(question.getQuestion())
                            .append("\nVotre réponse: ").append(userAnswer)
                            .append("\nRéponse correcte: ").append(question.getReponseCorrecte())
                            .append("\n");
            }
            
            emailData.put("message", messageBuilder.toString());

            // Log des données avant l'envoi
            System.out.println("Tentative d'envoi d'email à: " + email);
            System.out.println("URL de l'API: http://localhost:5000/send-email");
            System.out.println("Données de l'email: " + emailData.toString());

            // Envoyer la requête
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:5000/send-email"))
                .header("Content-Type", "application/json")
                .timeout(java.time.Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(emailData.toString()))
                .build();

            System.out.println("Envoi de la requête...");
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response == null) {
                throw new IOException("La réponse du serveur est nulle");
            }
            
            System.out.println("Réponse reçue - Status code: " + response.statusCode());
            System.out.println("Corps de la réponse: " + response.body());
            
            if (response.statusCode() == 200) {
                showAlert("Succès", "Les résultats ont été envoyés à votre adresse email: " + email);
                emailField.setDisable(true);
                sendEmailButton.setDisable(true);
            } else {
                String errorMessage = "Échec de l'envoi de l'email. Code: " + response.statusCode();
                if (response.body() != null && !response.body().isEmpty()) {
                    errorMessage += "\nDétails: " + response.body();
                }
                throw new IOException(errorMessage);
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Erreur de validation: " + e.getMessage());
            showAlert("Erreur de validation", e.getMessage());
        } catch (IOException e) {
            System.err.println("Erreur d'envoi d'email: " + e.getMessage());
            showAlert("Erreur de connexion", "Impossible de se connecter au serveur d'email. Veuillez vérifier que le serveur Python est en cours d'exécution sur le port 5000.");
        } catch (InterruptedException e) {
            System.err.println("L'envoi d'email a été interrompu: " + e.getMessage());
            Thread.currentThread().interrupt();
            showAlert("Erreur", "L'envoi de l'email a été interrompu");
        } catch (Exception e) {
            System.err.println("Erreur inattendue lors de l'envoi de l'email: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Une erreur inattendue est survenue lors de l'envoi de l'email. Veuillez réessayer plus tard.");
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}