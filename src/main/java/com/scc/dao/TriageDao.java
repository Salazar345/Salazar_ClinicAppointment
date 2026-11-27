package com.scc.dao;

import com.scc.model.TriageRecord;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * All SQL for the triage process. Creates its own table (CREATE TABLE IF NOT EXISTS) the
 * first time it is used, so DatabaseInitializer and ClinicSchema do not need to be touched.
 *
 * Priority: 1 = EMERGENCY, 2 = URGENT, 3 = LESS URGENT, 4 = NON-URGENT
 * Status:   WAITING -> IN_CONSULT -> DONE   (or CANCELLED)
 */
public class TriageDao {

    private static boolean ready = false;

    private static final String SELECT_JOIN = """
            SELECT t.*, p.full_name AS patient_name, d.full_name AS doctor_name
            FROM triage_assessments t
            JOIN patients p ON p.patient_id = t.patient_id
            LEFT JOIN doctors d ON d.doctor_id = t.doctor_id
            """;

    public TriageDao() {
        ClinicSchema.ensureTables();
        ensureTable();
    }

    private static synchronized void ensureTable() {
        if (ready) {
            return;
        }
        String sql = """
            CREATE TABLE IF NOT EXISTS triage_assessments (
                triage_id       INTEGER PRIMARY KEY AUTOINCREMENT,
                patient_id      INTEGER NOT NULL,
                assessed_by     INTEGER,
                doctor_id       INTEGER,
                chief_complaint TEXT NOT NULL,
                temperature     REAL,
                bp_systolic     INTEGER,
                bp_diastolic    INTEGER,
                heart_rate      INTEGER,
                resp_rate       INTEGER,
                spo2            INTEGER,
                pain_score      INTEGER,
                priority        INTEGER NOT NULL,
                status          TEXT NOT NULL DEFAULT 'WAITING',
                notes           TEXT,
                arrival_time    TEXT DEFAULT (datetime('now','localtime')),
                FOREIGN KEY (patient_id)  REFERENCES patients(patient_id),
                FOREIGN KEY (assessed_by) REFERENCES users(user_id) ON DELETE SET NULL,
                FOREIGN KEY (doctor_id)   REFERENCES doctors(doctor_id) ON DELETE SET NULL
            )
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement()) {
            st.execute(sql);
            ready = true;
        } catch (SQLException e) {
            System.err.println("Error creating triage table: " + e.getMessage());
        }
    }

    /** Returns the new triage_id, or -1 on failure. */
    public int addTriage(int patientId, int assessedBy, String complaint, Double temp,
                         Integer sys, Integer dia, Integer hr, Integer rr, Integer spo2,
                         Integer pain, int priority, String notes) {
        String sql = "INSERT INTO triage_assessments(patient_id, assessed_by, chief_complaint, temperature, "
                + "bp_systolic, bp_diastolic, heart_rate, resp_rate, spo2, pain_score, priority, notes) "
                + "VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, patientId);
            ps.setInt(2, assessedBy);
            ps.setString(3, complaint);
            setDouble(ps, 4, temp);
            setInt(ps, 5, sys);
            setInt(ps, 6, dia);
            setInt(ps, 7, hr);
            setInt(ps, 8, rr);
            setInt(ps, 9, spo2);
            setInt(ps, 10, pain);
            ps.setInt(11, priority);
            ps.setString(12, notes);
            if (ps.executeUpdate() > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Add triage failed: " + e.getMessage());
        }
        return -1;
    }

    /** Active queue (WAITING + IN_CONSULT): most urgent first, then first come first served. */
    public List<TriageRecord> getActiveQueue() {
        return query(" WHERE t.status IN ('WAITING','IN_CONSULT') "
                + "ORDER BY t.priority ASC, t.arrival_time ASC, t.triage_id ASC");
    }

    public List<TriageRecord> getByPatient(int patientId) {
        return query(" WHERE t.patient_id = ? ORDER BY t.triage_id DESC", patientId);
    }

    public List<TriageRecord> getToday() {
        return query(" WHERE date(t.arrival_time) = date('now','localtime') ORDER BY t.triage_id DESC");
    }

    public TriageRecord getById(int triageId) {
        List<TriageRecord> list = query(" WHERE t.triage_id = ?", triageId);
        return list.isEmpty() ? null : list.get(0);
    }

    /** True if this patient already has an open (WAITING / IN_CONSULT) triage entry. */
    public boolean hasActiveEntry(int patientId) {
        String sql = "SELECT 1 FROM triage_assessments WHERE patient_id = ? "
                + "AND status IN ('WAITING','IN_CONSULT') LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Check active triage failed: " + e.getMessage());
            return false;
        }
    }

    public boolean updatePriority(int triageId, int priority, String notes) {
        return exec("UPDATE triage_assessments SET priority = ?, notes = ? WHERE triage_id = ?",
                priority, notes, triageId);
    }

    /** Assigns a doctor; the patient stays in the queue as WAITING for that doctor. */
    public boolean assignDoctor(int triageId, int doctorId) {
        return exec("UPDATE triage_assessments SET doctor_id = ? WHERE triage_id = ?", doctorId, triageId);
    }

    public boolean updateStatus(int triageId, String status) {
        return exec("UPDATE triage_assessments SET status = ? WHERE triage_id = ?", status, triageId);
    }

    // ---------------------------------------------------------------- helpers

    private boolean exec(String sql, Object... params) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Triage update failed: " + e.getMessage());
            return false;
        }
    }

    private List<TriageRecord> query(String tail, Object... params) {
        List<TriageRecord> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_JOIN + tail)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Load triage records failed: " + e.getMessage());
        }
        return list;
    }

    private TriageRecord map(ResultSet rs) throws SQLException {
        return new TriageRecord(
                rs.getInt("triage_id"),
                rs.getInt("patient_id"),
                rs.getString("patient_name"),
                getInt(rs, "doctor_id"),
                rs.getString("doctor_name"),
                rs.getString("chief_complaint"),
                getDouble(rs, "temperature"),
                getInt(rs, "bp_systolic"),
                getInt(rs, "bp_diastolic"),
                getInt(rs, "heart_rate"),
                getInt(rs, "resp_rate"),
                getInt(rs, "spo2"),
                getInt(rs, "pain_score"),
                rs.getInt("priority"),
                rs.getString("status"),
                rs.getString("notes"),
                rs.getString("arrival_time")
        );
    }

    private static Integer getInt(ResultSet rs, String col) throws SQLException {
        int v = rs.getInt(col);
        return rs.wasNull() ? null : v;
    }

    private static Double getDouble(ResultSet rs, String col) throws SQLException {
        double v = rs.getDouble(col);
        return rs.wasNull() ? null : v;
    }

    private static void setInt(PreparedStatement ps, int i, Integer v) throws SQLException {
        if (v == null) ps.setNull(i, Types.INTEGER);
        else ps.setInt(i, v);
    }

    private static void setDouble(PreparedStatement ps, int i, Double v) throws SQLException {
        if (v == null) ps.setNull(i, Types.REAL);
        else ps.setDouble(i, v);
    }
}