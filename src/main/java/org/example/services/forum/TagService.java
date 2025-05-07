package org.example.services.forum;

import org.example.models.forum.Tag;
import utils.dataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TagService {
    public void create(Tag tag) throws SQLException {
        String query = "INSERT INTO tag (name) VALUES (?)";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = dataSource.getInstance().getConnection();
            stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, tag.getName());
            stmt.executeUpdate();
            
            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                tag.setId(rs.getInt(1));
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { /* ignore */ }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { /* ignore */ }
            if (conn != null) dataSource.getInstance().releaseConnection(conn);
        }
    }

    public Tag read(int id) throws SQLException {
        String query = "SELECT * FROM tag WHERE id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = dataSource.getInstance().getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, id);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return new Tag(
                    rs.getInt("id"),
                    rs.getString("name")
                );
            }
            return null;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { /* ignore */ }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { /* ignore */ }
            if (conn != null) dataSource.getInstance().releaseConnection(conn);
        }
    }

    public List<Tag> readAll() throws SQLException {
        List<Tag> tags = new ArrayList<>();
        String query = "SELECT * FROM tag ORDER BY name ASC";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = dataSource.getInstance().getConnection();
            stmt = conn.prepareStatement(query);
            rs = stmt.executeQuery();
            while (rs.next()) {
                tags.add(new Tag(
                    rs.getInt("id"),
                    rs.getString("name")
                ));
            }
            return tags;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { /* ignore */ }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { /* ignore */ }
            if (conn != null) dataSource.getInstance().releaseConnection(conn);
        }
    }

    public void update(Tag tag) throws SQLException {
        String query = "UPDATE tag SET name = ? WHERE id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = dataSource.getInstance().getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, tag.getName());
            stmt.setInt(2, tag.getId());
            stmt.executeUpdate();
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { /* ignore */ }
            if (conn != null) dataSource.getInstance().releaseConnection(conn);
        }
    }

    public void delete(int id) throws SQLException {
        String query = "DELETE FROM tag WHERE id = ?";
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

    public void addTagToPost(int postId, int tagId) throws SQLException {
        String query = "INSERT INTO post_tag (post_id, tag_id) VALUES (?, ?)";
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = dataSource.getInstance().getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, postId);
            stmt.setInt(2, tagId);
            stmt.executeUpdate();
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { /* ignore */ }
            if (conn != null) dataSource.getInstance().releaseConnection(conn);
        }
    }

    public void removeTagFromPost(int postId, int tagId) throws SQLException {
        String query = "DELETE FROM post_tag WHERE post_id = ? AND tag_id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = dataSource.getInstance().getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, postId);
            stmt.setInt(2, tagId);
            stmt.executeUpdate();
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { /* ignore */ }
            if (conn != null) dataSource.getInstance().releaseConnection(conn);
        }
    }

    public List<Tag> getTagsForPost(int postId) throws SQLException {
        List<Tag> tags = new ArrayList<>();
        String query = "SELECT t.* FROM tag t JOIN post_tag pt ON t.id = pt.tag_id WHERE pt.post_id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = dataSource.getInstance().getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, postId);
            rs = stmt.executeQuery();
            while (rs.next()) {
                tags.add(new Tag(
                    rs.getInt("id"),
                    rs.getString("name")
                ));
            }
            return tags;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { /* ignore */ }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { /* ignore */ }
            if (conn != null) dataSource.getInstance().releaseConnection(conn);
        }
    }
    
    /**
     * Remove all tag associations for a post
     * @param postId The ID of the post
     * @throws SQLException if a database error occurs
     */
    public void removeAllTagsFromPost(int postId) throws SQLException {
        String query = "DELETE FROM post_tag WHERE post_id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = dataSource.getInstance().getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, postId);
            int count = stmt.executeUpdate();
            System.out.println("Removed " + count + " tag associations for post " + postId);
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { /* ignore */ }
            if (conn != null) dataSource.getInstance().releaseConnection(conn);
        }
    }
} 