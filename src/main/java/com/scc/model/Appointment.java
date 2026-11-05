package com.scc.model;

public record Appointment(int appointment_id, int patient_id, String patient_name,
                          int doctor_id, String doctor_name, String appointment_date,
                          String appointment_time, String reason, String status) {
    @Override
    public String toString() {
        return String.format("[%d] %s with %s on %s %s - %s", appointment_id, patient_name,
                doctor_name, appointment_date, appointment_time, status);
    }
}