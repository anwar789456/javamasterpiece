package org.example.controller.menu;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import org.example.models.course.Course;
import org.example.models.interest.Interest;
import org.example.services.CourseService;
import org.example.services.InscriptionCoursService;
import org.example.services.InterestService;
import utils.dataSource;

public class CoursesDisplayController {
    
    @FXML
    private FlowPane coursesContainer;
    
    @FXML
    private StackPane interestsPopup;
    
    @FXML
    private VBox interestsContainer;
    
    @FXML
    private TextField interestSearchField;
    
    @FXML
    private Button interestsButton;
    
    // Method to support user ID from the front menu controller
    private int currentUserId = -1;
    
    private CourseService courseService;
    private InterestService interestService;
    private InscriptionCoursService inscriptionCoursService;
    private List<Interest> allInterests = new ArrayList<>();
    private Map<Integer, CheckBox> interestCheckboxes = new HashMap<>();
    private Map<Integer, Integer> courseLearnerCounts = new HashMap<>();
    
    @FXML
    private void initialize() {
        try {
            // Ensure proper styling
            ensureProperStylesheets();
            
            // Initialize services
            courseService = new CourseService();
            interestService = new InterestService();
            inscriptionCoursService = new InscriptionCoursService();
            
            // Set up the interests popup
            setupInterestsPopup();
            
            // Load learner counts for all courses
            try {
                courseLearnerCounts = inscriptionCoursService.getAllCoursesLearnerCounts();
            } catch (Exception e) {
                System.err.println("Error loading learner counts: " + e.getMessage());
                courseLearnerCounts = new HashMap<>();
            }
            
            // Load courses from database using service
            List<Course> courses;
            try {
                courses = courseService.getAllCourses();
                // Set learner count for each course
                for (Course course : courses) {
                    course.setLearnerCount(courseLearnerCounts.getOrDefault(course.getId(), 0));
                }
            } catch (Exception e) {
                // Fallback to direct query if service fails
                courses = getCoursesDirectly();
            }
            
            // Add course items to the container
            populateCourses(courses);
        } catch (SQLException e) {
            System.err.println("Error loading courses from database: " + e.getMessage());
            e.printStackTrace();
            // Show error message to user
            Label errorLabel = new Label("Could not load courses. Please try again later.");
            errorLabel.getStyleClass().add("error-message");
            coursesContainer.getChildren().add(errorLabel);
        }
    }
    
    /**
     * Sets up the interests popup with checkboxes and search functionality
     */
    private void setupInterestsPopup() {
        try {
            // Load all interests
            allInterests = interestService.getAllInterests();
            
            // Populate interests container
            populateInterestsContainer(allInterests);
            
            // Set up search field listener
            interestSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
                filterInterests(newValue);
            });
            
            // Make the popup initially invisible
            interestsPopup.setVisible(false);
            
