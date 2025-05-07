package org.example.controller.forum;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.example.models.forum.Post;
import org.example.models.forum.Comment;
import org.example.models.forum.Tag;
import org.example.services.forum.PostService;
import org.example.services.forum.CommentService;
import org.example.services.forum.TagService;
import org.example.services.forum.PostReactionService;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

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

    private boolean editMode = false;
    private PostService postService;
    private CommentService commentService;
    private TagService tagService;
    private PostReactionService reactionService;
    private Post selectedPost;
    private Comment selectedComment;
    private int currentUserId = 1; // Temporary hardcoded user ID
    private boolean editingComment = false;

    @FXML
    private void initialize() {
        try {
            // Initialize services only once
            postService = new PostService();
            commentService = new CommentService();
            tagService = new TagService();
            reactionService = new PostReactionService();
            
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
                                showError("Post not found");
                            }
                        }
                    } catch (NumberFormatException e) {
                        showError("Invalid post ID format: " + e.getMessage());
                    } catch (SQLException e) {
                        showError("Database error: " + e.getMessage());
                        e.printStackTrace(); // Log full stack trace
                    } catch (Exception e) {
                        showError("Unexpected error: " + e.getMessage());
                        e.printStackTrace(); // Log full stack trace
                    }
                }
            });
        } catch (Exception e) {
            showError("Error initializing forum: " + e.getMessage());
            e.printStackTrace(); // Log full stack trace
        }
    }

    private void loadPosts() {
        try {
            forumListView.getItems().clear();
            List<Post> posts = postService.readAll();
            for (Post post : posts) {
                forumListView.getItems().add(post.getId() + " - " + post.getTitle());
            }
        } catch (SQLException e) {
            showError("Error loading posts: " + e.getMessage());
        }
    }

    private void showPostDetails(Post post) {
        postDetailsBox.setVisible(true);
        
        // Switch to edit mode
        editMode = true;
        titleTextField.setText(post.getTitle());
        postTextField.setText(post.getContent());
        postButton.setText("Update Post");
        
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
        } catch (SQLException e) {
            showError("Error loading post details: " + e.getMessage());
            e.printStackTrace(); // Log full stack trace
        }
    }

    private void loadComments(int postId) {
        try {
            commentsListView.getItems().clear();
            List<Comment> comments = commentService.readByPostId(postId);
            for (Comment comment : comments) {
                commentsListView.getItems().add(comment.getContent() + " - " + 
                    comment.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            }
        } catch (SQLException e) {
            showError("Error loading comments: " + e.getMessage());
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
        String title = titleTextField.getText();
        String content = postTextField.getText();
        String tags = tagTextField.getText();

        if (title.isEmpty() || content.isEmpty()) {
            showError("Title and content are required");
            return;
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
                showInfo("Post updated successfully");
            } else {
                // Create new post
                Post post = new Post();
                post.setOwnerId(currentUserId);
                post.setTitle(title);
                post.setContent(content);
                postService.create(post);

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

                loadPosts();
                clearFields();
                showInfo("Post created successfully");
            }
        } catch (SQLException e) {
            String action = editMode ? "updating" : "creating";
            showError("Error " + action + " post: " + e.getMessage());
        }
    }

    @FXML
    private void handleComment() {
        if (selectedPost == null) {
            showError("Please select a post first");
            return;
        }

        String content = commentTextArea.getText();
        if (content.isEmpty()) {
            showError("Comment cannot be empty");
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
            commentService.create(comment);
            
            loadComments(selectedPost.getId());
            commentTextArea.clear();
        } catch (SQLException e) {
            showError("Error adding comment: " + e.getMessage());
        }
    }

    @FXML
    private void handleLike() {
        if (selectedPost == null) return;
        try {
            reactionService.toggleReaction(selectedPost.getId(), currentUserId, true);
            updateReactionCounts(selectedPost.getId());
        } catch (SQLException e) {
            showError("Error updating reaction: " + e.getMessage());
        }
    }

    @FXML
    private void handleDislike() {
        if (selectedPost == null) return;
        try {
            reactionService.toggleReaction(selectedPost.getId(), currentUserId, false);
            updateReactionCounts(selectedPost.getId());
        } catch (SQLException e) {
            showError("Error updating reaction: " + e.getMessage());
        }
    }

    @FXML
    private void handleDeletePost() {
        if (selectedPost == null) {
            showError("No post selected");
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
                    // Delete all comments first (foreign key constraint)
                    List<Comment> comments = commentService.readByPostId(selectedPost.getId());
                    for (Comment comment : comments) {
                        commentService.delete(comment.getId());
                    }
                    
                    // Delete post
                    postService.delete(selectedPost.getId());
                    loadPosts();
                    clearFields();
                    postDetailsBox.setVisible(false);
                    selectedPost = null;
                } catch (SQLException e) {
                    showError("Error deleting post: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }
    
    @FXML
    private void handleDeleteComment() {
        String selected = commentsListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
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
        if (selected == null) {
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
                // Provide visual feedback
                showInfo("Editing comment. Press 'Update Comment' when done.");
            }
        } catch (SQLException e) {
            showError("Error editing comment: " + e.getMessage());
            e.printStackTrace();
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
            selectedComment.setContent(content);
            commentService.update(selectedComment);
            
            // Reset state
            commentTextArea.clear();
            selectedComment = null;
            editingComment = false;
            
            // Refresh comments
            loadComments(selectedPost.getId());
            showInfo("Comment updated successfully");
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
} 