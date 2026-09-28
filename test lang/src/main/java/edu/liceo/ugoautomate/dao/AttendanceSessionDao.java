package edu.liceo.ugoautomate.dao;

import edu.liceo.ugoautomate.model.AttendanceSession;

import java.util.List;
import java.util.Optional;

/**
 * Persistence for attendance sessions (the activities QR codes are issued for).
 */
public interface AttendanceSessionDao {

    long insert(AttendanceSession session);

    /** Updates title, type, venue, schedule, and active flag. */
    void update(AttendanceSession session);

    void updateNonce(long sessionId, String nonce);

    void setActive(long sessionId, boolean active);

    Optional<AttendanceSession> findById(long id);

    /** @return all sessions, newest first, each with its attendee count populated */
    List<AttendanceSession> findAll();

    boolean delete(long id);
}
