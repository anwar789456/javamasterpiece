package org.example.services;

import org.example.models.course.Review;
import org.example.services.SentimentAnalysisService.Sentiment;
import utils.dataSource;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service class to handle CRUD operations for course reviews
 */
public class ReviewService {
    // SQL Queries
    private static final String INSERT_QUERY = "INSERT INTO reviews (idcours_id, user_id, thumbsup, review, datecreation) VALUES (?, ?, ?, ?, ?)";
    private static final String UPDATE_QUERY = "UPDATE reviews SET thumbsup = ?, review = ? WHERE id = ?";
    private static final String SELECT_BY_ID_QUERY = "SELECT * FROM reviews WHERE id = ?";
    private static final String SELECT_BY_COURSE_QUERY = "SELECT * FROM reviews WHERE idcours_id = ? ORDER BY datecreation DESC";
    private static final String SELECT_BY_USER_QUERY = "SELECT * FROM reviews WHERE user_id = ? ORDER BY datecreation DESC";
    private static final String CHECK_USER_REVIEW_QUERY = "SELECT * FROM reviews WHERE idcours_id = ? AND user_id = ?";
    private static final String DELETE_QUERY = "DELETE FROM reviews WHERE id = ?";
    
    // Sentiment Analysis Service
    private SentimentAnalysisService sentimentService = new SentimentAnalysisService();
    
    /**
     * Add a new review for a course
     */
    public Review addReview(int courseId, int userId, boolean thumbsup, String reviewText) throws SQLException {
        Connection connection = dataSource.getInstance().getConnection();
        
        try (PreparedStatement pst = connection.prepareStatement(INSERT_QUERY, Statement.RETURN_GENERATED_KEYS)) {
            LocalDateTime now = LocalDateTime.now();
            
            pst.setInt(1, courseId);
            pst.setInt(2, userId);
            pst.setBoolean(3, thumbsup);
            pst.setString(4, reviewText);
            pst.setTimestamp(5, Timestamp.valueOf(now));
            
            int affectedRows = pst.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Creating review failed, no rows affected.");
            }
            
            // Analyze sentiment
            Sentiment sentiment = Sentiment.NEUTRAL;
            try {
                sentiment = sentimentService.analyzeSentiment(reviewText);
            } catch (Exception e) {
                System.err.println("Error analyzing sentiment: " + e.getMessage());
                // Continue with neutral sentiment if analysis fails
            }
            
            try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int id = generatedKeys.getInt(1);
                    return new Review(id, courseId, userId, thumbsup, reviewText, now, sentiment);
                } else {
                    throw new SQLException("Creating review failed, no ID obtained.");
                }
            }
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
    }
    
    /**
     * Update an existing review
     */
    public Review updateReview(int reviewId, boolean thumbsup, String reviewText) throws SQLException {
        Connection connection = dataSource.getInstance().getConnection();
        
        try (PreparedStatement pst = connection.prepareStatement(UPDATE_QUERY)) {
            pst.setBoolean(1, thumbsup);
            pst.setString(2, reviewText);
            pst.setInt(3, reviewId);
            
            pst.executeUpdate();
            
            // Get the updated review
            Review updatedReview = getReviewById(reviewId);
            
            // Analyze sentiment
            if (updatedReview != null) {
                try {
                    Sentiment sentiment = sentimentService.analyzeSentiment(reviewText);
                    updatedReview.setSentiment(sentiment);
                } catch (Exception e) {
                    System.err.println("Error analyzing sentiment: " + e.getMessage());
                    // Continue with neutral sentiment if analysis fails
                }
            }
            
            return updatedReview;
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
    }
    
    /**
     * Delete a review
     */
    public void deleteReview(int reviewId) throws SQLException {
        Connection connection = dataSource.getInstance().getConnection();
        
        try (PreparedStatement pst = connection.prepareStatement(DELETE_QUERY)) {
            pst.setInt(1, reviewId);
            
            pst.executeUpdate();
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
    }
    
    /**
     * Get a review by its ID
     */
    public Review getReviewById(int reviewId) throws SQLException {
        Connection connection = dataSource.getInstance().getConnection();
        
        try (PreparedStatement pst = connection.prepareStatement(SELECT_BY_ID_QUERY)) {
            pst.setInt(1, reviewId);
            
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    Review review = mapResultSetToReview(rs);
                    // Analyze sentiment
                    try {
                        Sentiment sentiment = sentimentService.analyzeSentiment(review.getReview());
                        review.setSentiment(sentiment);
                    } catch (Exception e) {
                        System.err.println("Error analyzing sentiment for review " + reviewId + ": " + e.getMessage());
                    }
                    return review;
                }
            }
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
        
        return null;
    }
    
    /**
     * Get all reviews for a specific course
     */
    public List<Review> getReviewsByCourse(int courseId) throws SQLException {
        List<Review> reviews = new ArrayList<>();
        Connection connection = dataSource.getInstance().getConnection();
        
        try (PreparedStatement pst = connection.prepareStatement(SELECT_BY_COURSE_QUERY)) {
            pst.setInt(1, courseId);
            
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Review review = mapResultSetToReview(rs);
                    // Analyze sentiment
                    try {
                        Sentiment sentiment = sentimentService.analyzeSentiment(review.getReview());
                        review.setSentiment(sentiment);
                    } catch (Exception e) {
                        System.err.println("Error analyzing sentiment for review in course " + courseId + ": " + e.getMessage());
                    }
                    reviews.add(review);
                }
            }
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
        
        return reviews;
    }
    
    /**
     * Get all reviews from a specific user
     */
    public List<Review> getReviewsByUser(int userId) throws SQLException {
        List<Review> reviews = new ArrayList<>();
        Connection connection = dataSource.getInstance().getConnection();
        
        try (PreparedStatement pst = connection.prepareStatement(SELECT_BY_USER_QUERY)) {
            pst.setInt(1, userId);
            
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Review review = mapResultSetToReview(rs);
                    // Analyze sentiment
                    try {
                        Sentiment sentiment = sentimentService.analyzeSentiment(review.getReview());
                        review.setSentiment(sentiment);
                    } catch (Exception e) {
                        System.err.println("Error analyzing sentiment for review by user " + userId + ": " + e.getMessage());
                    }
                    reviews.add(review);
                }
            }
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
        
        return reviews;
    }
    
    /**
     * Check if a user has already reviewed a course
     * Returns the existing review if found, null otherwise
     */
    public Review getUserReviewForCourse(int courseId, int userId) throws SQLException {
        Connection connection = dataSource.getInstance().getConnection();
        
        try (PreparedStatement pst = connection.prepareStatement(CHECK_USER_REVIEW_QUERY)) {
            pst.setInt(1, courseId);
            pst.setInt(2, userId);
            
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    Review review = mapResultSetToReview(rs);
                    // Analyze sentiment
                    try {
                        Sentiment sentiment = sentimentService.analyzeSentiment(review.getReview());
                        review.setSentiment(sentiment);
                    } catch (Exception e) {
                        System.err.println("Error analyzing sentiment for user " + userId + " review in course " + courseId + ": " + e.getMessage());
                    }
                    return review;
                }
            }
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
        
        return null;
    }
    
    /**
     * Helper method to map ResultSet to Review object
     */
    private Review mapResultSetToReview(ResultSet rs) throws SQLException {
        return new Review(
            rs.getInt("id"),
            rs.getInt("idcours_id"),
            rs.getInt("user_id"),
            rs.getBoolean("thumbsup"),
            rs.getString("review"),
            rs.getTimestamp("datecreation").toLocalDateTime()
        );
    }
} 