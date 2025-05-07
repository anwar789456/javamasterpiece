package org.example.services;

import org.example.models.course.Course;
import utils.dataSource;

import java.sql.*;
import java.util.*;

/**
 * Service class to provide statistics and analytics data for courses
 */
public class StatisticsService {

    private CourseService courseService;
    
    public StatisticsService() {
        this.courseService = new CourseService();
    }
    
    /**
     * Get most liked courses based on thumbsup reviews
     * @param limit Number of top courses to return
     * @return Map of course objects and their like counts, sorted by most likes
     */
    public Map<Course, Integer> getMostLikedCourses(int limit) throws SQLException {
        Map<Course, Integer> courseToLikesMap = new LinkedHashMap<>();
        Connection connection = dataSource.getInstance().getConnection();
        
        try {
            // SQL query to get courses with their like counts, ordered by likes (thumbsup=1)
            String query = "SELECT idcours_id, COUNT(*) as like_count FROM reviews " +
                           "WHERE thumbsup = 1 " +
                           "GROUP BY idcours_id " +
                           "ORDER BY like_count DESC " +
                           "LIMIT ?";
            
            PreparedStatement pst = connection.prepareStatement(query);
            pst.setInt(1, limit);
            
            ResultSet rs = pst.executeQuery();
            
            while (rs.next()) {
                int courseId = rs.getInt("idcours_id");
                int likeCount = rs.getInt("like_count");
                
                // Get the course object using CourseService
                Course course = courseService.getCourseById(courseId);
                if (course != null) {
                    courseToLikesMap.put(course, likeCount);
                }
            }
            
            return courseToLikesMap;
            
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
    }
    
    /**
     * Get most popular courses based on number of subscriptions
     * @param limit Number of top courses to return
     * @return Map of course objects and their subscription counts, sorted by most subscriptions
     */
    public Map<Course, Integer> getMostPopularCourses(int limit) throws SQLException {
        Map<Course, Integer> courseToSubscriptionsMap = new LinkedHashMap<>();
        Connection connection = dataSource.getInstance().getConnection();
        
        try {
            // SQL query to get courses with their subscription counts
            String query = "SELECT idcours_id, COUNT(*) as subscription_count FROM inscriptioncours " +
                           "GROUP BY idcours_id " +
                           "ORDER BY subscription_count DESC " +
                           "LIMIT ?";
            
            PreparedStatement pst = connection.prepareStatement(query);
            pst.setInt(1, limit);
            
            ResultSet rs = pst.executeQuery();
            
            while (rs.next()) {
                int courseId = rs.getInt("idcours_id");
                int subscriptionCount = rs.getInt("subscription_count");
                
                // Get the course object using CourseService
                Course course = courseService.getCourseById(courseId);
                if (course != null) {
                    courseToSubscriptionsMap.put(course, subscriptionCount);
                }
            }
            
            return courseToSubscriptionsMap;
            
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
    }
    
    /**
     * Get like/dislike ratio for a specific course
     * @param courseId The course ID
     * @return Map with keys "likes" and "dislikes" containing counts
     */
    public Map<String, Integer> getCourseLikeDislikeRatio(int courseId) throws SQLException {
        Map<String, Integer> ratioMap = new HashMap<>();
        Connection connection = dataSource.getInstance().getConnection();
        
        try {
            String query = "SELECT thumbsup, COUNT(*) as count FROM reviews " +
                           "WHERE idcours_id = ? " +
                           "GROUP BY thumbsup";
            
            PreparedStatement pst = connection.prepareStatement(query);
            pst.setInt(1, courseId);
            
            ResultSet rs = pst.executeQuery();
            
            // Initialize with zeros
            ratioMap.put("likes", 0);
            ratioMap.put("dislikes", 0);
            
            while (rs.next()) {
                boolean isLike = rs.getBoolean("thumbsup");
                int count = rs.getInt("count");
                
                if (isLike) {
                    ratioMap.put("likes", count);
                } else {
                    ratioMap.put("dislikes", count);
                }
            }
            
            return ratioMap;
            
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
    }
} 