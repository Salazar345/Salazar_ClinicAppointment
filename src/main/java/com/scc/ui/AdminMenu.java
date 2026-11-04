package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.UserDao;
import com.scc.model.ActivityLogs;
import com.scc.model.User;

import java.util.List;
import java.util.Scanner;

public class AdminMenu {

    private final UserDao userDao;
    private final ActivityLogDao activityDao;
    private final Scanner sc;

    public AdminMenu(UserDao userDao, ActivityLogDao activityDao, Scanner sc) {
        this.userDao = userDao;
        this.activityDao = activityDao;
        this.sc = sc;
    }

    public void show(User admin) {
        boolean loggedIn = true;

        while (loggedIn) {
            System.out.println("\n==============================");
            System.out.println("           ADMIN MENU ");
            System.out.println("    User: " + admin.full_name());
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
                case "5" -> activityLog();
                case "0" -> {
                    System.out.println("You have been logged out. Goodbye, " + admin.full_name() + "!");
                    activityDao.log(admin.id(), "LOGOUT", "users", admin.id(),
                            admin.full_name() + " logged out");
                    loggedIn = false;
                }
                default -> System.out.println("-> Invalid choice. Try again.");
            }
        }
    }

    private void manageUserAccounts(User admin) {
        while (true) {
            System.out.println("\n===============================");
            System.out.println("      MANAGE USER ACCOUNTS");
            System.out.println("===============================");
            System.out.println("[1] Add User\n[2] View All Users\n[3] View Pending Users\n[4] Approve User" +
                    "\n[5] Update User (ROLE/STATUS)\n[6] Delete Account\n[0] Back");
            System.out.print("Enter Choice: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> addUser(admin);
                case "2" -> viewAllUsers();
                case "3" -> viewPendingUsers();
                case "4" -> approveUser(admin);
                case "5" -> updateUser(admin);
                case "6" -> deleteUser(admin);
                case "0" -> {
                    return;
                }
                default -> System.out.println("-> Invalid choice! 0 to 6 only.");
            }
        }
    }

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
                    u.role(), u.status());
        }

        System.out.println(border);
    }

    private String cut(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 2) + "..";
    }

    private void viewAllUsers() {
        displayUserTable(userDao.getAllUsers(), "ALL REGISTERED USERS");
    }

    private void viewPendingUsers() {
        displayUserTable(userDao.getPendingUsers(), "PENDING APPROVALS");
    }

    private void approveUser(User admin) {
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
            activityDao.log(admin.id(), "APPROVED", "users", target.id(), "Approved account of " + target.full_name());
        } else {
            System.out.println(" -> [ERROR] Failed to approve user.");
        }
    }

    private void addUser(User admin) {
        System.out.println("\n===== ADD USER =====");

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

        String username;
        while (true) {
            System.out.print("Enter Username (0 to cancel): ");
            username = sc.nextLine().trim().toLowerCase();
            if (username.equals("0")) {
                System.out.println(" -> Cancelled.");
                return;
            }
            if (username.isEmpty() || username.contains(" ")) {
                System.out.println(" -> [ERROR] Username cannot be empty or contain spaces.");
            } else if (userDao.usernameExist(username)) {
                System.out.println(" -> [ERROR] Username already taken.");
            } else {
                break;
            }
        }

        String password;
        while (true) {
            System.out.print("Enter Password (min 5 chars, 0 to cancel): ");
            password = sc.nextLine().trim();
            if (password.equals("0")) {
                System.out.println(" -> Cancelled.");
                return;
            }
            if (password.length() < 5) {
                System.out.println(" -> [ERROR] Password must be at least 5 characters.");
                continue;
            }
            System.out.print("Confirm Password: ");
            if (!sc.nextLine().trim().equals(password)) {
                System.out.println(" -> [ERROR] Passwords do not match.");
                continue;
            }
            break;
        }

        String role = readValidRole();
        if (role == null) {
            System.out.println(" -> Cancelled.");
            return;
        }

        if (!confirm("Add user '" + username + "' (" + fullName + ") as " + role + "?")) {
            System.out.println(" -> Cancelled.");
            return;
        }

        boolean added = userDao.registerUser(fullName, username, password, role);
        if (added) {
            int newId = userDao.getUserIdByUsername(username);
            Integer recordId = null;
            if (newId > 0) {
                userDao.approveUser(newId);
                recordId = newId;
            }
            System.out.println(" -> User added successfully.");
            activityDao.log(admin.id(), "CREATE", "users", recordId,
                    "Admin added " + role + " account: " + username);
        } else {
            System.out.println(" -> [ERROR] Failed to add user.");
        }
    }

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

        if (userDao.updateUserAndStatus(userId, newRole, newStatus)) {
            activityDao.log(currentAdmin.id(), "UPDATE", "users", userId,
                    "Changed role to " + newRole + ", status to " + newStatus);
        }
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

    private void deleteUser(User admin) {
        List<User> others = userDao.getAllUsers().stream()
                .filter(u -> u.id() != admin.id())
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
            activityDao.log(admin.id(), "DELETE", "users", target.id(),
                    "Deleted account of " + target.full_name() + " (" + target.role() + ")");
        } else {
            System.out.println(" -> [ERROR] Failed to delete user.");
        }
    }

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

    private void displayLogTable(List<ActivityLogs> logs, String title) {
        System.out.println("\n====================== " + title + " ======================");

        if (logs.isEmpty()) {
            System.out.println("\n-> No Records.");
            return;
        }

        String border = "+-------+-------+----------------+------------+--------+----------------------------------------+---------------------+";
        String format = "| %-5s | %-5s | %-14s | %-10s | %-6s | %-38s | %-19s |\n";

        System.out.println();
        System.out.println(border);
        System.out.printf(format, "LogID", "User", "Action", "Table", "Rec ID", "Details", "Timestamp");
        System.out.println(border);

        for (ActivityLogs l : logs) {
            System.out.printf(format,
                    l.log_id(),
                    l.user_id() != null ? l.user_id() : "N/A",
                    cut(l.action(), 14),
                    l.table_affected() != null ? cut(l.table_affected(), 10) : "N/A",
                    l.record_id() != null ? l.record_id() : "N/A",
                    l.details() != null ? cut(l.details(), 38) : "",
                    l.log_timestamp());
        }

        System.out.println(border);
        System.out.println(" Total: " + logs.size() + " record(s)");
    }

    private void activityLog() {
        List<ActivityLogs> current = activityDao.getRecentLogs(20);
        String title = "RECENT LOGS (LAST 20)";

        while (true) {
            displayLogTable(current, title);

            System.out.println("\n--------- FILTER ---------");
            System.out.println("( [1] Show All | [2] Filter by Action | [3] Filter by Table | [4] Refresh (Recent 20) | [0] Back )");
            System.out.print("Enter Choice: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> {
                    current = activityDao.getAllLogs();
                    title = "ALL LOGS";
                }
                case "2" -> {
                    String action = readFilterAction();
                    if (action != null) {
                        current = activityDao.getLogsByAction(action);
                        title = "LOGS: ACTION = " + action;
                    }
                }
                case "3" -> {
                    String table = readFilterTable();
                    if (table != null) {
                        current = activityDao.getLogsByTable(table);
                        title = "LOGS: TABLE = " + table;
                    }
                }
                case "4" -> {
                    current = activityDao.getRecentLogs(20);
                    title = "RECENT LOGS (LAST 20)";
                }
                case "0" -> {
                    return;
                }
                default -> System.out.println("-> Invalid choice! 0 to 4 only.");
            }
        }
    }

    private String readFilterAction() {
        System.out.println("\nActions: LOGIN, LOGOUT, LOGIN_FAILED, REGISTER, CREATE, UPDATE, DELETE, APPROVE");
        System.out.print("Enter Action (0 to cancel): ");
        String action = sc.nextLine().trim().toUpperCase();
        if (action.equals("0") || action.isEmpty()) {
            System.out.println(" -> Cancelled.");
            return null;
        }
        return action;
    }

    private String readFilterTable() {
        System.out.println("\nTables: users, patients, doctors, appointments");
        System.out.print("Enter Table (0 to cancel): ");
        String table = sc.nextLine().trim().toLowerCase();
        if (table.equals("0") || table.isEmpty()) {
            System.out.println(" -> Cancelled.");
            return null;
        }
        return table;
    }
}