# Clinic Appointment System (CAS)

A console-based Java application for managing clinic patients, doctors and appointments.
Built for PF201 using N-Tier architecture, SQLite persistence, role-based access control (RBAC)
and system-wide audit logging.

## Tech Stack
- Java (Maven project)
- SQLite via `sqlite-jdbc` (database file: `CAS.db`)

## Project Structure (`com.scc`)
| Package | Responsibility |
| --- | --- |
| `model/` | Data records: `User`, `Patient`, `Doctor`, `Appointment`, `ActivityLogs` |
| `dao/` | All SQL (PreparedStatement): `UserDao`, `PatientDao`, `DoctorDao`, `AppointmentDao`, `ActivityLogDao`, `ActivityLogQueryDao`, `ReportDao`, schema/connection classes |
| `ui/` | Console menus per role (Admin, Receptionist, Doctor, Triage), audit log viewer and input validation helpers |
| `util/` | Reusable console helpers (`ConsoleUtil`) |

No SQL queries exist in the `ui/` package.

## How to Run
1. Open the project in IntelliJ IDEA (or any Maven-aware IDE) and let Maven download dependencies.
2. Run `com.scc.Main` for the full system.
3. Run `com.scc.AuditLogApp` to open the admin-only audit log viewer directly.
4. Tables are created automatically on first start (`DatabaseInitializer` and `ClinicSchema`).
   A copy of the schema is in `schema.sql`.

## Default Admin Account
- Username: `admin`
- Password: `admin123`

New accounts register as `PENDING` and cannot log in until an Admin approves them.

## Roles
| Role | Main Features |
| --- | --- |
| ADMIN | Manage user accounts (approve / add / update / delete), manage doctors, view patients & appointments, reports, audit logs |
| RECEPTIONIST | Register / edit / search patients, book / reschedule / cancel appointments, daily schedule |
| DOCTOR | View own schedule, view assigned patient details, update appointment status |
| TRIAGE | Placeholder menu (coming soon) |

## Audit Logging
Authentication, account approvals/updates and domain transactions are recorded in the
`activity_logs` table (LOGIN, LOGOUT, LOGIN_FAILED, REGISTER, APPROVED, CREATE, UPDATE, DELETE).
Admins can view and filter the logs by Action, Table or User ID.

## Database Tables
`users`, `activity_logs`, `patients`, `doctors`, `appointments` (see `schema.sql`).