package org.example.model;

import java.sql.Date;

public class TeamMember {
    private int id;
    private int projectId;
    private int userId;
    private String role;
    private Date joinDate;

    public TeamMember(int id, int projectId, int userId, String role, Date joinDate) {
        this.id = id;
        this.projectId = projectId;
        this.userId = userId;
        this.role = role;
        this.joinDate = joinDate;
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

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Date getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(Date joinDate) {
        this.joinDate = joinDate;
    }
}