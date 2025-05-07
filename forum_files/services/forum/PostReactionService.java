package org.example.services.forum;

import org.example.models.forum.PostReaction;
import utils.dataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PostReactionService {
    public void create(PostReaction reaction) throws SQLException {
        String query = "INSERT INTO post_reaction (post_id, user_id, is_like) VALUES (?, ?, ?)";
        try (Connection conn = dataSource.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, reaction.getPostId());
            stmt.setInt(2, reaction.getUserId());
            stmt.setBoolean(3, reaction.isLike());
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    reaction.setId(rs.getInt(1));
                }
            }
        }
    }

    public PostReaction read(int id) throws SQLException {
        String query = "SELECT * FROM post_reaction WHERE id = ?";
        try (Connection conn = dataSource.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new PostReaction(
                        rs.getInt("id"),
                        rs.getInt("post_id"),
                        rs.getInt("user_id"),
                        rs.getBoolean("is_like"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                    );
                }
            }
        }
        return null;
    }

    public List<PostReaction> readByPostId(int postId) throws SQLException {
        List<PostReaction> reactions = new ArrayList<>();
        String query = "SELECT * FROM post_reaction WHERE post_id = ? ORDER BY created_at DESC";
        try (Connection conn = dataSource.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, postId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reactions.add(new PostReaction(
                        rs.getInt("id"),
                        rs.getInt("post_id"),
                        rs.getInt("user_id"),
                        rs.getBoolean("is_like"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                    ));
                }
            }
        }
        return reactions;
    }

    public void update(PostReaction reaction) throws SQLException {
        String query = "UPDATE post_reaction SET is_like = ? WHERE id = ?";
        try (Connection conn = dataSource.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setBoolean(1, reaction.isLike());
            stmt.setInt(2, reaction.getId());
            stmt.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String query = "DELETE FROM post_reaction WHERE id = ?";
        try (Connection conn = dataSource.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    public void toggleReaction(int postId, int userId, boolean isLike) throws SQLException {
        String query = "INSERT INTO post_reaction (post_id, user_id, is_like) VALUES (?, ?, ?) " +
                      "ON DUPLICATE KEY UPDATE is_like = ?";
        try (Connection conn = dataSource.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, postId);
            stmt.setInt(2, userId);
            stmt.setBoolean(3, isLike);
            stmt.setBoolean(4, isLike);
            stmt.executeUpdate();
        }
    }

    public int getLikeCount(int postId) throws SQLException {
        String query = "SELECT COUNT(*) FROM post_reaction WHERE post_id = ? AND is_like = true";
        try (Connection conn = dataSource.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, postId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public int getDislikeCount(int postId) throws SQLException {
        String query = "SELECT COUNT(*) FROM post_reaction WHERE post_id = ? AND is_like = false";
        try (Connection conn = dataSource.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, postId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }
} 