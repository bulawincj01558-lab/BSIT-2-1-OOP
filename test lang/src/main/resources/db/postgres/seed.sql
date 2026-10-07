INSERT INTO users (id, role, email, password_hash, first_name, last_name, contact_number, active, created_at) VALUES
 (1, 'ADMIN',   'admin@liceo.edu.ph',          '$2a$10$6pVrPDCgPyMQibUJJH2OyO0CmNsrhEGwXp5c1LVzM4FO1oucy/9.i', 'System', 'Administrator', '0917-000-0000', 1, to_char(LOCALTIMESTAMP, 'YYYY-MM-DD"T"HH24:MI:SS')),
 (2, 'STUDENT', 'juan.delacruz@liceo.edu.ph',  '$2a$10$6D0KSZuccDMsNvUV86Vq6e4X.NP8.6358Dmoo7EiADiuAHhRn81IS', 'Juan',   'Dela Cruz',     '0917-111-1111', 1, to_char(LOCALTIMESTAMP, 'YYYY-MM-DD"T"HH24:MI:SS')),
 (3, 'STUDENT', 'maria.santos@liceo.edu.ph',   '$2a$10$6D0KSZuccDMsNvUV86Vq6e4X.NP8.6358Dmoo7EiADiuAHhRn81IS', 'Maria',  'Santos',        '0917-222-2222', 1, to_char(LOCALTIMESTAMP, 'YYYY-MM-DD"T"HH24:MI:SS')),
 (4, 'STUDENT', 'jose.reyes@liceo.edu.ph',     '$2a$10$6D0KSZuccDMsNvUV86Vq6e4X.NP8.6358Dmoo7EiADiuAHhRn81IS', 'Jose',   'Reyes',         '0917-333-3333', 1, to_char(LOCALTIMESTAMP, 'YYYY-MM-DD"T"HH24:MI:SS')),
 (5, 'GUEST',   'guest.ana@example.com',       '$2a$10$YwyOCsyttVozghBIjbcgPOMimrCdzV5EHFg7OssGiTvrljxT9tCUe', 'Ana',    'Lim',           '0917-444-4444', 1, to_char(LOCALTIMESTAMP, 'YYYY-MM-DD"T"HH24:MI:SS'));

INSERT INTO administrators (user_id, position) VALUES (1, 'Campus Systems Administrator');

INSERT INTO students (user_id, student_number, course, year_level) VALUES
 (2, '2023-00123', 'BS Information Technology', 2),
 (3, '2022-00456', 'BS Computer Science', 3),
 (4, '2024-00789', 'BS Information Technology', 1);

INSERT INTO guests (user_id, purpose_of_visit, person_to_visit) VALUES
 (5, 'Enrollment inquiry for my sibling', 'Registrar''s Office');

INSERT INTO locations (name, type, building, floor, description) VALUES
 ('Main Gate',                              'LANDMARK', NULL,                                  'Ground', 'Main pedestrian and vehicle entrance. Entry verification happens here.'),
 ('Security Office',                        'OFFICE',   'Main Gate',                           'Ground', 'Campus security personnel and lost-and-found.'),
 ('Administration Building',                'BUILDING', NULL,                                  NULL,     'Houses the main administrative offices of the university.'),
 ('Registrar''s Office',                    'OFFICE',   'Administration Building',             '1st Floor', 'Student records, enrollment documents, and transcripts.'),
 ('Cashier / Accounting Office',            'OFFICE',   'Administration Building',             '1st Floor', 'Tuition and fee payments (in person).'),
 ('Office of Student Affairs',              'OFFICE',   'Administration Building',             '2nd Floor', 'Student organizations, activities, and IDs.'),
 ('Office of the President',                'OFFICE',   'Administration Building',             '3rd Floor', 'Office of the University President.'),
 ('College of Information Technology Building', 'BUILDING', NULL,                              NULL,     'Classrooms and computer laboratories for IT and CS programs.'),
 ('CIT Dean''s Office',                     'OFFICE',   'College of Information Technology Building', '2nd Floor', 'Dean and program chairs of the College of Information Technology.'),
 ('Computer Laboratory 1',                  'FACILITY', 'College of Information Technology Building', '3rd Floor', '40-seat computer laboratory.'),
 ('Computer Laboratory 2',                  'FACILITY', 'College of Information Technology Building', '3rd Floor', '40-seat computer laboratory with projector.'),
 ('Engineering Building',                   'BUILDING', NULL,                                  NULL,     'Classrooms and laboratories of the College of Engineering.'),
 ('University Library',                     'BUILDING', NULL,                                  NULL,     'Main library, reading areas, and e-library.'),
 ('E-Library',                              'FACILITY', 'University Library',                  '2nd Floor', 'Computers for research and online journals.'),
 ('Student Center',                         'BUILDING', NULL,                                  NULL,     'Student services building.'),
 ('Guidance and Counseling Office',         'OFFICE',   'Student Center',                      '1st Floor', 'Counseling, testing, and career guidance.'),
 ('Health Services Clinic',                 'OFFICE',   'Student Center',                      '1st Floor', 'First aid and medical/dental services.'),
 ('Gymnasium',                              'FACILITY', NULL,                                  'Ground', 'Indoor court for PE classes, sports, and large events.'),
 ('Cafeteria',                              'FACILITY', NULL,                                  'Ground', 'Food court and dining area.'),
 ('University Chapel',                      'LANDMARK', NULL,                                  'Ground', 'Chapel for masses and quiet reflection.'),
 ('Parking Area',                           'LANDMARK', NULL,                                  'Ground', 'Parking for faculty, staff, and visitors.');

INSERT INTO attendance_sessions (title, session_type, location_id, starts_at, ends_at, created_by, created_at) VALUES
 ('IT 211 - Object-Oriented Programming (Sample)', 'CLASS',
  (SELECT id FROM locations WHERE name = 'Computer Laboratory 1'),
  to_char(LOCALTIMESTAMP, 'YYYY-MM-DD"T00:00:00"'),
  to_char(LOCALTIMESTAMP, 'YYYY-MM-DD"T23:59:00"'),
  1,
  to_char(LOCALTIMESTAMP, 'YYYY-MM-DD"T"HH24:MI:SS'));

SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT MAX(id) FROM users));
