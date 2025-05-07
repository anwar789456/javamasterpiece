package org.example.controller.forum;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
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
import org.example.utils.ProfanityFilter;
import org.example.utils.EmojiSelector;
import org.example.utils.FactService;
import org.example.utils.DictionaryService;
import org.example.utils.AiTitleSuggester;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Optional;

public class UserForumController {
    @FXML
    private ListView<Integer> forumListView;
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
    @FXML
    private Button deletePostButton;
    @FXML
    private Button deleteCommentButton;
    @FXML
    private Button addCommentButton;
    @FXML
    private Button commentReplyButton;
    @FXML
    private TextField searchTextField;
    @FXML
    private Button searchButton;
    @FXML
    private Label factLabel;
    @FXML
    private Button refreshFactButton;
    @FXML
    private TextField dictionaryTextField;
    @FXML
    private Button lookupButton;
    @FXML
    private Button clearDictionaryButton;
    @FXML
    private Label dictionaryResultLabel;
    @FXML
    private Button suggestTitleButton;
    
    private PostService postService;
    private CommentService commentService;
    private TagService tagService;
    private PostReactionService reactionService;
    private UserService userService;
    private Post selectedPost;
    private int currentUserId = 1; // Temporary hardcoded user ID
    private final int MAX_TITLE_LENGTH = 100;
    private final int MAX_CONTENT_LENGTH = 5000;
    private final int MAX_COMMENT_LENGTH = 1000;
    private List<Comment> currentComments = new ArrayList<>();
    private Comment selectedComment;
    private boolean replyingToComment = false;
    private VBox inlineReplyBox;
    private int replyToCommentId = -1;
    private VBox postEmojiPane;
    private VBox commentEmojiPane;
    private VBox replyEmojiPane;
    private boolean emojiPaneVisible = false;
    private ScheduledExecutorService factScheduler;

