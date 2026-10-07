package edu.liceo.ugoautomate.ui.common;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class TypewriterText extends HBox {
    private static final Color FLARE = Color.rgb(255, 190, 60);

    private static final Interpolator BACK_OUT = new Interpolator() {
        @Override
        protected double curve(double t) {
            double overshoot = 1.70158;
            double u = t - 1;
            return 1 + (overshoot + 1) * u * u * u + overshoot * u * u;
        }
    };

    private final String text;
    private final SequentialTransition typing = new SequentialTransition();

    public TypewriterText(String text, String styleClass) {
        this.text = text;
        setAlignment(Pos.CENTER);
        for (char c : text.toCharArray()) {
            Text letter = new Text(String.valueOf(c));
            letter.getStyleClass().add(styleClass);
            letter.setOpacity(0);
            getChildren().add(c == ' ' ? letter : withFlare(letter));
        }
    }

    public String getText() {
        return text;
    }

    public void play(Duration perLetter, Runnable onDone) {
        typing.getChildren().clear();
        for (Node child : getChildren()) {
            PauseTransition beat = new PauseTransition(perLetter);
            beat.setOnFinished(e -> boom(child));
            typing.getChildren().add(beat);
        }

        typing.getChildren().add(new PauseTransition(Duration.millis(350)));
        typing.setOnFinished(e -> {
            if (onDone != null) {
                onDone.run();
            }
        });
        typing.playFromStart();
    }

    private static StackPane withFlare(Text letter) {
        Circle flare = new Circle(0, new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(255, 245, 200, 0.95)), new Stop(0.4, FLARE.deriveColor(0, 1, 1, 0.7)),
                new Stop(1, Color.TRANSPARENT)));
        flare.setManaged(false);

        flare.setVisible(false);
        flare.setBlendMode(BlendMode.ADD);
        flare.setMouseTransparent(true);
        StackPane cell = new StackPane(flare, letter);
        flare.centerXProperty().bind(cell.widthProperty().divide(2));
        flare.centerYProperty().bind(cell.heightProperty().divide(2));
        return cell;
    }

    private static void boom(Node child) {
        Text letter;
        Circle flare = null;
        if (child instanceof StackPane cell) {
            flare = (Circle) cell.getChildren().get(0);
            letter = (Text) cell.getChildren().get(1);
        } else {
            letter = (Text) child;
        }
        double size = letter.getFont().getSize();

        FadeTransition appear = new FadeTransition(Duration.millis(90), letter);
        appear.setFromValue(0);
        appear.setToValue(1);
        ScaleTransition pop = new ScaleTransition(Duration.millis(280), letter);
        pop.setFromX(2.6);
        pop.setFromY(2.6);
        pop.setToX(1);
        pop.setToY(1);
        pop.setInterpolator(BACK_OUT);

        DropShadow glow = new DropShadow(size * 1.2, FLARE);
        glow.setSpread(0.35);
        letter.setEffect(glow);
        Timeline glowFade = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(glow.radiusProperty(), size * 1.2)),
                new KeyFrame(Duration.millis(450), new KeyValue(glow.radiusProperty(), size * 0.12, Interpolator.EASE_OUT)));
        glowFade.setOnFinished(e -> letter.setEffect(new DropShadow(size * 0.15, FLARE.deriveColor(0, 1, 1, 0.6))));

        ParallelTransition all = new ParallelTransition(appear, pop, glowFade);
        if (flare != null) {
            Circle burst = flare;
            burst.setVisible(true);
            Timeline flash = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(flare.radiusProperty(), size * 0.1),
                            new KeyValue(flare.opacityProperty(), 1)),
                    new KeyFrame(Duration.millis(380),
                            new KeyValue(flare.radiusProperty(), size * 1.1, Interpolator.EASE_OUT),
                            new KeyValue(flare.opacityProperty(), 0, Interpolator.EASE_IN)));
            flash.setOnFinished(e -> burst.setVisible(false));
            all.getChildren().add(flash);
        }
        all.play();
    }
}
