package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.AppointmentManageDao;
import com.scc.dao.DoctorDao;
import com.scc.dao.PatientDao;
import com.scc.dao.TriageDao;
import com.scc.dao.UserDao;
import com.scc.model.Appointment;
import com.scc.model.Doctor;
import com.scc.model.Patient;
import com.scc.model.TriageRecord;
import com.scc.model.User;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class DoctorMenu {

    private final UserDao userdao;
    private final ActivityLogDao activityDao;
    private final Scanner sc;
    private final ClinicUi ui;

    private final DoctorDao doctorDao = new DoctorDao();
    private final PatientDao patientDao = new PatientDao();
    private final AppointmentManageDao appointmentDao = new AppointmentManageDao();
    private final TriageDao triageDao = new TriageDao();

    public DoctorMenu(UserDao userDao, ActivityLogDao activityDao, Scanner sc) {
        this.userdao = userDao;
        this.activityDao = activityDao;
        this.sc = sc;
        this.ui = new ClinicUi(sc);
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
                case 1 -> viewSchedule(doctor);
                case 2 -> viewAssignedPatient(doctor);
                case 3 -> updateAppointmentStatus(doctor);
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

    /** The doctor record linked to this login, or null (with a message) if none. */
    private Doctor myRecord(User user) {
        Doctor me = doctorDao.getDoctorByUserId(user.id());
        if (me == null) {
            System.out.println("\n -> [ERROR] Your account is not linked to a doctor record yet.");
            System.out.println("    Please ask the administrator (Manage Doctor Records) to link it.");
            InputHelper.pause();
        }
        return me;
    }

    private List<Appointment> sortAsc(List<Appointment> list) {
        return list.stream()
                .sorted(Comparator.comparing(Appointment::appointment_date)
                        .thenComparing(Appointment::appointment_time))
                .toList();
    }

    // ---------------------------------------------------------------- 1. schedule

    private void viewSchedule(User user) {
        Doctor me = myRecord(user);
        if (me == null) return;

        while (true) {
            System.out.println("\n[1] Today\n[2] Upcoming (Scheduled)\n[3] By Date\n[4] All My Appointments\n[0] Back");
            System.out.print("Enter Choice: ");
            String choice = sc.nextLine().trim();

            String today = LocalDate.now().toString();
            List<Appointment> mine = appointmentDao.getByDoctor(me.doctor_id());

            switch (choice) {
                case "1" -> ui.appointmentTable(sortAsc(mine.stream()
                        .filter(a -> a.appointment_date().equals(today)).toList()), "MY SCHEDULE TODAY (" + today + ")");
                case "2" -> ui.appointmentTable(sortAsc(mine.stream()
                        .filter(a -> "SCHEDULED".equalsIgnoreCase(a.status()))
                        .filter(a -> a.appointment_date().compareTo(today) >= 0).toList()), "MY UPCOMING APPOINTMENTS");
                case "3" -> {
                    String date = ui.readDate("Enter Date (YYYY-MM-DD, 0 to cancel): ", false);
                    if (date == null) {
                        System.out.println(" -> Cancelled.");
                    } else {
                        ui.appointmentTable(sortAsc(mine.stream()
                                .filter(a -> a.appointment_date().equals(date)).toList()), "MY APPOINTMENTS ON " + date);
                    }
                }
                case "4" -> ui.appointmentTable(mine, "ALL MY APPOINTMENTS");
                case "0" -> {
                    return;
                }
                default -> System.out.println("-> Invalid choice! 0 to 4 only.");
            }
        }
    }

    // ---------------------------------------------------------------- 2. patient details

    private void viewAssignedPatient(User user) {
        Doctor me = myRecord(user);
        if (me == null) return;

        List<Appointment> mine = appointmentDao.getByDoctor(me.doctor_id());
        if (mine.isEmpty()) {
            System.out.println("\n-> You have no assigned patients yet.");
            InputHelper.pause();
            return;
        }

        Map<Integer, String> patients = new LinkedHashMap<>();
        for (Appointment a : mine) {
            patients.putIfAbsent(a.patient_id(), a.patient_name());
        }

        System.out.println("\n--- My Patients ---");
        for (Map.Entry<Integer, String> e : patients.entrySet()) {
            System.out.println("  [" + e.getKey() + "] " + e.getValue());
        }

        int patientId = ui.readId("Enter Patient ID (0 to cancel): ");
        if (patientId == 0) {
            System.out.println(" -> Cancelled.");
            return;
        }
        if (!patients.containsKey(patientId)) {
            System.out.println(" -> [ERROR] That patient is not assigned to you.");
            return;
        }

        Patient p = patientDao.getPatientById(patientId);
        if (p == null) {
            System.out.println(" -> [ERROR] Patient record not found.");
            return;
        }

        System.out.println("\n====================== PATIENT DETAILS ======================");
        System.out.println(" ID             : " + p.patient_id());
        System.out.println(" Full Name      : " + p.full_name());
        System.out.println(" Birth Date     : " + ClinicUi.orNA(p.birth_date()));
        System.out.println(" Gender         : " + ClinicUi.orNA(p.gender()));
        System.out.println(" Contact        : " + ClinicUi.orNA(p.contact_number()));
        System.out.println(" Address        : " + ClinicUi.orNA(p.address()));
        System.out.println(" Date Registered: " + ClinicUi.orNA(p.date_registered()));

        TriageRecord vitals = triageDao.getLatestForPatient(patientId);
        System.out.println("\n--- Latest Recorded Vitals ---");
        if (vitals == null) {
            System.out.println(" No vitals recorded yet.");
        } else {
            ui.printVitals(vitals);
        }

        ui.appointmentTable(mine.stream().filter(a -> a.patient_id() == patientId).toList(),
                "MY APPOINTMENTS WITH " + p.full_name().toUpperCase());
        InputHelper.pause();
    }

    // ---------------------------------------------------------------- 3. update status

    private void updateAppointmentStatus(User user) {
        Doctor me = myRecord(user);
        if (me == null) return;

        List<Appointment> scheduled = sortAsc(appointmentDao.getByDoctor(me.doctor_id()).stream()
                .filter(a -> "SCHEDULED".equalsIgnoreCase(a.status())).toList());

        ui.appointmentTable(scheduled, "SCHEDULED APPOINTMENTS");
        if (scheduled.isEmpty()) {
            return;
        }

        int id = ui.readId("Enter Appointment ID to update (0 to cancel): ");
        if (id == 0) {
            System.out.println(" -> Cancelled.");
            return;
        }

        Appointment target = scheduled.stream().filter(a -> a.appointment_id() == id).findFirst().orElse(null);
        if (target == null) {
            System.out.println(" -> [ERROR] Appointment ID not found in your scheduled list.");
            return;
        }

        System.out.println("\n[1] COMPLETED\n[2] CANCELLED\n[0] Cancel");
        System.out.print("New status: ");
        String s = sc.nextLine().trim();
        String newStatus;
        switch (s) {
            case "1" -> newStatus = "COMPLETED";
            case "2" -> newStatus = "CANCELLED";
            default -> {
                System.out.println(" -> Cancelled.");
                return;
            }
        }

        if (!InputHelper.confirm("Mark appointment " + id + " (" + target.patient_name() + ") as " + newStatus + "?")) {
            System.out.println(" -> Cancelled.");
            return;
        }

        if (appointmentDao.updateStatus(id, newStatus)) {
            System.out.println(" -> Appointment " + id + " is now " + newStatus + ".");
            activityDao.log(user.id(), "UPDATE", "appointments", id,
                    "Doctor set appointment status to " + newStatus + " (" + target.patient_name() + ")");
        } else {
            System.out.println(" -> [ERROR] Failed to update appointment.");
        }
        InputHelper.pause();
    }
}