package org.example.controller.lesson;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
// Temporarily comment out media imports
// import javafx.scene.media.Media;
// import javafx.scene.media.MediaException;
// import javafx.scene.media.MediaPlayer;
// import javafx.scene.media.MediaView;
import javafx.scene.text.Text;
// WebView imports for YouTube support
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.Pair;
import org.example.models.course.Course;
import org.example.models.lesson.Lesson;
import org.example.models.lesson.Note;
import org.example.services.LessonService;
import org.example.services.NoteService;
import utils.BreakTimer;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CourseLearningController {
    
    @FXML private VBox videoPlaceholder;
    @FXML private Label videoInfoLabel;
    @FXML private Button openExternalButton;
    @FXML private StackPane mediaContainer;
    @FXML private Button playPauseButton;
    @FXML private Slider timeSlider;
    @FXML private Label timeLabel;
    @FXML private Button fullScreenButton;
    @FXML private Label courseTitle;
    @FXML private Label lessonTitle;
    @FXML private Label lessonDescription;
    @FXML private ListView<Lesson> lessonsListView;
    @FXML private Button previousLessonButton;
    @FXML private Button nextLessonButton;
    @FXML private VBox sidebarContainer;
    @FXML private Button toggleSidebarButton;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private VBox noContentPlaceholder;
    @FXML private HBox mediaControls;
    @FXML private Button backButton;
    @FXML private Button notesButton;
    @FXML private Button resourcesButton;
    
    // New UI components for break timer
    @FXML private Button breakReminderButton;
    @FXML private Label breakTimerLabel;
    
    // New UI components for notes
    private VBox notesPanel;
    private TextArea noteTextArea;
    private Button saveNoteButton;
    private final BooleanProperty notesPanelVisible = new SimpleBooleanProperty(false);
    
    private Course currentCourse;
    private LessonService lessonService;
    private NoteService noteService;
    private List<Lesson> lessons;
    private int currentLessonIndex = 0;
    private String currentVideoUrl;
    private Timeline timelineUpdater;
    private final BooleanProperty sidebarVisible = new SimpleBooleanProperty(true);
    private final BooleanProperty isPlaying = new SimpleBooleanProperty(false);
    private boolean updatingSlider = false;
    private int currentUserId;
    private Note currentNote;
    
    // Break timer properties
    private BreakTimer breakTimer;
    private final BooleanProperty breakTimerActive = new SimpleBooleanProperty(false);
    private final BooleanProperty breakReminderSet = new SimpleBooleanProperty(false);
    
    @FXML
    public void initialize() {
        try {
            System.out.println("CourseLearningController: initializing");
            
            // Log system information
            printSystemInfo();
            
            // Initialize services
            lessonService = new LessonService();
            noteService = new NoteService();
            
            // Initialize video placeholder
            videoPlaceholder.setVisible(true);
            openExternalButton.setVisible(false); // Hide until we have a valid URL
            
            // Initialize notes panel
            initializeNotesPanel();
            
            // Configure lessons list view
            lessonsListView.setCellFactory(param -> new ListCell<Lesson>() {
                @Override
                protected void updateItem(Lesson lesson, boolean empty) {
                    super.updateItem(lesson, empty);
                    
                    if (empty || lesson == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        String displayText = lesson.getOrdre() + ". " + lesson.getTitre();
                        
                        // Bold the text for the currently selected lesson
                        if (currentLessonIndex == getIndex()) {
                            setText(null);
                            Label boldLabel = new Label(displayText);
                            boldLabel.setStyle("-fx-font-weight: bold;");
                            setGraphic(boldLabel);
                        } else {
                            setText(displayText);
                            setGraphic(null);
                        }
                    }
                }
            });
            
            // Setup lesson selection listener
            lessonsListView.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue.intValue() >= 0 && newValue.intValue() < lessons.size()) {
                    currentLessonIndex = newValue.intValue();
                    loadLesson(lessons.get(currentLessonIndex));
                }
            });
            
            // Connect time slider to media player
            timeSlider.valueProperty().addListener((obs, oldValue, newValue) -> {
                // Just update for visual feedback 
                if (timeSlider.isValueChanging()) {
                    System.out.println("Slider changed: " + newValue);
                    // Update time label based on slider position
                    int minute = (int) (5 * newValue.doubleValue() / 100);
                    int second = (int) (60 * (newValue.doubleValue() / 100) % 60);
                    timeLabel.setText(String.format("%02d:%02d / 05:00", minute, second));
                }
            });
            
            // Setup sidebar visibility binding
            sidebarVisible.addListener((obs, wasVisible, isNowVisible) -> {
                sidebarContainer.setVisible(isNowVisible);
                sidebarContainer.setManaged(isNowVisible);
                
                // Update toggle button icon
                updateToggleButtonIcon();
            });
            
            // Initially the sidebar is visible
            sidebarVisible.set(true);
            
            // Setup navigation buttons
            updateNavigationButtons();
            
            // Handle image placeholders - create default graphics
            setupDefaultIcons();
            
            // Initialize break reminder components
            initializeBreakReminder();
            
            // Don't try to access the scene here as it might not be attached yet
            // Instead, we'll add a listener to be called once the scene is available
            mediaContainer.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    System.out.println("Scene is now available - initializing scene-dependent components");
                    initializeWithScene(newScene);
                }
            });
            
            System.out.println("CourseLearningController: initialization complete");
        } catch (Exception e) {
            System.err.println("Error during initialization: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Print system information for debugging
     */
    private void printSystemInfo() {
        try {
            // Get JavaFX version using the runtime class
            String version = System.getProperty("javafx.runtime.version");
            System.out.println("JavaFX Runtime Version: " + (version != null ? version : "Unknown"));
            
            // Print Java version
            System.out.println("Java Version: " + System.getProperty("java.version"));
            System.out.println("Java VM: " + System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version"));
            
            // Check if Desktop is supported (for opening browser)
            System.out.println("Desktop supported: " + Desktop.isDesktopSupported());
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                System.out.println("Desktop browse supported: " + desktop.isSupported(Desktop.Action.BROWSE));
            }
        } catch (Exception e) {
            System.err.println("Error getting system info: " + e.getMessage());
        }
    }
    
    /**
     * Initialize components that depend on the scene being available
     */
    private void initializeWithScene(Scene scene) {
        try {
            // Apply CSS
            String cssPath = getClass().getResource("/css/course_learning.css").toExternalForm();
            if (!scene.getStylesheets().contains(cssPath)) {
                scene.getStylesheets().add(cssPath);
                System.out.println("Added CSS: " + cssPath);
            }
            
            // Fix back button icon if needed
            setupBackButton();
            
            // Setup keyboard shortcuts and other scene-dependent features
            scene.setOnKeyPressed(event -> {
                switch (event.getCode()) {
                    case SPACE:
                        handlePlayPause();
                        break;
                    case LEFT:
                        handlePreviousLesson();
                        break;
                    case RIGHT:
                        handleNextLesson();
                        break;
                    case F11:
                        handleFullScreen();
                        break;
                    default:
                        // Do nothing for other keys
                        break;
                }
            });
            
            // Adjust layouts and styles based on screen size
            adjustLayoutForScreenSize(scene);
        } catch (Exception e) {
            System.err.println("Error in initializeWithScene: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Adjust layouts based on screen size
     */
    private void adjustLayoutForScreenSize(Scene scene) {
        // Example: adjust media size based on screen size
        double screenWidth = scene.getWindow().getWidth();
        if (screenWidth < 1200) {
            sidebarContainer.setPrefWidth(220);
        } else {
            sidebarContainer.setPrefWidth(300);
        }
    }
    
    /**
     * Setup default icons for UI elements
     */
    private void setupDefaultIcons() {
        try {
            // Create vector-based icons for all buttons that might have missing images
            setupBackButton();
            setupPlayPauseButton();
            setupFullScreenButton();
            setupToggleSidebarButton();
            
            System.out.println("Default icons setup complete");
        } catch (Exception e) {
            System.err.println("Error setting up default icons: " + e.getMessage());
        }
    }
    
    /**
     * Set the course to be displayed
     */
    public void setCourse(Course course) {
        this.currentCourse = course;
        courseTitle.setText(course.getTitre());
        
        // Load lessons for this course
        loadLessons();
    }
    
    /**
     * Load lessons for the current course
     */
    private void loadLessons() {
        try {
            lessons = lessonService.getLessonsByCourse(currentCourse.getId());
            lessonsListView.getItems().setAll(lessons);
            
            if (!lessons.isEmpty()) {
                // Select the first lesson
                lessonsListView.getSelectionModel().select(0);
                loadLesson(lessons.get(0));
            } else {
                // No lessons available
                showNoContent("No lessons available for this course");
            }
            
            updateNavigationButtons();
        } catch (Exception e) {
            System.err.println("Error loading lessons: " + e.getMessage());
            showNoContent("Error loading course lessons");
        }
    }
    
    /**
     * Load a specific lesson's content
     */
    private void loadLesson(Lesson lesson) {
        try {
            lessonTitle.setText(lesson.getTitre());
            lessonDescription.setText(lesson.getDescription());
            
            // Show loading indicator
            showLoading(true);
            
            // Hide all content containers initially
            videoPlaceholder.setVisible(false);
            noContentPlaceholder.setVisible(false);
            mediaControls.setVisible(false);
            openExternalButton.setVisible(false);
            
            // Check what type of content we have and load accordingly
            String videoPath = lesson.getVideoPath();
            this.currentVideoUrl = videoPath;
            
            if (videoPath != null && !videoPath.isEmpty()) {
                if (isYouTubeUrl(videoPath)) {
                    displayYouTubeInfo(videoPath);
                } else {
                    // Otherwise show video info (local video handling)
                showVideoInfo(videoPath);
                }
            } else {
                // Hide loading indicator and show no content message
                showLoading(false);
                showNoContent("No video path specified for this lesson");
            }
            
            // Load notes for this lesson if any
            loadNotes(lesson.getId());
            
            // Update navigation buttons
            updateNavigationButtons();
        } catch (Exception e) {
            System.err.println("Error in loadLesson: " + e.getMessage());
            e.printStackTrace();
            showNoContent("Error loading lesson: " + e.getMessage());
        }
    }
    
    /**
     * Check if the URL is a YouTube video URL
     */
    private boolean isYouTubeUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        
        // Common YouTube URL patterns
        return url.contains("youtube.com/watch") || 
               url.contains("youtu.be/") || 
               url.contains("youtube.com/embed/");
    }
    
    /**
     * Extract YouTube video ID from URL
     */
    private String extractYouTubeVideoId(String url) {
        String videoId = null;
        
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        // Pattern 1: youtube.com/watch?v=VIDEO_ID
        Pattern pattern1 = Pattern.compile("(?:youtube\\.com\\/watch\\?v=)([\\w-]+)");
        Matcher matcher1 = pattern1.matcher(url);
        if (matcher1.find()) {
            videoId = matcher1.group(1);
        }
        
        // Pattern 2: youtu.be/VIDEO_ID
        if (videoId == null) {
            Pattern pattern2 = Pattern.compile("(?:youtu\\.be\\/)([\\w-]+)");
            Matcher matcher2 = pattern2.matcher(url);
            if (matcher2.find()) {
                videoId = matcher2.group(1);
            }
        }
        
        // Pattern 3: youtube.com/embed/VIDEO_ID
        if (videoId == null) {
            Pattern pattern3 = Pattern.compile("(?:youtube\\.com\\/embed\\/)([\\w-]+)");
            Matcher matcher3 = pattern3.matcher(url);
            if (matcher3.find()) {
                videoId = matcher3.group(1);
            }
        }
        
        return videoId;
    }
    
    /**
     * Display YouTube video information
     */
    private void displayYouTubeInfo(String youtubeUrl) {
        try {
            System.out.println("Displaying YouTube video information: " + youtubeUrl);
            
            String videoId = extractYouTubeVideoId(youtubeUrl);
            if (videoId != null) {
                // Update the video info display
                videoInfoLabel.setText("YouTube Video ID: " + videoId);
                
                // Show video placeholder
                videoPlaceholder.setVisible(true);
                
                // Enable "Open in browser" button
                openExternalButton.setVisible(true);
                
                // Hide loading indicator
                showLoading(false);
            } else {
                showNoContent("Invalid YouTube URL: " + youtubeUrl);
            }
        } catch (Exception e) {
            System.err.println("Error displaying YouTube info: " + e.getMessage());
            e.printStackTrace();
            showNoContent("Error displaying YouTube video: " + e.getMessage());
        }
    }
    
    /**
     * Display video information instead of loading the actual video
     */
    private void showVideoInfo(String videoPath) {
        try {
            // Hide loading indicator
            showLoading(false);
            
            // Update video info display
            videoInfoLabel.setText("Local video path: " + videoPath);
            
            // Show video placeholder
            videoPlaceholder.setVisible(true);
            
            // Show media controls if needed
            mediaControls.setVisible(true);
            
            // Update time label with dummy data
            timeLabel.setText("00:00 / 05:00");
            
            // Reset slider
            timeSlider.setValue(0);
            
            // We're not really playing anything
            isPlaying.set(false);
            updateTogglePlayIcon();
            
            System.out.println("Displayed video info for: " + videoPath);
            
        } catch (Exception e) {
            System.err.println("Error in showVideoInfo: " + e.getMessage());
            e.printStackTrace();
            showNoContent("Error displaying video info: " + e.getMessage());
        }
    }
    
    /**
     * Handle opening the current video in external browser
     */
    @FXML
    private void handleOpenExternal() {
        try {
            if (currentVideoUrl != null && !currentVideoUrl.isEmpty()) {
                System.out.println("Opening in browser: " + currentVideoUrl);
                
                // Check if Desktop is supported (most platforms)
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI(currentVideoUrl));
                } else {
                    // Fallback for platforms without Desktop support
                    showAlert(Alert.AlertType.ERROR, "Browser Error", 
                        "Cannot open browser", 
                        "Your system doesn't support opening links automatically.");
                }
            } else {
                showAlert(Alert.AlertType.WARNING, "No Video", 
                    "No video URL available", 
                    "There is no video URL available to open.");
            }
        } catch (URISyntaxException e) {
            System.err.println("Invalid URL: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "URL Error", 
                "Invalid URL", "The video URL is not valid: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error opening browser: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Browser Error", 
                "Cannot open browser", "Error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error in handleOpenExternal: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", 
                "Unknown error", "Error: " + e.getMessage());
        }
    }
    
    /**
     * Handle play/pause button click
     */
    @FXML
    private void handlePlayPause() {
        // Disabled for now - just toggle the play/pause icon
        isPlaying.set(!isPlaying.get());
        updateTogglePlayIcon();
        
        if (isPlaying.get()) {
            System.out.println("Play button clicked (placeholder only)");
        } else {
            System.out.println("Pause button clicked (placeholder only)");
        }
    }
    
    /**
     * Setup timeline to update time slider and label (disabled for now)
     */
    private void setupTimelineUpdater() {
        // For now, just set the slider and time label once
        timeSlider.setValue(0);
        timeLabel.setText("00:00 / 05:00");
    }
    
    /**
     * Show or hide the loading indicator
     */
    private void showLoading(boolean show) {
        loadingIndicator.setVisible(show);
    }
    
    /**
     * Show no content message
     */
    private void showNoContent(String message) {
        try {
            // Find the label in the VBox
            Label noContentLabel = (Label) noContentPlaceholder.getChildren().stream()
                .filter(node -> node instanceof Label)
                .findFirst()
                .orElse(null);
                
            if (noContentLabel != null) {
                noContentLabel.setText(message);
            } else {
                // If no label exists, create one
                Label newLabel = new Label(message);
                newLabel.setTextFill(javafx.scene.paint.Color.WHITE);
                newLabel.setFont(new javafx.scene.text.Font(18.0));
                noContentPlaceholder.getChildren().add(newLabel);
            }
            
            // Check if there's an ImageView in the VBox
            ImageView placeholderImage = (ImageView) noContentPlaceholder.getChildren().stream()
                .filter(node -> node instanceof ImageView)
                .findFirst()
                .orElse(null);
                
            if (placeholderImage != null) {
                // Create a video placeholder icon
                javafx.scene.layout.StackPane videoIcon = new javafx.scene.layout.StackPane();
                
                // Outer rectangle (TV/monitor)
                javafx.scene.shape.Rectangle tv = new javafx.scene.shape.Rectangle(80, 60);
                tv.setFill(javafx.scene.paint.Color.TRANSPARENT);
                tv.setStroke(javafx.scene.paint.Color.LIGHTGRAY);
                tv.setStrokeWidth(2);
                tv.setArcWidth(8);
                tv.setArcHeight(8);
                
                // Play button in the middle
                javafx.scene.shape.Polygon play = new javafx.scene.shape.Polygon();
                play.getPoints().addAll(
                    -15.0, -15.0,  // Top left
                    15.0, 0.0,     // Middle right
                    -15.0, 15.0    // Bottom left
                );
                play.setFill(javafx.scene.paint.Color.LIGHTGRAY);
                
                videoIcon.getChildren().addAll(tv, play);
                
                // Replace the ImageView with our custom icon
                        int index = noContentPlaceholder.getChildren().indexOf(placeholderImage);
                        if (index >= 0) {
                    noContentPlaceholder.getChildren().set(index, videoIcon);
                }
            }
            
            videoPlaceholder.setVisible(false);
            noContentPlaceholder.setVisible(true);
            mediaControls.setVisible(false);
            
        } catch (Exception e) {
            System.err.println("Error showing no content message: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Update the navigation buttons based on current position
     */
    private void updateNavigationButtons() {
        if (lessons == null || lessons.isEmpty()) {
            previousLessonButton.setDisable(true);
            nextLessonButton.setDisable(true);
            return;
        }
        
        previousLessonButton.setDisable(currentLessonIndex <= 0);
        nextLessonButton.setDisable(currentLessonIndex >= lessons.size() - 1);
    }
    
    /**
     * Handle fullscreen button click
     */
    @FXML
    private void handleFullScreen() {
        Stage stage = (Stage) mediaContainer.getScene().getWindow();
        stage.setFullScreen(!stage.isFullScreen());
    }
    
    /**
     * Toggle sidebar visibility
     */
    @FXML
    private void toggleSidebar() {
        sidebarVisible.set(!sidebarVisible.get());
    }
    
    /**
     * Update the toggle sidebar button icon
     */
    private void updateToggleButtonIcon() {
        try {
            if (sidebarVisible.get()) {
                // Create a "hide sidebar" icon (three lines with left arrow)
                VBox menuIcon = new VBox(4);
                menuIcon.setAlignment(Pos.CENTER_LEFT);
                
                for (int i = 0; i < 3; i++) {
                    javafx.scene.shape.Rectangle line = new javafx.scene.shape.Rectangle(16, 2);
                    line.setFill(javafx.scene.paint.Color.WHITE);
                    menuIcon.getChildren().add(line);
                }
                
                // Add a left-pointing arrow
                javafx.scene.shape.Polygon arrow = new javafx.scene.shape.Polygon();
                arrow.getPoints().addAll(
                    8.0, 0.0,  // Top right
                    0.0, 6.0,  // Middle left
                    8.0, 12.0  // Bottom right
                );
                arrow.setFill(javafx.scene.paint.Color.WHITE);
                arrow.setTranslateY(6);
                
                javafx.scene.layout.StackPane icon = new javafx.scene.layout.StackPane();
                icon.getChildren().addAll(menuIcon, arrow);
                
                toggleSidebarButton.setGraphic(icon);
                } else {
                // Create a "show sidebar" icon (three lines with right arrow)
                VBox menuIcon = new VBox(4);
                menuIcon.setAlignment(Pos.CENTER_LEFT);
                
                for (int i = 0; i < 3; i++) {
                    javafx.scene.shape.Rectangle line = new javafx.scene.shape.Rectangle(16, 2);
                    line.setFill(javafx.scene.paint.Color.WHITE);
                    menuIcon.getChildren().add(line);
                }
                
                // Add a right-pointing arrow
                javafx.scene.shape.Polygon arrow = new javafx.scene.shape.Polygon();
                arrow.getPoints().addAll(
                    0.0, 0.0,  // Top left
                    8.0, 6.0,  // Middle right
                    0.0, 12.0  // Bottom left
                );
                arrow.setFill(javafx.scene.paint.Color.WHITE);
                arrow.setTranslateY(6);
                
                javafx.scene.layout.StackPane icon = new javafx.scene.layout.StackPane();
                icon.getChildren().addAll(menuIcon, arrow);
                
                toggleSidebarButton.setGraphic(icon);
            }
        } catch (Exception e) {
            System.err.println("Error updating sidebar toggle icon: " + e.getMessage());
        }
    }
    
    /**
     * Go to previous lesson
     */
    @FXML
    private void handlePreviousLesson() {
        if (currentLessonIndex > 0) {
            currentLessonIndex--;
            lessonsListView.getSelectionModel().select(currentLessonIndex);
        }
    }
    
    /**
     * Go to next lesson
     */
    @FXML
    private void handleNextLesson() {
        if (currentLessonIndex < lessons.size() - 1) {
            currentLessonIndex++;
            lessonsListView.getSelectionModel().select(currentLessonIndex);
        }
    }
    
    /**
     * Handle notes button click
     */
    @FXML
    private void handleNotes() {
        toggleNotesPanel();
    }
    
    /**
     * Handle resources button click
     */
    @FXML
    private void handleResources() {
        showAlert(Alert.AlertType.INFORMATION, "Coming Soon", 
            "Resources Feature", "The resources feature is coming soon!");
    }
    
    /**
     * Handle back button click
     */
    @FXML
    private void handleBack() {
        try {
            // Stop timeline
            if (timelineUpdater != null) {
                timelineUpdater.stop();
            }
            
            // Return to course details
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/course/course_details.fxml"));
            Parent root = loader.load();
            
            // Get the course details controller
            org.example.controller.course.CourseDetailsController controller = loader.getController();
            
            // Set course first to initialize the UI
            controller.setCourse(currentCourse);
            
            // Then set the user ID to check enrollment status and update UI accordingly
            if (currentUserId > 0) {
                controller.setCurrentUserId(currentUserId);
            }
            
            // Set the scene
            Scene scene = mediaContainer.getScene();
            
            // Clear all existing stylesheets to prevent style contamination
            scene.getStylesheets().clear();
            
            // Add the course details CSS
            String courseDetailsCss = getClass().getResource("/css/course_details.css").toExternalForm();
            scene.getStylesheets().add(courseDetailsCss);
            
            // Set the new root
            scene.setRoot(root);
            
            System.out.println("Navigated back to course details");
            
        } catch (IOException e) {
            System.err.println("Error returning to course details: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Navigation Error", 
                "Could not return to course details", e.getMessage());
        }
    }
    
    /**
     * Set the current user ID
     */
    public void setCurrentUserId(int userId) {
        this.currentUserId = userId;
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
    
    /**
     * Setup the back button with a proper icon
     */
    private void setupBackButton() {
        try {
            // Create a back arrow shape
                        javafx.scene.shape.Polygon arrow = new javafx.scene.shape.Polygon();
                        arrow.getPoints().addAll(
                20.0, 0.0,  // Top right
                0.0, 12.0,  // Middle left
                20.0, 24.0  // Bottom right
                        );
                        arrow.setFill(javafx.scene.paint.Color.WHITE);
                        
                        // Use this shape as the graphic
                        backButton.setGraphic(arrow);
            
        } catch (Exception e) {
            System.err.println("Error setting up back button: " + e.getMessage());
        }
    }
    
    /**
     * Setup play/pause button with proper icon
     */
    private void setupPlayPauseButton() {
        try {
            // Create a play triangle
            javafx.scene.shape.Polygon playIcon = new javafx.scene.shape.Polygon();
            playIcon.getPoints().addAll(
                0.0, 0.0,   // Top left
                20.0, 10.0, // Middle right
                0.0, 20.0   // Bottom left
            );
            playIcon.setFill(javafx.scene.paint.Color.WHITE);
            
            // Set the initial graphic based on play state
            playPauseButton.setGraphic(playIcon);
            
            // Update the play/pause button when the isPlaying property changes
            isPlaying.addListener((obs, wasPlaying, isNowPlaying) -> {
                updateTogglePlayIcon();
            });
            
        } catch (Exception e) {
            System.err.println("Error setting up play/pause button: " + e.getMessage());
        }
    }
    
    /**
     * Setup fullscreen button with proper icon
     */
    private void setupFullScreenButton() {
        try {
            // Create a fullscreen icon
            javafx.scene.layout.StackPane fullscreenIcon = new javafx.scene.layout.StackPane();
            
            javafx.scene.shape.Rectangle outer = new javafx.scene.shape.Rectangle(20, 20);
            outer.setFill(javafx.scene.paint.Color.TRANSPARENT);
            outer.setStroke(javafx.scene.paint.Color.WHITE);
            outer.setStrokeWidth(2);
            
            javafx.scene.shape.Rectangle inner = new javafx.scene.shape.Rectangle(10, 10);
            inner.setFill(javafx.scene.paint.Color.TRANSPARENT);
            inner.setStroke(javafx.scene.paint.Color.WHITE);
            inner.setStrokeWidth(1);
            inner.setTranslateX(2);
            inner.setTranslateY(2);
            
            fullscreenIcon.getChildren().addAll(outer, inner);
            fullScreenButton.setGraphic(fullscreenIcon);
            
        } catch (Exception e) {
            System.err.println("Error setting up fullscreen button: " + e.getMessage());
        }
    }
    
    /**
     * Setup toggle sidebar button with proper icon
     */
    private void setupToggleSidebarButton() {
        try {
            updateToggleButtonIcon();
        } catch (Exception e) {
            System.err.println("Error setting up toggle sidebar button: " + e.getMessage());
        }
    }
    
    /**
     * Update the play/pause button icon based on current state
     */
    private void updateTogglePlayIcon() {
        try {
            if (isPlaying.get()) {
                // Create pause icon (two vertical bars)
                HBox pauseIcon = new HBox(4);
                pauseIcon.setAlignment(Pos.CENTER);
                
                javafx.scene.shape.Rectangle bar1 = new javafx.scene.shape.Rectangle(6, 20);
                javafx.scene.shape.Rectangle bar2 = new javafx.scene.shape.Rectangle(6, 20);
                bar1.setFill(javafx.scene.paint.Color.WHITE);
                bar2.setFill(javafx.scene.paint.Color.WHITE);
                
                pauseIcon.getChildren().addAll(bar1, bar2);
                playPauseButton.setGraphic(pauseIcon);
            } else {
                // Create play triangle
                javafx.scene.shape.Polygon playIcon = new javafx.scene.shape.Polygon();
                playIcon.getPoints().addAll(
                    0.0, 0.0,   // Top left
                    20.0, 10.0, // Middle right
                    0.0, 20.0   // Bottom left
                );
                playIcon.setFill(javafx.scene.paint.Color.WHITE);
                playPauseButton.setGraphic(playIcon);
            }
        } catch (Exception e) {
            System.err.println("Error updating play/pause icon: " + e.getMessage());
        }
    }
    
    /**
     * Initialize the notes panel
     */
    private void initializeNotesPanel() {
        try {
            // Create notes panel
            notesPanel = new VBox(10);
            notesPanel.setPrefWidth(300);
            notesPanel.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 15px; -fx-border-width: 0 0 0 1; -fx-border-color: #ddd;");
            notesPanel.setVisible(false);
            notesPanel.setManaged(false);
            
            // Create header for notes panel
            HBox header = new HBox(10);
            header.setAlignment(Pos.CENTER_LEFT);
            
            Label notesLabel = new Label("Lesson Notes");
            notesLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
            
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            
            Button closeButton = new Button("×");
            closeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #555; -fx-font-size: 18px; -fx-cursor: hand;");
            closeButton.setOnAction(event -> toggleNotesPanel());
            
            header.getChildren().addAll(notesLabel, spacer, closeButton);
            
            // Create instructional text
            Label instructionLabel = new Label("Take notes for this lesson. Notes are saved automatically when you click save.");
            instructionLabel.setWrapText(true);
            instructionLabel.setStyle("-fx-text-fill: #555555;");
            
            // Create note text area
            noteTextArea = new TextArea();
            noteTextArea.setPrefHeight(400);
            noteTextArea.setWrapText(true);
            noteTextArea.setPromptText("Type your notes here...");
            VBox.setVgrow(noteTextArea, Priority.ALWAYS);
            
            // Create character count label
            Label charCountLabel = new Label("0/250 characters");
            charCountLabel.setStyle("-fx-text-fill: #555555;");
            
            // Add listener to update character count
            noteTextArea.textProperty().addListener((observable, oldValue, newValue) -> {
                int count = newValue.length();
                charCountLabel.setText(count + "/250 characters");
                
                // Change style if over limit
                if (count > 250) {
                    charCountLabel.setStyle("-fx-text-fill: red;");
                    saveNoteButton.setDisable(true);
                } else {
                    charCountLabel.setStyle("-fx-text-fill: #555555;");
                    saveNoteButton.setDisable(false);
                }
            });
            
            // Create save button
            saveNoteButton = new Button("Save Note");
            saveNoteButton.setStyle("-fx-background-color: #3f51b5; -fx-text-fill: white; -fx-cursor: hand;");
            saveNoteButton.setPrefWidth(Double.MAX_VALUE);
            saveNoteButton.setOnAction(event -> saveNote());
            
            // Add components to notes panel
            notesPanel.getChildren().addAll(header, instructionLabel, noteTextArea, charCountLabel, saveNoteButton);
            
            // Set up visibility binding
            notesPanelVisible.addListener((obs, wasVisible, isNowVisible) -> {
                notesPanel.setVisible(isNowVisible);
                notesPanel.setManaged(isNowVisible);
                
                // If showing the panel, focus the text area
                if (isNowVisible) {
                    Platform.runLater(() -> noteTextArea.requestFocus());
                }
            });
            
            System.out.println("Notes panel initialized");
        } catch (Exception e) {
            System.err.println("Error initializing notes panel: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Toggle notes panel visibility
     */
    private void toggleNotesPanel() {
        try {
            notesPanelVisible.set(!notesPanelVisible.get());
            
            // Get the main BorderPane
            BorderPane root = (BorderPane) mediaContainer.getScene().getRoot();
            
            // Update right panel based on visibility
            if (notesPanelVisible.get()) {
                root.setRight(notesPanel);
            } else {
                root.setRight(null);
            }
            
            System.out.println("Notes panel visibility toggled: " + notesPanelVisible.get());
        } catch (Exception e) {
            System.err.println("Error toggling notes panel: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Load notes for the current lesson
     */
    private void loadNotes(int lessonId) {
        try {
            if (currentUserId > 0) {
                // Try to get existing note
                currentNote = noteService.getNoteByLessonAndUser(lessonId, currentUserId);
                
                // Set text area content
                if (currentNote != null) {
                    noteTextArea.setText(currentNote.getNote_text());
                } else {
                    noteTextArea.setText("");
                }
            } else {
                // No user logged in
                noteTextArea.setText("");
                noteTextArea.setDisable(true);
                noteTextArea.setPromptText("Please log in to add notes");
                saveNoteButton.setDisable(true);
            }
        } catch (SQLException e) {
            System.err.println("Error loading notes: " + e.getMessage());
            noteTextArea.setText("");
            noteTextArea.setPromptText("Error loading notes");
        }
    }
    
    /**
     * Save the current note
     */
    private void saveNote() {
        try {
            if (currentUserId <= 0) {
                showAlert(Alert.AlertType.WARNING, "Not Logged In", 
                    "Login Required", "Please log in to save notes.");
                return;
            }
            
            if (lessons.isEmpty() || currentLessonIndex < 0 || currentLessonIndex >= lessons.size()) {
                showAlert(Alert.AlertType.WARNING, "No Lesson Selected", 
                    "No Lesson", "Please select a lesson first.");
                return;
            }
            
            int lessonId = lessons.get(currentLessonIndex).getId();
            String noteText = noteTextArea.getText().trim();
            
            // Enforce character limit
            if (noteText.length() > 250) {
                showAlert(Alert.AlertType.WARNING, "Note Too Long", 
                    "Character Limit Exceeded", "Notes are limited to 250 characters. Please shorten your note.");
                return;
            }
            
            if (noteText.isEmpty()) {
                // If note is empty and there's an existing note, delete it
                if (currentNote != null) {
                    noteService.deleteNote(currentNote.getId());
                    currentNote = null;
                    System.out.println("Note deleted");
                    showAlert(Alert.AlertType.INFORMATION, "Note Deleted", 
                        "Note Deleted", "Your note has been deleted.");
                }
            } else {
                // If there's already a note, update it
                if (currentNote != null) {
                    currentNote.setNote_text(noteText);
                    noteService.updateNote(currentNote);
                    System.out.println("Note updated");
                } else {
                    // Create a new note
                    Note newNote = new Note(currentUserId, lessonId, noteText);
                    currentNote = noteService.addNote(newNote);
                    System.out.println("New note created");
                }
                
                showAlert(Alert.AlertType.INFORMATION, "Note Saved", 
                    "Note Saved", "Your note has been saved successfully.");
            }
        } catch (SQLException e) {
            System.err.println("Error saving note: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", 
                "Error Saving Note", "There was an error saving your note: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error in saveNote: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", 
                "Unexpected Error", "An unexpected error occurred: " + e.getMessage());
        }
    }
    
    /**
     * Initialize break reminder components
     */
    private void initializeBreakReminder() {
        try {
            // Create new components if not found in FXML
            if (breakReminderButton == null) {
                breakReminderButton = new Button("Remind Me to Take a Break");
                breakReminderButton.getStyleClass().add("action-button");
                breakReminderButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                breakReminderButton.setOnAction(event -> handleBreakReminder());
                
                // Add to top HBox after notes and resources buttons
                if (notesButton != null && notesButton.getParent() instanceof HBox) {
                    HBox topHBox = (HBox) notesButton.getParent();
                    topHBox.getChildren().add(breakReminderButton);
                }
            }
            
            if (breakTimerLabel == null) {
                breakTimerLabel = new Label("");
                breakTimerLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                breakTimerLabel.setVisible(false);
                
                // Add to top HBox
                if (notesButton != null && notesButton.getParent() instanceof HBox) {
                    HBox topHBox = (HBox) notesButton.getParent();
                    topHBox.getChildren().add(breakTimerLabel);
                }
            }
            
            // Set up bindings
            breakReminderSet.addListener((obs, wasSet, isNowSet) -> {
                if (breakReminderButton != null) {
                    breakReminderButton.setText(isNowSet ? "Stop Break Timer" : "Remind Me to Take a Break");
                }
                
                if (breakTimerLabel != null) {
                    breakTimerLabel.setVisible(isNowSet);
                }
            });
            
        } catch (Exception e) {
            System.err.println("Error initializing break reminder: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Handle break reminder button click
     */
    @FXML
    private void handleBreakReminder() {
        if (breakReminderSet.get()) {
            // Timer is already running, stop it
            if (breakTimer != null) {
                breakTimer.stop();
                breakTimer = null;
            }
            breakReminderSet.set(false);
            showAlert(Alert.AlertType.INFORMATION, "Break Timer Stopped", 
                    "Break reminder has been stopped", 
                    "The break reminder has been cancelled.");
        } else {
            // Show dialog to set timer
            Pair<Integer, Pair<Integer, Integer>> result = BreakReminderDialog.showDialog();
            
            if (result != null) {
                int hours = result.getKey();
                int minutes = result.getValue().getKey();
                int seconds = result.getValue().getValue();
                
                // Create and start timer
                breakTimer = new BreakTimer(
                    hours, 
                    minutes, 
                    seconds,
                    unused -> handleTimerComplete(),
                    timeStr -> updateBreakTimerDisplay(timeStr)
                );
                
                breakTimer.start();
                breakReminderSet.set(true);
                
                showAlert(Alert.AlertType.INFORMATION, "Break Timer Started", 
                        "Break reminder set", 
                        String.format("You will be reminded to take a break after %02d:%02d:%02d", 
                                hours, minutes, seconds));
            }
        }
    }
    
    /**
     * Update the break timer display
     */
    private void updateBreakTimerDisplay(String timeString) {
        if (breakTimerLabel != null) {
            breakTimerLabel.setText("Break in: " + timeString);
        }
    }
    
    /**
     * Handle timer completion
     */
    private void handleTimerComplete() {
        boolean continueStudying = BreakTimer.showBreakReminder();
        
        if (continueStudying) {
            // User chose to continue studying - ask if they want to set another reminder
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Set Another Reminder?");
            confirmation.setHeaderText("Would you like to set another break reminder?");
            confirmation.setContentText("Regular breaks improve learning effectiveness.");
            
            ButtonType yesButton = new ButtonType("Yes", ButtonBar.ButtonData.YES);
            ButtonType noButton = new ButtonType("No", ButtonBar.ButtonData.NO);
            confirmation.getButtonTypes().setAll(yesButton, noButton);
            
            confirmation.showAndWait().ifPresent(buttonType -> {
                if (buttonType == yesButton) {
                    // Set another reminder
                    Platform.runLater(() -> handleBreakReminder());
                } else {
                    breakReminderSet.set(false);
                }
            });
        } else {
            // User chose to take a break - reset the reminder state
            breakReminderSet.set(false);
        }
    }
    
    @Override
    protected void finalize() throws Throwable {
        // Stop break timer when controller is garbage collected
        if (breakTimer != null) {
            breakTimer.stop();
        }
        super.finalize();
    }
} 