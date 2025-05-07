package org.example.services.forum;

import org.example.models.forum.Comment;
import utils.dataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommentService {
    public void create(Comment comment) throws SQLException {
        String query = "INSERT INTO comment (post_id, owner_id, parent_id, content, created_at) VALUES (?, ?, ?, ?, NOW())";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = dataSource.getInstance().getConnection();
            stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, comment.getPostId());
            stmt.setInt(2, comment.getOwnerId());
            if (comment.getParentId() != null) {
                stmt.setInt(3, comment.getParentId());
            } else {
                stmt.setNull(3, Types.INTEGER);
            }
            stmt.setString(4, comment.getContent());
            stmt.executeUpdate();
            
            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                comment.setId(rs.getInt(1));
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { /* ignore */ }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { /* ignore */ }
            if (conn != null) dataSource.getInstance().releaseConnection(conn);
        }
    }

    public Comment read(int id) throws SQLException {
        String query = "SELECT * FROM comment WHERE id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = dataSource.getInstance().getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, id);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return new Comment(
                    rs.getInt("id"),
                    rs.getInt("post_id"),
                    rs.getInt("owner_id"),
                    rs.getObject("parent_id", Integer.class),
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

    public List<Comment> readByPostId(int postId) throws SQLException {
        List<Comment> comments = new ArrayList<>();
        String query = "SELECT * FROM comment WHERE post_id = ? ORDER BY created_at ASC";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = dataSource.getInstance().getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, postId);
            rs = stmt.executeQuery();
            while (rs.next()) {
                comments.add(new Comment(
                    rs.getInt("id"),
                    rs.getInt("post_id"),
                    rs.getInt("owner_id"),
                    rs.getObject("parent_id", Integer.class),
                    rs.getString("content"),
                    rs.getTimestamp("created_at").toLocalDateTime()
                ));
            }
            return comments;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { /* ignore */ }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { /* ignore */ }
            if (conn != null) dataSource.getInstance().releaseConnection(conn);
        }
    }

    public void update(Comment comment) throws SQLException {
        String query = "UPDATE comment SET content = ? WHERE id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = dataSource.getInstance().getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, comment.getContent());
            stmt.setInt(2, comment.getId());
            stmt.executeUpdate();
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { /* ignore */ }
            if (conn != null) dataSource.getInstance().releaseConnection(conn);
        }
    }

    public void delete(int id) throws SQLException {
        String query = "DELETE FROM comment WHERE id = ?";
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