package com.scc.model;

public record Doctor(int doctor_id, Integer user_id, String full_name, String specialization,
                     String contact_number, String available_days, String status) {
    @Override
    public String toString() {
        return String.format("[%d] %s - %s | Status: %s", doctor_id, full_name, specialization, status);
    }
}