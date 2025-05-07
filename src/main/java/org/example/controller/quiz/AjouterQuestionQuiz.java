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
import java.util.HashMap;
import java.util.Map;

public class AjouterQuestionQuiz {
    @FXML
    private ComboBox<String> quizComboBox;
    @FXML
    private TextField questionField;
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
    private Map<String, Integer> quizMap = new HashMap<>();

    @FXML
    private void initialize() {
        loadQuizzes();
        typeQuestionComboBox.getItems().addAll("QCM", "Vrai/Faux");
        typeQuestionComboBox.setValue("QCM");
        bonneReponseComboBox.getItems().addAll("Option 1", "Option 2", "Option 3", "Option 4");
        bonneReponseComboBox.setValue("Option 1");
    }

    private void loadQuizzes() {
        try (Connection conn = dataSource.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT id, titre FROM quiz");
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String titre = rs.getString("titre");
                int id = rs.getInt("id");
                quizComboBox.getItems().add(titre);
                quizMap.put(titre, id);
            }

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les quiz: " + e.getMessage());
        }
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    @FXML
    private void handleSave() {
        System.out.println("handleSave appelé");
        
        if (isInputValid()) {
            System.out.println("Validation réussie");
            try (Connection conn = dataSource.getInstance().getConnection()) {
                System.out.println("Connexion à la base de données établie");
                
                String query = "INSERT INTO QuestionQuiz (id_quiz, question, option_1, option_2, option_3, option_4, bonne_reponse, type_question) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                System.out.println("Requête SQL : " + query);
                
                PreparedStatement stmt = conn.prepareStatement(query);
                
                System.out.println("Quiz ID: " + quizMap.get(quizComboBox.getValue()));
                System.out.println("Question: " + questionField.getText());
                
                stmt.setInt(1, quizMap.get(quizComboBox.getValue()));
                stmt.setString(2, questionField.getText());
                stmt.setString(3, option1Field.getText());
                stmt.setString(4, option2Field.getText());
                stmt.setString(5, option3Field.getText());
                stmt.setString(6, option4Field.getText());
                
                String bonneReponseText = bonneReponseComboBox.getValue();
                System.out.println("Bonne réponse sélectionnée: " + bonneReponseText);
                int bonneReponse = Integer.parseInt(bonneReponseText.replace("Option ", ""));
                System.out.println("Bonne réponse (numéro): " + bonneReponse);
                
                stmt.setInt(7, bonneReponse);
                stmt.setString(8, typeQuestionComboBox.getValue());

                int rowsInserted = stmt.executeUpdate();
                if (rowsInserted > 0) {
                    System.out.println("Question ajoutée avec succès");
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Question ajoutée avec succès");
                    dialogStage.close();
                } else {
                    System.out.println("Aucune ligne insérée");
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de l'insertion dans la base de données");
                }
            } catch (SQLException e) {
                System.out.println("Erreur SQL: " + e.getMessage());
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ajouter la question: " + e.getMessage());
            }
        } else {
            System.out.println("Validation échouée");
        }
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    private boolean isInputValid() {
        String errorMessage = "";

        if (quizComboBox.getValue() == null) {
            errorMessage += "Veuillez sélectionner un quiz!\n";
        }
        if (questionField.getText() == null || questionField.getText().length() == 0) {
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