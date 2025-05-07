package org.example.services;

import org.example.models.course.Course;
import utils.dataSource;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CourseService {
    private static final String INSERT_QUERY = "INSERT INTO cours (idowner_id, titre, price, img, description, is_free, datecreation, likes, dislikes, minimal_hours, maximal_hours) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_QUERY = "UPDATE cours SET idowner_id = ?, titre = ?, price = ?, img = ?, description = ?, is_free = ?, likes = ?, dislikes = ?, minimal_hours = ?, maximal_hours = ? WHERE id = ?";
    private static final String DELETE_QUERY = "DELETE FROM cours WHERE id = ?";
    private static final String SELECT_ALL_QUERY = "SELECT * FROM cours";
    private static final String SELECT_BY_ID_QUERY = "SELECT * FROM cours WHERE id = ?";
    private static final String SELECT_BY_OWNER_QUERY = "SELECT * FROM cours WHERE idowner_id = ?";
    private static final String UPDATE_LIKES_QUERY = "UPDATE cours SET likes = ? WHERE id = ?";
    private static final String UPDATE_DISLIKES_QUERY = "UPDATE cours SET dislikes = ? WHERE id = ?";
    private static final String UPDATE_HOURS_QUERY = "UPDATE cours SET minimal_hours = ?, maximal_hours = ? WHERE id = ?";
    private static final String SELECT_BY_TEACHER_QUERY = "SELECT c.* FROM cours c " +
            "INNER JOIN teachers t ON c.idowner_id = t.user_id " +
            "WHERE t.user_id = ?";

    // Add a new course
    public void addCourse(Course course) throws SQLException {
        Connection connection = dataSource.getInstance().getConnection();
        try (PreparedStatement pst = connection.prepareStatement(INSERT_QUERY, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, course.getIdowner_id());
            pst.setString(2, course.getTitre());
            pst.setDouble(3, course.getPrice());
            pst.setString(4, course.getImg());
            pst.setString(5, course.getDescription());
            pst.setBoolean(6, course.isIs_free());
            pst.setTimestamp(7, Timestamp.valueOf(course.getDatecreation()));
            pst.setInt(8, course.getLikes());
            pst.setInt(9, course.getDislikes());
            pst.setInt(10, course.getMinimal_hours());
            pst.setInt(11, course.getMaximal_hours());

            int affectedRows = pst.executeUpdate();

            if (affectedRows > 0) {
                ResultSet generatedKeys = pst.getGeneratedKeys();
                if (generatedKeys.next()) {
                    course.setId(generatedKeys.getInt(1));
                }
            }
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
    }

    // Update an existing course
    public void updateCourse(Course course) throws SQLException {
        Connection connection = dataSource.getInstance().getConnection();
        try (PreparedStatement pst = connection.prepareStatement(UPDATE_QUERY)) {
            pst.setInt(1, course.getIdowner_id());
            pst.setString(2, course.getTitre());
            pst.setDouble(3, course.getPrice());
            pst.setString(4, course.getImg());
            pst.setString(5, course.getDescription());
            pst.setBoolean(6, course.isIs_free());
            pst.setInt(7, course.getLikes());
            pst.setInt(8, course.getDislikes());
            pst.setInt(9, course.getMinimal_hours());
            pst.setInt(10, course.getMaximal_hours());
            pst.setInt(11, course.getId());

            pst.executeUpdate();
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
    }

    // Delete a course
    public void deleteCourse(int id) throws SQLException {
        Connection connection = dataSource.getInstance().getConnection();
        try (PreparedStatement pst = connection.prepareStatement(DELETE_QUERY)) {
            pst.setInt(1, id);
            pst.executeUpdate();
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
    }

    // Get all courses
    public List<Course> getAllCourses() throws SQLException {
        List<Course> courses = new ArrayList<>();
        Connection connection = dataSource.getInstance().getConnection();

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(SELECT_ALL_QUERY)) {

            while (rs.next()) {
                courses.add(mapResultSetToCourse(rs));
            }
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }

        return courses;
    }

    // Get a course by ID
    public Course getCourseById(int id) throws SQLException {
        Course course = null;
        Connection connection = dataSource.getInstance().getConnection();

        try (PreparedStatement pst = connection.prepareStatement(SELECT_BY_ID_QUERY)) {
            pst.setInt(1, id);

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    course = mapResultSetToCourse(rs);
                }
            }
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }

        return course;
    }

    // Get courses by owner ID
    public List<Course> getCoursesByOwner(int ownerId) throws SQLException {
        List<Course> courses = new ArrayList<>();
        Connection connection = dataSource.getInstance().getConnection();

        try (PreparedStatement pst = connection.prepareStatement(SELECT_BY_OWNER_QUERY)) {
            pst.setInt(1, ownerId);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    courses.add(mapResultSetToCourse(rs));
                }
            }
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }

        return courses;
    }

    // Update course likes
    public void updateCourseLikes(int courseId, int likes) throws SQLException {
        Connection connection = dataSource.getInstance().getConnection();
        try (PreparedStatement pst = connection.prepareStatement(UPDATE_LIKES_QUERY)) {
            pst.setInt(1, likes);
            pst.setInt(2, courseId);
            pst.executeUpdate();
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
    }

    // Update course dislikes
    public void updateCourseDislikes(int courseId, int dislikes) throws SQLException {
        Connection connection = dataSource.getInstance().getConnection();
        try (PreparedStatement pst = connection.prepareStatement(UPDATE_DISLIKES_QUERY)) {
            pst.setInt(1, dislikes);
            pst.setInt(2, courseId);
            pst.executeUpdate();
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
    }

    // Update course hours
    public void updateCourseHours(int courseId, int minimalHours, int maximalHours) throws SQLException {
        Connection connection = dataSource.getInstance().getConnection();
        try (PreparedStatement pst = connection.prepareStatement(UPDATE_HOURS_QUERY)) {
            pst.setInt(1, minimalHours);
            pst.setInt(2, maximalHours);
            pst.setInt(3, courseId);
            pst.executeUpdate();
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }
    }

    // Get courses by teacher ID (user_id in teachers table)
    public List<Course> getCoursesByTeacher(int teacherId) throws SQLException {
        List<Course> courses = new ArrayList<>();
        Connection connection = dataSource.getInstance().getConnection();

        try (PreparedStatement pst = connection.prepareStatement(SELECT_BY_TEACHER_QUERY)) {
            pst.setInt(1, teacherId);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    courses.add(mapResultSetToCourse(rs));
                }
            }
        } finally {
            dataSource.getInstance().releaseConnection(connection);
        }

        return courses;
    }

    // Helper method to map ResultSet to Course object
    private Course mapResultSetToCourse(ResultSet rs) throws SQLException {
        return new Course(
                rs.getInt("id"),
                rs.getInt("idowner_id"),
                rs.getString("titre"),
                rs.getDouble("price"),
                rs.getString("img"),
                rs.getString("description"),
                rs.getBoolean("is_free"),
                rs.getTimestamp("datecreation").toLocalDateTime(),
                rs.getInt("likes"),
                rs.getInt("dislikes"),
                rs.getInt("minimal_hours"),
                rs.getInt("maximal_hours")
        );
    }
} 