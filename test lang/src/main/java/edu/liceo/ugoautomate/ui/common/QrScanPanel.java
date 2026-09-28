package edu.liceo.ugoautomate.ui.common;

import edu.liceo.ugoautomate.model.ScanResult;
import edu.liceo.ugoautomate.util.DateTimeUtil;
import edu.liceo.ugoautomate.util.QrCodeUtil;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Function;

/**
 * Reusable QR input panel offering three ways to provide a code: live camera,
 * uploaded image file, or typed/pasted text. Every attempt ends with an
 * explicit result on the {@link ResultBanner}.
 * <p>
 * Data structures used here:
 * <ul>
 *   <li><b>Queue</b> ({@link ArrayDeque}) - codes that arrive while another is
 *       still being verified wait here and are processed in arrival order
 *       (FIFO): {@code offer}/{@code poll} are O(1).</li>
 *   <li><b>Linked list</b> ({@link LinkedList}) - the last {@value #HISTORY_SIZE}
 *       results, newest first: {@code addFirst}/{@code removeLast} are O(1).</li>
 * </ul>
 */
public class QrScanPanel extends JPanel {

    private static final int HISTORY_SIZE = 10;

    private final Function<String, ScanResult> processor;
    private final ResultBanner banner;
    private final JTextField codeField = new JTextField(18);

    private final Queue<String> pendingScans = new ArrayDeque<>();
    private final LinkedList<String> recentScans = new LinkedList<>();
    private final DefaultListModel<String> historyModel = new DefaultListModel<>();
    private final JLabel queueLabel = new JLabel("Queue: empty");
    private boolean busy;

    /**
     * @param instructions short guidance shown above the options
     * @param processor    service call that validates and records the scanned payload
     */
    public QrScanPanel(String instructions, Function<String, ScanResult> processor) {
        super(new BorderLayout(0, 14));
        this.processor = processor;
        this.banner = new ResultBanner(instructions);

        JButton cameraButton = UiTheme.primaryButton("Scan with Camera");
        JButton uploadButton = new JButton("Upload QR Image...");
        JButton submitButton = new JButton("Submit");
        cameraButton.addActionListener(e -> scanWithCamera());
        uploadButton.addActionListener(e -> uploadImage());
        submitButton.addActionListener(e -> submitTyped());
        codeField.addActionListener(e -> submitTyped());
        codeField.putClientProperty("JTextField.placeholderText", "Paste the code (LUGA1|...)");

        JPanel typed = new JPanel(new BorderLayout(8, 0));
        typed.setOpaque(false);
        typed.add(codeField, BorderLayout.CENTER);
        typed.add(submitButton, BorderLayout.EAST);

        JPanel options = new JPanel(new ResponsiveGridLayout(240, 3, 12, 12));
        options.add(option("1. Camera", "Uses your webcam if one is connected.", UiTheme.buttonRow(cameraButton)));
        options.add(option("2. Image file", "Pick a photo or screenshot of the QR code.", UiTheme.buttonRow(uploadButton)));
        options.add(option("3. Type the code", "Use this if scanning is not possible.", typed));

        JList<String> historyList = new JList<>(historyModel);
        historyList.setVisibleRowCount(5);
        JScrollPane historyScroll = new JScrollPane(historyList);
        historyScroll.setPreferredSize(new Dimension(200, 150));
        queueLabel.setForeground(UiTheme.TEXT_MUTED);
        JPanel history = new JPanel(new BorderLayout(0, 6));
        history.add(queueLabel, BorderLayout.NORTH);
        history.add(historyScroll, BorderLayout.CENTER);

        banner.setPreferredSize(new Dimension(200, 190));
        add(options, BorderLayout.NORTH);
        add(banner, BorderLayout.CENTER);
        add(UiTheme.card("Recent scans (newest first)", history), BorderLayout.SOUTH);
    }

    /** Shows a result produced outside the QR flow (e.g. credential verification) and records it. */
    public void showResult(ScanResult result, long elapsedMillis) {
        banner.showResult(result, elapsedMillis);
        remember(result);
    }

    public ResultBanner getBanner() {
        return banner;
    }

    /**
     * Validates a payload through the processor and shows the outcome. If a
     * verification is already running, the payload is queued and handled next.
     */
    public void process(String payload) {
        if (payload == null || payload.isBlank()) {
            showResult(ScanResult.failure("Please scan, upload, or type a QR code first."), 0);
            return;
        }
        if (busy) {
            pendingScans.offer(payload.trim());                  // enqueue at the rear, O(1)
            updateQueueLabel();
            return;
        }
        busy = true;
        banner.showProcessing();
        long started = System.nanoTime();
        Async.run(this, () -> processor.apply(payload.trim()),
                result -> showResult(result, (System.nanoTime() - started) / 1_000_000),
                () -> {
                    busy = false;
                    String next = pendingScans.poll();           // dequeue from the front, O(1)
                    updateQueueLabel();
                    if (next != null) {
                        process(next);
                    }
                });
    }

    private void submitTyped() {
        String text = codeField.getText();
        codeField.setText("");
        process(text);
    }

    private void remember(ScanResult result) {
        String firstLine = result.getMessage().split("\n", 2)[0];
        recentScans.addFirst(DateTimeUtil.formatTime(LocalDateTime.now())
                + (result.isSuccess() ? "   SUCCESS   " : "   FAILED   ") + firstLine);   // O(1)
        if (recentScans.size() > HISTORY_SIZE) {
            recentScans.removeLast();                                                   // O(1)
        }
        historyModel.clear();
        recentScans.forEach(historyModel::addElement);
    }

    private void updateQueueLabel() {
        queueLabel.setText(pendingScans.isEmpty() ? "Queue: empty"
                : "Queue: " + pendingScans.size() + " code(s) waiting to be verified");
    }

    private void scanWithCamera() {
        Optional<String> code = WebcamScanDialog.scan(this);
        code.ifPresent(this::process);
    }

    private void uploadImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select QR Code Image");
        chooser.setFileFilter(new FileNameExtensionFilter("Images (PNG, JPG, GIF, BMP)", "png", "jpg", "jpeg", "gif", "bmp"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path file = chooser.getSelectedFile().toPath();
        Async.run(this, () -> QrCodeUtil.decode(file), decoded -> {
            if (decoded.isPresent()) {
                process(decoded.get());
            } else {
                showResult(ScanResult.failure("No QR code could be read from " + file.getFileName()
                        + ".\nTry a clearer, well-lit image or type the code."), 0);
            }
        });
    }

    private static JPanel option(String title, String description, JPanel control) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(title), UiTheme.padding(8)));
        panel.add(UiTheme.paragraph(description), BorderLayout.NORTH);
        panel.add(control, BorderLayout.CENTER);
        return panel;
    }
}
