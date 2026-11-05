package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.DoctorDao;
import com.scc.dao.UserDao;
import com.scc.model.Doctor;
import com.scc.model.User;

import java.util.List;
import java.util.Scanner;

/** Admin > Manage Doctor Records. */
public class DoctorRecordsMenu {

    private final DoctorDao doctorDao = new DoctorDao();
    private final UserDao userDao;
    private final ActivityLogDao activityDao;
    private final Scanner sc;

    public DoctorRecordsMenu(UserDao userDao, ActivityLogDao activityDao, Scanner sc) {
        this.userDao = userDao;
        this.activityDao = activityDao;
        this.sc = sc;
    }

    public void show() {
        while (true) {
            System.out.println("\n[1] Add Doctor\n[2] View All Doctors\n[3] Search Doctor" +
                    "\n[4] Update Doctor\n[5] Delete Doctor\n[0] Back");
            System.out.print("Enter Choice: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> addDoctor();
                case "2" -> displayDoctorTable(doctorDao.getAllDoctors(), "ALL DOCTORS");
                case "3" -> searchDoctors();
                case "4" -> updateDoctor();
                case "5" -> deleteDoctor();
                case "0" -> {
                    return;
                }
                default -> System.out.println("-> Invalid choice! 0 to 5 only.");
            }
        }
    }

    // ---------------------------------------------------------------- display

    private void displayDoctorTable(List<Doctor> doctors, String title) {
        System.out.println("\n====================== " + title + " ======================");

        if (doctors.isEmpty()) {
            System.out.println("\n-> No Records.");
            return;
        }

        String border = "+------+------------------------+----------------------+----------------+--------------------+----------+---------+";
        String format = "| %-4s | %-22s | %-20s | %-14s | %-18s | %-8s | %-7s |\n";

        System.out.println();
        System.out.println(border);
        System.out.printf(format, "ID", "Full Name", "Specialization", "Contact", "Available Days", "Status", "User ID");
        System.out.println(border);

        for (Doctor d : doctors) {
            System.out.printf(format, d.doctor_id(), cut(d.full_name(), 22), cut(d.specialization(), 20),
                    cut(orNA(d.contact_number()), 14), cut(orNA(d.available_days()), 18),
                    d.status(), d.user_id() != null ? d.user_id() : "N/A");
        }

        System.out.println(border);
        System.out.println(" Total: " + doctors.size() + " record(s)");
    }

    private void displayDoctorUserTable(List<User> users) {
        String border = "+------+------------------------+------------------------------+";
        String format = "| %-4s | %-22s | %-28s |\n";

        System.out.println("\n--- Doctor accounts not yet linked to a doctor record ---");
        System.out.println(border);
        System.out.printf(format, "ID", "Full Name", "Username");
        System.out.println(border);
        for (User u : users) {
            System.out.printf(format, u.id(), cut(u.full_name(), 22), cut(u.username(), 28));
        }
        System.out.println(border);
    }

    private void searchDoctors() {
        System.out.print("Enter name / specialization / contact / ID (0 to cancel): ");
        String keyword = sc.nextLine().trim();

        if (keyword.equals("0") || keyword.isEmpty()) {
            System.out.println(" -> Cancelled.");
            return;
        }
        displayDoctorTable(doctorDao.searchDoctors(keyword), "SEARCH: " + keyword);
    }

    // ---------------------------------------------------------------- add

