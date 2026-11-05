package com.scc.model;

public record Patient(int patient_id, String full_name, String birth_date, String gender,
                      String contact_number, String address, String date_registered) {
    @Override
    public String toString() {
        return String.format("[%d] %s (%s)", patient_id, full_name, gender != null ? gender : "N/A");
    }
}