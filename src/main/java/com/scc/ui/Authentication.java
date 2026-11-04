package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.Scanner;

public class Authentication {

    private final UserDao userDao;
    private final ActivityLogDao activityDao;
    private final Scanner sc;

    public Authentication(UserDao userDao, ActivityLogDao activityDao, Scanner sc) {
        this.userDao = userDao;
        this.activityDao = activityDao;
        this.sc = sc;
    }

    public void start() {
        while (true) {
            System.out.println("\n    ===== CLINIC APPOINTMENT SYSTEM =====");
            System.out.println("[1] Login");
            System.out.println("[2] Register");
            System.out.println("[3] Exit");
            System.out.print("Enter Choice: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> loginUser();
                case "2" -> registerUser();
                case "3" -> {
                    System.out.println("Goodbye!");
                    return;
                }
                default -> System.out.println("Invalid! Please select 1, 2, or 3 only.");
            }
        }
    }

    private void loginUser() {
        System.out.println("\n    ===== LOG IN =====");

        String user;
        while (true) {
            System.out.print("Enter Username: ");
            user = sc.nextLine().trim();
            if (user.isEmpty()) {
                System.out.println("Invalid! Username cannot be empty.");
            } else {
                break;
            }
        }

        String pass;
        while (true) {
            System.out.print("Enter Password: ");
            pass = sc.nextLine().trim();
            if (pass.isEmpty()) {
                System.out.println("Invalid! Password cannot be empty.");
            } else {
                break;
            }
        }

        User loggedIn = userDao.login(user, pass);

        if (loggedIn == null) {
            int existingId = userDao.getUserIdByUsername(user.toLowerCase());
            activityDao.log(existingId > 0 ? existingId : null, "LOGIN_FAILED", "users",
                    existingId > 0 ? existingId : null,
                    "Failed login attempt for username: " + user);
            return;
        }

        System.out.println("\nSuccessfully Login");
        System.out.println("\n -> Welcome " + loggedIn.full_name() + " | ROLE: " + loggedIn.role());
        activityDao.log(loggedIn.id(), "LOGIN", "users", loggedIn.id(),
                loggedIn.username() + " logged in as " + loggedIn.role());

        switch (loggedIn.role()) {
            case "ADMIN"        -> new AdminMenu(userDao, activityDao, sc).show(loggedIn);
            case "DOCTOR"       -> new DoctorMenu(userDao, activityDao, sc).show(loggedIn);
            case "RECEPTIONIST" -> new ReceptionistMenu(userDao, activityDao, sc).show(loggedIn);
            case "TRIAGE"       -> new TriageMenu(userDao, activityDao, sc).show(loggedIn);
            default             -> System.out.println("Unknown role: " + loggedIn.role());
        }
    }

    private void registerUser() {
        System.out.println("\n    ===== REGISTER =====");

        String fullName;
        while (true) {
            System.out.print("Enter your Fullname: ");
            fullName = sc.nextLine().trim();
            if (fullName.isEmpty()) {
                System.out.println("Invalid! Fullname cannot be empty.");
            } else {
                break;
            }
        }

        String user;
        while (true) {
            System.out.print("Enter username: ");
            user = sc.nextLine().trim().toLowerCase();

            if (user.isEmpty() || user.contains(" ")) {
                System.out.println("Invalid! Username cannot be empty or contain spaces.");
            } else if (userDao.usernameExist(user)) {
                System.out.println("Invalid! Username already taken.");
            } else {
                break;
            }
        }

        String pass;
        while (true) {
            System.out.print("Enter Password: ");
            pass = sc.nextLine().trim();

            if (pass.length() < 5) {
                System.out.println("Invalid! Password must be at least 5 characters.");
                continue;
            }

            boolean confirmed = false;
            for (int attempt = 1; attempt <= 3; attempt++) {
                System.out.print("Confirm Password: ");
                String confirm = sc.nextLine().trim();
                if (confirm.equals(pass)) {
                    confirmed = true;
                    break;
                }
                System.out.println("Passwords do not match! Attempt " + attempt + " of 3.");
            }

            if (confirmed) {
                break;
            }
            System.out.println("Too many wrong attempts. Register Again.");
            return;
        }

        String role;
        while (true) {
            System.out.println("\n-- Select Role --");
            System.out.println("[1] DOCTOR");
            System.out.println("[2] RECEPTIONIST");
            System.out.println("[3] TRIAGE");
            System.out.print("Enter Choice: ");
            String roleChoice = sc.nextLine().trim();

            if (roleChoice.equals("1")) {
                role = "DOCTOR";
                break;
            } else if (roleChoice.equals("2")) {
                role = "RECEPTIONIST";
                break;
            } else if (roleChoice.equals("3")) {
                role = "TRIAGE";
                break;
            } else {
                System.out.println("Invalid! Please select 1, 2, or 3 only.");
            }
        }

        if (userDao.registerUser(fullName, user, pass, role)) {
            System.out.println("\n -> Successful Registration, Waiting for Approval!");
            int newId = userDao.getUserIdByUsername(user);
            if (newId > 0) {
                activityDao.log(newId, "REGISTER", "users", newId,
                        "New " + role + " account registered: " + user);
            }
        } else {
            System.out.println("\n -> [ERROR] Registration failed.");
        }
    }
}