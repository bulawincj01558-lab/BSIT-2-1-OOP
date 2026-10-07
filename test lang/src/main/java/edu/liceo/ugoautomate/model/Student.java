package edu.liceo.ugoautomate.model;

public class Student extends User {
    private String studentNumber;
    private String course;
    private int yearLevel;

    public Student() {
        super(Role.STUDENT);
    }

    @Override
    public String getSummary() {
        return studentNumber + " - " + course + " " + yearLevel + getYearSuffix(yearLevel) + " Year";
    }

    private static String getYearSuffix(int year) {
        return switch (year) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
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
}
