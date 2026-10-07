INSERT INTO class_offerings (term, subject_code, subject_title, units, section, faculty_id, days, start_time, end_time, room) VALUES
 ('SY 2026-2027 1st Semester', 'IT 211', 'Object-Oriented Programming',     3, 'BSIT 2-A',
  (SELECT id FROM faculty WHERE last_name = 'Villanueva' AND first_name = 'Ramon'), 'MONDAY,WEDNESDAY,FRIDAY', '08:00', '09:00', 'Computer Laboratory 1'),
 ('SY 2026-2027 1st Semester', 'IT 212', 'Data Structures and Algorithms', 3, 'BSIT 2-A',
  (SELECT id FROM faculty WHERE last_name = 'Mercado' AND first_name = 'Liza'), 'MONDAY,WEDNESDAY,FRIDAY', '09:00', '10:00', 'Computer Laboratory 2'),
 ('SY 2026-2027 1st Semester', 'IT 213', 'Information Management',         3, 'BSIT 2-A',
  (SELECT id FROM faculty WHERE last_name = 'Bautista' AND first_name = 'Arnel'), 'TUESDAY,THURSDAY', '08:00', '09:30', 'Computer Laboratory 1'),
 ('SY 2026-2027 1st Semester', 'GE 104', 'Mathematics in the Modern World', 3, 'BSIT 2-A',
  (SELECT id FROM faculty WHERE last_name = 'Tan' AND first_name = 'Grace'), 'TUESDAY,THURSDAY', '10:00', '11:30', 'Engineering Building'),
 ('SY 2026-2027 1st Semester', 'GE 105', 'Purposive Communication',        3, 'BSIT 2-A',
  (SELECT id FROM faculty WHERE last_name = 'Dumapias' AND first_name = 'Joel'), 'SATURDAY', '08:00', '11:00', 'University Library');

INSERT INTO class_enrollments (class_id, student_id)
SELECT c.id, s.user_id
FROM class_offerings c
JOIN students s ON s.student_number = '2023-00123'
WHERE c.section = 'BSIT 2-A' AND c.term = 'SY 2026-2027 1st Semester';

INSERT INTO app_settings (setting_key, setting_value) VALUES ('seed.classes', 'done')
 ON CONFLICT (setting_key) DO NOTHING;
