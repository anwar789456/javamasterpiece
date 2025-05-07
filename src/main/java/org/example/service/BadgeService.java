package org.example.service;

import org.example.models.Badge;
import utils.dataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BadgeService {
    /**
     * Add a badge for a user
     * @param userId The ID of the user
     * @param nbrStars The number of stars (1-5)
     * @return true if successful, false otherwise
     */
    public boolean addBadge(int userId, int nbrStars) {
        if (nbrStars < 1 || nbrStars > 5) {
            return false;
        }

        String typeBadge = nbrStars <= 3 ? "silver" : "gold";

        try {
            Connection conn = dataSource.getInstance().getConnection();
            String query = "INSERT INTO badges (user_id, typebadge, nbrstars) VALUES (?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setString(2, typeBadge);
            stmt.setInt(3, nbrStars);

            int rowsAffected = stmt.executeUpdate();
            stmt.close();
            conn.close();

            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get all badges for a user
     * @param userId The ID of the user
     * @return List of badges
     */
    public List<Badge> getUserBadges(int userId) {
        List<Badge> badges = new ArrayList<>();
        try {
            Connection conn = dataSource.getInstance().getConnection();
            String query = "SELECT * FROM badges WHERE user_id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Badge badge = new Badge(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    rs.getString("typebadge"),
                    rs.getInt("nbrstars")
                );
                badges.add(badge);
            }

            rs.close();
            stmt.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return badges;
    }

    /**
     * Get the average number of stars for a user
     * @param userId The ID of the user
     * @return Average number of stars
     */
    public double getUserAverageStars(int userId) {
        try {
            Connection conn = dataSource.getInstance().getConnection();
            String query = "SELECT AVG(nbrstars) as avg_stars FROM badges WHERE user_id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("avg_stars");
            }

            rs.close();
            stmt.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    /**
     * Add a badge for a user by their email
     * @param email The email of the user
     * @param nbrStars The number of stars (1-5)
     * @return true if successful, false otherwise
     */
    public boolean addBadgeByEmail(String email, int nbrStars) {
        if (nbrStars < 1 || nbrStars > 5) {
            return false;
        }

        try {
            Connection conn = dataSource.getInstance().getConnection();
            
            // First, get the user ID from the email
            String userIdQuery = "SELECT id FROM user WHERE email = ?";
            PreparedStatement userIdStmt = conn.prepareStatement(userIdQuery);
            userIdStmt.setString(1, email);
            
            ResultSet rs = userIdStmt.executeQuery();
            if (!rs.next()) {
                rs.close();
                userIdStmt.close();
                conn.close();
                return false; // User not found
            }
            
            int userId = rs.getInt("id");
            rs.close();
            userIdStmt.close();
            
            // Now add the badge
            String typeBadge = nbrStars <= 3 ? "silver" : "gold";
            String badgeQuery = "INSERT INTO badges (user_id, typebadge, nbrstars) VALUES (?, ?, ?)";
            PreparedStatement badgeStmt = conn.prepareStatement(badgeQuery);
            badgeStmt.setInt(1, userId);
            badgeStmt.setString(2, typeBadge);
            badgeStmt.setInt(3, nbrStars);

            int rowsAffected = badgeStmt.executeUpdate();
            badgeStmt.close();
            conn.close();

            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get all badges in the system with user emails
     * @return List of all badges
     */
    public List<Badge> getAllBadges() {
        List<Badge> badges = new ArrayList<>();
        try {
            Connection conn = dataSource.getInstance().getConnection();
            String query = "SELECT b.id, b.user_id, b.typebadge, b.nbrstars, b.created_at, u.email " +
                           "FROM badges b " +
                           "JOIN user u ON b.user_id = u.id " +
                           "ORDER BY b.id DESC";
            PreparedStatement stmt = conn.prepareStatement(query);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Badge badge = new Badge(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    rs.getString("typebadge"),
                    rs.getInt("nbrstars")
                );
                badge.setUserEmail(rs.getString("email"));
                
                // Set the date if available, otherwise use current date
                try {
                    if (rs.getTimestamp("created_at") != null) {
                        badge.setDateAwarded(rs.getTimestamp("created_at").toLocalDateTime());
                    }
                } catch (SQLException e) {
                    // If the 'created_at' column doesn't exist, just use the current date
                    System.out.println("Note: 'created_at' column not found in badges table");
                }
                
                badges.add(badge);
            }

            rs.close();
            stmt.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return badges;
    }
} 