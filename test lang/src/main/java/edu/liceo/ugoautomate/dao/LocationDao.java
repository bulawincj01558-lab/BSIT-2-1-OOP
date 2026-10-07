package edu.liceo.ugoautomate.dao;

import edu.liceo.ugoautomate.model.CampusLocation;
import edu.liceo.ugoautomate.model.LocationType;

import java.util.List;
import java.util.Optional;

public interface LocationDao {
    List<CampusLocation> findAll();

    List<CampusLocation> search(String text, LocationType type);

    Optional<CampusLocation> findById(long id);

    long create(CampusLocation location);

    void update(CampusLocation location);

    void delete(long id);
}
