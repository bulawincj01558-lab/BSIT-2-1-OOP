package edu.liceo.ugoautomate.model;

/**
 * A building, office, facility, or landmark on campus, shown in the Campus
 * Navigator list with its details.
 */
public class CampusLocation {

    private long id;
    private String name;
    private LocationType type;
    private String building;
    private String floor;
    private String description;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocationType getType() {
        return type;
    }

    public void setType(LocationType type) {
        this.type = type;
    }

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public String getFloor() {
        return floor;
    }

    public void setFloor(String floor) {
        this.floor = floor;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /** Persisted locations are equal when their ids match; unsaved ones only by identity. */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CampusLocation other)) {
            return false;
        }
        return id != 0 && id == other.id;
    }

    @Override
    public int hashCode() {
        return id != 0 ? Long.hashCode(id) : System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return name;
    }
}
