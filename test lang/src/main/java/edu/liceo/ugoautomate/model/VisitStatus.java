package edu.liceo.ugoautomate.model;

public enum VisitStatus {
    PENDING("Pending"),
    CHECKED_IN("Checked in"),
    CHECKED_OUT("Checked out"),
    CANCELLED("Cancelled");

    private final String label;

    VisitStatus(String label) {
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
