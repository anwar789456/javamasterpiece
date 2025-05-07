package org.example.services;

import org.example.models.lesson.Note;
import utils.dataSource;

import java.sql.*;

/**
 * Service class to handle CRUD operations for notes
 */
public class NoteService {
    // SQL Queries
    private static final String INSERT_QUERY = "INSERT INTO notes (user_id, lesson_id, note_text) VALUES (?, ?, ?)";
    private static final String UPDATE_QUERY = "UPDATE notes SET note_text = ? WHERE id = ?";
    private static final String SELECT_BY_ID_QUERY = "SELECT * FROM notes WHERE id = ?";
    private static final String SELECT_BY_LESSON_AND_USER_QUERY = "SELECT * FROM notes WHERE lesson_id = ? AND user_id = ?";
    private static final String DELETE_QUERY = "DELETE FROM notes WHERE id = ?";
    
    /**
     * Add a new note
     */
    public Note addNote(Note note) throws SQLException {
        Connection connection = dataSource.getInstance().getConnection();
        
        try (PreparedStatement pst = connection.prepareStatement(INSERT_QUERY, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, note.getUser_id());
            pst.setInt(2, note.getLesson_id());
            pst.setString(3, note.getNote_text());
            
            int affectedRows = pst.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Creating note failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    note.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating note failed, no ID obtained.");
                }
            }
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
        
        return note;
    }
    
    /**
     * Update an existing note
     */
    public void updateNote(Note note) throws SQLException {
        Connection connection = dataSource.getInstance().getConnection();
        
        try (PreparedStatement pst = connection.prepareStatement(UPDATE_QUERY)) {
            pst.setString(1, note.getNote_text());
            pst.setInt(2, note.getId());
            
            pst.executeUpdate();
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
    }
    
    /**
     * Get a note by ID
     */
    public Note getNoteById(int id) throws SQLException {
        Note note = null;
        Connection connection = dataSource.getInstance().getConnection();
        
        try (PreparedStatement pst = connection.prepareStatement(SELECT_BY_ID_QUERY)) {
            pst.setInt(1, id);
            
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    note = mapResultSetToNote(rs);
                }
            }
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
        
        return note;
    }
    
    /**
     * Get a note by lesson ID and user ID
     */
    public Note getNoteByLessonAndUser(int lessonId, int userId) throws SQLException {
        Note note = null;
        Connection connection = dataSource.getInstance().getConnection();
        
        try (PreparedStatement pst = connection.prepareStatement(SELECT_BY_LESSON_AND_USER_QUERY)) {
            pst.setInt(1, lessonId);
            pst.setInt(2, userId);
            
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    note = mapResultSetToNote(rs);
                }
            }
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
        
        return note;
    }
    
    /**
     * Delete a note by ID
     */
    public void deleteNote(int id) throws SQLException {
        Connection connection = dataSource.getInstance().getConnection();
        
        try (PreparedStatement pst = connection.prepareStatement(DELETE_QUERY)) {
            pst.setInt(1, id);
            pst.executeUpdate();
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
    }
    
    /**
     * Map a ResultSet row to a Note object
     */
    private Note mapResultSetToNote(ResultSet rs) throws SQLException {
        return new Note(
            rs.getInt("id"),
            rs.getInt("user_id"),
            rs.getInt("lesson_id"),
            rs.getString("note_text")
        );
    }
}