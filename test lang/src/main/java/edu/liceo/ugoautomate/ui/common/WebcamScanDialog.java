package edu.liceo.ugoautomate.ui.common;

import edu.liceo.ugoautomate.util.QrCodeUtil;
import edu.liceo.ugoautomate.util.WebcamScanner;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.Optional;

/**
 * Live camera preview that decodes QR codes from webcam frames.
 * <p>
 * The camera is opened and read on a background thread. If no camera is
 * available the dialog explains the fallback options instead of failing.
 */
public final class WebcamScanDialog extends JDialog {

    private static final long FRAME_INTERVAL_MS = 120;

    private final JLabel preview = new JLabel("Starting camera...", SwingConstants.CENTER);
    private volatile boolean running = true;
    private volatile String result;

    private WebcamScanDialog(Window owner) {
        super(owner, "Scan QR Code with Camera", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        preview.setPreferredSize(new Dimension(640, 480));

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());
        JLabel hint = UiTheme.subtitle("Hold the QR code steady in front of the camera.");

        JPanel south = new JPanel(new BorderLayout());
        south.setBorder(UiTheme.padding(10));
        south.add(hint, BorderLayout.WEST);
        south.add(cancel, BorderLayout.EAST);

        getContentPane().add(preview, BorderLayout.CENTER);
        getContentPane().add(south, BorderLayout.SOUTH);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                running = false;
            }
        });
        UiTheme.fitToScreen(this, owner);
    }

    /**
     * Opens the camera dialog and blocks until a code is read or the user cancels.
     *
     * @return the decoded QR text, or empty if cancelled / no camera
     */
    public static Optional<String> scan(Component parent) {
        Window owner = parent instanceof Window w ? w : SwingUtilities.getWindowAncestor(parent);
        WebcamScanDialog dialog = new WebcamScanDialog(owner);
        dialog.startCapture();
        dialog.setVisible(true);
        return Optional.ofNullable(dialog.result);
    }

    private void startCapture() {
        Thread worker = new Thread(() -> {
            Optional<WebcamScanner> camera = WebcamScanner.openDefault();
            if (camera.isEmpty()) {
                SwingUtilities.invokeLater(() -> preview.setText("<html><div style='text-align:center'>"
                        + "<b>No camera detected.</b><br><br>"
                        + "Close this window and use <b>Upload QR Image</b> or<br>"
                        + "<b>type the code</b> instead.</div></html>"));
                return;
            }
            try (WebcamScanner scanner = camera.get()) {
                while (running) {
                    BufferedImage frame = scanner.grabFrame();
                    if (frame != null) {
                        SwingUtilities.invokeLater(() -> {
                            preview.setText(null);
                            preview.setIcon(new ImageIcon(frame));
                        });
                        Optional<String> decoded = QrCodeUtil.decode(frame, false);
                        if (decoded.isPresent()) {
                            result = decoded.get();
                            running = false;
                            SwingUtilities.invokeLater(this::dispose);
                            break;
                        }
                    }
                    pause();
                }
            }
        }, "qr-webcam-scanner");
        worker.setDaemon(true);
        worker.start();
    }

    private static void pause() {
        try {
            Thread.sleep(FRAME_INTERVAL_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
