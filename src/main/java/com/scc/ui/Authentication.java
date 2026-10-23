package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.Scanner;

public class Authentication {

    private final UserDao userDao;
    private final Scanner sc;

    public Authentication(UserDao userDao, Scanner sc) {
        this.userDao = userDao;
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

        if (loggedIn != null) {
            System.out.println("\nSuccessfully Login");
            System.out.println("\n -> Welcome " + loggedIn.full_name() + " | ROLE: " + loggedIn.role());
            switch (loggedIn.role()) {
                case "ADMIN"        -> new AdminMenu(userDao, sc).show(loggedIn);
                case "DOCTOR"       -> new DoctorMenu(userDao, sc).show(loggedIn);
                case "RECEPTIONIST" -> new ReceptionistMenu(userDao, sc).show(loggedIn);
                case "TRIAGE"       -> new TriageMenu(userDao, sc).show(loggedIn);
                default             -> System.out.println("Unknown role: " + loggedIn.role());
            }
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

            if (!user.endsWith("@gmail.com") || user.length() <= "@gmail.com".length() || user.contains(" ")) {
                System.out.println("Invalid! Enter a valid Gmail without spaces (example: lito@gmail.com).");
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
            System.out.println("[1] ADMIN");
            System.out.println("[2] DOCTOR");
            System.out.println("[3] RECEPTIONIST");
            System.out.println("[4] TRIAGE");
            System.out.print("Enter Choice: ");
            String roleChoice = sc.nextLine().trim();

            if (roleChoice.equals("1")) {
                role = "ADMIN";
                break;
            } else if (roleChoice.equals("2")) {
                role = "DOCTOR";
                break;
            } else if (roleChoice.equals("3")) {
                role = "RECEPTIONIST";
                break;
            } else if (roleChoice.equals("4")) {
                role = "TRIAGE";
                break;
            } else {
                System.out.println("Invalid! Please select 1, 2, 3, or 4 only.");
            }
        }

        userDao.registerUser(fullName, user, pass, role);
    }
}