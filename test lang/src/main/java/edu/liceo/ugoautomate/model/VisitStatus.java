package edu.liceo.ugoautomate.model;

/**
 * Lifecycle of a guest visit: registered, then checked in at the gate
 * (single use), or cancelled.
 */
public enum VisitStatus {
    REGISTERED("Registered"),
    CHECKED_IN("Checked In"),
    CANCELLED("Cancelled");

    private final String displayName;

    VisitStatus(String displayName) {
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
