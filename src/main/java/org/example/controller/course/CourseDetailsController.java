package org.example.controller.course;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import org.example.models.course.Course;
import org.example.models.course.Review;
import org.example.models.lesson.Lesson;
import org.example.models.user.User;
import org.example.services.CourseService;
import org.example.services.InscriptionCoursService;
import org.example.services.LessonService;
import org.example.services.ReviewService;
import org.example.services.SentimentAnalysisService;
import org.example.services.UserService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class CourseDetailsController {

    @FXML
    private Label courseTitle;
    
    @FXML
    private Label courseSubtitle;
    
    @FXML
    private ImageView courseImage;
    
    @FXML
    private Label learnersLabel;
    
    @FXML
    private Label likesLabel;
    
    @FXML
    private Label dislikesLabel;
    
    @FXML
    private Label durationLabel;
    
    @FXML
    private Label descriptionLabel;
    
    @FXML
    private Label priceLabel;
    
    @FXML
    private Button startLearningButton;
    
    @FXML
    private ListView<Lesson> lessonsListView;
    
    @FXML
    private ImageView publisherImage;
    
    @FXML
    private Label publisherName;
    
    @FXML
    private Label publisherInfo;
    
    // New fields for the review section
    @FXML
    private VBox reviewsContainer;
    
    @FXML
    private VBox addReviewContainer;
    
    @FXML
    private TextArea reviewTextArea;
    
    @FXML
    private ToggleGroup ratingToggleGroup;
    
    @FXML
    private RadioButton likeRadioButton;
    
    @FXML
    private RadioButton dislikeRadioButton;
    
    @FXML
    private Button submitReviewButton;
    
    @FXML
    private Label reviewStatusLabel;
    
    @FXML
    private ScrollPane reviewsScrollPane;
    
    @FXML
    private Label reviewSectionTitle;
    
    private Course currentCourse;
    private CourseService courseService;
    private LessonService lessonService;
    private InscriptionCoursService inscriptionCoursService;
    private ReviewService reviewService;
    private SentimentAnalysisService sentimentService = new SentimentAnalysisService();
    private UserService userService;
    private int currentUserId = -1;
    private Review userExistingReview;
    
    @FXML
    public void initialize() {
        courseService = new CourseService();
        lessonService = new LessonService();
        inscriptionCoursService = new InscriptionCoursService();
        reviewService = new ReviewService();
        userService = new UserService();
        
        // Test sentiment analysis
        testSentimentAnalysis();
        
        // Ensure course details stylesheets are properly applied
        Scene scene = startLearningButton.getScene();
        if (scene != null) {
            // Ensure the course details CSS is applied
            String courseDetailsCssPath = getClass().getResource("/css/course_details.css").toExternalForm();
            if (!scene.getStylesheets().contains(courseDetailsCssPath)) {
                scene.getStylesheets().add(courseDetailsCssPath);
            }
            
            System.out.println("Course Details: Applied course_details.css");
        }
        
        // Configure lessons list view
        lessonsListView.setCellFactory(param -> new ListCell<Lesson>() {
            @Override
            protected void updateItem(Lesson lesson, boolean empty) {
                super.updateItem(lesson, empty);
                
                if (empty || lesson == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(lesson.getOrdre() + ". " + lesson.getTitre());
                }
            }
        });
        
        // Initialize review components
        if (ratingToggleGroup == null) {
            ratingToggleGroup = new ToggleGroup();
            if (likeRadioButton != null && dislikeRadioButton != null) {
                likeRadioButton.setToggleGroup(ratingToggleGroup);
                dislikeRadioButton.setToggleGroup(ratingToggleGroup);
                likeRadioButton.setSelected(true);
            }
        }
    }
    
    /**
     * Test the sentiment analysis service
     */
    private void testSentimentAnalysis() {
        try {
            System.out.println("Testing sentiment analysis service...");
            String testText = "I love this course! It's amazing!";
            SentimentAnalysisService.Sentiment sentiment = sentimentService.analyzeSentiment(testText);
            System.out.println("Test sentiment result: " + sentiment);
            
            // Test a negative sentiment
            testText = "I hate this course. It's terrible.";
            sentiment = sentimentService.analyzeSentiment(testText);
            System.out.println("Test negative sentiment result: " + sentiment);
        } catch (Exception e) {
            System.err.println("Sentiment analysis test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Updates the UI elements based on the user's enrollment status
     */
    private void updateEnrollmentStatus() {
        // Check if user is enrolled
        boolean isEnrolled = false;
        if (currentUserId > 0 && currentCourse != null) {
            try {
                isEnrolled = inscriptionCoursService.isUserEnrolled(currentUserId, currentCourse.getId());
                System.out.println("Enrollment check for user " + currentUserId + 
                    " in course " + currentCourse.getId() + ": " + (isEnrolled ? "Enrolled" : "Not enrolled"));
            } catch (SQLException e) {
                System.err.println("Error checking enrollment status: " + e.getMessage());
            }
        }
        
        // Set price and button text based on enrollment status
        if (isEnrolled) {
            // User is already enrolled
            priceLabel.setText("Already Enrolled");
            priceLabel.setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");
            startLearningButton.setText("Continue Learning");
            startLearningButton.getStyleClass().remove("start-learning-button");
            startLearningButton.getStyleClass().add("continue-learning-button");
        } else {
            // User is not enrolled
            if (currentCourse != null && currentCourse.isIs_free()) {
                priceLabel.setText("Free");
                startLearningButton.setText("Start Learning");
                startLearningButton.getStyleClass().remove("continue-learning-button");
                startLearningButton.getStyleClass().add("start-learning-button");
            } else if (currentCourse != null) {
                priceLabel.setText("$" + String.format("%.2f", currentCourse.getPrice()));
                startLearningButton.setText("Purchase Course");
                startLearningButton.getStyleClass().remove("continue-learning-button");
                startLearningButton.getStyleClass().add("start-learning-button");
            }
        }
    }
    
    /**
     * Sets the course to be displayed and updates the UI
     */
    public void setCourse(Course course) {
        this.currentCourse = course;
        
        // Update UI elements with course data
        courseTitle.setText(course.getTitre());
        
        // Set subtitle (using first sentence of description or a default)
        String description = course.getDescription();
        int firstSentenceEnd = description.indexOf('.');
        if (firstSentenceEnd > 0) {
            courseSubtitle.setText(description.substring(0, firstSentenceEnd + 1));
        } else {
            courseSubtitle.setText("This online course will help you master new skills and advance your career.");
        }
        
        try {
            // Load course image
            String imagePath = course.getImg();
            System.out.println("Course image path: " + imagePath);
            if (imagePath != null && !imagePath.isEmpty()) {
                boolean imageLoaded = false;
                
                // Method 1: Try direct URL
                if (!imageLoaded) {
                    try {
                        System.out.println("Trying direct URL: " + imagePath);
                        Image image = new Image(imagePath);
                        courseImage.setImage(image);
                        System.out.println("Success: Direct URL loading");
                        imageLoaded = true;
                    } catch (Exception e) {
                        System.out.println("Failed: Direct URL loading - " + e.getMessage());
                    }
                }
                
                // Method 2: Try as absolute file path
                if (!imageLoaded) {
                    try {
                        String filePath = "file:" + imagePath;
                        System.out.println("Trying absolute file path: " + filePath);
                        Image image = new Image(filePath);
                        courseImage.setImage(image);
                        System.out.println("Success: Absolute file path loading");
                        imageLoaded = true;
                    } catch (Exception e) {
                        System.out.println("Failed: Absolute file path loading - " + e.getMessage());
                    }
                }
                
                // Method 3: Try as a path relative to user.dir
                if (!imageLoaded) {
                    try {
                        String userDirPath = "file:" + System.getProperty("user.dir") + "/" + imagePath;
                        System.out.println("Trying user.dir relative path: " + userDirPath);
                        Image image = new Image(userDirPath);
                        courseImage.setImage(image);
                        System.out.println("Success: user.dir relative path loading");
                        imageLoaded = true;
                    } catch (Exception e) {
                        System.out.println("Failed: user.dir relative path loading - " + e.getMessage());
                    }
                }
                
                // Method 4: Try in images subfolder of user.dir
                if (!imageLoaded) {
                    try {
                        String imagesFolderPath = "file:" + System.getProperty("user.dir") + "/images/" + imagePath;
                        System.out.println("Trying images folder path: " + imagesFolderPath);
                        Image image = new Image(imagesFolderPath);
                        courseImage.setImage(image);
                        System.out.println("Success: images folder path loading");
                        imageLoaded = true;
                    } catch (Exception e) {
                        System.out.println("Failed: images folder path loading - " + e.getMessage());
                    }
                }
                
                // Method 5: Try as a classpath resource
                if (!imageLoaded) {
                    try {
                        System.out.println("Trying classpath resource: " + imagePath);
                        Image image = new Image(getClass().getResourceAsStream(imagePath));
                        courseImage.setImage(image);
                        System.out.println("Success: classpath resource loading");
                        imageLoaded = true;
                    } catch (Exception e) {
                        System.out.println("Failed: classpath resource loading - " + e.getMessage());
                    }
                }
                
                // Method 6: Try as a classpath resource with /images prefix
                if (!imageLoaded) {
                    try {
                        String resourcePath = "/images/" + imagePath;
                        System.out.println("Trying classpath resource with /images prefix: " + resourcePath);
                        Image image = new Image(getClass().getResourceAsStream(resourcePath));
                        courseImage.setImage(image);
                        System.out.println("Success: classpath resource with /images prefix loading");
                        imageLoaded = true;
                    } catch (Exception e) {
                        System.out.println("Failed: classpath resource with /images prefix loading - " + e.getMessage());
                    }
                }
                
                // If all methods failed, use placeholder
                if (!imageLoaded) {
                    System.out.println("All image loading methods failed. Using placeholder.");
                    loadPlaceholderImage();
                }
            } else {
                loadPlaceholderImage();
            }
        } catch (Exception e) {
            System.err.println("Error loading course image: " + e.getMessage());
            loadPlaceholderImage();
        }
        
        // Set course stats
        learnersLabel.setText(String.valueOf(course.getLearnerCount()));
        likesLabel.setText(String.valueOf(course.getLikes()));
        dislikesLabel.setText(String.valueOf(course.getDislikes()));
        
        // Set duration
        durationLabel.setText(course.getMinimal_hours() + "-" + course.getMaximal_hours() + " hours");
        
        // Set full description
        descriptionLabel.setText(course.getDescription());
        
        // Set publisher info - fetch from user table
        try {
            User publisher = userService.getUserById(course.getIdowner_id());
            if (publisher != null) {
                // Set publisher name from user table
                publisherName.setText(publisher.getName());
                publisherInfo.setText("Course by " + publisher.getName());
                
                // Set publisher image from user's image_url
                try {
                    String imageUrl = publisher.getImageUrl();
                    System.out.println("Publisher image URL: " + imageUrl);
                    try {
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            boolean imageLoaded = false;
                            
                            // Method 1: Try direct URL
                            if (!imageLoaded) {
                                try {
                                    System.out.println("Trying direct URL: " + imageUrl);
                                    Image pubImage = new Image(imageUrl);
                                    publisherImage.setImage(pubImage);
                                    System.out.println("Success: Direct URL loading");
                                    imageLoaded = true;
                                } catch (Exception e) {
                                    System.out.println("Failed: Direct URL loading - " + e.getMessage());
                                }
                            }
                        
                            // Method 2: Try as absolute file path
                            if (!imageLoaded) {
                                try {
                                    String filePath = "file:" + imageUrl;
                                    System.out.println("Trying absolute file path: " + filePath);
                                    Image pubImage = new Image(filePath);
                                    publisherImage.setImage(pubImage);
                                    System.out.println("Success: Absolute file path loading");
                                    imageLoaded = true;
                                } catch (Exception e) {
                                    System.out.println("Failed: Absolute file path loading - " + e.getMessage());
                                }
                            }
                        
                            // Method 3: Try as a path relative to user.dir
                            if (!imageLoaded) {
                                try {
                                    String userDirPath = "file:" + System.getProperty("user.dir") + "/" + imageUrl;
                                    System.out.println("Trying user.dir relative path: " + userDirPath);
                                    Image pubImage = new Image(userDirPath);
                                    publisherImage.setImage(pubImage);
                                    System.out.println("Success: user.dir relative path loading");
                                    imageLoaded = true;
                                } catch (Exception e) {
                                    System.out.println("Failed: user.dir relative path loading - " + e.getMessage());
                                }
                            }
                        
                            // Method 4: Try in images subfolder of user.dir
                            if (!imageLoaded) {
                                try {
                                    String imagesFolderPath = "file:" + System.getProperty("user.dir") + "/images/" + imageUrl;
                                    System.out.println("Trying images folder path: " + imagesFolderPath);
                                    Image pubImage = new Image(imagesFolderPath);
                                    publisherImage.setImage(pubImage);
                                    System.out.println("Success: images folder path loading");
                                    imageLoaded = true;
                                } catch (Exception e) {
                                    System.out.println("Failed: images folder path loading - " + e.getMessage());
                                }
                            }
                        
                            // Method 5: Try as a classpath resource
                            if (!imageLoaded) {
                                try {
                                    System.out.println("Trying classpath resource: " + imageUrl);
                                    Image pubImage = new Image(getClass().getResourceAsStream(imageUrl));
                                    publisherImage.setImage(pubImage);
                                    System.out.println("Success: classpath resource loading");
                                    imageLoaded = true;
                                } catch (Exception e) {
                                    System.out.println("Failed: classpath resource loading - " + e.getMessage());
                                }
                            }
                        
                            // Method 6: Try as a classpath resource with /images prefix
                            if (!imageLoaded) {
                                try {
                                    String resourcePath = "/images/" + imageUrl;
                                    System.out.println("Trying classpath resource with /images prefix: " + resourcePath);
                                    Image pubImage = new Image(getClass().getResourceAsStream(resourcePath));
                                    publisherImage.setImage(pubImage);
                                    System.out.println("Success: classpath resource with /images prefix loading");
                                    imageLoaded = true;
                                } catch (Exception e) {
                                    System.out.println("Failed: classpath resource with /images prefix loading - " + e.getMessage());
                                }
                            }
                        
                            // Apply circular clipping to publisher image if loaded
                            if (imageLoaded) {
                                double radius = publisherImage.getFitWidth() / 2;
                                publisherImage.setClip(new javafx.scene.shape.Circle(radius, radius, radius));
                            }
                            // If all methods failed, use default image
                            else {
                                System.out.println("Publisher image URL is null or empty. Using default image.");
                                try {
                                    Image defaultPubImage = new Image(getClass().getResourceAsStream("/images/publisher_icon.png"));
                                    publisherImage.setImage(defaultPubImage);
                                    
                                    // Apply circular clipping to default image
                                    double radius = publisherImage.getFitWidth() / 2;
                                    publisherImage.setClip(new javafx.scene.shape.Circle(radius, radius, radius));
                                } catch (Exception ex) {
                                    System.err.println("Could not load default publisher image: " + ex.getMessage());
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error loading publisher image: " + e.getMessage());
                        try {
                            // Load default publisher image
                            Image defaultPubImage = new Image(getClass().getResourceAsStream("/images/publisher_icon.png"));
                            publisherImage.setImage(defaultPubImage);
                            
                            // Apply circular clipping to default image
                            double radius = publisherImage.getFitWidth() / 2;
                            publisherImage.setClip(new javafx.scene.shape.Circle(radius, radius, radius));
                        } catch (Exception ex) {
                            System.err.println("Could not load default publisher image: " + ex.getMessage());
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error loading publisher image: " + e.getMessage());
                    try {
                        // Load default publisher image
                        Image defaultPubImage = new Image(getClass().getResourceAsStream("/images/publisher_icon.png"));
                        publisherImage.setImage(defaultPubImage);
                        
                        // Apply circular clipping to default image
                        double radius = publisherImage.getFitWidth() / 2;
                        publisherImage.setClip(new javafx.scene.shape.Circle(radius, radius, radius));
                    } catch (Exception ex) {
                        System.err.println("Could not load default publisher image: " + ex.getMessage());
                    }
                }
            } else {
                // Fallback if publisher not found
                String publisherNameText = "Course Provider";
                if (course.getIdowner_id() > 0) {
                    publisherNameText = "Course Provider #" + course.getIdowner_id();
                }
                publisherName.setText(publisherNameText);
                publisherInfo.setText("Course by " + publisherNameText);
                
                // Load default publisher image
                try {
                    Image defaultPubImage = new Image(getClass().getResourceAsStream("/images/publisher_icon.png"));
                    publisherImage.setImage(defaultPubImage);
                    
                    // Apply circular clipping to default image
                    double radius = publisherImage.getFitWidth() / 2;
                    publisherImage.setClip(new javafx.scene.shape.Circle(radius, radius, radius));
                } catch (Exception ex) {
                    System.err.println("Could not load default publisher image: " + ex.getMessage());
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching publisher details: " + e.getMessage());
            // Fallback to using ID as reference
            String publisherNameText = "Course Provider";
            if (course.getIdowner_id() > 0) {
                publisherNameText = "Course Provider #" + course.getIdowner_id();
            }
            publisherName.setText(publisherNameText);
            publisherInfo.setText("Course by " + publisherNameText);
            
            // Load default publisher image
            try {
                Image publisherImg = new Image(getClass().getResourceAsStream("/images/publisher_icon.png"));
                publisherImage.setImage(publisherImg);
            } catch (Exception ex) {
                System.err.println("Could not load publisher default image: " + ex.getMessage());
            }
        }
        
        // Set price label based on course type (free or paid)
        if (course.isIs_free()) {
            priceLabel.setText("Free");
            priceLabel.setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");
        } else {
            priceLabel.setText("$" + String.format("%.2f", course.getPrice()));
        }
        
        // Load lessons for this course
        loadLessons();
        
        // Check enrollment status and update UI accordingly
        updateEnrollmentStatus();
        
        // Initialize the review section
        initializeReviewSection();
    }
    
    /**
     * Loads a placeholder image when the course image is not available
     */
    private void loadPlaceholderImage() {
        try {
            // Look for a category-specific placeholder based on title keywords
            String title = currentCourse.getTitre().toLowerCase();
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
            courseImage.setImage(placeholder);
        } catch (Exception ex) {
            System.err.println("Failed to load placeholder image: " + ex.getMessage());
        }
    }
    
    /**
     * Initializes the review section UI and loads existing reviews
     */
    private void initializeReviewSection() {
        // Create UI elements if they don't exist yet
        if (reviewsContainer == null) {
            reviewsContainer = new VBox(10);
            reviewsContainer.setPadding(new Insets(20, 0, 20, 0));
            
            reviewSectionTitle = new Label("Course Reviews");
            reviewSectionTitle.getStyleClass().add("section-title");
            
            reviewsContainer.getChildren().add(reviewSectionTitle);
            
            // Add a subtitle explaining sentiment analysis
            Label sentimentExplanation = new Label("Reviews are automatically analyzed for sentiment (POSITIVE, WEAK_POSITIVE, NEUTRAL, WEAK_NEGATIVE, NEGATIVE)");
            sentimentExplanation.getStyleClass().add("sentiment-explanation");
            sentimentExplanation.setStyle("-fx-font-size: 12px; -fx-text-fill: #6c757d; -fx-font-style: italic;");
            sentimentExplanation.setWrapText(true);
            
            reviewsContainer.getChildren().add(sentimentExplanation);
        } else {
            // Clear any existing login/enrollment prompts
            for (int i = reviewsContainer.getChildren().size() - 1; i >= 0; i--) {
                Node node = reviewsContainer.getChildren().get(i);
                // Only remove prompt labels, not the title, sentiment explanation, scrollpane or review form
                if (node instanceof Label && 
                    i != 0 && i != 1 && // Skip the title and sentiment explanation labels 
                    node.getStyleClass().contains("review-prompt")) {
                    reviewsContainer.getChildren().remove(i);
                }
            }
        }
        
        // Ensure we have a scrollPane for reviews
        if (reviewsScrollPane == null) {
            reviewsScrollPane = new ScrollPane();
            reviewsScrollPane.setFitToWidth(true);
            reviewsScrollPane.setPrefHeight(250);
            reviewsScrollPane.setMinHeight(200);
            reviewsScrollPane.setMaxHeight(300);
            reviewsScrollPane.setStyle("-fx-background-color: transparent;");
            
            VBox reviewsContent = new VBox(10);
            reviewsContent.setPadding(new Insets(10));
            reviewsScrollPane.setContent(reviewsContent);
            
            // Add ScrollPane after the title
            if (reviewsContainer.getChildren().size() > 0) {
                reviewsContainer.getChildren().add(reviewsScrollPane);
            }
        }
        
        // Create or reset the review form container
        if (addReviewContainer == null) {
            addReviewContainer = new VBox(10);
            addReviewContainer.setPadding(new Insets(20, 10, 10, 10));
            addReviewContainer.setStyle("-fx-background-color: #f8f9fa; -fx-border-radius: 5px; -fx-background-radius: 5px;");
            
            Label addReviewLabel = new Label("Add Your Review");
            addReviewLabel.getStyleClass().add("sub-section-title");
            
            reviewTextArea = new TextArea();
            reviewTextArea.setPromptText("Share your experience with this course...");
            reviewTextArea.setPrefRowCount(3);
            
            HBox ratingBox = new HBox(20);
            ratingBox.setAlignment(Pos.CENTER_LEFT);
            
            Label ratingLabel = new Label("Rate this course:");
            
            likeRadioButton = new RadioButton("Like 👍");
            dislikeRadioButton = new RadioButton("Dislike 👎");
            
            if (ratingToggleGroup == null) {
                ratingToggleGroup = new ToggleGroup();
            }
            likeRadioButton.setToggleGroup(ratingToggleGroup);
            dislikeRadioButton.setToggleGroup(ratingToggleGroup);
            likeRadioButton.setSelected(true);
            
            ratingBox.getChildren().addAll(ratingLabel, likeRadioButton, dislikeRadioButton);
            
            submitReviewButton = new Button("Submit Review");
            submitReviewButton.getStyleClass().add("primary-button");
            submitReviewButton.setOnAction(event -> handleSubmitReview());
            
            reviewStatusLabel = new Label();
            reviewStatusLabel.setVisible(false);
            
            addReviewContainer.getChildren().addAll(
                addReviewLabel, reviewTextArea, ratingBox, submitReviewButton, reviewStatusLabel
            );
            
            // Add the review form after the ScrollPane
            if (reviewsContainer.getChildren().size() > 1) {
                reviewsContainer.getChildren().add(addReviewContainer);
            }
        } else {
            // Reset review form status
            reviewTextArea.clear();
            likeRadioButton.setSelected(true);
            submitReviewButton.setText("Submit Review");
            reviewStatusLabel.setVisible(false);
        }
        
        // By default, hide the review form until enrollment is checked
        addReviewContainer.setVisible(false);
        
        // Load existing reviews - this will be visible for all users
        loadReviews();
        
        // Check if user is logged in and enrolled in the course
        // This will show/hide the review form appropriately
        checkUserEnrollmentForReview();
    }
    
    /**
     * Checks if current user is enrolled and can review the course
     */
    private void checkUserEnrollmentForReview() {
        // Remove any existing prompts (we'll add new ones if needed)
        for (int i = reviewsContainer.getChildren().size() - 1; i >= 0; i--) {
            Node node = reviewsContainer.getChildren().get(i);
            // Only remove prompt labels, not the title, scrollpane or review form
            if (node instanceof Label && 
                i != 0 && i != 1 && // Skip the title and sentiment explanation labels
                node.getStyleClass().contains("review-prompt")) {
                reviewsContainer.getChildren().remove(i);
            }
        }
        
        // Always ensure reviewsScrollPane is visible for all users (enrolled or not)
        if (reviewsScrollPane != null) {
            reviewsScrollPane.setVisible(true);
        }
        
        // Default state - hide review form
        if (addReviewContainer != null) {
            addReviewContainer.setVisible(false);
        }
        
        if (currentUserId <= 0) {
            // User not logged in
            Label loginPrompt = new Label("Please log in to review this course");
            loginPrompt.setPadding(new Insets(10));
            loginPrompt.getStyleClass().add("review-prompt");
            
            // Add after title, before scrollpane
            if (reviewsContainer.getChildren().size() > 1) {
                reviewsContainer.getChildren().add(1, loginPrompt);
            }
            return;
        }
        
        // Check if user is enrolled in the course
        try {
            boolean isEnrolled = inscriptionCoursService.isUserEnrolled(currentUserId, currentCourse.getId());
            
            if (!isEnrolled) {
                // User not enrolled
                Label enrollPrompt = new Label("Enroll in this course to leave a review");
                enrollPrompt.setPadding(new Insets(10));
                enrollPrompt.getStyleClass().add("review-prompt");
                
                // Add after title, before scrollpane
                if (reviewsContainer.getChildren().size() > 1) {
                    reviewsContainer.getChildren().add(1, enrollPrompt);
                }
                return;
            }
            
            // User is enrolled, check if they already left a review
            userExistingReview = reviewService.getUserReviewForCourse(currentCourse.getId(), currentUserId);
            
            if (userExistingReview != null) {
                // User already has a review, populate the form for editing
                reviewTextArea.setText(userExistingReview.getReview());
                if (userExistingReview.isThumbsup()) {
                    likeRadioButton.setSelected(true);
                } else {
                    dislikeRadioButton.setSelected(true);
                }
                submitReviewButton.setText("Update Review");
            } else {
                // User is enrolled but hasn't reviewed yet, reset form
                reviewTextArea.clear();
                likeRadioButton.setSelected(true);
                submitReviewButton.setText("Submit Review");
            }
            
            // User is enrolled, so show the review form regardless of whether they've reviewed before
            addReviewContainer.setVisible(true);
            
        } catch (SQLException e) {
            System.err.println("Error checking enrollment: " + e.getMessage());
            
            // Show error message
            Label errorLabel = new Label("Error checking enrollment: " + e.getMessage());
            errorLabel.setPadding(new Insets(10));
            errorLabel.setStyle("-fx-text-fill: #dc3545;");
            errorLabel.getStyleClass().add("review-prompt");
            
            // Add after title, before scrollpane
            if (reviewsContainer.getChildren().size() > 1) {
                reviewsContainer.getChildren().add(1, errorLabel);
            }
        }
    }
    
    /**
     * Loads and displays reviews for the current course
     */
    private void loadReviews() {
        if (reviewsScrollPane == null || currentCourse == null) {
            return;
        }
        
        try {
            List<Review> reviews = reviewService.getReviewsByCourse(currentCourse.getId());
            
            VBox reviewsContent = new VBox(15);
            reviewsContent.setPadding(new Insets(10));
            
            if (reviews.isEmpty()) {
                Label noReviewsLabel = new Label("No reviews yet. Be the first to review this course!");
                noReviewsLabel.setPadding(new Insets(10));
                reviewsContent.getChildren().add(noReviewsLabel);
            } else {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy");
                
                for (Review review : reviews) {
                    VBox reviewBox = new VBox(5);
                    reviewBox.setPadding(new Insets(10));
                    reviewBox.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; " +
                            "-fx-border-radius: 5px; -fx-background-radius: 5px;");
                    
                    // User and date info
                    HBox headerBox = new HBox(10);
                    headerBox.setAlignment(Pos.CENTER_LEFT);
                    
                    Circle userIcon = new Circle(20, 20, 20);
                    
                    Label userLabel = new Label("User #" + review.getUser_id());
                    userLabel.setStyle("-fx-font-weight: bold;");
                    
                    Label dateLabel = new Label(review.getDatecreation().format(formatter));
                    dateLabel.setStyle("-fx-text-fill: #6c757d;");
                    
                    // Create a spacer to push sentiment to the right
                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);
                    
                    // Add sentiment label
                    Label sentimentLabel = new Label(review.getSentiment().toString());
                    sentimentLabel.setStyle(sentimentService.getSentimentStyle(review.getSentiment()));
                    
                    // Rating indicator
                    Label ratingLabel = new Label(review.isThumbsup() ? "👍 Liked" : "👎 Disliked");
                    ratingLabel.setStyle(review.isThumbsup() ? 
                            "-fx-text-fill: #28a745; -fx-font-weight: bold;" : 
                            "-fx-text-fill: #dc3545; -fx-font-weight: bold;");
                    
                    headerBox.getChildren().addAll(userIcon, userLabel, dateLabel, ratingLabel, spacer, sentimentLabel);
                    reviewBox.getChildren().add(headerBox);
                    
                    // Review text
                    Text reviewText = new Text(review.getReview());
                    reviewText.setWrappingWidth(reviewBox.getPrefWidth() - 20);
                    
                    reviewBox.getChildren().add(reviewText);
                    
                    // If this is the current user's review, add edit/delete buttons
                    if (currentUserId > 0 && review.getUser_id() == currentUserId) {
                        HBox actionBox = new HBox(10);
                        actionBox.setAlignment(Pos.CENTER_RIGHT);
                        actionBox.setPadding(new Insets(10, 0, 0, 0));
                        
                        Button editButton = new Button("Edit");
                        editButton.getStyleClass().add("edit-button");
                        
                        editButton.setOnAction(event -> {
                            // Set the review text and rating
                            reviewTextArea.setText(review.getReview());
                            if (review.isThumbsup()) {
                                likeRadioButton.setSelected(true);
                            } else {
                                dislikeRadioButton.setSelected(true);
                            }
                            
                            // Change button text to indicate update
                            submitReviewButton.setText("Update Review");
                            
                            // Set focus on the textarea
                            reviewTextArea.requestFocus();
                        });
                        
                        Button deleteButton = new Button("Delete");
                        deleteButton.getStyleClass().add("delete-button");
                        
                        deleteButton.setOnAction(event -> {
                            try {
                                reviewService.deleteReview(review.getId());
                                userExistingReview = null;
                                // Reset the form
                                reviewTextArea.clear();
                                likeRadioButton.setSelected(true);
                                submitReviewButton.setText("Submit Review");
                                // Reload reviews
                                loadReviews();
                            } catch (SQLException e) {
                                System.err.println("Error deleting review: " + e.getMessage());
                                showAlert(Alert.AlertType.ERROR, "Error", 
                                    "Could not delete review", e.getMessage());
                            }
                        });
                        
                        actionBox.getChildren().addAll(editButton, deleteButton);
                        reviewBox.getChildren().add(actionBox);
                    }
                    
                    reviewsContent.getChildren().add(reviewBox);
                }
            }
            
            reviewsScrollPane.setContent(reviewsContent);
            reviewsScrollPane.setVisible(true);
            
        } catch (SQLException e) {
            System.err.println("Error loading reviews: " + e.getMessage());
            VBox errorContent = new VBox();
            Label errorLabel = new Label("Could not load reviews: " + e.getMessage());
            errorLabel.setPadding(new Insets(10));
            errorContent.getChildren().add(errorLabel);
            reviewsScrollPane.setContent(errorContent);
        }
    }
    
    /**
     * Handle the submit/update review button
     */
    @FXML
    private void handleSubmitReview() {
        if (currentUserId <= 0 || currentCourse == null) {
            showAlert(Alert.AlertType.ERROR, "Error", 
                "Not logged in", "You must be logged in to leave a review.");
            return;
        }
        
        String reviewText = reviewTextArea.getText().trim();
        if (reviewText.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", 
                "Empty review", "Please enter your review before submitting.");
            return;
        }
        
        boolean isLike = likeRadioButton.isSelected();
        
        try {
            if (userExistingReview != null) {
                // Update existing review
                reviewService.updateReview(userExistingReview.getId(), isLike, reviewText);
            } else {
                // Create new review
                reviewService.addReview(currentCourse.getId(), currentUserId, isLike, reviewText);
            }
            
            // Show success message
            reviewStatusLabel.setText("Review submitted successfully!");
            reviewStatusLabel.setStyle("-fx-text-fill: #28a745;");
            reviewStatusLabel.setVisible(true);
            
            // Reload reviews to show the updated list
            loadReviews();
            
            // Check if the user's review status changed
            userExistingReview = reviewService.getUserReviewForCourse(currentCourse.getId(), currentUserId);
            
        } catch (SQLException e) {
            System.err.println("Error submitting review: " + e.getMessage());
            reviewStatusLabel.setText("Error: Could not submit review.");
            reviewStatusLabel.setStyle("-fx-text-fill: #dc3545;");
            reviewStatusLabel.setVisible(true);
        }
    }
    
    // Inner class for the user icon
    private class Circle extends javafx.scene.shape.Circle {
        public Circle(double centerX, double centerY, double radius) {
            super(centerX, centerY, radius);
            setFill(javafx.scene.paint.Color.LIGHTGRAY);
        }
    }
    
    /**
     * Loads lessons for the current course
     */
    private void loadLessons() {
        try {
            List<Lesson> lessons = lessonService.getLessonsByCourse(currentCourse.getId());
            lessonsListView.getItems().setAll(lessons);
        } catch (SQLException e) {
            System.err.println("Error loading lessons: " + e.getMessage());
            // Show a placeholder if lessons can't be loaded
            Lesson placeholder = new Lesson();
            placeholder.setTitre("No lessons available");
            placeholder.setOrdre(1);
            lessonsListView.getItems().add(placeholder);
        }
    }
    
    /**
     * Handle back button click to return to courses list
     */
    @FXML
    private void handleBack() {
        try {
            // First load the front_menu.fxml (main container with navigation bar)
            FXMLLoader menuLoader = new FXMLLoader(getClass().getResource("/Menu/front_menu.fxml"));
            Parent menuRoot = menuLoader.load();
            
            // Get the front menu controller
            org.example.controller.menu.FrontMenu frontMenuController = menuLoader.getController();
            
            // Pass the current user ID to the front menu controller
            if (currentUserId > 0) {
                frontMenuController.setCurrentUserId(currentUserId);
            }
            
            // Create the scene with the front menu
            Scene scene = startLearningButton.getScene();
            
            // Clear all existing stylesheets to prevent style contamination
            scene.getStylesheets().clear();
            
            // Add the menu CSS
            String menuCssPath = getClass().getResource("/css/menu.css").toExternalForm();
            scene.getStylesheets().add(menuCssPath);
            
            // Set the main menu as the root
            scene.setRoot(menuRoot);
            
            // Now trigger the courses button to load the courses content
            // This will load the courses content into the content pane
            frontMenuController.handleCoursesButton();
            
            System.out.println("Navigation: Back to front menu with courses content");
            
        } catch (IOException e) {
            System.err.println("Error returning to courses: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", 
                "Could not return to courses page", e.getMessage());
        }
    }
    
    /**
     * Handle start learning/purchase button click
     */
    @FXML
    private void handleStartLearning() {
        // Check if user is logged in
        if (currentUserId <= 0) {
            showAlert(Alert.AlertType.INFORMATION, "Login Required", 
                "Please log in", "You need to log in to enroll in this course.");
            return;
        }
        
        try {
            // Check if user is already enrolled
            boolean isEnrolled = inscriptionCoursService.isUserEnrolled(currentUserId, currentCourse.getId());
            
            if (isEnrolled) {
                // User already enrolled, go directly to learning interface
                showLearningInterface();
                return;
            }
            
            // User not enrolled, check if course is free or paid
            if (currentCourse.isIs_free()) {
                // For free courses, directly enroll the user
                enrollInCourse();
            } else {
                // For paid courses, show payment dialog
                showPaymentDialog();
            }
        } catch (SQLException e) {
            System.err.println("Error checking enrollment status: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Error", 
                "Could not check enrollment status", e.getMessage());
        }
    }
    
    /**
     * Enroll the current user in the course
     */
    private void enrollInCourse() {
        try {
            // Check if user is already enrolled
            if (inscriptionCoursService.isUserEnrolled(currentUserId, currentCourse.getId())) {
                // User already enrolled, go directly to learning interface
                showLearningInterface();
                return;
            }
            
            // Enroll the user
            inscriptionCoursService.enrollUserInCourse(currentUserId, currentCourse.getId());
            
            // Update the UI - Enable review section
            checkUserEnrollmentForReview();
            
            // Update enrollment status UI
            updateEnrollmentStatus();
            
            // Show learning interface
            showLearningInterface();
            
        } catch (SQLException e) {
            System.err.println("Error enrolling in course: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Enrollment Error", 
                "Could not enroll in the course", e.getMessage());
        }
    }
    
    /**
     * Show the learning interface after successful enrollment
     */
    private void showLearningInterface() {
        try {
            // Get the current stage
            Stage stage = (Stage) startLearningButton.getScene().getWindow();
            
            // Load the lesson view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/lesson/course_learning.fxml"));
            Parent root = loader.load();
            
            // Get the lesson controller and set up course information
            org.example.controller.lesson.CourseLearningController controller = loader.getController();
            controller.setCurrentUserId(currentUserId);
            controller.setCourse(currentCourse);
            
            // Create a new scene with the lesson view
            Scene scene = new Scene(root);
            
            // Set the scene and show the stage
            stage.setScene(scene);
            stage.setTitle("Learning: " + currentCourse.getTitre());
            stage.show();
            
            System.out.println("Navigation: Opened learning interface for course " + currentCourse.getId());
            
        } catch (IOException e) {
            System.err.println("Error opening learning interface: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Navigation Error", 
                "Could not open learning interface", e.getMessage());
        }
    }
    
    /**
     * Show payment dialog for paid courses
     */
    private void showPaymentDialog() {
        try {
            // Create a new dialog
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Payment Required");
            dialog.setHeaderText("Complete your purchase");
            
            // Set the icon (optional)
            // Commenting out this code since the payment icon image doesn't exist
            /*
            Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/images/payment-icon.png")));
            */
            
            // Create the payment form
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(15);
            grid.setPadding(new Insets(20, 20, 20, 20));
            
            // Set minimum width for dialog
            dialog.getDialogPane().setMinWidth(450);
            
            // Payment method toggle group
            ToggleGroup paymentMethodGroup = new ToggleGroup();
            
            VBox paymentMethodsBox = new VBox(10);
            paymentMethodsBox.setPadding(new Insets(10));
            paymentMethodsBox.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 5px;");
            
            Label paymentMethodLabel = new Label("Payment Method");
            paymentMethodLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            
            RadioButton creditCardRadio = new RadioButton("Credit Card");
            creditCardRadio.setToggleGroup(paymentMethodGroup);
            creditCardRadio.setSelected(true);
            
            RadioButton paypalRadio = new RadioButton("PayPal");
            paypalRadio.setToggleGroup(paymentMethodGroup);
            
            paymentMethodsBox.getChildren().addAll(paymentMethodLabel, creditCardRadio, paypalRadio);
            
            // Credit card form
            VBox creditCardForm = new VBox(15);
            creditCardForm.setPadding(new Insets(15, 0, 0, 0));
            
            // Card number field with validation
            TextField cardNumberField = new TextField();
            cardNumberField.setPromptText("Card Number");
            cardNumberField.setPrefWidth(300);
            
            // Format as user types (add spaces every 4 digits)
            cardNumberField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue.length() > 19) {
                    cardNumberField.setText(oldValue);
                    return;
                }
                
                // Remove any non-digit characters
                String digitsOnly = newValue.replaceAll("[^0-9]", "");
                
                // Format with spaces every 4 digits
                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < digitsOnly.length(); i++) {
                    if (i > 0 && i % 4 == 0) {
                        formatted.append(" ");
                    }
                    formatted.append(digitsOnly.charAt(i));
                }
                
                if (!formatted.toString().equals(newValue)) {
                    cardNumberField.setText(formatted.toString());
                }
            });
            
            // Expiration date and CVV
            HBox expCvvBox = new HBox(10);
            
            TextField expiryField = new TextField();
            expiryField.setPromptText("MM/YY");
            expiryField.setPrefWidth(100);
            
            // Format expiry date as user types
            expiryField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue.length() > 5) {
                    expiryField.setText(oldValue);
                    return;
                }
                
                // Remove any non-digit characters
                String digitsOnly = newValue.replaceAll("[^0-9]", "");
                
                // Format with slash
                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < digitsOnly.length(); i++) {
                    if (i == 2) {
                        formatted.append("/");
                    }
                    formatted.append(digitsOnly.charAt(i));
                }
                
                if (!formatted.toString().equals(newValue)) {
                    expiryField.setText(formatted.toString());
                }
            });
            
            PasswordField cvvField = new PasswordField();
            cvvField.setPromptText("CVV");
            cvvField.setPrefWidth(70);
            cvvField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue.matches("\\d{0,3}")) {
                    cvvField.setText(newValue.replaceAll("[^0-9]", ""));
                }
                if (newValue.length() > 3) {
                    cvvField.setText(oldValue);
                }
            });
            
            HBox.setHgrow(expiryField, Priority.ALWAYS);
            expCvvBox.getChildren().addAll(expiryField, cvvField);
            
            // Cardholder name
            TextField nameField = new TextField();
            nameField.setPromptText("Cardholder Name");
            
            // Course info and price display
            HBox courseInfoBox = new HBox(10);
            courseInfoBox.setAlignment(Pos.CENTER_LEFT);
            courseInfoBox.setPadding(new Insets(15, 10, 15, 10));
            courseInfoBox.setStyle("-fx-background-color: #e8f4fc; -fx-background-radius: 5px;");
            
            Label courseNameLabel = new Label(currentCourse.getTitre());
            courseNameLabel.setStyle("-fx-font-weight: bold;");
            
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            
            Label priceLabel = new Label("$" + String.format("%.2f", currentCourse.getPrice()));
            priceLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
            
            courseInfoBox.getChildren().addAll(courseNameLabel, spacer, priceLabel);
            
            // PayPal form
            VBox paypalForm = new VBox(15);
            paypalForm.setPadding(new Insets(15, 0, 0, 0));
            
            TextField emailField = new TextField();
            emailField.setPromptText("PayPal Email Address");
            
            paypalForm.getChildren().addAll(
                new Label("PayPal Email:"),
                emailField
            );
            paypalForm.setVisible(false);
            
            // Add elements to credit card form
            creditCardForm.getChildren().addAll(
                new Label("Card Number:"),
                cardNumberField,
                new Label("Expiry Date / CVV:"),
                expCvvBox,
                new Label("Cardholder Name:"),
                nameField
            );
            
            // Toggle visibility based on selected payment method
            paymentMethodGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue == creditCardRadio) {
                    creditCardForm.setVisible(true);
                    paypalForm.setVisible(false);
                } else {
                    creditCardForm.setVisible(false);
                    paypalForm.setVisible(true);
                }
            });
            
            // Error message label
            Label errorMessageLabel = new Label();
            errorMessageLabel.setStyle("-fx-text-fill: #dc3545;");
            errorMessageLabel.setVisible(false);
            
            // Main container
            VBox mainContainer = new VBox(15);
            mainContainer.getChildren().addAll(
                courseInfoBox,
                paymentMethodsBox,
                creditCardForm,
                paypalForm,
                errorMessageLabel
            );
            
            // Add to dialog
            dialog.getDialogPane().setContent(mainContainer);
            
            // Add buttons
            ButtonType payButton = new ButtonType("Pay Now", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().addAll(payButton, cancelButton);
            
            // Style the pay button
            Button payButtonNode = (Button) dialog.getDialogPane().lookupButton(payButton);
            payButtonNode.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
            
            // Validation before allowing payment to proceed
            payButtonNode.addEventFilter(ActionEvent.ACTION, event -> {
                boolean isValid = true;
                errorMessageLabel.setVisible(false);
                
                if (paymentMethodGroup.getSelectedToggle() == creditCardRadio) {
                    // Validate credit card form
                    if (cardNumberField.getText().trim().replaceAll("\\s", "").length() != 16) {
                        isValid = false;
                        errorMessageLabel.setText("Please enter a valid 16-digit card number");
                    } else if (expiryField.getText().trim().length() != 5) {
                        isValid = false;
                        errorMessageLabel.setText("Please enter a valid expiry date (MM/YY)");
                    } else if (cvvField.getText().trim().length() != 3) {
                        isValid = false;
                        errorMessageLabel.setText("Please enter a valid 3-digit CVV");
                    } else if (nameField.getText().trim().isEmpty()) {
                        isValid = false;
                        errorMessageLabel.setText("Please enter the cardholder name");
                    }
                } else {
                    // Validate PayPal form
                    String email = emailField.getText().trim();
                    if (email.isEmpty() || !email.matches("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")) {
                        isValid = false;
                        errorMessageLabel.setText("Please enter a valid email address");
                    }
                }
                
                if (!isValid) {
                    errorMessageLabel.setVisible(true);
                    event.consume(); // Prevent dialog from closing
                }
            });
            
            // Get the result and process payment
            Optional<ButtonType> result = dialog.showAndWait();
            
            if (result.isPresent() && result.get() == payButton) {
                // Get the payment data based on selected method
                String paymentData = "";
                boolean isCreditCard = paymentMethodGroup.getSelectedToggle() == creditCardRadio;
                
                if (isCreditCard) {
                    paymentData = cardNumberField.getText();
                } else {
                    paymentData = emailField.getText();
                }
                
                // Simulate payment processing
                boolean paymentSuccess = processPayment(isCreditCard, paymentData);
                
                if (paymentSuccess) {
                    // Payment successful, enroll the user
                    showAlert(Alert.AlertType.INFORMATION, "Payment Successful", 
                        "Payment processed successfully", "You have been enrolled in the course: " + currentCourse.getTitre());
                    enrollInCourse();
                } else {
                    // Payment failed
                    showAlert(Alert.AlertType.ERROR, "Payment Failed", 
                        "Payment processing failed", "Please check your payment details and try again.");
                }
            }
        } catch (Exception e) {
            System.err.println("Error showing payment dialog: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", 
                "Could not show payment dialog", e.getMessage());
        }
    }
    
    /**
     * Process the payment (simulated)
     * For testing:
     * - Credit card payments: 
     *   - Success: Card numbers starting with 4 (Visa test cards) or 5 (Mastercard test cards)
     *   - Failure: All other card numbers
     * - PayPal payments: 
     *   - Success: Emails containing "success" or "valid"
     *   - Failure: All other emails
     * 
     * @param isCreditCard Whether payment is via credit card or PayPal
     * @param paymentData Payment information (card number or email)
     * @return true if payment was successful, false otherwise
     */
    private boolean processPayment(boolean isCreditCard, String paymentData) {
        try {
            // Simulate payment processing delay
            Thread.sleep(1000);
            
            // This method would normally connect to a payment processor
            // For testing purposes, we'll use a simple rule:
            
            if (isCreditCard) {
                // Remove spaces from card number
                String cardNumber = paymentData.replaceAll("\\s", "");
                
                // Test cards:
                // Success: Card numbers starting with 4 (Visa) or 5 (Mastercard)
                // Failure: All other card numbers
                if (cardNumber.startsWith("4") || cardNumber.startsWith("5")) {
                    System.out.println("Payment successful with test card: " + cardNumber);
                    return true;
                }
                
                System.out.println("Payment failed with invalid test card: " + cardNumber);
                return false;
            } else {
                // For PayPal payments
                String email = paymentData.toLowerCase();
                
                // Success for emails containing "success" or "valid"
                if (email.contains("success") || email.contains("valid")) {
                    System.out.println("PayPal payment successful with test email: " + email);
                    return true;
                }
                
                System.out.println("PayPal payment failed with email: " + email);
                return false;
            }
        } catch (InterruptedException e) {
            System.err.println("Payment processing interrupted: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Set the current user ID
     */
    public void setCurrentUserId(int userId) {
        this.currentUserId = userId;
        System.out.println("CourseDetailsController: User ID set to " + userId);
        
        // Update UI for user-specific content
        if (currentCourse != null) {
            // Update enrollment status UI (price label and button)
            updateEnrollmentStatus();
            
            // Update review form visibility
            checkUserEnrollmentForReview();
        }
    }
    
    /**
     * Helper method to show alerts
     */
    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    @FXML
    private void handleTestLearning() {
        // For testing purposes only
        showLearningInterface();
    }
} 