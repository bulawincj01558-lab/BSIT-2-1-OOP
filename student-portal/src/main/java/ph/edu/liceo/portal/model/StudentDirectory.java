package ph.edu.liceo.portal.model;

import java.util.ArrayList;

public class StudentDirectory {

    private final ArrayList<Student> students = new ArrayList<>();

    public StudentDirectory() {
        students.add(new Student("2026-00123", "liceo123", "Ana Marie Dela Cruz",
                "BS Information Technology", 3, "ana.delacruz@liceo.edu.ph"));
        students.add(new Student("2026-00456", "gcash456", "Jerome Bacaltos",
                "BS Computer Science", 2, "jerome.bacaltos@liceo.edu.ph"));
        students.add(new Student("2026-00789", "maya789", "Liza Manalo",
                "BS Information Systems", 4, "liza.manalo@liceo.edu.ph"));
        students.add(new Student("20252101558", "liceo1558", "Cavhen Joshua Bulawin",
                "BS Information Technology", 2, "bulawincj01558@liceo.edu.ph"));
    }

    public Student login(String studentNo, String password) {
        for (Student s : students) {
            if (s.getStudentNo().equals(studentNo) && s.getPassword().equals(password)) {
                return s;
            }
        }
        return null;
    }
}