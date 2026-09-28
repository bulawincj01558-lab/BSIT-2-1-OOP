package edu.liceo.ugoautomate.ui.common;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.util.function.Consumer;

/**
 * Dashboard landing page: a greeting plus one shortcut card per feature.
 * Cards show two per row on wide screens and one per row on narrow ones.
 */
public class HomePanel extends JPanel {

    /** A shortcut card: the page to open and a one-line explanation. */
    public record Shortcut(String page, String description) {
    }

    public HomePanel(String greeting, String intro, Consumer<String> navigator, Shortcut... shortcuts) {
        super(new BorderLayout());
        JPanel grid = new JPanel(new ResponsiveGridLayout(300, 2, 14, 14));
        grid.setOpaque(false);
        for (Shortcut shortcut : shortcuts) {
            grid.add(card(shortcut, navigator));
        }
        JPanel holder = new JPanel(new BorderLayout());
        holder.add(grid, BorderLayout.NORTH);
        add(UiTheme.page(greeting, intro, holder), BorderLayout.CENTER);
    }

    private static JPanel card(Shortcut shortcut, Consumer<String> navigator) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE2DADC)),
                UiTheme.padding(14)));

        JLabel title = new JLabel(shortcut.page());
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        title.setForeground(UiTheme.PRIMARY_DARK);

        JTextArea text = UiTheme.paragraph(shortcut.description());
        text.setForeground(UiTheme.TEXT_MUTED);

        JButton open = new JButton("Open " + shortcut.page());
        open.addActionListener(e -> navigator.accept(shortcut.page()));

        card.add(title, BorderLayout.NORTH);
        card.add(text, BorderLayout.CENTER);
        card.add(UiTheme.buttonRow(open), BorderLayout.SOUTH);
        return card;
    }
}
