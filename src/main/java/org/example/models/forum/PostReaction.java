package org.example.models.forum;

import java.time.LocalDateTime;

public class PostReaction {
    private int id;
    private int postId;
    private int userId;
    private boolean isLike;
    private LocalDateTime createdAt;

    public PostReaction() {}

    public PostReaction(int id, int postId, int userId, boolean isLike, LocalDateTime createdAt) {
        this.id = id;
        this.postId = postId;
        this.userId = userId;
        this.isLike = isLike;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getPostId() { return postId; }
    public void setPostId(int postId) { this.postId = postId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public boolean isLike() { return isLike; }
    public void setLike(boolean like) { isLike = like; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
} 