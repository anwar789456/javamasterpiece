package org.example.controller.projet;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import utils.dataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AjouterProjetController {

    @FXML
    private TextField tfOwnerId;

    @FXML
    private TextField tfProjectName;

    @FXML
    private TextArea taDescription;

    @FXML
    private DatePicker dpStartDate;

    @FXML
    private DatePicker dpEndDate;

    @FXML
    private ComboBox<String> cbStatus;

    @FXML
    private Button btnAdd;

    @FXML
    private Button btnCancel;

    @FXML
    private void initialize() {
        // Set up status options
        cbStatus.getItems().addAll("Active", "Completed", "On Hold", "Cancelled");
        cbStatus.setValue("Active"); // Set default value

        // Add numeric validation for owner ID
        tfOwnerId.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                tfOwnerId.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
    }

    @FXML
    public void addProject() {
        // Validate inputs
        if (!validateInputs()) {
            return;
        }

        try {
            Connection conn = dataSource.getInstance().getConnection();
            String query = "INSERT INTO projet (owner_id, project_name, description, " +
                          "start_date, end_date, status) VALUES (?, ?, ?, ?, ?, ?)";
            
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, Integer.parseInt(tfOwnerId.getText().trim()));
            stmt.setString(2, tfProjectName.getText().trim());
            stmt.setString(3, taDescription.getText().trim());
            stmt.setDate(4, java.sql.Date.valueOf(dpStartDate.getValue()));
            stmt.setDate(5, java.sql.Date.valueOf(dpEndDate.getValue()));
            stmt.setString(6, cbStatus.getValue());

            int rowsInserted = stmt.executeUpdate();
            if (rowsInserted > 0) {
                showSuccessAlert("Le projet a été ajouté avec succès!");
                closeWindow();
            } else {
                showErrorAlert("Échec de l'ajout du projet.");
            }

        } catch (SQLException e) {
            if (e.getMessage().contains("foreign key constraint")) {
                showErrorAlert("L'ID du propriétaire spécifié n'existe pas dans la base de données.");
            } else {
                showErrorAlert("Erreur de base de données: " + e.getMessage());
            }
        } catch (NumberFormatException e) {
            showErrorAlert("L'ID du propriétaire doit être un nombre valide.");
        }
    }

    @FXML
    public void closeWindow() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }

    private boolean validateInputs() {
        if (tfOwnerId.getText().trim().isEmpty()) {
            showErrorAlert("L'ID du propriétaire est requis.");
            return false;
        }

        try {
            Integer.parseInt(tfOwnerId.getText().trim());
        } catch (NumberFormatException e) {
            showErrorAlert("L'ID du propriétaire doit être un nombre valide.");
            return false;
        }

        if (tfProjectName.getText().trim().isEmpty()) {
            showErrorAlert("Le nom du projet est requis.");
            return false;
        }

        if (taDescription.getText().trim().isEmpty()) {
            showErrorAlert("La description est requise.");
            return false;
        }

        if (dpStartDate.getValue() == null) {
            showErrorAlert("La date de début est requise.");
            return false;
        }

        if (dpEndDate.getValue() == null) {
            showErrorAlert("La date de fin est requise.");
            return false;
        }

        if (dpStartDate.getValue().isAfter(dpEndDate.getValue())) {
            showErrorAlert("La date de début ne peut pas être après la date de fin.");
            return false;
        }

        if (cbStatus.getValue() == null) {
            showErrorAlert("Le statut est requis.");
            return false;
        }

        return true;
    }

    private void showSuccessAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText("Opération réussie");
        alert.setContentText(message);
        ImageView icon = createAlertIcon("success.png");
        if (icon != null) {
            alert.setGraphic(icon);
        }
        alert.showAndWait();
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText("Échec de l'opération");
        alert.setContentText(message);
        ImageView icon = createAlertIcon("error.png");
        if (icon != null) {
            alert.setGraphic(icon);
        }
        alert.showAndWait();
    }

    private ImageView createAlertIcon(String imageName) {
        try {
            String imagePath = "/images/" + imageName;
            if (getClass().getResourceAsStream(imagePath) != null) {
                return new ImageView(new Image(getClass().getResourceAsStream(imagePath)));
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not load image " + imageName);
        }
        return null;
    }
} 