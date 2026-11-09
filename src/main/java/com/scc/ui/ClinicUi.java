package com.scc.ui;

import com.scc.model.Appointment;
import com.scc.model.Patient;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

/** Shared input + table helpers for the Doctor, Receptionist and Triage menus. */
public class ClinicUi {

    private final Scanner sc;

    public ClinicUi(Scanner sc) {
        this.sc = sc;
    }

    // ---------------------------------------------------------------- static helpers

    public static String cut(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 2) + "..";
    }

    public static String orNA(String s) {
        return s == null || s.isBlank() ? "N/A" : s;
    }

    public static String emptyToNull(String s) {
        return s == null || s.isBlank() || s.equals("N/A") ? null : s;
    }

    // ---------------------------------------------------------------- input

    /** Numeric ID (>= 0). 0 normally means cancel. */
    public int readId(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            try {
                int n = Integer.parseInt(input);
                if (n >= 0) return n;
            } catch (NumberFormatException ignored) {
            }
            System.out.println(" -> [ERROR] Please enter a valid numeric ID.");
        }
    }

    /** Returns YYYY-MM-DD, or null if the user typed 0. If blankIsToday, Enter = today. */
    public String readDate(String prompt, boolean blankIsToday) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            if (input.equals("0")) return null;
            if (input.isEmpty() && blankIsToday) return LocalDate.now().toString();
            try {
                return LocalDate.parse(input).toString();
            } catch (DateTimeParseException e) {
                System.out.println(" -> [ERROR] Invalid date. Use the format YYYY-MM-DD (e.g. 2026-11-05).");
            }
        }
    }

    /** Returns HH:mm, or null if the user typed 0. */
    public String readTime(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            if (input.equals("0")) return null;
            try {
                return LocalTime.parse(input).withSecond(0).withNano(0).toString();
            } catch (DateTimeParseException e) {
                System.out.println(" -> [ERROR] Invalid time. Use 24-hour HH:mm (e.g. 09:30 or 14:00).");
            }
        }
    }

    /** Optional birth date: Enter = skip (null). Must not be in the future. */
    public String readOptionalBirthDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            if (input.isEmpty()) return null;
            try {
                LocalDate d = LocalDate.parse(input);
                if (d.isAfter(LocalDate.now())) {
                    System.out.println(" -> [ERROR] Birth date cannot be in the future.");
                    continue;
                }
                return d.toString();
            } catch (DateTimeParseException e) {
                System.out.println(" -> [ERROR] Invalid date. Use the format YYYY-MM-DD (e.g. 1999-04-21).");
            }
        }
    }

    // ---------------------------------------------------------------- tables

    public void patientTable(List<Patient> patients, String title) {
        System.out.println("\n====================== " + title + " ======================");

        if (patients.isEmpty()) {
            System.out.println("\n-> No Records.");
            return;
        }

        String border = "+------+------------------------+------------+--------+----------------+--------------------------+";
        String format = "| %-4s | %-22s | %-10s | %-6s | %-14s | %-24s |\n";

        System.out.println();
        System.out.println(border);
        System.out.printf(format, "ID", "Full Name", "Birth Date", "Gender", "Contact", "Address");
        System.out.println(border);
        for (Patient p : patients) {
            System.out.printf(format, p.patient_id(), cut(p.full_name(), 22), orNA(p.birth_date()),
                    cut(orNA(p.gender()), 6), cut(orNA(p.contact_number()), 14), cut(orNA(p.address()), 24));
        }
        System.out.println(border);
        System.out.println(" Total: " + patients.size() + " record(s)");
    }

    public void appointmentTable(List<Appointment> list, String title) {
        System.out.println("\n====================== " + title + " ======================");

        if (list.isEmpty()) {
            System.out.println("\n-> No Records.");
            return;
        }

        String border = "+------+------------+-------+------------------+------------------+----------------------+-----------+";
        String format = "| %-4s | %-10s | %-5s | %-16s | %-16s | %-20s | %-9s |\n";

        System.out.println();
        System.out.println(border);
        System.out.printf(format, "ID", "Date", "Time", "Patient", "Doctor", "Reason", "Status");
        System.out.println(border);
        for (Appointment a : list) {
            System.out.printf(format, a.appointment_id(), a.appointment_date(), a.appointment_time(),
                    cut(a.patient_name(), 16), cut(a.doctor_name(), 16),
                    cut(orNA(a.reason()), 20), cut(a.status(), 9));
        }
        System.out.println(border);
        System.out.println(" Total: " + list.size() + " record(s)");
    }
}