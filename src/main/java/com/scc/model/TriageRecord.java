package com.scc.model;

public record TriageRecord(int triage_id, int patient_id, String patient_name, Integer doctor_id,
                           String doctor_name, String chief_complaint, Double temperature,
                           Integer bp_systolic, Integer bp_diastolic, Integer heart_rate,
                           Integer resp_rate, Integer spo2, Integer pain_score, int priority,
                           String status, String notes, String arrival_time) {

    public static String priorityLabel(int priority) {
        return switch (priority) {
            case 1 -> "EMERGENCY";
            case 2 -> "URGENT";
            case 3 -> "LESS URGENT";
            default -> "NON-URGENT";
        };
    }

    public String bloodPressure() {
        return bp_systolic == null || bp_diastolic == null ? "N/A" : bp_systolic + "/" + bp_diastolic;
    }

    @Override
    public String toString() {
        return String.format("[%d] %s - P%d %s | %s", triage_id, patient_name, priority,
                priorityLabel(priority), status);
    }
}