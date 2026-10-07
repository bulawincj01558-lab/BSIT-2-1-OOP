INSERT INTO discipline_records (student_id, incident_date, offense, severity, description, sanction, status,
                                resolution, recorded_by, recorded_at, resolved_at)
SELECT s.user_id, '2026-08-18', 'Improper uniform', 'MINOR', 'Came to class without the prescribed uniform.',
       'Verbal warning', 'RESOLVED', 'Student complied with the uniform policy the next day.',
       (SELECT id FROM users WHERE email = 'admin@liceo.edu.ph'), '2026-08-18T10:15:00', '2026-08-19T08:00:00'
FROM students s WHERE s.student_number = '2024-00789';

INSERT INTO discipline_records (student_id, incident_date, offense, severity, description, sanction, status,
                                resolution, recorded_by, recorded_at, resolved_at)
SELECT s.user_id, '2026-09-22', 'Habitual tardiness', 'MINOR', 'Late for the first class five times this month.',
       'Written warning; parent conference scheduled', 'OPEN', NULL,
       (SELECT id FROM users WHERE email = 'admin@liceo.edu.ph'), '2026-09-22T14:30:00', NULL
FROM students s WHERE s.student_number = '2024-00789';

INSERT INTO app_settings (setting_key, setting_value) VALUES ('seed.discipline', 'done')
 ON CONFLICT (setting_key) DO NOTHING;
