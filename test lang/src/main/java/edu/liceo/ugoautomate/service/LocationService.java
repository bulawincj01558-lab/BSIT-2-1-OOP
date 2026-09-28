package edu.liceo.ugoautomate.service;

import edu.liceo.ugoautomate.dao.LocationDao;
import edu.liceo.ugoautomate.model.CampusLocation;
import edu.liceo.ugoautomate.model.LocationType;
import edu.liceo.ugoautomate.security.AccessControl;
import edu.liceo.ugoautomate.util.ValidationException;
import edu.liceo.ugoautomate.util.Validators;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Campus Navigator data (F-2.x) and administrator location management (F-5.2).
 * <p>
 * Locations are small, read-mostly reference data, so they are cached in
 * memory for {@link #CACHE_TTL}. Browsing and searching are served from the
 * cache without touching the database; any admin change invalidates it.
 */
public class LocationService {

    static final Duration CACHE_TTL = Duration.ofSeconds(60);

    private final LocationDao locationDao;
    private final AccessControl access;
    private final Clock clock;

    private List<CampusLocation> cache;
    private Instant cacheLoadedAt = Instant.EPOCH;

    public LocationService(LocationDao locationDao, AccessControl access, Clock clock) {
        this.locationDao = locationDao;
        this.access = access;
        this.clock = clock;
    }

    /** @return all campus locations (signed-in users only) */
    public List<CampusLocation> listAll() {
        access.requireAuthenticated();
        return cachedLocations();
    }

    /** Searches name, building, floor, type, and description (F-2.2). */
    public List<CampusLocation> search(String query, LocationType type) {
        return filter(listAll(), query, type);
    }

    /**
     * Public directory of office names, used to help guests fill in the
     * "person / office to visit" field during registration (before login).
     */
    public List<String> officeNames() {
        return cachedLocations().stream()
                .filter(l -> l.getType() == LocationType.OFFICE)
                .map(CampusLocation::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public CampusLocation add(CampusLocation location) {
        access.requireAdmin();
        validate(location);
        locationDao.insert(location);
        invalidate();
        return location;
    }

    public CampusLocation update(CampusLocation location) {
        access.requireAdmin();
        validate(location);
        locationDao.findById(location.getId())
                .orElseThrow(() -> new ServiceException("The selected location no longer exists."));
        locationDao.update(location);
        invalidate();
        return location;
    }

    public void remove(long locationId) {
        access.requireAdmin();
        if (!locationDao.delete(locationId)) {
            throw new ServiceException("The selected location no longer exists.");
        }
        invalidate();
    }

    /**
     * Case-insensitive filter over a location list. Every whitespace-separated
     * term must appear in at least one searchable field.
     */
    public static List<CampusLocation> filter(List<CampusLocation> all, String query, LocationType type) {
        String[] terms = query == null || query.isBlank()
                ? new String[0]
                : query.trim().toLowerCase(Locale.ROOT).split("\\s+");
        return all.stream()
                .filter(l -> type == null || l.getType() == type)
                .filter(l -> {
                    String haystack = String.join(" ",
                            nz(l.getName()), nz(l.getBuilding()), nz(l.getFloor()),
                            l.getType().getDisplayName(), nz(l.getDescription())).toLowerCase(Locale.ROOT);
                    for (String term : terms) {
                        if (!haystack.contains(term)) {
                            return false;
                        }
                    }
                    return true;
                })
                .sorted(Comparator.comparing(CampusLocation::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private synchronized List<CampusLocation> cachedLocations() {
        Instant now = clock.instant();
        if (cache == null || Duration.between(cacheLoadedAt, now).compareTo(CACHE_TTL) > 0) {
            cache = List.copyOf(locationDao.findAll());
            cacheLoadedAt = now;
        }
        return cache;
    }

    private synchronized void invalidate() {
        cache = null;
    }

    private static void validate(CampusLocation l) {
        l.setName(Validators.requireText(l.getName(), "Name", 100));
        if (l.getType() == null) {
            throw new ValidationException("Location type is required.");
        }
        l.setBuilding(Validators.optionalText(l.getBuilding(), "Building", 100));
        l.setFloor(Validators.optionalText(l.getFloor(), "Floor", 50));
        l.setDescription(Validators.optionalText(l.getDescription(), "Description", 1000));
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
