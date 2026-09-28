# Liceo U Go Automate

Object-Oriented Automated Campus Management System for **Liceo de Cagayan University**.
A standalone Java desktop application (Swing) with local SQLite storage, covering campus
navigation, QR code attendance, and campus entry verification for **Students**, **Guests**,
and **Administrators**.

## Requirements

- Java 17 or newer (JDK to build, JRE to run)
- Maven 3.8+
- Optional: a webcam. Without one, QR codes can be read from an image file or typed/pasted.

## Build and run

```bash
mvn clean package
java -jar target/liceo-u-go-automate.jar
```

`mvn clean package` compiles, runs the test suite, and produces a single runnable jar with all
dependencies. On Java 22+ you may add `--enable-native-access=ALL-UNNAMED` to silence
native-access warnings (the jar manifest already declares it).

On first start, the app creates the database at `~/.liceo-ugo/ugo.db` (on Windows,
`C:\Users\<you>\.liceo-ugo\ugo.db`), builds the schema, and loads the seed data. To use a
different file:

```bash
java -Dugo.db=/path/to/campus.db -jar target/liceo-u-go-automate.jar
```

Delete the database file to reset the app to its seed state.

## Default accounts

| Role          | Username            | Password      |
|---------------|---------------------|---------------|
| Administrator | `admin`             | `Admin@123`   |
| Student       | `2023-00001`        | `Student@123` |
| Student       | `2023-00002`        | `Student@123` |
| Student       | `2022-00015`        | `Student@123` |
| Student       | `2024-00107`        | `Student@123` |
| Guest         | `guest@example.com` | `Guest@123`   |

Change the administrator password after first login (My Profile → Change Password).
Students log in with their student ID and guests with their email address.

## Features

| Requirement | Where |
|---|---|
| F-1.1 Student registration | Login → *Register as Student* (admins: Accounts → Add Student) |
| F-1.2 Guest registration (name, contact, purpose, person/office to visit) | Login → *Register as Guest* |
| F-1.3 Secure login | Login screen routes to the Student, Guest, or Admin dashboard |
| F-1.4 Profile view/update | *My Profile* (only fields permitted for the role are editable) |
| F-2.1 Organized campus directory | *Campus Navigator*: buildings, offices, facilities, landmarks by type |
| F-2.2 Search | *Campus Navigator* search box + type filter |
| F-2.3 Location details | *Campus Navigator* details card (name, type, building, floor, description) |
| F-3.1 Generate attendance QR | Admin → *Attendance Sessions* → Show QR Code (save PNG / copy code) |
| F-3.2 / F-3.3 Scan and record attendance | Student → *Scan Attendance* (camera, image file, or typed code) |
| F-3.4 View attendance | Admin → *Attendance Records* |
| F-4.1 Student entry | Student → *Entry Pass* QR, or credentials at the gate |
| F-4.2 Guest visit registration | Guest → *My Visits* → Register Visit for Today → visit pass |
| F-4.3 / F-4.4 Verify and log entries | Admin → *Entry Verification* |
| F-5.1 Manage accounts | Admin → *Accounts* (add, edit, activate/deactivate, reset password, delete) |
| F-5.2 Manage locations | Admin → *Campus Locations* (add, edit, remove) |
| Data Structures subject | *Data Structures* page (all roles): where each linear structure is used, memory allocation, Big-O |
| F-5.3 Manage attendance | Admin → *Attendance Sessions* / *Attendance Records* (manual add, delete) |
| F-5.4 Entry and visit logs | Admin → *Entry & Visit Logs* |

### Typical flows

**Attendance.** An admin creates a session under Attendance Sessions and clicks
*Show QR Code* to project it on screen or save it as a PNG. Students open *Scan Attendance*
and scan the code. Each scan shows a green SUCCESS or red FAILED banner with the reason and the
processing time.

**Guest entry.** The guest registers, logs in, opens *My Visits*, and clicks
*Register Visit for Today* to get a visit pass. At the gate, the admin scans the pass under
*Entry Verification*. The pass works once, and only on the visit day.

**Student entry.** The student shows the *Entry Pass* QR, which renews every 10 minutes.
A student without a device gives their student ID and password at the gate instead.

## Architecture

```
edu.liceo.ugoautomate
├── App, AppContext      entry point and composition root (wires DAOs → services → UI)
├── model                encapsulated domain classes: User ← Student / Guest / Administrator,
│                        CampusLocation, AttendanceSession, AttendanceRecord, GuestVisit, EntryLog …
├── dao                  DAO interfaces (UserDao, LocationDao, AttendanceSessionDao, …)
│   └── impl             JDBC/SQLite implementations (AbstractJdbcDao shared plumbing)
├── service              business rules and authorization (AuthService, AttendanceService,
│                        EntryService, GuestVisitService, LocationService, AccountService, ProfileService)
├── security             PasswordHasher (BCrypt), SessionManager, AccessControl,
│                        QrTokenService (HMAC-signed QR payloads), SecretKeyProvider
├── util                 Database (HikariCP pool), DatabaseInitializer, QrCodeUtil (ZXing),
│                        WebcamScanner, Validators, DateTimeUtil, AppConfig
└── ui                   Swing: LoginFrame, registration dialogs
    ├── common           DashboardFrame shell, CampusNavigatorPanel, DataStructuresPanel, responsive layouts,
    │                    QrScanPanel, ResultBanner, DataTable, QrDisplayDialog …
    ├── student, guest, admin   role dashboards and their pages
```

