package com.scc.dao;

import com.scc.model.TriageRecord;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/** Vitals + triage status per appointment. Creates its own table (ClinicSchema is untouched). */
public class TriageDao {

    public TriageDao() {
        ClinicSchema.ensureTables();
        ensureTable();
    }

    private void ensureTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS triage_records (
                triage_id        INTEGER PRIMARY KEY AUTOINCREMENT,
                appointment_id   INTEGER NOT NULL UNIQUE,
                blood_pressure   TEXT,
                temperature      REAL,
                pulse_rate       INTEGER,
                respiratory_rate INTEGER,
                weight_kg        REAL,
                height_cm        REAL,
                notes            TEXT,
                triage_status    TEXT NOT NULL DEFAULT 'WAITING',
                recorded_by      INTEGER,
                updated_at       TEXT DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (appointment_id) REFERENCES appointments(appointment_id) ON DELETE CASCADE,
                FOREIGN KEY (recorded_by)    REFERENCES users(user_id) ON DELETE SET NULL
            )
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Error creating triage table: " + e.getMessage());
        }
    }

    public boolean saveVitals(int appointmentId, String bloodPressure, Double temperature,
                              Integer pulse, Integer respiratory, Double weight, Double height,
                              String notes, Integer recordedBy) {
        String sql = """
            INSERT INTO triage_records(appointment_id, blood_pressure, temperature, pulse_rate,
                respiratory_rate, weight_kg, height_cm, notes, triage_status, recorded_by)
            VALUES(?, ?, ?, ?, ?, ?, ?, ?, 'VITALS_TAKEN', ?)
            ON CONFLICT(appointment_id) DO UPDATE SET
                blood_pressure   = excluded.blood_pressure,
                temperature      = excluded.temperature,
                pulse_rate       = excluded.pulse_rate,
                respiratory_rate = excluded.respiratory_rate,
                weight_kg        = excluded.weight_kg,
                height_cm        = excluded.height_cm,
                notes            = excluded.notes,
                recorded_by      = excluded.recorded_by,
                updated_at       = CURRENT_TIMESTAMP,
                triage_status    = CASE WHEN triage_records.triage_status IN ('WAITING','IN_TRIAGE')
                                        THEN 'VITALS_TAKEN' ELSE triage_records.triage_status END
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, appointmentId);
            ps.setObject(2, bloodPressure);
            ps.setObject(3, temperature);
            ps.setObject(4, pulse);
            ps.setObject(5, respiratory);
            ps.setObject(6, weight);
            ps.setObject(7, height);
            ps.setObject(8, notes);
            ps.setObject(9, recordedBy);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Save vitals failed: " + e.getMessage());
            return false;
        }
    }

    public boolean setStatus(int appointmentId, String status, Integer recordedBy) {
        String sql = """
            INSERT INTO triage_records(appointment_id, triage_status, recorded_by) VALUES(?, ?, ?)
            ON CONFLICT(appointment_id) DO UPDATE SET
                triage_status = excluded.triage_status,
                recorded_by   = excluded.recorded_by,
                updated_at    = CURRENT_TIMESTAMP
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, appointmentId);
            ps.setString(2, status.toUpperCase());
            ps.setObject(3, recordedBy);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Set triage status failed: " + e.getMessage());
            return false;
        }
    }

    public TriageRecord getRecord(int appointmentId) {
        String sql = "SELECT * FROM triage_records WHERE appointment_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, appointmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Load triage record failed: " + e.getMessage());
        }
        return null;
    }

    /** Most recent triage record (by appointment date/time) of a patient, or null. */
    public TriageRecord getLatestForPatient(int patientId) {
        String sql = """
            SELECT t.* FROM triage_records t
            JOIN appointments a ON a.appointment_id = t.appointment_id
            WHERE a.patient_id = ? AND t.blood_pressure IS NOT NULL
            ORDER BY a.appointment_date DESC, a.appointment_time DESC LIMIT 1
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Load latest vitals failed: " + e.getMessage());
        }
        return null;
    }

    /** appointment_id -> triage_status for every appointment on the given date. */
    public Map<Integer, String> getStatusMap(String date) {
        Map<Integer, String> map = new HashMap<>();
        String sql = """
            SELECT t.appointment_id, t.triage_status FROM triage_records t
            JOIN appointments a ON a.appointment_id = t.appointment_id
            WHERE a.appointment_date = ?
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, date);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getInt(1), rs.getString(2));
                }
            }
        } catch (SQLException e) {
            System.err.println("Load triage statuses failed: " + e.getMessage());
        }
        return map;
    }

    private TriageRecord map(ResultSet rs) throws SQLException {
        return new TriageRecord(
                rs.getInt("appointment_id"),
                rs.getString("blood_pressure"),
                getDouble(rs, "temperature"),
                getInt(rs, "pulse_rate"),
                getInt(rs, "respiratory_rate"),
                getDouble(rs, "weight_kg"),
                getDouble(rs, "height_cm"),
                rs.getString("notes"),
                rs.getString("triage_status"),
                getInt(rs, "recorded_by"),
                rs.getString("updated_at"));
    }

    private Integer getInt(ResultSet rs, String col) throws SQLException {
        int v = rs.getInt(col);
        return rs.wasNull() ? null : v;
    }

    private Double getDouble(ResultSet rs, String col) throws SQLException {
        double v = rs.getDouble(col);
        return rs.wasNull() ? null : v;
    }
}