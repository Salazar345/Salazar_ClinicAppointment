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
        while (true) {
            System.out.println("\n=======================================");
            System.out.println("             ADMIN MENU");
            System.out.println(" User: " + admin.full_name());
            System.out.println("=======================================");
            System.out.println("  1. Manage User Accounts");
            System.out.println("  2. Manage Doctor Records");
            System.out.println("  3. View All Patients and Appointments");
            System.out.println("  4. View Reports");
            System.out.println("  5. View Activity Logs");
            System.out.println("  6. Logout");

            int choice = InputHelper.readChoice("Choose (1-6): ", 1, 6);

            switch (choice) {
                case 1 -> manageUserAccounts(admin);
                case 2 -> System.out.println("\n[Manage Doctor Records] - coming soon.");
                case 3 -> System.out.println("\n[View All Patients and Appointments] - coming soon.");
                case 4 -> System.out.println("\n[View Reports] - coming soon.");
                case 5 -> System.out.println("\n[View Activity Logs] - coming soon.");
                case 6 -> {
                    System.out.println("\nLogging out...");
                    return;
                }
            }
        }
    }

    // ===================== MANAGE USER ACCOUNTS =====================

    private void manageUserAccounts(User admin) {
        while (true) {
            System.out.println("\n=======================================");
            System.out.println("         MANAGE USER ACCOUNTS");
            System.out.println("=======================================");
            System.out.println("  1. View All Users");
            System.out.println("  2. Approve / Reject Pending Accounts");
            System.out.println("  3. Deactivate Account");
            System.out.println("  4. Reactivate Account");
            System.out.println("  5. Delete Account");
            System.out.println("  6. Back");

            int choice = InputHelper.readChoice("Choose (1-6): ", 1, 6);

            switch (choice) {
                case 1 -> viewAllUsers();
                case 2 -> handlePending();
                case 3 -> deactivateUser(admin);
                case 4 -> reactivateUser();
                case 5 -> deleteUser(admin);
                case 6 -> {
                    return;
                }
            }
        }
    }

    // ---------- 1. View all ----------

    private void viewAllUsers() {
        List<User> users = userDao.getAllUsers();
        System.out.println("\n--- ALL USER ACCOUNTS ---");
        printUsers(users);
        InputHelper.pause();
    }

    // ---------- 2. Approve / Reject ----------

    private void handlePending() {
        List<User> pending = userDao.getPendingUsers();
        System.out.println("\n--- PENDING ACCOUNTS ---");

        if (pending.isEmpty()) {
            System.out.println("  No pending accounts.");
            return;
        }

        printUsers(pending);

        User target = pickUser("Enter User ID to review (0 to cancel): ", pending);
        if (target == null) return;

        System.out.println("\nReviewing: " + target.full_name() + " (" + target.username() + ") - " + target.role());
        System.out.println("  1. Approve");
        System.out.println("  2. Reject");
        System.out.println("  0. Cancel");
        int action = InputHelper.readChoice("Choose (0-2): ", 0, 2);

        switch (action) {
            case 1 -> {
                if (userDao.approveUser(target.id())) {
                    System.out.println("\n -> " + target.full_name() + " has been APPROVED.");
                } else {
                    System.out.println("\n -> Failed to approve user.");
                }
            }
            case 2 -> {
                if (InputHelper.confirm("Reject " + target.full_name() + "?")
                        && userDao.updateStatus(target.id(), "REJECTED")) {
                    System.out.println("\n -> " + target.full_name() + " has been REJECTED.");
                } else {
                    System.out.println("\n -> Cancelled / failed.");
                }
            }
            default -> System.out.println("\n -> Cancelled.");
        }
    }

    // ---------- 3. Deactivate ----------

    private void deactivateUser(User admin) {
        List<User> approved = userDao.getAllUsers().stream()
                .filter(u -> "APPROVED".equalsIgnoreCase(u.active()))
                .filter(u -> u.id() != admin.id())          // dili ma-deactivate ang kaugalingon
                .toList();

        System.out.println("\n--- ACTIVE ACCOUNTS ---");
        if (approved.isEmpty()) {
            System.out.println("  No accounts available to deactivate.");
            return;
        }
        printUsers(approved);

        User target = pickUser("Enter User ID to deactivate (0 to cancel): ", approved);
        if (target == null) return;

        if (isLastActiveAdmin(target)) {
            System.out.println("\n -> Cannot deactivate the last active ADMIN.");
            return;
        }

        if (InputHelper.confirm("Deactivate " + target.full_name() + "?")) {
            if (userDao.updateStatus(target.id(), "DEACTIVATED")) {
                System.out.println("\n -> " + target.full_name() + " has been DEACTIVATED.");
            } else {
                System.out.println("\n -> Failed to deactivate user.");
            }
        } else {
            System.out.println("\n -> Cancelled.");
        }
    }

    // ---------- 4. Reactivate ----------

    private void reactivateUser() {
        List<User> inactive = userDao.getAllUsers().stream()
                .filter(u -> "DEACTIVATED".equalsIgnoreCase(u.active())
                        || "REJECTED".equalsIgnoreCase(u.active()))
                .toList();

        System.out.println("\n--- DEACTIVATED / REJECTED ACCOUNTS ---");
        if (inactive.isEmpty()) {
            System.out.println("  No accounts to reactivate.");
            return;
        }
        printUsers(inactive);

        User target = pickUser("Enter User ID to reactivate (0 to cancel): ", inactive);
        if (target == null) return;

        if (InputHelper.confirm("Reactivate " + target.full_name() + "?")) {
            if (userDao.updateStatus(target.id(), "APPROVED")) {
                System.out.println("\n -> " + target.full_name() + " is now APPROVED.");
            } else {
                System.out.println("\n -> Failed to reactivate user.");
            }
        } else {
            System.out.println("\n -> Cancelled.");
        }
    }

    // ---------- 5. Delete ----------

    private void deleteUser(User admin) {
        List<User> others = userDao.getAllUsers().stream()
                .filter(u -> u.id() != admin.id())          // dili ma-delete ang kaugalingon
                .toList();

        System.out.println("\n--- DELETE ACCOUNT ---");
        if (others.isEmpty()) {
            System.out.println("  No other accounts to delete.");
            return;
        }
        printUsers(others);

        User target = pickUser("Enter User ID to delete (0 to cancel): ", others);
        if (target == null) return;

        if (isLastActiveAdmin(target)) {
            System.out.println("\n -> Cannot delete the last active ADMIN.");
            return;
        }

        System.out.println("\n  WARNING: This permanently deletes " + target.full_name() + ".");
        if (InputHelper.confirm("Are you sure?")) {
            if (userDao.deleteUser(target.id())) {
                System.out.println("\n -> Account deleted.");
            } else {
                System.out.println("\n -> Failed to delete user.");
            }
        } else {
            System.out.println("\n -> Cancelled.");
        }
    }

    // ===================== HELPERS =====================

    /** Mo-prompt og User ID gikan sa gihatag nga list. Return null kung 0 (cancel). */
    private User pickUser(String prompt, List<User> options) {
        while (true) {
            int id = InputHelper.readChoice(prompt, 0, Integer.MAX_VALUE);
            if (id == 0) {
                System.out.println("\n -> Cancelled.");
                return null;
            }
            for (User u : options) {
                if (u.id() == id) return u;
            }
            System.out.println("  [!] ID " + id + " is not in the list above. Try again.");
        }
    }

    /** True kung ang target ADMIN ug siya na lang ang active admin. */
    private boolean isLastActiveAdmin(User target) {
        if (!"ADMIN".equalsIgnoreCase(target.role())) return false;
        long activeAdmins = userDao.getAllUsers().stream()
                .filter(u -> "ADMIN".equalsIgnoreCase(u.role()))
                .filter(u -> "APPROVED".equalsIgnoreCase(u.active()))
                .count();
        return activeAdmins <= 1;
    }

    private void printUsers(List<User> users) {
        if (users.isEmpty()) {
            System.out.println("  (no records)");
            return;
        }
        System.out.println("-".repeat(86));
        System.out.printf("%-5s %-22s %-28s %-14s %-12s%n", "ID", "FULL NAME", "USERNAME", "ROLE", "STATUS");
        System.out.println("-".repeat(86));
        for (User u : users) {
            System.out.printf("%-5d %-22s %-28s %-14s %-12s%n",
                    u.id(), cut(u.full_name(), 22), cut(u.username(), 28), u.role(), u.active());
        }
        System.out.println("-".repeat(86));
    }

    private String cut(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 2) + "..";
    }
}