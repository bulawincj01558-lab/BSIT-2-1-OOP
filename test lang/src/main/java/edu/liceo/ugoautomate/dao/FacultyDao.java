package edu.liceo.ugoautomate.dao;

import edu.liceo.ugoautomate.model.Faculty;

import java.util.List;
import java.util.Optional;

public interface FacultyDao {
    List<Faculty> findAll(boolean activeOnly);

    Optional<Faculty> findById(long id);

    long create(Faculty faculty);

    void update(Faculty faculty);

    void delete(long id);
}
