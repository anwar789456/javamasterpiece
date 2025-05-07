package org.example.controller.quiz;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.model.quiz.Quiz;
import utils.dataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class AjouterQuiz {
    @FXML
    private TextField titreField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private DatePicker dateDebutPicker;
    @FXML
    private DatePicker dateFinPicker;
    @FXML
    private ComboBox<String> statutComboBox;
    @FXML
    private Button saveButton;
    @FXML
    private Button cancelButton;

    private Stage dialogStage;

    @FXML
    private void initialize() {
        statutComboBox.getItems().setAll("Actif", "Inactif", "Terminé");
        statutComboBox.setValue("Actif");
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    @FXML
    private void handleSave() {
        if (isInputValid()) {
            try (Connection conn = dataSource.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "INSERT INTO Quiz (titre, description, date_creation, datedebut, datefin, statut) VALUES (?, ?, ?, ?, ?, ?)")) {

                stmt.setString(1, titreField.getText());
                stmt.setString(2, descriptionArea.getText());
                stmt.setTimestamp(3, java.sql.Timestamp.valueOf(LocalDateTime.now()));
                stmt.setTimestamp(4, java.sql.Timestamp.valueOf(LocalDateTime.of(dateDebutPicker.getValue(), LocalTime.MIDNIGHT)));
                stmt.setTimestamp(5, java.sql.Timestamp.valueOf(LocalDateTime.of(dateFinPicker.getValue(), LocalTime.MIDNIGHT)));
                stmt.setString(6, statutComboBox.getValue());

                int rowsInserted = stmt.executeUpdate();
                if (rowsInserted > 0) {
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Quiz ajouté avec succès");
                    dialogStage.close();
                }
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ajouter le quiz: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    private boolean isInputValid() {
        String errorMessage = "";

        if (titreField.getText() == null || titreField.getText().length() == 0) {
            errorMessage += "Le titre est requis!\n";
        }
        if (descriptionArea.getText() == null || descriptionArea.getText().length() == 0) {
            errorMessage += "La description est requise!\n";
        }
        if (dateDebutPicker.getValue() == null) {
            errorMessage += "La date de début est requise!\n";
        }
        if (dateFinPicker.getValue() == null) {
            errorMessage += "La date de fin est requise!\n";
        }
        if (dateDebutPicker.getValue() != null && dateFinPicker.getValue() != null &&
                dateDebutPicker.getValue().isAfter(dateFinPicker.getValue())) {
            errorMessage += "La date de début doit être antérieure à la date de fin!\n";
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