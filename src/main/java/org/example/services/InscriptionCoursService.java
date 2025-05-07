package org.example.services;

import org.example.models.course.InscriptionCours;
import utils.dataSource;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InscriptionCoursService {
    // SQL Queries
    private static final String INSERT_QUERY = "INSERT INTO inscriptioncours (idcours_id, iduser_id, dateinscription) VALUES (?, ?, ?)";
    private static final String SELECT_BY_COURSE_QUERY = "SELECT * FROM inscriptioncours WHERE idcours_id = ?";
    private static final String SELECT_BY_USER_QUERY = "SELECT * FROM inscriptioncours WHERE iduser_id = ?";
    private static final String COUNT_BY_COURSE_QUERY = "SELECT COUNT(*) as learner_count FROM inscriptioncours WHERE idcours_id = ?";
    private static final String COUNT_ALL_COURSES_QUERY = "SELECT idcours_id, COUNT(*) as learner_count FROM inscriptioncours GROUP BY idcours_id";
    private static final String CHECK_ENROLLMENT_QUERY = "SELECT COUNT(*) FROM inscriptioncours WHERE idcours_id = ? AND iduser_id = ?";
    private static final String DELETE_ENROLLMENT_QUERY = "DELETE FROM inscriptioncours WHERE idcours_id = ? AND iduser_id = ?";
    
    // Add a new course enrollment
    public void enrollUserInCourse(int userId, int courseId) throws SQLException {
        Connection connection = dataSource.getInstance().getConnection();
        try (PreparedStatement pst = connection.prepareStatement(INSERT_QUERY, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, courseId);
            pst.setInt(2, userId);
            pst.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            
            pst.executeUpdate();
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
    }
    
    // Check if a user is enrolled in a course
    public boolean isUserEnrolled(int userId, int courseId) throws SQLException {
        Connection connection = dataSource.getInstance().getConnection();
        try (PreparedStatement pst = connection.prepareStatement(CHECK_ENROLLMENT_QUERY)) {
            pst.setInt(1, courseId);
            pst.setInt(2, userId);
            
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
        
        return false;
    }
    
    // Remove a user's enrollment from a course
    public void unenrollUserFromCourse(int userId, int courseId) throws SQLException {
        Connection connection = dataSource.getInstance().getConnection();
        try (PreparedStatement pst = connection.prepareStatement(DELETE_ENROLLMENT_QUERY)) {
            pst.setInt(1, courseId);
            pst.setInt(2, userId);
            
            pst.executeUpdate();
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
    }
    
    // Get all enrollments for a specific course
    public List<InscriptionCours> getEnrollmentsByCourse(int courseId) throws SQLException {
        List<InscriptionCours> enrollments = new ArrayList<>();
        Connection connection = dataSource.getInstance().getConnection();
        
        try (PreparedStatement pst = connection.prepareStatement(SELECT_BY_COURSE_QUERY)) {
            pst.setInt(1, courseId);
            
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    enrollments.add(mapResultSetToInscriptionCours(rs));
                }
            }
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
        
        return enrollments;
    }
    
    // Get all courses a user is enrolled in
    public List<InscriptionCours> getEnrollmentsByUser(int userId) throws SQLException {
        List<InscriptionCours> enrollments = new ArrayList<>();
        Connection connection = dataSource.getInstance().getConnection();
        
        try (PreparedStatement pst = connection.prepareStatement(SELECT_BY_USER_QUERY)) {
            pst.setInt(1, userId);
            
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    enrollments.add(mapResultSetToInscriptionCours(rs));
                }
            }
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
        
        return enrollments;
    }
    
    // Count the number of learners for a specific course
    public int countLearnersByCourse(int courseId) throws SQLException {
        Connection connection = dataSource.getInstance().getConnection();
        try (PreparedStatement pst = connection.prepareStatement(COUNT_BY_COURSE_QUERY)) {
            pst.setInt(1, courseId);
            
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("learner_count");
                }
            }
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
        
        return 0;
    }
    
    // Get a map of course IDs to learner counts for all courses
    public Map<Integer, Integer> getAllCoursesLearnerCounts() throws SQLException {
        Map<Integer, Integer> learnerCounts = new HashMap<>();
        Connection connection = dataSource.getInstance().getConnection();
        
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(COUNT_ALL_COURSES_QUERY)) {
            
            while (rs.next()) {
                int courseId = rs.getInt("idcours_id");
                int learnerCount = rs.getInt("learner_count");
                learnerCounts.put(courseId, learnerCount);
            }
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
        
        return learnerCounts;
    }
    
    // Helper method to map ResultSet to InscriptionCours object
    private InscriptionCours mapResultSetToInscriptionCours(ResultSet rs) throws SQLException {
        return new InscriptionCours(
            rs.getInt("id"),
            rs.getInt("idcours_id"),
            rs.getInt("iduser_id"),
            rs.getTimestamp("dateinscription").toLocalDateTime()
        );
    }
} 