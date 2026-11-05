package com.scc.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReportDao {

    public ReportDao() {
        ClinicSchema.ensureTables();
    }

    public Map<String, Integer> usersByRole() {
        return groupCount("SELECT roles, COUNT(*) FROM users GROUP BY roles ORDER BY roles");
    }

    public Map<String, Integer> usersByStatus() {
        return groupCount("SELECT status, COUNT(*) FROM users GROUP BY status ORDER BY status");
    }

    public Map<String, Integer> doctorsByStatus() {
        return groupCount("SELECT status, COUNT(*) FROM doctors GROUP BY status ORDER BY status");
    }

    public Map<String, Integer> appointmentsByStatus() {
        return groupCount("SELECT status, COUNT(*) FROM appointments GROUP BY status ORDER BY status");
    }

    public int countPatients() {
        return scalar("SELECT COUNT(*) FROM patients");
    }

    public int countAppointments() {
        return scalar("SELECT COUNT(*) FROM appointments");
    }

    public int countAppointmentsOn(String date) {
        return scalar("SELECT COUNT(*) FROM appointments WHERE appointment_date = ?", date);
    }

    public int countUpcoming(String fromDate) {
        return scalar("SELECT COUNT(*) FROM appointments WHERE status = 'SCHEDULED' "
                + "AND appointment_date >= ?", fromDate);
    }

    /** Rows: doctor name, specialization, total, scheduled, completed, cancelled, other. */
    public List<String[]> appointmentsPerDoctor() {
        String sql = """
                SELECT d.full_name, d.specialization,
                       COUNT(a.appointment_id),
                       COALESCE(SUM(CASE WHEN a.status = 'SCHEDULED' THEN 1 ELSE 0 END), 0),
                       COALESCE(SUM(CASE WHEN a.status = 'COMPLETED' THEN 1 ELSE 0 END), 0),
                       COALESCE(SUM(CASE WHEN a.status = 'CANCELLED' THEN 1 ELSE 0 END), 0)
                FROM doctors d
                LEFT JOIN appointments a ON a.doctor_id = d.doctor_id
                GROUP BY d.doctor_id
                ORDER BY COUNT(a.appointment_id) DESC, d.full_name
                """;
        return statRows(sql, 2);
    }

    /** Rows: date, total, scheduled, completed, cancelled, other. */
    public List<String[]> appointmentsPerDay(String fromDate, String toDate) {
        String sql = """
                SELECT appointment_date,
                       COUNT(*),
                       SUM(CASE WHEN status = 'SCHEDULED' THEN 1 ELSE 0 END),
                       SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END),
                       SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END)
                FROM appointments
                WHERE appointment_date BETWEEN ? AND ?
                GROUP BY appointment_date
                ORDER BY appointment_date
                """;
        return statRows(sql, 1, fromDate, toDate);
    }

    /**
     * Reads rows where the first 'textCols' columns are text and the next four are
     * total/scheduled/completed/cancelled. Adds an 'other' column (total - the three known).
     */
    private List<String[]> statRows(String sql, int textCols, Object... params) {
        List<String[]> rows = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String[] row = new String[textCols + 5];
                    for (int i = 0; i < textCols; i++) {
                        row[i] = rs.getString(i + 1);
                    }
                    int total = rs.getInt(textCols + 1);
                    int scheduled = rs.getInt(textCols + 2);
                    int completed = rs.getInt(textCols + 3);
                    int cancelled = rs.getInt(textCols + 4);
                    row[textCols] = String.valueOf(total);
                    row[textCols + 1] = String.valueOf(scheduled);
                    row[textCols + 2] = String.valueOf(completed);
                    row[textCols + 3] = String.valueOf(cancelled);
                    row[textCols + 4] = String.valueOf(total - scheduled - completed - cancelled);
                    rows.add(row);
                }
            }
        } catch (SQLException e) {
            System.err.println("Load report failed: " + e.getMessage());
        }
        return rows;
    }

    private Map<String, Integer> groupCount(String sql) {
        Map<String, Integer> map = new LinkedHashMap<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString(1), rs.getInt(2));
            }
        } catch (SQLException e) {
            System.err.println("Load report failed: " + e.getMessage());
        }
        return map;
    }

    private int scalar(String sql, Object... params) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Load report failed: " + e.getMessage());
        }
        return 0;
    }
}