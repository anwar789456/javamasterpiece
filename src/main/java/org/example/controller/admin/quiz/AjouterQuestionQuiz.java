package org.example.controller.admin.quiz;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
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
    private TextArea questionArea;
    @FXML
    private TextField option1Field;
    @FXML
    private TextField option2Field;
    @FXML
    private TextField option3Field;
    @FXML
    private TextField option4Field;
    @FXML
    private TextField reponseCorrecteField;
    @FXML
    private Button saveButton;
    @FXML
    private Button cancelButton;

    private Map<String, Integer> quizMap = new HashMap<>();

    @FXML
    private void initialize() {
        loadQuizzes();
    }

    private void loadQuizzes() {
        try (Connection conn = dataSource.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT id, titre FROM quiz");
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String titre = rs.getString("titre");
                quizMap.put(titre, id);
                quizComboBox.getItems().add(titre);
            }

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les quizzes: " + e.getMessage());
        }
    }

    @FXML
    private void handleSave() {
        if (isInputValid()) {
            try {
                Connection conn = dataSource.getInstance().getConnection();
                String query = "INSERT INTO questionquiz (id_quiz, question, option_1, option_2, option_3, option_4, " +
                        "bonne_reponse, explication, date_creation, type_question) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), ?)";

                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setInt(1, quizMap.get(quizComboBox.getValue()));
                stmt.setString(2, questionArea.getText());
                stmt.setString(3, option1Field.getText());
                stmt.setString(4, option2Field.getText());
                stmt.setString(5, option3Field.getText());
                stmt.setString(6, option4Field.getText());
                stmt.setInt(7, Integer.parseInt(reponseCorrecteField.getText()));
                stmt.setString(8, ""); // explication vide par défaut
                stmt.setString(9, "Choix multiple"); // type de question par défaut

                int rowsInserted = stmt.executeUpdate();
                if (rowsInserted > 0) {
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Question ajoutée avec succès");
                    Stage stage = (Stage) saveButton.getScene().getWindow();
                    stage.close();
                }

                stmt.close();
                conn.close();

            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ajouter la question: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleCancel() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    private boolean isInputValid() {
        String errorMessage = "";

        if (quizComboBox.getValue() == null) {
            errorMessage += "Veuillez sélectionner un quiz!\n";
        }

        if (questionArea.getText() == null || questionArea.getText().isEmpty()) {
            errorMessage += "Question invalide!\n";
        }

        if (option1Field.getText() == null || option1Field.getText().isEmpty()) {
            errorMessage += "Option 1 invalide!\n";
        }

        if (option2Field.getText() == null || option2Field.getText().isEmpty()) {
            errorMessage += "Option 2 invalide!\n";
        }

        if (option3Field.getText() == null || option3Field.getText().isEmpty()) {
            errorMessage += "Option 3 invalide!\n";
        }

        if (option4Field.getText() == null || option4Field.getText().isEmpty()) {
            errorMessage += "Option 4 invalide!\n";
        }

        if (reponseCorrecteField.getText() == null || reponseCorrecteField.getText().isEmpty()) {
            errorMessage += "Réponse correcte invalide!\n";
        }

        if (errorMessage.isEmpty()) {
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