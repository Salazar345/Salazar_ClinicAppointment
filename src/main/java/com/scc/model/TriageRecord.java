package com.scc.model;

public record TriageRecord(int appointment_id, String blood_pressure, Double temperature,
                           Integer pulse_rate, Integer respiratory_rate, Double weight_kg,
                           Double height_cm, String notes, String triage_status,
                           Integer recorded_by, String updated_at) {
}