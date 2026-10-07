package edu.liceo.ugoautomate.model;

public enum SessionType {
    CLASS("Class"),
    EVENT("Event");

    private final String label;

    SessionType(String label) {
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
