INSERT INTO tuition_entries (student_id, term, entry_type, description, amount_cents, reference, entry_date, recorded_by, recorded_at)
SELECT s.user_id, 'SY 2026-2027 1st Semester', 'CHARGE', 'Tuition fee (15 units x ₱1,200.00)', 1800000, NULL, '2026-08-03',
       (SELECT id FROM users WHERE email = 'admin@liceo.edu.ph'), '2026-08-03T09:00:00'
FROM students s WHERE s.student_number = '2023-00123';

INSERT INTO tuition_entries (student_id, term, entry_type, description, amount_cents, reference, entry_date, recorded_by, recorded_at)
SELECT s.user_id, 'SY 2026-2027 1st Semester', 'CHARGE', 'Miscellaneous fees', 500000, NULL, '2026-08-03',
       (SELECT id FROM users WHERE email = 'admin@liceo.edu.ph'), '2026-08-03T09:00:00'
FROM students s WHERE s.student_number = '2023-00123';

INSERT INTO tuition_entries (student_id, term, entry_type, description, amount_cents, reference, entry_date, recorded_by, recorded_at)
SELECT s.user_id, 'SY 2026-2027 1st Semester', 'PAYMENT', 'Payment - GCash', 1000000, 'OR 2026-004512', '2026-08-05',
       (SELECT id FROM users WHERE email = 'admin@liceo.edu.ph'), '2026-08-05T13:20:00'
FROM students s WHERE s.student_number = '2023-00123';

INSERT INTO app_settings (setting_key, setting_value) VALUES ('seed.tuition', 'done')
 ON CONFLICT (setting_key) DO NOTHING;
