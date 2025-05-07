package org.example.models.forum;

import java.time.LocalDateTime;

public class Post {
    private int id;
    private int ownerId;
    private String title;
    private String content;
    private LocalDateTime createdAt;

    public Post() {}

    public Post(int id, int ownerId, String title, String content, LocalDateTime createdAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getOwnerId() { return ownerId; }
    public void setOwnerId(int ownerId) { this.ownerId = ownerId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
} 