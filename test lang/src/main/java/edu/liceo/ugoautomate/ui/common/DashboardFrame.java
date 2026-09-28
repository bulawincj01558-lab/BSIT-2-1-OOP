package edu.liceo.ugoautomate.ui.common;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.User;
import edu.liceo.ugoautomate.ui.LoginFrame;
import edu.liceo.ugoautomate.util.AppConfig;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Common shell for the Student, Guest, and Administrator dashboards: a header
 * with the signed-in user and Logout, a toolbar with Back (and Menu on narrow
 * screens), a left navigation bar, and a card area showing one page at a time.
 * <p>
 * Responsive behaviour: below {@link Responsive#COMPACT_WIDTH} the sidebar is
 * hidden and its pages move into a Menu popup, so the same structure works on
 * desktops, laptops, and portrait or landscape tablets.
 * <p>
 * Data structure: page history for the Back button is a <b>stack</b> (LIFO)
 * implemented with {@link ArrayDeque}: {@code push} and {@code pop} are O(1).
 */
public abstract class DashboardFrame extends JFrame {

    private static final int MAX_HISTORY = 30;

    protected final AppContext ctx;

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JPanel nav = new JPanel();
    private final JScrollPane navScroll;
    private final ButtonGroup navGroup = new ButtonGroup();
    private final Map<String, JComponent> pages = new LinkedHashMap<>();
    private final Map<String, JToggleButton> navButtons = new LinkedHashMap<>();

    /** Pages visited before the current one; top of the stack = previous page. */
    private final Deque<String> history = new ArrayDeque<>();
    private String currentPage;

    private final JButton backButton = new JButton("← Back");
    private final JButton menuButton = new JButton("≡ Menu");
    private final JLabel currentPageLabel = new JLabel();
    private final JLabel appLabel = new JLabel(AppConfig.APP_NAME);
    private final JLabel universityLabel;
    private final JLabel userLabel;
    private Boolean compact;

    protected DashboardFrame(AppContext ctx, String roleLabel) {
        super(AppConfig.APP_NAME + " - " + roleLabel);
        this.ctx = ctx;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(360, 520));
        Responsive.sizeToScreen(this, 1240, 820);

        universityLabel = new JLabel(AppConfig.UNIVERSITY + "  |  " + roleLabel + " Portal");
        userLabel = new JLabel("Signed in as " + ctx.session().currentUser().map(User::getFullName).orElse(""));

        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setBackground(UiTheme.SURFACE);
        nav.setBorder(BorderFactory.createEmptyBorder(14, 10, 14, 10));
        navScroll = new JScrollPane(nav, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        navScroll.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(0xE2DADC)));
        navScroll.setPreferredSize(new Dimension(220, 0));

        JPanel top = new JPanel(new BorderLayout());
        top.add(buildHeader(), BorderLayout.NORTH);
        top.add(buildToolbar(), BorderLayout.SOUTH);

        JPanel root = new JPanel(new BorderLayout());
        root.add(top, BorderLayout.NORTH);
        root.add(navScroll, BorderLayout.WEST);
        root.add(content, BorderLayout.CENTER);
        setContentPane(root);

        Responsive.onResize(this, this::updateLayoutMode);
    }

    /** Adds a page and its navigation button. Pages are shown in insertion order. */
    protected void addPage(String title, JComponent page) {
        JToggleButton button = new JToggleButton(title);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        button.setAlignmentX(0f);
        button.setFocusPainted(false);
        button.putClientProperty("JButton.buttonType", "toolBarButton");
        button.addActionListener(e -> showPage(title));
        navGroup.add(button);
        nav.add(button);
        nav.add(Box.createVerticalStrut(4));

        content.add(page, title);
        pages.put(title, page);
        navButtons.put(title, button);
    }

    /** Shows a page, remembering the current one for Back, and reloads it if {@link Refreshable}. */
    public void showPage(String title) {
        navigate(title, true);
    }

    private void navigate(String title, boolean remember) {
        JComponent page = pages.get(title);
        if (page == null) {
            return;
        }
        if (remember && currentPage != null && !currentPage.equals(title)) {
            history.push(currentPage);                 // O(1)
            if (history.size() > MAX_HISTORY) {
                history.removeLast();                  // drop the oldest entry, O(1)
            }
        }
        currentPage = title;
        cards.show(content, title);
        navButtons.get(title).setSelected(true);
        currentPageLabel.setText(title);
        backButton.setEnabled(!history.isEmpty());
        if (page instanceof Refreshable refreshable) {
            refreshable.refresh();
        }
    }

    private void goBack() {
        if (!history.isEmpty()) {
            navigate(history.pop(), false);            // O(1)
        }
    }

    private void showMenu() {
        JPopupMenu menu = new JPopupMenu();
        for (String title : pages.keySet()) {
            JMenuItem item = new JMenuItem(title);
            item.setPreferredSize(new Dimension(Math.max(240, item.getPreferredSize().width), 40));
            if (title.equals(currentPage)) {
                item.setFont(item.getFont().deriveFont(Font.BOLD));
            }
            item.addActionListener(e -> showPage(title));
            menu.add(item);
        }
        menu.show(menuButton, 0, menuButton.getHeight());
    }

    /** Switches between the sidebar layout and the compact (menu button) layout. */
    private void updateLayoutMode() {
        boolean nowCompact = getWidth() < Responsive.COMPACT_WIDTH;
        if (compact != null && compact == nowCompact) {
            return;
        }
        compact = nowCompact;
        navScroll.setVisible(!nowCompact);
        menuButton.setVisible(nowCompact);
        universityLabel.setVisible(!nowCompact);
        userLabel.setVisible(!nowCompact);
        appLabel.setFont(appLabel.getFont().deriveFont(Font.BOLD, nowCompact ? 16f : 18f));
        getContentPane().revalidate();
        getContentPane().repaint();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setBackground(UiTheme.PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 12));

        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        appLabel.setForeground(Color.WHITE);
        appLabel.setFont(appLabel.getFont().deriveFont(Font.BOLD, 18f));
        universityLabel.setForeground(new Color(0xF3D98B));
        brand.add(appLabel);
        brand.add(universityLabel);

        userLabel.setForeground(Color.WHITE);
        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> logout());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 2));
        right.setOpaque(false);
        right.add(userLabel);
        right.add(logout);

        header.add(brand, BorderLayout.CENTER);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel buildToolbar() {
        backButton.setEnabled(false);
        backButton.setToolTipText("Go back to the previous page");
        backButton.addActionListener(e -> goBack());
        menuButton.addActionListener(e -> showMenu());
        currentPageLabel.setForeground(UiTheme.TEXT_MUTED);

        JPanel toolbar = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 4));
        toolbar.setBackground(Color.WHITE);
        toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xE2DADC)));
        toolbar.add(menuButton);
        toolbar.add(backButton);
        toolbar.add(currentPageLabel);
        return toolbar;
    }

    private void logout() {
        if (!Dialogs.confirm(this, "Do you want to log out?")) {
            return;
        }
        ctx.auth().logout();
        dispose();
        new LoginFrame(ctx).setVisible(true);
    }
}
