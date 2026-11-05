package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.AppointmentManageDao;
import com.scc.dao.TriageDao;
import com.scc.dao.UserDao;
import com.scc.model.Appointment;
import com.scc.model.TriageRecord;
import com.scc.model.User;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;

public class TriageMenu {

    private static final Pattern BP = Pattern.compile("\\d{2,3}/\\d{2,3}");

    private final UserDao userDao;
    private final ActivityLogDao activityDao;
    private final Scanner sc;
    private final ClinicUi ui;

    private final AppointmentManageDao appointmentDao = new AppointmentManageDao();
    private final TriageDao triageDao = new TriageDao();

    public TriageMenu(UserDao userDao, ActivityLogDao activityDao, Scanner sc) {
        this.userDao = userDao;
        this.activityDao = activityDao;
        this.sc = sc;
        this.ui = new ClinicUi(sc);
    }

    public void show(User triage) {
        while (true) {
            System.out.println("\n=====================================");
            System.out.println("            TRIAGE / NURSE MENU");
            System.out.println(" User: " + triage.username());
            System.out.println("=====================================");
            System.out.println("  1. View Daily Queue");
            System.out.println("  2. Record Patient Vitals");
            System.out.println("  3. Update Triage Status");
            System.out.println("  4. Logout");

            int choice = InputHelper.readChoice("Choose (1-4): ", 1, 4);
            switch (choice) {
                case 1 -> {
                    displayQueue(todayQueue());
                    InputHelper.pause();
                }
                case 2 -> recordVitals(triage);
                case 3 -> updateTriageStatus(triage);
                case 4 -> {
                    if (InputHelper.confirm("Are you sure you want to logout?")) {
                        System.out.println("Goodbye, " + triage.username() + "! You are logged out.");
                        activityDao.log(triage.id(), "LOGOUT", "users", triage.id(),
                                triage.full_name() + " logged out");
                        return;
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- queue

    private List<Appointment> todayQueue() {
        String today = LocalDate.now().toString();
        return appointmentDao.getByDate(today).stream()
                .filter(a -> "SCHEDULED".equalsIgnoreCase(a.status()))
                .sorted(Comparator.comparing(Appointment::appointment_time))
                .toList();
    }

    private void displayQueue(List<Appointment> queue) {
        String today = LocalDate.now().toString();
        System.out.println("\n====================== DAILY QUEUE (" + today + ") ======================");

        if (queue.isEmpty()) {
            System.out.println("\n-> No scheduled patients for today.");
            return;
        }

        Map<Integer, String> statuses = triageDao.getStatusMap(today);

        String border = "+------+-------+------------------+------------------+----------------------+------------------+";
        String format = "| %-4s | %-5s | %-16s | %-16s | %-20s | %-16s |\n";

        System.out.println();
        System.out.println(border);
        System.out.printf(format, "ID", "Time", "Patient", "Doctor", "Reason", "Triage Status");
        System.out.println(border);
        for (Appointment a : queue) {
            System.out.printf(format, a.appointment_id(), a.appointment_time(),
                    ClinicUi.cut(a.patient_name(), 16), ClinicUi.cut(a.doctor_name(), 16),
                    ClinicUi.cut(ClinicUi.orNA(a.reason()), 20),
                    statuses.getOrDefault(a.appointment_id(), "WAITING"));
        }
        System.out.println(border);
        System.out.println(" Total: " + queue.size() + " patient(s)");
    }

    private Appointment pickFromQueue(List<Appointment> queue) {
        displayQueue(queue);
        if (queue.isEmpty()) return null;

        int id = ui.readId("Enter Appointment ID (0 to cancel): ");
        if (id == 0) {
            System.out.println(" -> Cancelled.");
            return null;
        }
        Appointment a = queue.stream().filter(x -> x.appointment_id() == id).findFirst().orElse(null);
        if (a == null) {
            System.out.println(" -> [ERROR] Appointment ID not found in today's queue.");
        }
        return a;
    }

    // ---------------------------------------------------------------- vitals

    private void recordVitals(User user) {
        Appointment target = pickFromQueue(todayQueue());
        if (target == null) return;

        TriageRecord cur = triageDao.getRecord(target.appointment_id());
        if (cur != null && cur.blood_pressure() != null) {
            System.out.println("\n--- Existing vitals (Enter keeps a value) ---");
            ui.printVitals(cur);
        }

        System.out.println("\n===== VITALS: " + target.patient_name() + " =====");

        String bp = readBp(cur != null ? cur.blood_pressure() : null);
        Double temp = readDouble("Temperature in C", 30, 45, cur != null ? cur.temperature() : null);
        Integer pulse = readInt("Pulse rate (bpm)", 20, 250, cur != null ? cur.pulse_rate() : null);
        Integer resp = readInt("Respiratory rate (/min)", 5, 80, cur != null ? cur.respiratory_rate() : null);
        Double weight = readDouble("Weight (kg)", 1, 500, cur != null ? cur.weight_kg() : null);
        Double height = readDouble("Height (cm)", 30, 260, cur != null ? cur.height_cm() : null);

        String curNotes = cur != null ? cur.notes() : null;
        System.out.print("Notes [" + ClinicUi.orNA(curNotes) + "]: ");
        String notes = sc.nextLine().trim();
        if (notes.isEmpty()) notes = curNotes;

        if (bp == null && temp == null && pulse == null && resp == null && weight == null && height == null) {
            System.out.println(" -> Nothing to save.");
            return;
        }

        if (!InputHelper.confirm("Save vitals for " + target.patient_name() + "?")) {
            System.out.println(" -> Cancelled.");
            return;
        }

        if (triageDao.saveVitals(target.appointment_id(), bp, temp, pulse, resp, weight, height,
                ClinicUi.emptyToNull(notes), user.id())) {
            System.out.println(" -> Vitals saved.");
            activityDao.log(user.id(), "UPDATE", "triage_records", target.appointment_id(),
                    "Recorded vitals for " + target.patient_name());
        } else {
            System.out.println(" -> [ERROR] Failed to save vitals.");
        }
        InputHelper.pause();
    }

    private String readBp(String current) {
        while (true) {
            System.out.print("Blood pressure e.g. 120/80 [" + ClinicUi.orNA(current) + "]: ");
            String in = sc.nextLine().trim();
            if (in.isEmpty()) return current;
            if (BP.matcher(in).matches()) return in;
            System.out.println(" -> [ERROR] Use the format 120/80.");
        }
    }

    private Double readDouble(String label, double min, double max, Double current) {
        while (true) {
            System.out.print(label + " [" + (current != null ? current : "N/A") + "]: ");
            String in = sc.nextLine().trim();
            if (in.isEmpty()) return current;
            try {
                double v = Double.parseDouble(in);
                if (v >= min && v <= max) return v;
                System.out.println(" -> [ERROR] Value must be between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println(" -> [ERROR] Please enter a number.");
            }
        }
    }

    private Integer readInt(String label, int min, int max, Integer current) {
        while (true) {
            System.out.print(label + " [" + (current != null ? current : "N/A") + "]: ");
            String in = sc.nextLine().trim();
            if (in.isEmpty()) return current;
            try {
                int v = Integer.parseInt(in);
                if (v >= min && v <= max) return v;
                System.out.println(" -> [ERROR] Value must be between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println(" -> [ERROR] Please enter a whole number.");
            }
        }
    }

    // ---------------------------------------------------------------- triage status

    private void updateTriageStatus(User user) {
        Appointment target = pickFromQueue(todayQueue());
        if (target == null) return;

        System.out.println("\n[1] WAITING\n[2] IN_TRIAGE\n[3] VITALS_TAKEN\n[4] READY_FOR_DOCTOR\n[0] Cancel");
        System.out.print("New triage status: ");
        String s = sc.nextLine().trim();

        String status;
        switch (s) {
            case "1" -> status = "WAITING";
            case "2" -> status = "IN_TRIAGE";
            case "3" -> status = "VITALS_TAKEN";
            case "4" -> status = "READY_FOR_DOCTOR";
            default -> {
                System.out.println(" -> Cancelled.");
                return;
            }
        }

        if (triageDao.setStatus(target.appointment_id(), status, user.id())) {
            System.out.println(" -> " + target.patient_name() + " is now " + status + ".");
            activityDao.log(user.id(), "UPDATE", "triage_records", target.appointment_id(),
                    "Triage status of " + target.patient_name() + " set to " + status);
        } else {
            System.out.println(" -> [ERROR] Failed to update triage status.");
        }
        InputHelper.pause();
    }
}