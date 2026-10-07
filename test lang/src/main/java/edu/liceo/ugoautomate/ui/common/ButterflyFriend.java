package edu.liceo.ugoautomate.ui.common;

import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Group;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.transform.Scale;
import javafx.util.Duration;

import java.net.URL;
import java.util.Random;

public class ButterflyFriend extends Group {
    private static final String FACE = "/edu/liceo/ugoautomate/ui/images/butterfly-face.png";
    private static final double FACE_SIZE = 100;
    private static final double SPEED = 150;

    private final Random random = new Random();
    private final Scale leftFlap = new Scale(1, 1, 0, 0);
    private final Scale rightFlap = new Scale(1, 1, 0, 0);
    private final Timeline flapping;
    private double x = -1;
    private double y;
    private double vx;
    private double vy;
    private double targetX;
    private double targetY;
    private double clock;

    private double dizzy;
    private double spin;
    private long lastFrame;

    private final AnimationTimer flight = new AnimationTimer() {
        @Override
        public void handle(long now) {
            double dt = lastFrame == 0 ? 1 / 60.0 : Math.max(0, Math.min(0.05, (now - lastFrame) / 1e9));
            lastFrame = now;
            fly(dt);
        }
    };

    public static void addTo(Pane sky) {
        URL face = ButterflyFriend.class.getResource(FACE);
        if (face != null) {
            sky.getChildren().add(new ButterflyFriend(new Image(face.toExternalForm(), FACE_SIZE * 2, FACE_SIZE * 2, true, true)));
        }
    }

    private ButterflyFriend(Image face) {
        setMouseTransparent(true);
        double s = FACE_SIZE / 64;

        Group left = wing(s);
        left.getTransforms().addAll(leftFlap, new Scale(-1, 1));
        Group right = wing(s);
        right.getTransforms().add(rightFlap);

        ImageView body = new ImageView(face);
        body.setFitWidth(FACE_SIZE);
        body.setFitHeight(FACE_SIZE);
        body.setLayoutX(-FACE_SIZE / 2);
        body.setLayoutY(-FACE_SIZE / 2);
        Circle ring = new Circle(0, 0, FACE_SIZE / 2, Color.TRANSPARENT);
        ring.setStroke(Color.rgb(245, 197, 66));
        ring.setStrokeWidth(3 * s);

        getChildren().addAll(antenna(-1, s), antenna(1, s), left, right, body, ring);
        setEffect(new DropShadow(14 * s, 3 * s, 8 * s, Color.rgb(0, 0, 0, 0.35)));

        flapping = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(leftFlap.xProperty(), 1), new KeyValue(rightFlap.xProperty(), 1)),
                new KeyFrame(Duration.millis(170),
                        new KeyValue(leftFlap.xProperty(), 0.22, Interpolator.EASE_BOTH),
                        new KeyValue(rightFlap.xProperty(), 0.22, Interpolator.EASE_BOTH)));
        flapping.setAutoReverse(true);
        flapping.setCycleCount(Animation.INDEFINITE);

        sceneProperty().addListener((obs, old, scene) -> {
            if (scene == null) {
                flight.stop();
                flapping.stop();
            } else {
                lastFrame = 0;
                flight.start();
                flapping.play();
            }
        });
    }

    private static Group wing(double s) {
        LinearGradient orange = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(255, 196, 60)), new Stop(0.55, Color.rgb(245, 124, 20)),
                new Stop(1, Color.rgb(190, 60, 10)));
        Color edge = Color.rgb(45, 20, 10);

        Path upper = new Path(new MoveTo(0, -6 * s),
                new CubicCurveTo(28 * s, -74 * s, 112 * s, -86 * s, 104 * s, -22 * s),
                new CubicCurveTo(98 * s, 4 * s, 42 * s, 10 * s, 0, 4 * s));
        Path lower = new Path(new MoveTo(0, 6 * s),
                new CubicCurveTo(62 * s, 10 * s, 88 * s, 60 * s, 58 * s, 82 * s),
                new CubicCurveTo(30 * s, 98 * s, 6 * s, 52 * s, 0, 16 * s));
        for (Path lobe : new Path[]{upper, lower}) {
            lobe.setFill(orange);
            lobe.setStroke(edge);
            lobe.setStrokeWidth(3.5 * s);
        }
        Group wing = new Group(lower, upper);
        double[][] spots = {{92, -34, 5}, {80, -52, 4}, {100, -14, 3.5}, {56, 70, 4}, {44, 80, 3}};
        for (double[] spot : spots) {
            wing.getChildren().add(new Circle(spot[0] * s, spot[1] * s, spot[2] * s, Color.rgb(255, 255, 255, 0.9)));
        }
        return wing;
    }

    private static Group antenna(int side, double s) {
        double top = -FACE_SIZE / 2;
        CubicCurve line = new CubicCurve(side * 8 * s, top + 4 * s, side * 10 * s, top - 22 * s,
                side * 24 * s, top - 30 * s, side * 30 * s, top - 36 * s);
        line.setFill(null);
        line.setStroke(Color.rgb(45, 20, 10));
        line.setStrokeWidth(2.5 * s);
        line.setStrokeLineCap(StrokeLineCap.ROUND);
        Circle knob = new Circle(side * 30 * s, top - 36 * s, 4 * s, Color.rgb(45, 20, 10));
        return new Group(line, knob);
    }

    private void fly(double dt) {
        if (!(getParent() instanceof Pane sky) || sky.getWidth() <= 0 || sky.getHeight() <= 0) {
            return;
        }
        double margin = FACE_SIZE * 1.4;
        double width = Math.max(sky.getWidth() - 2 * margin, 1);
        double height = Math.max(sky.getHeight() - 2 * margin, 1);
        if (x < 0) {
            x = margin + random.nextDouble() * width;
            y = margin + random.nextDouble() * height;
            pickTarget(margin, width, height);
        }
        double dx = targetX - x;
        double dy = targetY - y;
        double distance = Math.hypot(dx, dy);
        if (distance < 30) {
            pickTarget(margin, width, height);
        } else {
            double steer = Math.min(1, dt * 1.6);
            vx += (dx / distance * SPEED - vx) * steer;
            vy += (dy / distance * SPEED - vy) * steer;
        }
        x += vx * dt;
        y += vy * dt;
        clock += dt;
        setLayoutX(x);
        setLayoutY(y + Math.sin(clock * 3.2) * 10);
        double tilt = Math.max(-20, Math.min(20, vx * 0.08));
        if (dizzy > 0) {
            dizzy -= dt;
            spin += 900 * dt * Math.max(0.2, dizzy);
            setRotate(tilt + spin);
        } else {
            spin = 0;
            setRotate(tilt);
        }
    }

    public boolean isTargetable() {
        return x >= 0 && dizzy <= 0;
    }

    public double getVelocityX() {
        return vx;
    }

    public double getVelocityY() {
        return vy;
    }

    public void hit(double pushX, double pushY) {
        dizzy = 1.4;
        vx += pushX;
        vy += pushY;
    }

    private void pickTarget(double margin, double width, double height) {
        targetX = margin + random.nextDouble() * width;
        targetY = margin + random.nextDouble() * height;
    }
}
