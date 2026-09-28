package edu.liceo.ugoautomate.model;

/**
 * How an entrant's authorization was verified at the gate.
 */
public enum VerificationMethod {
    QR_CODE("QR Code"),
    CREDENTIALS("Account Credentials");

    private final String displayName;

    VerificationMethod(String displayName) {
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
