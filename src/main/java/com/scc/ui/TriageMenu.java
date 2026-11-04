package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.Scanner;


public class TriageMenu {

    private final UserDao userDao;
    private final ActivityLogDao activityDao;
    private final Scanner sc;

    public TriageMenu(UserDao userDao, ActivityLogDao activityDao, Scanner sc){
        this.userDao = userDao;
        this.activityDao = activityDao;
        this.sc = sc;
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
                case 1 -> comingSoon("View Daily Queue");
                case 2 -> comingSoon("Record Patient Vitals");
                case 3 -> comingSoon("Update Triage Status");
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

    private void comingSoon(String feature) {
        System.out.println("\n[" + feature + "] coming soon.");
        InputHelper.pause();
    }
}
