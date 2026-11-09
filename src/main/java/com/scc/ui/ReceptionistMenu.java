package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.AppointmentManageDao;
import com.scc.dao.DoctorDao;
import com.scc.dao.PatientManageDao;
import com.scc.dao.UserDao;
import com.scc.model.Appointment;
import com.scc.model.Doctor;
import com.scc.model.Patient;
import com.scc.model.User;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

public class ReceptionistMenu {

    private final UserDao userDao;
    private final ActivityLogDao activityDao;
    private final Scanner sc;
    private final ClinicUi ui;

    private final PatientManageDao patientDao = new PatientManageDao();
    private final DoctorDao doctorDao = new DoctorDao();
    private final AppointmentManageDao appointmentDao = new AppointmentManageDao();

    public ReceptionistMenu(UserDao userDao, ActivityLogDao activityDao, Scanner sc) {
        this.userDao = userDao;
        this.activityDao = activityDao;
        this.sc = sc;
        this.ui = new ClinicUi(sc);
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
                case 1 -> registerOrEditPatient(receptionist);
                case 2 -> searchPatients();
                case 3 -> bookAppointment(receptionist);
                case 4 -> rescheduleAppointment(receptionist);
                case 5 -> cancelAppointment(receptionist);
                case 6 -> viewDailySchedule();
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

    // ---------------------------------------------------------------- 1. register / edit patient

    private void registerOrEditPatient(User user) {
        while (true) {
            System.out.println("\n[1] Register New Patient\n[2] Edit Existing Patient\n[0] Back");
            System.out.print("Enter Choice: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> registerPatient(user);
                case "2" -> editPatient(user);
                case "0" -> {
                    return;
                }
                default -> System.out.println("-> Invalid choice! 0 to 2 only.");
            }
        }
    }

    private void registerPatient(User user) {
        System.out.println("\n===== REGISTER PATIENT =====");

        System.out.print("Enter Full Name (0 to cancel): ");
        String fullName = sc.nextLine().trim();
        if (fullName.equals("0")) {
            System.out.println(" -> Cancelled.");
            return;
        }
        if (fullName.isEmpty()) {
            System.out.println(" -> [ERROR] Full name cannot be empty.");
            return;
        }

        String birth = ui.readOptionalBirthDate("Enter Birth Date YYYY-MM-DD (Enter to skip): ");
        String gender = readGender(null);

        System.out.print("Enter Contact Number (Enter to skip): ");
        String contact = ClinicUi.emptyToNull(sc.nextLine().trim());

        System.out.print("Enter Address (Enter to skip): ");
        String address = ClinicUi.emptyToNull(sc.nextLine().trim());

        if (!InputHelper.confirm("Register patient '" + fullName + "'?")) {
            System.out.println(" -> Cancelled.");
            return;
        }

        int newId = patientDao.addPatient(fullName, birth, gender, contact, address, user.id());
        if (newId > 0) {
            System.out.println(" -> Patient registered (ID: " + newId + ").");
        } else {
            System.out.println(" -> [ERROR] Failed to register patient.");
        }
    }

    private void editPatient(User user) {
        List<Patient> patients = patientDao.getAllPatients();
        ui.patientTable(patients, "SELECT PATIENT TO EDIT");
        if (patients.isEmpty()) return;

        int id = ui.readId("Enter Patient ID to edit (0 to cancel): ");
        if (id == 0) {
            System.out.println(" -> Cancelled.");
            return;
        }

        Patient cur = patients.stream().filter(p -> p.patient_id() == id).findFirst().orElse(null);
        if (cur == null) {
            System.out.println(" -> [ERROR] Patient ID not found.");
            return;
        }

        System.out.println("\n(Press Enter to keep the current value)");

        System.out.print("Full Name [" + cur.full_name() + "]: ");
        String fullName = sc.nextLine().trim();
        if (fullName.isEmpty()) fullName = cur.full_name();

        String birth = cur.birth_date();
        String newBirth = ui.readOptionalBirthDate("Birth Date [" + ClinicUi.orNA(cur.birth_date()) + "]: ");
        if (newBirth != null) birth = newBirth;

        String gender = readGender(cur.gender());

        System.out.print("Contact Number [" + ClinicUi.orNA(cur.contact_number()) + "]: ");
        String contact = sc.nextLine().trim();
        if (contact.isEmpty()) contact = cur.contact_number();

        System.out.print("Address [" + ClinicUi.orNA(cur.address()) + "]: ");
        String address = sc.nextLine().trim();
        if (address.isEmpty()) address = cur.address();

        if (!InputHelper.confirm("Save changes to patient " + id + "?")) {
            System.out.println(" -> Cancelled.");
            return;
        }

        if (patientDao.updatePatient(id, fullName, birth, gender,
                ClinicUi.emptyToNull(contact), ClinicUi.emptyToNull(address), user.id())) {
            System.out.println(" -> Patient ID " + id + " updated successfully!");
        } else {
            System.out.println(" -> [ERROR] Failed to update patient.");
        }
    }

    /** current == null: Enter skips. Otherwise Enter keeps current. */
    private String readGender(String current) {
        while (true) {
            if (current == null) {
                System.out.print("Enter Gender (Male/Female/Other, Enter to skip): ");
            } else {
                System.out.print("Gender [" + ClinicUi.orNA(current) + "] (Male/Female/Other, Enter = keep): ");
            }
            String g = sc.nextLine().trim();
            if (g.isEmpty()) return current;
            switch (g.toUpperCase()) {
                case "M", "MALE" -> {
                    return "Male";
                }
                case "F", "FEMALE" -> {
                    return "Female";
                }
                case "O", "OTHER" -> {
                    return "Other";
                }
                default -> System.out.println(" -> [ERROR] Use Male, Female or Other.");
            }
        }
    }

    // ---------------------------------------------------------------- 2. search

    private void searchPatients() {
        System.out.print("Enter name / contact / ID (0 to cancel): ");
        String keyword = sc.nextLine().trim();
        if (keyword.equals("0") || keyword.isEmpty()) {
            System.out.println(" -> Cancelled.");
            return;
        }
        ui.patientTable(patientDao.searchPatients(keyword), "SEARCH: " + keyword);
        InputHelper.pause();
    }

    // ---------------------------------------------------------------- 3. book

    private void bookAppointment(User user) {
        System.out.println("\n===== BOOK APPOINTMENT =====");

        Patient patient = pickPatient();
        if (patient == null) return;

        Doctor doctor = pickDoctor();
        if (doctor == null) return;

        String[] slot = readSlot(doctor.doctor_id(), -1);
        if (slot == null) return;

        System.out.print("Enter Reason / Complaint (Enter to skip): ");
        String reason = ClinicUi.emptyToNull(sc.nextLine().trim());

        if (!InputHelper.confirm("Book " + patient.full_name() + " with Dr. " + doctor.full_name()
                + " on " + slot[0] + " " + slot[1] + "?")) {
            System.out.println(" -> Cancelled.");
            return;
        }

        int newId = appointmentDao.book(patient.patient_id(), doctor.doctor_id(),
                slot[0], slot[1], reason, user.id());
        if (newId > 0) {
            System.out.println(" -> Appointment booked (ID: " + newId + ").");
        } else {
            System.out.println(" -> [ERROR] Failed to book appointment.");
        }
        InputHelper.pause();
    }

    /** Patient picker: ID, or S to search. Returns null if cancelled. */
    private Patient pickPatient() {
        while (true) {
            System.out.print("Enter Patient ID (S = search, 0 to cancel): ");
            String input = sc.nextLine().trim();

            if (input.equals("0")) {
                System.out.println(" -> Cancelled.");
                return null;
            }
            if (input.equalsIgnoreCase("S")) {
                System.out.print("Enter name / contact: ");
                String keyword = sc.nextLine().trim();
                if (!keyword.isEmpty()) {
                    ui.patientTable(patientDao.searchPatients(keyword), "SEARCH: " + keyword);
                }
                continue;
            }
            try {
                Patient p = patientDao.getPatientById(Integer.parseInt(input));
                if (p != null) return p;
                System.out.println(" -> [ERROR] Patient ID not found.");
            } catch (NumberFormatException e) {
                System.out.println(" -> [ERROR] Enter a numeric ID, S, or 0.");
            }
        }
    }

    private Doctor pickDoctor() {
        List<Doctor> active = doctorDao.getAllDoctors().stream()
                .filter(d -> "ACTIVE".equalsIgnoreCase(d.status())).toList();

        if (active.isEmpty()) {
            System.out.println("\n-> No active doctors available.");
            return null;
        }

        System.out.println("\n--- Active Doctors ---");
        for (Doctor d : active) {
            System.out.println("  " + d + " | Days: " + ClinicUi.orNA(d.available_days()));
        }

        while (true) {
            int id = ui.readId("Enter Doctor ID (0 to cancel): ");
            if (id == 0) {
                System.out.println(" -> Cancelled.");
                return null;
            }
            Doctor d = active.stream().filter(x -> x.doctor_id() == id).findFirst().orElse(null);
            if (d != null) return d;
            System.out.println(" -> [ERROR] Pick an ID from the list above.");
        }
    }

    /** Asks date + time, checks past dates and doctor conflicts. Returns {date, time} or null. */
    private String[] readSlot(int doctorId, int excludeAppointmentId) {
        while (true) {
            String date = ui.readDate("Enter Date (YYYY-MM-DD, 0 to cancel): ", false);
            if (date == null) {
                System.out.println(" -> Cancelled.");
                return null;
            }
            if (date.compareTo(LocalDate.now().toString()) < 0) {
                System.out.println(" -> [ERROR] Date cannot be in the past.");
                continue;
            }

            while (true) {
                String time = ui.readTime("Enter Time (HH:mm, 0 to change date): ");
                if (time == null) break;

                if (date.equals(LocalDate.now().toString()) && LocalTime.parse(time).isBefore(LocalTime.now())) {
                    System.out.println(" -> [ERROR] That time has already passed today.");
                    continue;
                }
                if (appointmentDao.hasConflict(doctorId, date, time, excludeAppointmentId)) {
                    System.out.println(" -> [ERROR] The doctor already has an appointment at that time.");
                    continue;
                }
                return new String[]{date, time};
            }
        }
    }

    // ---------------------------------------------------------------- 4. reschedule

    private void rescheduleAppointment(User user) {
        Appointment target = pickScheduled("SELECT APPOINTMENT TO RESCHEDULE");
        if (target == null) return;

        System.out.println("\nCurrent: " + target.appointment_date() + " " + target.appointment_time()
                + " with " + target.doctor_name());

        String[] slot = readSlot(target.doctor_id(), target.appointment_id());
        if (slot == null) return;

        if (!InputHelper.confirm("Move appointment " + target.appointment_id() + " to "
                + slot[0] + " " + slot[1] + "?")) {
            System.out.println(" -> Cancelled.");
            return;
        }

        if (appointmentDao.reschedule(target.appointment_id(), slot[0], slot[1], user.id())) {
            System.out.println(" -> Appointment rescheduled.");
        } else {
            System.out.println(" -> [ERROR] Failed to reschedule.");
        }
        InputHelper.pause();
    }

    // ---------------------------------------------------------------- 5. cancel

    private void cancelAppointment(User user) {
        Appointment target = pickScheduled("SELECT APPOINTMENT TO CANCEL");
        if (target == null) return;

        if (!InputHelper.confirm("Cancel appointment " + target.appointment_id() + " ("
                + target.patient_name() + " on " + target.appointment_date() + " "
                + target.appointment_time() + ")?")) {
            System.out.println(" -> Cancelled.");
            return;
        }

        if (appointmentDao.updateStatus(target.appointment_id(), "CANCELLED", user.id())) {
            System.out.println(" -> Appointment cancelled.");
        } else {
            System.out.println(" -> [ERROR] Failed to cancel appointment.");
        }
        InputHelper.pause();
    }

    /** Shows scheduled appointments and lets the user pick one. Null if none/cancelled. */
    private Appointment pickScheduled(String title) {
        List<Appointment> scheduled = appointmentDao.getByStatus("SCHEDULED").stream()
                .sorted(Comparator.comparing(Appointment::appointment_date)
                        .thenComparing(Appointment::appointment_time))
                .toList();

        ui.appointmentTable(scheduled, title);
        if (scheduled.isEmpty()) return null;

        int id = ui.readId("Enter Appointment ID (0 to cancel): ");
        if (id == 0) {
            System.out.println(" -> Cancelled.");
            return null;
        }
        Appointment a = scheduled.stream().filter(x -> x.appointment_id() == id).findFirst().orElse(null);
        if (a == null) {
            System.out.println(" -> [ERROR] Appointment ID not found in the scheduled list.");
        }
        return a;
    }

    // ---------------------------------------------------------------- 6. daily schedule

    private void viewDailySchedule() {
        String date = ui.readDate("Enter Date (YYYY-MM-DD, Enter = today, 0 to cancel): ", true);
        if (date == null) {
            System.out.println(" -> Cancelled.");
            return;
        }
        List<Appointment> list = appointmentDao.getByDate(date).stream()
                .sorted(Comparator.comparing(Appointment::appointment_time)).toList();
        ui.appointmentTable(list, "DAILY SCHEDULE: " + date);
        InputHelper.pause();
    }
}
