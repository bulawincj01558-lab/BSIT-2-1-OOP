| B-tree | database index |# Liceo U Go Automate

Object-Oriented Automated Campus Management System for **Liceo de Cagayan University** — a standalone
Java 17+ JavaFX desktop application (FXML views) with a Supabase (PostgreSQL) or local SQLite database.

Features: student and guest registration, secure login, profiles, signed QR code attendance, QR-based
campus entry verification with guest visit registration, administrator management of accounts,
locations, attendance, and entry/visit logs, **class programs** (administrators create each term's
classes and enroll students with automatic schedule-conflict checks; students see their class
schedule), anonymous **faculty evaluation** (students rate the teachers of their classes once per term
on five criteria; administrators manage faculty, open/close the period, and see averages and
comments), **discipline monitoring** (administrators record and resolve cases; students see only their
own record), **tuition balance** (a per-term ledger of charges, payments, and discounts; tuition can be
assessed from class program units; students see their own statement of account), and a **Data Structures** page (for the Data Structures subject) showing the structures
the app uses with their memory allocation and time complexity.

The layout is responsive: it adapts to phone-, tablet-, and desktop-sized windows in portrait or
landscape. (It is a Java desktop app, so it runs on Windows, macOS, and Linux — not on phones or iPads.)

---

## Requirements

- JDK 17 or newer (`java -version`)
- Maven 3.8+ (`mvn -version`)
- A webcam is optional. Without one, QR codes can be uploaded as an image file or typed/pasted in.

## Build and run

```bash
mvn clean package
java -jar target/liceo-u-go-automate.jar
```

`mvn clean package` compiles the code, runs the tests, and produces a single runnable jar that contains all dependencies.

### Running from IntelliJ IDEA

1. **File → Open…** and select this folder's `pom.xml`, then choose **Open as Project**.
   IntelliJ imports it as a Maven project and downloads the libraries.
2. Open `src/main/java/edu/liceo/ugoautomate/App.java` and click the green ▶ next to `main`.

## Default accounts

| Role          | Email                         | Password      |
|---------------|-------------------------------|---------------|
| Administrator | `admin@liceo.edu.ph`          | `Admin@123`   |
| Student       | `juan.delacruz@liceo.edu.ph`  | `Student@123` |
| Student       | `maria.santos@liceo.edu.ph`   | `Student@123` |
| Student       | `jose.reyes@liceo.edu.ph`     | `Student@123` |
| Guest         | `guest.ana@example.com`       | `Guest@123`   |

Change the administrator password after the first login (**My Profile → Change password**).

## Database: Supabase or local SQLite

If a `supabase.properties` file exists in the project folder (or in `~/.liceo-ugo/`), the app uses that
Supabase project; otherwise it uses a local SQLite file. Either way the tables and sample accounts are
created automatically on first run.

**Supabase setup:** copy `supabase.properties.example` to `supabase.properties` and fill in the
Session pooler host, user, and database password (Supabase: **Connect → Direct → Session pooler**).
`supabase.properties` is git-ignored because it holds the password; never commit it. In the Supabase
Table Editor, the `student_list` view shows each student with their name and email.

**Local SQLite:** without `supabase.properties`, the database is created at:

```
~/.liceo-ugo/ugo.db          (Windows: C:\Users\<you>\.liceo-ugo\ugo.db)
```

Use a different file with `java -Dugo.db=path/to/other.db -jar target/liceo-u-go-automate.jar`.
Delete the file to start over with fresh seed data.

The seeded buildings and offices are **sample data** for demonstration; administrators can edit them
on the **Locations** page.

---

## How to try each feature

**QR attendance (F-3)**
1. Log in as the admin → **Attendance Sessions**. A sample session that is open all day today is seeded.
   Select it → **Show QR code** (you can **Save as PNG** or **Copy code**).
2. Log out, log in as a student → **Scan Attendance**. Use the webcam, **Upload QR image…** with the saved
   PNG, or paste the code. A green banner confirms; scanning again shows a red "Already recorded".
