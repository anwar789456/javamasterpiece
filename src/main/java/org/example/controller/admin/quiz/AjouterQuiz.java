package org.example.controller.admin.quiz;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.model.quiz.Quiz;
import utils.dataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    @FXML
    private void initialize() {
        statutComboBox.getItems().setAll("Actif", "Inactif", "Terminé");
        statutComboBox.setValue("Actif");
    }

    @FXML
    private void handleSave() {
        if (!isInputValid()) {
            return;
        }

        try (Connection conn = dataSource.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO Quiz (titre, description, date_creation, datedebut, datefin, statut) " +
                     "VALUES (?, ?, ?, ?, ?, ?)")) {

            LocalDateTime now = LocalDateTime.now();
            stmt.setString(1, titreField.getText());
            stmt.setString(2, descriptionArea.getText());
            stmt.setTimestamp(3, java.sql.Timestamp.valueOf(now));
            stmt.setTimestamp(4, java.sql.Timestamp.valueOf(dateDebutPicker.getValue().atStartOfDay()));
            stmt.setTimestamp(5, java.sql.Timestamp.valueOf(dateFinPicker.getValue().atStartOfDay()));
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

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    private boolean isInputValid() {
        String errorMessage = "";

        if (titreField.getText() == null || titreField.getText().isEmpty()) {
            errorMessage += "Veuillez saisir un titre\n";
        }
        if (descriptionArea.getText() == null || descriptionArea.getText().isEmpty()) {
            errorMessage += "Veuillez saisir une description\n";
        }
        if (dateDebutPicker.getValue() == null) {
            errorMessage += "Veuillez sélectionner une date de début\n";
        }
        if (dateFinPicker.getValue() == null) {
            errorMessage += "Veuillez sélectionner une date de fin\n";
        }
        if (statutComboBox.getValue() == null) {
            errorMessage += "Veuillez sélectionner un statut\n";
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