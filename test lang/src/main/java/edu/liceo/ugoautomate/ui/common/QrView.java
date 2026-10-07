package edu.liceo.ugoautomate.ui.common;

import edu.liceo.ugoautomate.security.QrToken;
import edu.liceo.ugoautomate.util.QrCodes;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.Instant;

public class QrView extends VBox {
    private static final int QR_SIZE = 300;

    @FXML private Label caption;
    @FXML private ImageView image;
    @FXML private Label placeholder;
    @FXML private Label expiry;
    @FXML private TextArea codeText;
    @FXML private Button saveButton;
    @FXML private Button copyButton;

    private final Timeline countdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateExpiry()));
    private BufferedImage currentImage;
    private Instant expiresAt;
    private String fileName = "qr-code.png";

    public QrView() {
        Views.loadInto(this, "common/QrView.fxml");
        countdown.setCycleCount(Animation.INDEFINITE);
        clear("No QR code generated yet.");
    }

    public void show(QrToken token, String captionText, String suggestedFileName) {
        currentImage = QrCodes.generate(token.getEncoded(), QR_SIZE);
        image.setImage(SwingFXUtils.toFXImage(currentImage, null));
        placeholder.setText("");
        caption.setText(captionText);
        codeText.setText(token.getEncoded());
        expiresAt = token.getExpiresAt();
        fileName = suggestedFileName;
        saveButton.setDisable(false);
        copyButton.setDisable(false);
        updateExpiry();
        countdown.play();
    }

    public void clear(String message) {
        countdown.stop();
        currentImage = null;
        expiresAt = null;
        image.setImage(null);
        placeholder.setText(message);
        caption.setText("");
        expiry.setText("");
        codeText.clear();
        saveButton.setDisable(true);
        copyButton.setDisable(true);
    }

    public void stop() {
        countdown.stop();
    }

    private void updateExpiry() {
        if (expiresAt == null) {
            return;
        }
        java.time.Duration left = java.time.Duration.between(Instant.now(), expiresAt);
        boolean warn;
        if (left.isNegative() || left.isZero()) {
            expiry.setText("EXPIRED - generate a new code");
            warn = true;
            countdown.stop();
        } else if (left.toHours() >= 1) {
            expiry.setText("Valid until " + DisplayTime.format(expiresAt));
            warn = false;
        } else {
            expiry.setText(String.format("Expires in %d:%02d", left.toMinutes(), left.toSecondsPart()));
            warn = left.toSeconds() < 60;
        }
        expiry.getStyleClass().setAll("label", warn ? "expiry-warn" : "expiry-ok");
    }

    @FXML
    private void save() {
        if (currentImage == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save QR code");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG image", "*.png"));
        chooser.setInitialFileName(fileName);
        File file = chooser.showSaveDialog(Views.windowOf(this));
        if (file == null) {
            return;
        }
        if (!file.getName().toLowerCase().endsWith(".png")) {
            file = new File(file.getParentFile(), file.getName() + ".png");
        }
        try {
            QrCodes.savePng(currentImage, file);
            Dialogs.info(this, "Saved QR code to\n" + file.getAbsolutePath());
        } catch (IOException ex) {
            Dialogs.error(this, "Could not save the image: " + ex.getMessage());
        }
    }

    @FXML
    private void copy() {
        ClipboardContent content = new ClipboardContent();
        content.putString(codeText.getText());
        Clipboard.getSystemClipboard().setContent(content);
    }
}
