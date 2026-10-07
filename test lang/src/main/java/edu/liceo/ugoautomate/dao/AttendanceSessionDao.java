package edu.liceo.ugoautomate.dao;

import edu.liceo.ugoautomate.model.AttendanceSession;

import java.util.List;
import java.util.Optional;

public interface AttendanceSessionDao {
    List<AttendanceSession> findAll();

    Optional<AttendanceSession> findById(long id);

    long create(AttendanceSession session);

    void update(AttendanceSession session);

    void delete(long id);
}
