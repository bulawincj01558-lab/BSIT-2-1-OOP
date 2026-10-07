package edu.liceo.ugoautomate.model;

public enum EntrantType {
    STUDENT("Student"),
    GUEST("Guest");

    private final String label;

    EntrantType(String label) {
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
