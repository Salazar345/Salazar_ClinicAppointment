package com.scc.dao;

import com.scc.model.ActivityLogs;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogDao {

    public boolean log(Integer userId, String action, String details) {
        return log(userId, action, null, null, details);
    }

    public boolean log(Integer userId, String action, String tableAffected, Integer recordId, String details) {
        if (action == null || action.isBlank()) {
            System.err.println("Failed to write activity log: action is required");
            return false;
        }

        String sql = "INSERT INTO activity_logs(user_id, action, table_affected, record_id, details) "
                + "VALUES(?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (userId != null) {
                ps.setInt(1, userId);
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setString(2, action.toUpperCase());
            ps.setString(3, tableAffected);
            if (recordId != null) {
                ps.setInt(4, recordId);
            } else {
                ps.setNull(4, Types.INTEGER);
            }
            ps.setString(5, details);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Failed to write activity log: " + e.getMessage());
            return false;
        }
    }

    public List<ActivityLogs> getAllLogs() {
        List<ActivityLogs> list = new ArrayList<>();
        String sql = "SELECT * FROM activity_logs ORDER BY log_id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapLog(rs));
            }
        } catch (SQLException e) {
            System.err.println("Load activity logs failed: " + e.getMessage());
        }
        return list;
    }

    public List<ActivityLogs> getLogsByAction(String action) {
        List<ActivityLogs> list = new ArrayList<>();
        if (action == null) {
            return list;
        }
        String sql = "SELECT * FROM activity_logs WHERE action = ? ORDER BY log_id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, action.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapLog(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Load logs by action failed: " + e.getMessage());
        }
        return list;
    }

    public List<ActivityLogs> getLogsByTable(String tableAffected) {
        List<ActivityLogs> list = new ArrayList<>();
        String sql = "SELECT * FROM activity_logs WHERE table_affected = ? ORDER BY log_id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableAffected);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapLog(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Load logs by table failed: " + e.getMessage());
        }
        return list;
    }

    public List<ActivityLogs> getRecentLogs(int limit) {
        List<ActivityLogs> list = new ArrayList<>();
        if (limit <= 0) {
            return list;
        }
        String sql = "SELECT * FROM activity_logs ORDER BY log_id DESC LIMIT ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapLog(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Load recent logs failed: " + e.getMessage());
        }
        return list;
    }

    private ActivityLogs mapLog(ResultSet rs) throws SQLException {
        int rawUserId = rs.getInt("user_id");
        Integer userId = rs.wasNull() ? null : rawUserId;

        int rawRecordId = rs.getInt("record_id");
        Integer recordId = rs.wasNull() ? null : rawRecordId;

        return new ActivityLogs(
                rs.getInt("log_id"),
                userId,
                rs.getString("action"),
                rs.getString("table_affected"),
                recordId,
                rs.getString("details"),
                rs.getString("log_timestamp")
        );
    }
}