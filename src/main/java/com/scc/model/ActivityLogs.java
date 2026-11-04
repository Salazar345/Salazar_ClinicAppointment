package com.scc.model;

public record ActivityLogs(int log_id, Integer user_id, String action, String table_affected,
                           Integer record_id, String details, String log_timestamp) {
    @Override
    public String toString() {
        return String.format("[%d] %s on %s (Record ID: %s) by %s at %s",
                log_id,
                action,
                table_affected != null ? table_affected : "N/A",
                record_id != null ? record_id : "N/A",
                user_id != null ? "User #" + user_id : "Unknown/Deleted User",
                log_timestamp);
    }
}