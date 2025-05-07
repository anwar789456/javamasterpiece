package org.example.models;

/**
 * Model class for teacher applications
 */
public class TeacherApplication {
    private int id;
    private int userId;
    private String bio;
    private String skills;
    private String experience;
    private String portfolioUrl;
    private String motivation;
    private String userName; // Transient field, not stored in the database

    public TeacherApplication() {
    }

    public TeacherApplication(int id, int userId, String bio, String skills, String experience, String portfolioUrl, String motivation) {
        this.id = id;
        this.userId = userId;
        this.bio = bio;
        this.skills = skills;
        this.experience = experience;
        this.portfolioUrl = portfolioUrl;
        this.motivation = motivation;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public String getPortfolioUrl() {
        return portfolioUrl;
    }

    public void setPortfolioUrl(String portfolioUrl) {
        this.portfolioUrl = portfolioUrl;
    }

    public String getMotivation() {
        return motivation;
    }

    public void setMotivation(String motivation) {
        this.motivation = motivation;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Override
    public String toString() {
        return "TeacherApplication{" +
                "id=" + id +
                ", userId=" + userId +
                ", bio='" + bio + '\'' +
                ", skills='" + skills + '\'' +
                ", experience='" + experience + '\'' +
                ", portfolioUrl='" + portfolioUrl + '\'' +
                ", motivation='" + motivation + '\'' +
                ", userName='" + userName + '\'' +
                '}';
    }
} 