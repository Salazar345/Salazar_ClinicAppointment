package com.scc.ui;

import java.util.Scanner;

public final class InputHelper {

    private static Scanner SC = new Scanner(System.in);

    private InputHelper() {
    }

    /** Gitawag sa Main aron parehas ang Scanner sa tibuok app. */
    public static void init(Scanner scanner) {
        SC = scanner;
    }

    public static String readText(String prompt) {
        while (true) {
            System.out.print(prompt);
            if (!SC.hasNextLine()) {
                System.out.println("\nInput closed. Exiting...");
                System.exit(0);
            }
            String line = SC.nextLine().trim();
            if (!line.isEmpty()) {
                return line;
            }
            System.out.println("  [!] This field cannot be empty. Please try again.");
        }
    }

    public static int readChoice(String prompt, int min, int max) {
        while (true) {
            String line = readText(prompt);
            try {
                int n = Integer.parseInt(line);
                if (n >= min && n <= max) {
                    return n;
                }
                System.out.println("  [!] Please enter a number from " + min + " to " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("  [!] '" + line + "' is not a number. Please enter " + min + "-" + max + ".");
            }
        }
    }

    public static boolean confirm(String prompt) {
        while (true) {
            String a = readText(prompt + " (y/n): ").toLowerCase();
            if (a.equals("y") || a.equals("yes")) return true;
            if (a.equals("n") || a.equals("no")) return false;
            System.out.println("  [!] Please type y or n.");
        }
    }

    public static void pause() {
        System.out.print("\nPress Enter to continue...");
        if (SC.hasNextLine()) SC.nextLine();
    }
}