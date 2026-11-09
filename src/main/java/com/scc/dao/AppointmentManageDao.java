package com.scc.dao;

import com.scc.model.Appointment;

import java.sql.*;

/** Write operations for appointments (AppointmentDao is read-only, so this extends it). */
public class AppointmentManageDao extends AppointmentDao {

    private final ActivityLogDao logDao = new ActivityLogDao();

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

    /**
     * CORE BUSINESS PROCESS: books an appointment and writes the activity log.
     * Returns the new appointment_id, or -1 if it failed.
     */
    public int book(int patientId, int doctorId, String date, String time, String reason,
                    Integer actingUserId) {
        String sql = "INSERT INTO appointments(patient_id, doctor_id, appointment_date, "
                + "appointment_time, reason, status) VALUES(?, ?, ?, ?, ?, 'SCHEDULED')";
        int newId = -1;

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
                        newId = keys.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Book appointment failed: " + e.getMessage());
        }

        if (newId > 0) {
            Appointment a = getById(newId);
            String patient = a != null ? a.patient_name() : "Patient #" + patientId;
            String doctor = a != null ? a.doctor_name() : "Doctor #" + doctorId;
            logDao.log(actingUserId, "CREATE", "appointments", newId,
                    "Booked appointment #" + newId + " for " + patient + " with Dr. " + doctor
                            + " on " + date + " " + time);
        }
        return newId;
    }

    /** LIFECYCLE CHANGE: reschedule + activity log. */
    public boolean reschedule(int appointmentId, String date, String time, Integer actingUserId) {
        Appointment before = getById(appointmentId);
        String sql = "UPDATE appointments SET appointment_date = ?, appointment_time = ? "
                + "WHERE appointment_id = ? AND status = 'SCHEDULED'";
        boolean ok = false;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, date);
            ps.setString(2, time);
            ps.setInt(3, appointmentId);
            ok = ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Reschedule failed: " + e.getMessage());
        }

        if (ok) {
            String from = before != null
                    ? before.appointment_date() + " " + before.appointment_time() : "N/A";
            String who = before != null ? " (" + before.patient_name() + ")" : "";
            logDao.log(actingUserId, "UPDATE", "appointments", appointmentId,
                    "Rescheduled appointment #" + appointmentId + who + " from " + from
                            + " to " + date + " " + time);
        }
        return ok;
    }

    /** LIFECYCLE CHANGE: status edit (COMPLETED / CANCELLED ...) + activity log. */
    public boolean updateStatus(int appointmentId, String status, Integer actingUserId) {
        Appointment before = getById(appointmentId);
        String newStatus = status.toUpperCase();
        String sql = "UPDATE appointments SET status = ? WHERE appointment_id = ?";
        boolean ok = false;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, appointmentId);
            ok = ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Update appointment status failed: " + e.getMessage());
        }

        if (ok) {
            String oldStatus = before != null ? before.status() : "UNKNOWN";
            String who = before != null ? " (" + before.patient_name() + ")" : "";
            logDao.log(actingUserId, "UPDATE", "appointments", appointmentId,
                    "Updated appointment #" + appointmentId + who + " status from "
                            + oldStatus + " to " + newStatus);
        }
        return ok;
    }
}
