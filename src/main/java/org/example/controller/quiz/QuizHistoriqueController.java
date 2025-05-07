package org.example.controller.quiz;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.example.model.quiz.Quiz;
import utils.dataSource;
import java.io.FileOutputStream;
import java.sql.*;
import java.util.*;


public class QuizHistoriqueController {
    @FXML
    private TableView<QuizHistoryRow> historyTable;
    @FXML
    private TableColumn<QuizHistoryRow, String> quizTitleColumn;
    @FXML
    private TableColumn<QuizHistoryRow, String> dateColumn;
    @FXML
    private TableColumn<QuizHistoryRow, String> scoreColumn;

    private int userId;

    public void setUserId(int userId) {
        this.userId = userId;
        loadHistory();
    }

    @FXML
    private void initialize() {
        quizTitleColumn.setCellValueFactory(new PropertyValueFactory<>("quizTitle"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        scoreColumn.setCellValueFactory(new PropertyValueFactory<>("score"));
    }

    private void loadHistory() {
        historyTable.getItems().clear();
        try (Connection conn = dataSource.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT q.titre, h.date_passage, h.score, h.total_questions " +
                "FROM quiz_history h JOIN quiz q ON h.id_quiz = q.id " +
                "WHERE h.id_utilisateur = ? ORDER BY h.date_passage DESC")) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String titre = rs.getString("titre");
                String date = rs.getTimestamp("date_passage").toLocalDateTime().toString();
                int score = rs.getInt("score");
                int total = rs.getInt("total_questions");
                historyTable.getItems().add(new QuizHistoryRow(titre, date, score + "/" + total));
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger l'historique : " + e.getMessage());
        }
    }

    private String getOptionText(ResultSet rs, int option) throws SQLException {
        switch (option) {
            case 1: return rs.getString("option_1");
            case 2: return rs.getString("option_2");
            case 3: return rs.getString("option_3");
            case 4: return rs.getString("option_4");
            default: return "(Aucune réponse)";
        }
    }

    public static class QuizHistoryRow {
        private final String quizTitle;
        private final String date;
        private final String score;

        public QuizHistoryRow(String quizTitle, String date, String score) {
            this.quizTitle = quizTitle;
            this.date = date;
            this.score = score;
        }
        public String getQuizTitle() { return quizTitle; }
        public String getDate() { return date; }
        public String getScore() { return score; }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
} 