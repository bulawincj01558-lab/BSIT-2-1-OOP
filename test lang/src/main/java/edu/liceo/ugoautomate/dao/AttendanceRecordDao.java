package edu.liceo.ugoautomate.dao;

import edu.liceo.ugoautomate.model.AttendanceRecord;

import java.util.List;

public interface AttendanceRecordDao {
    long create(AttendanceRecord record);

    boolean exists(long sessionId, long studentId);

    List<AttendanceRecord> findAll(Long sessionId);

    List<AttendanceRecord> findByStudent(long studentId);

    void delete(long id);
}
