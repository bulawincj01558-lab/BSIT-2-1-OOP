package edu.liceo.ugoautomate.dao;

import edu.liceo.ugoautomate.model.AttendanceRecord;
import edu.liceo.ugoautomate.model.AttendanceSlot;
import edu.liceo.ugoautomate.model.VerificationMethod;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AttendanceRecordDao {
    long create(AttendanceRecord record);

    boolean exists(long sessionId, long studentId);

    List<AttendanceRecord> findAll(Long sessionId);

    List<AttendanceRecord> findByStudent(long studentId);

    void delete(long id);

    Optional<LocalDateTime> findTime(long sessionId, long studentId, AttendanceSlot slot);

    void createTime(long sessionId, long studentId, AttendanceSlot slot, LocalDateTime recordedAt,
                    VerificationMethod method);
}
