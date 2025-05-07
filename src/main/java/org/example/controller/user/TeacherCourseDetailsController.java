package org.example.controller.user;

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.models.course.Course;
import org.example.services.CourseService;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class TeacherCourseDetailsController {
    
    @FXML
    private TextField titleField;
    
    @FXML
    private TextArea descriptionArea;
    
    @FXML
    private TextField priceField;
    
    @FXML
    private CheckBox isFreeCheckBox;
    
    @FXML
    private Button uploadImageButton;
    
    @FXML
    private ImageView courseImageView;
    
    @FXML
    private Spinner<Integer> minHoursSpinner;
    
    @FXML
    private Spinner<Integer> maxHoursSpinner;
    
    @FXML
    private Button saveButton;
    
    @FXML
    private Button cancelButton;
    
    @FXML
    private Label titleLabel;
    
    private CourseService courseService;
    private Course currentCourse;
    private boolean viewMode = false;
    private boolean isNewCourse = false;
    private int teacherId;
    private String imagePath;
    private Runnable onSaveCallback;
    private Runnable onCancelCallback;
    
    @FXML
    private void initialize() {
        courseService = new CourseService();
        
        // Configure price field to accept only numbers and decimal point
        priceField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d*)?")) {
                priceField.setText(oldValue);
            }
        });
        
        // Configure hour spinners
        SpinnerValueFactory<Integer> minHoursFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 1);
        minHoursSpinner.setValueFactory(minHoursFactory);
        
        SpinnerValueFactory<Integer> maxHoursFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 2);
        maxHoursSpinner.setValueFactory(maxHoursFactory);
        
        // Set up free checkbox to disable/enable price field
        isFreeCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            priceField.setDisable(newValue);
            if (newValue) {
                priceField.setText("0.00");
            }
        });
        
        // Set up image upload button
        uploadImageButton.setOnAction(event -> uploadImage());
        
        // Set up save and cancel buttons
        saveButton.setOnAction(event -> saveCourse());
        cancelButton.setOnAction(event -> closeWindow());
    }
    
    /**
     * Set the course for editing or viewing
     */
    public void setCourse(Course course) {
        this.currentCourse = course;
        this.isNewCourse = false;
        
        // Fill form with course data
        titleField.setText(course.getTitre());
        descriptionArea.setText(course.getDescription());
        priceField.setText(String.format("%.2f", course.getPrice()));
        isFreeCheckBox.setSelected(course.isIs_free());
        
        minHoursSpinner.getValueFactory().setValue(course.getMinimal_hours());
        maxHoursSpinner.getValueFactory().setValue(course.getMaximal_hours());
        
        // Load course image
        if (course.getImg() != null && !course.getImg().isEmpty()) {
            imagePath = course.getImg();
            try {
                Image image = new Image(imagePath);
                courseImageView.setImage(image);
            } catch (Exception e) {
                System.err.println("Error loading image: " + e.getMessage());
            }
        }
        
        // Apply view mode if needed
        applyViewMode();
    }
    
    /**
     * Create a new course
     */
    public void createNewCourse(int teacherId) {
        this.teacherId = teacherId;
        this.isNewCourse = true;
        this.currentCourse = new Course();
        currentCourse.setIdowner_id(teacherId);
        currentCourse.setDatecreation(LocalDateTime.now());
        
        titleLabel.setText("Create New Course");
        saveButton.setText("Create Course");
        
        // Set form to editable
        setFormEditable(true);
    }
    
    /**
     * Set callback to be run after successful save
     */
    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }
    
    /**
     * Set whether form is in view mode
     */
    public void setViewMode(boolean viewMode) {
        this.viewMode = viewMode;
        applyViewMode();
    }
    
    /**
     * Apply view mode settings to the form
     */
    private void applyViewMode() {
        if (viewMode) {
            titleLabel.setText("Course Details");
            saveButton.setText("Edit Course");
            saveButton.setOnAction(event -> {
                setViewMode(false);
                saveButton.setText("Save Changes");
                saveButton.setOnAction(e -> saveCourse());
            });
            setFormEditable(false);
        } else {
            titleLabel.setText("Edit Course");
            saveButton.setText("Save Changes");
            setFormEditable(true);
        }
    }
    
    /**
     * Set form fields editable or not
     */
    private void setFormEditable(boolean editable) {
        titleField.setEditable(editable);
        descriptionArea.setEditable(editable);
        priceField.setEditable(editable);
        isFreeCheckBox.setDisable(!editable);
        minHoursSpinner.setDisable(!editable);
        maxHoursSpinner.setDisable(!editable);
        uploadImageButton.setVisible(editable);
    }
    
    /**
     * Upload a course image
     */
    private void uploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Course Image");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        
        File selectedFile = fileChooser.showOpenDialog(uploadImageButton.getScene().getWindow());
        if (selectedFile != null) {
            // Store path for saving later
            imagePath = selectedFile.toURI().toString();
            
            // Display image preview
            Image image = new Image(imagePath);
            courseImageView.setImage(image);
        }
    }
    
    /**
     * Save the course (create new or update existing)
     */
    private void saveCourse() {
        // Validate input
        if (titleField.getText().trim().isEmpty()) {
            showError("Title is required");
            return;
        }
        
        if (descriptionArea.getText().trim().isEmpty()) {
            showError("Description is required");
            return;
        }
        
        try {
            // Update course object with form data
            currentCourse.setTitre(titleField.getText().trim());
            currentCourse.setDescription(descriptionArea.getText().trim());
            currentCourse.setIs_free(isFreeCheckBox.isSelected());
            
            double price = 0.0;
            if (!isFreeCheckBox.isSelected()) {
                try {
                    price = Double.parseDouble(priceField.getText());
                } catch (NumberFormatException e) {
                    showError("Invalid price format");
                    return;
                }
            }
            currentCourse.setPrice(price);
            
            currentCourse.setImg(imagePath);
            currentCourse.setMinimal_hours(minHoursSpinner.getValue());
            currentCourse.setMaximal_hours(maxHoursSpinner.getValue());
            
            // Save to database
            if (isNewCourse) {
                courseService.addCourse(currentCourse);
                showInfo("Course created successfully");
            } else {
                courseService.updateCourse(currentCourse);
                showInfo("Course updated successfully");
            }
            
            // Call callback if provided
            if (onSaveCallback != null) {
                onSaveCallback.run();
            }
            
            // Close the window
            closeWindow();
            
        } catch (SQLException e) {
            showError("Error saving course: " + e.getMessage());
        }
    }
    
    /**
     * Set the teacher ID 
     */
    public void setTeacherId(int teacherId) {
        this.teacherId = teacherId;
    }

    /**
     * Set callback to be run after cancellation
     */
    public void setOnCancelCallback(Runnable callback) {
        this.onCancelCallback = callback;
        
        // Update the cancel button action
        if (cancelButton != null) {
            cancelButton.setOnAction(event -> {
                if (onCancelCallback != null) {
                    onCancelCallback.run();
                } else {
                    closeWindow();
                }
            });
        }
    }
    
    /**
     * Get the current scene for navigation
     */
    public Scene getScene() {
        if (cancelButton != null && cancelButton.getScene() != null) {
            return cancelButton.getScene();
        }
        return null;
    }
    
    /**
     * Close the window
     */
    private void closeWindow() {
        if (onCancelCallback != null) {
            onCancelCallback.run();
        } else {
            Stage stage = (Stage) cancelButton.getScene().getWindow();
            stage.close();
        }
    }
    
    /**
     * Show an error message dialog
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Show an information message dialog
     */
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
} 