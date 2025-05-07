package org.example.controller.login;
import org.example.models.user.User;

public class SessionManager {
    private static SessionManager instance;
    private User currentUser;

    // Private constructor to prevent instantiation
    private SessionManager() {
        // Initialize with no user
        currentUser = null;
    }

    // Singleton pattern to ensure only one instance exists
    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    /**
     * Set the current logged-in user
     * @param user The user object to store in the session
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    /**
     * Get the current logged-in user
     * @return The current user object or null if no user is logged in
     */
    public User getCurrentUser() {
        return this.currentUser;
    }

    /**
     * Get the ID of the current logged-in user
     * @return The user ID or -1 if no user is logged in
     */
    public int getCurrentUserId() {
        return (currentUser != null) ? currentUser.getId() : -1;
    }

    /**
     * Check if a user is currently logged in
     * @return true if a user is logged in, false otherwise
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * End the current user session (logout)
     */
    public void clearSession() {
        this.currentUser = null;
    }
}