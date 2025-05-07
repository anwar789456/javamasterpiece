package org.example.services;

import org.example.models.TeacherApplication;
import utils.dataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service class to handle operations on the applications table
 */
public class TeacherApplicationService {
    
    private static final String INSERT_QUERY = "INSERT INTO applications (user_id, bio, skills, experience, portfolio_url, motivation) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String CHECK_APPLICATION_QUERY = "SELECT * FROM applications WHERE user_id = ?";
    private static final String GET_APPLICATION_QUERY = "SELECT * FROM applications WHERE id = ?";
    private static final String DELETE_APPLICATION_QUERY = "DELETE FROM applications WHERE id = ?";
    private static final String GET_ALL_APPLICATIONS_QUERY = "SELECT a.*, u.name FROM applications a JOIN user u ON a.user_id = u.id";
    
    /**
     * Submit a teacher application
     * 
     * @param application The teacher application to submit
     * @return true if the application was submitted successfully
     */
    public boolean submitApplication(TeacherApplication application) throws SQLException {
        // First check if the user has already applied
        if (hasApplied(application.getUserId())) {
            return false;
        }
        
        Connection connection = dataSource.getInstance().getConnection();
        try (PreparedStatement pst = connection.prepareStatement(INSERT_QUERY, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, application.getUserId());
            pst.setString(2, application.getBio());
            pst.setString(3, application.getSkills());
            pst.setString(4, application.getExperience());
            pst.setString(5, application.getPortfolioUrl());
            pst.setString(6, application.getMotivation());
            
            int affectedRows = pst.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        application.setId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
            return false;
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
    }
    
    /**
     * Check if a user has already applied to be a teacher
     * 
     * @param userId The ID of the user to check
     * @return true if the user has already applied, false otherwise
     */
    public boolean hasApplied(int userId) throws SQLException {
        Connection connection = dataSource.getInstance().getConnection();
        try (PreparedStatement pst = connection.prepareStatement(CHECK_APPLICATION_QUERY)) {
            pst.setInt(1, userId);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next(); // If there's a result, the user has already applied
            }
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
    }
    
    /**
     * Get a teacher application by ID
     * 
     * @param applicationId The ID of the application to retrieve
     * @return The TeacherApplication object if found, null otherwise
     */
    public TeacherApplication getApplicationById(int applicationId) throws SQLException {
        Connection connection = dataSource.getInstance().getConnection();
        try (PreparedStatement pst = connection.prepareStatement(GET_APPLICATION_QUERY)) {
            pst.setInt(1, applicationId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    TeacherApplication application = new TeacherApplication();
                    application.setId(rs.getInt("id"));
                    application.setUserId(rs.getInt("user_id"));
                    application.setBio(rs.getString("bio"));
                    application.setSkills(rs.getString("skills"));
                    application.setExperience(rs.getString("experience"));
                    application.setPortfolioUrl(rs.getString("portfolio_url"));
                    application.setMotivation(rs.getString("motivation"));
                    return application;
                }
                return null;
            }
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
    }
    
    /**
     * Get all teacher applications
     * 
     * @return List of all teacher applications
     */
    public List<TeacherApplication> getAllApplications() throws SQLException {
        List<TeacherApplication> applications = new ArrayList<>();
        Connection connection = dataSource.getInstance().getConnection();
        
        try (PreparedStatement pst = connection.prepareStatement(GET_ALL_APPLICATIONS_QUERY);
             ResultSet rs = pst.executeQuery()) {
            
            while (rs.next()) {
                TeacherApplication application = new TeacherApplication();
                application.setId(rs.getInt("id"));
                application.setUserId(rs.getInt("user_id"));
                application.setBio(rs.getString("bio"));
                application.setSkills(rs.getString("skills"));
                application.setExperience(rs.getString("experience"));
                application.setPortfolioUrl(rs.getString("portfolio_url"));
                application.setMotivation(rs.getString("motivation"));
                // User name can be included as a transient property
                application.setUserName(rs.getString("name"));
                applications.add(application);
            }
            
            return applications;
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
    }
    
    /**
     * Delete an application by ID
     * 
     * @param applicationId The ID of the application to delete
     * @return true if the application was deleted successfully
     */
    public boolean deleteApplication(int applicationId) throws SQLException {
        Connection connection = dataSource.getInstance().getConnection();
        try (PreparedStatement pst = connection.prepareStatement(DELETE_APPLICATION_QUERY)) {
            pst.setInt(1, applicationId);
            int affectedRows = pst.executeUpdate();
            return affectedRows > 0;
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
    }

    /**
     * Check if a user's application has been declined
     * 
     * @param userId The ID of the user to check
     * @return true if the application has been declined, false otherwise
     */
    public boolean isApplicationDeclined(int userId) throws SQLException {
        Connection connection = dataSource.getInstance().getConnection();
        
        try {
            // First find the application ID for this user
            String appQuery = "SELECT id FROM applications WHERE user_id = ?";
            PreparedStatement appStmt = connection.prepareStatement(appQuery);
            appStmt.setInt(1, userId);
            ResultSet appRs = appStmt.executeQuery();
            
            if (!appRs.next()) {
                // No application found for this user
                appRs.close();
                appStmt.close();
                return false;
            }
            
            int applicationId = appRs.getInt("id");
            appRs.close();
            appStmt.close();
            
            // Check if this application ID exists in the declined_applications table
            String declineQuery = "SELECT COUNT(*) FROM declined_applications WHERE application_id = ?";
            PreparedStatement declineStmt = connection.prepareStatement(declineQuery);
            declineStmt.setInt(1, applicationId);
            ResultSet declineRs = declineStmt.executeQuery();
            
            declineRs.next();
            int count = declineRs.getInt(1);
            
            declineRs.close();
            declineStmt.close();
            
            return count > 0;
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
    }

    /**
     * Get the decline reason for a user's application
     * 
     * @param userId The ID of the user to check
     * @return The decline reason if found, null if the application is not declined
     */
    public String getDeclineReason(int userId) throws SQLException {
        Connection connection = dataSource.getInstance().getConnection();
        
        try {
            // Find the user's application
            String appQuery = "SELECT id FROM applications WHERE user_id = ?";
            PreparedStatement appStmt = connection.prepareStatement(appQuery);
            appStmt.setInt(1, userId);
            ResultSet appRs = appStmt.executeQuery();
            
            if (!appRs.next()) {
                // No application found for this user
                appRs.close();
                appStmt.close();
                return null;
            }
            
            int applicationId = appRs.getInt("id");
            appRs.close();
            appStmt.close();
            
            // Check if this application has been declined
            String declineQuery = "SELECT decline_reason FROM declined_applications WHERE application_id = ?";
            PreparedStatement declineStmt = connection.prepareStatement(declineQuery);
            declineStmt.setInt(1, applicationId);
            ResultSet declineRs = declineStmt.executeQuery();
            
            if (!declineRs.next()) {
                // Application not declined
                declineRs.close();
                declineStmt.close();
                return null;
            }
            
            String reason = declineRs.getString("decline_reason");
            declineRs.close();
            declineStmt.close();
            return reason;
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
    }
} 