package org.example.models.forum;

import java.time.LocalDateTime;

public class Comment {
    private int id;
    private int postId;
    private int ownerId;
    private Integer parentId;
    private String content;
    private LocalDateTime createdAt;

    public Comment() {}

    public Comment(int id, int postId, int ownerId, Integer parentId, String content, LocalDateTime createdAt) {
        this.id = id;
        this.postId = postId;
        this.ownerId = ownerId;
        this.parentId = parentId;
        this.content = content;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getPostId() { return postId; }
    public void setPostId(int postId) { this.postId = postId; }
    public int getOwnerId() { return ownerId; }
    public void setOwnerId(int ownerId) { this.ownerId = ownerId; }
    public Integer getParentId() { return parentId; }
    public void setParentId(Integer parentId) { this.parentId = parentId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
} 