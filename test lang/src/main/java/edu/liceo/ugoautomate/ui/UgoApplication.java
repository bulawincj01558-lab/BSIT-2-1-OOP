package edu.liceo.ugoautomate.ui;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.ui.common.Dialogs;
import edu.liceo.ugoautomate.ui.common.SpinningLogo;
import edu.liceo.ugoautomate.ui.common.TypewriterText;
import edu.liceo.ugoautomate.ui.common.Views;
import edu.liceo.ugoautomate.util.AppConfig;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.Clock;
import java.util.List;

public class UgoApplication extends Application {
    private static final Duration ICON_TURN = Duration.seconds(7);
    private static final int ICON_FRAMES = 36;

    private AppContext ctx;
    private Timeline iconSpin;

    private boolean introDone;
    private boolean loginShown;

    @Override
    public void start(Stage stage) {
        TypewriterText title = new TypewriterText(AppConfig.APP_NAME, "splash-title");
        TypewriterText status = new TypewriterText("Connecting to "
                + (AppConfig.supabaseConfigPath().isPresent() ? "Supabase" : "the database") + "...", "splash-status");
        ProgressIndicator progress = new ProgressIndicator();
        progress.getStyleClass().add("splash-progress");
        VBox splash = new VBox(18, title, status, progress);
        splash.setAlignment(Pos.CENTER);
        splash.getStyleClass().add("splash");
        stage.setTitle(AppConfig.APP_NAME);
        stage.getIcons().add(Views.appIcon());
        stage.setScene(Views.scene(splash, 560, 320));
        stage.show();
        spinWindowIcon(stage);
        title.play(Duration.millis(70), () -> status.play(Duration.millis(45), () -> {
            introDone = true;
            showLoginWhenReady(stage);
        }));

        Thread opener = new Thread(() -> {
            try {
                AppContext opened = AppContext.create(AppConfig.openDatabase(), Clock.systemDefaultZone());
                Platform.runLater(() -> {
                    ctx = opened;
                    showLoginWhenReady(stage);
                });
            } catch (RuntimeException e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    Dialogs.error(stage, AppConfig.APP_NAME + " could not start.\n\n" + e.getMessage()
                            + "\n\nDatabase: " + AppConfig.databaseDescription());
                    Platform.exit();
                });
            }
        }, "ugo-startup");
        opener.setDaemon(true);
        opener.start();
    }

    private void showLoginWhenReady(Stage stage) {
        if (introDone && ctx != null && !loginShown) {
            loginShown = true;
            Navigator.init(stage, ctx);
            Navigator.showLogin();
        }
    }

    private void spinWindowIcon(Stage stage) {
        List<Image> frames = SpinningLogo.frames(64, ICON_FRAMES);
        int[] next = {0};
        iconSpin = new Timeline(new KeyFrame(ICON_TURN.divide(ICON_FRAMES), e -> {
            stage.getIcons().setAll(frames.get(next[0]));
            next[0] = (next[0] + 1) % frames.size();
        }));
        iconSpin.setCycleCount(Animation.INDEFINITE);
        iconSpin.play();
    }

    @Override
    public void stop() {
        if (iconSpin != null) {
            iconSpin.stop();
        }
        if (ctx != null) {
            ctx.close();
        }
    }
}
