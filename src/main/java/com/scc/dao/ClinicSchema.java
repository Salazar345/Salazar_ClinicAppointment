package com.scc.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Creates the doctors, patients and appointments tables if they do not exist yet.
 * Safe to call many times (CREATE TABLE IF NOT EXISTS). The new DAOs call this
 * in their constructors, so DatabaseInitializer does not need to be touched.
 */
public final class ClinicSchema {

    private static boolean ready = false;

    private ClinicSchema() {
    }

    public static synchronized void ensureTables() {
        if (ready) {
            return;
        }

        String patientsSql = """
            CREATE TABLE IF NOT EXISTS patients (
                patient_id      INTEGER PRIMARY KEY AUTOINCREMENT,
                full_name       TEXT NOT NULL,
                birth_date      TEXT,
                gender          TEXT,
                contact_number  TEXT,
                address         TEXT,
                date_registered TEXT DEFAULT CURRENT_TIMESTAMP
            )
            """;

        String doctorsSql = """
            CREATE TABLE IF NOT EXISTS doctors (
                doctor_id      INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id        INTEGER UNIQUE,
                full_name      TEXT NOT NULL,
                specialization TEXT NOT NULL,
                contact_number TEXT,
                available_days TEXT,
                status         TEXT NOT NULL DEFAULT 'ACTIVE',
                date_created   TEXT DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL
            )
            """;

        String appointmentsSql = """
            CREATE TABLE IF NOT EXISTS appointments (
                appointment_id   INTEGER PRIMARY KEY AUTOINCREMENT,
                patient_id       INTEGER NOT NULL,
                doctor_id        INTEGER NOT NULL,
                appointment_date TEXT NOT NULL,
                appointment_time TEXT NOT NULL,
                reason           TEXT,
                status           TEXT NOT NULL DEFAULT 'SCHEDULED',
                date_created     TEXT DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (patient_id) REFERENCES patients(patient_id),
                FOREIGN KEY (doctor_id)  REFERENCES doctors(doctor_id)
            )
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(patientsSql);
            stmt.execute(doctorsSql);
            stmt.execute(appointmentsSql);
            ready = true;
        } catch (SQLException e) {
            System.err.println("Error creating clinic tables: " + e.getMessage());
        }
    }
}