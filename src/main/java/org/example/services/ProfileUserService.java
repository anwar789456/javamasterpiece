package org.example.services;

import utils.dataSource;
import org.example.models.user.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service class to handle user operations for the profile page
 * This service returns org.example.models.user.User objects
 */
public class ProfileUserService {
    private static final String SELECT_BY_ID_QUERY = "SELECT * FROM user WHERE id = ?";
    private static final String SELECT_ALL_QUERY = "SELECT * FROM user";

    /**
     * Get a user by ID
     * @param id The user ID
     * @return The user object or null if not found
     */
    public User getUserById(int id) throws SQLException {
        User user = null;
        Connection connection = dataSource.getInstance().getConnection();
        
        try (PreparedStatement pst = connection.prepareStatement(SELECT_BY_ID_QUERY)) {
            pst.setInt(1, id);
            
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    user = mapResultSetToUser(rs);
                }
            }
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
        
        return user;
    }
    
    /**
     * Get all users
     * @return List of all users
     */
    public List<User> getAllUsers() throws SQLException {
        List<User> users = new ArrayList<>();
        Connection connection = dataSource.getInstance().getConnection();
        
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(SELECT_ALL_QUERY)) {
            
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
        
        return users;
    }
    
    /**
     * Helper method to map ResultSet to User object
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setCin(rs.getString("cin"));
        user.setEmail(rs.getString("email"));
        user.setRoles(rs.getString("roles"));
        user.setName(rs.getString("name"));
        user.setLoginCount(rs.getInt("login_count"));
        user.setImageUrl(rs.getString("image_url"));
        user.setNumTel(rs.getString("numtel"));
        
        Timestamp penalizedUntil = rs.getTimestamp("penalized_until");
        if (penalizedUntil != null) {
            user.setPenalizedUntil(penalizedUntil.toLocalDateTime());
        }
        
        return user;
    }
} 