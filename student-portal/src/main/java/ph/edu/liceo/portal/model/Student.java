package ph.edu.liceo.portal.model;

public class Student {

    private final String studentNo;
    private final String password;
    private final String fullName;
    private final String course;
    private final int yearLevel;
    private final String email;

    public Student(String studentNo, String password, String fullName,
                   String course, int yearLevel, String email) {
        this.studentNo = studentNo;
        this.password = password;
        this.fullName = fullName;
        this.course = course;
        this.yearLevel = yearLevel;
        this.email = email;
    }

    public String getStudentNo() { return studentNo; }
    public String getPassword()  { return password; }
    public String getFullName()  { return fullName; }
    public String getCourse()    { return course; }
    public int getYearLevel()    { return yearLevel; }
    public String getEmail()     { return email; }
}