            // Close popup when clicking outside of it
            interestsPopup.setOnMouseClicked(event -> {
                if (event.getTarget() == interestsPopup) {
                    closeInterestsPopup();
                }
            });
        } catch (SQLException e) {
            System.err.println("Error loading interests: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Populates the interests container with checkboxes
     */
    private void populateInterestsContainer(List<Interest> interests) {
        interestsContainer.getChildren().clear();
        interestCheckboxes.clear();
        
        if (interests.isEmpty()) {
            Label noInterestsLabel = new Label("No interests found");
            noInterestsLabel.getStyleClass().add("no-interests-message");
            interestsContainer.getChildren().add(noInterestsLabel);
            return;
        }
        
        for (Interest interest : interests) {
            try {
                // Get the count of courses for this interest
                int courseCount = interestService.getCoursesCountByInterest(interest.getId());
                
                // Create an HBox to hold the checkbox and count
                HBox interestBox = new HBox();
                interestBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                interestBox.setSpacing(5);
                
                // Create the checkbox with just the interest name
                CheckBox checkbox = new CheckBox(interest.getName());
                checkbox.getStyleClass().add("interest-checkbox");
                checkbox.setSelected(interest.isSelected());
                checkbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                    interest.setSelected(newVal);
                });
                
                // Create the course count label
                Label countLabel = new Label("(" + courseCount + ")");
                countLabel.getStyleClass().add("course-count");
                
                // Add both to the HBox
                interestBox.getChildren().addAll(checkbox, countLabel);
                
                // Store the checkbox for reference
                interestCheckboxes.put(interest.getId(), checkbox);
                
                // Add the HBox to the container
                interestsContainer.getChildren().add(interestBox);
            } catch (SQLException e) {
                System.err.println("Error getting course count for interest " + interest.getId() + ": " + e.getMessage());
                // Fall back to just showing the interest without count
                CheckBox checkbox = new CheckBox(interest.getName());
                checkbox.getStyleClass().add("interest-checkbox");
                checkbox.setSelected(interest.isSelected());
                checkbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                    interest.setSelected(newVal);
                });
                
                interestCheckboxes.put(interest.getId(), checkbox);
                interestsContainer.getChildren().add(checkbox);
            }
        }
    }
    
    /**
     * Filters interests in the popup by search term
     */
    private void filterInterests(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            // Show all interests
            populateInterestsContainer(allInterests);
            return;
        }
        
        // Filter interests by name
        String term = searchTerm.toLowerCase().trim();
        List<Interest> filteredInterests = allInterests.stream()
            .filter(interest -> interest.getName().toLowerCase().contains(term))
            .collect(Collectors.toList());
        
        populateInterestsContainer(filteredInterests);
    }
    
    /**
     * Handles the interests button click to show/hide the popup
     */
    @FXML
    private void handleInterestsButtonClick() {
        if (interestsPopup.isVisible()) {
            closeInterestsPopup();
        } else {
            interestsPopup.setVisible(true);
        }
    }
    
    /**
     * Closes the interests popup
     */
    @FXML
    private void closeInterestsPopup() {
        interestsPopup.setVisible(false);
    }
    
    /**
     * Applies the interests filter and updates the course list
     */
    @FXML
    private void applyInterestsFilter() {
        try {
            // Get selected interest IDs
            List<Integer> selectedInterestIds = allInterests.stream()
                .filter(Interest::isSelected)
                .map(Interest::getId)
                .collect(Collectors.toList());
            
            // Clear existing content
            coursesContainer.getChildren().clear();
            
            if (selectedInterestIds.isEmpty()) {
                // If no interests selected, show all courses
                List<Course> allCourses = courseService.getAllCourses();
                // Set learner count for each course
                for (Course course : allCourses) {
                    course.setLearnerCount(courseLearnerCounts.getOrDefault(course.getId(), 0));
                }
                populateCourses(allCourses);
            } else {
                // Get courses filtered by interests
                List<Course> filteredCourses = interestService.getCoursesByInterests(selectedInterestIds);
                
                // Set learner count for each course
                for (Course course : filteredCourses) {
                    course.setLearnerCount(courseLearnerCounts.getOrDefault(course.getId(), 0));
                }
                
                if (filteredCourses.isEmpty()) {
                    Label noCourses = new Label("No courses found for the selected interests");
                    noCourses.getStyleClass().add("no-courses-message");
                    coursesContainer.getChildren().add(noCourses);
                } else {
                    populateCourses(filteredCourses);
                }
            }
            
            // Close the popup
            closeInterestsPopup();
            
        } catch (SQLException e) {
            System.err.println("Error applying interests filter: " + e.getMessage());
            e.printStackTrace();
            
            Label errorLabel = new Label("Could not filter courses by interests. Please try again.");
            errorLabel.getStyleClass().add("error-message");
            coursesContainer.getChildren().add(errorLabel);
        }
    }
    
    private List<Course> getCoursesDirectly() throws SQLException {
        List<Course> courses = new ArrayList<>();
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            // Get database connection
            connection = dataSource.getInstance().getConnection();
            
            // Prepare SQL query
            String query = "SELECT id, idowner_id, titre, price, img, description, is_free, datecreation, likes, dislikes, minimal_hours, maximal_hours " +
                          "FROM cours " +
                          "ORDER BY datecreation DESC";
            
            statement = connection.prepareStatement(query);
            resultSet = statement.executeQuery();
            
            // Process results
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                int idowner_id = resultSet.getInt("idowner_id");
                String titre = resultSet.getString("titre");
                double price = resultSet.getDouble("price");
                String img = resultSet.getString("img");
                String description = resultSet.getString("description");
                boolean is_free = resultSet.getBoolean("is_free");
                int likes = resultSet.getInt("likes");
                int dislikes = resultSet.getInt("dislikes");
                int minimal_hours = resultSet.getInt("minimal_hours");
                int maximal_hours = resultSet.getInt("maximal_hours");
                
                // Parse date
                LocalDateTime datecreation = null;
                try {
                    datecreation = resultSet.getTimestamp("datecreation").toLocalDateTime();
                } catch (Exception e) {
                    System.err.println("Error parsing date for course ID " + id + ": " + e.getMessage());
                    datecreation = LocalDateTime.now(); // Fallback
                }
                
                // Create course object using the existing model
                Course course = new Course(
                    id, 
                    idowner_id, 
                    titre, 
                    price, 
                    img, 
                    description, 
                    is_free, 
                    datecreation,
                    likes,
                    dislikes,
                    minimal_hours,
                    maximal_hours
                );
                
                // Set the learner count
                course.setLearnerCount(courseLearnerCounts.getOrDefault(id, 0));
                
                courses.add(course);
            }
            
            System.out.println("Loaded " + courses.size() + " courses from database directly");
            
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            throw e;
        } finally {
            // Close resources in reverse order
            if (resultSet != null) {
                try { resultSet.close(); } catch (SQLException e) { /* ignore */ }
            }
            if (statement != null) {
                try { statement.close(); } catch (SQLException e) { /* ignore */ }
            }
            if (connection != null) {
                try { 
                    dataSource.getInstance().releaseConnection(connection);
                } catch (Exception e) { 
                    System.err.println("Error releasing connection: " + e.getMessage());
                }
            }
        }
        
        return courses;
    }
    
    private void populateCourses(List<Course> courses) {
        if (courses.isEmpty()) {
            // Show message if no courses are available
            Label noCourseLabel = new Label("No courses available at the moment. Check back later!");
            noCourseLabel.getStyleClass().add("no-courses-message");
            noCourseLabel.setWrapText(true);
            coursesContainer.getChildren().add(noCourseLabel);
            return;
        }
        
        for (Course course : courses) {
            VBox courseCard = createCourseCard(course);
            coursesContainer.getChildren().add(courseCard);
        }
    }
    
    private VBox createCourseCard(Course course) {
        // Create course card container
        VBox card = new VBox();
        card.getStyleClass().add("course-card");
        card.setPrefWidth(300);
        card.setMaxWidth(300);
        card.setPrefHeight(320);
        card.setSpacing(10);
        card.setPadding(new Insets(0));
        
        // Make the entire card clickable to navigate to course details
        card.setOnMouseClicked(event -> {
            openCourseDetails(course);
        });
        
        // Add drop shadow effect
        DropShadow shadow = new DropShadow();
        shadow.setRadius(10.0);
        shadow.setOffsetX(0.0);
        shadow.setOffsetY(2.0);
        shadow.setColor(Color.color(0, 0, 0, 0.2));
        card.setEffect(shadow);
        
        // Course image
        ImageView imageView = new ImageView();
        try {
            // Try to load the image from the stored path
            String imagePath = course.getImg();
            if (imagePath != null && !imagePath.isEmpty()) {
                // Check if the path seems to be a URL or a file path
                if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                    // Load from URL
                    Image image = new Image(imagePath, true); // true enables background loading
                    imageView.setImage(image);
                } else {
                    // Try as local file path - could be relative or absolute
                    try {
                        Image image = new Image("file:" + imagePath);
                        imageView.setImage(image);
                    } catch (Exception e) {
                        // Try as classpath resource
                        Image image = new Image(getClass().getResourceAsStream(imagePath));
                        imageView.setImage(image);
                    }
                }
            } else {
                throw new Exception("Image path is empty or null");
            }
        } catch (Exception e) {
            System.err.println("Error loading image for course " + course.getId() + ": " + e.getMessage());
            // Fall back to placeholder
            try {
                // Look for a category-specific placeholder based on title keywords
                String title = course.getTitre().toLowerCase();
                String placeholderPath = "/images/course_placeholder.png";
                
                // Try to match course with appropriate placeholder
                if (title.contains("java")) {
                    placeholderPath = "/images/course_java.png";
                } else if (title.contains("web") || title.contains("html") || title.contains("css")) {
                    placeholderPath = "/images/course_web.png";
                } else if (title.contains("database") || title.contains("sql")) {
                    placeholderPath = "/images/course_db.png";
                } else if (title.contains("mobile") || title.contains("android") || title.contains("ios")) {
                    placeholderPath = "/images/course_mobile.png";
                } else if (title.contains("python")) {
                    placeholderPath = "/images/course_python.png";
                } else if (title.contains("machine") || title.contains("learn")) {
                    placeholderPath = "/images/course_ml.png";
                }
                
                Image placeholder = new Image(getClass().getResourceAsStream(placeholderPath));
                imageView.setImage(placeholder);
            } catch (Exception ex) {
                System.err.println("Failed to load placeholder image: " + ex.getMessage());
            }
        }
        
        imageView.setFitWidth(300);
        imageView.setFitHeight(160);
        imageView.setPreserveRatio(false);
        
        // Course content container
        VBox contentBox = new VBox();
        contentBox.setSpacing(8);
        contentBox.setPadding(new Insets(10, 15, 15, 15));
        
        // Course title
        Label titleLabel = new Label(course.getTitre());
        titleLabel.getStyleClass().add("course-title");
        titleLabel.setWrapText(true);
        
        // Course description - limit to a reasonable preview
        String descriptionPreview = course.getDescription();
        if (descriptionPreview != null && descriptionPreview.length() > 100) {
            descriptionPreview = descriptionPreview.substring(0, 97) + "...";
        }
        
        Label descLabel = new Label(descriptionPreview);
        descLabel.getStyleClass().add("course-description");
        descLabel.setWrapText(true);
        descLabel.setMaxHeight(60);
        
        // Course duration display
        HBox durationBox = new HBox();
        durationBox.setSpacing(5);
        durationBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        durationBox.setPadding(new Insets(3, 0, 3, 0));
        
        Label clockIcon = new Label("🕒");
        clockIcon.setStyle("-fx-font-size: 14px;");
        
        // Create duration text
        String durationText = course.getMinimal_hours() + "-" + course.getMaximal_hours() + " hrs";
        Label durationLabel = new Label(durationText);
        durationLabel.getStyleClass().add("course-duration");
        
        // Add spacing between duration and learner count
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Learner count display
        Label mortarboardIcon = new Label("🎓");
        mortarboardIcon.setStyle("-fx-font-size: 14px;");
        
        Label learnerCountLabel = new Label(String.valueOf(course.getLearnerCount()));
        learnerCountLabel.getStyleClass().add("course-learners");
        
        durationBox.getChildren().addAll(clockIcon, durationLabel, spacer, mortarboardIcon, learnerCountLabel);
        
        // Try to load course interests
        try {
            List<Interest> courseInterests = interestService.getInterestsByCourse(course.getId());
            if (!courseInterests.isEmpty()) {
                HBox tagsBox = new HBox();
                tagsBox.setSpacing(5);
                tagsBox.setPadding(new Insets(0, 0, 5, 0));
                
                for (Interest interest : courseInterests.subList(0, Math.min(3, courseInterests.size()))) {
                    Label tag = new Label(interest.getName());
                    tag.getStyleClass().add("interest-tag");
                    tagsBox.getChildren().add(tag);
                }
                
                if (courseInterests.size() > 3) {
                    Label moreTag = new Label("+" + (courseInterests.size() - 3));
                    moreTag.getStyleClass().add("more-tag");
                    tagsBox.getChildren().add(moreTag);
                }
                
                contentBox.getChildren().add(tagsBox);
            }
        } catch (SQLException e) {
            // Silently ignore errors in loading interests
            System.err.println("Could not load interests for course " + course.getId() + ": " + e.getMessage());
        }
        
        // Likes and dislikes section
        HBox ratingsBox = new HBox();
        ratingsBox.setSpacing(15);
        ratingsBox.setPadding(new Insets(5, 0, 5, 0));
        
        // Likes display
        HBox likesBox = new HBox();
        likesBox.setSpacing(5);
        likesBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label thumbsUpIcon = new Label("👍");
        thumbsUpIcon.setStyle("-fx-font-size: 14px;");
        
        Label likesCount = new Label(String.valueOf(course.getLikes()));
        likesCount.getStyleClass().add("likes-count");
        
        likesBox.getChildren().addAll(thumbsUpIcon, likesCount);
        
        // Dislikes display
        HBox dislikesBox = new HBox();
        dislikesBox.setSpacing(5);
        dislikesBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label thumbsDownIcon = new Label("👎");
        thumbsDownIcon.setStyle("-fx-font-size: 14px;");
        
        Label dislikesCount = new Label(String.valueOf(course.getDislikes()));
        dislikesCount.getStyleClass().add("dislikes-count");
        
        dislikesBox.getChildren().addAll(thumbsDownIcon, dislikesCount);
        
        ratingsBox.getChildren().addAll(likesBox, dislikesBox);
        HBox.setHgrow(ratingsBox, Priority.ALWAYS);
        
        // Price/Enroll section
        HBox priceBox = new HBox();
        priceBox.setSpacing(10);
        priceBox.setPadding(new Insets(5, 0, 0, 0));
        priceBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        
        // Button with price or "Start for free" or "Already Enrolled"
        Button enrollButton = new Button();
        enrollButton.getStyleClass().add("course-button");
        
        // Check if the user is enrolled in this course
        boolean isEnrolled = false;
        if (currentUserId > 0) {
            try {
                isEnrolled = inscriptionCoursService.isUserEnrolled(currentUserId, course.getId());
                System.out.println("User " + currentUserId + " enrollment in course " + course.getId() + ": " + isEnrolled);
            } catch (SQLException e) {
                System.err.println("Error checking enrollment status for course card: " + e.getMessage());
            }
        }
        
        if (isEnrolled) {
            // User is already enrolled
            enrollButton.setText("Continue Learning");
            enrollButton.getStyleClass().add("enrolled-course-button");
            
            // Create a container for the enrolled label with background
            HBox enrolledLabelBox = new HBox();
            enrolledLabelBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            enrolledLabelBox.getStyleClass().add("enrolled-label-box");
            enrolledLabelBox.setPadding(new Insets(3, 8, 3, 8));
            enrolledLabelBox.setStyle("-fx-background-color: #e8f5e9; -fx-background-radius: 4px; -fx-border-color: #28a745; -fx-border-radius: 4px; -fx-border-width: 1px;");
            
            // Add a label indicating enrollment status - ADDING FIRST TO SHOW AT LEFT SIDE
            Label enrolledLabel = new Label("Already Enrolled");
            enrolledLabel.getStyleClass().add("enrolled-label");
            enrolledLabel.setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");
            
            // Add the label to its container
            enrolledLabelBox.getChildren().add(enrolledLabel);
            
            // Add the container to the price box
            priceBox.getChildren().add(enrolledLabelBox);
            
            // Add spacer to push button to the right
            Region priceBoxSpacer = new Region();
            HBox.setHgrow(priceBoxSpacer, Priority.ALWAYS);
            priceBox.getChildren().add(priceBoxSpacer);
            
            // Add button after the Already Enrolled label
            priceBox.getChildren().add(enrollButton);
        } else if (course.isIs_free()) {
            enrollButton.setText("Start for Free");
            enrollButton.getStyleClass().add("free-course-button");
            priceBox.getChildren().add(enrollButton);
        } else {
            enrollButton.setText("$" + String.format("%.2f", course.getPrice()));
            enrollButton.getStyleClass().add("paid-course-button");
            priceBox.getChildren().add(enrollButton);
        }
        
        // Change enrollButton action to navigate to course details
        enrollButton.setOnAction(event -> {
            openCourseDetails(course);
        });
        
        // Add all elements to the content box
        contentBox.getChildren().addAll(titleLabel, descLabel, durationBox, ratingsBox, priceBox);
        
        // Add image and content to card
        card.getChildren().addAll(imageView, contentBox);
        
        return card;
    }
    
    /**
     * Opens the course details page for the selected course
     */
    private void openCourseDetails(Course course) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/course/course_details.fxml"));
            Parent root = loader.load();
            
            // Get the controller and pass the course
            org.example.controller.course.CourseDetailsController controller = loader.getController();
            
            // Set course first to initialize the UI
            controller.setCourse(course);
            
            // Then set the user ID to check enrollment status and update UI accordingly
            if (currentUserId > 0) {
                controller.setCurrentUserId(currentUserId);
                System.out.println("Opening course details for user " + currentUserId);
            }
            
            // Manage CSS stylesheets properly
            Scene scene = coursesContainer.getScene();
            
            // Clear all existing stylesheets
            scene.getStylesheets().clear();
            
            // Add only the course details CSS to avoid stylesheet conflicts
            String courseDetailsCss = getClass().getResource("/css/course_details.css").toExternalForm();
            scene.getStylesheets().add(courseDetailsCss);
            
            System.out.println("Navigation: To course details, applied course_details.css only");
            
            // Set the new root
            scene.setRoot(root);
            
        } catch (IOException e) {
            System.err.println("Error loading course details: " + e.getMessage());
            e.printStackTrace();
            
            // Show error in alert
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Navigation Error");
            alert.setHeaderText("Could not open course details");
            alert.setContentText("An error occurred: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    /**
     * Updates the UI to reflect current user's enrollment status
     */
    private void updateCourseEnrollmentStatus() {
        System.out.println("Updating course enrollment status for user ID: " + currentUserId);
        
        // Refresh the courses to update enrollment status UI
        refreshCourses();
    }

    /**
     * Sets the current user ID
     * @param userId The ID of the current user
     */
    public void setCurrentUserId(int userId) {
        this.currentUserId = userId;
        System.out.println("Course display received user ID: " + userId);
        
        // Update UI to reflect enrollment status
        if (coursesContainer != null && coursesContainer.getScene() != null) {
            // Only refresh if the UI is already loaded
            updateCourseEnrollmentStatus();
        }
    }
    
    /**
     * Ensures proper stylesheet application
     */
    private void ensureProperStylesheets() {
        Scene scene = coursesContainer.getScene();
        if (scene != null) {
            // First, get all stylesheets that should be applied
            String menuCssPath = getClass().getResource("/css/menu.css").toExternalForm();
            
            // Clear all stylesheets and re-add only the ones we need
            // This ensures we have a clean slate and prevents any stylesheet conflicts
            scene.getStylesheets().clear();
            
            // Add the menu CSS which is required for the course list display
            scene.getStylesheets().add(menuCssPath);
            
            System.out.println("StyleSheet Reset: Applied menu.css");
        }
    }
    
    /**
     * Refreshes the course display with potentially updated data
     */
    @FXML
    public void refreshCourses() {
        try {
            // Ensure proper styling
            ensureProperStylesheets();
            
            // Clear existing content
            coursesContainer.getChildren().clear();
            
            // Refresh learner counts
            try {
                courseLearnerCounts = inscriptionCoursService.getAllCoursesLearnerCounts();
            } catch (Exception e) {
                System.err.println("Error refreshing learner counts: " + e.getMessage());
            }
            
            // Reload courses and repopulate
            List<Course> courses;
            try {
                courses = courseService.getAllCourses();
                // Set learner count for each course
                for (Course course : courses) {
                    course.setLearnerCount(courseLearnerCounts.getOrDefault(course.getId(), 0));
                }
            } catch (Exception e) {
                // Fallback to direct query
                courses = getCoursesDirectly();
            }
            
            populateCourses(courses);
        } catch (SQLException e) {
            System.err.println("Error refreshing courses: " + e.getMessage());
            e.printStackTrace();
            
            // Show error message
            Label errorLabel = new Label("Could not refresh courses. Please try again.");
            errorLabel.getStyleClass().add("error-message");
            coursesContainer.getChildren().add(errorLabel);
        }
    }
    
    /**
     * Filters courses to show only free courses
     */
    @FXML
    public void showFreeCourses() {
        try {
            // Ensure proper styling
            ensureProperStylesheets();
            
            // Clear existing content
            coursesContainer.getChildren().clear();
            
            // Get all courses
            List<Course> allCourses;
            try {
                allCourses = courseService.getAllCourses();
                // Set learner count for each course
                for (Course course : allCourses) {
                    course.setLearnerCount(courseLearnerCounts.getOrDefault(course.getId(), 0));
                }
            } catch (Exception e) {
                // Fallback to direct query
                allCourses = getCoursesDirectly();
            }
            
            // Filter for free courses
            List<Course> freeCourses = allCourses.stream()
                .filter(Course::isIs_free)
                .collect(Collectors.toList());
            
            // Show filtered results
            if (freeCourses.isEmpty()) {
                Label noFreeCourses = new Label("No free courses are currently available.");
                noFreeCourses.getStyleClass().add("no-courses-message");
                coursesContainer.getChildren().add(noFreeCourses);
            } else {
                populateCourses(freeCourses);
            }
            
        } catch (SQLException e) {
            System.err.println("Error filtering for free courses: " + e.getMessage());
            e.printStackTrace();
            
            // Show error message
            Label errorLabel = new Label("Could not filter courses. Please try again.");
            errorLabel.getStyleClass().add("error-message");
            coursesContainer.getChildren().add(errorLabel);
        }
    }
    
    /**
     * Filters courses to show only paid courses
     */
    @FXML
    public void showPaidCourses() {
        try {
            // Ensure proper styling
            ensureProperStylesheets();
            
            // Clear existing content
            coursesContainer.getChildren().clear();
            
            // Get all courses
            List<Course> allCourses;
            try {
                allCourses = courseService.getAllCourses();
                // Set learner count for each course
                for (Course course : allCourses) {
                    course.setLearnerCount(courseLearnerCounts.getOrDefault(course.getId(), 0));
                }
            } catch (Exception e) {
                // Fallback to direct query
                allCourses = getCoursesDirectly();
            }
            
            // Filter for paid courses
            List<Course> paidCourses = allCourses.stream()
                .filter(course -> !course.isIs_free())
                .collect(Collectors.toList());
            
            // Show filtered results
            if (paidCourses.isEmpty()) {
                Label noPaidCourses = new Label("No paid courses are currently available.");
                noPaidCourses.getStyleClass().add("no-courses-message");
                coursesContainer.getChildren().add(noPaidCourses);
            } else {
                populateCourses(paidCourses);
            }
            
        } catch (SQLException e) {
            System.err.println("Error filtering for paid courses: " + e.getMessage());
            e.printStackTrace();
            
            // Show error message
            Label errorLabel = new Label("Could not filter courses. Please try again.");
            errorLabel.getStyleClass().add("error-message");
            coursesContainer.getChildren().add(errorLabel);
        }
    }
} 