package org.example.controller.lesson;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.models.course.Course;
import org.example.models.lesson.Lesson;
import org.example.services.LessonService;

import java.sql.SQLException;
import java.util.Optional;

public class LessonController {

    @FXML
    private TextField titleField;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private TextField videoUrlField;

    @FXML
    private Spinner<Integer> orderSpinner;
    
    @FXML
    private Button saveBtn;

    @FXML
    private Button cancelBtn;
    
    @FXML
    private Button insertYouTubeBtn;
    
    private LessonService lessonService;
    private Lesson currentLesson;
    private Course parentCourse;
    private boolean isEditMode = false;
    private int originalOrder = 0;
    
    /**
     * Initialize the controller
     */
    public void initialize() {
        lessonService = new LessonService();
        
        // Configure the order spinner
        SpinnerValueFactory<Integer> valueFactory = 
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 1);
        orderSpinner.setValueFactory(valueFactory);
        orderSpinner.setEditable(true);
        
        // Add tooltip for video URL field
        Tooltip videoTooltip = new Tooltip("Enter a YouTube URL (e.g., https://www.youtube.com/watch?v=dQw4w9WgXcQ) or a local file path");
        videoUrlField.setTooltip(videoTooltip);
        
        // Add a placeholder text for the video URL field
        videoUrlField.setPromptText("https://www.youtube.com/watch?v=VIDEO_ID or local file path");
    }
    
    /**
     * Handle insert YouTube sample button
     */
    @FXML
    private void handleInsertYouTube(ActionEvent event) {
        videoUrlField.setText(getRandomYouTubeVideoUrl());
    }
    
    /**
     * Returns a random educational YouTube video URL
     */
    private String getRandomYouTubeVideoUrl() {
        String[] educationalVideos = {
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ", // Rick Astley - Never Gonna Give You Up
            "https://www.youtube.com/watch?v=8mAITcNt710", // Harvard CS50 Introduction to Computer Science
            "https://www.youtube.com/watch?v=rfscVS0vtbw", // Learn Python - Full Course for Beginners
            "https://www.youtube.com/watch?v=yZ0cHJrKNsw", // Introduction to Java Programming
            "https://www.youtube.com/watch?v=5X1VIyiQXYI", // Web Development Tutorial for Beginners
            "https://www.youtube.com/watch?v=1Rs2ND1ryYc", // CSS Tutorial - Full Course for Beginners
            "https://www.youtube.com/watch?v=7S_tz1z_5bA"  // MySQL Tutorial for Beginners
        };
        
        // Return a random video URL
        int randomIndex = (int) (Math.random() * educationalVideos.length);
        return educationalVideos[randomIndex];
    }
    
    /**
     * Set up for adding a new lesson to a course
     */
    public void setParentCourse(Course course) {
        this.parentCourse = course;
        this.isEditMode = false;
        
        // Set course-specific window title and load max order value
        try {
            // Create a new lesson and get the next order number
            Lesson newLesson = new Lesson();
            newLesson.setIdcours_id(course.getId());
            
            this.currentLesson = newLesson;
            
            // Try to find the current max order
            int maxOrder = 1;
            try {
                java.util.List<Lesson> lessons = lessonService.getLessonsByCourse(course.getId());
                if (!lessons.isEmpty()) {
                    // Find the highest order
                    for (Lesson lesson : lessons) {
                        if (lesson.getOrdre() > maxOrder) {
                            maxOrder = lesson.getOrdre();
                        }
                    }
                    maxOrder++; // Increment for the new lesson
                }
            } catch (SQLException e) {
                System.err.println("Error getting max order: " + e.getMessage());
            }
            
            // Update spinner to show next available order
            SpinnerValueFactory<Integer> valueFactory = 
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, maxOrder);
            orderSpinner.setValueFactory(valueFactory);
            
        } catch (Exception e) {
            System.err.println("Error initializing new lesson: " + e.getMessage());
        }
    }
    
    /**
     * Set up for editing an existing lesson
     */
    public void setLesson(Lesson lesson) {
        this.currentLesson = lesson;
        this.isEditMode = true;
        this.originalOrder = lesson.getOrdre();
        
        // Populate form with lesson data
        titleField.setText(lesson.getTitre());
        descriptionArea.setText(lesson.getDescription());
        
        if (lesson.getVideourl() != null) {
            videoUrlField.setText(lesson.getVideourl());
        }
        
        // Set the order spinner value
        SpinnerValueFactory<Integer> valueFactory = 
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, lesson.getOrdre());
        orderSpinner.setValueFactory(valueFactory);
        
        // Change the save button text
        if (saveBtn != null) {
            saveBtn.setText("Update Lesson");
        }
    }
    
    /**
     * Handle save button click
     */
    @FXML
    void handleSave(ActionEvent event) {
        if (!validateForm()) {
            return;
        }
        
        try {
            // Prepare lesson data
            if (currentLesson == null) {
                currentLesson = new Lesson();
                if (parentCourse != null) {
                    currentLesson.setIdcours_id(parentCourse.getId());
                }
            }
            
            currentLesson.setTitre(titleField.getText().trim());
            currentLesson.setDescription(descriptionArea.getText().trim());
            currentLesson.setVideourl(videoUrlField.getText().trim().isEmpty() ? null : videoUrlField.getText().trim());
            currentLesson.setOrdre(orderSpinner.getValue());
            
            // Save to database
            if (isEditMode) {
                lessonService.updateLesson(currentLesson, originalOrder);
                showAlert(Alert.AlertType.INFORMATION, "Success", null, "Lesson updated successfully!");
            } else {
                lessonService.addLesson(currentLesson);
                showAlert(Alert.AlertType.INFORMATION, "Success", null, "Lesson added successfully!");
            }
            
            // Close the window
            closeWindow();
            
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Database Error", 
                    "Failed to save lesson: " + e.getMessage());
        }
    }
    
    /**
     * Handle cancel button click
     */
    @FXML
    void handleCancel(ActionEvent event) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Cancel Lesson " + (isEditMode ? "Edit" : "Creation"));
        confirmation.setContentText("Are you sure you want to cancel? Any changes will be lost.");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            closeWindow();
        }
    }
    
    /**
     * Validate form inputs
     */
    private boolean validateForm() {
        StringBuilder errorMessage = new StringBuilder();
        
        if (titleField.getText().trim().isEmpty()) {
            errorMessage.append("- Title is required\n");
        }
        
        if (descriptionArea.getText().trim().isEmpty()) {
            errorMessage.append("- Description is required\n");
        }
        
        if (errorMessage.length() > 0) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", 
                    "Please correct the following errors:", errorMessage.toString());
            return false;
        }
        
        return true;
    }
    
    /**
     * Close the current window
     */
    private void closeWindow() {
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
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