-- =====================================================================
-- Liceo U Go Automate - SQLite schema
-- Timestamps are stored as ISO-8601 text (yyyy-MM-ddTHH:mm:ss) so they
-- sort lexicographically and can be range-scanned through indexes.
-- =====================================================================

CREATE TABLE IF NOT EXISTS app_settings (
    setting_key   TEXT PRIMARY KEY,
    setting_value TEXT NOT NULL
);

-- ---------------------------------------------------------------------
-- Accounts: one base row per user plus one subtype row per role
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    username       TEXT    NOT NULL UNIQUE COLLATE NOCASE,
    password_hash  TEXT    NOT NULL,
    role           TEXT    NOT NULL CHECK (role IN ('STUDENT', 'GUEST', 'ADMIN')),
    full_name      TEXT    NOT NULL,
    email          TEXT,
    contact_number TEXT,
    active         INTEGER NOT NULL DEFAULT 1,
    created_at     TEXT    NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_users_role_name ON users (role, full_name);

CREATE TABLE IF NOT EXISTS students (
    user_id        INTEGER PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    student_number TEXT    NOT NULL UNIQUE,
    course         TEXT    NOT NULL,
    year_level     INTEGER NOT NULL CHECK (year_level BETWEEN 1 AND 6)
);

CREATE TABLE IF NOT EXISTS guests (
    user_id          INTEGER PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    purpose_of_visit TEXT NOT NULL,
    person_to_visit  TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS administrators (
    user_id INTEGER PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    office  TEXT NOT NULL
);

-- ---------------------------------------------------------------------
-- Campus Navigator
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS campus_locations (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    name          TEXT    NOT NULL COLLATE NOCASE,
    location_type TEXT    NOT NULL CHECK (location_type IN ('BUILDING', 'OFFICE', 'FACILITY', 'LANDMARK')),
    building      TEXT,
    floor         TEXT,
    description   TEXT
);
CREATE INDEX IF NOT EXISTS idx_locations_type_name ON campus_locations (location_type, name);

-- ---------------------------------------------------------------------
-- QR Code Attendance
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS attendance_sessions (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    title        TEXT    NOT NULL,
    session_type TEXT    NOT NULL CHECK (session_type IN ('CLASS', 'EVENT')),
    venue        TEXT,
    start_time   TEXT    NOT NULL,
    end_time     TEXT    NOT NULL,
    qr_nonce     TEXT    NOT NULL,
    active       INTEGER NOT NULL DEFAULT 1,
    created_by   INTEGER REFERENCES users (id) ON DELETE SET NULL,
    created_at   TEXT    NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_sessions_start ON attendance_sessions (start_time);

CREATE TABLE IF NOT EXISTS attendance_records (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id      INTEGER NOT NULL REFERENCES attendance_sessions (id) ON DELETE CASCADE,
    student_user_id INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    recorded_at     TEXT    NOT NULL,
    UNIQUE (session_id, student_user_id)
);
CREATE INDEX IF NOT EXISTS idx_attendance_student ON attendance_records (student_user_id, recorded_at);
CREATE INDEX IF NOT EXISTS idx_attendance_recorded ON attendance_records (recorded_at);

-- ---------------------------------------------------------------------
-- Campus Entry and Access Verification
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS guest_visits (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    guest_user_id   INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    purpose         TEXT    NOT NULL,
    person_to_visit TEXT    NOT NULL,
    visit_date      TEXT    NOT NULL,
    status          TEXT    NOT NULL CHECK (status IN ('REGISTERED', 'CHECKED_IN', 'CANCELLED')),
    pass_nonce      TEXT    NOT NULL,
    registered_at   TEXT    NOT NULL,
    checked_in_at   TEXT
);
CREATE INDEX IF NOT EXISTS idx_visits_guest ON guest_visits (guest_user_id, visit_date);
CREATE INDEX IF NOT EXISTS idx_visits_date ON guest_visits (visit_date);
CREATE INDEX IF NOT EXISTS idx_visits_registered ON guest_visits (registered_at);

-- Entry logs keep a snapshot of the entrant's name/identifier so the
-- log stays readable even if the account is later removed.
CREATE TABLE IF NOT EXISTS entry_logs (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    entrant_type        TEXT NOT NULL CHECK (entrant_type IN ('STUDENT', 'GUEST')),
    user_id             INTEGER REFERENCES users (id) ON DELETE SET NULL,
    visit_id            INTEGER REFERENCES guest_visits (id) ON DELETE SET NULL,
    entrant_name        TEXT NOT NULL,
    identifier          TEXT NOT NULL,
    details             TEXT,
    verification_method TEXT NOT NULL CHECK (verification_method IN ('QR_CODE', 'CREDENTIALS')),
    verified_by         INTEGER REFERENCES users (id) ON DELETE SET NULL,
    entry_time          TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_entry_time ON entry_logs (entry_time);
