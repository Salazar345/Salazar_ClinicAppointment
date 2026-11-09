package com.scc.dao;

import java.sql.*;

/** Write operations for patients (PatientDao is read-only, so this extends it). */
public class PatientManageDao extends PatientDao {

    private final ActivityLogDao logDao = new ActivityLogDao();

    /** CORE BUSINESS PROCESS (intake). Returns the new patient_id, or -1 if it failed. */
    public int addPatient(String fullName, String birthDate, String gender,
                          String contactNumber, String address, Integer actingUserId) {
        String sql = "INSERT INTO patients(full_name, birth_date, gender, contact_number, address) "
                + "VALUES(?, ?, ?, ?, ?)";
        int newId = -1;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, fullName);
            ps.setString(2, birthDate);
            ps.setString(3, gender);
            ps.setString(4, contactNumber);
            ps.setString(5, address);
            if (ps.executeUpdate() > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        newId = keys.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Add patient failed: " + e.getMessage());
        }

        if (newId > 0) {
            logDao.log(actingUserId, "CREATE", "patients", newId,
                    "Registered new patient #" + newId + ": " + fullName);
        }
        return newId;
    }

    public boolean updatePatient(int patientId, String fullName, String birthDate, String gender,
                                 String contactNumber, String address, Integer actingUserId) {
        String sql = "UPDATE patients SET full_name = ?, birth_date = ?, gender = ?, "
                + "contact_number = ?, address = ? WHERE patient_id = ?";
        boolean ok = false;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, birthDate);
            ps.setString(3, gender);
            ps.setString(4, contactNumber);
            ps.setString(5, address);
            ps.setInt(6, patientId);
            ok = ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Update patient failed: " + e.getMessage());
        }

        if (ok) {
            logDao.log(actingUserId, "UPDATE", "patients", patientId,
                    "Updated patient #" + patientId + " record: " + fullName);
        }
        return ok;
    }
}
