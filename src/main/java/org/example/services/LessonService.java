package org.example.services;

import org.example.models.lesson.Lesson;
import utils.dataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LessonService {
    private static final String INSERT_QUERY = "INSERT INTO lesson (idcours_id, titre, description, videourl, ordre) VALUES (?, ?, ?, ?, ?)";
    private static final String UPDATE_QUERY = "UPDATE lesson SET idcours_id = ?, titre = ?, description = ?, videourl = ?, ordre = ? WHERE id = ?";
    private static final String DELETE_QUERY = "DELETE FROM lesson WHERE id = ?";
    private static final String SELECT_ALL_QUERY = "SELECT * FROM lesson ORDER BY ordre ASC";
    private static final String SELECT_BY_ID_QUERY = "SELECT * FROM lesson WHERE id = ?";
    private static final String SELECT_BY_COURSE_QUERY = "SELECT * FROM lesson WHERE idcours_id = ? ORDER BY ordre ASC";
    private static final String SELECT_MAX_ORDER_QUERY = "SELECT MAX(ordre) as max_ordre FROM lesson WHERE idcours_id = ?";
    private static final String SELECT_LESSONS_WITH_HIGHER_ORDER = "SELECT * FROM lesson WHERE idcours_id = ? AND ordre >= ? ORDER BY ordre ASC";
    private static final String UPDATE_LESSON_ORDER = "UPDATE lesson SET ordre = ? WHERE id = ?";
    
    /**
     * Add a new lesson with automatic order adjustment
     */
    public void addLesson(Lesson lesson) throws SQLException {
        Connection connection = dataSource.getInstance().getConnection();
        try {
            // Start a transaction
            connection.setAutoCommit(false);
            
            // If no order specified, set to the max + 1
            if (lesson.getOrdre() <= 0) {
                int maxOrdre = getMaxOrderForCourse(connection, lesson.getIdcours_id());
                lesson.setOrdre(maxOrdre + 1);
            } else {
                // If order specified, move up other lessons
                reorderLessons(connection, lesson.getIdcours_id(), lesson.getOrdre());
            }
            
            // Insert the new lesson
            try (PreparedStatement pst = connection.prepareStatement(INSERT_QUERY, Statement.RETURN_GENERATED_KEYS)) {
                pst.setObject(1, lesson.getIdcours_id()); // Using setObject for null safety
                pst.setString(2, lesson.getTitre());
                pst.setString(3, lesson.getDescription());
                pst.setString(4, lesson.getVideourl());
                pst.setInt(5, lesson.getOrdre());
                
                int affectedRows = pst.executeUpdate();
                
                if (affectedRows > 0) {
                    ResultSet generatedKeys = pst.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        lesson.setId(generatedKeys.getInt(1));
                    }
                }
            }
            
            // Commit the transaction
            connection.commit();
            
        } catch (SQLException e) {
            // Rollback in case of error
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                    throw new SQLException("Error during rollback: " + ex.getMessage(), ex);
                }
            }
            throw e;
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    dataSource.getInstance().releaseConnection(connection);
                } catch (SQLException e) {
                    System.err.println("Error resetting auto-commit: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Update an existing lesson with order adjustment
     */
    public void updateLesson(Lesson lesson, int originalOrdre) throws SQLException {
        Connection connection = dataSource.getInstance().getConnection();
        try {
            // Start a transaction
            connection.setAutoCommit(false);
            
            // If order has changed, reorder lessons
            if (lesson.getOrdre() != originalOrdre) {
                reorderLessons(connection, lesson.getIdcours_id(), lesson.getOrdre());
            }
            
            // Update the lesson
            try (PreparedStatement pst = connection.prepareStatement(UPDATE_QUERY)) {
                pst.setObject(1, lesson.getIdcours_id()); // Using setObject for null safety
                pst.setString(2, lesson.getTitre());
                pst.setString(3, lesson.getDescription());
                pst.setString(4, lesson.getVideourl());
                pst.setInt(5, lesson.getOrdre());
                pst.setInt(6, lesson.getId());
                
                pst.executeUpdate();
            }
            
            // Commit the transaction
            connection.commit();
            
        } catch (SQLException e) {
            // Rollback in case of error
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                    throw new SQLException("Error during rollback: " + ex.getMessage(), ex);
                }
            }
            throw e;
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    dataSource.getInstance().releaseConnection(connection);
                } catch (SQLException e) {
                    System.err.println("Error resetting auto-commit: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Delete a lesson and reorder remaining lessons
     */
    public void deleteLesson(int id) throws SQLException {
        Lesson lesson = getLessonById(id);
        if (lesson == null) {
            throw new SQLException("Lesson with ID " + id + " not found");
        }
        
        Connection connection = dataSource.getInstance().getConnection();
        try {
            // Start a transaction
            connection.setAutoCommit(false);
            
            // Delete the lesson
            try (PreparedStatement pst = connection.prepareStatement(DELETE_QUERY)) {
                pst.setInt(1, id);
                pst.executeUpdate();
            }
            
            // Get lessons with higher order and shift them down
            try (PreparedStatement pst = connection.prepareStatement(SELECT_LESSONS_WITH_HIGHER_ORDER)) {
                pst.setObject(1, lesson.getIdcours_id());
                pst.setInt(2, lesson.getOrdre() + 1); // Shift only lessons with higher order
                
                try (ResultSet rs = pst.executeQuery()) {
                    while (rs.next()) {
                        int lessonId = rs.getInt("id");
                        int currentOrdre = rs.getInt("ordre");
                        
                        // Update order (shift down by 1)
                        try (PreparedStatement updatePst = connection.prepareStatement(UPDATE_LESSON_ORDER)) {
                            updatePst.setInt(1, currentOrdre - 1);
                            updatePst.setInt(2, lessonId);
                            updatePst.executeUpdate();
                        }
                    }
                }
            }
            
            // Commit the transaction
            connection.commit();
            
        } catch (SQLException e) {
            // Rollback in case of error
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                    throw new SQLException("Error during rollback: " + ex.getMessage(), ex);
                }
            }
            throw e;
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    dataSource.getInstance().releaseConnection(connection);
                } catch (SQLException e) {
                    System.err.println("Error resetting auto-commit: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Get all lessons
     */
    public List<Lesson> getAllLessons() throws SQLException {
        List<Lesson> lessons = new ArrayList<>();
        Connection connection = dataSource.getInstance().getConnection();
        
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(SELECT_ALL_QUERY)) {
            
            while (rs.next()) {
                lessons.add(mapResultSetToLesson(rs));
            }
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
        
        return lessons;
    }
    
    /**
     * Get a lesson by ID
     */
    public Lesson getLessonById(int id) throws SQLException {
        Connection connection = dataSource.getInstance().getConnection();
        
        try (PreparedStatement pst = connection.prepareStatement(SELECT_BY_ID_QUERY)) {
            pst.setInt(1, id);
            
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToLesson(rs);
                }
            }
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
        
        return null;
    }
    
    /**
     * Get lessons by course ID
     */
    public List<Lesson> getLessonsByCourse(int courseId) throws SQLException {
        List<Lesson> lessons = new ArrayList<>();
        Connection connection = dataSource.getInstance().getConnection();
        
        try (PreparedStatement pst = connection.prepareStatement(SELECT_BY_COURSE_QUERY)) {
            pst.setInt(1, courseId);
            
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    lessons.add(mapResultSetToLesson(rs));
                }
            }
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
        
        return lessons;
    }
    
    /**
     * Reorder lessons when inserting a new lesson with a specific order
     */
    private void reorderLessons(Connection connection, Integer courseId, int newOrder) throws SQLException {
        // Get lessons with same or higher order and shift them up
        try (PreparedStatement pst = connection.prepareStatement(SELECT_LESSONS_WITH_HIGHER_ORDER)) {
            pst.setObject(1, courseId);
            pst.setInt(2, newOrder);
            
            List<Integer> lessonIds = new ArrayList<>();
            List<Integer> currentOrders = new ArrayList<>();
            
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    lessonIds.add(rs.getInt("id"));
                    currentOrders.add(rs.getInt("ordre"));
                }
            }
            
            // Update order for each affected lesson (shift up by 1)
            for (int i = 0; i < lessonIds.size(); i++) {
                try (PreparedStatement updatePst = connection.prepareStatement(UPDATE_LESSON_ORDER)) {
                    updatePst.setInt(1, currentOrders.get(i) + 1);
                    updatePst.setInt(2, lessonIds.get(i));
                    updatePst.executeUpdate();
                }
            }
        }
    }
    
    /**
     * Get the maximum order number for a course
     */
    private int getMaxOrderForCourse(Connection connection, Integer courseId) throws SQLException {
        if (courseId == null) {
            return 0;
        }
        
        try (PreparedStatement pst = connection.prepareStatement(SELECT_MAX_ORDER_QUERY)) {
            pst.setInt(1, courseId);
            
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("max_ordre");
                }
            }
        }
        
        return 0;
    }
    
    /**
     * Map ResultSet to Lesson object
     */
    private Lesson mapResultSetToLesson(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        
        // Handle possible null for idcours_id
        Integer idcours_id = rs.getInt("idcours_id");
        if (rs.wasNull()) {
            idcours_id = null;
        }
        
        String titre = rs.getString("titre");
        String description = rs.getString("description");
        String videourl = rs.getString("videourl");
        int ordre = rs.getInt("ordre");
        
        return new Lesson(id, idcours_id, titre, description, videourl, ordre);
    }
} 