The UI talks only to services. Services talk only to DAO interfaces. Every protected
service method checks the caller's role through `AccessControl`.

Schema and seed data: `src/main/resources/db/schema.sql` and `src/main/resources/db/seed.sql`.
The seed accounts are created in `DatabaseInitializer`, so their passwords are hashed with BCrypt
at install time.

## Security

- **Passwords** are hashed with BCrypt (cost 10) and never stored in plain text. When a
  username doesn't exist, login still checks a dummy hash, so failures take the same time
  either way. Password fields are wiped from memory after use.
- **Authorization**: every protected service call re-checks the session role *and* re-reads
  the account, so a deactivated account loses access immediately. Admin functions require an
  administrator account. Students and guests can only read their own attendance and visits.
- **QR codes** are signed, not guessable. The payload looks like
  `LUGA1|TYPE|subjectId|nonce|expiry|HMAC-SHA256`, and each installation generates its own random
  key on first start. Before recording anything, the app checks:
  - the signature (compared in constant time) and the expiry;
  - the type, so an attendance code can't be used as an entry pass;
  - the session binding: the nonce must match the session's current one, so *Regenerate QR*
    kills old codes;
  - the check-in window, and that the student isn't already recorded;
  - for visit passes, single use: an atomic `REGISTERED → CHECKED_IN` update.
- All SQL uses prepared statements.

## Performance

- The HikariCP connection pool is capped at 10 connections. SQLite runs in WAL mode (reads
  never block writes) with a 5-second busy timeout, and foreign keys are enforced.
- Every hot-path query uses an index: username, student number, the
  `(session_id, student_user_id)` unique key, per-student attendance, visit dates, and entry
  time. List views are capped at 500 rows.
- Campus locations are cached in memory for 60 seconds, so navigator search never touches the
  database. Admin edits clear the cache.
- All database work runs off the Swing event thread. Scan results show their processing time;
  typical scans finish in tens of milliseconds, well under the 3-second target.

## Data Structures (linked to the Data Structures subject)

Every dashboard has a **Data Structures** page. For each linear structure the app uses, it shows
a diagram, where the structure is used in the code, how its memory is allocated, and a Big-O
table of its operations. There is also a comparison summary.

| Structure | Java type | Used in | Key operations |
|---|---|---|---|
| Array | `String[]`, `byte[]` | `RegisterStudentDialog.COURSES`, the 32-byte QR key, `QrTokenService.verify` (6 payload parts) | access O(1) |
| Dynamic array | `ArrayList` | `AbstractJdbcDao.query` results, `ListTableModel` rows | `get` O(1), `add` amortized O(1) |
| Linked list | `LinkedList` | `QrScanPanel` recent scans (last 10, newest first) | `addFirst`/`removeLast` O(1) |
| Stack | `ArrayDeque` (push/pop) | `DashboardFrame` Back-button history | `push`/`pop` O(1) |
| Queue | `ArrayDeque` (offer/poll) | `QrScanPanel` pending scans that arrive during verification | `offer`/`poll` O(1) |

Memory figures assume a 64-bit JVM with compressed references: a 12-byte object header,
4-byte references, and 8-byte alignment.

## Screen sizes and orientation

The app is a Java desktop app, so it runs on Windows, macOS, and Linux computers, including
Windows tablets such as the Surface. It does not run on Android phones or iPads. The layout
adapts to any window size or orientation without breaking:

- **Wide screens:** a sidebar menu. Below 780 px wide, the sidebar folds into a **Menu**
  button. The **Back** button works at every size.
- **Cards:** home, profile, entry pass, and scan options show side by side when there is room,
  and stack into one column on narrow or portrait windows.
- **Pages:** every page scrolls vertically and re-flows to the window width, so nothing is cut
  off. Button rows and filters wrap onto new lines.
- **Tables:** wide tables keep readable columns and scroll sideways inside their own box.
- **Dialogs:** they always fit on the screen and scroll when needed.
- **Login:** on a narrow screen the brand banner moves above the form.
- **Touch:** buttons, rows, and scroll bars are larger for touch use.

## Portability and graceful degradation

- The app is plain Java and runs on Windows, macOS, and Linux.
- If no camera is found, or the webcam native driver fails, the scan dialog says so and
  points to the other two input methods: image upload and typed code. They always work.

## Tests

```bash
mvn test
```

- `QrTokenServiceTest` covers signing, tampering, foreign keys, expiry, and malformed input.
- `CampusFlowTest` runs end to end against a temporary SQLite database: seed data,
  login/authorization, registration, the attendance flow (duplicate, stale, and expired codes),
  student entry by pass and by credentials, single-use guest passes, and immediate lockout of a
  deactivated account.

## Scope notes

The following are deliberately not included: AI chatbot, push notifications, messaging,
payments, delivery, route guidance or walking directions, attendance analytics dashboards,
online guest pre-registration (visits can only be registered for the current day), and
integration with university systems.

The campus buildings and offices in the seed data are an illustrative sample layout.
Administrators can replace them under **Campus Locations**.