3. The student sees it under **My Attendance**; the admin sees the count in **Attendance Sessions** (Attendees).
4. Or use the student's own QR: in **Scan Attendance** the student sees **My attendance QR** (renews every
   5 minutes). They pick any **Subject** from their class program, then **Save as PNG** / **Copy code** and upload
   or paste it on the same page (recorded in today's class of that subject, created if needed).

**Student entry (F-4.1, F-4.3, F-4.4)**
1. As a student → **My Entry QR**. The code appears right away and renews itself every 5 minutes; **Edit profile** fixes wrong details first.
2. As admin → **Entry Gate** → scan/upload/paste it → "Entry granted". See **Entry & Visit Logs**.

**Guest visit (F-4.2)**
1. On the login screen → **Register as Guest** (or use the sample guest).
2. As the guest → **My Visits** → register a visit for today → the guest pass QR opens.
3. As admin → **Entry Gate** → scan the pass → the visit becomes *Checked in* and an entry is logged.
   A pass works once, and only on its visit date.

---

## Architecture

```
edu.liceo.ugoautomate
├── App, AppContext          entry point and composition root (wires every layer once)
├── model                    encapsulated domain classes; User → Student / Guest / Administrator
├── dao                      DAO interfaces
│   └── impl                 JDBC implementations (AbstractJdbcDao holds shared plumbing)
├── service                  business rules, validation, role checks
├── security                 BCrypt hashing, session, access control, signed QR tokens
├── util                     database pool/initializer, config, validation, ZXing QR helpers
├── ds                       Data Structures subject: catalog, memory estimator, B-tree model, benchmark
└── ui                       JavaFX: login, registration, Navigator (switches login/dashboard)
    ├── common               dashboard frame, QR scanner, DS page and diagram, shared helpers
    │                        (Views = FXML loader, Async, Dialogs, Tables, Responsive)
    ├── student / guest / admin   role dashboards and page controllers

resources/edu/liceo/ugoautomate/ui
├── *.fxml, common/, student/, guest/, admin/   one FXML view per screen (fx:controller = its controller)
└── app.css                                     the theme (Liceo maroon and gold)
```

## Data Structures page

Every dashboard has a **Data Structures** page. Each structure is one the app really uses, drawn with
live data, with a memory breakdown and a Big-O table:

| Structure | Java type | Where the app uses it |
|---|---|---|
| Array | `int[]` in ZXing `BitMatrix` | every QR code's module grid |
| Dynamic array | `ArrayList` | every table (DAO results shown in a `TableView`) |
| Hash table | `HashMap` | scanner's recent-code check (no double scans) |
| Stack | `ArrayDeque` | the **Back** button's navigation history |
| Queue | `ArrayDeque` | "Recent scans" (bounded, oldest removed first) |
| Doubly linked list | `LinkedHashMap` | dashboard page registry / menu order |
| B-tree | SQLite index | every `CREATE INDEX` (login by email, logs by date, ...) |

Memory figures are estimates for a 64-bit JVM with compressed references (`ds/MemoryEstimator`).
The **Measure time complexity** tab times real operations at n = 1,000 / 10,000 / 100,000 so O(1),
O(log n), and O(n) growth is visible.

## Responsive layout

Resize the window (or maximize/restore) to see it adapt:

- **Wide (desktop, tablet landscape):** sidebar menu; two-column forms and panels.
- **Narrow (phone, tablet portrait):** the sidebar becomes a **☰ Menu** button, side panels move
  below the main content, and cards drop to one column.
- **Short (phone landscape):** pages scroll vertically; wide tables scroll sideways instead of squeezing.

Layers depend only downward: **UI → service → DAO → database**. The UI never touches JDBC, and services
read the acting user from the session instead of trusting the UI.

Schema and seed scripts: `src/main/resources/db/sqlite/` and `db/postgres/` (`schema.sql`, `seed.sql`).

### Security

- **Passwords**: BCrypt (work factor 10); never stored or logged in plain text. Failed logins give one
  generic message and take the same time whether or not the email exists.
- **Authorization**: every protected service method calls `AccessControl.requireRole(...)`; admin
  functions reject non-admins even if a screen were reached. Students only see their own attendance;
  guests only their own visits.
- **QR tokens**: `UGO1.<TYPE>.<subjectId>.<issued>.<expires>.<nonce>.<HMAC-SHA256>`, signed with a random
  per-installation secret stored in the database. Before recording anything the app checks the
  signature (constant-time), expiry, token type, and the bound session/student/visit in the database
  (session open now, not already recorded, account active, visit pending and dated today).

### Performance

- HikariCP connection pool (small pool for Supabase); SQLite in WAL mode with a busy timeout so readers
  never block the writer.
- Indexes on every hot lookup (email, role, session/student pairs, timestamps, visit dates). Timestamps
  are fixed-width ISO text so date ranges use index range scans.
- All database, BCrypt, and QR decoding work runs off the JavaFX application thread (`ui.common.Async`).

## Tests

```bash
mvn test
```

- `QrTokenServiceTest`: signing, tampering, foreign keys, expiry, malformed input.
- `CampusFlowTest`: end-to-end flows on a temporary database — login, registration, attendance
  (including duplicates and tampering), student entry, guest pass, role enforcement, profile updates,
  and admin management.
- `DataStructuresTest`: ArrayList/HashMap growth rules (checked against the real JDK HashMap), bucket
  hashing, memory totals, B-tree ordering and balance, and catalog completeness.

## Out of scope (by design)

No AI chatbot, push notifications, messaging, payments, delivery features, route guidance or walking
directions, attendance analytics dashboards, online guest pre-registration, or integration with
university systems.
