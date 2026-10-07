package edu.liceo.ugoautomate.ui.common;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SnapshotParameters;
import javafx.scene.SubScene;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Light;
import javafx.scene.effect.Lighting;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SpinningLogo extends StackPane {
    private static final Duration ONE_TURN = Duration.seconds(7);
    private static final int EDGE_LAYERS = 12;

    private final Rotate spin = new Rotate(0, Rotate.Y_AXIS);
    private final Timeline timeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(spin.angleProperty(), 0, Interpolator.LINEAR)),
            new KeyFrame(ONE_TURN, new KeyValue(spin.angleProperty(), 360, Interpolator.LINEAR)));
    private final Group coin = new Group();
    private final List<Node> frontToBack = new ArrayList<>();
    private boolean showingFront = true;
    private double size;
    private boolean fire;
    private SubScene view;
    private LogoFire flames;

    public SpinningLogo() {
        this(200);
    }

    public SpinningLogo(double size) {
        getStyleClass().add("spinning-logo");
        timeline.setCycleCount(Animation.INDEFINITE);
        spin.angleProperty().addListener((obs, old, angle) -> {
            updateDrawingOrder(angle.doubleValue());
            if (flames != null) {
                flames.setTurn(angle.doubleValue());
            }
        });

        sceneProperty().addListener((obs, old, scene) -> {
            if (scene == null) {
                timeline.stop();
            } else {
                timeline.play();
            }
        });
        setSize(size);
    }

    public static List<Image> frames(double frameSize, int count) {
        SpinningLogo logo = new SpinningLogo(frameSize * 0.72);

        new Scene(new Group(logo)).setFill(Color.TRANSPARENT);
        logo.timeline.stop();
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        List<Image> frames = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            logo.spin.setAngle(360.0 * i / count);
            frames.add(logo.snapshot(params, null));
        }
        return frames;
    }

    public double getSize() {
        return size;
    }

    public boolean isFire() {
        return fire;
    }

    public void setFire(boolean on) {
        this.fire = on;
        build();
    }

    public final void setSize(double newSize) {
        this.size = newSize;
        build();
    }

    private void build() {
        Image seal = new Image(Views.class.getResource("/edu/liceo/ugoautomate/ui/images/liceo-logo.png").toExternalForm(),
                size, size, true, true);
        double thickness = size * 0.06;
        Lighting bevel = new Lighting(new Light.Distant(-135, 55, Color.WHITE));
        bevel.setSurfaceScale(size / 50);
        bevel.setDiffuseConstant(1.25);
        bevel.setSpecularConstant(0.6);
        bevel.setSpecularExponent(18);

        frontToBack.clear();
        ImageView front = face(seal, -thickness / 2, false);
        front.setEffect(bevel);
        frontToBack.add(front);

        for (int i = 1; i <= EDGE_LAYERS; i++) {
            ImageView layer = face(seal, -thickness / 2 + thickness * i / (EDGE_LAYERS + 1), false);
            layer.setEffect(new ColorAdjust(0, 0.25, -0.22, 0));
            frontToBack.add(layer);
        }

        ImageView back = face(seal, thickness / 2, true);
        back.setEffect(bevel);
        frontToBack.add(back);

        coin.getChildren().clear();
        coin.getTransforms().setAll(spin);
        spin.setPivotX(size / 2);
        spin.setPivotY(size / 2);
        showingFront = true;
        drawInOrder();

        double width = size * 1.35;
        double height = size * 1.2;
        coin.setTranslateX((width - size) / 2);
        coin.setTranslateY((height - size) / 2);
        view = new SubScene(new Group(coin), width, height, false, SceneAntialiasing.BALANCED);
        view.setFill(Color.TRANSPARENT);
        view.setCamera(new PerspectiveCamera(false));
        DropShadow shadow = new DropShadow(size / 9, size / 40, size / 18, Color.rgb(0, 0, 0, 0.55));
        if (fire) {
            DropShadow glow = new DropShadow(BlurType.GAUSSIAN, Color.rgb(255, 120, 0, 0.85), size / 5, 0.3, 0, 0);
            glow.setInput(shadow);
            view.setEffect(glow);

            flames = new LogoFire(size, size * 1.6, size * 1.9);
            flames.setManaged(false);
            flames.setTurn(spin.getAngle());
            getChildren().setAll(flames, view);
        } else {
            view.setEffect(shadow);
            flames = null;
            getChildren().setAll(view);
        }
        setMinSize(width, height);
        setPrefSize(width, height);
        setMaxSize(width, height);
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        if (flames != null) {
            flames.relocate((getWidth() - flames.getWidth()) / 2, getHeight() - flames.getHeight() + size * 0.12);
        }
    }

    private ImageView face(Image image, double z, boolean flipped) {
        ImageView view = new ImageView(image);
        view.setSmooth(true);
        view.setTranslateZ(z);
        if (flipped) {
            view.getTransforms().add(new Rotate(180, size / 2, size / 2, 0, Rotate.Y_AXIS));
        }
        return view;
    }

    private void updateDrawingOrder(double angle) {
        boolean front = Math.cos(Math.toRadians(angle)) > 0;
        if (front != showingFront) {
            showingFront = front;
            drawInOrder();
        }
    }

    private void drawInOrder() {
        List<Node> order = new ArrayList<>(frontToBack);
        if (showingFront) {
            Collections.reverse(order);
        }
        coin.getChildren().setAll(order);
    }
}
