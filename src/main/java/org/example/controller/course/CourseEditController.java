package org.example.controller.course;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.models.course.Course;
import org.example.services.CourseService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class CourseEditController {

    @FXML
    private TextField titleField;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private TextField priceField;

    @FXML
    private CheckBox isFreeCheckBox;

    @FXML
    private Button uploadImageBtn;

    @FXML
    private Button saveBtn;

    @FXML
    private Button cancelBtn;
    
    @FXML
    private ImageView courseImageView;
    
    private CourseService courseService;
    private String imagePath = "";
    private Course currentCourse;
    
    public void initialize() {
        courseService = new CourseService();
        
        // Make price field disabled when course is free
        isFreeCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            priceField.setDisable(newValue);
            if (newValue) {
                priceField.setText("0");
            }
        });
        
        // Set up the image view
        courseImageView.setFitHeight(150);
        courseImageView.setFitWidth(200);
        courseImageView.setPreserveRatio(true);
    }
    
    public void setCourse(Course course) {
        this.currentCourse = course;
        
        // Populate form fields with course data
        titleField.setText(course.getTitre());
        descriptionArea.setText(course.getDescription());
        isFreeCheckBox.setSelected(course.isIs_free());
        priceField.setText(String.valueOf(course.getPrice()));
        priceField.setDisable(course.isIs_free());
        imagePath = course.getImg();
        
        // Load course image
        if (imagePath != null && !imagePath.isEmpty()) {
            try {
                Image image = new Image("file:forum_files/" + imagePath);
                courseImageView.setImage(image);
            } catch (Exception e) {
                // If loading fails, use default image
                courseImageView.setImage(new Image(getClass().getResourceAsStream("/images/default-course.png")));
            }
        } else {
            // Use default image if no path specified
            courseImageView.setImage(new Image(getClass().getResourceAsStream("/images/default-course.png")));
        }
    }
    
    @FXML
    void handleUploadImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Course Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        
        Stage stage = (Stage) uploadImageBtn.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);
        
        if (selectedFile != null) {
            try {
                // Generate a unique filename
                String uniqueFileName = UUID.randomUUID().toString() + "_" + selectedFile.getName();
                
                // Create directory if it doesn't exist
                Path imageDir = Paths.get("forum_files/course_images");
                if (!Files.exists(imageDir)) {
                    Files.createDirectories(imageDir);
                }
                
                // Copy file to the image directory
                Path destinationPath = imageDir.resolve(uniqueFileName);
                Files.copy(selectedFile.toPath(), destinationPath, StandardCopyOption.REPLACE_EXISTING);
                
                // Save the relative path
                imagePath = "course_images/" + uniqueFileName;
                
                // Update the image view
                Image image = new Image(selectedFile.toURI().toString());
                courseImageView.setImage(image);
                
            } catch (IOException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to upload image: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }
    
    @FXML
    void handleSave(ActionEvent event) {
        if (!validateForm()) {
            return;
        }
        
        try {
            // Update course with form data
            currentCourse.setTitre(titleField.getText().trim());
            currentCourse.setDescription(descriptionArea.getText().trim());
            currentCourse.setIs_free(isFreeCheckBox.isSelected());
            currentCourse.setPrice(Double.parseDouble(priceField.getText().trim()));
            currentCourse.setImg(imagePath);
            
            // Update in database
            courseService.updateCourse(currentCourse);
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText("Course updated successfully!");
            alert.showAndWait();
            
            // Close the window
            Stage stage = (Stage) saveBtn.getScene().getWindow();
            stage.close();
            
        } catch (SQLException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Database Error");
            alert.setContentText("Failed to update course: " + e.getMessage());
            alert.showAndWait();
        } catch (NumberFormatException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Invalid Input");
            alert.setContentText("Please enter a valid price.");
            alert.showAndWait();
        }
    }
    
    @FXML
    void handleCancel(ActionEvent event) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Cancel Course Edit");
        confirmation.setContentText("Are you sure you want to cancel? Any changes will be lost.");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Close the window
            Stage stage = (Stage) cancelBtn.getScene().getWindow();
            stage.close();
        }
    }
    
    private boolean validateForm() {
        StringBuilder errorMessage = new StringBuilder();
        
        if (titleField.getText().trim().isEmpty()) {
            errorMessage.append("- Title is required\n");
        }
        
        if (descriptionArea.getText().trim().isEmpty()) {
            errorMessage.append("- Description is required\n");
        }
        
        if (!isFreeCheckBox.isSelected() && priceField.getText().trim().isEmpty()) {
            errorMessage.append("- Price is required for non-free courses\n");
        }
        
        if (!isFreeCheckBox.isSelected()) {
            try {
                double price = Double.parseDouble(priceField.getText().trim());
                if (price <= 0) {
                    errorMessage.append("- Price must be greater than zero for non-free courses\n");
                }
            } catch (NumberFormatException e) {
                errorMessage.append("- Price must be a valid number\n");
            }
        }
        
        if (imagePath.isEmpty()) {
            errorMessage.append("- Course image is required\n");
        }
        
        if (errorMessage.length() > 0) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Validation Error");
            alert.setHeaderText("Please correct the following errors:");
            alert.setContentText(errorMessage.toString());
            alert.showAndWait();
            return false;
        }
        
        return true;
    }
} 