package edu.liceo.ugoautomate.ui.common;

import edu.liceo.ugoautomate.model.ScanResult;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class ResultBanner extends HBox {
    private static final String[] STATES = {"banner-success", "banner-error", "banner-info"};

    @FXML private Label icon;
    @FXML private Label title;
    @FXML private Label message;

    public ResultBanner() {
        Views.loadInto(this, "common/ResultBanner.fxml");
        showInfo("Waiting for a QR code...");
    }

    public void showResult(ScanResult result) {
        if (result.isSuccess()) {
            apply("✔", result.getTitle(), result.getMessage(), "banner-success");
        } else {
            apply("✖", result.getTitle(), result.getMessage(), "banner-error");
        }
    }

    public void showInfo(String text) {
        apply("ℹ", "Ready", text, "banner-info");
    }

    private void apply(String symbol, String titleText, String messageText, String state) {
        icon.setText(symbol);
        title.setText(titleText);
        message.setText(messageText == null ? "" : messageText);
        getStyleClass().removeAll(STATES);
        getStyleClass().add(state);
    }
}
