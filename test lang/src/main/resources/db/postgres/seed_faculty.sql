INSERT INTO faculty (first_name, last_name, department, email, active) VALUES
 ('Ramon',    'Villanueva', 'College of Information Technology', 'rvillanueva@liceo.edu.ph', 1),
 ('Liza',     'Mercado',    'College of Information Technology', 'lmercado@liceo.edu.ph',    1),
 ('Arnel',    'Bautista',   'College of Information Technology', 'abautista@liceo.edu.ph',   1),
 ('Grace',    'Tan',        'College of Arts and Sciences',      'gtan@liceo.edu.ph',        1),
 ('Joel',     'Dumapias',   'College of Arts and Sciences',      'jdumapias@liceo.edu.ph',   1),
 ('Cristina', 'Abellana',   'College of Business',               'cabellana@liceo.edu.ph',   1);

INSERT INTO app_settings (setting_key, setting_value) VALUES ('seed.faculty', 'done')
 ON CONFLICT (setting_key) DO NOTHING;
