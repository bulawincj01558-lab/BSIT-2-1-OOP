package edu.liceo.ugoautomate.service;

import edu.liceo.ugoautomate.dao.LocationDao;
import edu.liceo.ugoautomate.model.CampusLocation;
import edu.liceo.ugoautomate.model.LocationType;
import edu.liceo.ugoautomate.model.Role;
import edu.liceo.ugoautomate.security.AccessControl;
import edu.liceo.ugoautomate.util.Validation;
import edu.liceo.ugoautomate.util.ValidationException;

import java.util.List;

public class LocationService {
    private final LocationDao locationDao;
    private final AccessControl access;

    public LocationService(LocationDao locationDao, AccessControl access) {
        this.locationDao = locationDao;
        this.access = access;
    }

    public List<CampusLocation> listAll() {
        access.requireLoggedIn();
        return locationDao.findAll();
    }

    public List<CampusLocation> search(String text, LocationType type) {
        access.requireLoggedIn();
        return locationDao.search(text, type);
    }

    public CampusLocation get(long id) {
        access.requireLoggedIn();
        return locationDao.findById(id).orElseThrow(() -> new ServiceException("Location not found."));
    }

    public CampusLocation create(CampusLocation location) {
        access.requireRole(Role.ADMIN);
        validate(location);
        locationDao.create(location);
        return location;
    }

    public CampusLocation update(CampusLocation location) {
        access.requireRole(Role.ADMIN);
        get(location.getId());
        validate(location);
        locationDao.update(location);
        return location;
    }

    public void delete(long id) {
        access.requireRole(Role.ADMIN);
        get(id);
        locationDao.delete(id);
    }

    private static void validate(CampusLocation l) {
        l.setName(Validation.required(l.getName(), "Name", 100));
        if (l.getType() == null) {
            throw new ValidationException("Type is required.");
        }
        l.setBuilding(Validation.optional(l.getBuilding(), 100, "Building"));
        l.setFloor(Validation.optional(l.getFloor(), 40, "Floor"));
        l.setDescription(Validation.optional(l.getDescription(), 500, "Description"));
    }
}
