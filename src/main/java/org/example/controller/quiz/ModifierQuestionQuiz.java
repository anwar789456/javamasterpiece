package org.example.controller.quiz;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.model.quiz.QuestionQuiz;
import utils.dataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ModifierQuestionQuiz {
    @FXML
    private TextField quizIdField;
    @FXML
    private TextArea questionTextArea;
    @FXML
    private TextField option1Field;
    @FXML
    private TextField option2Field;
    @FXML
    private TextField option3Field;
    @FXML
    private TextField option4Field;
    @FXML
    private ComboBox<String> bonneReponseComboBox;
    @FXML
    private ComboBox<String> typeQuestionComboBox;
    @FXML
    private Button saveButton;
    @FXML
    private Button cancelButton;

    private Stage dialogStage;
    private QuestionQuiz question;

    @FXML
    private void initialize() {
        typeQuestionComboBox.getItems().addAll("QCM", "Vrai/Faux");
        bonneReponseComboBox.getItems().addAll("Option 1", "Option 2", "Option 3", "Option 4");
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setQuestion(QuestionQuiz question) {
        this.question = question;
        quizIdField.setText(String.valueOf(question.getIdQuiz()));
        questionTextArea.setText(question.getQuestion());
        option1Field.setText(question.getOption1());
        option2Field.setText(question.getOption2());
        option3Field.setText(question.getOption3());
        option4Field.setText(question.getOption4());
        bonneReponseComboBox.setValue("Option " + question.getBonneReponse());
        typeQuestionComboBox.setValue(question.getTypeQuestion());
    }

    @FXML
    private void handleSave() {
        if (isInputValid()) {
            try (Connection conn = dataSource.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "UPDATE QuestionQuiz SET question = ?, option_1 = ?, option_2 = ?, option_3 = ?, option_4 = ?, bonne_reponse = ?, type_question = ? WHERE id = ?")) {

                stmt.setString(1, questionTextArea.getText());
                stmt.setString(2, option1Field.getText());
                stmt.setString(3, option2Field.getText());
                stmt.setString(4, option3Field.getText());
                stmt.setString(5, option4Field.getText());
                int bonneReponse = Integer.parseInt(bonneReponseComboBox.getValue().replace("Option ", ""));
                stmt.setInt(6, bonneReponse);
                stmt.setString(7, typeQuestionComboBox.getValue());
                stmt.setInt(8, question.getId());

                int rowsUpdated = stmt.executeUpdate();
                if (rowsUpdated > 0) {
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Question modifiée avec succès");
                    dialogStage.close();
                }
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de modifier la question: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    private boolean isInputValid() {
        String errorMessage = "";

        if (questionTextArea.getText() == null || questionTextArea.getText().length() == 0) {
            errorMessage += "La question est requise!\n";
        }
        if (option1Field.getText() == null || option1Field.getText().length() == 0) {
            errorMessage += "L'option 1 est requise!\n";
        }
        if (option2Field.getText() == null || option2Field.getText().length() == 0) {
            errorMessage += "L'option 2 est requise!\n";
        }
        if (option3Field.getText() == null || option3Field.getText().length() == 0) {
            errorMessage += "L'option 3 est requise!\n";
        }
        if (option4Field.getText() == null || option4Field.getText().length() == 0) {
            errorMessage += "L'option 4 est requise!\n";
        }
        if (bonneReponseComboBox.getValue() == null) {
            errorMessage += "Veuillez sélectionner la bonne réponse!\n";
        }
        if (typeQuestionComboBox.getValue() == null) {
            errorMessage += "Veuillez sélectionner le type de question!\n";
        }

        if (errorMessage.length() == 0) {
            return true;
        } else {
            showAlert(Alert.AlertType.ERROR, "Erreur de validation", errorMessage);
            return false;
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