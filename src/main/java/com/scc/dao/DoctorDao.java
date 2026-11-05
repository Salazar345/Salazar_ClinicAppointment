package com.scc.dao;

import com.scc.model.Doctor;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DoctorDao {

    public DoctorDao() {
        ClinicSchema.ensureTables();
    }

    public List<Doctor> getAllDoctors() {
        List<Doctor> list = new ArrayList<>();
        String sql = "SELECT * FROM doctors ORDER BY doctor_id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapDoctor(rs));
            }
        } catch (SQLException e) {
            System.err.println("Load doctors failed: " + e.getMessage());
        }
        return list;
    }

    public List<Doctor> searchDoctors(String keyword) {
        List<Doctor> list = new ArrayList<>();
        if (keyword == null || keyword.isBlank()) {
            return list;
        }
        String sql = "SELECT * FROM doctors WHERE full_name LIKE ? OR specialization LIKE ? "
                + "OR contact_number LIKE ? OR CAST(doctor_id AS TEXT) = ? ORDER BY doctor_id";
        String like = "%" + keyword.trim() + "%";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, keyword.trim());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapDoctor(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Search doctors failed: " + e.getMessage());
        }
        return list;
    }

    public Doctor getDoctorById(int doctorId) {
        String sql = "SELECT * FROM doctors WHERE doctor_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapDoctor(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lookup doctor failed: " + e.getMessage());
        }
        return null;
    }

    public Doctor getDoctorByUserId(int userId) {
        String sql = "SELECT * FROM doctors WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapDoctor(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lookup doctor by user failed: " + e.getMessage());
        }
        return null;
    }

    /** Returns the new doctor_id, or -1 if it failed. */
    public int addDoctor(Integer userId, String fullName, String specialization,
                         String contactNumber, String availableDays, String status) {
        String sql = "INSERT INTO doctors(user_id, full_name, specialization, contact_number, "
                + "available_days, status) VALUES(?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (userId != null) {
                ps.setInt(1, userId);
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setString(2, fullName);
            ps.setString(3, specialization);
            ps.setString(4, contactNumber);
            ps.setString(5, availableDays);
            ps.setString(6, status.toUpperCase());

            if (ps.executeUpdate() > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Add doctor failed: " + e.getMessage());
        }
        return -1;
    }

    public boolean updateDoctor(int doctorId, Integer userId, String fullName, String specialization,
                                String contactNumber, String availableDays, String status) {
        String sql = "UPDATE doctors SET user_id = ?, full_name = ?, specialization = ?, "
                + "contact_number = ?, available_days = ?, status = ? WHERE doctor_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (userId != null) {
                ps.setInt(1, userId);
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setString(2, fullName);
            ps.setString(3, specialization);
            ps.setString(4, contactNumber);
            ps.setString(5, availableDays);
            ps.setString(6, status.toUpperCase());
            ps.setInt(7, doctorId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Update doctor failed: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteDoctor(int doctorId) {
        String sql = "DELETE FROM doctors WHERE doctor_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Delete doctor failed: " + e.getMessage());
            return false;
        }
    }

    public int countAppointments(int doctorId) {
        String sql = "SELECT COUNT(*) FROM appointments WHERE doctor_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Count appointments failed: " + e.getMessage());
        }
        return 0;
    }

    private Doctor mapDoctor(ResultSet rs) throws SQLException {
        int rawUserId = rs.getInt("user_id");
        Integer userId = rs.wasNull() ? null : rawUserId;

        return new Doctor(
                rs.getInt("doctor_id"),
                userId,
                rs.getString("full_name"),
                rs.getString("specialization"),
                rs.getString("contact_number"),
                rs.getString("available_days"),
                rs.getString("status")
        );
    }
}