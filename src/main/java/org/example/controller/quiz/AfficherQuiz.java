package org.example.controller.quiz;

import org.example.model.quiz.Quiz;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

public class AfficherQuiz {
    @FXML
    private TableView<Quiz> quizTableView;
    @FXML
    private TableColumn<Quiz, Integer> idColumn;
    @FXML
    private TableColumn<Quiz, String> titreColumn;
    @FXML
    private TableColumn<Quiz, String> descriptionColumn;
    @FXML
    private TableColumn<Quiz, LocalDateTime> dateCreationColumn;
    @FXML
    private TableColumn<Quiz, LocalDateTime> dateDebutColumn;
    @FXML
    private TableColumn<Quiz, LocalDateTime> dateFinColumn;
    @FXML
    private TableColumn<Quiz, String> statutColumn;
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

    private ObservableList<Quiz> quizList = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        titreColumn.setCellValueFactory(new PropertyValueFactory<>("titre"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        dateCreationColumn.setCellValueFactory(new PropertyValueFactory<>("dateCreation"));
        dateDebutColumn.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));
        dateFinColumn.setCellValueFactory(new PropertyValueFactory<>("dateFin"));
        statutColumn.setCellValueFactory(new PropertyValueFactory<>("statut"));

        // Ajouter le style pour les statuts
        statutColumn.setCellFactory(column -> new TableCell<Quiz, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    getStyleClass().clear();
                } else {
                    setText(item);
                    getStyleClass().clear();
                    switch (item.toLowerCase()) {
                        case "actif":
                            getStyleClass().add("status-active");
                            break;
                        case "inactif":
                            getStyleClass().add("status-inactive");
                            break;
                        case "en attente":
                            getStyleClass().add("status-pending");
                            break;
                        default:
                            getStyleClass().clear();
                    }
                }
            }
        });



        loadQuizData();

        refreshButton.setOnAction(event -> loadQuizData());
        addButton.setOnAction(event -> handleAdd());
        modifyButton.setOnAction(event -> handleModify());
        deleteButton.setOnAction(event -> handleDelete());
        returnButton.setOnAction(event -> handleReturn());
    }

    @FXML
    private void handleReturn() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Menu/menu.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) returnButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de retourner au menu: " + e.getMessage());
        }
    }

    @FXML
    private void loadQuizData() {
        try (Connection conn = dataSource.getInstance().getConnection()) {
            String query = "SELECT id, titre, description, date_creation, datedebut, datefin, statut FROM quiz ORDER BY id DESC";
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            quizList.clear();
            
            while (rs.next()) {
                try {
                    Quiz quiz = new Quiz(
                        rs.getInt("id"),
                        rs.getString("titre"),
                        rs.getString("description"),
                        rs.getTimestamp("date_creation").toLocalDateTime(),
                        rs.getTimestamp("datedebut").toLocalDateTime(),
                        rs.getTimestamp("datefin").toLocalDateTime(),
                        rs.getString("statut")
                    );
                    quizList.add(quiz);
                } catch (Exception e) {
                    // Ignorer les erreurs individuelles
                }
            }
            quizTableView.setItems(quizList);
            
        } catch (SQLException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Erreur lors du chargement des quiz");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void handleAdd() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/quiz/AjouterQuiz.fxml"));
            Parent root = loader.load();
            org.example.controller.quiz.AjouterQuiz controller = loader.getController();
            Stage stage = new Stage();
            controller.setDialogStage(stage);
            stage.setTitle("Ajouter un Quiz");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadQuizData();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir la fenêtre d'ajout: " + e.getMessage());
        }
    }

    @FXML
    private void handleModify() {
        Quiz selectedQuiz = quizTableView.getSelectionModel().getSelectedItem();
        if (selectedQuiz == null) {
            showAlert(Alert.AlertType.WARNING, "Avertissement", "Veuillez sélectionner un quiz à modifier");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/quiz/ModifierQuiz.fxml"));
            Parent root = loader.load();
            ModifierQuiz controller = loader.getController();
            controller.setQuiz(selectedQuiz);
            Stage stage = new Stage();
            controller.setDialogStage(stage);
            stage.setTitle("Modifier le Quiz");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadQuizData();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir la fenêtre de modification: " + e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        Quiz selectedQuiz = quizTableView.getSelectionModel().getSelectedItem();
        if (selectedQuiz == null) {
            showAlert(Alert.AlertType.WARNING, "Avertissement", "Veuillez sélectionner un quiz à supprimer");
            return;
        }

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirmation de suppression");
        confirmDialog.setHeaderText("Supprimer le quiz");
        confirmDialog.setContentText("Êtes-vous sûr de vouloir supprimer ce quiz ?");

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try (Connection conn = dataSource.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM Quiz WHERE id = ?")) {
                stmt.setInt(1, selectedQuiz.getId());
                int rowsDeleted = stmt.executeUpdate();
                if (rowsDeleted > 0) {
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Quiz supprimé avec succès");
                    loadQuizData();
                }
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de supprimer le quiz: " + e.getMessage());
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