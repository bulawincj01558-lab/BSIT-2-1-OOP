package edu.liceo.ugoautomate.model;

public class CampusLocation {
    private long id;
    private String name;
    private LocationType type;
    private String building;
    private String floor;
    private String description;

    public String getPlacement() {
        boolean hasBuilding = building != null && !building.isBlank();
        boolean hasFloor = floor != null && !floor.isBlank();
        if (hasBuilding && hasFloor) {
            return building + ", " + floor;
        }
        if (hasBuilding) {
            return building;
        }
        return hasFloor ? floor : "-";
    }

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

    @Override
    public String toString() {
        return name;
    }
}
