package org.example.controller.admin.quiz;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import utils.dataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class ModifierQuestionQuiz {
    @FXML private ComboBox<Integer> quizComboBox;
    @FXML private TextArea questionTextArea;
    @FXML private TextField option1Field;
    @FXML private TextField option2Field;
    @FXML private TextField option3Field;
    @FXML private TextField option4Field;
    @FXML private ComboBox<Integer> bonneReponseComboBox;
    @FXML private TextArea explicationTextArea;
    @FXML private ComboBox<String> typeQuestionComboBox;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private Stage dialogStage;
    private int questionId;
    private int idQuiz;
    private String question;
    private String option1;
    private String option2;
    private String option3;
    private String option4;
    private int bonneReponse;
    private String explication;
    private LocalDateTime dateCreation;
    private String typeQuestion;

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setQuestionData(int id, int idQuiz, String question, String option1, String option2,
                              String option3, String option4, int bonneReponse, String explication,
                              LocalDateTime dateCreation, String typeQuestion) {
        this.questionId = id;
        this.idQuiz = idQuiz;
        this.question = question;
        this.option1 = option1;
        this.option2 = option2;
        this.option3 = option3;
        this.option4 = option4;
        this.bonneReponse = bonneReponse;
        this.explication = explication;
        this.dateCreation = dateCreation;
        this.typeQuestion = typeQuestion;

        // Initialize UI with existing data
        quizComboBox.setValue(idQuiz);
        questionTextArea.setText(question);
        option1Field.setText(option1);
        option2Field.setText(option2);
        option3Field.setText(option3);
        option4Field.setText(option4);
        bonneReponseComboBox.setValue(bonneReponse);
        explicationTextArea.setText(explication);
        typeQuestionComboBox.setValue(typeQuestion);
    }

    @FXML
    private void initialize() {
        // Initialize combo boxes
        typeQuestionComboBox.getItems().addAll("QCM", "Vrai/Faux");
        bonneReponseComboBox.getItems().addAll(1, 2, 3, 4);

        // Load quiz IDs
        try (Connection conn = dataSource.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT id FROM Quiz")) {
            var rs = stmt.executeQuery();
            while (rs.next()) {
                quizComboBox.getItems().add(rs.getInt("id"));
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les quiz: " + e.getMessage());
        }

        // Set button actions
        saveButton.setOnAction(event -> handleSave());
        cancelButton.setOnAction(event -> handleCancel());
    }

    @FXML
    private void handleSave() {
        if (!validateInput()) {
            return;
        }

        try (Connection conn = dataSource.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE questionquiz SET id_quiz = ?, question = ?, option_1 = ?, " +
                     "option_2 = ?, option_3 = ?, option_4 = ?, bonne_reponse = ?, " +
                     "explication = ?, type_question = ? WHERE id = ?")) {

            stmt.setInt(1, quizComboBox.getValue());
            stmt.setString(2, questionTextArea.getText());
            stmt.setString(3, option1Field.getText());
            stmt.setString(4, option2Field.getText());
            stmt.setString(5, option3Field.getText());
            stmt.setString(6, option4Field.getText());
            stmt.setInt(7, bonneReponseComboBox.getValue());
            stmt.setString(8, explicationTextArea.getText());
            stmt.setString(9, typeQuestionComboBox.getValue());
            stmt.setInt(10, questionId);

            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Question modifiée avec succès");
                dialogStage.close();
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de modifier la question");
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la modification: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    private boolean validateInput() {
        if (quizComboBox.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Veuillez sélectionner un quiz");
            return false;
        }
        if (questionTextArea.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Veuillez saisir la question");
            return false;
        }
        if (option1Field.getText().isEmpty() || option2Field.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Veuillez saisir au moins deux options");
            return false;
        }
        if (bonneReponseComboBox.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Veuillez sélectionner la bonne réponse");
            return false;
        }
        if (typeQuestionComboBox.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Veuillez sélectionner le type de question");
            return false;
        }
        return true;
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
} 