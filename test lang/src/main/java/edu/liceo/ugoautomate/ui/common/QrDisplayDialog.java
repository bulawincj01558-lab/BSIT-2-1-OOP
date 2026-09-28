package edu.liceo.ugoautomate.ui.common;

import edu.liceo.ugoautomate.util.QrCodeUtil;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;

/**
 * Shows a QR code with options to save it as PNG or copy its text (for the
 * manual "type the code" fallback).
 */
public final class QrDisplayDialog extends JDialog {

    private static final int QR_SIZE = 360;

    private QrDisplayDialog(Window owner, String title, String subtitle, String payload, String fileName) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JLabel heading = new JLabel(title, SwingConstants.CENTER);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 18f));
        heading.setForeground(UiTheme.PRIMARY_DARK);
        JLabel sub = new JLabel("<html><div style='text-align:center'>" + subtitle.replace("\n", "<br>")
                + "</div></html>", SwingConstants.CENTER);
        sub.setForeground(UiTheme.TEXT_MUTED);

        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.add(heading, BorderLayout.NORTH);
        header.add(sub, BorderLayout.CENTER);

        JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBorder(UiTheme.padding(16));
        content.add(header, BorderLayout.NORTH);
        content.add(qrPanel(this, payload, fileName, QR_SIZE), BorderLayout.CENTER);
        content.add(UiTheme.buttonRow(close), BorderLayout.SOUTH);
        setContentPane(UiTheme.dialogBody(content));
        UiTheme.fitToScreen(this, owner);
    }

    public static void show(Component parent, String title, String subtitle, String payload, String fileName) {
        Window owner = parent instanceof Window w ? w : SwingUtilities.getWindowAncestor(parent);
        new QrDisplayDialog(owner, title, subtitle, payload, fileName).setVisible(true);
    }

    /**
     * Builds a QR image with "Save as PNG" and "Copy Code" buttons. Also used
     * inline by pages that display a pass without a dialog.
     */
    public static JComponent qrPanel(Component parent, String payload, String fileName, int size) {
        JLabel image = new JLabel(new ImageIcon(QrCodeUtil.generate(payload, size)));
        image.setBorder(BorderFactory.createLineBorder(new Color(0xE2DADC)));
        image.setHorizontalAlignment(SwingConstants.CENTER);

        JButton save = new JButton("Save as PNG...");
        save.addActionListener(e -> savePng(parent, payload, fileName));
        JButton copy = new JButton("Copy Code");
        copy.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(payload), null);
            Dialogs.info(parent, "The code text was copied to the clipboard.");
        });

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        panel.add(image, BorderLayout.CENTER);
        JPanel buttons = UiTheme.buttonRow(save, copy);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private static void savePng(Component parent, String payload, String fileName) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save QR Code");
        chooser.setFileFilter(new FileNameExtensionFilter("PNG image", "png"));
        chooser.setSelectedFile(new File(fileName.endsWith(".png") ? fileName : fileName + ".png"));
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".png")) {
            file = new File(file.getParentFile(), file.getName() + ".png");
        }
        try {
            QrCodeUtil.writePng(payload, 600, file.toPath());
            Dialogs.info(parent, "QR code saved to\n" + file.getAbsolutePath());
        } catch (IOException ex) {
            Dialogs.error(parent, "Could not save the QR code: " + ex.getMessage());
        }
    }
}
