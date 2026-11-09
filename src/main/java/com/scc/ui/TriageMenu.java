package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.Scanner;

public class TriageMenu {

    private final ActivityLogDao activityDao;

    public TriageMenu(UserDao userDao, ActivityLogDao activityDao, Scanner sc) {
        this.activityDao = activityDao;
    }

    public void show(User triage) {
        System.out.println("\n=====================================");
        System.out.println("        TRIAGE / NURSE MENU");
        System.out.println(" User: " + triage.username());
        System.out.println("=====================================");
        System.out.println("\n -> Coming soon!");
        InputHelper.pause();

        System.out.println("Goodbye, " + triage.username() + "! You are logged out.");
        activityDao.log(triage.id(), "LOGOUT", "users", triage.id(),
                triage.full_name() + " logged out");
    }
}