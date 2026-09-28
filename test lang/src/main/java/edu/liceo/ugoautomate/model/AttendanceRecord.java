package edu.liceo.ugoautomate.model;

import java.time.LocalDateTime;

/**
 * One student's recorded attendance for one session, with the date and time
 * it was recorded. Session and student details are joined in for display.
 */
public class AttendanceRecord {

    private long id;
    private long sessionId;
    private String sessionTitle;
    private long studentUserId;
    private String studentNumber;
    private String studentName;
    private String course;
    private LocalDateTime recordedAt;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionTitle() {
        return sessionTitle;
    }

    public void setSessionTitle(String sessionTitle) {
        this.sessionTitle = sessionTitle;
    }

    public long getStudentUserId() {
        return studentUserId;
    }

    public void setStudentUserId(long studentUserId) {
        this.studentUserId = studentUserId;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public void setStudentNumber(String studentNumber) {
        this.studentNumber = studentNumber;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }
}
