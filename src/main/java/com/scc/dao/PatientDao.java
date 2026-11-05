package com.scc.dao;

import com.scc.model.Patient;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PatientDao {

    public PatientDao() {
        ClinicSchema.ensureTables();
    }

    public List<Patient> getAllPatients() {
        List<Patient> list = new ArrayList<>();
        String sql = "SELECT * FROM patients ORDER BY patient_id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapPatient(rs));
            }
        } catch (SQLException e) {
            System.err.println("Load patients failed: " + e.getMessage());
        }
        return list;
    }

    public List<Patient> searchPatients(String keyword) {
        List<Patient> list = new ArrayList<>();
        if (keyword == null || keyword.isBlank()) {
            return list;
        }
        String sql = "SELECT * FROM patients WHERE full_name LIKE ? OR contact_number LIKE ? "
                + "OR CAST(patient_id AS TEXT) = ? ORDER BY patient_id";
        String like = "%" + keyword.trim() + "%";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, keyword.trim());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapPatient(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Search patients failed: " + e.getMessage());
        }
        return list;
    }

    public Patient getPatientById(int patientId) {
        String sql = "SELECT * FROM patients WHERE patient_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapPatient(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lookup patient failed: " + e.getMessage());
        }
        return null;
    }

    private Patient mapPatient(ResultSet rs) throws SQLException {
        return new Patient(
                rs.getInt("patient_id"),
                rs.getString("full_name"),
                rs.getString("birth_date"),
                rs.getString("gender"),
                rs.getString("contact_number"),
                rs.getString("address"),
                rs.getString("date_registered")
        );
    }
}