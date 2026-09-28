package edu.liceo.ugoautomate.ui.common;

import edu.liceo.ugoautomate.model.ScanResult;
import edu.liceo.ugoautomate.util.DateTimeUtil;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Toolkit;
import java.time.LocalDateTime;

/**
 * Large, color-coded banner that gives explicit success/failure feedback for
 * every QR scan or entry verification.
 */
public class ResultBanner extends JPanel {

    private final JLabel heading = new JLabel();
    private final JLabel message = new JLabel();
    private final JLabel footer = new JLabel();

    public ResultBanner(String idleText) {
        super(new BorderLayout(0, 6));
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 24f));
        message.setFont(message.getFont().deriveFont(16f));
        footer.setFont(footer.getFont().deriveFont(12f));
        message.setVerticalAlignment(JLabel.TOP);
        add(heading, BorderLayout.NORTH);
        add(message, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
        showIdle(idleText);
    }

    public void showIdle(String text) {
        style(UiTheme.SURFACE, new Color(0xD9D0D2), UiTheme.TEXT_MUTED);
        heading.setText("Waiting for a code");
        message.setText(html(text));
        footer.setText(" ");
    }

    public void showProcessing() {
        style(UiTheme.SURFACE, new Color(0xD9D0D2), UiTheme.TEXT_MUTED);
        heading.setText("Verifying...");
        message.setText(" ");
        footer.setText(" ");
    }

    /**
     * @param result        outcome to display
     * @param elapsedMillis processing time, shown so the 3-second target is visible
     */
    public void showResult(ScanResult result, long elapsedMillis) {
        if (result.isSuccess()) {
            style(UiTheme.SUCCESS_BG, UiTheme.SUCCESS, UiTheme.SUCCESS);
            heading.setText("SUCCESS");
        } else {
            style(UiTheme.DANGER_BG, UiTheme.DANGER, UiTheme.DANGER);
            heading.setText("FAILED");
            Toolkit.getDefaultToolkit().beep();
        }
        message.setText(html(result.getMessage()));
        footer.setText("Processed in " + elapsedMillis + " ms at "
                + DateTimeUtil.formatTime(LocalDateTime.now()));
    }

    private void style(Color background, Color border, Color text) {
        setBackground(background);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border, 2, true),
                UiTheme.padding(16)));
        heading.setForeground(text);
        message.setForeground(text.darker());
        footer.setForeground(UiTheme.TEXT_MUTED);
    }

    private static String html(String text) {
        String escaped = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return "<html>" + escaped.replace("\n", "<br>") + "</html>";
    }
}
