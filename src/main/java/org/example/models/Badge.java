package org.example.models;

import java.time.LocalDateTime;

public class Badge {
    private int id;
    private int userId;
    private String typeBadge;
    private int nbrStars;
    private String userEmail;
    private LocalDateTime dateAwarded;

    public Badge(int id, int userId, String typeBadge, int nbrStars) {
        this.id = id;
        this.userId = userId;
        this.typeBadge = typeBadge;
        this.nbrStars = nbrStars;
        this.dateAwarded = LocalDateTime.now();
    }

    // Getters and Setters
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

    public String getTypeBadge() {
        return typeBadge;
    }

    public void setTypeBadge(String typeBadge) {
        this.typeBadge = typeBadge;
    }

    public int getNbrStars() {
        return nbrStars;
    }

    public void setNbrStars(int nbrStars) {
        this.nbrStars = nbrStars;
        // Update typeBadge based on nbrStars
        if (nbrStars <= 3) {
            this.typeBadge = "silver";
        } else if (nbrStars >= 4 && nbrStars <= 5) {
            this.typeBadge = "gold";
        }
    }
    
    // Email getter and setter
    public String getUserEmail() {
        return userEmail;
    }
    
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public LocalDateTime getDateAwarded() {
        return dateAwarded;
    }

    public void setDateAwarded(LocalDateTime dateAwarded) {
        this.dateAwarded = dateAwarded;
    }
} 