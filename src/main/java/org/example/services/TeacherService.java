package org.example.services;

import utils.dataSource;

import java.sql.*;

/**
 * Service class to handle operations on the teachers table
 */
public class TeacherService {
    
    private static final String INSERT_QUERY = "INSERT INTO teachers (user_id) VALUES (?)";
    private static final String CHECK_TEACHER_QUERY = "SELECT * FROM teachers WHERE user_id = ?";
    
    /**
     * Add a user as a teacher
     * 
     * @param userId The ID of the user to add as a teacher
     * @return true if the user was added successfully, false if they're already a teacher
     */
    public boolean addTeacher(int userId) throws SQLException {
        // First check if the user is already a teacher
        if (isTeacher(userId)) {
            return false;
        }
        
        Connection connection = dataSource.getInstance().getConnection();
        try (PreparedStatement pst = connection.prepareStatement(INSERT_QUERY)) {
            pst.setInt(1, userId);
            int affectedRows = pst.executeUpdate();
            return affectedRows > 0;
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
    }
    
    /**
     * Check if a user is already a teacher
     * 
     * @param userId The ID of the user to check
     * @return true if the user is already a teacher, false otherwise
     */
    public boolean isTeacher(int userId) throws SQLException {
        Connection connection = dataSource.getInstance().getConnection();
        try (PreparedStatement pst = connection.prepareStatement(CHECK_TEACHER_QUERY)) {
            pst.setInt(1, userId);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next(); // If there's a result, the user is already a teacher
            }
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
    }
} 