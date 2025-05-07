package org.example.controller.forum;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.example.models.forum.Post;
import org.example.models.forum.Comment;
import org.example.models.forum.Tag;
import org.example.services.forum.PostService;
import org.example.services.forum.CommentService;
import org.example.services.forum.TagService;
import org.example.services.forum.PostReactionService;
import org.example.services.UserService;
import org.example.utils.InputValidator;
import org.example.utils.InputValidator.ValidationResult;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

public class ForumController {
    @FXML
    private ListView<String> forumListView;
    @FXML
    private TextArea postTextField;
    @FXML
    private TextField titleTextField;
    @FXML
    private TextField tagTextField;
    @FXML
    private VBox postDetailsBox;
    @FXML
    private TextArea commentTextArea;
    @FXML
    private ListView<String> commentsListView;
    @FXML
    private Label likesLabel;
    @FXML
    private Label dislikesLabel;
    @FXML
    private Button postButton;
    @FXML
    private Label postAuthorLabel;
    @FXML
    private Label postDateLabel;
    @FXML
    private Button addCommentButton;

    private boolean editMode = false;
    private PostService postService;
    private CommentService commentService;
    private TagService tagService;
    private PostReactionService reactionService;
    private Post selectedPost;
    private Comment selectedComment;
    private int currentUserId = 1; // Temporary hardcoded user ID
    private boolean editingComment = false;
    private boolean replyingToComment = false;
    private UserService userService;