    @FXML
    private void initialize() {
        try {
            postService = new PostService();
            commentService = new CommentService();
            tagService = new TagService();
            reactionService = new PostReactionService();
            userService = new UserService();
            
            // Initialize profanity filter with additional words if needed
            ProfanityFilter.addBadWords(new String[] {
                "damn", "hell", "crap", "shit", "fuck", "ass", "bitch"
            });
            
            // Find a valid user from the database
            initializeValidUser();
            
            // Hide the main reply button since we now use inline reply
            if (commentReplyButton != null) {
                commentReplyButton.setVisible(false);
                commentReplyButton.setManaged(false);
            }
            
            // Set up search field Enter key handler
            if (searchTextField != null) {
                searchTextField.setOnKeyPressed(event -> {
                    if (event.getCode().toString().equals("ENTER")) {
                        handleSearch();
                    }
                });
            }
            
            // Set up Did You Know section
            setupFactSection();
            
            // Set up Dictionary lookup section
            setupDictionarySection();
            
            // Set up custom cell factories
            setupForumListViewCellFactory();
            setupCommentsListViewCellFactory();
            
            // Configure les écouteurs et validations en temps réel
            setupInputValidation();
            
            // Set up emoji selectors
            setupEmojiSelectors();
            
            loadPosts();
            
            forumListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    try {
                        // Extract post ID from the custom cell content
                        int postId = (int) forumListView.getSelectionModel().getSelectedItem();
                        selectedPost = postService.read(postId);
                        if (selectedPost != null) {
                            showPostDetails(selectedPost);
                        } else {
                            showError("Publication non trouvée");
                        }
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

    private void initializeValidUser() {
        try {
            // Try to find a valid user or create one if none exists
            currentUserId = userService.getOrCreateValidUserId();
        } catch (SQLException e) {
            showError("Erreur lors de l'initialisation de l'utilisateur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupForumListViewCellFactory() {
        forumListView.setCellFactory(listView -> new ListCell<Integer>() {
            private final TranslateTransition translate = new TranslateTransition(Duration.millis(200));
            
            {
                // Set up hover effect animation
                setOnMouseEntered(e -> {
                    if (!isEmpty()) {
                        setStyle("-fx-background-color: #f5f5f5;");
                        translate.setNode(this);
                        translate.setFromX(0);
                        translate.setToX(5);
                        translate.playFromStart();
                    }
                });
                
                setOnMouseExited(e -> {
                    if (!isEmpty()) {
                        setStyle("-fx-background-color: transparent;");
                        translate.setNode(this);
                        translate.setFromX(5);
                        translate.setToX(0);
                        translate.playFromStart();
                    }
                });
            }
            
            @Override
            protected void updateItem(Integer postId, boolean empty) {
                super.updateItem(postId, empty);
                
                // Reset styles and transition state
                setStyle("-fx-background-color: transparent;");
                
                if (empty || postId == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    try {
                        Post post = postService.read(postId);
                        
                        // Create custom layout for each post cell
                        VBox container = new VBox();
                        container.setSpacing(5);
                        container.setPadding(new Insets(10));
                        container.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #e0e0e0; -fx-border-width: 1;");
                        
                        // Filter post title for profanity
                        String filteredTitle = ProfanityFilter.filter(post.getTitle());
                        
                        // Post title with styling
                        Label titleLabel = new Label(filteredTitle);
                        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
                        titleLabel.setStyle("-fx-text-fill: #303030;");
                        
                        // Post date
                        Label dateLabel = new Label(post.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
                        dateLabel.setStyle("-fx-text-fill: #757575; -fx-font-size: 12px;");
                        
                        // Tags for the post
                        HBox tagsBox = new HBox();
                        tagsBox.setSpacing(5);
                        List<Tag> tags = tagService.getTagsForPost(post.getId());
                        for (Tag tag : tags) {
                            Label tagLabel = new Label("#" + tag.getName());
                            tagLabel.setStyle("-fx-text-fill: #5e35b1; -fx-font-size: 11px; -fx-background-color: #f3e5f5; -fx-background-radius: 12; -fx-padding: 2 8;");
                            tagsBox.getChildren().add(tagLabel);
                            if (tagsBox.getChildren().size() >= 3) break; // Limit to 3 tags
                        }
                        
                        // Engagement statistics
                        HBox statsBox = new HBox();
                        statsBox.setSpacing(15);
                        statsBox.setAlignment(Pos.CENTER_LEFT);
                        
                        Label likesStats = new Label("👍 " + reactionService.getLikeCount(post.getId()));
                        likesStats.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 12px;");
                        
                        Label commentsStats = new Label("💬 " + getCommentCount(post.getId()));
                        commentsStats.setStyle("-fx-text-fill: #3949ab; -fx-font-size: 12px;");
                        
                        statsBox.getChildren().addAll(likesStats, commentsStats);
                        
                        // Add all components to the container
                        container.getChildren().addAll(titleLabel, dateLabel, tagsBox, statsBox);
                        
                        setGraphic(container);
                        setText(null);
                    } catch (SQLException e) {
                        setText("Error loading post");
                        setGraphic(null);
                    }
                }
            }
        });
    }
    
    private void setupCommentsListViewCellFactory() {
        commentsListView.setCellFactory(listView -> new ListCell<String>() {
            @Override
            protected void updateItem(String commentStr, boolean empty) {
                super.updateItem(commentStr, empty);
                
                if (empty || commentStr == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else if (commentStr.equals("No comments yet. Be the first to comment!")) {
                    // Handle the "no comments" message
                    setText(commentStr);
                    setGraphic(null);
                    setStyle("-fx-text-fill: #757575; -fx-font-style: italic;");
                } else if (commentStr.startsWith("REPLY_FORM:")) {
                    // This is our inline reply form
                    setText(null);
                    setGraphic(inlineReplyBox);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    try {
                        // Extract comment ID from string
                        // Format is: "%d - %s[%s] %s%s - %s" (id, indent, username, replyPrefix, content, datetime)
                        String[] parts = commentStr.split(" - ", 3);
                        
                        if (parts.length >= 2) {
                            String idAndIndent = parts[0];
                            int commentId = Integer.parseInt(idAndIndent.trim().split(" ")[0]);
                            Comment comment = null;
                            
                            // Find the comment in our cached list to avoid DB query
                            for (Comment c : currentComments) {
                                if (c.getId() == commentId) {
                                    comment = c;
                                    break;
                                }
                            }
                            
                            if (comment != null) {
                                // Create custom layout for each comment cell
                                VBox container = new VBox();
                                container.setSpacing(5);
                                container.setPadding(new Insets(8));
                                
                                // Determine if this is a reply (has parent ID)
                                boolean isReply = comment.getParentId() != null;
                                
                                // Apply different styling based on whether it's a reply
                                if (isReply) {
                                    container.setStyle("-fx-background-color: #f3e5f5; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-padding: 8 8 8 16;");
                                } else {
                                    container.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #e0e0e0; -fx-border-width: 1;");
                                }
                                
                                // Comment user info
                                HBox userInfoBox = new HBox();
                                userInfoBox.setAlignment(Pos.CENTER_LEFT);
                                userInfoBox.setSpacing(8);
                                
                                // Get username
                                String username;
                                try {
                                    username = userService.getUsernameById(comment.getOwnerId());
                                } catch (SQLException e) {
                                    username = "User #" + comment.getOwnerId();
                                }
                                
                                // Add reply indicator if it's a reply
                                Label replyIndicator = null;
                                if (isReply) {
                                    replyIndicator = new Label("↪");
                                    replyIndicator.setFont(Font.font("System", 14));
                                    replyIndicator.setStyle("-fx-text-fill: #5e35b1; -fx-font-weight: bold;");
                                }
                                
                                Label userLabel = new Label(username);
                                userLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
                                userLabel.setStyle("-fx-text-fill: #5e35b1;");
                                
                                Region spacer = new Region();
                                HBox.setHgrow(spacer, Priority.ALWAYS);
                                
                                Label dateLabel = new Label(comment.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, HH:mm")));
                                dateLabel.setStyle("-fx-text-fill: #757575; -fx-font-size: 11px;");
                                
                                // Add Reply link
                                final Comment finalComment = comment;
                                Button replyButton = new Button("Reply");
                                replyButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #3949ab; -fx-font-weight: bold; -fx-padding: 0; -fx-cursor: hand; -fx-font-size: 11px;");
                                replyButton.setOnAction(e -> {
                                    showInlineReplyForm(finalComment);
                                });
                                
                                if (replyIndicator != null) {
                                    userInfoBox.getChildren().addAll(replyIndicator, userLabel, spacer, dateLabel, replyButton);
                                } else {
                                    userInfoBox.getChildren().addAll(userLabel, spacer, dateLabel, replyButton);
                                }
                                
                                // Filter comment content for profanity
                                String filteredContent = ProfanityFilter.filter(comment.getContent());
                                
                                // Comment content
                                Label contentLabel = new Label(filteredContent);
                                contentLabel.setWrapText(true);
                                contentLabel.setStyle("-fx-text-fill: #424242; -fx-font-size: 13px;");
                                
                                // Add all components to the container
                                container.getChildren().addAll(userInfoBox, contentLabel);
                                
                                setGraphic(container);
                                setText(null);
                            } else {
                                setText(commentStr);
                                setGraphic(null);
                            }
                        } else {
                            setText(commentStr);
                            setGraphic(null);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        setText("Error loading comment");
                        setGraphic(null);
                    }
                }
            }
        });
    }

    private void loadPosts() {
        try {
            forumListView.getItems().clear();
            List<Post> posts = postService.readAll();
            for (Post post : posts) {
                forumListView.getItems().add(post.getId());
            }
        } catch (SQLException e) {
            showError("Error loading posts: " + e.getMessage());
        }
    }

    private void showPostDetails(Post post) {
        // Hide the details box initially for animation
        postDetailsBox.setOpacity(0);
        postDetailsBox.setScaleX(0.95);
        postDetailsBox.setScaleY(0.95);
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
            
            // Filter post content for profanity
            String filteredTitle = ProfanityFilter.filter(post.getTitle());
            String filteredContent = ProfanityFilter.filter(post.getContent());
            
            // Set post details in post details section
            postTitleLabel.setText(filteredTitle);
            postContentLabel.setText(filteredContent);
            postTagsLabel.setText("Tags: " + tagString.toString());
            
            // Check if the post belongs to the current user and update delete button visibility
            if (post.getOwnerId() == currentUserId) {
                // Post belongs to current user, allow deletion
                deletePostButton.setVisible(true);
                deletePostButton.setManaged(true);
            } else {
                // Post belongs to someone else, hide delete button
                deletePostButton.setVisible(false);
                deletePostButton.setManaged(false);
            }
            
            // Load comments
            loadComments(post.getId());
            
            // Hide the delete comment button initially as no comment is selected
            deleteCommentButton.setVisible(false);
            deleteCommentButton.setManaged(false);
            
            // Load reactions
            updateReactionCounts(post.getId());
            
            // Animate the post details box
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), postDetailsBox);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            
            ScaleTransition scaleIn = new ScaleTransition(Duration.millis(300), postDetailsBox);
            scaleIn.setFromX(0.95);
            scaleIn.setFromY(0.95);
            scaleIn.setToX(1);
            scaleIn.setToY(1);
            
            fadeIn.play();
            scaleIn.play();
            
        } catch (SQLException e) {
            showError("Error loading post details: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadComments(int postId) {
        try {
            commentsListView.getItems().clear();
            currentComments = commentService.readByPostId(postId);
            
            if (currentComments.isEmpty()) {
                commentsListView.getItems().add("No comments yet. Be the first to comment!");
                return;
            }
            
            // Group comments by their parent IDs
            Map<Integer, List<Comment>> commentsByParent = new HashMap<>();
            List<Comment> topLevelComments = new ArrayList<>();
            
            // Organize comments into parent-child groups
            for (Comment comment : currentComments) {
                if (comment.getParentId() == null) {
                    topLevelComments.add(comment);
                } else {
                    commentsByParent.computeIfAbsent(comment.getParentId(), k -> new ArrayList<>())
                                    .add(comment);
                }
            }
            
            // Add comments recursively with indentation
            addCommentsToListView(topLevelComments, commentsByParent, 0);
            
            // Configurer le gestionnaire de sélection pour les commentaires
            commentsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && !newVal.equals("No comments yet. Be the first to comment!")) {
                    // Analyser la sélection pour extraire l'ID du commentaire
                    try {
                        int index = commentsListView.getSelectionModel().getSelectedIndex();
                        // Get all comments in flattened form to match the list view
                        List<Comment> flattenedComments = new ArrayList<>();
                        Map<Integer, List<Comment>> commentMap = new HashMap<>();
                        List<Comment> rootComments = new ArrayList<>();
                        
                        // Rebuild the hierarchy for proper indexing
                        for (Comment comment : currentComments) {
                            if (comment.getParentId() == null) {
                                rootComments.add(comment);
                            } else {
                                commentMap.computeIfAbsent(comment.getParentId(), k -> new ArrayList<>())
                                          .add(comment);
                            }
                        }
                        
                        // Get comments in display order
                        flattenCommentsList(rootComments, commentMap, flattenedComments);
                        
                        if (index >= 0 && index < flattenedComments.size()) {
                            selectedComment = flattenedComments.get(index);
                            
                            // Afficher le bouton de suppression uniquement si le commentaire appartient à l'utilisateur actuel
                            if (selectedComment.getOwnerId() == currentUserId) {
                                deleteCommentButton.setVisible(true);
                                deleteCommentButton.setManaged(true);
                            } else {
                                deleteCommentButton.setVisible(false);
                                deleteCommentButton.setManaged(false);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        deleteCommentButton.setVisible(false);
                        deleteCommentButton.setManaged(false);
                    }
                } else {
                    selectedComment = null;
                    deleteCommentButton.setVisible(false);
                    deleteCommentButton.setManaged(false);
                }
            });
            
        } catch (SQLException e) {
            showError("Error loading comments: " + e.getMessage());
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
            
            // Filter comment content for profanity
            String filteredContent = ProfanityFilter.filter(comment.getContent());
            
            String formattedComment = String.format("%d - %s[%s] %s%s - %s", 
                comment.getId(),
                indent,
                userInfo,
                replyPrefix,
                filteredContent,
                comment.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                
            commentsListView.getItems().add(formattedComment);
            
            // Recursively add child comments
            List<Comment> childComments = commentsByParent.get(comment.getId());
            if (childComments != null) {
                addCommentsToListView(childComments, commentsByParent, level + 1);
            }
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
        
        // Set a fact as suggested content
        postTextField.setText(FactService.getFactSuggestion());
        postTextField.selectAll();
        
        // Reset selection and hide details
        forumListView.getSelectionModel().clearSelection();
        selectedPost = null;
        postDetailsBox.setVisible(false);
        
        // Focus on the title field
        titleTextField.requestFocus();
    }

    /**
     * Valide l'ensemble du formulaire de création de publication
     * @return true si le formulaire est valide, false sinon
     */
    private boolean validatePostForm() {
        String title = titleTextField.getText().trim();
        String content = postTextField.getText().trim();
        String tags = tagTextField.getText().trim();
        
        StringBuilder errorMessages = new StringBuilder();
        boolean isValid = true;
        
        // Validation du titre
        ValidationResult titleResult = InputValidator.validateTitle(title);
        if (!titleResult.isValid()) {
            errorMessages.append("- ").append(titleResult.getErrorMessage()).append("\n");
            isValid = false;
        }
        
        // Validation du contenu
        ValidationResult contentResult = InputValidator.validatePostContent(content);
        if (!contentResult.isValid()) {
            errorMessages.append("- ").append(contentResult.getErrorMessage()).append("\n");
            isValid = false;
        }
        
        // Validation des tags
        ValidationResult tagsResult = InputValidator.validateTags(tags);
        if (!tagsResult.isValid()) {
            errorMessages.append("- ").append(tagsResult.getErrorMessage()).append("\n");
            isValid = false;
        }
        
        if (!isValid) {
            Platform.runLater(() -> showError("Veuillez corriger les erreurs suivantes:\n" + errorMessages.toString()));
        }
        
        return isValid;
    }

    @FXML
    private void handlePost() {
        // Valider le formulaire
        if (!validatePostForm()) {
            return;
        }
        
        String title = titleTextField.getText().trim();
        String content = postTextField.getText().trim();
        String tags = tagTextField.getText().trim();
        
        // Check for profanity and warn the user if found
        if (ProfanityFilter.containsProfanity(title) || ProfanityFilter.containsProfanity(content)) {
            boolean proceed = showProfanityWarning();
            if (!proceed) {
                return;
            }
        }
        
        try {
            // Create the post (original text is stored, but displayed filtered)
            Post post = new Post();
            post.setTitle(title);
            post.setContent(content);
            post.setOwnerId(currentUserId);
            postService.create(post);
            
            // Add tags if present
            if (!tags.isEmpty()) {
                String[] tagArray = tags.split(",");
                for (String tagName : tagArray) {
                    Tag tag = new Tag();
                    tag.setName(tagName.trim());
                    tagService.create(tag);
                    tagService.addTagToPost(post.getId(), tag.getId());
                }
            }
            
            // Animation for success feedback
            Button postButton = this.postButton;
            String originalStyle = postButton.getStyle();
            
            postButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 24; -fx-padding: 12 25; -fx-cursor: hand;");
            
            // Create a scale animation
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
                
                // Clear the form
                titleTextField.clear();
                postTextField.clear();
                tagTextField.clear();
                
                // Reset form styles
                titleTextField.setStyle("");
                postTextField.setStyle("");
                tagTextField.setStyle("");
                
                // Show success message AFTER animation using Platform.runLater
                Platform.runLater(() -> {
                    showInfo("Publication créée avec succès !");
                });
                
                // Reload the posts
                loadPosts();
            });
            
            scaleUp.play();
            
        } catch (SQLException e) {
            // Check if it's a foreign key constraint error
            if (e.getMessage().contains("foreign key constraint") && e.getMessage().contains("owner_id")) {
                // Try to reinitialize valid user and retry
                try {
                    // Show warning using Platform.runLater
                    Platform.runLater(() -> {
                        showWarning("Erreur de référence utilisateur. Tentative de réinitialisation...");
                    });
                    
                    currentUserId = userService.getOrCreateValidUserId();
                    
                    // Retry post creation with new user ID
                    Post post = new Post();
                    post.setTitle(title);
                    post.setContent(content);
                    post.setOwnerId(currentUserId);
                    postService.create(post);
                    
                    // Clear the form
                    titleTextField.clear();
                    postTextField.clear();
                    tagTextField.clear();
                    
                    // Success message and refresh using Platform.runLater
                    Platform.runLater(() -> {
                        showInfo("Publication créée avec succès après réinitialisation !");
                    });
                    
                    // Reload the posts
                    loadPosts();
                } catch (SQLException retryEx) {
                    Platform.runLater(() -> {
                        showError("Échec de la réinitialisation. Erreur: " + retryEx.getMessage());
                    });
                    retryEx.printStackTrace();
                }
            } else {
                Platform.runLater(() -> {
                    showError("Erreur lors de la création de la publication: " + e.getMessage());
                });
                e.printStackTrace();
            }
        }
    }

    /**
     * Valide le formulaire de commentaire
     * @return true si le formulaire est valide, false sinon
     */
    private boolean validateCommentForm() {
        if (selectedPost == null) {
            Platform.runLater(() -> showError("Aucune publication sélectionnée"));
            return false;
        }
        
        String content = commentTextArea.getText().trim();
        ValidationResult commentResult = InputValidator.validateCommentContent(content);
        
        if (!commentResult.isValid()) {
            Platform.runLater(() -> showError(commentResult.getErrorMessage()));
            return false;
        }
        
        return true;
    }

    @FXML
    private void handleReplyComment() {
        String selected = commentsListView.getSelectionModel().getSelectedItem();
        if (selected == null || selected.equals("No comments yet. Be the first to comment!")) {
            showError("No comment selected");
            return;
        }
        
        try {
            int index = commentsListView.getSelectionModel().getSelectedIndex();
            // Get all comments in flattened form to match the list view
            List<Comment> flattenedComments = new ArrayList<>();
            Map<Integer, List<Comment>> commentMap = new HashMap<>();
            List<Comment> rootComments = new ArrayList<>();
            
            // Rebuild the hierarchy for proper indexing
            for (Comment comment : currentComments) {
                if (comment.getParentId() == null) {
                    rootComments.add(comment);
                } else {
                    commentMap.computeIfAbsent(comment.getParentId(), k -> new ArrayList<>())
                              .add(comment);
                }
            }
            
            // Get comments in display order
            flattenCommentsList(rootComments, commentMap, flattenedComments);
            
            if (index >= 0 && index < flattenedComments.size()) {
                selectedComment = flattenedComments.get(index);
                
                // Set up for reply mode
                replyingToComment = true;
                
                // Clear textarea and focus it
                commentTextArea.clear();
                commentTextArea.requestFocus();
                
                // Update the button to show reply mode
                if (addCommentButton != null) {
                    addCommentButton.setText("Post Reply");
                    addCommentButton.setStyle("-fx-background-color: #3949ab; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 24; -fx-padding: 8 15; -fx-cursor: hand;");
                }
                
                // Set prompt text to indicate reply mode
                commentTextArea.setPromptText("Reply to " + getUserDisplayName(selectedComment.getOwnerId()) + "...");
                
                // Show username of comment author being replied to
                String userInfo = getUserDisplayName(selectedComment.getOwnerId());
                showInfo("Replying to " + userInfo + ". Write your reply and press 'Post Reply'.");
            }
        } catch (Exception e) {
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

    @FXML
    private void handleComment() {
        // Valider le formulaire de commentaire
        if (!validateCommentForm()) {
            return;
        }
        
        String content = commentTextArea.getText().trim();
        
        // Check for profanity and warn the user if found
        if (ProfanityFilter.containsProfanity(content)) {
            boolean proceed = showProfanityWarning();
            if (!proceed) {
                return;
            }
        }
        
        try {
            Comment comment = new Comment();
            comment.setContent(content);
            comment.setPostId(selectedPost.getId());
            comment.setOwnerId(currentUserId);
            
            // Set parentId if replying to a comment
            if (replyingToComment && selectedComment != null) {
                comment.setParentId(selectedComment.getId());
            }
            
            commentService.create(comment);
            
            // Clear the comment text field and reset state
            commentTextArea.clear();
            commentTextArea.setStyle("");
            commentTextArea.setPromptText("Join the conversation...");
            replyingToComment = false;
            
            // Reset button style
            if (addCommentButton != null) {
                addCommentButton.setText("Post Comment");
                addCommentButton.setStyle("-fx-background-color: #5e35b1; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 24; -fx-padding: 8 15; -fx-cursor: hand;");
            }
            
            // Show success message using Platform.runLater
            Platform.runLater(() -> {
                String message = replyingToComment ? "Reply added successfully!" : "Comment added successfully!";
                showInfo(message);
            });
            
            // Reset state
            selectedComment = null;
            replyingToComment = false;
            
            // Reload comments with animation
            loadCommentsWithAnimation(selectedPost.getId());
            
            // Update the reaction counts
            updateReactionCounts(selectedPost.getId());
        } catch (SQLException e) {
            // Check if it's a foreign key constraint error
            if (e.getMessage().contains("foreign key constraint") && (e.getMessage().contains("owner_id") || e.getMessage().contains("user"))) {
                // Try to reinitialize valid user and retry
                try {
                    showWarning("Erreur de référence utilisateur. Tentative de réinitialisation...");
                    currentUserId = userService.getOrCreateValidUserId();
                    
                    // Retry comment creation with new user ID
                    Comment comment = new Comment();
                    comment.setContent(content);
                    comment.setPostId(selectedPost.getId());
                    comment.setOwnerId(currentUserId);
                    
                    // Set parentId if replying to a comment
                    if (replyingToComment && selectedComment != null) {
                        comment.setParentId(selectedComment.getId());
                    }
                    
                    commentService.create(comment);
                    
                    // Clear the comment text field
                    commentTextArea.clear();
                    commentTextArea.setPromptText("Join the conversation...");
                    replyingToComment = false;
                    
                    // Reset button style
                    if (addCommentButton != null) {
                        addCommentButton.setText("Post Comment");
                        addCommentButton.setStyle("-fx-background-color: #5e35b1; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 24; -fx-padding: 8 15; -fx-cursor: hand;");
                    }
                    
                    // Show messages using Platform.runLater
                    Platform.runLater(() -> {
                        showWarning("Erreur de référence utilisateur. Tentative de réinitialisation...");
                    });
                    
                    // Show success message using Platform.runLater
                    Platform.runLater(() -> {
                        String message = replyingToComment ? "Reply added successfully!" : "Comment added successfully!";
                        showInfo(message);
                    });
                    
                    // Reset state
                    selectedComment = null;
                    replyingToComment = false;
                    
                    // Reload comments with animation
                    loadCommentsWithAnimation(selectedPost.getId());
                    
                    // Update the reaction counts
                    updateReactionCounts(selectedPost.getId());
                } catch (SQLException retryEx) {
                    Platform.runLater(() -> {
                        showError("Échec de la réinitialisation. Erreur: " + retryEx.getMessage());
                    });
                    retryEx.printStackTrace();
                }
            } else {
                Platform.runLater(() -> {
                    showError("Erreur lors de l'ajout du commentaire: " + e.getMessage());
                });
                e.printStackTrace();
            }
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
                    // Try to reinitialize valid user and retry
                    currentUserId = userService.getOrCreateValidUserId();
                    reactionService.toggleReaction(selectedPost.getId(), currentUserId, true);
                    updateReactionCounts(selectedPost.getId());
                } catch (SQLException retryEx) {
                    Platform.runLater(() -> {
                        showError("Erreur lors de la réaction: " + retryEx.getMessage());
                    });
                }
            } else {
                Platform.runLater(() -> {
                    showError("Erreur lors de la réaction: " + e.getMessage());
                });
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
                    // Try to reinitialize valid user and retry
                    currentUserId = userService.getOrCreateValidUserId();
                    reactionService.toggleReaction(selectedPost.getId(), currentUserId, false);
                    updateReactionCounts(selectedPost.getId());
                } catch (SQLException retryEx) {
                    Platform.runLater(() -> {
                        showError("Erreur lors de la réaction: " + retryEx.getMessage());
                    });
                }
            } else {
                Platform.runLater(() -> {
                    showError("Erreur lors de la réaction: " + e.getMessage());
                });
            }
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
    
    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Avertissement");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public void setCurrentUserId(int userId) {
        this.currentUserId = userId;
    }

    // Helper method to get comment count for a post
    private int getCommentCount(int postId) throws SQLException {
        List<Comment> comments = commentService.readByPostId(postId);
        return comments.size();
    }

    private void loadCommentsWithAnimation(int postId) throws SQLException {
        List<Comment> comments = commentService.readByPostId(postId);
        currentComments = comments;
        
        commentsListView.getItems().clear();
        
        if (comments.isEmpty()) {
            commentsListView.getItems().add("No comments yet. Be the first to comment!");
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
        
        // Apply a fade transition to the comments list
        FadeTransition fade = new FadeTransition(Duration.millis(300), commentsListView);
        fade.setFromValue(0.5);
        fade.setToValue(1.0);
        fade.play();
        
        // Scroll to the last comment
        if (!comments.isEmpty()) {
            commentsListView.scrollTo(commentsListView.getItems().size() - 1);
        }
    }

    /**
     * Configure la validation en temps réel des champs de saisie
     */
    private void setupInputValidation() {
        // Validation du titre en temps réel
        titleTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            int length = newValue.length();
            titleCountLabel.setText(length + "/" + MAX_TITLE_LENGTH);
            
            if (length > MAX_TITLE_LENGTH) {
                titleTextField.setText(oldValue);
                titleCountLabel.setText(oldValue.length() + "/" + MAX_TITLE_LENGTH);
                titleTextField.setStyle("-fx-border-color: #ff5555; -fx-background-color: #fff0f0;");
            } else if (length == 0) {
                // Champ vide
                titleCountLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #757575;");
                titleTextField.setStyle("");
            } else if (length < 5) {
                // Trop court
                titleCountLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #ff5555;");
                titleTextField.setStyle("-fx-border-color: #ffaa55; -fx-background-color: #fffaf0;");
            } else if (length > MAX_TITLE_LENGTH * 0.9) {
                // Proche de la limite
                titleCountLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #ff5555;");
                titleTextField.setStyle("-fx-border-color: #ffaa55;");
            } else {
                // Valide
                titleCountLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #757575;");
                titleTextField.setStyle("-fx-border-color: #4CAF50; -fx-background-color: #f0fff0;");
            }
            
            // Vérification du format
            if (length > 0 && !newValue.matches("^[a-zA-Z0-9\\sÀ-ÿ\\.,\\?!;:'\"-_\\(\\)]+$")) {
                titleTextField.setStyle("-fx-border-color: #ff5555; -fx-background-color: #fff0f0;");
            }
        });
        
        // Validation du contenu en temps réel
        postTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            int length = newValue.length();
            contentCountLabel.setText(length + "/" + MAX_CONTENT_LENGTH);
            
            if (length > MAX_CONTENT_LENGTH) {
                postTextField.setText(oldValue);
                contentCountLabel.setText(oldValue.length() + "/" + MAX_CONTENT_LENGTH);
                postTextField.setStyle("-fx-border-color: #ff5555; -fx-background-color: #fff0f0;");
            } else if (length == 0) {
                // Champ vide
                contentCountLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #757575;");
                postTextField.setStyle("");
            } else if (length < 10) {
                // Trop court
                contentCountLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #ff5555;");
                postTextField.setStyle("-fx-border-color: #ffaa55; -fx-background-color: #fffaf0;");
            } else if (length > MAX_CONTENT_LENGTH * 0.9) {
                // Proche de la limite
                contentCountLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #ff5555;");
                postTextField.setStyle("-fx-border-color: #ffaa55;");
            } else {
                // Valide
                contentCountLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #757575;");
                postTextField.setStyle("-fx-border-color: #4CAF50; -fx-background-color: #f0fff0;");
            }
        });
        
        // Validation des tags en temps réel
        tagTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.isEmpty()) {
                tagTextField.setStyle("");
                return;
            }
            
            ValidationResult result = InputValidator.validateTags(newValue);
            if (result.isValid()) {
                tagTextField.setStyle("-fx-border-color: #4CAF50; -fx-background-color: #f0fff0;");
            } else {
                tagTextField.setStyle("-fx-border-color: #ffaa55; -fx-background-color: #fffaf0;");
            }
        });
        
        // Validation du commentaire en temps réel
        commentTextArea.textProperty().addListener((observable, oldValue, newValue) -> {
            int length = newValue.length();
            commentCountLabel.setText(length + "/" + MAX_COMMENT_LENGTH);
            
            if (length > MAX_COMMENT_LENGTH) {
                commentTextArea.setText(oldValue);
                commentCountLabel.setText(oldValue.length() + "/" + MAX_COMMENT_LENGTH);
                commentTextArea.setStyle("-fx-border-color: #ff5555; -fx-background-color: #fff0f0;");
            } else if (length == 0) {
                // Champ vide
                commentCountLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #757575;");
                commentTextArea.setStyle("");
            } else if (length < 2) {
                // Trop court
                commentCountLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #ff5555;");
                commentTextArea.setStyle("-fx-border-color: #ffaa55; -fx-background-color: #fffaf0;");
            } else if (length > MAX_COMMENT_LENGTH * 0.9) {
                // Proche de la limite
                commentCountLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #ff5555;");
                commentTextArea.setStyle("-fx-border-color: #ffaa55;");
            } else {
                // Valide
                commentCountLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #757575;");
                commentTextArea.setStyle("-fx-border-color: #4CAF50; -fx-background-color: #f0fff0;");
            }
            
            // Vérification du format
            if (length > 0 && !newValue.matches("^[a-zA-Z0-9\\sÀ-ÿ\\.,\\?!;:'\"-_\\(\\)\\[\\]]+$")) {
                commentTextArea.setStyle("-fx-border-color: #ff5555; -fx-background-color: #fff0f0;");
            }
        });
        
        // Initialisation des compteurs
        titleCountLabel.setText("0/" + MAX_TITLE_LENGTH);
        contentCountLabel.setText("0/" + MAX_CONTENT_LENGTH);
        commentCountLabel.setText("0/" + MAX_COMMENT_LENGTH);
        
        // Add event listener for AI title suggestions
        if (postTextField != null && titleTextField != null && suggestTitleButton != null) {
            suggestTitleButton.setOnAction(event -> handleSuggestTitle());
            
            // Enable the suggest title button only when there's content
            postTextField.textProperty().addListener((observable, oldValue, newValue) -> {
                suggestTitleButton.setDisable(newValue == null || newValue.trim().isEmpty());
            });
        }
    }

    /**
     * Handle the AI title suggestion button click
     */
    private void handleSuggestTitle() {
        String content = postTextField.getText();
        String currentTitle = titleTextField.getText();
        String tags = tagTextField.getText();
        
        if (content == null || content.trim().isEmpty()) {
            showWarning("Please enter some post content first to generate title suggestions.");
            return;
        }
        
        // Disable the button and show loading state
        suggestTitleButton.setDisable(true);
        suggestTitleButton.setText("Generating...");
        
        // Process in a background thread to avoid freezing the UI
        new Thread(() -> {
            try {
                // Get title suggestions from the AI service with the complete content
                final List<String> suggestions = AiTitleSuggester.suggestTitles(content, currentTitle, tags);
                
                // Update UI on the JavaFX thread
                Platform.runLater(() -> {
                    // Restore button state
                    suggestTitleButton.setDisable(false);
                    suggestTitleButton.setText("Suggest Title");
                    
                    if (suggestions.isEmpty()) {
                        showInfo("No title suggestions could be generated. Try adding more content to your post.");
                        return;
                    }
                    
                    // Create a dialog to display the suggestions
                    Dialog<String> dialog = new Dialog<>();
                    dialog.setTitle("AI Title Suggestions");
                    dialog.setHeaderText("Select a suggested title");
                    
                    // Set the button types
                    ButtonType selectButtonType = new ButtonType("Select", ButtonBar.ButtonData.OK_DONE);
                    dialog.getDialogPane().getButtonTypes().addAll(selectButtonType, ButtonType.CANCEL);
                    
                    // Create the suggestion list
                    VBox vbox = new VBox(10);
                    vbox.setPadding(new Insets(20, 10, 10, 10));
                    
                    ToggleGroup group = new ToggleGroup();
                    
                    for (String suggestion : suggestions) {
                        RadioButton radioButton = new RadioButton(suggestion);
                        radioButton.setToggleGroup(group);
                        radioButton.setWrapText(true);
                        radioButton.setMaxWidth(Double.MAX_VALUE);
                        vbox.getChildren().add(radioButton);
                        
                        // Make first option selected by default
                        if (suggestions.indexOf(suggestion) == 0) {
                            radioButton.setSelected(true);
                        }
                    }
                    
                    // Create a styled dialog pane
                    dialog.getDialogPane().setContent(vbox);
                    dialog.getDialogPane().setPrefWidth(500);
                    dialog.getDialogPane().getStyleClass().add("ai-suggestion-dialog");
                    
                    // Add CSS styling
                    dialog.getDialogPane().getStylesheets().add(getClass().getResource("/Forum/forum_styles.css").toExternalForm());
                    
                    // Add a cool AI icon
                    try {
                        dialog.setGraphic(new Label("🤖"));
                    } catch (Exception e) {
                        // Fallback if emoji doesn't work
                    }
                    
                    // Request focus on the select button by default
                    Platform.runLater(() -> {
                        Button selectButton = (Button) dialog.getDialogPane().lookupButton(selectButtonType);
                        selectButton.setDefaultButton(true);
                    });
                    
                    // Convert the result
                    dialog.setResultConverter(dialogButton -> {
                        if (dialogButton == selectButtonType) {
                            RadioButton selectedRadioButton = (RadioButton) group.getSelectedToggle();
                            return selectedRadioButton == null ? null : selectedRadioButton.getText();
                        }
                        return null;
                    });
                    
                    // Add animation to the dialog
                    dialog.setOnShowing(dialogEvent -> {
                        // Apply fade-in effect to the suggestions list
                        FadeTransition fadeIn = new FadeTransition(Duration.millis(400), vbox);
                        fadeIn.setFromValue(0.0);
                        fadeIn.setToValue(1.0);
                        fadeIn.play();
                    });
                    
                    // Show the dialog and process the result
                    Optional<String> result = dialog.showAndWait();
                    
                    result.ifPresent(selectedTitle -> {
                        // Update the title field with the selected suggestion
                        titleTextField.setText(selectedTitle);
                        
                        // Show success notification
                        showSuccess("Title updated with AI suggestion!");
                    });
                });
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    // Restore button state
                    suggestTitleButton.setDisable(false);
                    suggestTitleButton.setText("Suggest Title");
                    
                    showError("Error generating title suggestions: " + e.getMessage());
                    e.printStackTrace();
                });
            }
        }).start();
    }

    /**
     * Shows a success alert
     */
    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        // Style the alert
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStyleClass().add("success-alert");
        
        // Add a brief auto-close timer (3 seconds)
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                Platform.runLater(alert::close);
            } catch (Exception e) {
                // Ignore
            }
        }).start();
        
        alert.show();
    }

    /**
     * Gérer la suppression d'un post par l'utilisateur courant
     */
    @FXML
    private void handleDeletePost() {
        if (selectedPost == null) {
            showError("No post selected");
            return;
        }
        
        // Vérifier que le post appartient à l'utilisateur actuel
        if (selectedPost.getOwnerId() != currentUserId) {
            showError("You can only delete your own posts");
            return;
        }
        
        // Confirmer la suppression
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Post");
        alert.setHeaderText("Delete Post");
        alert.setContentText("Are you sure you want to delete this post? This action cannot be undone.");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    int postId = selectedPost.getId();
                    System.out.println("Deleting post ID: " + postId);
                    
                    // 1. Supprimer les réactions
                    try {
                        reactionService.deleteAllForPost(postId);
                    } catch (SQLException e) {
                        System.out.println("Error deleting reactions: " + e.getMessage());
                    }
                    
                    // 2. Supprimer les associations de tags
                    try {
                        tagService.removeAllTagsFromPost(postId);
                    } catch (SQLException e) {
                        System.out.println("Error removing tag associations: " + e.getMessage());
                    }
                    
                    // 3. Supprimer les commentaires
                    try {
                        List<Comment> comments = commentService.readByPostId(postId);
                        for (Comment comment : comments) {
                            commentService.delete(comment.getId());
                        }
                    } catch (SQLException e) {
                        System.out.println("Error deleting comments: " + e.getMessage());
                    }
                    
                    // 4. Supprimer le post
                    postService.delete(postId);
                    
                    // Mettre à jour l'interface
                    loadPosts();
                    
                    // Masquer les détails du post
                    postDetailsBox.setVisible(false);
                    selectedPost = null;
                    
                    showInfo("Post deleted successfully");
                } catch (SQLException e) {
                    showError("Error deleting post: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }
    
    /**
     * Gérer la suppression d'un commentaire par l'utilisateur courant
     */
    @FXML
    private void handleDeleteComment() {
        if (selectedComment == null) {
            showError("No comment selected");
            return;
        }
        
        // Vérifier que le commentaire appartient à l'utilisateur actuel
        if (selectedComment.getOwnerId() != currentUserId) {
            showError("You can only delete your own comments");
            return;
        }
        
        // Confirmer la suppression
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Comment");
        alert.setHeaderText("Delete Comment");
        alert.setContentText("Are you sure you want to delete this comment? This action cannot be undone.");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    // Supprimer le commentaire
                    commentService.delete(selectedComment.getId());
                    
                    // Recharger les commentaires
                    loadComments(selectedPost.getId());
                    
                    // Réinitialiser la sélection
                    selectedComment = null;
                    
                    // Masquer le bouton de suppression
                    deleteCommentButton.setVisible(false);
                    deleteCommentButton.setManaged(false);
                    
                    showInfo("Comment deleted successfully");
                } catch (SQLException e) {
                    showError("Error deleting comment: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    private void showInlineReplyForm(Comment parentComment) {
        if (inlineReplyBox == null) {
            createInlineReplyBox();
        }
        
        // Set the parent comment ID
        replyToCommentId = parentComment.getId();
        
        // Update prompt text with parent username
        String parentUsername = getUserDisplayName(parentComment.getOwnerId());
        TextArea replyTextArea = (TextArea) inlineReplyBox.lookup("#inlineReplyTextArea");
        if (replyTextArea != null) {
            replyTextArea.setPromptText("Reply to " + parentUsername + "...");
            replyTextArea.clear();
            replyTextArea.requestFocus();
        }
        
        // Find the index of the comment in the list
        int commentIndex = -1;
        for (int i = 0; i < commentsListView.getItems().size(); i++) {
            String item = commentsListView.getItems().get(i);
            if (!item.startsWith("REPLY_FORM:") && !item.equals("No comments yet. Be the first to comment!")) {
                String[] parts = item.split(" - ", 3);
                if (parts.length >= 2) {
                    try {
                        int id = Integer.parseInt(parts[0].trim().split(" ")[0]);
                        if (id == parentComment.getId()) {
                            commentIndex = i;
                            break;
                        }
                    } catch (NumberFormatException e) {
                        // Skip this item
                    }
                }
            }
        }
        
        // If we found the comment, add the reply form after it
        if (commentIndex >= 0) {
            // First, remove any existing reply form
            for (int i = commentsListView.getItems().size() - 1; i >= 0; i--) {
                if (commentsListView.getItems().get(i).startsWith("REPLY_FORM:")) {
                    commentsListView.getItems().remove(i);
                }
            }
            
            // Add the reply form below the selected comment
            commentsListView.getItems().add(commentIndex + 1, "REPLY_FORM:" + parentComment.getId());
            
            // Scroll to make it visible
            commentsListView.scrollTo(commentIndex + 1);
        }
    }
    
    private void createInlineReplyBox() {
        inlineReplyBox = new VBox();
        inlineReplyBox.setSpacing(10);
        inlineReplyBox.setPadding(new Insets(5, 5, 5, 20)); // More left padding for indentation
        inlineReplyBox.setStyle("-fx-background-color: #f1f5ff; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #d0d9ff; -fx-border-width: 1;");
        
        // Title
        Label titleLabel = new Label("Write your reply");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #3949ab; -fx-font-size: 12px;");
        
        // Reply text area
        TextArea replyTextArea = new TextArea();
        replyTextArea.setId("inlineReplyTextArea");
        replyTextArea.setPrefRowCount(3);
        replyTextArea.setPrefHeight(60);
        replyTextArea.setWrapText(true);
        replyTextArea.setPromptText("Write your reply here...");
        replyTextArea.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #d0d9ff;");
        
        // Emoji button for reply
        Button emojiButton = new Button("😀");
        emojiButton.setStyle("-fx-background-color: #3949ab; -fx-text-fill: white; -fx-font-size: 12px; -fx-background-radius: 20; -fx-padding: 2 8; -fx-cursor: hand;");
        emojiButton.setTooltip(new Tooltip("Insert emoji"));
        
        // Create emoji pane for reply
        replyEmojiPane = new VBox();
        replyEmojiPane.setVisible(false);
        replyEmojiPane.setManaged(false);
        
        emojiButton.setOnAction(e -> toggleEmojiPane(replyTextArea, replyEmojiPane, emojiButton));
        
        // Buttons
        HBox buttonBox = new HBox();
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setSpacing(10);
        
        Button cancelButton = new Button("Cancel");
        cancelButton.setStyle("-fx-background-color: #e0e0e0; -fx-text-fill: #424242; -fx-background-radius: 16; -fx-padding: 5 15;");
        cancelButton.setOnAction(e -> {
            // Remove the reply form from the list
            for (int i = commentsListView.getItems().size() - 1; i >= 0; i--) {
                if (commentsListView.getItems().get(i).startsWith("REPLY_FORM:")) {
                    commentsListView.getItems().remove(i);
                    break;
                }
            }
            replyToCommentId = -1;
        });
        
        Button submitButton = new Button("Post Reply");
        submitButton.setStyle("-fx-background-color: #3949ab; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 16; -fx-padding: 5 15;");
        submitButton.setOnAction(e -> {
            handleInlineReply(replyTextArea.getText());
        });
        
        // Add character counter
        Label charCountLabel = new Label("0/1000");
        charCountLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #757575;");
        
        // Update character count when text changes
        replyTextArea.textProperty().addListener((obs, oldVal, newVal) -> {
            int length = newVal.length();
            charCountLabel.setText(length + "/1000");
            
            if (length > MAX_COMMENT_LENGTH) {
                replyTextArea.setText(oldVal);
                charCountLabel.setText(oldVal.length() + "/1000");
                replyTextArea.setStyle("-fx-border-color: #ff5555; -fx-background-color: #fff0f0;");
            } else if (length > 0) {
                replyTextArea.setStyle("-fx-border-color: #4CAF50; -fx-background-color: #f0fff0;");
            }
        });
        
        buttonBox.getChildren().addAll(emojiButton, charCountLabel, cancelButton, submitButton);
        
        inlineReplyBox.getChildren().addAll(titleLabel, replyTextArea, buttonBox, replyEmojiPane);
    }
    
    private void handleInlineReply(String content) {
        if (content == null || content.trim().isEmpty()) {
            showWarning("Reply cannot be empty");
            return;
        }
        
        if (replyToCommentId < 0) {
            showWarning("Invalid comment to reply to");
            return;
        }
        
        if (content.length() > MAX_COMMENT_LENGTH) {
            showWarning("Reply is too long (maximum " + MAX_COMMENT_LENGTH + " characters)");
            return;
        }
        
        // Check for profanity and warn the user if found
        if (ProfanityFilter.containsProfanity(content)) {
            boolean proceed = showProfanityWarning();
            if (!proceed) {
                return;
            }
        }
        
        try {
            // Create the reply comment
            Comment reply = new Comment();
            reply.setContent(content.trim());
            reply.setPostId(selectedPost.getId());
            reply.setOwnerId(currentUserId);
            reply.setParentId(replyToCommentId);
            
            commentService.create(reply);
            
            // Remove the reply form
            for (int i = commentsListView.getItems().size() - 1; i >= 0; i--) {
                if (commentsListView.getItems().get(i).startsWith("REPLY_FORM:")) {
                    commentsListView.getItems().remove(i);
                    break;
                }
            }
            
            // Reset state
            replyToCommentId = -1;
            
            // Reload comments
            loadCommentsWithAnimation(selectedPost.getId());
            
            // Show success message
            showInfo("Reply added successfully!");
            
        } catch (SQLException e) {
            showError("Error adding reply: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSearch() {
        String searchTerm = searchTextField.getText().trim().toLowerCase();
        
        if (searchTerm.isEmpty()) {
            // If search is empty, reload all posts
            loadPosts();
            return;
        }
        
        try {
            // Get all posts and filter them
            List<Post> allPosts = postService.readAll();
            List<Integer> filteredPostIds = new ArrayList<>();
            
            for (Post post : allPosts) {
                boolean matchesTitle = post.getTitle().toLowerCase().contains(searchTerm);
                boolean matchesContent = post.getContent().toLowerCase().contains(searchTerm);
                
                // Check tags
                boolean matchesTags = false;
                try {
                    List<Tag> tags = tagService.getTagsForPost(post.getId());
                    for (Tag tag : tags) {
                        if (tag.getName().toLowerCase().contains(searchTerm)) {
                            matchesTags = true;
                            break;
                        }
                    }
                } catch (SQLException e) {
                    System.out.println("Error fetching tags for post " + post.getId() + ": " + e.getMessage());
                }
                
                if (matchesTitle || matchesContent || matchesTags) {
                    filteredPostIds.add(post.getId());
                }
            }
            
            // Update the UI with filtered posts
            forumListView.getItems().clear();
            if (filteredPostIds.isEmpty()) {
                // No results found - show message
                showInfo("No matching posts found for: " + searchTerm);
                // Optionally add a placeholder in the list
                loadPosts(); // Reload all posts
            } else {
                for (Integer postId : filteredPostIds) {
                    forumListView.getItems().add(postId);
                }
                // Highlight first result
                if (!forumListView.getItems().isEmpty()) {
                    forumListView.getSelectionModel().select(0);
                    forumListView.scrollTo(0);
                }
            }
        } catch (SQLException e) {
            showError("Error searching posts: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Show a warning about profanity and ask the user if they want to proceed
     * @return true if the user wants to proceed, false if they want to edit their content
     */
    private boolean showProfanityWarning() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Profanity Detected");
        alert.setHeaderText("Your post contains inappropriate language");
        alert.setContentText("We've detected potentially inappropriate language in your content. " +
                "This will be displayed with asterisks (*). Do you want to proceed or edit your content?");
        
        ButtonType proceedButton = new ButtonType("Proceed");
        ButtonType editButton = new ButtonType("Edit Content");
        
        alert.getButtonTypes().setAll(proceedButton, editButton);
        
        ButtonType result = alert.showAndWait().orElse(editButton);
        return result == proceedButton;
    }

    /**
     * Set up emoji selectors for text areas
     */
    private void setupEmojiSelectors() {
        // Create emoji selector for post text area
        if (postTextField != null) {
            postEmojiPane = new VBox();
            postEmojiPane.setVisible(false);
            postEmojiPane.setManaged(false);
            
            Button emojiButton = new Button("😀");
            emojiButton.setStyle("-fx-background-color: #5e35b1; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 20; -fx-padding: 4 10; -fx-cursor: hand;");
            emojiButton.setTooltip(new Tooltip("Insert emoji"));
            
            emojiButton.setOnAction(e -> toggleEmojiPane(postTextField, postEmojiPane, emojiButton));
            
            // Find the parent of the post text area to add the emoji button and panel
            if (postTextField.getParent() instanceof VBox) {
                VBox parent = (VBox) postTextField.getParent();
                int index = parent.getChildren().indexOf(postTextField);
                
                // Create HBox to contain the emoji button
                HBox buttonBox = new HBox();
                buttonBox.setAlignment(Pos.CENTER_RIGHT);
                buttonBox.getChildren().add(emojiButton);
                
                // Add the emoji button below the text area
                parent.getChildren().add(index + 1, buttonBox);
                
                // Add the emoji panel (initially hidden)
                parent.getChildren().add(index + 2, postEmojiPane);
            }
        }
        
        // Create emoji selector for comment text area
        if (commentTextArea != null) {
            commentEmojiPane = new VBox();
            commentEmojiPane.setVisible(false);
            commentEmojiPane.setManaged(false);
            
            Button emojiButton = new Button("😀");
            emojiButton.setStyle("-fx-background-color: #5e35b1; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 20; -fx-padding: 4 10; -fx-cursor: hand;");
            emojiButton.setTooltip(new Tooltip("Insert emoji"));
            
            emojiButton.setOnAction(e -> toggleEmojiPane(commentTextArea, commentEmojiPane, emojiButton));
            
            // Find the parent of the comment text area
            if (commentTextArea.getParent() instanceof VBox) {
                VBox parent = (VBox) commentTextArea.getParent();
                int index = parent.getChildren().indexOf(commentTextArea);
                
                // Create HBox to contain the emoji button
                HBox buttonBox = new HBox();
                buttonBox.setAlignment(Pos.CENTER_RIGHT);
                buttonBox.getChildren().add(emojiButton);
                
                // Add the emoji button below the text area
                parent.getChildren().add(index + 1, buttonBox);
                
                // Add the emoji panel (initially hidden)
                parent.getChildren().add(index + 2, commentEmojiPane);
            }
        }
    }

    /**
     * Toggle the visibility of an emoji pane
     */
    private void toggleEmojiPane(TextInputControl textInput, VBox emojiPane, Button toggleButton) {
        if (emojiPane.isVisible()) {
            // Hide emoji pane
            emojiPane.setVisible(false);
            emojiPane.setManaged(false);
            toggleButton.setText("😀");
        } else {
            // Show emoji pane and create its content if not created yet
            if (emojiPane.getChildren().isEmpty()) {
                emojiPane.getChildren().add(EmojiSelector.createEmojiSelector(textInput));
            }
            
            emojiPane.setVisible(true);
            emojiPane.setManaged(true);
            toggleButton.setText("❌");
            
            // To prevent multiple emoji panes being open, close others
            if (emojiPane != postEmojiPane && postEmojiPane != null) {
                postEmojiPane.setVisible(false);
                postEmojiPane.setManaged(false);
            }
            if (emojiPane != commentEmojiPane && commentEmojiPane != null) {
                commentEmojiPane.setVisible(false);
                commentEmojiPane.setManaged(false);
            }
            if (emojiPane != replyEmojiPane && replyEmojiPane != null) {
                replyEmojiPane.setVisible(false);
                replyEmojiPane.setManaged(false);
            }
        }
    }

    /**
     * Set up the "Did You Know?" fact section
     */
    private void setupFactSection() {
        if (factLabel != null) {
            // Set initial loading message
            factLabel.setText("Loading an interesting fact...");
            
            // Set up refresh button if available
            if (refreshFactButton != null) {
                refreshFactButton.setOnAction(e -> refreshFact());
            }
            
            // Load first fact
            refreshFact();
            
            // Schedule fact updates every 2 minutes
            factScheduler = Executors.newSingleThreadScheduledExecutor();
            factScheduler.scheduleAtFixedRate(
                () -> Platform.runLater(this::refreshFact),
                2, 2, TimeUnit.MINUTES
            );
        }
    }

    /**
     * Refresh the displayed fact
     */
    private void refreshFact() {
        if (factLabel != null) {
            // Show loading indicator
            factLabel.setText("Loading fact...");
            
            // Get fact asynchronously
            FactService.getRandomFactAsync().thenAccept(fact -> {
                Platform.runLater(() -> {
                    // Apply fade transition
                    FadeTransition fade = new FadeTransition(Duration.millis(500), factLabel);
                    fade.setFromValue(0.3);
                    fade.setToValue(1.0);
                    
                    factLabel.setText(fact);
                    fade.play();
                });
            });
        }
    }

    /**
     * Set up the dictionary lookup section
     */
    private void setupDictionarySection() {
        if (dictionaryTextField != null) {
            // Set up Enter key handler for dictionary lookup
            dictionaryTextField.setOnKeyPressed(event -> {
                if (event.getCode().toString().equals("ENTER")) {
                    handleDictionaryLookup();
                }
            });
            
            // Set up clear button
            if (clearDictionaryButton != null) {
                clearDictionaryButton.setOnAction(e -> {
                    dictionaryTextField.clear();
                    dictionaryResultLabel.setText("Enter a word to see its definition");
                    
                    // Apply fade transition for smooth effect
                    FadeTransition fade = new FadeTransition(Duration.millis(300), dictionaryResultLabel);
                    fade.setFromValue(0.3);
                    fade.setToValue(1.0);
                    fade.play();
                });
            }
        }
    }
    
    /**
     * Handle the dictionary lookup request
     */
    @FXML
    private void handleDictionaryLookup() {
        String word = dictionaryTextField.getText().trim();
        if (word.isEmpty()) {
            showError("Please enter a word to look up");
            return;
        }
        
        // Show loading state
        dictionaryResultLabel.setText("Looking up '" + word + "'...");
        
        // Fetch definition asynchronously
        DictionaryService.getDefinitionAsync(word).thenAccept(definition -> {
            Platform.runLater(() -> {
                // Apply fade transition for smooth effect
                FadeTransition fade = new FadeTransition(Duration.millis(500), dictionaryResultLabel);
                fade.setFromValue(0.3);
                fade.setToValue(1.0);
                
                dictionaryResultLabel.setText(definition);
                fade.play();
            });
        });
    }
} 