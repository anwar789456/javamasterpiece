package org.example.services;

import org.example.models.DeclinedApplication;
import utils.dataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service class to handle operations on the declined_applications table
 */
public class DeclinedApplicationsService {
    
    private static final String INSERT_QUERY = "INSERT INTO declined_applications (application_id, admin_id, decline_reason) VALUES (?, ?, ?)";
    private static final String GET_BY_APP_ID_QUERY = "SELECT * FROM declined_applications WHERE application_id = ?";
    private static final String GET_ALL_QUERY = "SELECT da.*, u.name as admin_name FROM declined_applications da " +
            "JOIN user u ON da.admin_id = u.id";
    
    /**
     * Save a declined application
     * 
     * @param applicationId The ID of the application being declined
     * @param adminId The ID of the admin who declined the application
     * @param reason The reason for declining the application
     * @return true if the declined application was saved successfully
     */
    public boolean saveDeclinedApplication(int applicationId, int adminId, String reason) throws SQLException {
        Connection connection = dataSource.getInstance().getConnection();
        try (PreparedStatement pst = connection.prepareStatement(INSERT_QUERY)) {
            pst.setInt(1, applicationId);
            pst.setInt(2, adminId);
            pst.setString(3, reason);
            
            int affectedRows = pst.executeUpdate();
            return affectedRows > 0;
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
    }
    
    /**
     * Get a declined application by application ID
     * 
     * @param applicationId The ID of the application
     * @return The DeclinedApplication object if found, null otherwise
     */
    public DeclinedApplication getByApplicationId(int applicationId) throws SQLException {
        Connection connection = dataSource.getInstance().getConnection();
        try (PreparedStatement pst = connection.prepareStatement(GET_BY_APP_ID_QUERY)) {
            pst.setInt(1, applicationId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToDeclinedApplication(rs);
                }
                return null;
            }
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
    }
    
    /**
     * Get all declined applications
     * 
     * @return List of all declined applications
     */
    public List<DeclinedApplication> getAllDeclinedApplications() throws SQLException {
        List<DeclinedApplication> declinedApplications = new ArrayList<>();
        Connection connection = dataSource.getInstance().getConnection();
        
        try (PreparedStatement pst = connection.prepareStatement(GET_ALL_QUERY);
             ResultSet rs = pst.executeQuery()) {
            
            while (rs.next()) {
                declinedApplications.add(mapResultSetToDeclinedApplication(rs));
            }
            
            return declinedApplications;
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
    }
    
    /**
     * Map ResultSet to DeclinedApplication object
     */
    private DeclinedApplication mapResultSetToDeclinedApplication(ResultSet rs) throws SQLException {
        DeclinedApplication declinedApplication = new DeclinedApplication();
        declinedApplication.setId(rs.getInt("id"));
        declinedApplication.setApplicationId(rs.getInt("application_id"));
        declinedApplication.setAdminId(rs.getInt("admin_id"));
        declinedApplication.setDeclineReason(rs.getString("decline_reason"));
        
        // Check if admin_name column exists (from join query)
        try {
            declinedApplication.setAdminName(rs.getString("admin_name"));
        } catch (SQLException e) {
            // Column doesn't exist, ignore
        }
        
        return declinedApplication;
    }
} 