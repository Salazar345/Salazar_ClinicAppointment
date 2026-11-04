package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.Scanner;


public class ReceptionistMenu {

    private final UserDao userDao;
    private final ActivityLogDao activityDao;
    private final Scanner sc;

    public ReceptionistMenu(UserDao userDao, ActivityLogDao activityDao, Scanner sc){
        this.userDao = userDao;
        this.activityDao = activityDao;
        this.sc = sc;
    }

    public void show(User receptionist) {
        while (true) {
            System.out.println("\n=====================================");
            System.out.println("            RECEPTIONIST MENU ");
            System.out.println("User: " + receptionist.username());
            System.out.println("=====================================");
            System.out.println("  1. Register / Edit Patient");
            System.out.println("  2. Search Patients");
            System.out.println("  3. Book Appointment");
            System.out.println("  4. Reschedule Appointment");
            System.out.println("  5. Cancel Appointment");
            System.out.println("  6. View Daily Schedule");
            System.out.println("  7. Logout");

            int choice = InputHelper.readChoice("Choose (1-7): ", 1, 7);
            switch (choice) {
                case 1 -> comingSoon("Register / Edit Patient");
                case 2 -> comingSoon("Search Patients");
                case 3 -> comingSoon("Book Appointment");
                case 4 -> comingSoon("Reschedule Appointment");
                case 5 -> comingSoon("Cancel Appointment");
                case 6 -> comingSoon("View Daily Schedule");
                case 7 -> {
                    if (InputHelper.confirm("Are you sure you want to logout?")) {
                        System.out.println("Goodbye, " + receptionist.username() + "! You are logged out.");
                        activityDao.log(receptionist.id(), "LOGOUT", "users", receptionist.id(),
                                receptionist.full_name() + " logged out");
                        return;
                    }
                }
            }
        }
    }

    private void comingSoon(String feature) {
        System.out.println("\n[" + feature + "] - coming soon.");
        InputHelper.pause();
    }
}
