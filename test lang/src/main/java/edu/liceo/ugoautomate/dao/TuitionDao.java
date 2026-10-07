package edu.liceo.ugoautomate.dao;

import edu.liceo.ugoautomate.model.TuitionAccount;
import edu.liceo.ugoautomate.model.TuitionEntry;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TuitionDao {
    List<TuitionAccount> findAccounts(String term, String search);

    List<TuitionEntry> findEntries(long studentId, String term);

    Optional<TuitionEntry> findById(long id);

    List<String> findTerms();

    Map<Long, Integer> findEnrolledUnits(String term);

    boolean hasCharge(long studentId, String term, String prefix);

    long create(TuitionEntry entry);

    void delete(long id);
}
