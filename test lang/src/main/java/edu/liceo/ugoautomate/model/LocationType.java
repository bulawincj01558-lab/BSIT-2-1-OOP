package edu.liceo.ugoautomate.model;

public enum LocationType {
    BUILDING("Building"),
    OFFICE("Office"),
    FACILITY("Facility"),
    LANDMARK("Landmark");

    private final String label;

    LocationType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
