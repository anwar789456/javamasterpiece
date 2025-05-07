package org.example.util;

public class UserSession {
    private static UserSession instance;
    private String userEmail;
    private int userId;

    private UserSession() {
        // Private constructor to enforce singleton pattern
    }

    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    public void setUserEmail(String email) {
        this.userEmail = email;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserId(int id) {
        this.userId = id;
    }

    public int getUserId() {
        return userId;
    }

    public void clearSession() {
        userEmail = null;
        userId = 0;
    }
}