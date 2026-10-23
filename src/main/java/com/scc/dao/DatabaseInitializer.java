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

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("-> Database tables verified.");
        } catch (SQLException e) {
            System.err.println("Error creating tables: " + e.getMessage());
        }
    }
}