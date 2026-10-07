package edu.liceo.ugoautomate.ui.common;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.ui.common.Views.View;
import edu.liceo.ugoautomate.util.AppConfig;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class HomeController {
    public record Shortcut(String page, String description) {
    }

    @FXML private Label greeting;
    @FXML private Label roleLine;
    @FXML private FlowPane cards;

    public static View<HomeController> create(AppContext ctx, String greeting, String roleLine,
                                              Consumer<String> openPage, Shortcut... shortcuts) {
        View<HomeController> view = Views.load(ctx, "common/Home.fxml");
        view.controller().fill(greeting, roleLine, openPage, shortcuts);
        return view;
    }

    private void fill(String greetingText, String roleText, Consumer<String> openPage, Shortcut... shortcuts) {
        greeting.setText(greetingText);
        roleLine.setText(roleText + "\n" + AppConfig.APP_NAME + " - " + AppConfig.UNIVERSITY_NAME);
        for (Shortcut shortcut : shortcuts) {
            Label title = new Label(shortcut.page());
            title.getStyleClass().add("card-title");
            Label description = new Label(shortcut.description());
            description.setWrapText(true);
            description.getStyleClass().add("muted");
            Button open = new Button("Open");
            open.getStyleClass().add("primary");
            open.setOnAction(e -> openPage.accept(shortcut.page()));

            VBox card = new VBox(title, description, open);
            card.getStyleClass().addAll("card", "feature-card");
            cards.getChildren().add(card);
        }
    }
}
