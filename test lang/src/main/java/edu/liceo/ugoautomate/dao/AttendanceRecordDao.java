package edu.liceo.ugoautomate.dao;

import edu.liceo.ugoautomate.model.AttendanceRecord;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Persistence for recorded attendance.
 */
public interface AttendanceRecordDao {

    /**
     * Records attendance if the student has not yet been recorded for the session.
     *
     * @return true if a new record was inserted, false if one already existed
     */
    boolean insertIfAbsent(long sessionId, long studentUserId, LocalDateTime recordedAt);

    List<AttendanceRecord> findBySession(long sessionId);

    List<AttendanceRecord> findByStudent(long studentUserId);

    List<AttendanceRecord> findRecent(int limit);

    boolean delete(long recordId);
}
