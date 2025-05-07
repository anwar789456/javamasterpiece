package org.example.models.user;

import java.time.LocalDateTime;

public class User {
    private int id;
    private String email;
    private String password;
    private String roles;
    private String name;
    private int loginCount;
    private String imageUrl;
    private String numTel;
    private String cin;
    private LocalDateTime penalizedUntil;

    // No-argument constructor
    public User() {
        // Default constructor
    }

    // Constructor
    public User(int id, String email, String password, String roles, String name,
                int loginCount, String imageUrl, String numTel, String cin, LocalDateTime penalizedUntil) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.roles = roles;
        this.name = name;
        this.loginCount = loginCount;
        this.imageUrl = imageUrl;
        this.numTel = numTel;
        this.cin = cin;
        this.penalizedUntil = penalizedUntil;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRoles() { return roles; }
    public void setRoles(String roles) { this.roles = roles; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getLoginCount() { return loginCount; }
    public void setLoginCount(int loginCount) { this.loginCount = loginCount; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getNumTel() { return numTel; }
    public void setNumTel(String numTel) { this.numTel = numTel; }

    public String getCin() { return cin; }
    public void setCin(String cin) { this.cin = cin; }

    public LocalDateTime getPenalizedUntil() { return penalizedUntil; }
    public void setPenalizedUntil(LocalDateTime penalizedUntil) { this.penalizedUntil = penalizedUntil; }

    /**
     * Checks if the user is currently penalized
     * @return true if the user is penalized (penalizedUntil is in the future), false otherwise
     */
    public boolean isPenalized() {
        if (penalizedUntil == null) {
            return false;
        }
        return LocalDateTime.now().isBefore(penalizedUntil);
    }
    
    /**
     * Increments the login count by 1
     */
    public void incrementLoginCount() {
        this.loginCount++;
    }
    
    /**
     * Checks if the user has admin role
     * @return true if the user has admin role, false otherwise
     */
    public boolean isAdmin() {
        return roles != null && roles.contains("ADMIN");
    }

    // toString for Debugging
    @Override
    public String toString() {
        return "User{" +
                "email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", roles='" + roles + '\'' +
                ", name='" + name + '\'' +
                ", loginCount=" + loginCount +
                ", imageUrl='" + imageUrl + '\'' +
                ", numTel='" + numTel + '\'' +
                ", cin='" + cin + '\'' +
                ", penalizedUntil=" + penalizedUntil +
                '}';
    }
}
