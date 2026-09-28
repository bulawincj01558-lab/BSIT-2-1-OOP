package edu.liceo.ugoautomate.model;

/**
 * Category of a campus location shown in the Campus Navigator.
 */
public enum LocationType {
    BUILDING("Building"),
    OFFICE("Office"),
    FACILITY("Facility"),
    LANDMARK("Landmark");

    private final String displayName;

    LocationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
