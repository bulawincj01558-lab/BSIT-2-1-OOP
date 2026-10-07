package edu.liceo.ugoautomate.model;

import java.time.LocalDateTime;

public final class ScanResult {
    private final boolean success;
    private final String title;
    private final String message;
    private final LocalDateTime processedAt;

    private ScanResult(boolean success, String title, String message) {
        this.success = success;
        this.title = title;
        this.message = message;
        this.processedAt = LocalDateTime.now();
    }

    public static ScanResult success(String title, String message) {
        return new ScanResult(true, title, message);
    }

    public static ScanResult failure(String title, String message) {
        return new ScanResult(false, title, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    @Override
    public String toString() {
        return (success ? "SUCCESS: " : "FAILED: ") + title + " - " + message;
    }
}
