-- =====================================================================
-- Sample campus locations (illustrative; administrators can add, edit,
-- or remove locations from the Campus Locations screen).
-- =====================================================================

-- Buildings
INSERT INTO campus_locations (name, location_type, building, floor, description) VALUES
('Administration Building', 'BUILDING', 'Administration Building', 'Ground - 3rd Floor', 'Houses the Registrar, Cashier, Admissions, and the University Clinic. First stop for enrollment and document requests.'),
('Main Academic Building', 'BUILDING', 'Main Academic Building', 'Ground - 5th Floor', 'General education classrooms, faculty rooms, and the Guidance and Counseling Office.'),
('University Library', 'BUILDING', 'University Library', 'Ground - 2nd Floor', 'Main library with reading areas, periodicals section, e-library terminals, and the Audio-Visual Room.'),
('College of Information Technology Building', 'BUILDING', 'College of Information Technology Building', 'Ground - 4th Floor', 'Computer laboratories, IT classrooms, and the Office of the Dean of the College of IT.'),
('Gymnasium', 'BUILDING', 'Gymnasium', 'Ground Floor', 'Indoor court used for PE classes, university assemblies, and major campus events.'),
('University Chapel', 'BUILDING', 'University Chapel', 'Ground Floor', 'Venue for masses, recollections, and quiet reflection. Open daily.'),
('Student Center', 'BUILDING', 'Student Center', 'Ground - 2nd Floor', 'Student organizations, the Office of Student Affairs, and the campus canteen.');

-- Offices
INSERT INTO campus_locations (name, location_type, building, floor, description) VALUES
('Registrar''s Office', 'OFFICE', 'Administration Building', '1st Floor', 'Enrollment records, transcript of records, certifications, and ID validation.'),
('Cashier / Accounting Office', 'OFFICE', 'Administration Building', '1st Floor', 'Tuition and fee inquiries, official receipts, and clearances.'),
('Admissions Office', 'OFFICE', 'Administration Building', '1st Floor', 'Applications for new students and transferees, entrance exam schedules.'),
('Guidance and Counseling Office', 'OFFICE', 'Main Academic Building', '2nd Floor', 'Counseling services, career guidance, and student wellness programs.'),
('Office of Student Affairs', 'OFFICE', 'Student Center', '2nd Floor', 'Student organizations, activity permits, and student discipline concerns.'),
('Office of the Dean, College of IT', 'OFFICE', 'College of Information Technology Building', '3rd Floor', 'Academic advising and concerns for IT and Computer Science students.'),
('Campus Security Office', 'OFFICE', 'Main Gate', 'Ground Floor', 'Security personnel, lost and found, and entry verification assistance.');

-- Facilities
INSERT INTO campus_locations (name, location_type, building, floor, description) VALUES
('University Clinic', 'FACILITY', 'Administration Building', 'Ground Floor', 'First aid, medical consultation, and health records. Open during class hours.'),
('Computer Laboratory 1', 'FACILITY', 'College of Information Technology Building', '2nd Floor', 'Networked computer laboratory for programming and multimedia classes.'),
('Computer Laboratory 2', 'FACILITY', 'College of Information Technology Building', '2nd Floor', 'Computer laboratory for database and systems development classes.'),
('Audio-Visual Room', 'FACILITY', 'University Library', '2nd Floor', 'Presentation room for seminars, film viewing, and small events.'),
('Canteen', 'FACILITY', 'Student Center', 'Ground Floor', 'Campus food service area with seating for students and visitors.'),
('Covered Court', 'FACILITY', 'Gymnasium', 'Ground Floor', 'Outdoor covered court beside the gymnasium for sports and practice.');

-- Landmarks
INSERT INTO campus_locations (name, location_type, building, floor, description) VALUES
('Main Gate', 'LANDMARK', NULL, NULL, 'Main campus entrance. Students and registered guests are verified here before entry.'),
('Parking Area', 'LANDMARK', NULL, NULL, 'Parking for faculty, staff, and visitors. Present your visit pass at the Main Gate first.'),
('Quadrangle', 'LANDMARK', NULL, NULL, 'Open grounds used for flag ceremonies and outdoor gatherings.'),
('Flagpole', 'LANDMARK', NULL, NULL, 'University flagpole at the center of the quadrangle.');
