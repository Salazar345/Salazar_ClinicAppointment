package com.scc.util;

import java.util.Scanner;

/** Small reusable console helpers (validation loop + text trimming). */
public final class ConsoleUtil {

    private ConsoleUtil() {
    }

    /** Shortens text so it fits a printf column. */
    public static String cut(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, Math.max(0, max - 3)) + "...";
    }

    /**
     * Re-prompts until the user types a whole number >= 0 (no crash on bad input).
     * Returns 0 when the user types 0 (used as "cancel").
     */
    public static int readNonNegativeInt(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = sc.nextLine().trim();
            try {
                int n = Integer.parseInt(line);
                if (n >= 0) {
                    return n;
                }
                System.out.println("  [!] Please enter 0 or a positive number.");
            } catch (NumberFormatException e) {
                System.out.println("  [!] '" + line + "' is not a valid number. Try again.");
            }
        }
    }
}