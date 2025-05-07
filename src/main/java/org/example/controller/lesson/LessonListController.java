package org.example.controller.lesson;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.models.course.Course;
import org.example.models.lesson.Lesson;
import org.example.services.LessonService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class LessonListController {

    @FXML
    private Label courseNameLabel;

    @FXML
    private TableView<Lesson> lessonTable;
    
    @FXML
    private TableColumn<Lesson, Integer> idColumn;
    
    @FXML
    private TableColumn<Lesson, String> titleColumn;
    
    @FXML
    private TableColumn<Lesson, Integer> orderColumn;
    
    @FXML
    private TableColumn<Lesson, String> videoColumn;
    
    @FXML
    private TableColumn<Lesson, Void> actionsColumn;
    
    @FXML
    private Button addLessonButton;
    
    @FXML
    private Button refreshButton;
    
    @FXML
    private Button backButton;
    
    @FXML
    private Label totalLessonsLabel;
    
    private LessonService lessonService;
    private ObservableList<Lesson> lessonList;
    private Course currentCourse;
    
    @FXML
    public void initialize() {
        lessonService = new LessonService();
        lessonList = FXCollections.observableArrayList();
        
        // Initialize columns
        idColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getId()).asObject());
        titleColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTitre()));
        orderColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getOrdre()).asObject());
        videoColumn.setCellValueFactory(cellData -> {
            String videoUrl = cellData.getValue().getVideourl();
            return new SimpleStringProperty(videoUrl != null && !videoUrl.isEmpty() ? "Yes" : "No");
        });
        
        // Configure actions column
        setupActionsColumn();
    }
    
    /**
     * Set the course to display lessons for
     */
    public void setCourse(Course course) {
        this.currentCourse = course;
        courseNameLabel.setText("Lessons for: " + course.getTitre());
        
        // Load lessons for this course
        loadLessons();
    }
    
    /**
     * Set up the actions column with buttons
     */
    private void setupActionsColumn() {
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button viewBtn = new Button("View");
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final Button moveUpBtn = new Button("↑");
            private final Button moveDownBtn = new Button("↓");
            
            {
                // Style buttons
                viewBtn.getStyleClass().add("action-button");
                editBtn.getStyleClass().add("action-button");
                deleteBtn.getStyleClass().add("action-button");
                moveUpBtn.getStyleClass().add("action-button");
                moveDownBtn.getStyleClass().add("action-button");
                
                // Set tooltips
                moveUpBtn.setTooltip(new Tooltip("Move lesson up"));
                moveDownBtn.setTooltip(new Tooltip("Move lesson down"));
                
                // Set button actions
                viewBtn.setOnAction(event -> {
                    Lesson lesson = getTableView().getItems().get(getIndex());
                    viewLesson(lesson);
                });
                
                editBtn.setOnAction(event -> {
                    Lesson lesson = getTableView().getItems().get(getIndex());
                    editLesson(lesson);
                });
                
                deleteBtn.setOnAction(event -> {
                    Lesson lesson = getTableView().getItems().get(getIndex());
                    deleteLesson(lesson);
                });
                
                moveUpBtn.setOnAction(event -> {
                    Lesson lesson = getTableView().getItems().get(getIndex());
                    moveLesson(lesson, lesson.getOrdre() - 1);
                });
                
                moveDownBtn.setOnAction(event -> {
                    Lesson lesson = getTableView().getItems().get(getIndex());
                    moveLesson(lesson, lesson.getOrdre() + 1);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                
                if (empty) {
                    setGraphic(null);
                } else {
                    // Create a vertical layout for all buttons
                    VBox actionBox = new VBox(5, viewBtn, editBtn, deleteBtn);
                    
                    // Create a horizontal layout for move buttons
                    javafx.scene.layout.HBox moveBox = new javafx.scene.layout.HBox(5, moveUpBtn, moveDownBtn);
                    
                    // Add move buttons to the action box
                    actionBox.getChildren().add(moveBox);
                    
                    setGraphic(actionBox);
                    
                    // Enable/disable move buttons based on position
                    Lesson lesson = getTableView().getItems().get(getIndex());
                    boolean isFirst = lesson.getOrdre() <= 1;
                    boolean isLast = lesson.getOrdre() >= lessonList.size();
                    
                    moveUpBtn.setDisable(isFirst);
                    moveDownBtn.setDisable(isLast);
                }
            }
        });
    }
    
    /**
     * Load lessons for the current course
     */
    private void loadLessons() {
        try {
            List<Lesson> lessons = lessonService.getLessonsByCourse(currentCourse.getId());
            lessonList.setAll(lessons);
            lessonTable.setItems(lessonList);
            updateTotalCount();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", 
                    "Failed to load lessons", e.getMessage());
        }
    }
    
    /**
     * Update the total lessons count
     */
    private void updateTotalCount() {
        totalLessonsLabel.setText(String.valueOf(lessonList.size()));
    }
    
    /**
     * Handle add lesson button click
     */
    @FXML
    void handleAddLesson(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/lesson/add_lesson.fxml"));
            Parent root = loader.load();
            
            LessonController controller = loader.getController();
            controller.setParentCourse(currentCourse);
            
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Add New Lesson");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            
            // Refresh the list when the dialog closes
            loadLessons();
            
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", 
                    "Failed to open Add Lesson form", e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Handle refresh button click
     */
    @FXML
    void handleRefresh(ActionEvent event) {
        loadLessons();
    }
    
    /**
     * Handle back button click
     */
    @FXML
    void handleBack(ActionEvent event) {
        // Close this window
        Stage stage = (Stage) backButton.getScene().getWindow();
        stage.close();
    }
    
    /**
     * View lesson details
     */
    private void viewLesson(Lesson lesson) {
        // Create a dialog with lesson details
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Lesson Details");
        dialog.setHeaderText(lesson.getTitre());
        
        // Create content
        VBox content = new VBox(10);
        
        // Add lesson details
        content.getChildren().addAll(
            new Label("ID: " + lesson.getId()),
            new Label("Title: " + lesson.getTitre()),
            new Label("Order: " + lesson.getOrdre()),
            new Label("Video URL: " + (lesson.getVideourl() != null ? lesson.getVideourl() : "None")),
            new Label("Description:"),
            new TextArea(lesson.getDescription())
        );
        
        // Get the description TextArea and make it read-only
        TextArea descArea = (TextArea) content.getChildren().get(5);
        descArea.setEditable(false);
        descArea.setWrapText(true);
        descArea.setPrefHeight(200);
        
        // Add content to dialog
        dialog.getDialogPane().setContent(content);
        
        // Add close button
        ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeButton);
        
        // Show dialog
        dialog.showAndWait();
    }
    
    /**
     * Edit an existing lesson
     */
    private void editLesson(Lesson lesson) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/lesson/edit_lesson.fxml"));
            Parent root = loader.load();
            
            LessonController controller = loader.getController();
            controller.setLesson(lesson);
            
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Edit Lesson");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            
            // Refresh the list when the dialog closes
            loadLessons();
            
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", 
                    "Failed to open Edit Lesson form", e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Delete a lesson
     */
    private void deleteLesson(Lesson lesson) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Deletion");
        confirmation.setHeaderText("Delete Lesson");
        confirmation.setContentText("Are you sure you want to delete the lesson: " + lesson.getTitre() + "?");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                lessonService.deleteLesson(lesson.getId());
                
                // Refresh the list
                loadLessons();
                
                // Show success message
                showAlert(Alert.AlertType.INFORMATION, "Success", 
                        "Lesson Deleted", "The lesson has been successfully deleted.");
                
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Error", 
                        "Failed to delete lesson", e.getMessage());
            }
        }
    }
    
    /**
     * Move a lesson to a new position
     */
    private void moveLesson(Lesson lesson, int newOrder) {
        if (newOrder < 1) {
            // Can't move before first position
            return;
        }
        
        if (newOrder > lessonList.size()) {
            // Can't move beyond last position
            return;
        }
        
        try {
            int originalOrder = lesson.getOrdre();
            lesson.setOrdre(newOrder);
            
            // Update in database
            lessonService.updateLesson(lesson, originalOrder);
            
            // Refresh the list
            loadLessons();
            
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", 
                    "Failed to move lesson", e.getMessage());
            // Reset order in case of error
            lesson.setOrdre(lesson.getOrdre());
        }
    }
    
    /**
     * Utility method to show alerts
     */
    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
} 