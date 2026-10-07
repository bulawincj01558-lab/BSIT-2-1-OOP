CREATE TABLE IF NOT EXISTS users (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    role            TEXT    NOT NULL CHECK (role IN ('STUDENT', 'GUEST', 'ADMIN')),
    email           TEXT    NOT NULL UNIQUE COLLATE NOCASE,
    password_hash   TEXT    NOT NULL,
    first_name      TEXT    NOT NULL,
    last_name       TEXT    NOT NULL,
    contact_number  TEXT,
    active          INTEGER NOT NULL DEFAULT 1,
    created_at      TEXT    NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_users_role ON users (role);
CREATE INDEX IF NOT EXISTS idx_users_last_name ON users (last_name COLLATE NOCASE);

CREATE TABLE IF NOT EXISTS students (
    user_id         INTEGER PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    student_number  TEXT    NOT NULL UNIQUE,
    course          TEXT    NOT NULL,
    year_level      INTEGER NOT NULL CHECK (year_level BETWEEN 1 AND 6)
);

CREATE TABLE IF NOT EXISTS guests (
    user_id          INTEGER PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    purpose_of_visit TEXT    NOT NULL,
    person_to_visit  TEXT    NOT NULL
);

CREATE TABLE IF NOT EXISTS administrators (
    user_id   INTEGER PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    position  TEXT
);

CREATE TABLE IF NOT EXISTS locations (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    name         TEXT    NOT NULL,
    type         TEXT    NOT NULL CHECK (type IN ('BUILDING', 'OFFICE', 'FACILITY', 'LANDMARK')),
    building     TEXT,
    floor        TEXT,
    description  TEXT
);
CREATE INDEX IF NOT EXISTS idx_locations_name ON locations (name COLLATE NOCASE);
CREATE INDEX IF NOT EXISTS idx_locations_type ON locations (type);

CREATE TABLE IF NOT EXISTS attendance_sessions (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    title         TEXT    NOT NULL,
    session_type  TEXT    NOT NULL CHECK (session_type IN ('CLASS', 'EVENT')),
    location_id   INTEGER REFERENCES locations (id) ON DELETE SET NULL,
    starts_at     TEXT    NOT NULL,
    ends_at       TEXT    NOT NULL,
    created_by    INTEGER REFERENCES users (id) ON DELETE SET NULL,
    created_at    TEXT    NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_sessions_starts ON attendance_sessions (starts_at);

CREATE TABLE IF NOT EXISTS attendance_records (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id   INTEGER NOT NULL REFERENCES attendance_sessions (id) ON DELETE CASCADE,
    student_id   INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    recorded_at  TEXT    NOT NULL,
    method       TEXT    NOT NULL CHECK (method IN ('WEBCAM', 'IMAGE_FILE', 'MANUAL_ENTRY')),
    UNIQUE (session_id, student_id)
);
CREATE INDEX IF NOT EXISTS idx_attendance_student ON attendance_records (student_id, recorded_at);
CREATE INDEX IF NOT EXISTS idx_attendance_recorded ON attendance_records (recorded_at);

CREATE TABLE IF NOT EXISTS guest_visits (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    guest_id         INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    purpose          TEXT    NOT NULL,
    person_to_visit  TEXT    NOT NULL,
    visit_date       TEXT    NOT NULL,
    status           TEXT    NOT NULL CHECK (status IN ('PENDING', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED')),
    registered_at    TEXT    NOT NULL,
    checked_in_at    TEXT,
    checked_out_at   TEXT
);
CREATE INDEX IF NOT EXISTS idx_visits_guest ON guest_visits (guest_id, visit_date);
CREATE INDEX IF NOT EXISTS idx_visits_date ON guest_visits (visit_date);

CREATE TABLE IF NOT EXISTS entry_logs (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    entrant_type  TEXT    NOT NULL CHECK (entrant_type IN ('STUDENT', 'GUEST')),
    user_id       INTEGER REFERENCES users (id) ON DELETE SET NULL,
    visit_id      INTEGER REFERENCES guest_visits (id) ON DELETE SET NULL,
    entrant_name  TEXT    NOT NULL,
    details       TEXT,
    method        TEXT    NOT NULL CHECK (method IN ('WEBCAM', 'IMAGE_FILE', 'MANUAL_ENTRY')),
    verified_by   INTEGER REFERENCES users (id) ON DELETE SET NULL,
    logged_at     TEXT    NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_entry_logged ON entry_logs (logged_at);
CREATE INDEX IF NOT EXISTS idx_entry_user ON entry_logs (user_id);

CREATE TABLE IF NOT EXISTS app_settings (
    setting_key    TEXT PRIMARY KEY,
    setting_value  TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS faculty (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    first_name  TEXT    NOT NULL,
    last_name   TEXT    NOT NULL,
    department  TEXT    NOT NULL,
    email       TEXT,
    active      INTEGER NOT NULL DEFAULT 1
);
CREATE INDEX IF NOT EXISTS idx_faculty_last_name ON faculty (last_name COLLATE NOCASE);

CREATE TABLE IF NOT EXISTS faculty_evaluations (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    faculty_id    INTEGER NOT NULL REFERENCES faculty (id) ON DELETE CASCADE,
    student_id    INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    term          TEXT    NOT NULL,
    knowledge     INTEGER NOT NULL CHECK (knowledge BETWEEN 1 AND 5),
    clarity       INTEGER NOT NULL CHECK (clarity BETWEEN 1 AND 5),
    management    INTEGER NOT NULL CHECK (management BETWEEN 1 AND 5),
    fairness      INTEGER NOT NULL CHECK (fairness BETWEEN 1 AND 5),
    punctuality   INTEGER NOT NULL CHECK (punctuality BETWEEN 1 AND 5),
    comment       TEXT,
    submitted_at  TEXT    NOT NULL,
    UNIQUE (faculty_id, student_id, term)
);
CREATE INDEX IF NOT EXISTS idx_evaluations_term ON faculty_evaluations (term, faculty_id);
CREATE INDEX IF NOT EXISTS idx_evaluations_student ON faculty_evaluations (student_id, term);

CREATE TABLE IF NOT EXISTS class_offerings (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    term           TEXT    NOT NULL,
    subject_code   TEXT    NOT NULL,
    subject_title  TEXT    NOT NULL,
    units          INTEGER NOT NULL CHECK (units BETWEEN 0 AND 10),
    section        TEXT    NOT NULL,
    faculty_id     INTEGER REFERENCES faculty (id) ON DELETE SET NULL,
    days           TEXT    NOT NULL,
    start_time     TEXT    NOT NULL,
    end_time       TEXT    NOT NULL,
    room           TEXT
);
CREATE INDEX IF NOT EXISTS idx_classes_term ON class_offerings (term, subject_code);

CREATE TABLE IF NOT EXISTS class_enrollments (
    class_id    INTEGER NOT NULL REFERENCES class_offerings (id) ON DELETE CASCADE,
    student_id  INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    PRIMARY KEY (class_id, student_id)
);
CREATE INDEX IF NOT EXISTS idx_enrollments_student ON class_enrollments (student_id);

CREATE TABLE IF NOT EXISTS discipline_records (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    student_id     INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    incident_date  TEXT    NOT NULL,
    offense        TEXT    NOT NULL,
    severity       TEXT    NOT NULL CHECK (severity IN ('MINOR', 'MAJOR', 'GRAVE')),
    description    TEXT,
    sanction       TEXT,
    status         TEXT    NOT NULL CHECK (status IN ('OPEN', 'RESOLVED')),
    resolution     TEXT,
    recorded_by    INTEGER REFERENCES users (id) ON DELETE SET NULL,
    recorded_at    TEXT    NOT NULL,
    resolved_at    TEXT
);
CREATE INDEX IF NOT EXISTS idx_discipline_student ON discipline_records (student_id, incident_date);
CREATE INDEX IF NOT EXISTS idx_discipline_status ON discipline_records (status, incident_date);

CREATE TABLE IF NOT EXISTS tuition_entries (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    student_id    INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    term          TEXT    NOT NULL,
    entry_type    TEXT    NOT NULL CHECK (entry_type IN ('CHARGE', 'PAYMENT', 'DISCOUNT')),
    description   TEXT    NOT NULL,
    amount_cents  INTEGER NOT NULL CHECK (amount_cents > 0),
    reference     TEXT,
    entry_date    TEXT    NOT NULL,
    recorded_by   INTEGER REFERENCES users (id) ON DELETE SET NULL,
    recorded_at   TEXT    NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_tuition_student ON tuition_entries (student_id, term);
CREATE INDEX IF NOT EXISTS idx_tuition_term ON tuition_entries (term);
