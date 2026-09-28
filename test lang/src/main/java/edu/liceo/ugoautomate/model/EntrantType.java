package edu.liceo.ugoautomate.model;

/**
 * Who passed through campus entry verification.
 */
public enum EntrantType {
    STUDENT("Student"),
    GUEST("Guest");

    private final String displayName;

    EntrantType(String displayName) {
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
