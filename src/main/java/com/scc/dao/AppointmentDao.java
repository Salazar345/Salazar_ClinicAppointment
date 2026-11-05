package com.scc.dao;

import com.scc.model.Appointment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AppointmentDao {

    private static final String SELECT_JOIN = """
            SELECT a.appointment_id, a.patient_id, p.full_name AS patient_name,
                   a.doctor_id, d.full_name AS doctor_name,
                   a.appointment_date, a.appointment_time, a.reason, a.status
            FROM appointments a
            JOIN patients p ON p.patient_id = a.patient_id
            JOIN doctors  d ON d.doctor_id  = a.doctor_id
            """;

    private static final String ORDER = " ORDER BY a.appointment_date DESC, a.appointment_time DESC, a.appointment_id DESC";

    public AppointmentDao() {
        ClinicSchema.ensureTables();
    }

    public List<Appointment> getAllAppointments() {
        return query("");
    }

    public List<Appointment> getByPatient(int patientId) {
        return query(" WHERE a.patient_id = ?", patientId);
    }

    public List<Appointment> getByDoctor(int doctorId) {
        return query(" WHERE a.doctor_id = ?", doctorId);
    }

    public List<Appointment> getByStatus(String status) {
        return query(" WHERE a.status = ?", status.toUpperCase());
    }

    public List<Appointment> getByDate(String date) {
        return query(" WHERE a.appointment_date = ?", date);
    }

    public List<Appointment> getByDateRange(String fromDate, String toDate) {
        return query(" WHERE a.appointment_date BETWEEN ? AND ?", fromDate, toDate);
    }

    private List<Appointment> query(String where, Object... params) {
        List<Appointment> list = new ArrayList<>();
        String sql = SELECT_JOIN + where + ORDER;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapAppointment(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Load appointments failed: " + e.getMessage());
        }
        return list;
    }

    private Appointment mapAppointment(ResultSet rs) throws SQLException {
        return new Appointment(
                rs.getInt("appointment_id"),
                rs.getInt("patient_id"),
                rs.getString("patient_name"),
                rs.getInt("doctor_id"),
                rs.getString("doctor_name"),
                rs.getString("appointment_date"),
                rs.getString("appointment_time"),
                rs.getString("reason"),
                rs.getString("status")
        );
    }
}