    @FXML
    private void initialize() {
        try {
            // Initialize services only once
            postService = new PostService();
            commentService = new CommentService();
            tagService = new TagService();
            reactionService = new PostReactionService();
            userService = new UserService();
            
            // Try to get a valid user
            initializeValidUser();
            
            loadPosts();
            
            forumListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    try {
                        String[] parts = newVal.split(" - ", 2);
                        if (parts.length >= 1) {
                            int postId = Integer.parseInt(parts[0].trim());
                            selectedPost = postService.read(postId);
                            if (selectedPost != null) {
                                showPostDetails(selectedPost);
                            } else {
                                Platform.runLater(() -> showError("Post not found"));
                            }
                        }
                    } catch (NumberFormatException e) {
                        Platform.runLater(() -> showError("Invalid post ID format: " + e.getMessage()));
                    } catch (SQLException e) {
                        Platform.runLater(() -> showError("Database error: " + e.getMessage()));
                        e.printStackTrace(); // Log full stack trace
                    } catch (Exception e) {
                        Platform.runLater(() -> showError("Unexpected error: " + e.getMessage()));
                        e.printStackTrace(); // Log full stack trace
                    }
                }
            });
        } catch (Exception e) {
            Platform.runLater(() -> showError("Error initializing forum: " + e.getMessage()));
            e.printStackTrace(); // Log full stack trace
        }
    }

    private void initializeValidUser() {
        try {
            // Try to find a valid user or create one if none exists
            currentUserId = userService.getOrCreateValidUserId();
        } catch (SQLException e) {
            Platform.runLater(() -> showError("Error initializing user: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    private void loadPosts() {
        try {
            forumListView.getItems().clear();
            List<Post> posts = postService.readAll();
            for (Post post : posts) {
                try {
                    String userInfo = userService.getUsernameById(post.getOwnerId());
                    forumListView.getItems().add(post.getId() + " - " + post.getTitle() + " (par " + userInfo + ")");
                } catch (SQLException e) {
                    // En cas d'erreur, afficher sans l'information d'utilisateur
                    forumListView.getItems().add(post.getId() + " - " + post.getTitle());
                    System.out.println("Error getting user info for post " + post.getId() + ": " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            showError("Error loading posts: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showPostDetails(Post post) {
        // Hide initially for animation
        postDetailsBox.setOpacity(0);
        postDetailsBox.setScaleX(0.97);
        postDetailsBox.setScaleY(0.97);
        postDetailsBox.setVisible(true);
        
        // Switch to edit mode
        editMode = true;
        titleTextField.setText(post.getTitle());
        postTextField.setText(post.getContent());
        
        // Ajouter le nom d'utilisateur et la date aux informations de post
        try {
            String userInfo = userService.getUsernameById(post.getOwnerId());
            postButton.setText("Update Post");
            
            // Mettre à jour les labels d'auteur et de date
            if (postAuthorLabel != null) {
                postAuthorLabel.setText(userInfo);
            }
            if (postDateLabel != null) {
                postDateLabel.setText("Date: " + post.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            }
        } catch (SQLException e) {
            postButton.setText("Update Post");
            
            // Gérer l'erreur pour les labels
            if (postAuthorLabel != null) {
                postAuthorLabel.setText("User ID: " + post.getOwnerId());
            }
            if (postDateLabel != null) {
                postDateLabel.setText("Date: " + (post.getCreatedAt() != null ? 
                    post.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : 
                    "Non disponible"));
            }
            
            System.out.println("Error getting username: " + e.getMessage());
        }
        
        try {
            // Load tags
            List<Tag> tags = tagService.getTagsForPost(post.getId());
            StringBuilder tagString = new StringBuilder();
            for (Tag tag : tags) {
                tagString.append(tag.getName()).append(", ");
            }
            // Remove trailing comma if exists
            if (tagString.length() > 2) {
                tagString.setLength(tagString.length() - 2);
            }
            tagTextField.setText(tagString.toString());
            
            // Load comments
            loadComments(post.getId());
            
            // Load reactions
            updateReactionCounts(post.getId());
            
            // Animate post details display
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), postDetailsBox);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            
            ScaleTransition scaleIn = new ScaleTransition(Duration.millis(300), postDetailsBox);
            scaleIn.setFromX(0.97);
            scaleIn.setFromY(0.97);
            scaleIn.setToX(1);
            scaleIn.setToY(1);
            
            fadeIn.play();
            scaleIn.play();
            
        } catch (SQLException e) {
            showError("Error loading post details: " + e.getMessage());
            e.printStackTrace(); // Log full stack trace
        }
    }

    private void loadComments(int postId) {
        try {
            commentsListView.getItems().clear();
            List<Comment> comments = commentService.readByPostId(postId);
            
            if (comments.isEmpty()) {
                commentsListView.getItems().add("Aucun commentaire pour le moment");
            } else {
                // Group comments by their parent IDs
                Map<Integer, List<Comment>> commentsByParent = new HashMap<>();
                List<Comment> topLevelComments = new ArrayList<>();
                
                // Organize comments into parent-child groups
                for (Comment comment : comments) {
                    if (comment.getParentId() == null) {
                        topLevelComments.add(comment);
                    } else {
                        commentsByParent.computeIfAbsent(comment.getParentId(), k -> new ArrayList<>())
                                        .add(comment);
                    }
                }
                
                // Add comments recursively with indentation
                addCommentsToListView(topLevelComments, commentsByParent, 0);
            }
            
            // Assurez-vous que la ListView est visible
            commentsListView.setVisible(true);
            
            // Log pour le débogage
            System.out.println("Chargement de " + comments.size() + " commentaires");
        } catch (SQLException e) {
            showError("Error loading comments: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void addCommentsToListView(List<Comment> comments, Map<Integer, List<Comment>> commentsByParent, int level) {
        if (comments == null) return;
        
        for (Comment comment : comments) {
            // Récupérer le nom d'utilisateur
            String userInfo;
            try {
                userInfo = userService.getUsernameById(comment.getOwnerId());
            } catch (SQLException e) {
                userInfo = "User ID: " + comment.getOwnerId();
                System.out.println("Error getting user info for comment " + comment.getId() + ": " + e.getMessage());
            }
            
            // Add indentation based on the level
            String indent = "";
            for (int i = 0; i < level; i++) {
                indent += "    ";
            }
            
            // Add reply indicator for nested comments
            String replyPrefix = level > 0 ? "↪ " : "";
            
            String formattedComment = String.format("%d - %s[%s] %s%s - %s", 
                comment.getId(),
                indent,
                userInfo,
                replyPrefix,
                comment.getContent(),
                comment.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                
            commentsListView.getItems().add(formattedComment);
            
            // Recursively add child comments
            List<Comment> childComments = commentsByParent.get(comment.getId());
            if (childComments != null) {
                addCommentsToListView(childComments, commentsByParent, level + 1);
            }
        }
    }

    private void updateReactionCounts(int postId) {
        try {
            int likes = reactionService.getLikeCount(postId);
            int dislikes = reactionService.getDislikeCount(postId);
            likesLabel.setText("Likes: " + likes);
            dislikesLabel.setText("Dislikes: " + dislikes);
        } catch (SQLException e) {
            showError("Error loading reaction counts: " + e.getMessage());
        }
    }

    @FXML
    private void handlePost() {
        String title = titleTextField.getText().trim();
        String content = postTextField.getText().trim();
        String tags = tagTextField.getText().trim();

        // Form validation
        ValidationResult titleResult = InputValidator.validateTitle(title);
        if (!titleResult.isValid()) {
            Platform.runLater(() -> showError(titleResult.getErrorMessage()));
            return;
        }
        
        ValidationResult contentResult = InputValidator.validatePostContent(content);
        if (!contentResult.isValid()) {
            Platform.runLater(() -> showError(contentResult.getErrorMessage()));
            return;
        }
        
        if (!tags.isEmpty()) {
            ValidationResult tagsResult = InputValidator.validateTags(tags);
            if (!tagsResult.isValid()) {
                Platform.runLater(() -> showError(tagsResult.getErrorMessage()));
                return;
            }
        }

        try {
            if (editMode && selectedPost != null) {
                // Update existing post
                selectedPost.setTitle(title);
                selectedPost.setContent(content);
                postService.update(selectedPost);
                
                // Handle tags (optional - you could update tags here if needed)
                
                // Reset UI
                loadPosts();
                clearFields();
                Platform.runLater(() -> showInfo("Post updated successfully"));
            } else {
                // Create new post
                Post post = new Post();
                post.setOwnerId(currentUserId);
                post.setTitle(title);
                post.setContent(content);
                
                try {
                    postService.create(post);
                } catch (SQLException e) {
                    // Check if it's a foreign key constraint error
                    if (e.getMessage().contains("foreign key constraint") && e.getMessage().contains("owner_id")) {
                        try {
                            // Try to reset the valid user and retry
                            Platform.runLater(() -> showWarning("User reference error. Attempting reset..."));
                            currentUserId = userService.getOrCreateValidUserId();
                            
                            // Retry creating the post with the new user ID
                            post.setOwnerId(currentUserId);
                            postService.create(post);
                        } catch (SQLException retryEx) {
                            Platform.runLater(() -> showError("Reset failed. Error: " + retryEx.getMessage()));
                            retryEx.printStackTrace();
                            return;
                        }
                    } else {
                        throw e; // Re-throw exception if it's not a foreign key constraint error
                    }
                }

                // Add tags
                if (!tags.isEmpty()) {
                    String[] tagNames = tags.split(",");
                    for (String tagName : tagNames) {
                        tagName = tagName.trim();
                        if (!tagName.isEmpty()) {
                            Tag tag = new Tag();
                            tag.setName(tagName);
                            tagService.create(tag);
                            tagService.addTagToPost(post.getId(), tag.getId());
                        }
                    }
                }

                // Animation effect for the button
                Button postButton = this.postButton;
                String originalStyle = postButton.getStyle();
                
                postButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 24; -fx-padding: 12 25; -fx-cursor: hand;");
                
                // Create scale animation
                ScaleTransition scaleUp = new ScaleTransition(Duration.millis(100), postButton);
                scaleUp.setFromX(1.0);
                scaleUp.setFromY(1.0);
                scaleUp.setToX(1.1);
                scaleUp.setToY(1.1);
                
                ScaleTransition scaleDown = new ScaleTransition(Duration.millis(100), postButton);
                scaleDown.setFromX(1.1);
                scaleDown.setFromY(1.1);
                scaleDown.setToX(1.0);
                scaleDown.setToY(1.0);
                
                scaleUp.setOnFinished(e -> {
                    scaleDown.play();
                });
                
                scaleDown.setOnFinished(e -> {
                    // Reset button style after animation
                    postButton.setStyle(originalStyle);
                    
                    // Reset form and interface
                    loadPosts();
                    clearFields();
                    
                    // Show success message
                    Platform.runLater(() -> showInfo("Post created successfully"));
                });
                
                scaleUp.play();
            }
        } catch (SQLException e) {
            String action = editMode ? "updating" : "creating";
            Platform.runLater(() -> showError("Error " + action + " post: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    @FXML
    private void handleComment() {
        if (selectedPost == null) {
            Platform.runLater(() -> showError("Please select a post first"));
            return;
        }

        String content = commentTextArea.getText().trim();
        
        // Form validation for comment
        ValidationResult commentResult = InputValidator.validateCommentContent(content);
        if (!commentResult.isValid()) {
            Platform.runLater(() -> showError(commentResult.getErrorMessage()));
            return;
        }

        // If we're in edit mode, redirect to handleUpdateComment
        if (editingComment) {
            handleUpdateComment();
            return;
        }

        try {
            Comment comment = new Comment();
            comment.setPostId(selectedPost.getId());
            comment.setOwnerId(currentUserId);
            comment.setContent(content);
            
            // Set parentId if replying to a comment
            if (replyingToComment && selectedComment != null) {
                comment.setParentId(selectedComment.getId());
            }
            
            try {
                commentService.create(comment);
                System.out.println("Commentaire créé avec ID: " + comment.getId());
            } catch (SQLException e) {
                // Check if it's a foreign key constraint error
                if (e.getMessage().contains("foreign key constraint") && (e.getMessage().contains("owner_id") || e.getMessage().contains("user"))) {
                    try {
                        // Try to reset the valid user and retry
                        Platform.runLater(() -> showWarning("User reference error. Attempting reset..."));
                        currentUserId = userService.getOrCreateValidUserId();
                        
                        // Retry creating the comment with the new user ID
                        comment.setOwnerId(currentUserId);
                        commentService.create(comment);
                        System.out.println("Commentaire créé après correction avec ID: " + comment.getId());
                    } catch (SQLException retryEx) {
                        Platform.runLater(() -> showError("Reset failed. Error: " + retryEx.getMessage()));
                        retryEx.printStackTrace();
                        return;
                    }
                } else {
                    throw e; // Re-throw exception if it's not a foreign key constraint error
                }
            }
            
            // Clear comment text area and reset state
            commentTextArea.clear();
            resetCommentUI();
            
            // Refresh comments list with animation
            try {
                loadCommentsWithAnimation(selectedPost.getId());
                
                String message = replyingToComment ? "Reply added successfully" : "Comment added successfully";
                selectedComment = null;
                replyingToComment = false;
                Platform.runLater(() -> showInfo(message));
            } catch (SQLException e) {
                // Fallback to regular load if animation fails
                loadComments(selectedPost.getId());
                Platform.runLater(() -> showInfo("Comment added successfully (with fallback refresh)"));
            }
        } catch (SQLException e) {
            Platform.runLater(() -> showError("Error adding comment: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLike() {
        if (selectedPost == null) return;
        try {
            reactionService.toggleReaction(selectedPost.getId(), currentUserId, true);
            updateReactionCounts(selectedPost.getId());
        } catch (SQLException e) {
            // Check if it's a foreign key constraint error
            if (e.getMessage().contains("foreign key constraint") && e.getMessage().contains("user_id")) {
                try {
                    // Try to reset the valid user and retry
                    currentUserId = userService.getOrCreateValidUserId();
                    reactionService.toggleReaction(selectedPost.getId(), currentUserId, true);
                    updateReactionCounts(selectedPost.getId());
                } catch (SQLException retryEx) {
                    Platform.runLater(() -> showError("Error updating reaction: " + retryEx.getMessage()));
                }
            } else {
                Platform.runLater(() -> showError("Error updating reaction: " + e.getMessage()));
            }
        }
    }

    @FXML
    private void handleDislike() {
        if (selectedPost == null) return;
        try {
            reactionService.toggleReaction(selectedPost.getId(), currentUserId, false);
            updateReactionCounts(selectedPost.getId());
        } catch (SQLException e) {
            // Check if it's a foreign key constraint error
            if (e.getMessage().contains("foreign key constraint") && e.getMessage().contains("user_id")) {
                try {
                    // Try to reset the valid user and retry
                    currentUserId = userService.getOrCreateValidUserId();
                    reactionService.toggleReaction(selectedPost.getId(), currentUserId, false);
                    updateReactionCounts(selectedPost.getId());
                } catch (SQLException retryEx) {
                    Platform.runLater(() -> showError("Error updating reaction: " + retryEx.getMessage()));
                }
            } else {
                Platform.runLater(() -> showError("Error updating reaction: " + e.getMessage()));
            }
        }
    }

    @FXML
    private void handleDeletePost() {
        if (selectedPost == null) {
            Platform.runLater(() -> showError("No post selected"));
            return;
        }
        
        // Confirm deletion
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Post");
        alert.setHeaderText("Delete Post");
        alert.setContentText("Are you sure you want to delete this post? This action cannot be undone.");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    int postId = selectedPost.getId();
                    System.out.println("Deleting post ID: " + postId);
                    
                    // 1. Delete all reactions for this post
                    try {
                        reactionService.deleteAllForPost(postId);
                    } catch (SQLException e) {
                        System.out.println("Error deleting reactions: " + e.getMessage());
                    }
                    
                    // 2. Delete all tag associations for this post
                    try {
                        tagService.removeAllTagsFromPost(postId);
                    } catch (SQLException e) {
                        System.out.println("Error removing tag associations: " + e.getMessage());
                    }
                    
                    // 3. Delete all comments
                    List<Comment> comments = commentService.readByPostId(postId);
                    for (Comment comment : comments) {
                        try {
                            commentService.delete(comment.getId());
                        } catch (SQLException e) {
                            System.out.println("Error deleting comment " + comment.getId() + ": " + e.getMessage());
                        }
                    }
                    
                    // 4. Finally delete the post
                    postService.delete(postId);
                    
                    // Update UI
                    loadPosts();
                    clearFields();
                    postDetailsBox.setVisible(false);
                    selectedPost = null;
                    showInfo("Post deleted successfully");
                } catch (SQLException e) {
                    Platform.runLater(() -> showError("Error deleting post: " + e.getMessage()));
                    e.printStackTrace();
                }
            }
        });
    }
    
    @FXML
    private void handleDeleteComment() {
        String selected = commentsListView.getSelectionModel().getSelectedItem();
        if (selected == null || selected.equals("Aucun commentaire pour le moment")) {
            showError("No comment selected");
            return;
        }
        
        // Confirm deletion
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Comment");
        alert.setHeaderText("Delete Comment");
        alert.setContentText("Are you sure you want to delete this comment? This action cannot be undone.");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    int index = commentsListView.getSelectionModel().getSelectedIndex();
                    List<Comment> comments = commentService.readByPostId(selectedPost.getId());
                    
                    if (index >= 0 && index < comments.size()) {
                        selectedComment = comments.get(index);
                        commentService.delete(selectedComment.getId());
                        loadComments(selectedPost.getId());
                        showInfo("Comment deleted successfully");
                    }
                } catch (SQLException e) {
                    showError("Error deleting comment: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML
    private void handleEditComment() {
        String selected = commentsListView.getSelectionModel().getSelectedItem();
        if (selected == null || selected.equals("Aucun commentaire pour le moment")) {
            showError("No comment selected");
            return;
        }
        
        try {
            int index = commentsListView.getSelectionModel().getSelectedIndex();
            List<Comment> comments = commentService.readByPostId(selectedPost.getId());
            
            if (index >= 0 && index < comments.size()) {
                selectedComment = comments.get(index);
                // Populate the text area with the comment content
                commentTextArea.setText(selectedComment.getContent());
                // Change the button text or provide some indication that we're editing
                editingComment = true;
                
                // Show username of comment author
                String userInfo;
                try {
                    userInfo = userService.getUsernameById(selectedComment.getOwnerId());
                } catch (SQLException e) {
                    userInfo = "User ID: " + selectedComment.getOwnerId();
                    System.out.println("Error getting user info: " + e.getMessage());
                }
                showInfo("Editing comment by " + userInfo + ". Press 'Update Comment' when done.");
            }
        } catch (SQLException e) {
            showError("Error editing comment: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleReplyComment() {
        String selected = commentsListView.getSelectionModel().getSelectedItem();
        if (selected == null || selected.equals("Aucun commentaire pour le moment")) {
            showError("No comment selected");
            return;
        }
        
        try {
            int index = commentsListView.getSelectionModel().getSelectedIndex();
            List<Comment> comments = commentService.readByPostId(selectedPost.getId());
            
            // Find the actual comment from the flattened view
            Comment actualComment = null;
            int count = 0;
            
            Map<Integer, List<Comment>> commentsByParent = new HashMap<>();
            List<Comment> topLevelComments = new ArrayList<>();
            
            // Group comments by their parent IDs
            for (Comment comment : comments) {
                if (comment.getParentId() == null) {
                    topLevelComments.add(comment);
                } else {
                    commentsByParent.computeIfAbsent(comment.getParentId(), k -> new ArrayList<>())
                                    .add(comment);
                }
            }
            
            List<Comment> flattenedComments = new ArrayList<>();
            flattenCommentsList(topLevelComments, commentsByParent, flattenedComments);
            
            if (index >= 0 && index < flattenedComments.size()) {
                selectedComment = flattenedComments.get(index);
                
                // Set up for reply mode
                replyingToComment = true;
                editingComment = false;
                
                // Clear textarea and focus it
                commentTextArea.clear();
                commentTextArea.requestFocus();
                
                // Update the UI to show we're in reply mode
                if (addCommentButton != null) {
                    addCommentButton.setText("Envoyer Réponse");
                    addCommentButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 15; -fx-cursor: hand;");
                }
                
                // Set prompt text to indicate reply mode
                commentTextArea.setPromptText("Répondre au commentaire de " + getUserDisplayName(selectedComment.getOwnerId()) + "...");
                
                // Show username of comment author being replied to
                String userInfo = getUserDisplayName(selectedComment.getOwnerId());
                showInfo("Replying to " + userInfo + ". Write your reply and press 'Send Reply'.");
            }
        } catch (SQLException e) {
            showError("Error preparing reply: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private String getUserDisplayName(int userId) {
        try {
            return userService.getUsernameById(userId);
        } catch (SQLException e) {
            System.out.println("Error getting user info: " + e.getMessage());
            return "User ID: " + userId;
        }
    }
    
    private void flattenCommentsList(List<Comment> comments, Map<Integer, List<Comment>> commentsByParent, List<Comment> result) {
        if (comments == null) return;
        
        for (Comment comment : comments) {
            result.add(comment);
            
            // Recursively add child comments
            List<Comment> childComments = commentsByParent.get(comment.getId());
            if (childComments != null) {
                flattenCommentsList(childComments, commentsByParent, result);
            }
        }
    }

    @FXML
    private void handleUpdateComment() {
        if (selectedComment == null) {
            showError("No comment selected for editing");
            return;
        }

        String content = commentTextArea.getText();
        if (content.isEmpty()) {
            showError("Comment cannot be empty");
            return;
        }

        try {
            // Get username for feedback message
            String userInfo = getUserDisplayName(selectedComment.getOwnerId());
            
            selectedComment.setContent(content);
            commentService.update(selectedComment);
            
            // Reset state
            commentTextArea.clear();
            selectedComment = null;
            editingComment = false;
            resetCommentUI();
            
            // Refresh comments
            loadComments(selectedPost.getId());
            showInfo("Comment by " + userInfo + " updated successfully");
        } catch (SQLException e) {
            showError("Error updating comment: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleNewPost() {
        // Clear the form and reset to creation mode
        clearFields();
        
        // Reset selection in the list view
        forumListView.getSelectionModel().clearSelection();
        selectedPost = null;
        
        // Ensure form is visible and focused
        titleTextField.requestFocus();
        
        // Hide post details section if visible
        postDetailsBox.setVisible(false);
    }

    private void clearFields() {
        titleTextField.clear();
        postTextField.clear();
        tagTextField.clear();
        postDetailsBox.setVisible(false);
        editMode = false;
        postButton.setText("Create Post");
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void loadCommentsWithAnimation(int postId) throws SQLException {
        List<Comment> comments = commentService.readByPostId(postId);
        
        commentsListView.getItems().clear();
        
        if (comments.isEmpty()) {
            commentsListView.getItems().add("Aucun commentaire pour le moment");
        } else {
            // Group comments by their parent IDs
            Map<Integer, List<Comment>> commentsByParent = new HashMap<>();
            List<Comment> topLevelComments = new ArrayList<>();
            
            // Organize comments into parent-child groups
            for (Comment comment : comments) {
                if (comment.getParentId() == null) {
                    topLevelComments.add(comment);
                } else {
                    commentsByParent.computeIfAbsent(comment.getParentId(), k -> new ArrayList<>())
                                    .add(comment);
                }
            }
            
            // Add comments recursively with indentation
            addCommentsToListView(topLevelComments, commentsByParent, 0);
        }
        
        System.out.println("Chargement avec animation de " + comments.size() + " commentaires");
        
        // Assurez-vous que la ListView est visible
        commentsListView.setVisible(true);
        
        // Apply fade transition to comment list
        FadeTransition fade = new FadeTransition(Duration.millis(300), commentsListView);
        fade.setFromValue(0.5);
        fade.setToValue(1.0);
        fade.play();
        
        // Scroll to last comment if there are any
        if (!comments.isEmpty()) {
            commentsListView.scrollTo(commentsListView.getItems().size() - 1);
        }
    }

    private void resetCommentUI() {
        replyingToComment = false;
        editingComment = false;
        selectedComment = null;
        commentTextArea.setPromptText("Ajouter ou modifier un commentaire...");
        
        if (addCommentButton != null) {
            addCommentButton.setText("Ajouter Commentaire");
            addCommentButton.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 15; -fx-cursor: hand;");
        }
    }
} 