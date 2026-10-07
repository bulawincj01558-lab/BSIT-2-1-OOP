package edu.liceo.ugoautomate.dao;

import edu.liceo.ugoautomate.model.EntryLog;

import java.time.LocalDate;
import java.util.List;

public interface EntryLogDao {
    long create(EntryLog log);

    List<EntryLog> findBetween(LocalDate from, LocalDate to);
}
