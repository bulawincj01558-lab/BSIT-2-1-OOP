package edu.liceo.ugoautomate.model;

/**
 * A registered Liceo student. Students log in with their student number.
 */
public class Student extends User {

    private String studentNumber;
    private String course;
    private int yearLevel;

    @Override
    public Role getRole() {
        return Role.STUDENT;
    }

    @Override
    public String getDisplayIdentifier() {
        return studentNumber;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public void setStudentNumber(String studentNumber) {
        this.studentNumber = studentNumber;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public int getYearLevel() {
        return yearLevel;
    }

    public void setYearLevel(int yearLevel) {
        this.yearLevel = yearLevel;
    }

    /** @return e.g. "BSIT 2" */
    public String getCourseAndYear() {
        return course + " " + yearLevel;
    }
}
