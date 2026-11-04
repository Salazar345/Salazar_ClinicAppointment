package com.scc.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {
    public static void initialize() {
        String sql = """
            CREATE TABLE IF NOT EXISTS users (
                user_id      INTEGER PRIMARY KEY AUTOINCREMENT,
                full_name    TEXT NOT NULL,
                username     TEXT NOT NULL UNIQUE,
                password     TEXT NOT NULL,
                roles        TEXT NOT NULL,
                status       TEXT NOT NULL DEFAULT 'PENDING',
                date_created TEXT DEFAULT CURRENT_TIMESTAMP
            )
            """;

        String logsSql = """
            CREATE TABLE IF NOT EXISTS activity_logs (
                log_id         INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id        INTEGER,
                action         TEXT NOT NULL,
                table_affected TEXT,
                record_id      INTEGER,
                details        TEXT,
                log_timestamp  TEXT DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL
            )
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            stmt.execute(logsSql);
            System.out.println("-> Database tables verified.");
        } catch (SQLException e) {
            System.err.println("Error creating tables: " + e.getMessage());
        }
    }
}