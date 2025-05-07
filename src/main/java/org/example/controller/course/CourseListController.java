package org.example.controller.course;

import javafx.beans.property.SimpleDoubleProperty;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.controller.lesson.LessonListController;
import org.example.models.course.Course;
import org.example.services.CourseService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class CourseListController {

    @FXML
    private TableView<Course> courseTable;
    
    @FXML
    private TableColumn<Course, Integer> idColumn;
    
    @FXML
    private TableColumn<Course, String> titleColumn;
    
    @FXML
    private TableColumn<Course, Double> priceColumn;
    
    @FXML
    private TableColumn<Course, String> freeColumn;
    
    @FXML
    private TableColumn<Course, String> dateColumn;
    
    @FXML
    private TableColumn<Course, Void> actionsColumn;
    
    @FXML
    private Button addCourseButton;
    
    @FXML
    private Button refreshButton;
    
    @FXML
    private Button statisticsButton;
    
    @FXML
    private TextField searchField;
    
    @FXML
    private Label totalCoursesLabel;
    
    private CourseService courseService;
    private ObservableList<Course> courseList;
    
    @FXML
    public void initialize() {
        courseService = new CourseService();
        courseList = FXCollections.observableArrayList();
        
        // Initialize columns
        idColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getId()).asObject());
        titleColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTitre()));
        priceColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getPrice()).asObject());
        
        freeColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().isIs_free() ? "Yes" : "No"));
        
        dateColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getDatecreation().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        
        // Configure actions column
        setupActionsColumn();
        
        // Set initial data
        loadCourses();
        
        // Setup search functionality
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterCourses(newValue);
        });
        
        // Update total count when the list changes
        courseList.addListener((javafx.collections.ListChangeListener.Change<? extends Course> c) -> {
            updateTotalCount();
        });
        
        // Setup statistics button if it exists
        if (statisticsButton != null) {
            statisticsButton.setOnAction(this::handleStatistics);
        }
    }
    
    private void setupActionsColumn() {
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button viewBtn = new Button("View");
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final Button lessonsBtn = new Button("Lessons");
            
            {
                // Style buttons
                viewBtn.getStyleClass().add("action-button");
                editBtn.getStyleClass().add("action-button");
                deleteBtn.getStyleClass().add("action-button");
                lessonsBtn.getStyleClass().add("action-button");
                lessonsBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                
                // Set button actions
                viewBtn.setOnAction(event -> {
                    Course course = getTableView().getItems().get(getIndex());
                    viewCourse(course);
                });
                
                editBtn.setOnAction(event -> {
                    Course course = getTableView().getItems().get(getIndex());
                    editCourse(course);
                });
                
                deleteBtn.setOnAction(event -> {
                    Course course = getTableView().getItems().get(getIndex());
                    deleteCourse(course);
                });
                
                lessonsBtn.setOnAction(event -> {
                    Course course = getTableView().getItems().get(getIndex());
                    manageLessons(course);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                
                if (empty) {
                    setGraphic(null);
                } else {
                    // Create a vertical layout for all buttons
                    VBox box = new VBox(5, viewBtn, editBtn, lessonsBtn, deleteBtn);
                    setGraphic(box);
                }
            }
        });
    }
    
    private void loadCourses() {
        try {
            List<Course> courses = courseService.getAllCourses();
            courseList.setAll(courses);
            courseTable.setItems(courseList);
            updateTotalCount();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", 
                    "Failed to load courses", e.getMessage());
        }
    }
    
    private void updateTotalCount() {
        totalCoursesLabel.setText(String.valueOf(courseList.size()));
    }
    
    private void filterCourses(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            courseTable.setItems(courseList);
            return;
        }
        
        // Convert to lowercase for case-insensitive search
        String lowerCaseSearch = searchText.toLowerCase();
        
        // Create filtered list
        ObservableList<Course> filteredList = FXCollections.observableArrayList();
        
        for (Course course : courseList) {
            if (course.getTitre().toLowerCase().contains(lowerCaseSearch) ||
                course.getDescription().toLowerCase().contains(lowerCaseSearch)) {
                filteredList.add(course);
            }
        }
        
        courseTable.setItems(filteredList);
        // Update count for filtered results
        totalCoursesLabel.setText(String.valueOf(filteredList.size()));
    }
    
    @FXML
    void handleAddCourse(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/course/add_course.fxml"));
            Parent root = loader.load();
            
            Stage stage = new Stage();
            stage.setTitle("Add New Course");
            stage.setScene(new Scene(root));
            stage.show();
            
            // Refresh the list when the dialog closes
            stage.setOnHidden(e -> loadCourses());
            
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", 
                    "Failed to open Add Course form", e.getMessage());
        }
    }
    
    @FXML
    void handleRefresh(ActionEvent event) {
        loadCourses();
        searchField.clear();
    }
    
    /**
     * Open the lessons management screen for a course
     */
    private void manageLessons(Course course) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/lesson/lessons_list.fxml"));
            Parent root = loader.load();
            
            // Get controller and pass the course
            LessonListController controller = loader.getController();
            controller.setCourse(course);
            
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Lessons for: " + course.getTitre());
            stage.setScene(new Scene(root));
            stage.show();
            
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", 
                    "Failed to open Lessons management", e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void viewCourse(Course course) {
        // Create a dialog with course details
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Course Details");
        dialog.setHeaderText(course.getTitre());
        
        // Create content
        VBox content = new VBox(10);
        
        // Add course image
        try {
            ImageView imageView = new ImageView();
            imageView.setFitHeight(200);
            imageView.setFitWidth(300);
            imageView.setPreserveRatio(true);
            
            // Try to load the image from the specified path
            String imagePath = course.getImg();
            if (imagePath != null && !imagePath.isEmpty()) {
                try {
                    Image image = new Image("file:forum_files/" + imagePath);
                    imageView.setImage(image);
                } catch (Exception e) {
                    // If loading fails, use default image
                    imageView.setImage(new Image(getClass().getResourceAsStream("/images/default-course.png")));
                }
            } else {
                // Use default image if no path specified
                imageView.setImage(new Image(getClass().getResourceAsStream("/images/default-course.png")));
            }
            
            content.getChildren().add(imageView);
        } catch (Exception e) {
            // Skip image if there's an issue
            System.err.println("Error loading image: " + e.getMessage());
        }
        
        // Add course details
        content.getChildren().addAll(
            new Label("ID: " + course.getId()),
            new Label("Owner ID: " + course.getIdowner_id()),
            new Label("Title: " + course.getTitre()),
            new Label("Price: " + (course.isIs_free() ? "Free" : "$" + course.getPrice())),
            new Label("Description: " + course.getDescription()),
            new Label("Created: " + course.getDatecreation().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
        );
        
        // Add content to dialog
        dialog.getDialogPane().setContent(content);
        
        // Add close button
        ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeButton);
        
        // Show dialog
        dialog.showAndWait();
    }
    
    private void editCourse(Course course) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/course/edit_course.fxml"));
            Parent root = loader.load();
            
            // Get controller and pass the course to edit
            CourseEditController controller = loader.getController();
            controller.setCourse(course);
            
            Stage stage = new Stage();
            stage.setTitle("Edit Course");
            stage.setScene(new Scene(root));
            stage.show();
            
            // Refresh the list when the dialog closes
            stage.setOnHidden(e -> loadCourses());
            
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", 
                    "Failed to open Edit Course form", e.getMessage());
        }
    }
    
    private void deleteCourse(Course course) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Deletion");
        confirmation.setHeaderText("Delete Course");
        confirmation.setContentText("Are you sure you want to delete the course: " + course.getTitre() + "?");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                courseService.deleteCourse(course.getId());
                courseList.remove(course);
                
                // Show success message
                showAlert(Alert.AlertType.INFORMATION, "Success", 
                        "Course Deleted", "The course has been successfully deleted.");
                
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Error", 
                        "Failed to delete course", e.getMessage());
            }
        }
    }
    
    @FXML
    void handleStatistics(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/course/course_statistics.fxml"));
            Parent root = loader.load();
            
            // Get the content pane from the current scene
            Parent currentRoot = ((Button)event.getSource()).getScene().getRoot();
            
            // Find the content pane in the current scene (assuming BorderPane with center)
            if (currentRoot instanceof javafx.scene.layout.BorderPane) {
                javafx.scene.layout.BorderPane borderPane = (javafx.scene.layout.BorderPane) currentRoot;
                
                // Replace the center content with statistics
                borderPane.setCenter(root);
                
                // Update window title
                Stage stage = (Stage) ((Button)event.getSource()).getScene().getWindow();
                stage.setTitle("Course Statistics");
            } else {
                // Fallback if we can't find the content pane: open in new window
                Stage stage = new Stage();
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.setTitle("Course Statistics");
                stage.setScene(new Scene(root));
                stage.show();
            }
            
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", 
                    "Failed to open statistics view", e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
} 