    private void addDoctor() {
        System.out.println("\n===== ADD DOCTOR =====");

        System.out.print("Enter Full Name (0 to cancel): ");
        String fullName = sc.nextLine().trim();
        if (fullName.equals("0")) {
            System.out.println(" -> Cancelled.");
            return;
        }
        if (fullName.isEmpty()) {
            System.out.println(" -> [ERROR] Full name cannot be empty.");
            return;
        }

        System.out.print("Enter Specialization (0 to cancel): ");
        String specialization = sc.nextLine().trim();
        if (specialization.equals("0")) {
            System.out.println(" -> Cancelled.");
            return;
        }
        if (specialization.isEmpty()) {
            System.out.println(" -> [ERROR] Specialization cannot be empty.");
            return;
        }

        System.out.print("Enter Contact Number (Enter to skip): ");
        String contact = sc.nextLine().trim();

        System.out.print("Enter Available Days, e.g. Mon-Fri (Enter to skip): ");
        String days = sc.nextLine().trim();

        Integer userId = readLinkedUserId(null);
        if (userId != null && userId == -1) {
            System.out.println(" -> Cancelled.");
            return;
        }

        String status = readValidStatus(true);
        if (status == null) {
            System.out.println(" -> Cancelled.");
            return;
        }

        if (!confirm("Add Dr. " + fullName + " (" + specialization + "), status = " + status + "?")) {
            System.out.println(" -> Cancelled.");
            return;
        }

        int newId = doctorDao.addDoctor(userId, fullName, specialization,
                emptyToNull(contact), emptyToNull(days), status);

        if (newId > 0) {
            System.out.println(" -> Doctor added (ID: " + newId + ").");
            activityDao.log(null, "CREATE", "doctors", newId,
                    "Admin added doctor record: " + fullName + " (" + specialization + ")");
        } else {
            System.out.println(" -> [ERROR] Failed to add doctor.");
        }
    }

    // ---------------------------------------------------------------- update

    private void updateDoctor() {
        List<Doctor> doctors = doctorDao.getAllDoctors();
        displayDoctorTable(doctors, "SELECT DOCTOR TO UPDATE");

        if (doctors.isEmpty()) {
            return;
        }

        int doctorId = readInput("Enter Doctor ID to update (0 to cancel): ");

        if (doctorId == 0) {
            System.out.println(" -> Operation Cancelled.");
            return;
        }

        Doctor current = doctors.stream()
                .filter(d -> d.doctor_id() == doctorId)
                .findFirst()
                .orElse(null);

        if (current == null) {
            System.out.println(" -> [ERROR] Doctor ID not found.");
            return;
        }

        System.out.println("\n(Press Enter to keep the current value)");

        System.out.print("Full Name [" + current.full_name() + "]: ");
        String fullName = sc.nextLine().trim();
        if (fullName.isEmpty()) fullName = current.full_name();

        System.out.print("Specialization [" + current.specialization() + "]: ");
        String specialization = sc.nextLine().trim();
        if (specialization.isEmpty()) specialization = current.specialization();

        System.out.print("Contact Number [" + orNA(current.contact_number()) + "]: ");
        String contact = sc.nextLine().trim();
        if (contact.isEmpty()) contact = current.contact_number();

        System.out.print("Available Days [" + orNA(current.available_days()) + "]: ");
        String days = sc.nextLine().trim();
        if (days.isEmpty()) days = current.available_days();

        Integer userId = readLinkedUserId(current);
        if (userId != null && userId == -1) {
            System.out.println(" -> Operation Cancelled.");
            return;
        }

        String status = readValidStatusKeep(current.status());
        if (status == null) {
            System.out.println(" -> Operation Cancelled.");
            return;
        }

        if (!confirm("Save changes to doctor " + doctorId + "?")) {
            System.out.println(" -> Operation Cancelled.");
            return;
        }

        if (doctorDao.updateDoctor(doctorId, userId, fullName, specialization,
                emptyToNull(contact), emptyToNull(days), status)) {
            System.out.println(" -> Doctor ID " + doctorId + " updated successfully!");
            activityDao.log(null, "UPDATE", "doctors", doctorId,
                    "Updated doctor record: " + fullName + " (" + specialization + "), status " + status);
        } else {
            System.out.println(" -> [ERROR] Failed to update doctor.");
        }
    }

    // ---------------------------------------------------------------- delete

