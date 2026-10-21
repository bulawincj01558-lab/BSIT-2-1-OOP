package edu.liceo.ugoautomate.security;

public enum QrTokenType {
    ATTENDANCE("an attendance QR code"),

    STUDENT_ENTRY("a student entry QR code"),

    STUDENT_ATTENDANCE("a student attendance QR code"),

    GUEST_PASS("a guest pass");

    private final String description;

    QrTokenType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
