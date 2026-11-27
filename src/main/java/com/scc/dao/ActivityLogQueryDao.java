package com.scc.dao;

import com.scc.model.ActivityLogs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Extra audit-log queries. Extends ActivityLogDao so the existing class stays untouched.
 * Adds: filter by User ID (the existing DAO only filters by action / table).
 */
public class ActivityLogQueryDao extends ActivityLogDao {

    public List<ActivityLogs> getLogsByUserId(int userId) {
        List<ActivityLogs> list = new ArrayList<>();
        String sql = "SELECT * FROM activity_logs WHERE user_id = ? ORDER BY log_id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Load logs by user failed: " + e.getMessage());
        }
        return list;
    }

    private ActivityLogs map(ResultSet rs) throws SQLException {
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
                rs.getString("log_timestamp"));
    }
}