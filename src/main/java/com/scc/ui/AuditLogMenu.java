package com.scc.ui;

import com.scc.dao.ActivityLogQueryDao;
import com.scc.model.ActivityLogs;
import com.scc.util.ConsoleUtil;

import java.util.List;
import java.util.Scanner;

/**
 * Admin audit-log viewer with filters: Show All, Action, Table, User ID, Recent 20.
 * No SQL here - all queries are in ActivityLogQueryDao / ActivityLogDao.
 */
public class AuditLogMenu {

    private final ActivityLogQueryDao logDao = new ActivityLogQueryDao();
    private final Scanner sc;

    public AuditLogMenu(Scanner sc) {
        this.sc = sc;
    }

    public void show() {
        List<ActivityLogs> current = logDao.getRecentLogs(20);
        String title = "RECENT LOGS (LAST 20)";

        while (true) {
            displayLogTable(current, title);

            System.out.println("\n--------- FILTER ---------");
            System.out.println("( [1] Show All | [2] Filter by Action | [3] Filter by Table | "
                    + "[4] Filter by User ID | [5] Refresh (Recent 20) | [0] Back )");
            System.out.print("Enter Choice: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> {
                    current = logDao.getAllLogs();
                    title = "ALL LOGS";
                }
                case "2" -> {
                    System.out.println("\nActions: LOGIN, LOGOUT, LOGIN_FAILED, REGISTER, CREATE, UPDATE, DELETE, APPROVED");
                    System.out.print("Enter Action (0 to cancel): ");
                    String action = sc.nextLine().trim().toUpperCase();
                    if (action.isEmpty() || action.equals("0")) {
                        System.out.println(" -> Cancelled.");
                    } else {
                        current = logDao.getLogsByAction(action);
                        title = "LOGS: ACTION = " + action;
                    }
                }
                case "3" -> {
                    System.out.println("\nTables: users, patients, doctors, appointments");
                    System.out.print("Enter Table (0 to cancel): ");
                    String table = sc.nextLine().trim().toLowerCase();
                    if (table.isEmpty() || table.equals("0")) {
                        System.out.println(" -> Cancelled.");
                    } else {
                        current = logDao.getLogsByTable(table);
                        title = "LOGS: TABLE = " + table;
                    }
                }
                case "4" -> {
                    int userId = ConsoleUtil.readNonNegativeInt(sc, "\nEnter User ID (0 to cancel): ");
                    if (userId == 0) {
                        System.out.println(" -> Cancelled.");
                    } else {
                        current = logDao.getLogsByUserId(userId);
                        title = "LOGS: USER ID = " + userId;
                    }
                }
                case "5" -> {
                    current = logDao.getRecentLogs(20);
                    title = "RECENT LOGS (LAST 20)";
                }
                case "0" -> {
                    return;
                }
                default -> System.out.println("-> Invalid choice! 0 to 5 only.");
            }
        }
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
                    ConsoleUtil.cut(l.action(), 14),
                    l.table_affected() != null ? ConsoleUtil.cut(l.table_affected(), 10) : "N/A",
                    l.record_id() != null ? l.record_id() : "N/A",
                    l.details() != null ? ConsoleUtil.cut(l.details(), 38) : "",
                    l.log_timestamp());
        }

        System.out.println(border);
        System.out.println(" Total: " + logs.size() + " record(s)");
    }
}