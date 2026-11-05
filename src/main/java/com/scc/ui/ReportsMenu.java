package com.scc.ui;

import com.scc.dao.ReportDao;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/** Admin > View Reports (read-only). */
public class ReportsMenu {

    private final ReportDao reportDao = new ReportDao();
    private final Scanner sc;

    public ReportsMenu(Scanner sc) {
        this.sc = sc;
    }

    public void show() {
        while (true) {
            System.out.println("\n[1] System Summary\n[2] Appointments by Status\n[3] Appointments per Doctor" +
                    "\n[4] Appointments by Date Range\n[0] Back");
            System.out.print("Enter Choice: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> systemSummary();
                case "2" -> appointmentsByStatus();
                case "3" -> appointmentsPerDoctor();
                case "4" -> appointmentsByDateRange();
                case "0" -> {
                    return;
                }
                default -> System.out.println("-> Invalid choice! 0 to 4 only.");
            }
        }
    }

    // ---------------------------------------------------------------- reports

    private void systemSummary() {
        String today = LocalDate.now().toString();

        System.out.println("\n====================== SYSTEM SUMMARY (" + today + ") ======================");

        printGroup("USERS BY ROLE", reportDao.usersByRole());
        printGroup("USERS BY STATUS", reportDao.usersByStatus());
        printGroup("DOCTORS BY STATUS", reportDao.doctorsByStatus());

        System.out.println("\n PATIENTS");
        System.out.printf("   %-26s : %d\n", "Total Registered Patients", reportDao.countPatients());

        System.out.println("\n APPOINTMENTS");
        System.out.printf("   %-26s : %d\n", "Total Appointments", reportDao.countAppointments());
        System.out.printf("   %-26s : %d\n", "Appointments Today", reportDao.countAppointmentsOn(today));
        System.out.printf("   %-26s : %d\n", "Upcoming (Scheduled)", reportDao.countUpcoming(today));
    }

    private void appointmentsByStatus() {
        Map<String, Integer> byStatus = reportDao.appointmentsByStatus();

        System.out.println("\n====================== APPOINTMENTS BY STATUS ======================");

        if (byStatus.isEmpty()) {
            System.out.println("\n-> No Records.");
            return;
        }

        int total = byStatus.values().stream().mapToInt(Integer::intValue).sum();

        String border = "+------------------+---------+----------+";
        String format = "| %-16s | %-7s | %-8s |\n";

        System.out.println();
        System.out.println(border);
        System.out.printf(format, "Status", "Count", "Percent");
        System.out.println(border);

        for (Map.Entry<String, Integer> e : byStatus.entrySet()) {
            System.out.printf(format, cut(e.getKey(), 16), e.getValue(),
                    String.format("%.1f%%", e.getValue() * 100.0 / total));
        }

        System.out.println(border);
        System.out.printf(format, "TOTAL", total, "100.0%");
        System.out.println(border);
    }

    private void appointmentsPerDoctor() {
        List<String[]> rows = reportDao.appointmentsPerDoctor();

        System.out.println("\n====================== APPOINTMENTS PER DOCTOR ======================");

        if (rows.isEmpty()) {
            System.out.println("\n-> No Records.");
            return;
        }

        String border = "+------------------------+----------------------+-------+-----------+-----------+-----------+-------+";
        String format = "| %-22s | %-20s | %-5s | %-9s | %-9s | %-9s | %-5s |\n";

        System.out.println();
        System.out.println(border);
        System.out.printf(format, "Doctor", "Specialization", "Total", "Scheduled", "Completed", "Cancelled", "Other");
        System.out.println(border);

        for (String[] r : rows) {
            System.out.printf(format, cut(r[0], 22), cut(r[1], 20), r[2], r[3], r[4], r[5], r[6]);
        }

        System.out.println(border);
        System.out.println(" Total: " + rows.size() + " doctor(s)");
    }

    private void appointmentsByDateRange() {
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

        List<String[]> rows = reportDao.appointmentsPerDay(from, to);

        System.out.println("\n====================== APPOINTMENTS " + from + " TO " + to + " ======================");

        if (rows.isEmpty()) {
            System.out.println("\n-> No Records.");
            return;
        }

        String border = "+------------+-------+-----------+-----------+-----------+-------+";
        String format = "| %-10s | %-5s | %-9s | %-9s | %-9s | %-5s |\n";

        System.out.println();
        System.out.println(border);
        System.out.printf(format, "Date", "Total", "Scheduled", "Completed", "Cancelled", "Other");
        System.out.println(border);

        int[] sums = new int[5];
        for (String[] r : rows) {
            System.out.printf(format, r[0], r[1], r[2], r[3], r[4], r[5]);
            for (int i = 0; i < 5; i++) {
                sums[i] += Integer.parseInt(r[i + 1]);
            }
        }

        System.out.println(border);
        System.out.printf(format, "TOTAL", sums[0], sums[1], sums[2], sums[3], sums[4]);
        System.out.println(border);
    }

    // ---------------------------------------------------------------- helpers

    private void printGroup(String title, Map<String, Integer> data) {
        System.out.println("\n " + title);
        if (data.isEmpty()) {
            System.out.println("   (none)");
            return;
        }
        int total = 0;
        for (Map.Entry<String, Integer> e : data.entrySet()) {
            System.out.printf("   %-26s : %d\n", e.getKey(), e.getValue());
            total += e.getValue();
        }
        System.out.printf("   %-26s : %d\n", "Total", total);
    }

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

    private String cut(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 2) + "..";
    }
}