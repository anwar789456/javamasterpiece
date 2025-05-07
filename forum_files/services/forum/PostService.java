package org.example.services.forum;

import org.example.models.forum.Post;
import utils.dataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PostService {
    public void create(Post post) throws SQLException {
        String query = "INSERT INTO post (owner_id, title, content, created_at) VALUES (?, ?, ?, NOW())";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = dataSource.getInstance().getConnection();
            stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, post.getOwnerId());
            stmt.setString(2, post.getTitle());
            stmt.setString(3, post.getContent());
            stmt.executeUpdate();
            
            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                post.setId(rs.getInt(1));
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { /* ignore */ }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { /* ignore */ }
            if (conn != null) dataSource.getInstance().releaseConnection(conn);
        }
    }

    public Post read(int id) throws SQLException {
        String query = "SELECT * FROM post WHERE id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = dataSource.getInstance().getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, id);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return new Post(
                    rs.getInt("id"),
                    rs.getInt("owner_id"),
                    rs.getString("title"),
                    rs.getString("content"),
                    rs.getTimestamp("created_at").toLocalDateTime()
                );
            }
            return null;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { /* ignore */ }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { /* ignore */ }
            if (conn != null) dataSource.getInstance().releaseConnection(conn);
        }
    }

    public List<Post> readAll() throws SQLException {
        List<Post> posts = new ArrayList<>();
        String query = "SELECT * FROM post ORDER BY created_at DESC";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = dataSource.getInstance().getConnection();
            stmt = conn.prepareStatement(query);
            rs = stmt.executeQuery();
            while (rs.next()) {
                posts.add(new Post(
                    rs.getInt("id"),
                    rs.getInt("owner_id"),
                    rs.getString("title"),
                    rs.getString("content"),
                    rs.getTimestamp("created_at").toLocalDateTime()
                ));
            }
            return posts;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { /* ignore */ }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { /* ignore */ }
            if (conn != null) dataSource.getInstance().releaseConnection(conn);
        }
    }

    public void update(Post post) throws SQLException {
        String query = "UPDATE post SET title = ?, content = ? WHERE id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = dataSource.getInstance().getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, post.getTitle());
            stmt.setString(2, post.getContent());
            stmt.setInt(3, post.getId());
            stmt.executeUpdate();
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { /* ignore */ }
            if (conn != null) dataSource.getInstance().releaseConnection(conn);
        }
    }

    public void delete(int id) throws SQLException {
        String query = "DELETE FROM post WHERE id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = dataSource.getInstance().getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { /* ignore */ }
            if (conn != null) dataSource.getInstance().releaseConnection(conn);
        }
    }
} 