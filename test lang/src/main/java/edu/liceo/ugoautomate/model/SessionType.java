package edu.liceo.ugoautomate.model;

/**
 * Kind of activity an attendance session is created for.
 */
public enum SessionType {
    CLASS("Class Session"),
    EVENT("Event");

    private final String displayName;

    SessionType(String displayName) {
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
