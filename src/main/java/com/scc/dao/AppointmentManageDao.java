package com.scc.dao;

import com.scc.model.Appointment;

import java.sql.*;

/** Write operations for appointments (AppointmentDao is read-only, so this extends it). */
public class AppointmentManageDao extends AppointmentDao {

    public Appointment getById(int appointmentId) {
        String sql = """
                SELECT a.appointment_id, a.patient_id, p.full_name AS patient_name,
                       a.doctor_id, d.full_name AS doctor_name,
                       a.appointment_date, a.appointment_time, a.reason, a.status
                FROM appointments a
                JOIN patients p ON p.patient_id = a.patient_id
                JOIN doctors  d ON d.doctor_id  = a.doctor_id
                WHERE a.appointment_id = ?
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, appointmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Appointment(
                            rs.getInt("appointment_id"), rs.getInt("patient_id"),
                            rs.getString("patient_name"), rs.getInt("doctor_id"),
                            rs.getString("doctor_name"), rs.getString("appointment_date"),
                            rs.getString("appointment_time"), rs.getString("reason"),
                            rs.getString("status"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lookup appointment failed: " + e.getMessage());
        }
        return null;
    }

    /** True if the doctor already has a SCHEDULED appointment at that date/time. */
    public boolean hasConflict(int doctorId, String date, String time, int excludeAppointmentId) {
        String sql = "SELECT COUNT(*) FROM appointments WHERE doctor_id = ? AND appointment_date = ? "
                + "AND appointment_time = ? AND status = 'SCHEDULED' AND appointment_id <> ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            ps.setString(2, date);
            ps.setString(3, time);
            ps.setInt(4, excludeAppointmentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Conflict check failed: " + e.getMessage());
            return true; // be safe: block the booking if we cannot verify
        }
    }

    /** Returns the new appointment_id, or -1 if it failed. */
    public int book(int patientId, int doctorId, String date, String time, String reason) {
        String sql = "INSERT INTO appointments(patient_id, doctor_id, appointment_date, "
                + "appointment_time, reason, status) VALUES(?, ?, ?, ?, ?, 'SCHEDULED')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, patientId);
            ps.setInt(2, doctorId);
            ps.setString(3, date);
            ps.setString(4, time);
            ps.setString(5, reason);
            if (ps.executeUpdate() > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Book appointment failed: " + e.getMessage());
        }
        return -1;
    }

    public boolean reschedule(int appointmentId, String date, String time) {
        String sql = "UPDATE appointments SET appointment_date = ?, appointment_time = ? "
                + "WHERE appointment_id = ? AND status = 'SCHEDULED'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, date);
            ps.setString(2, time);
            ps.setInt(3, appointmentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Reschedule failed: " + e.getMessage());
            return false;
        }
    }

    public boolean updateStatus(int appointmentId, String status) {
        String sql = "UPDATE appointments SET status = ? WHERE appointment_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.toUpperCase());
            ps.setInt(2, appointmentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Update appointment status failed: " + e.getMessage());
            return false;
        }
    }
}