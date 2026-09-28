package edu.liceo.ugoautomate.model;

/**
 * The three kinds of accounts supported by the system.
 */
public enum Role {
    STUDENT("Student"),
    GUEST("Guest"),
    ADMIN("Administrator");

    private final String displayName;

    Role(String displayName) {
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
