package edu.liceo.ugoautomate.model;

public enum OffenseSeverity {
    MINOR("Minor"),
    MAJOR("Major"),
    GRAVE("Grave");

    private final String label;

    OffenseSeverity(String label) {
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
