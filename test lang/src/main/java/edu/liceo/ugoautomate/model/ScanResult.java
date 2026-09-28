package edu.liceo.ugoautomate.model;

/**
 * Outcome of processing a QR scan or entry verification. Every scan returns
 * one of these so the UI can always show explicit success/failure feedback.
 */
public final class ScanResult {

    private final boolean success;
    private final String message;

    private ScanResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static ScanResult success(String message) {
        return new ScanResult(true, message);
    }

    public static ScanResult failure(String message) {
        return new ScanResult(false, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return (success ? "SUCCESS: " : "FAILED: ") + message;
    }
}
