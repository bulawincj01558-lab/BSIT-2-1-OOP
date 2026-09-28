package edu.liceo.ugoautomate.util;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Optional;

/**
 * Thin wrapper over the webcam-capture library.
 * <p>
 * Webcam support depends on native drivers that may be missing or
 * incompatible on some machines. {@link #openDefault()} therefore catches
 * every failure (including linkage errors) and returns empty, letting the UI
 * fall back to image upload or typed codes.
 */
public final class WebcamScanner implements AutoCloseable {

    private static final long DISCOVERY_TIMEOUT_MS = 4_000;

    private final Webcam webcam;

    private WebcamScanner(Webcam webcam) {
        this.webcam = webcam;
    }

    /**
     * Opens the default camera. Blocking; call from a background thread.
     *
     * @return an open scanner, or empty if no usable camera is present
     */
    public static Optional<WebcamScanner> openDefault() {
        try {
            Webcam webcam = Webcam.getDefault(DISCOVERY_TIMEOUT_MS);
            if (webcam == null) {
                return Optional.empty();
            }
            Dimension vga = WebcamResolution.VGA.getSize();
            if (!webcam.isOpen() && Arrays.asList(webcam.getViewSizes()).contains(vga)) {
                webcam.setViewSize(vga);
            }
            if (!webcam.open()) {
                return Optional.empty();
            }
            return Optional.of(new WebcamScanner(webcam));
        } catch (Throwable t) {
            // No device, timeout, missing native bridge, or unsupported platform.
            return Optional.empty();
        }
    }

    /** @return the latest frame, or {@code null} if none is available */
    public BufferedImage grabFrame() {
        try {
            return webcam.getImage();
        } catch (Throwable t) {
            return null;
        }
    }

    public String getName() {
        return webcam.getName();
    }

    @Override
    public void close() {
        try {
            webcam.close();
        } catch (Throwable ignored) {
            // Closing a failed device must never crash the UI.
        }
    }
}
