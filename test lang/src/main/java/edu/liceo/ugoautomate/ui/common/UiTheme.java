package edu.liceo.ugoautomate.ui.common;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Window;
import java.util.Map;

/**
 * Central look-and-feel settings and small factory methods so every screen
 * shares the same colors, fonts, spacing, and button styles. Controls are
 * sized for touch as well as mouse use.
 */
public final class UiTheme {

    public static final Color PRIMARY = new Color(0x7A0019);      // Liceo maroon
    public static final Color PRIMARY_DARK = new Color(0x4F0010);
    public static final Color ACCENT = new Color(0xE0A800);       // gold
    public static final Color SURFACE = new Color(0xF7F4F4);
    public static final Color TEXT_MUTED = new Color(0x5F5F66);
    public static final Color SUCCESS = new Color(0x1B6E2F);
    public static final Color SUCCESS_BG = new Color(0xE2F4E6);
    public static final Color DANGER = new Color(0xA0001C);
    public static final Color DANGER_BG = new Color(0xFBE4E7);

    private UiTheme() {
    }

    /** Installs FlatLaf with the Liceo accent color; falls back to the system look and feel. */
    public static void install() {
        try {
            FlatLaf.setGlobalExtraDefaults(Map.of("@accentColor", "#7A0019"));
            FlatLightLaf.setup();
            UIManager.put("Button.arc", 10);
            UIManager.put("Component.arc", 8);
            UIManager.put("TextComponent.arc", 8);
            // Touch-friendly sizes: taller rows, roomier buttons, wider scroll bars.
            UIManager.put("Button.margin", new Insets(7, 14, 7, 14));
            UIManager.put("TextField.margin", new Insets(5, 6, 5, 6));
            UIManager.put("PasswordField.margin", new Insets(5, 6, 5, 6));
            UIManager.put("Table.rowHeight", 32);
            UIManager.put("List.cellHeight", 32);
            UIManager.put("ScrollBar.width", 14);
            UIManager.put("ScrollBar.showButtons", false);
            UIManager.put("Table.showHorizontalLines", true);
            UIManager.put("TabbedPane.showTabSeparators", true);
            UIManager.put("TabbedPane.tabHeight", 38);
            UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Default Metal look and feel is acceptable.
            }
        }
    }

    public static JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(PRIMARY);
        button.setForeground(Color.WHITE);
        button.setFont(button.getFont().deriveFont(Font.BOLD));
        button.setFocusPainted(false);
        return button;
    }

    public static JButton dangerButton(String text) {
        JButton button = new JButton(text);
        button.setForeground(DANGER);
        return button;
    }

    public static JLabel title(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 22f));
        label.setForeground(PRIMARY_DARK);
        return label;
    }

    public static JLabel subtitle(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT_MUTED);
        return label;
    }

    /** Multi-line text that wraps to the available width (unlike a plain label). */
    public static JTextArea paragraph(String text) {
        JTextArea area = new JTextArea(text) {
            /**
             * A wrapped text area reports its current width as its minimum,
             * which would stop the layout from ever shrinking again after a
             * window is made narrower. Report a small minimum instead.
             */
            @Override
            public Dimension getMinimumSize() {
                return new Dimension(40, super.getMinimumSize().height);
            }
        };
        area.setEditable(false);
        area.setFocusable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setBorder(null);
        area.setFont(new JLabel().getFont());
        return area;
    }

    public static Border padding(int size) {
        return BorderFactory.createEmptyBorder(size, size, size, size);
    }

    /**
     * Wraps content in a vertical scroll pane that re-flows to the window width.
     * Used for pages and dialogs so nothing is cut off on small or portrait screens.
     */
    public static JScrollPane scroll(JComponent content) {
        ScrollablePanel holder = new ScrollablePanel(new BorderLayout());
        holder.add(content, BorderLayout.CENTER);
        JScrollPane scrollPane = new JScrollPane(holder,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        return scrollPane;
    }

    /** Content pane for dialogs: scrolls when the screen is smaller than the form. */
    public static JPanel dialogBody(JComponent content) {
        JPanel body = new JPanel(new BorderLayout());
        body.add(scroll(content), BorderLayout.CENTER);
        return body;
    }

    /** Packs a window, keeps it inside the visible screen, and centers it on {@code relativeTo}. */
    public static void fitToScreen(Window window, Component relativeTo) {
        Responsive.packToScreen(window, relativeTo);
    }

    /**
     * Standard page layout: a title and subtitle header above the content,
     * inside a responsive scroll container.
     */
    public static JPanel page(String title, String subtitle, JComponent content) {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        JLabel t = title(title);
        JTextArea s = paragraph(subtitle);
        s.setForeground(TEXT_MUTED);
        t.setAlignmentX(0f);
        s.setAlignmentX(0f);
        header.add(t);
        header.add(Box.createVerticalStrut(4));
        header.add(s);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));

        JPanel inner = new JPanel(new BorderLayout());
        inner.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        inner.add(header, BorderLayout.NORTH);
        inner.add(content, BorderLayout.CENTER);

        JPanel page = new JPanel(new BorderLayout());
        page.add(scroll(inner), BorderLayout.CENTER);
        return page;
    }

    /** A left-aligned row of buttons that wraps onto new lines when space runs out. */
    public static JPanel buttonRow(JComponent... components) {
        JPanel row = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 4));
        row.setOpaque(false);
        for (JComponent c : components) {
            row.add(c);
        }
        return row;
    }

    /** A titled, padded card for grouping related controls. */
    public static JPanel card(String title, JComponent content) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(0xD9D0D2)), title),
                padding(10)));
        card.add(content, BorderLayout.CENTER);
        return card;
    }
}
