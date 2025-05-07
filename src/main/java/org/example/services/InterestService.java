package org.example.services;

import org.example.models.course.Course;
import org.example.models.interest.Interest;
import utils.dataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InterestService {
    // SQL queries for interests
    private static final String SELECT_ALL_INTERESTS = "SELECT * FROM interests ORDER BY name";
    private static final String SELECT_INTEREST_BY_ID = "SELECT * FROM interests WHERE id = ?";
    private static final String SELECT_INTERESTS_BY_COURSE = "SELECT i.* FROM interests i " +
            "JOIN course_interests ci ON i.id = ci.interest_id " +
            "WHERE ci.course_id = ?";
    private static final String SEARCH_INTERESTS = "SELECT * FROM interests WHERE name LIKE ? ORDER BY name";
    
    // SQL queries for course interests
    private static final String SELECT_COURSE_IDS_BY_INTERESTS = "SELECT DISTINCT course_id FROM course_interests " +
            "WHERE interest_id IN (%s)";
    
    private static final String COUNT_COURSES_BY_INTEREST = "SELECT COUNT(DISTINCT course_id) AS course_count " +
            "FROM course_interests WHERE interest_id = ?";
    
    /**
     * Get all interests from the database
     */
    public List<Interest> getAllInterests() throws SQLException {
        List<Interest> interests = new ArrayList<>();
        Connection connection = dataSource.getInstance().getConnection();
        
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(SELECT_ALL_INTERESTS)) {
            
            while (rs.next()) {
                interests.add(mapResultSetToInterest(rs));
            }
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
        
        return interests;
    }
    
    /**
     * Get an interest by its ID
     */
    public Interest getInterestById(int id) throws SQLException {
        Interest interest = null;
        Connection connection = dataSource.getInstance().getConnection();
        
        try (PreparedStatement pst = connection.prepareStatement(SELECT_INTEREST_BY_ID)) {
            pst.setInt(1, id);
            
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    interest = mapResultSetToInterest(rs);
                }
            }
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
        
        return interest;
    }
    
    /**
     * Get all interests associated with a specific course
     */
    public List<Interest> getInterestsByCourse(int courseId) throws SQLException {
        List<Interest> interests = new ArrayList<>();
        Connection connection = dataSource.getInstance().getConnection();
        
        try (PreparedStatement pst = connection.prepareStatement(SELECT_INTERESTS_BY_COURSE)) {
            pst.setInt(1, courseId);
            
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    interests.add(mapResultSetToInterest(rs));
                }
            }
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
        
        return interests;
    }
    
    /**
     * Search for interests by name (case insensitive, partial match)
     */
    public List<Interest> searchInterests(String searchTerm) throws SQLException {
        List<Interest> interests = new ArrayList<>();
        Connection connection = dataSource.getInstance().getConnection();
        
        try (PreparedStatement pst = connection.prepareStatement(SEARCH_INTERESTS)) {
            pst.setString(1, "%" + searchTerm + "%");
            
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    interests.add(mapResultSetToInterest(rs));
                }
            }
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
        
        return interests;
    }
    
    /**
     * Get all course IDs that match the specified interests
     * @param interestIds List of interest IDs to filter by
     * @return Set of course IDs
     */
    public Set<Integer> getCourseIdsByInterests(List<Integer> interestIds) throws SQLException {
        Set<Integer> courseIds = new HashSet<>();
        
        if (interestIds == null || interestIds.isEmpty()) {
            return courseIds;
        }
        
        // Build the IN clause for the SQL query
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < interestIds.size(); i++) {
            if (i > 0) {
                placeholders.append(",");
            }
            placeholders.append("?");
        }
        
        String query = String.format(SELECT_COURSE_IDS_BY_INTERESTS, placeholders.toString());
        Connection connection = dataSource.getInstance().getConnection();
        
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            // Set the interest IDs as parameters
            for (int i = 0; i < interestIds.size(); i++) {
                pst.setInt(i + 1, interestIds.get(i));
            }
            
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    courseIds.add(rs.getInt("course_id"));
                }
            }
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
        
        return courseIds;
    }
    
    /**
     * Get courses filtered by interest IDs
     * @param interestIds List of interest IDs to filter by
     * @return List of courses
     */
    public List<Course> getCoursesByInterests(List<Integer> interestIds) throws SQLException {
        if (interestIds == null || interestIds.isEmpty()) {
            // If no interests selected, return all courses
            return new CourseService().getAllCourses();
        }
        
        // Get the matching course IDs
        Set<Integer> courseIds = getCourseIdsByInterests(interestIds);
        
        if (courseIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Fetch the actual courses
        List<Course> courses = new ArrayList<>();
        CourseService courseService = new CourseService();
        
        for (int courseId : courseIds) {
            Course course = courseService.getCourseById(courseId);
            if (course != null) {
                courses.add(course);
            }
        }
        
        return courses;
    }
    
    /**
     * Get the number of courses associated with a specific interest
     */
    public int getCoursesCountByInterest(int interestId) throws SQLException {
        int count = 0;
        Connection connection = dataSource.getInstance().getConnection();
        
        try (PreparedStatement pst = connection.prepareStatement(COUNT_COURSES_BY_INTEREST)) {
            pst.setInt(1, interestId);
            
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    count = rs.getInt("course_count");
                }
            }
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
        
        return count;
    }
    
    /**
     * Map a database result set to an Interest object
     */
    private Interest mapResultSetToInterest(ResultSet rs) throws SQLException {
        return new Interest(
            rs.getInt("id"),
            rs.getString("name")
        );
    }
} 