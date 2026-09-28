package edu.liceo.ugoautomate.dao;

import edu.liceo.ugoautomate.model.EntryLog;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Persistence for campus entry logs.
 */
public interface EntryLogDao {

    long insert(EntryLog log);

    /** @return logs with entry time in [from, to], newest first */
    List<EntryLog> findBetween(LocalDateTime from, LocalDateTime to);
}
