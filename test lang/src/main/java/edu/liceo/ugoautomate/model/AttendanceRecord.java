package edu.liceo.ugoautomate.model;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;

public class AttendanceRecord {
    private long id;
    private long sessionId;
    private long studentId;
    private LocalDateTime recordedAt;
    private VerificationMethod method;

    private String sessionTitle;
    private String studentName;
    private String studentNumber;
    private final Map<AttendanceSlot, LocalDateTime> times = new EnumMap<>(AttendanceSlot.class);

    public LocalDateTime getTime(AttendanceSlot slot) {
        return times.get(slot);
    }

    public void setTime(AttendanceSlot slot, LocalDateTime time) {
        times.put(slot, time);
    }

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

    public long getStudentId() {
        return studentId;
    }

    public void setStudentId(long studentId) {
        this.studentId = studentId;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }

    public VerificationMethod getMethod() {
        return method;
    }

    public void setMethod(VerificationMethod method) {
        this.method = method;
    }

    public String getSessionTitle() {
        return sessionTitle;
    }

    public void setSessionTitle(String sessionTitle) {
        this.sessionTitle = sessionTitle;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public void setStudentNumber(String studentNumber) {
        this.studentNumber = studentNumber;
    }
}
