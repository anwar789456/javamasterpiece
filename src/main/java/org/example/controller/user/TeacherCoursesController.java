package org.example.controller.user;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.example.models.course.Course;
import org.example.services.CourseService;
import org.example.controller.menu.FrontMenu;
import org.example.controller.menu.HomeController;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class TeacherCoursesController {
    
    @FXML
    private TableView<Course> coursesTable;
    
    @FXML
    private TableColumn<Course, Integer> idColumn;
    
    @FXML
    private TableColumn<Course, String> titleColumn;
    
    @FXML
    private TableColumn<Course, String> descriptionColumn;
    
    @FXML
    private TableColumn<Course, String> priceColumn;
    
    @FXML
    private TableColumn<Course, String> dateColumn;
    
    @FXML
    private TableColumn<Course, String> actionsColumn;
    
    @FXML
    private Button addCourseButton;
    
    @FXML
    private Button backButton;
    
    @FXML
    private Label courseCountLabel;
    
    private CourseService courseService;
    private ObservableList<Course> courseList;
    private int teacherId;
    private FrontMenu frontMenu;
    
    @FXML
    private void initialize() {
        courseService = new CourseService();
        courseList = FXCollections.observableArrayList();
        
        // Initialize table columns
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("titre"));
        
        descriptionColumn.setCellValueFactory(data -> {
            String description = data.getValue().getDescription();
            // Truncate long descriptions
            if (description != null && description.length() > 50) {
                return new SimpleStringProperty(description.substring(0, 47) + "...");
            }
            return new SimpleStringProperty(description);
        });
        
        priceColumn.setCellValueFactory(data -> {
            Course course = data.getValue();
            if (course.isIs_free()) {
                return new SimpleStringProperty("Free");
            } else {
                return new SimpleStringProperty(String.format("$%.2f", course.getPrice()));
            }
        });
        
        dateColumn.setCellValueFactory(data -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            return new SimpleStringProperty(data.getValue().getDatecreation().format(formatter));
        });
        
        // Configure the actions column with buttons
        actionsColumn.setCellFactory(col -> new TableCell<Course, String>() {
            private final Button viewButton = new Button("View");
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");
            
            {
                viewButton.getStyleClass().add("table-btn");
                viewButton.getStyleClass().add("view-btn");
                
                editButton.getStyleClass().add("table-btn");
                editButton.getStyleClass().add("edit-btn");
                
                deleteButton.getStyleClass().add("table-btn");
                deleteButton.getStyleClass().add("delete-btn");
                
                viewButton.setOnAction(event -> {
                    Course course = getTableView().getItems().get(getIndex());
                    viewCourse(course);
                });
                
                editButton.setOnAction(event -> {
                    Course course = getTableView().getItems().get(getIndex());
                    editCourse(course);
                });
                
                deleteButton.setOnAction(event -> {
                    Course course = getTableView().getItems().get(getIndex());
                    deleteCourse(course);
                });
            }
            
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                
                if (empty) {
                    setGraphic(null);
                    setText(null);
                } else {
                    // Create a button bar with all three buttons
                    ButtonBar buttonBar = new ButtonBar();
                    buttonBar.getButtons().addAll(viewButton, editButton, deleteButton);
                    setGraphic(buttonBar);
                    setText(null);
                }
            }
        });
        
        // Set up add course button
        addCourseButton.setOnAction(event -> addNewCourse());
        
        // Update the back button text to match new navigation
        backButton.setText("← Back to Home");
        
        // Set up back button
        backButton.setOnAction(event -> backToHome());
    }
    
    /**
     * Set the teacher ID and load their courses
     */
    public void setTeacherId(int teacherId) {
        this.teacherId = teacherId;
        loadTeacherCourses();
    }
    
    /**
     * Load courses for the current teacher
     */
    private void loadTeacherCourses() {
        try {
            List<Course> courses = courseService.getCoursesByTeacher(teacherId);
            courseList.setAll(courses);
            coursesTable.setItems(courseList);
            
            // Update course count label
            courseCountLabel.setText("Total Courses: " + courses.size());
        } catch (SQLException e) {
            showError("Error loading courses: " + e.getMessage());
        }
    }
    
    /**
     * Set the FrontMenu controller for navigation
     */
    public void setFrontMenu(FrontMenu frontMenu) {
        this.frontMenu = frontMenu;
    }
    
    /**
     * Try to find the parent FrontMenu controller if not set directly
     */
    private FrontMenu findFrontMenu() {
        if (frontMenu != null) {
            return frontMenu;
        }
        
        // Try to find the FrontMenu controller in the scene
        Parent currentParent = backButton.getScene().getRoot();
        if (currentParent.getProperties().containsKey("controller")) {
            Object controller = currentParent.getProperties().get("controller");
            if (controller instanceof FrontMenu) {
                frontMenu = (FrontMenu) controller;
                return frontMenu;
            }
        }
        
        // If we can't find it, reload the FrontMenu
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Menu/front_menu.fxml"));
            Parent root = loader.load();
            frontMenu = loader.getController();
            frontMenu.setCurrentUserId(teacherId);
            
            // Replace the entire scene
            Scene scene = new Scene(root);
            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(scene);
            
            return frontMenu;
        } catch (IOException e) {
            showError("Error loading navigation: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * View course details using the FrontMenu
     */
    private void viewCourse(Course course) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Teacher/course_details.fxml"));
            Parent detailsContent = loader.load();
            
            TeacherCourseDetailsController controller = loader.getController();
            controller.setCourse(course);
            controller.setViewMode(true);
            controller.setTeacherId(teacherId);
            
            // Add a callback for returning to the courses list
            controller.setOnCancelCallback(() -> {
                try {
                    // Reload this screen to refresh data
                    FXMLLoader reloader = new FXMLLoader(getClass().getResource("/Teacher/manage_courses.fxml"));
                    Parent coursesContent = reloader.load();
                    TeacherCoursesController coursesController = reloader.getController();
                    coursesController.setTeacherId(teacherId);
                    
                    // Load content in the FrontMenu
                    FrontMenu menu = findFrontMenu();
                    if (menu != null) {
                        menu.loadCustomContent(coursesContent);
                    }
                } catch (IOException e) {
                    showError("Error returning to courses: " + e.getMessage());
                }
            });
            
            // Load the details in the FrontMenu
            FrontMenu menu = findFrontMenu();
            if (menu != null) {
                menu.loadCustomContent(detailsContent);
            }
        } catch (IOException e) {
            showError("Error opening course details: " + e.getMessage());
        }
    }
    
    /**
     * Edit a course using the FrontMenu
     */
    private void editCourse(Course course) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Teacher/course_details.fxml"));
            Parent editContent = loader.load();
            
            TeacherCourseDetailsController controller = loader.getController();
            controller.setCourse(course);
            controller.setViewMode(false);
            controller.setTeacherId(teacherId);
            
            // Add callbacks for saving and canceling
            controller.setOnSaveCallback(() -> {
                try {
                    // Reload this screen to refresh data
                    FXMLLoader reloader = new FXMLLoader(getClass().getResource("/Teacher/manage_courses.fxml"));
                    Parent coursesContent = reloader.load();
                    TeacherCoursesController coursesController = reloader.getController();
                    coursesController.setTeacherId(teacherId);
                    
                    // Load content in the FrontMenu
                    FrontMenu menu = findFrontMenu();
                    if (menu != null) {
                        menu.loadCustomContent(coursesContent);
                    }
                } catch (IOException e) {
                    showError("Error returning to courses: " + e.getMessage());
                }
            });
            
            controller.setOnCancelCallback(() -> {
                try {
                    // Reload this screen without saving
                    FXMLLoader reloader = new FXMLLoader(getClass().getResource("/Teacher/manage_courses.fxml"));
                    Parent coursesContent = reloader.load();
                    TeacherCoursesController coursesController = reloader.getController();
                    coursesController.setTeacherId(teacherId);
                    
                    // Load content in the FrontMenu
                    FrontMenu menu = findFrontMenu();
                    if (menu != null) {
                        menu.loadCustomContent(coursesContent);
                    }
                } catch (IOException e) {
                    showError("Error returning to courses: " + e.getMessage());
                }
            });
            
            // Load the editor in the FrontMenu
            FrontMenu menu = findFrontMenu();
            if (menu != null) {
                menu.loadCustomContent(editContent);
            }
        } catch (IOException e) {
            showError("Error opening course editor: " + e.getMessage());
        }
    }
    
    /**
     * Delete a course
     */
    private void deleteCourse(Course course) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete Course");
        confirmation.setHeaderText("Delete \"" + course.getTitre() + "\"?");
        confirmation.setContentText("This action cannot be undone. All lessons and materials in this course will also be deleted.");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                courseService.deleteCourse(course.getId());
                showInfo("Course deleted successfully");
                loadTeacherCourses(); // Refresh the table
            } catch (SQLException e) {
                showError("Error deleting course: " + e.getMessage());
            }
        }
    }
    
    /**
     * Add a new course using the FrontMenu
     */
    private void addNewCourse() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Teacher/course_details.fxml"));
            Parent createContent = loader.load();
            
            TeacherCourseDetailsController controller = loader.getController();
            controller.createNewCourse(teacherId);
            controller.setTeacherId(teacherId);
            
            // Add callbacks for saving and canceling
            controller.setOnSaveCallback(() -> {
                try {
                    // Reload this screen to refresh data
                    FXMLLoader reloader = new FXMLLoader(getClass().getResource("/Teacher/manage_courses.fxml"));
                    Parent coursesContent = reloader.load();
                    TeacherCoursesController coursesController = reloader.getController();
                    coursesController.setTeacherId(teacherId);
                    
                    // Load content in the FrontMenu
                    FrontMenu menu = findFrontMenu();
                    if (menu != null) {
                        menu.loadCustomContent(coursesContent);
                    }
                } catch (IOException e) {
                    showError("Error returning to courses: " + e.getMessage());
                }
            });
            
            controller.setOnCancelCallback(() -> {
                try {
                    // Reload this screen without saving
                    FXMLLoader reloader = new FXMLLoader(getClass().getResource("/Teacher/manage_courses.fxml"));
                    Parent coursesContent = reloader.load();
                    TeacherCoursesController coursesController = reloader.getController();
                    coursesController.setTeacherId(teacherId);
                    
                    // Load content in the FrontMenu
                    FrontMenu menu = findFrontMenu();
                    if (menu != null) {
                        menu.loadCustomContent(coursesContent);
                    }
                } catch (IOException e) {
                    showError("Error returning to courses: " + e.getMessage());
                }
            });
            
            // Load the creator in the FrontMenu
            FrontMenu menu = findFrontMenu();
            if (menu != null) {
                menu.loadCustomContent(createContent);
            }
        } catch (IOException e) {
            showError("Error opening course creator: " + e.getMessage());
        }
    }
    
    /**
     * Go back to the home page using the FrontMenu
     */
    private void backToHome() {
        try {
            // Get the FrontMenu and load the home content
            FrontMenu menu = findFrontMenu();
            if (menu != null) {
                // This will load the default home page
                menu.handleHomeButton();
            } else {
                // Fallback to direct scene replacement if necessary
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Menu/front_home.fxml"));
                Parent homeContent = loader.load();
                
                // Set the user ID in the home controller
                HomeController controller = loader.getController();
                controller.setCurrentUserId(teacherId);
                
                Scene scene = backButton.getScene();
                scene.setRoot(homeContent);
            }
        } catch (IOException e) {
            showError("Error returning to home: " + e.getMessage());
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