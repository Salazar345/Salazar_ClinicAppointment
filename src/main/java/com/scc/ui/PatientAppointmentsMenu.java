package com.scc.ui;

import com.scc.dao.AppointmentDao;
import com.scc.dao.DoctorDao;
import com.scc.dao.PatientDao;
import com.scc.model.Appointment;
import com.scc.model.Doctor;
import com.scc.model.Patient;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

/** Admin > View All Patients and Appointments (read-only). */
public class PatientAppointmentsMenu {

    private final PatientDao patientDao = new PatientDao();
    private final AppointmentDao appointmentDao = new AppointmentDao();
    private final DoctorDao doctorDao = new DoctorDao();
    private final Scanner sc;

    public PatientAppointmentsMenu(Scanner sc) {
        this.sc = sc;
    }

    public void show() {
        while (true) {
            System.out.println("\n[1] View All Patients\n[2] Search Patient\n[3] View Patient Details and Appointments" +
                    "\n[4] View All Appointments\n[5] Filter Appointments\n[0] Back");
            System.out.print("Enter Choice: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> displayPatientTable(patientDao.getAllPatients(), "ALL PATIENTS");
                case "2" -> searchPatients();
                case "3" -> viewPatientDetails();
                case "4" -> displayAppointmentTable(appointmentDao.getAllAppointments(), "ALL APPOINTMENTS");
                case "5" -> filterAppointments();
                case "0" -> {
                    return;
                }
                default -> System.out.println("-> Invalid choice! 0 to 5 only.");
            }
        }
    }

    // ---------------------------------------------------------------- patients

    private void displayPatientTable(List<Patient> patients, String title) {
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

    private void searchPatients() {
        System.out.print("Enter name / contact / ID (0 to cancel): ");
        String keyword = sc.nextLine().trim();

        if (keyword.equals("0") || keyword.isEmpty()) {
            System.out.println(" -> Cancelled.");
            return;
        }
        displayPatientTable(patientDao.searchPatients(keyword), "SEARCH: " + keyword);
    }

    private void viewPatientDetails() {
        int patientId = readInput("Enter Patient ID (0 to cancel): ");

        if (patientId == 0) {
            System.out.println(" -> Cancelled.");
            return;
        }

        Patient p = patientDao.getPatientById(patientId);
        if (p == null) {
            System.out.println(" -> [ERROR] Patient ID not found.");
            return;
        }

        System.out.println("\n====================== PATIENT DETAILS ======================");
        System.out.println(" ID             : " + p.patient_id());
        System.out.println(" Full Name      : " + p.full_name());
        System.out.println(" Birth Date     : " + orNA(p.birth_date()));
        System.out.println(" Gender         : " + orNA(p.gender()));
        System.out.println(" Contact        : " + orNA(p.contact_number()));
        System.out.println(" Address        : " + orNA(p.address()));
        System.out.println(" Date Registered: " + orNA(p.date_registered()));

        displayAppointmentTable(appointmentDao.getByPatient(patientId), "APPOINTMENTS OF " + p.full_name().toUpperCase());
    }

    // ---------------------------------------------------------------- appointments

    private void displayAppointmentTable(List<Appointment> list, String title) {
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

    private void filterAppointments() {
        while (true) {
            System.out.println("\n--------- FILTER APPOINTMENTS ---------");
            System.out.println("( [1] By Status | [2] By Date | [3] By Date Range | [4] By Doctor | [0] Back )");
            System.out.print("Enter Choice: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> filterByStatus();
                case "2" -> filterByDate();
                case "3" -> filterByDateRange();
                case "4" -> filterByDoctor();
                case "0" -> {
                    return;
                }
                default -> System.out.println("-> Invalid choice! 0 to 4 only.");
            }
        }
    }

    private void filterByStatus() {
        System.out.println("\nCommon statuses: SCHEDULED, COMPLETED, CANCELLED");
        System.out.print("Enter Status (0 to cancel): ");
        String status = sc.nextLine().trim().toUpperCase();

        if (status.equals("0") || status.isEmpty()) {
            System.out.println(" -> Cancelled.");
            return;
        }
        displayAppointmentTable(appointmentDao.getByStatus(status), "APPOINTMENTS: STATUS = " + status);
    }

    private void filterByDate() {
        String date = readDate("Enter Date (YYYY-MM-DD, 0 to cancel): ");
        if (date == null) {
            System.out.println(" -> Cancelled.");
            return;
        }
        displayAppointmentTable(appointmentDao.getByDate(date), "APPOINTMENTS ON " + date);
    }

    private void filterByDateRange() {
        String from = readDate("Enter From Date (YYYY-MM-DD, 0 to cancel): ");
        if (from == null) {
            System.out.println(" -> Cancelled.");
            return;
        }

        String to;
        while (true) {
            to = readDate("Enter To Date (YYYY-MM-DD, 0 to cancel): ");
            if (to == null) {
                System.out.println(" -> Cancelled.");
                return;
            }
            if (to.compareTo(from) < 0) {
                System.out.println(" -> [ERROR] 'To' date cannot be earlier than 'From' date.");
            } else {
                break;
            }
        }
        displayAppointmentTable(appointmentDao.getByDateRange(from, to), "APPOINTMENTS " + from + " TO " + to);
    }

    private void filterByDoctor() {
        List<Doctor> doctors = doctorDao.getAllDoctors();

        if (doctors.isEmpty()) {
            System.out.println("\n-> No doctor records yet.");
            return;
        }

        System.out.println();
        for (Doctor d : doctors) {
            System.out.println("  " + d);
        }

        int doctorId = readInput("Enter Doctor ID (0 to cancel): ");
        if (doctorId == 0) {
            System.out.println(" -> Cancelled.");
            return;
        }

        Doctor doctor = doctorDao.getDoctorById(doctorId);
        if (doctor == null) {
            System.out.println(" -> [ERROR] Doctor ID not found.");
            return;
        }
        displayAppointmentTable(appointmentDao.getByDoctor(doctorId),
                "APPOINTMENTS OF " + doctor.full_name().toUpperCase());
    }

    // ---------------------------------------------------------------- input helpers

    /** Returns a valid YYYY-MM-DD string, or null if the admin typed 0. */
    private String readDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();

            if (input.equals("0")) {
                return null;
            }
            try {
                return LocalDate.parse(input).toString();
            } catch (DateTimeParseException e) {
                System.out.println(" -> [ERROR] Invalid date. Use the format YYYY-MM-DD (e.g. 2026-11-05).");
            }
        }
    }

    private int readInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();

            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println(" -> [ERROR] Please enter a valid numeric ID.");
            }
        }
    }

    private String cut(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 2) + "..";
    }

    private String orNA(String s) {
        return s == null || s.isBlank() ? "N/A" : s;
    }
}