    private void deleteDoctor() {
        List<Doctor> doctors = doctorDao.getAllDoctors();
        displayDoctorTable(doctors, "DELETE DOCTOR");

        if (doctors.isEmpty()) {
            return;
        }

        int doctorId = readInput("Enter Doctor ID to delete (0 to cancel): ");

        if (doctorId == 0) {
            System.out.println(" -> Cancelled.");
            return;
        }

        Doctor target = doctors.stream()
                .filter(d -> d.doctor_id() == doctorId)
                .findFirst()
                .orElse(null);

        if (target == null) {
            System.out.println(" -> [ERROR] Doctor ID not found.");
            return;
        }

        int appointments = doctorDao.countAppointments(target.doctor_id());
        if (appointments > 0) {
            System.out.println(" -> [ERROR] " + target.full_name() + " has " + appointments
                    + " appointment record(s) and cannot be deleted.");
            System.out.println("    Tip: use Update Doctor and set the status to INACTIVE instead.");
            return;
        }

        System.out.println("\n [WARNING] This permanently deletes the record of " + target.full_name() + ".");
        if (!confirm("Are you sure?")) {
            System.out.println(" -> Cancelled.");
            return;
        }

        if (doctorDao.deleteDoctor(target.doctor_id())) {
            System.out.println(" -> Doctor record deleted.");
            activityDao.log(null, "DELETE", "doctors", target.doctor_id(),
                    "Deleted doctor record: " + target.full_name() + " (" + target.specialization() + ")");
        } else {
            System.out.println(" -> [ERROR] Failed to delete doctor.");
        }
    }

    // ---------------------------------------------------------------- input helpers

    /**
     * Asks which DOCTOR user account to link.
     * Returns the user id, null for "no link", or -1 if the admin cancelled.
     * When 'current' is not null (update mode), Enter keeps the current link and 0 unlinks.
     */
    private Integer readLinkedUserId(Doctor current) {
        Integer keep = current != null ? current.user_id() : null;

        List<User> available = userDao.getAllUsers().stream()
                .filter(u -> "DOCTOR".equalsIgnoreCase(u.role()))
                .filter(u -> {
                    Doctor linked = doctorDao.getDoctorByUserId(u.id());
                    return linked == null || (current != null && linked.doctor_id() == current.doctor_id());
                })
                .toList();

        if (available.isEmpty()) {
            return keep;
        }

        displayDoctorUserTable(available);

        while (true) {
            if (current == null) {
                System.out.print("Enter User ID to link (Enter to skip, C to cancel): ");
            } else {
                System.out.print("Linked User ID [" + (keep != null ? keep : "N/A")
                        + "] (Enter = keep, 0 = unlink, C = cancel): ");
            }
            String input = sc.nextLine().trim();

            if (input.equalsIgnoreCase("C")) {
                return -1;
            }
            if (input.isEmpty()) {
                return keep;
            }
            if (input.equals("0")) {
                return null;
            }

            try {
                int id = Integer.parseInt(input);
                if (available.stream().anyMatch(u -> u.id() == id)) {
                    return id;
                }
                System.out.println(" -> [ERROR] Pick an ID from the list above.");
            } catch (NumberFormatException e) {
                System.out.println(" -> [ERROR] Please enter a valid numeric ID.");
            }
        }
    }

    private String readValidStatus(boolean defaultActive) {
        while (true) {
            System.out.print("Enter Status (ACTIVE/INACTIVE, Enter = ACTIVE, 0 to cancel): ");
            String status = sc.nextLine().trim().toUpperCase();

            if (status.equals("0") || status.equals("CANCEL")) {
                return null;
            }
            if (status.isEmpty() && defaultActive) {
                return "ACTIVE";
            }
            if (status.equals("ACTIVE") || status.equals("INACTIVE")) {
                return status;
            }
            System.out.println(" -> [ERROR] Invalid Status. Must be ACTIVE or INACTIVE.");
        }
    }

    private String readValidStatusKeep(String currentStatus) {
        while (true) {
            System.out.print("Status [" + currentStatus + "] (ACTIVE/INACTIVE, Enter = keep, 0 to cancel): ");
            String status = sc.nextLine().trim().toUpperCase();

            if (status.equals("0") || status.equals("CANCEL")) {
                return null;
            }
            if (status.isEmpty()) {
                return currentStatus;
            }
            if (status.equals("ACTIVE") || status.equals("INACTIVE")) {
                return status;
            }
            System.out.println(" -> [ERROR] Invalid Status. Must be ACTIVE or INACTIVE.");
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

    private boolean confirm(String message) {
        System.out.print(message + " (Y/N): ");
        return sc.nextLine().trim().equalsIgnoreCase("Y");
    }

    private String cut(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 2) + "..";
    }

    private String orNA(String s) {
        return s == null || s.isBlank() ? "N/A" : s;
    }

    private String emptyToNull(String s) {
        return s == null || s.isBlank() || s.equals("N/A") ? null : s;
    }
}