package edu.liceo.ugoautomate.ui;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.User;
import edu.liceo.ugoautomate.ui.admin.AdminDashboard;
import edu.liceo.ugoautomate.ui.common.DashboardController;
import edu.liceo.ugoautomate.ui.common.Views;
import edu.liceo.ugoautomate.ui.common.Views.View;
import edu.liceo.ugoautomate.ui.guest.GuestDashboard;
import edu.liceo.ugoautomate.ui.student.StudentDashboard;
import edu.liceo.ugoautomate.util.AppConfig;
import javafx.stage.Stage;

public final class Navigator {
    private static Stage stage;
    private static AppContext ctx;
    private static DashboardController dashboard;

    private Navigator() {
    }

    static void init(Stage mainStage, AppContext context) {
        stage = mainStage;
        ctx = context;
        stage.setMinWidth(360);
        stage.setMinHeight(520);
        stage.setOnCloseRequest(e -> closeDashboard());
    }

    public static void showLogin() {
        closeDashboard();
        View<LoginController> login = Views.load(ctx, "Login.fxml");
        stage.setTitle(AppConfig.APP_NAME + " - Login");
        stage.setScene(Views.scene(login.root(), 960, 620));
        stage.show();
        login.controller().focus();
    }

    public static void showDashboard(User user) {
        View<DashboardController> view = switch (user.getRole()) {
            case STUDENT -> StudentDashboard.create(ctx, user);
            case GUEST -> GuestDashboard.create(ctx, user);
            case ADMIN -> AdminDashboard.create(ctx, user);
        };
        dashboard = view.controller();
        stage.setTitle(AppConfig.APP_NAME + " - " + user.getRole().getLabel());
        stage.setScene(Views.scene(view.root(), 1280, 800));
        stage.centerOnScreen();
    }

    private static void closeDashboard() {
        if (dashboard != null) {
            dashboard.close();
            dashboard = null;
        }
    }
}
