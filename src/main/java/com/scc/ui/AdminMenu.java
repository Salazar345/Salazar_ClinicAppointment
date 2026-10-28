package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.List;
import java.util.Scanner;

public class AdminMenu {

    private final UserDao userDao;
    private final Scanner sc;

    public AdminMenu(UserDao userDao, Scanner sc) {
        this.userDao = userDao;
        this.sc = sc;
    }

    // ===================== MAIN ADMIN MENU =====================

    public void show(User admin) {
        boolean loggedIn = true;

        while (loggedIn) {
            System.out.println("\n==============================");
            System.out.println("           ADMIN MENU ");
            System.out.println(" User: " + admin.full_name());
            System.out.println("==============================");
            System.out.println("1. Manage User Accounts");
            System.out.println("2. Manage Doctor Records");
            System.out.println("3. View All Patients and Appointments");
            System.out.println("4. View Reports");
            System.out.println("5. View Activity Logs");
            System.out.println("0. Logout");
            System.out.print("Enter choice: ");

            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> manageUserAccounts(admin);
                case "2" -> manageDoctorRecords();
                case "3" -> viewPatientsAndAppointments();
                case "4" -> reports();
                case "5" -> activityLogs();
                case "0" -> {
                    System.out.println("You have been logged out. Goodbye, " + admin.full_name() + "!");
                    loggedIn = false;
                }
                default -> System.out.println("-> Invalid choice. Try again.");
            }
        }
    }

    // ===================== MANAGE USER ACCOUNTS =====================

    private void manageUserAccounts(User admin) {
        while (true) {
            System.out.println("\n===============================");
            System.out.println("      MANAGE USER ACCOUNTS");
            System.out.println("===============================");
            System.out.println("[1] View All Users\n[2] View Pending Users\n[3] Approve User" +
                    "\n[4] Update User (ROLE/STATUS)\n[5] Delete Account\n[0] Back");
            System.out.print("Enter Choice: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> viewAllUsers();
                case "2" -> viewPendingUsers();
                case "3" -> approveUser();
                case "4" -> updateUser(admin);
                case "5" -> deleteUser(admin);
                case "0" -> {
                    return;
                }
                default -> System.out.println("-> Invalid choice! 0 to 5 only.");
            }
        }
    }

    // ---------- Table display ----------

    private void displayUserTable(List<User> users, String title) {
        System.out.println("\n====================== " + title + " ======================");

        if (users.isEmpty()) {
            System.out.println("\n-> No Records.");
            return;
        }

        String border = "+------+------------------------+------------------------------+----------------+------------+";
        String format = "| %-4s | %-22s | %-28s | %-14s | %-10s |\n";

        System.out.println();
        System.out.println(border);
        System.out.printf(format, "ID", "Full Name", "Username", "Role", "Status");
        System.out.println(border);

        for (User u : users) {
            System.out.printf(format, u.id(), cut(u.full_name(), 22), cut(u.username(), 28),
                    u.role(), u.active());
        }

        System.out.println(border);
    }

    private String cut(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 2) + "..";
    }

    // ---------- 1. View all ----------

    private void viewAllUsers() {
        displayUserTable(userDao.getAllUsers(), "ALL REGISTERED USERS");
    }

    // ---------- 2. View pending ----------

    private void viewPendingUsers() {
        displayUserTable(userDao.getPendingUsers(), "PENDING APPROVALS");
    }

    // ---------- 3. Approve ----------

    private void approveUser() {
        List<User> pendingUsers = userDao.getPendingUsers();
        displayUserTable(pendingUsers, "PENDING APPROVALS");

        if (pendingUsers.isEmpty()) {
            return;
        }

        int userId = readInput("Enter User ID to approve (0 to cancel): ");

        if (userId == 0) {
            System.out.println("-> Cancelled.");
            return;
        }

        User target = pendingUsers.stream()
                .filter(u -> u.id() == userId)
                .findFirst()
                .orElse(null);

        if (target == null) {
            System.out.println(" -> [ERROR] User ID not found in the pending list.");
            return;
        }

        if (userDao.approveUser(target.id())) {
            System.out.println(" -> " + target.full_name() + " has been APPROVED.");
        } else {
            System.out.println(" -> [ERROR] Failed to approve user.");
        }
    }

    // ---------- 4. Update role / status ----------

    private void updateUser(User currentAdmin) {
        List<User> users = userDao.getAllUsers();
        displayUserTable(users, "SELECT USER TO UPDATE");

        if (users.isEmpty()) {
            return;
        }

        int userId = readInput("Enter user ID to update (0 to cancel): ");

        if (userId == 0) {
            System.out.println(" -> Operation Cancelled.");
            return;
        }

        boolean exists = users.stream().anyMatch(u -> u.id() == userId);
        if (!exists) {
            System.out.println(" -> [ERROR] User ID not found.");
            return;
        }

        if (userId == currentAdmin.id()) {
            System.out.println(" -> [WARNING] Modifying current active Admin is Restricted.");
            return;
        }

        String newRole = readValidRole();
        if (newRole == null) {
            System.out.println(" -> Operation Cancelled.");
            return;
        }

        String newStatus = readValidStatus();
        if (newStatus == null) {
            System.out.println(" -> Operation Cancelled.");
            return;
        }

        if (!confirm("Update user " + userId + " to role = " + newRole + ", status = " + newStatus + "?")) {
            System.out.println(" -> Operation Cancelled.");
            return;
        }

        // DAO already prints success / not found messages
        userDao.updateUserAndStatus(userId, newRole, newStatus);
    }

    private String readValidRole() {
        while (true) {
            System.out.print("Enter Role (ADMIN/DOCTOR/RECEPTIONIST/TRIAGE [0 to cancel]): ");
            String role = sc.nextLine().trim().toUpperCase();

            if (role.equals("0") || role.equals("CANCEL")) {
                return null;
            }

            if (role.equals("ADMIN") || role.equals("DOCTOR")
                    || role.equals("RECEPTIONIST") || role.equals("TRIAGE")) {
                return role;
            }

            System.out.println(" -> [ERROR] Invalid Role.");
        }
    }

    private String readValidStatus() {
        while (true) {
            System.out.print("Enter Status (APPROVED/PENDING [0 to cancel]): ");
            String status = sc.nextLine().trim().toUpperCase();

            if (status.equals("0") || status.equals("CANCEL")) {
                return null;
            }

            if (status.equals("APPROVED") || status.equals("PENDING")) {
                return status;
            }

            System.out.println(" -> [ERROR] Invalid Status. Must be APPROVED or PENDING.");
        }
    }

    // ---------- 5. Delete ----------

    private void deleteUser(User admin) {
        List<User> others = userDao.getAllUsers().stream()
                .filter(u -> u.id() != admin.id())   // can't delete yourself
                .toList();

        displayUserTable(others, "DELETE ACCOUNT");

        if (others.isEmpty()) {
            return;
        }

        int userId = readInput("Enter User ID to delete (0 to cancel): ");

        if (userId == 0) {
            System.out.println(" -> Cancelled.");
            return;
        }

        User target = others.stream()
                .filter(u -> u.id() == userId)
                .findFirst()
                .orElse(null);

        if (target == null) {
            System.out.println(" -> [ERROR] User ID not found.");
            return;
        }

        System.out.println("\n [WARNING] This permanently deletes " + target.full_name() + ".");
        if (!confirm("Are you sure?")) {
            System.out.println(" -> Cancelled.");
            return;
        }

        if (userDao.deleteUser(target.id())) {
            System.out.println(" -> Account deleted.");
        } else {
            System.out.println(" -> [ERROR] Failed to delete user.");
        }
    }

    // ===================== COMING SOON =====================

    private void manageDoctorRecords() {
        System.out.println("\n=== MANAGE DOCTOR RECORDS ===");
    }

    private void viewPatientsAndAppointments() {
        System.out.println("\n=== PATIENTS AND APPOINTMENTS ===");
    }

    private void reports() {
        System.out.println("\n=== REPORTS ===");
    }

    private void activityLogs() {
        System.out.println("\n=== ACTIVITY LOGS ===");
    }

    // ===================== HELPERS =====================

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
}