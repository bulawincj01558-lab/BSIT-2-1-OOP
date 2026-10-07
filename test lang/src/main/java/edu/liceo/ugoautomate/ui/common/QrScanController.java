package edu.liceo.ugoautomate.ui.common;

import com.github.sarxos.webcam.Webcam;
import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.ScanResult;
import edu.liceo.ugoautomate.model.VerificationMethod;
import edu.liceo.ugoautomate.ui.common.Views.View;
import edu.liceo.ugoautomate.util.QrCodes;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

public class QrScanController implements Page {
    private static final long FRAME_DELAY_MS = 150;

    private static final long DUPLICATE_WINDOW_MS = 4000;

    private BiFunction<String, VerificationMethod, ScanResult> processor;
    private ScanHistory scanHistory;

    @FXML private Label heading;
    @FXML private Label help;
    @FXML private ResultBanner banner;
    @FXML private GridPane body;
    @FXML private StackPane previewPane;
    @FXML private ImageView preview;
    @FXML private Label previewText;
    @FXML private Button startCamera;
    @FXML private Button stopCamera;
    @FXML private Label cameraStatus;
    @FXML private TextField manualCode;
    @FXML private ListView<String> historyList;

    private final Map<String, Long> recentCodes = new HashMap<>();
    private final AtomicBoolean scanning = new AtomicBoolean(false);
    private final AtomicBoolean processing = new AtomicBoolean(false);
    private volatile Thread cameraThread;

    public static View<QrScanController> create(AppContext ctx, String title, String instruction, ScanHistory scanHistory,
                                                BiFunction<String, VerificationMethod, ScanResult> processor) {
        View<QrScanController> view = Views.load(ctx, "common/QrScan.fxml");
        QrScanController c = view.controller();
        c.heading.setText(title);
        c.help.setText(instruction);
        c.processor = processor;
        c.scanHistory = scanHistory;
        scanHistory.addListener(c::refreshHistory);
        c.refreshHistory();
        return view;
    }

    @FXML
    private void initialize() {
        Responsive.split(body, 760, 0.55);
        preview.fitWidthProperty().bind(previewPane.widthProperty().subtract(16));
        preview.fitHeightProperty().bind(previewPane.heightProperty().subtract(16));
    }

    @FXML
    private void submitManual() {
        String code = manualCode.getText().trim();
        if (code.isEmpty()) {
            banner.showResult(ScanResult.failure("No code entered", "Type or paste the QR code text first."));
            return;
        }
        process(code, VerificationMethod.MANUAL_ENTRY, manualCode::clear);
    }

    @FXML
    private void chooseImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose a QR code image");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Images (PNG, JPG, BMP, GIF)", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif"));
        File file = chooser.showOpenDialog(Views.windowOf(banner));
        if (file == null) {
            return;
        }
        Async.run(banner, () -> QrCodes.decode(file), decoded -> {
            if (decoded.isEmpty()) {
                showResult(ScanResult.failure("No QR code found",
                        "Could not find a QR code in \"" + file.getName() + "\". Try a clearer, closer image."));
            } else {
                process(decoded.get(), VerificationMethod.IMAGE_FILE, null);
            }
        });
    }

    private void process(String code, VerificationMethod method, Runnable onSuccess) {
        if (!processing.compareAndSet(false, true)) {
            return;
        }
        banner.showInfo("Verifying...");
        Async.run(banner, () -> processor.apply(code, method), result -> {
            showResult(result);
            if (result.isSuccess() && onSuccess != null) {
                onSuccess.run();
            }
        }, () -> processing.set(false));
    }

    private void showResult(ScanResult result) {
        banner.showResult(result);
        scanHistory.offer(DisplayTime.formatTime(result.getProcessedAt()) + "  "
                + (result.isSuccess() ? "✔ " : "✖ ") + result.getTitle());
    }

    private void refreshHistory() {
        List<String> oldestFirst = scanHistory.oldestFirst();
        historyList.getItems().clear();
        for (int i = oldestFirst.size() - 1; i >= 0; i--) {
            historyList.getItems().add(oldestFirst.get(i));
        }
    }

    @FXML
    private void startCamera() {
        if (!scanning.compareAndSet(false, true)) {
            return;
        }
        startCamera.setDisable(true);
        stopCamera.setDisable(false);
        cameraStatus.setText("Starting camera...");
        Thread thread = new Thread(this::cameraLoop, "qr-camera");
        thread.setDaemon(true);
        cameraThread = thread;
        thread.start();
    }

    @FXML
    private void stopCamera() {
        scanning.set(false);
        Thread thread = cameraThread;
        if (thread != null) {
            thread.interrupt();
        }
    }

    private void cameraLoop() {
        Webcam webcam = null;
        try {
            webcam = Webcam.getDefault(3000);
            if (webcam == null) {
                Platform.runLater(() -> cameraStatus.setText("No camera detected - use image upload or manual entry."));
                return;
            }
            Dimension best = Arrays.stream(webcam.getViewSizes())
                    .filter(d -> d.width <= 640)
                    .max(Comparator.comparingInt(d -> d.width * d.height))
                    .orElse(null);
            if (best != null) {
                webcam.setViewSize(best);
            }
            webcam.open();
            String name = webcam.getName();
            Platform.runLater(() -> cameraStatus.setText("Scanning with " + name));

            while (scanning.get() && !Thread.currentThread().isInterrupted()) {
                BufferedImage frame = webcam.getImage();
                if (frame != null) {
                    Image fxFrame = SwingFXUtils.toFXImage(frame, null);
                    Platform.runLater(() -> {
                        previewText.setText("");
                        preview.setImage(fxFrame);
                    });
                    Optional<String> decoded = QrCodes.decode(frame);
                    decoded.ifPresent(this::onCameraCode);
                }
                Thread.sleep(FRAME_DELAY_MS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Throwable t) {
            String reason = t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
            Platform.runLater(() -> cameraStatus.setText("Camera unavailable (" + reason + "). Use image upload or manual entry."));
        } finally {
            if (webcam != null) {
                try {
                    webcam.close();
                } catch (Throwable ignored) {
                }
            }
            scanning.set(false);
            Platform.runLater(() -> {
                preview.setImage(null);
                previewText.setText("Camera is off");
                startCamera.setDisable(false);
                stopCamera.setDisable(true);
                String status = cameraStatus.getText();
                if (status.startsWith("Scanning") || status.startsWith("Starting")) {
                    cameraStatus.setText("");
                }
            });
        }
    }

    private void onCameraCode(String code) {
        long now = System.currentTimeMillis();
        synchronized (recentCodes) {
            Long lastSeen = recentCodes.get(code);
            if (lastSeen != null && now - lastSeen < DUPLICATE_WINDOW_MS) {
                return;
            }
            recentCodes.put(code, now);
            recentCodes.values().removeIf(t -> now - t > DUPLICATE_WINDOW_MS * 10);
        }
        Platform.runLater(() -> process(code, VerificationMethod.WEBCAM, null));
    }

    @Override
    public void onShow() {
        Platform.runLater(manualCode::requestFocus);
    }

    @Override
    public void onHide() {
        stopCamera();
    }
}
