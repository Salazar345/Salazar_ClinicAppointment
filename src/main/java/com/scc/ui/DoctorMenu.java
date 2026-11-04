package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.Scanner;

public class DoctorMenu {

    private final UserDao userdao;
    private final ActivityLogDao activityDao;
    private final Scanner sc;

    public DoctorMenu(UserDao userDao, ActivityLogDao activityDao,Scanner sc){
        this.userdao = userDao;
        this.activityDao = activityDao;
        this.sc = sc;
    }

    public void show(User doctor) {
        while (true) {
            System.out.println("\n=====================================");
            System.out.println("            DOCTOR MENU ");
            System.out.println(" User : " + doctor.username());
            System.out.println("=====================================");
            System.out.println("  1. View My Appointment Schedule");
            System.out.println("  2. View Assigned Patient Details");
            System.out.println("  3. Update Appointment Status");
            System.out.println("  4. Logout");

            int choice = InputHelper.readChoice("Choose (1-4): ", 1, 4);
            switch (choice) {
                case 1 -> comingSoon("View My Appointment Schedule");
                case 2 -> comingSoon("View Assigned Patient Details");
                case 3 -> comingSoon("Update Appointment Status");
                case 4 -> {
                    if (InputHelper.confirm("Are you sure you want to logout?")) {
                        System.out.println("Goodbye, " + doctor.username() + "! You are logged out.");
                        activityDao.log(doctor.id(), "LOGOUT", "users", doctor.id(),
                                doctor.full_name() + " logged out");
                        return;   // back to the main menu
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
