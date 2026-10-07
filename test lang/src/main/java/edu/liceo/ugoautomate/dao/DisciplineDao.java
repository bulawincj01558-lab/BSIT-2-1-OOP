package edu.liceo.ugoautomate.dao;

import edu.liceo.ugoautomate.model.CaseStatus;
import edu.liceo.ugoautomate.model.DisciplineRecord;

import java.util.List;
import java.util.Optional;

public interface DisciplineDao {
    List<DisciplineRecord> search(CaseStatus status, String search);

    List<DisciplineRecord> findByStudent(long studentId);

    Optional<DisciplineRecord> findById(long id);

    long countByStatus(CaseStatus status);

    long create(DisciplineRecord record);

    void update(DisciplineRecord record);

    void delete(long id);
}
