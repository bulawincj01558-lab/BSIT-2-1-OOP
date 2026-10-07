package edu.liceo.ugoautomate.model;

public enum VerificationMethod {
    WEBCAM("Webcam"),
    IMAGE_FILE("Image file"),
    MANUAL_ENTRY("Manual entry");

    private final String label;

    VerificationMethod(String label) {
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
