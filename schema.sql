-- Clinic Appointment System (CAS) - SQLite schema
-- Mirrors the tables created by DatabaseInitializer (users, activity_logs)
-- and ClinicSchema (patients, doctors, appointments).

PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS users (
                                     user_id      INTEGER PRIMARY KEY AUTOINCREMENT,
                                     full_name    TEXT NOT NULL,
                                     username     TEXT NOT NULL UNIQUE,
                                     password     TEXT NOT NULL,
                                     roles        TEXT NOT NULL,
                                     status       TEXT NOT NULL DEFAULT 'PENDING',
                                     date_created TEXT DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS activity_logs (
                                             log_id         INTEGER PRIMARY KEY AUTOINCREMENT,
                                             user_id        INTEGER,
                                             action         TEXT NOT NULL,
                                             table_affected TEXT,
                                             record_id      INTEGER,
                                             details        TEXT,
                                             log_timestamp  TEXT DEFAULT CURRENT_TIMESTAMP,
                                             FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL
    );

CREATE TABLE IF NOT EXISTS patients (
                                        patient_id      INTEGER PRIMARY KEY AUTOINCREMENT,
                                        full_name       TEXT NOT NULL,
                                        birth_date      TEXT,
                                        gender          TEXT,
                                        contact_number  TEXT,
                                        address         TEXT,
                                        date_registered TEXT DEFAULT CURRENT_TIMESTAMP
);

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
    );

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
    );