package org.example.controller.admin.quiz;

import org.example.model.quiz.QuestionQuiz;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import utils.dataSource;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

public class AfficherQuestionQuiz {
    @FXML
    private TableView<QuestionQuiz> questionTableView;
    @FXML
    private TableColumn<QuestionQuiz, Integer> idColumn;
    @FXML
    private TableColumn<QuestionQuiz, Integer> idQuizColumn;
    @FXML
    private TableColumn<QuestionQuiz, String> questionColumn;
    @FXML
    private TableColumn<QuestionQuiz, String> option1Column;
    @FXML
    private TableColumn<QuestionQuiz, String> option2Column;
    @FXML
    private TableColumn<QuestionQuiz, String> option3Column;
    @FXML
    private TableColumn<QuestionQuiz, String> option4Column;
    @FXML
    private TableColumn<QuestionQuiz, Integer> bonneReponseColumn;
    @FXML
    private TableColumn<QuestionQuiz, String> typeQuestionColumn;
    @FXML
    private TextField searchField;
    @FXML
    private Button refreshButton;
    @FXML
    private Button addButton;
    @FXML
    private Button modifyButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button returnButton;

    private ObservableList<QuestionQuiz> questionList = FXCollections.observableArrayList();
    private FilteredList<QuestionQuiz> filteredData;

    @FXML
    private void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        idQuizColumn.setCellValueFactory(new PropertyValueFactory<>("idQuiz"));
        questionColumn.setCellValueFactory(new PropertyValueFactory<>("question"));
        option1Column.setCellValueFactory(new PropertyValueFactory<>("option1"));
        option2Column.setCellValueFactory(new PropertyValueFactory<>("option2"));
        option3Column.setCellValueFactory(new PropertyValueFactory<>("option3"));
        option4Column.setCellValueFactory(new PropertyValueFactory<>("option4"));
        bonneReponseColumn.setCellValueFactory(new PropertyValueFactory<>("bonneReponse"));
        typeQuestionColumn.setCellValueFactory(new PropertyValueFactory<>("typeQuestion"));

        loadQuestionData();
        setupSearch();

        refreshButton.setOnAction(event -> loadQuestionData());
        addButton.setOnAction(event -> handleAdd());
        modifyButton.setOnAction(event -> handleModify());
        deleteButton.setOnAction(event -> handleDelete());
        returnButton.setOnAction(event -> handleReturn());
    }

    private void setupSearch() {
        // Initialiser FilteredList
        filteredData = new FilteredList<>(questionList, p -> true);

        // Ajouter un listener au champ de recherche
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(question -> {
                // Si le champ de recherche est vide, afficher toutes les questions
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                String lowerCaseFilter = newValue.toLowerCase().trim();

                // Vérifier si la question contient le texte de recherche
                if (question.getQuestion() != null && 
                    question.getQuestion().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                
                // Vérifier si le type de question contient le texte de recherche
                if (question.getTypeQuestion() != null && 
                    question.getTypeQuestion().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                
                // Vérifier si l'ID du quiz correspond
                if (String.valueOf(question.getIdQuiz()).contains(lowerCaseFilter)) {
                    return true;
                }
                
                // Vérifier les options
                if (question.getOption1() != null && 
                    question.getOption1().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                if (question.getOption2() != null && 
                    question.getOption2().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                if (question.getOption3() != null && 
                    question.getOption3().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                if (question.getOption4() != null && 
                    question.getOption4().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                
                return false;
            });
        });

        // Créer un SortedList pour gérer le tri
        SortedList<QuestionQuiz> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(questionTableView.comparatorProperty());
        questionTableView.setItems(sortedData);
    }

    @FXML
    private void loadQuestionData() {
        questionList.clear();

        try (Connection conn = dataSource.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM questionquiz ORDER BY id DESC")) {

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
                    rs.getTimestamp("date_creation") != null ? rs.getTimestamp("date_creation").toLocalDateTime() : LocalDateTime.now(),
                    rs.getString("type_question")
                );
                questionList.add(question);
            }

            // Réinitialiser la recherche si le FilteredList existe déjà
            if (filteredData != null) {
                searchField.clear();
                setupSearch();
            }

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur de base de données",
                    "Impossible de charger les questions: " + e.getMessage());
        }
    }

    @FXML
    private void handleAdd() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/quiz/AjouterQuestionQuiz.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Ajouter une Question");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadQuestionData();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir la fenêtre d'ajout: " + e.getMessage());
        }
    }

    @FXML
    private void handleReturn() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Menu/menu.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            Stage stage = (Stage) returnButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Esprit Admin Panel");
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger le menu: " + e.getMessage());
        }
    }

    @FXML
    private void handleModify() {
        QuestionQuiz selectedQuestion = questionTableView.getSelectionModel().getSelectedItem();

        if (selectedQuestion == null) {
            showAlert(Alert.AlertType.WARNING, "Avertissement", "Veuillez sélectionner une question à modifier");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/views/admin/quiz/ModifierQuestionQuiz.fxml"));
            Parent root = loader.load();
            ModifierQuestionQuiz controller = loader.getController();
            controller.setQuestionData(
                selectedQuestion.getId(),
                selectedQuestion.getIdQuiz(),
                selectedQuestion.getQuestion(),
                selectedQuestion.getOption1(),
                selectedQuestion.getOption2(),
                selectedQuestion.getOption3(),
                selectedQuestion.getOption4(),
                selectedQuestion.getBonneReponse(),
                selectedQuestion.getExplication(),
                selectedQuestion.getDateCreation(),
                selectedQuestion.getTypeQuestion()
            );
            Stage stage = new Stage();
            controller.setDialogStage(stage);
            stage.setTitle("Modifier la Question");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadQuestionData();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir la fenêtre de modification: " + e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        QuestionQuiz selectedQuestion = questionTableView.getSelectionModel().getSelectedItem();

        if (selectedQuestion == null) {
            showAlert(Alert.AlertType.WARNING, "Avertissement", "Veuillez sélectionner une question à supprimer");
            return;
        }

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirmation de suppression");
        confirmDialog.setHeaderText("Supprimer la question");
        confirmDialog.setContentText("Êtes-vous sûr de vouloir supprimer cette question ?");

        Optional<ButtonType> result = confirmDialog.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try (Connection conn = dataSource.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM questionquiz WHERE id = ?")) {

                stmt.setInt(1, selectedQuestion.getId());
                int rowsDeleted = stmt.executeUpdate();

                if (rowsDeleted > 0) {
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Question supprimée avec succès");
                    loadQuestionData();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de supprimer la question.");
                }

            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur de base de données", "Impossible de supprimer la question: " + e.getMessage());
            }
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