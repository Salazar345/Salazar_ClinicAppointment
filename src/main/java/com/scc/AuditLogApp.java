package com.scc;

import com.scc.dao.DatabaseInitializer;
import com.scc.dao.UserDao;
import com.scc.model.User;
import com.scc.ui.AuditLogMenu;

import java.util.Scanner;

/**
 * Optional standalone launcher for the audit-log viewer (with Filter by User ID).
 * Run this class instead of Main when you only need to demo / screenshot the audit logs.
 * It still enforces RBAC: only an APPROVED ADMIN account can open the viewer.
 * Nothing in the existing project needs to be edited to use it.
 */
public class AuditLogApp {

    public static void main(String[] args) {
        DatabaseInitializer.initialize();
        UserDao userDao = new UserDao();
        Scanner sc = new Scanner(System.in);

        System.out.println("\n===== ADMIN AUDIT LOG VIEWER =====");
        System.out.print("Enter Username: ");
        String username = sc.nextLine().trim();
        System.out.print("Enter Password: ");
        String password = sc.nextLine().trim();

        User user = userDao.login(username, password);   // blocks PENDING / REJECTED accounts
        if (user == null) {
            sc.close();
            return;
        }
        if (!"ADMIN".equalsIgnoreCase(user.role())) {
            System.out.println("\n -> Access denied. Only ADMIN accounts can view audit logs.");
            sc.close();
            return;
        }

        new AuditLogMenu(sc).show();
        sc.close();
    }
}