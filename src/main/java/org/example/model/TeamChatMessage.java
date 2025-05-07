package org.example.model;

import java.sql.Timestamp;

public class TeamChatMessage {
    private int id;
    private int projectId;
    private int senderId;
    private String message;
    private Timestamp timestamp;
    private String messageType;
    private String filePath;

    public TeamChatMessage(int id, int projectId, int senderId, String message,
                           Timestamp timestamp, String messageType, String filePath) {
        this.id = id;
        this.projectId = projectId;
        this.senderId = senderId;
        this.message = message;
        this.timestamp = timestamp;
        this.messageType = messageType;
        this.filePath = filePath;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public int getSenderId() {
        return senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}