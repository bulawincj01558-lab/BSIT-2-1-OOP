package edu.liceo.ugoautomate.dao;

import edu.liceo.ugoautomate.model.CampusLocation;

import java.util.List;
import java.util.Optional;

/**
 * Persistence for campus locations used by the Campus Navigator.
 */
public interface LocationDao {

    List<CampusLocation> findAll();

    Optional<CampusLocation> findById(long id);

    long insert(CampusLocation location);

    void update(CampusLocation location);

    boolean delete(long id);
}
