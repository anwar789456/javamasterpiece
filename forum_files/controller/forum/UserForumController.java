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

public class UserForumController {
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
    private Label postTitleLabel;
    @FXML
    private Label postContentLabel;
    @FXML
    private Label postTagsLabel;
    @FXML
    private Label titleCountLabel;
    @FXML
    private Label contentCountLabel;
    @FXML
    private Label commentCountLabel;
    
    private PostService postService;
    private CommentService commentService;
    private TagService tagService;
    private PostReactionService reactionService;
    private Post selectedPost;
    private int currentUserId = 1; // Temporary hardcoded user ID
    private final int MAX_TITLE_LENGTH = 100;
    private final int MAX_CONTENT_LENGTH = 5000;
    private final int MAX_COMMENT_LENGTH = 1000;

    @FXML
    private void initialize() {
        try {
            postService = new PostService();
            commentService = new CommentService();
            tagService = new TagService();
            reactionService = new PostReactionService();
            
            loadPosts();
            
            // Ajouter des listeners pour compter les caractères
            titleTextField.textProperty().addListener((observable, oldValue, newValue) -> {
                int length = newValue.length();
                titleCountLabel.setText(length + "/" + MAX_TITLE_LENGTH);
                
                if (length > MAX_TITLE_LENGTH) {
                    titleTextField.setText(oldValue);
                    titleCountLabel.setText(oldValue.length() + "/" + MAX_TITLE_LENGTH);
                }
                
                // Changer la couleur si proche de la limite
                if (length > MAX_TITLE_LENGTH * 0.9) {
                    titleCountLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #ff5555;");
                } else {
                    titleCountLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #888888;");
                }
            });
            
            postTextField.textProperty().addListener((observable, oldValue, newValue) -> {
                int length = newValue.length();
                contentCountLabel.setText(length + "/" + MAX_CONTENT_LENGTH);
                
                if (length > MAX_CONTENT_LENGTH) {
                    postTextField.setText(oldValue);
                    contentCountLabel.setText(oldValue.length() + "/" + MAX_CONTENT_LENGTH);
                }
                
                // Changer la couleur si proche de la limite
                if (length > MAX_CONTENT_LENGTH * 0.9) {
                    contentCountLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #ff5555;");
                } else {
                    contentCountLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #888888;");
                }
            });
            
            commentTextArea.textProperty().addListener((observable, oldValue, newValue) -> {
                int length = newValue.length();
                commentCountLabel.setText(length + "/" + MAX_COMMENT_LENGTH);
                
                if (length > MAX_COMMENT_LENGTH) {
                    commentTextArea.setText(oldValue);
                    commentCountLabel.setText(oldValue.length() + "/" + MAX_COMMENT_LENGTH);
                }
                
                // Changer la couleur si proche de la limite
                if (length > MAX_COMMENT_LENGTH * 0.9) {
                    commentCountLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #ff5555;");
                } else {
                    commentCountLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #888888;");
                }
            });
            
            // Initialiser les compteurs
            titleCountLabel.setText("0/" + MAX_TITLE_LENGTH);
            contentCountLabel.setText("0/" + MAX_CONTENT_LENGTH);
            commentCountLabel.setText("0/" + MAX_COMMENT_LENGTH);
            
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
                                showError("Publication non trouvée");
                            }
                        }
                    } catch (NumberFormatException e) {
                        showError("Format d'ID de publication invalide: " + e.getMessage());
                    } catch (SQLException e) {
                        showError("Erreur de base de données: " + e.getMessage());
                        e.printStackTrace();
                    } catch (Exception e) {
                        showError("Erreur inattendue: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            showError("Erreur d'initialisation du forum: " + e.getMessage());
            e.printStackTrace();
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
            
            // Set post details in post details section
            postTitleLabel.setText(post.getTitle());
            postContentLabel.setText(post.getContent());
            postTagsLabel.setText("Tags: " + tagString.toString());
            
            // Load comments
            loadComments(post.getId());
            
            // Load reactions
            updateReactionCounts(post.getId());
        } catch (SQLException e) {
            showError("Error loading post details: " + e.getMessage());
            e.printStackTrace();
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
    private void handleNewPost() {
        // Clear the form for a new post
        titleTextField.clear();
        postTextField.clear();
        tagTextField.clear();
        
        // Reset selection and hide details
        forumListView.getSelectionModel().clearSelection();
        selectedPost = null;
        postDetailsBox.setVisible(false);
        
        // Focus on the title field
        titleTextField.requestFocus();
    }

    @FXML
    private void handlePost() {
        String title = titleTextField.getText().trim();
        String content = postTextField.getText().trim();
        String tags = tagTextField.getText().trim();

        // Validation du titre
        if (title.isEmpty()) {
            showError("Le titre ne peut pas être vide");
            return;
        }
        if (title.length() < 3) {
            showError("Le titre doit contenir au moins 3 caractères");
            return;
        }
        if (title.length() > 100) {
            showError("Le titre ne peut pas dépasser 100 caractères");
            return;
        }
        
        // Validation du contenu
        if (content.isEmpty()) {
            showError("Le contenu ne peut pas être vide");
            return;
        }
        if (content.length() < 10) {
            showError("Le contenu doit contenir au moins 10 caractères");
            return;
        }
        if (content.length() > 5000) {
            showError("Le contenu ne peut pas dépasser 5000 caractères");
            return;
        }
        
        // Validation des tags
        if (!tags.isEmpty()) {
            String[] tagArray = tags.split(",");
            for (String tag : tagArray) {
                String trimmedTag = tag.trim();
                if (trimmedTag.length() < 2) {
                    showError("Chaque tag doit contenir au moins 2 caractères");
                    return;
                }
                if (trimmedTag.length() > 20) {
                    showError("Les tags ne peuvent pas dépasser 20 caractères");
                    return;
                }
                if (!trimmedTag.matches("^[a-zA-Z0-9-_]+$")) {
                    showError("Les tags ne peuvent contenir que des lettres, chiffres, tirets et underscores");
                    return;
                }
            }
        }

        try {
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

            // Refresh posts and clear form
            loadPosts();
            titleTextField.clear();
            postTextField.clear();
            tagTextField.clear();
            showInfo("Publication créée avec succès");
        } catch (SQLException e) {
            showError("Erreur lors de la création de la publication: " + e.getMessage());
        }
    }

    @FXML
    private void handleComment() {
        if (selectedPost == null) {
            showError("Veuillez sélectionner une publication d'abord");
            return;
        }

        String content = commentTextArea.getText().trim();
        
        // Validation du commentaire
        if (content.isEmpty()) {
            showError("Le commentaire ne peut pas être vide");
            return;
        }
        if (content.length() < 2) {
            showError("Le commentaire doit contenir au moins 2 caractères");
            return;
        }
        if (content.length() > 1000) {
            showError("Le commentaire ne peut pas dépasser 1000 caractères");
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
            showInfo("Commentaire ajouté avec succès");
        } catch (SQLException e) {
            showError("Erreur lors de l'ajout du commentaire: " + e.getMessage());
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

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
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
    
    public void setCurrentUserId(int userId) {
        this.currentUserId = userId;
    }
} 