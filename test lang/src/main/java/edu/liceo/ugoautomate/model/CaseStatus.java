package edu.liceo.ugoautomate.model;

public enum CaseStatus {
    OPEN("Open"),
    RESOLVED("Resolved");

    private final String label;

    CaseStatus(String label) {
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
