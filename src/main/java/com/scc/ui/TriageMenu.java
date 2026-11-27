package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.DoctorDao;
import com.scc.dao.PatientDao;
import com.scc.dao.TriageDao;
import com.scc.dao.UserDao;
import com.scc.model.Doctor;
import com.scc.model.Patient;
import com.scc.model.TriageRecord;
import com.scc.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Triage / Nurse menu.
 *
 * Process: patient arrives -> nurse records complaint + vital signs -> system suggests a
 * priority (1-4) -> nurse confirms -> patient joins the queue (most urgent first, then
 * first come first served) -> nurse assigns a doctor -> consultation starts -> done.
 */
public class TriageMenu {

    private final ActivityLogDao activityDao;
    private final Scanner sc;
    private final ClinicUi ui;

    private final TriageDao triageDao = new TriageDao();
    private final PatientDao patientDao = new PatientDao();
    private final DoctorDao doctorDao = new DoctorDao();

    public TriageMenu(UserDao userDao, ActivityLogDao activityDao, Scanner sc) {
        this.activityDao = activityDao;
        this.sc = sc;
        this.ui = new ClinicUi(sc);
    }

    public void show(User triage) {
        while (true) {
            System.out.println("\n=====================================");
            System.out.println("        TRIAGE / NURSE MENU");
            System.out.println(" User: " + triage.username());
            System.out.println("=====================================");
            System.out.println("  1. New Triage Assessment (Check-in)");
            System.out.println("  2. View Waiting Queue");
            System.out.println("  3. Reassess Priority");
            System.out.println("  4. Assign Doctor");
            System.out.println("  5. Update Triage Status");
            System.out.println("  6. View Patient Triage History");
            System.out.println("  7. View Today's Triage Log");
            System.out.println("  8. Logout");

            int choice = InputHelper.readChoice("Choose (1-8): ", 1, 8);
            switch (choice) {
                case 1 -> newAssessment(triage);
                case 2 -> viewQueue();
                case 3 -> reassessPriority(triage);
                case 4 -> assignDoctor(triage);
                case 5 -> updateStatus(triage);
                case 6 -> viewHistory();
                case 7 -> recordTable(triageDao.getToday(), "TODAY'S TRIAGE LOG");
                case 8 -> {
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

    // ---------------------------------------------------------------- 1. new assessment

    private void newAssessment(User user) {
        System.out.println("\n===== NEW TRIAGE ASSESSMENT =====");

        Patient patient = pickPatient();
        if (patient == null) {
            return;
        }
        System.out.println(" Patient: " + patient);

        if (triageDao.hasActiveEntry(patient.patient_id())) {
            System.out.println(" -> [WARNING] This patient is already in the active triage queue.");
            if (!InputHelper.confirm("Create another assessment anyway?")) {
                System.out.println(" -> Cancelled.");
                return;
            }
        }

        System.out.print("Chief complaint / reason for visit (0 to cancel): ");
        String complaint = sc.nextLine().trim();
        if (complaint.equals("0")) {
            System.out.println(" -> Cancelled.");
            return;
        }
        if (complaint.isEmpty()) {
            System.out.println(" -> [ERROR] Chief complaint cannot be empty.");
            return;
        }

        System.out.println("\n-- Vital signs (press Enter to skip any) --");
        Double temp = readOptionalDouble("Temperature in C (30-45): ", 30, 45);
        Integer sys = readOptionalInt("Blood pressure - systolic (40-300): ", 40, 300);
        Integer dia = sys == null ? null : readOptionalInt("Blood pressure - diastolic (20-200): ", 20, 200);
        Integer hr = readOptionalInt("Heart rate in bpm (20-250): ", 20, 250);
        Integer rr = readOptionalInt("Respiratory rate per min (4-70): ", 4, 70);
        Integer spo2 = readOptionalInt("Oxygen saturation SpO2 % (50-100): ", 50, 100);
        Integer pain = readOptionalInt("Pain score (0-10): ", 0, 10);

        List<String> reasons = new ArrayList<>();
        int suggested = suggestPriority(temp, sys, dia, hr, rr, spo2, pain, reasons);

        System.out.println("\n-- Suggested priority: " + suggested + " (" + TriageRecord.priorityLabel(suggested) + ")");
        if (reasons.isEmpty()) {
            System.out.println("   No abnormal vital signs flagged.");
        } else {
            for (String r : reasons) {
                System.out.println("   * " + r);
            }
        }
        System.out.println("   (Guideline only - the nurse's clinical judgment decides.)");

        int priority = readPriority("Final priority 1-4 (Enter = " + suggested + "): ", suggested);

        System.out.print("Notes (Enter to skip): ");
        String notes = ClinicUi.emptyToNull(sc.nextLine().trim());

        if (!InputHelper.confirm("Save triage for '" + patient.full_name() + "' as P" + priority
                + " " + TriageRecord.priorityLabel(priority) + "?")) {
            System.out.println(" -> Cancelled.");
            return;
        }

        int id = triageDao.addTriage(patient.patient_id(), user.id(), complaint, temp, sys, dia,
                hr, rr, spo2, pain, priority, notes);
        if (id > 0) {
            System.out.println(" -> Triage saved (ID: " + id + "). Patient added to the waiting queue.");
            if (priority == 1) {
                System.out.println(" -> [!] EMERGENCY - notify a doctor immediately.");
            }
            activityDao.log(user.id(), "CREATE", "triage_assessments", id,
                    user.full_name() + " triaged " + patient.full_name() + " as P" + priority
                            + " (" + TriageRecord.priorityLabel(priority) + ")");
        } else {
            System.out.println(" -> [ERROR] Could not save triage record.");
        }
        InputHelper.pause();
    }

    /**
     * Simple vital-sign based guideline. Returns 1 (most urgent) to 4 (least urgent) and
     * fills 'reasons' with what triggered the level.
     */
    private int suggestPriority(Double temp, Integer sys, Integer dia, Integer hr, Integer rr,
                                Integer spo2, Integer pain, List<String> reasons) {
        int level = 4;

        if (spo2 != null) {
            if (spo2 < 90) level = flag(level, 1, reasons, "SpO2 " + spo2 + "% (critical)");
            else if (spo2 < 94) level = flag(level, 2, reasons, "SpO2 " + spo2 + "% (low)");
        }
        if (hr != null) {
            if (hr < 40 || hr > 150) level = flag(level, 1, reasons, "Heart rate " + hr + " bpm (critical)");
            else if (hr < 50 || hr >= 120) level = flag(level, 2, reasons, "Heart rate " + hr + " bpm (abnormal)");
            else if (hr >= 100) level = flag(level, 3, reasons, "Heart rate " + hr + " bpm (elevated)");
        }
        if (sys != null) {
            if (sys < 80 || sys > 200) level = flag(level, 1, reasons, "Systolic BP " + sys + " (critical)");
            else if (sys < 90 || sys >= 180) level = flag(level, 2, reasons, "Systolic BP " + sys + " (abnormal)");
            else if (sys >= 140) level = flag(level, 3, reasons, "Systolic BP " + sys + " (high)");
        }
        if (dia != null && dia >= 90 && (sys == null || sys < 180)) {
            level = flag(level, 3, reasons, "Diastolic BP " + dia + " (high)");
        }
        if (rr != null) {
            if (rr < 8 || rr > 30) level = flag(level, 1, reasons, "Respiratory rate " + rr + "/min (critical)");
            else if (rr >= 25) level = flag(level, 2, reasons, "Respiratory rate " + rr + "/min (fast)");
        }
        if (temp != null) {
            if (temp >= 40 || temp < 35) level = flag(level, 1, reasons, "Temperature " + temp + " C (critical)");
            else if (temp >= 39) level = flag(level, 2, reasons, "Temperature " + temp + " C (high fever)");
            else if (temp >= 38) level = flag(level, 3, reasons, "Temperature " + temp + " C (fever)");
        }
        if (pain != null) {
            if (pain >= 8) level = flag(level, 2, reasons, "Pain score " + pain + "/10 (severe)");
            else if (pain >= 5) level = flag(level, 3, reasons, "Pain score " + pain + "/10 (moderate)");
        }
        return level;
    }

    private int flag(int current, int candidate, List<String> reasons, String reason) {
        reasons.add(reason + " -> P" + candidate);
        return Math.min(current, candidate);
    }

    // ---------------------------------------------------------------- 2. queue

    private void viewQueue() {
        List<TriageRecord> queue = triageDao.getActiveQueue();
        recordTable(queue, "WAITING QUEUE (most urgent first)");
        if (!queue.isEmpty()) {
            System.out.println(" Legend: P1 Emergency | P2 Urgent | P3 Less urgent | P4 Non-urgent");
        }
        InputHelper.pause();
    }

    // ---------------------------------------------------------------- 3. reassess

    private void reassessPriority(User user) {
        TriageRecord rec = pickActiveRecord("REASSESS PRIORITY");
        if (rec == null) {
            return;
        }
        System.out.println(" Current: P" + rec.priority() + " " + TriageRecord.priorityLabel(rec.priority()));
        int priority = readPriority("New priority 1-4 (Enter = keep " + rec.priority() + "): ", rec.priority());
        if (priority == rec.priority()) {
            System.out.println(" -> Priority unchanged.");
            return;
        }
        System.out.print("Reason for change (Enter to skip): ");
        String reason = ClinicUi.emptyToNull(sc.nextLine().trim());
        String notes = rec.notes();
        if (reason != null) {
            notes = (notes == null ? "" : notes + " | ") + "Reassessed P" + rec.priority() + "->P" + priority + ": " + reason;
        }

        if (triageDao.updatePriority(rec.triage_id(), priority, notes)) {
            System.out.println(" -> Priority updated to P" + priority + " " + TriageRecord.priorityLabel(priority) + ".");
            activityDao.log(user.id(), "UPDATE", "triage_assessments", rec.triage_id(),
                    user.full_name() + " changed priority of " + rec.patient_name()
                            + " from P" + rec.priority() + " to P" + priority);
        } else {
            System.out.println(" -> [ERROR] Could not update priority.");
        }
    }

    // ---------------------------------------------------------------- 4. assign doctor

    private void assignDoctor(User user) {
        TriageRecord rec = pickActiveRecord("ASSIGN DOCTOR");
        if (rec == null) {
            return;
        }

        List<Doctor> doctors = new ArrayList<>();
        for (Doctor d : doctorDao.getAllDoctors()) {
            if ("ACTIVE".equalsIgnoreCase(d.status())) {
                doctors.add(d);
            }
        }
        if (doctors.isEmpty()) {
            System.out.println(" -> No active doctors available.");
            return;
        }
        System.out.println("\n-- Active doctors --");
        for (Doctor d : doctors) {
            System.out.println("  " + d + " | Days: " + ClinicUi.orNA(d.available_days()));
        }

        int docId = ui.readId("Enter Doctor ID (0 to cancel): ");
        if (docId == 0) {
            System.out.println(" -> Cancelled.");
            return;
        }
        Doctor chosen = null;
        for (Doctor d : doctors) {
            if (d.doctor_id() == docId) {
                chosen = d;
            }
        }
        if (chosen == null) {
            System.out.println(" -> [ERROR] No active doctor with that ID.");
            return;
        }

        if (triageDao.assignDoctor(rec.triage_id(), chosen.doctor_id())) {
            System.out.println(" -> " + rec.patient_name() + " assigned to " + chosen.full_name() + ".");
            activityDao.log(user.id(), "UPDATE", "triage_assessments", rec.triage_id(),
                    user.full_name() + " assigned " + rec.patient_name() + " to " + chosen.full_name());
        } else {
            System.out.println(" -> [ERROR] Could not assign doctor.");
        }
    }

    // ---------------------------------------------------------------- 5. status

    private void updateStatus(User user) {
        TriageRecord rec = pickActiveRecord("UPDATE TRIAGE STATUS");
        if (rec == null) {
            return;
        }
        System.out.println(" Current status: " + rec.status());
        System.out.println("  [1] Start Consultation (IN_CONSULT)");
        System.out.println("  [2] Mark Done (DONE)");
        System.out.println("  [3] Cancel / Left without being seen (CANCELLED)");
        System.out.println("  [0] Back");
        int c = InputHelper.readChoice("Choose (0-3): ", 0, 3);
        if (c == 0) {
            return;
        }

        String newStatus = switch (c) {
            case 1 -> "IN_CONSULT";
            case 2 -> "DONE";
            default -> "CANCELLED";
        };

        if (c == 1) {
            if (rec.doctor_id() == null) {
                System.out.println(" -> [ERROR] Assign a doctor first (option 4).");
                return;
            }
            if (rec.status().equals("IN_CONSULT")) {
                System.out.println(" -> Already in consultation.");
                return;
            }
            if (higherPriorityWaiting(rec)) {
                System.out.println(" -> [WARNING] A more urgent patient is still waiting in the queue.");
            }
        }
        if (c == 2 && !rec.status().equals("IN_CONSULT")
                && !InputHelper.confirm("Patient was never marked IN_CONSULT. Mark done anyway?")) {
            return;
        }
        if (!InputHelper.confirm("Set " + rec.patient_name() + " to " + newStatus + "?")) {
            System.out.println(" -> Cancelled.");
            return;
        }

        if (triageDao.updateStatus(rec.triage_id(), newStatus)) {
            System.out.println(" -> Status updated to " + newStatus + ".");
            activityDao.log(user.id(), "UPDATE", "triage_assessments", rec.triage_id(),
                    user.full_name() + " set triage of " + rec.patient_name() + " to " + newStatus);
        } else {
            System.out.println(" -> [ERROR] Could not update status.");
        }
    }

    private boolean higherPriorityWaiting(TriageRecord rec) {
        for (TriageRecord other : triageDao.getActiveQueue()) {
            if (other.status().equals("WAITING") && other.priority() < rec.priority()) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- 6. history

    private void viewHistory() {
        Patient patient = pickPatient();
        if (patient == null) {
            return;
        }
        recordTable(triageDao.getByPatient(patient.patient_id()), "TRIAGE HISTORY - " + patient.full_name());
        InputHelper.pause();
    }

    // ---------------------------------------------------------------- helpers

    /** Search by name / contact / ID, then choose by ID. Returns null if cancelled. */
    private Patient pickPatient() {
        while (true) {
            System.out.print("Search patient by name, contact or ID (0 to cancel): ");
            String keyword = sc.nextLine().trim();
            if (keyword.equals("0")) {
                System.out.println(" -> Cancelled.");
                return null;
            }
            if (keyword.isEmpty()) {
                System.out.println(" -> [ERROR] Please type something to search.");
                continue;
            }
            List<Patient> found = patientDao.searchPatients(keyword);
            if (found.isEmpty()) {
                System.out.println(" -> No patient found. (Ask the receptionist to register the patient first.)");
                continue;
            }
            ui.patientTable(found, "SEARCH RESULTS");
            int id = ui.readId("Enter Patient ID to select (0 to search again): ");
            if (id == 0) {
                continue;
            }
            for (Patient p : found) {
                if (p.patient_id() == id) {
                    return p;
                }
            }
            System.out.println(" -> [ERROR] That ID is not in the search results.");
        }
    }

    /** Shows the active queue and lets the nurse choose an entry. Returns null if cancelled. */
    private TriageRecord pickActiveRecord(String title) {
        List<TriageRecord> queue = triageDao.getActiveQueue();
        recordTable(queue, title);
        if (queue.isEmpty()) {
            return null;
        }
        while (true) {
            int id = ui.readId("Enter Triage ID (0 to cancel): ");
            if (id == 0) {
                System.out.println(" -> Cancelled.");
                return null;
            }
            for (TriageRecord r : queue) {
                if (r.triage_id() == id) {
                    return r;
                }
            }
            System.out.println(" -> [ERROR] That ID is not in the active queue.");
        }
    }

    private void recordTable(List<TriageRecord> list, String title) {
        System.out.println("\n====================== " + title + " ======================");
        if (list.isEmpty()) {
            System.out.println("\n-> No Records.");
            return;
        }
        String border = "+------+-----+-------------+------------------+----------------------+---------+-----------------+-------------+------------------+";
        String format = "| %-4s | %-3s | %-11s | %-16s | %-20s | %-7s | %-15s | %-11s | %-16s |\n";
        System.out.println();
        System.out.println(border);
        System.out.printf(format, "ID", "Pri", "Level", "Patient", "Complaint", "BP", "Doctor", "Status", "Arrived");
        System.out.println(border);
        for (TriageRecord r : list) {
            System.out.printf(format, r.triage_id(), "P" + r.priority(),
                    TriageRecord.priorityLabel(r.priority()), ClinicUi.cut(r.patient_name(), 16),
                    ClinicUi.cut(r.chief_complaint(), 20), r.bloodPressure(),
                    ClinicUi.cut(ClinicUi.orNA(r.doctor_name()), 15), r.status(),
                    ClinicUi.cut(ClinicUi.orNA(r.arrival_time()), 16));
        }
        System.out.println(border);
        System.out.println(" Total: " + list.size() + " record(s)");
    }

    private int readPriority(String prompt, int defaultValue) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            if (s.isEmpty()) {
                return defaultValue;
            }
            if (s.matches("[1-4]")) {
                return Integer.parseInt(s);
            }
            System.out.println(" -> [ERROR] Enter 1 (Emergency), 2 (Urgent), 3 (Less urgent) or 4 (Non-urgent).");
        }
    }

    /** Enter = skip (null). Re-prompts on invalid input or out-of-range values. */
    private Integer readOptionalInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            if (s.isEmpty()) {
                return null;
            }
            try {
                int n = Integer.parseInt(s);
                if (n >= min && n <= max) {
                    return n;
                }
            } catch (NumberFormatException ignored) {
            }
            System.out.println(" -> [ERROR] Enter a whole number from " + min + " to " + max + " (or Enter to skip).");
        }
    }

    private Double readOptionalDouble(String prompt, double min, double max) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            if (s.isEmpty()) {
                return null;
            }
            try {
                double n = Double.parseDouble(s);
                if (n >= min && n <= max) {
                    return n;
                }
            } catch (NumberFormatException ignored) {
            }
            System.out.println(" -> [ERROR] Enter a number from " + min + " to " + max + " (or Enter to skip).");
        }
    }
}