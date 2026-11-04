package com.scc.model;

public record ActivityLogs(int log_id, int user_id, String action, String table_affected,
                          Integer record_id, String details, String log_timestamp) {
    @Override
    public String toString() {
        return String.format("[%d] %s on %s (Record ID: %s) by User #%d at %s", log_id, action, table_affected, record_id != null ? record_id : "N/A", user_id, log_timestamp);
    }
}