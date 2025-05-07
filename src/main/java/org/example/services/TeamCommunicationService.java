package org.example.services;

import org.example.model.TeamMember;
import org.example.model.TeamChatMessage;
import utils.dataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TeamCommunicationService {

    // Add a new team member
    public boolean addTeamMember(int projectId, int userId, String role) {
        String query = "INSERT INTO team_members (project_id, user_id, role, join_date) VALUES (?, ?, ?, ?)";
        try (Connection conn = dataSource.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, projectId);
            pstmt.setInt(2, userId);
            pstmt.setString(3, role);
            pstmt.setDate(4, new Date(System.currentTimeMillis()));

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Get all team members for a project
    public List<TeamMember> getTeamMembers(int projectId) {
        List<TeamMember> members = new ArrayList<>();
        String query = "SELECT * FROM team_members WHERE project_id = ?";

        try (Connection conn = dataSource.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, projectId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                members.add(new TeamMember(
                        rs.getInt("id"),
                        rs.getInt("project_id"),
                        rs.getInt("user_id"),
                        rs.getString("role"),
                        rs.getDate("join_date")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return members;
    }

    // Send a message in team chat
    public boolean sendMessage(int projectId, int senderId, String message, String messageType, String filePath) {
        String query = "INSERT INTO team_chat (project_id, sender_id, message, message_type, file_path) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, projectId);
            pstmt.setInt(2, senderId);
            pstmt.setString(3, message);
            pstmt.setString(4, messageType);
            pstmt.setString(5, filePath);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Get chat messages for a project
    public List<TeamChatMessage> getChatMessages(int projectId) {
        List<TeamChatMessage> messages = new ArrayList<>();
        String query = "SELECT * FROM team_chat WHERE project_id = ? ORDER BY timestamp DESC";

        try (Connection conn = dataSource.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, projectId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                messages.add(new TeamChatMessage(
                        rs.getInt("id"),
                        rs.getInt("project_id"),
                        rs.getInt("sender_id"),
                        rs.getString("message"),
                        rs.getTimestamp("timestamp"),
                        rs.getString("message_type"),
                        rs.getString("file_path")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }

    // Update last read timestamp for a user in a chat
    public boolean updateLastRead(int chatId, int userId) {
        String query = "INSERT INTO team_chat_participants (chat_id, user_id, last_read) " +
                "VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE last_read = ?";

        try (Connection conn = dataSource.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            Timestamp now = new Timestamp(System.currentTimeMillis());
            pstmt.setInt(1, chatId);
            pstmt.setInt(2, userId);
            pstmt.setTimestamp(3, now);
            pstmt.setTimestamp(4, now);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}