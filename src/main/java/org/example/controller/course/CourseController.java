package org.example.controller.course;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.models.course.Course;
import org.example.services.CourseService;
import utils.dataSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.util.Optional;
import java.util.UUID;

public class CourseController {

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
    
    // Get a valid user ID from the database
    private int currentUserId;
    
    public void initialize() {
        courseService = new CourseService();
        
        // Find a valid user ID from the database
        findValidUserId();
        
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
        
        try {
            // Try to set default image
            Image defaultImage = new Image(getClass().getResourceAsStream("/images/default-course.png"));
            courseImageView.setImage(defaultImage);
        } catch (Exception e) {
            System.err.println("Failed to load default image: " + e.getMessage());
        }
    }
    
    /**
     * Find a valid user ID from the database to use for the foreign key
     */
    private void findValidUserId() {
        currentUserId = 1; // Default fallback value
        
        Connection connection = null;
        try {
            connection = dataSource.getInstance().getConnection();
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id FROM user LIMIT 1");
            
            if (rs.next()) {
                currentUserId = rs.getInt("id");
                System.out.println("Using user ID: " + currentUserId);
            } else {
                // No users found, show warning
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("No Users Found");
                alert.setHeaderText("No valid users exist in the database");
                alert.setContentText("You need to create a user before adding courses.");
                alert.showAndWait();
            }
            
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            System.err.println("Error finding valid user ID: " + e.getMessage());
        } finally {
            if (connection != null) {
                try {
                    dataSource.getInstance().releaseConnection(connection);
                } catch (Exception e) {
                    System.err.println("Error releasing connection: " + e.getMessage());
                }
            }
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
            String title = titleField.getText().trim();
            String description = descriptionArea.getText().trim();
            boolean isFree = isFreeCheckBox.isSelected();
            double price = isFree ? 0 : Double.parseDouble(priceField.getText().trim());
            
            Course course = new Course(currentUserId, title, price, imagePath, description, isFree);
            
            courseService.addCourse(course);
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText("Course added successfully!");
            alert.showAndWait();
            
            // Clear the form
            clearForm();
            
        } catch (SQLException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Database Error");
            alert.setContentText("Failed to save course: " + e.getMessage());
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
        confirmation.setHeaderText("Cancel Course Creation");
        confirmation.setContentText("Are you sure you want to cancel? Any changes will be lost.");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            clearForm();
            
            // Return to the previous screen/dashboard
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Menu/menu.fxml"));
                Parent root = loader.load();
                Scene scene = new Scene(root);
                Stage stage = (Stage) cancelBtn.getScene().getWindow();
                stage.setScene(scene);
                stage.setTitle("Dashboard");
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
            }
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
        
        // Make image optional by removing this validation
        /*
        if (imagePath.isEmpty()) {
            errorMessage.append("- Course image is required\n");
        }
        */
        
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
    
    private void clearForm() {
        titleField.clear();
        descriptionArea.clear();
        priceField.clear();
        isFreeCheckBox.setSelected(false);
        imagePath = "";
        
        try {
            // Reset image view to default
            Image defaultImage = new Image(getClass().getResourceAsStream("/images/default-course.png"));
            courseImageView.setImage(defaultImage);
        } catch (Exception e) {
            System.err.println("Failed to reset image: " + e.getMessage());
        }
    }
} 