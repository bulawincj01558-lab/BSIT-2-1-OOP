package edu.liceo.ugoautomate.ui.common;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.User;
import edu.liceo.ugoautomate.ui.Navigator;
import edu.liceo.ugoautomate.ui.common.Views.View;
import edu.liceo.ugoautomate.util.AppConfig;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DashboardController {
    private static final double SIDEBAR_BREAKPOINT = 820;
    private static final double COMPACT_HEADER_BREAKPOINT = 560;
    private static final int MAX_HISTORY = 50;

    private final AppContext ctx;
    private final ScanHistory scanHistory = new ScanHistory();

    private final Map<String, View<?>> pages = new LinkedHashMap<>();
    private final Map<String, ScrollPane> scrollers = new LinkedHashMap<>();
    private final Map<String, ToggleButton> navButtons = new LinkedHashMap<>();
    private final ToggleGroup navGroup = new ToggleGroup();
    private final Deque<String> history = new ArrayDeque<>();
    private String currentPage;

    @FXML private BorderPane root;
    @FXML private Button menuButton;
    @FXML private Label appName;
    @FXML private Label roleLabel;
    @FXML private Label userLabel;
    @FXML private Button backButton;
    @FXML private VBox sidebar;
    @FXML private StackPane content;

    public DashboardController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        appName.setText(AppConfig.APP_NAME);
        backButton.setDisable(true);
        root.widthProperty().addListener((obs, old, width) -> applyResponsiveLayout(width.doubleValue()));
        applyResponsiveLayout(1280);
    }

    public void setRole(String roleTitle) {
        roleLabel.setText(roleTitle + " Portal");
        refreshHeader();
    }

    public void addPage(String name, View<?> page) {
        pages.put(name, page);
        ScrollPane scroller = new ScrollPane(page.root());
        scroller.getStyleClass().add("page-scroll");
        Responsive.fillViewport(scroller);
        scrollers.put(name, scroller);

        ToggleButton button = new ToggleButton(name);
        button.getStyleClass().add("nav-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setToggleGroup(navGroup);

        button.setOnAction(e -> {
            button.setSelected(true);
            showPage(name);
        });
        navButtons.put(name, button);
        sidebar.getChildren().add(button);
    }

    public void showFirstPage() {
        pages.keySet().stream().findFirst().ifPresent(this::showPage);
    }

    public void showPage(String name) {
        if (!pages.containsKey(name)) {
            return;
        }
        if (!name.equals(history.peek())) {
            history.push(name);
            if (history.size() > MAX_HISTORY) {
                history.removeLast();
            }
        }
        display(name);
    }

    @FXML
    private void goBack() {
        if (history.size() > 1) {
            history.pop();
            display(history.peek());
        }
    }

    private void display(String name) {
        if (!name.equals(currentPage)) {
            hideCurrentPage();
        }
        currentPage = name;
        ScrollPane scroller = scrollers.get(name);
        scroller.setVvalue(0);
        content.getChildren().setAll(scroller);
        navButtons.get(name).setSelected(true);
        backButton.setDisable(history.size() <= 1);
        if (pages.get(name).controller() instanceof Page page) {
            page.onShow();
        }
    }

    private void hideCurrentPage() {
        if (currentPage != null && pages.get(currentPage).controller() instanceof Page page) {
            page.onHide();
        }
    }

    @FXML
    private void showMenu() {
        ContextMenu menu = new ContextMenu();
        for (String name : pages.keySet()) {
            MenuItem item = new MenuItem(name.equals(currentPage) ? "• " + name : name);
            item.setOnAction(e -> showPage(name));
            menu.getItems().add(item);
        }
        menu.show(menuButton, Side.BOTTOM, 0, 0);
    }

    private void applyResponsiveLayout(double width) {
        boolean narrow = width < SIDEBAR_BREAKPOINT;
        boolean compact = width < COMPACT_HEADER_BREAKPOINT;
        show(sidebar, !narrow);
        show(menuButton, narrow);
        show(roleLabel, !compact);
        show(userLabel, !compact);
        appName.setText(compact ? "U Go Automate" : AppConfig.APP_NAME);
        backButton.setText(compact ? "←" : "← Back");
        menuButton.setText(compact ? "☰" : "☰ Menu");
    }

    private static void show(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    public void refreshHeader() {
        userLabel.setText(ctx.session().current().map(User::getFullName).orElse(""));
    }

    public ScanHistory scanHistory() {
        return scanHistory;
    }

    public List<String> navigationHistory() {
        return new ArrayList<>(history);
    }

    public List<String> pageNames() {
        return new ArrayList<>(pages.keySet());
    }

    public void close() {
        hideCurrentPage();
        currentPage = null;
    }

    @FXML
    private void logout() {
        if (!Dialogs.confirm(root, "Log out of " + AppConfig.APP_NAME + "?")) {
            return;
        }
        ctx.auth().logout();
        Navigator.showLogin();
    }
}
