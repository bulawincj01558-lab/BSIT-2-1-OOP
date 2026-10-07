package edu.liceo.ugoautomate.ui.common;

import javafx.animation.AnimationTimer;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TRexFriend extends Group {
    private static final String FACE = "/edu/liceo/ugoautomate/ui/images/trex-face.png";
    private static final double FACE_SIZE = 100;
    private static final double WALK_SPEED = 85;
    private static final double ROAR_SECONDS = 1.8;

    private static final Color SKIN = Color.rgb(76, 160, 70);
    private static final Color SKIN_DARK = Color.rgb(38, 96, 40);
    private static final Color BELLY = Color.rgb(176, 214, 120);

    private final Random random = new Random();

    private final Scale facing = new Scale(1, 1, 0, 0);
    private final Group upper = new Group();
    private final Rotate frontLeg = new Rotate(0, 0, 0);
    private final Rotate backLeg = new Rotate(0, 0, 0);
    private final Rotate tailSway = new Rotate(0, 0, 0);
    private final Rotate headTilt = new Rotate(0, 0, 0);
    private final FireBreath fire;

    private double x = -1;
    private int direction = 1;
    private double stride;
    private double clock;
    private double untilRoar = 3;
    private double roarLeft;
    private long lastFrame;

    private final AnimationTimer life = new AnimationTimer() {
        @Override
        public void handle(long now) {
            double dt = lastFrame == 0 ? 1 / 60.0 : Math.max(0, Math.min(0.05, (now - lastFrame) / 1e9));
            lastFrame = now;
            live(dt);
        }
    };

    public static void addTo(Pane sky) {
        URL face = TRexFriend.class.getResource(FACE);
        if (face != null) {
            sky.getChildren().add(new TRexFriend(new Image(face.toExternalForm(), FACE_SIZE * 2, FACE_SIZE * 2, true, true)));
        }
    }

    private TRexFriend(Image face) {
        setMouseTransparent(true);
        double k = FACE_SIZE / 100;
        LinearGradient skin = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, SKIN.brighter()), new Stop(1, SKIN_DARK));

        Path tail = new Path(new MoveTo(-50 * k, -128 * k),
                new CubicCurveTo(-110 * k, -130 * k, -150 * k, -104 * k, -190 * k, -70 * k),
                new LineTo(-184 * k, -60 * k),
                new CubicCurveTo(-140 * k, -80 * k, -100 * k, -86 * k, -45 * k, -84 * k),
                new ClosePath());
        styleSkin(tail, skin, k);
        tailSway.setPivotX(-50 * k);
        tailSway.setPivotY(-106 * k);
        tail.getTransforms().add(tailSway);

        Ellipse body = new Ellipse(0, -105 * k, 78 * k, 50 * k);
        styleSkin(body, skin, k);
        Ellipse belly = new Ellipse(18 * k, -90 * k, 46 * k, 28 * k);
        belly.setFill(BELLY);
        Polygon neck = new Polygon(38 * k, -142 * k, 72 * k, -178 * k, 112 * k, -162 * k, 70 * k, -108 * k);
        styleSkin(neck, skin, k);

        Group spikes = new Group();
        for (double sx = -62; sx <= 40; sx += 17) {
            double top = -105 - 50 * Math.sqrt(Math.max(0, 1 - (sx / 78) * (sx / 78)));
            Polygon spike = new Polygon((sx - 8) * k, (top + 4) * k, sx * k, (top - 14) * k, (sx + 8) * k, (top + 4) * k);
            spike.setFill(SKIN_DARK);
            spikes.getChildren().add(spike);
        }

        Group arm = new Group(limb(58, -98, 76, -80, 9, k), limb(76, -80, 84, -84, 4, k), limb(76, -80, 82, -74, 4, k));

        ImageView head = new ImageView(face);
        head.setFitWidth(FACE_SIZE);
        head.setFitHeight(FACE_SIZE);
        double headX = 100 * k;
        double headY = -208 * k;
        head.setLayoutX(headX - FACE_SIZE / 2);
        head.setLayoutY(headY - FACE_SIZE / 2);
        Circle frame = new Circle(headX, headY, FACE_SIZE / 2, Color.TRANSPARENT);
        frame.setStroke(SKIN_DARK);
        frame.setStrokeWidth(5 * k);
        Group crest = new Group();
        for (int i = -2; i <= 2; i++) {
            double cx = headX + i * 16 * k;
            double cy = headY - FACE_SIZE / 2 + Math.abs(i) * 4 * k;
            Polygon point = new Polygon(cx - 8 * k, cy + 2 * k, cx, cy - 16 * k, cx + 8 * k, cy + 2 * k);
            point.setFill(SKIN_DARK);
            crest.getChildren().add(point);
        }

        fire = new FireBreath(k);
        fire.setLayoutX(headX + 40 * k);
        fire.setLayoutY(headY + 22 * k - fire.getHeight() / 2);
        Group headGroup = new Group(crest, head, frame, fire);
        headTilt.setPivotX(headX);
        headTilt.setPivotY(headY + FACE_SIZE / 2);
        headGroup.getTransforms().add(headTilt);

        upper.getChildren().addAll(tail, body, belly, spikes, neck, arm, headGroup);

        frontLeg.setPivotX(18 * k);
        frontLeg.setPivotY(-82 * k);
        backLeg.setPivotX(-22 * k);
        backLeg.setPivotY(-82 * k);
        Group legBehind = leg(-22, k, skin);
        legBehind.getTransforms().add(backLeg);
        Group legFront = leg(18, k, skin);
        legFront.getTransforms().add(frontLeg);

        Group rig = new Group(legBehind, upper, legFront);
        rig.getTransforms().add(facing);
        getChildren().add(rig);
        setEffect(new DropShadow(16 * k, 4 * k, 8 * k, Color.rgb(0, 0, 0, 0.35)));

        sceneProperty().addListener((obs, old, scene) -> {
            if (scene == null) {
                life.stop();
            } else {
                lastFrame = 0;
                life.start();
            }
        });
    }

    private static void styleSkin(Shape shape, LinearGradient skin, double k) {
        shape.setFill(skin);
        shape.setStroke(SKIN_DARK);
        shape.setStrokeWidth(3 * k);
    }

    private static Line limb(double x1, double y1, double x2, double y2, double width, double k) {
        Line line = new Line(x1 * k, y1 * k, x2 * k, y2 * k);
        line.setStroke(SKIN_DARK);
        line.setStrokeWidth(width * k);
        line.setStrokeLineCap(StrokeLineCap.ROUND);
        return line;
    }

    private static Group leg(double hipX, double k, LinearGradient skin) {
        Ellipse thigh = new Ellipse(hipX * k, -60 * k, 20 * k, 30 * k);
        styleSkin(thigh, skin, k);
        Rectangle shin = new Rectangle((hipX - 9) * k, -44 * k, 18 * k, 40 * k);
        shin.setArcWidth(12 * k);
        shin.setArcHeight(12 * k);
        styleSkin(shin, skin, k);
        Ellipse foot = new Ellipse((hipX + 10) * k, -5 * k, 22 * k, 7 * k);
        styleSkin(foot, skin, k);
        return new Group(shin, thigh, foot);
    }

    private void live(double dt) {
        if (!(getParent() instanceof Pane sky) || sky.getWidth() <= 0 || sky.getHeight() <= 0) {
            return;
        }
        double margin = 200 * FACE_SIZE / 100;
        double left = margin;
        double right = Math.max(margin + 1, sky.getWidth() - margin);
        if (x < 0) {
            x = left + random.nextDouble() * (right - left);
        }
        clock += dt;
        tailSway.setAngle(Math.sin(clock * 2.4) * 7);

        if (roarLeft > 0) {
            roarLeft -= dt;
            headTilt.setAngle(-10);
            if (roarLeft <= 0) {
                fire.setBurning(false);
                headTilt.setAngle(0);
                untilRoar = 4 + random.nextDouble() * 5;
            }
        } else {
            x += direction * WALK_SPEED * dt;
            if (x > right) {
                x = right;
                direction = -1;
            } else if (x < left) {
                x = left;
                direction = 1;
            }
            stride += dt * 7;
            frontLeg.setAngle(Math.sin(stride) * 24);
            backLeg.setAngle(-Math.sin(stride) * 24);
            upper.setTranslateY(-Math.abs(Math.sin(stride)) * 5);
            untilRoar -= dt;
            if (untilRoar <= 0) {
                roarLeft = ROAR_SECONDS;
                fire.setBurning(true);
            }
        }
        facing.setX(direction);
        setLayoutX(x);
        setLayoutY(sky.getHeight() - 6);
    }

    private static final class FireBreath extends Canvas {
        private static final Color[] HEAT = {
                Color.rgb(255, 245, 190), Color.rgb(255, 205, 70), Color.rgb(255, 135, 20),
                Color.rgb(225, 65, 10), Color.rgb(120, 30, 20)};

        private final double k;
        private final Random random = new Random();
        private final List<double[]> flames = new ArrayList<>();
        private boolean burning;
        private long lastFrame;

        private final AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double dt = lastFrame == 0 ? 1 / 60.0 : Math.max(0, Math.min(0.05, (now - lastFrame) / 1e9));
                lastFrame = now;
                step(dt);
                draw();
            }
        };

        FireBreath(double k) {
            super(420 * k, 200 * k);
            this.k = k;
            setEffect(new GaussianBlur(3 * k));
            sceneProperty().addListener((obs, old, scene) -> {
                if (scene == null) {
                    timer.stop();
                } else {
                    lastFrame = 0;
                    timer.start();
                }
            });
        }

        void setBurning(boolean on) {
            burning = on;
        }

        private void step(double dt) {
            if (burning) {
                int spawn = (int) Math.round(dt * 330);
                for (int i = 0; i < spawn; i++) {
                    flames.add(new double[]{random.nextDouble() * 6 * k, getHeight() / 2 + (random.nextDouble() - 0.5) * 12 * k,
                            (300 + random.nextDouble() * 200) * k, ((random.nextDouble() - 0.5) * 110 - 20) * k,
                            0, 0.5 + random.nextDouble() * 0.45});
                }
            }
            for (double[] f : flames) {
                f[0] += f[2] * dt;
                f[1] += f[3] * dt;
                f[3] -= 60 * k * dt;
                f[4] += dt;
            }
            flames.removeIf(f -> f[4] >= f[5] || f[0] > getWidth());
        }

        private void draw() {
            GraphicsContext g = getGraphicsContext2D();
            g.clearRect(0, 0, getWidth(), getHeight());
            g.setGlobalBlendMode(BlendMode.ADD);
            for (double[] f : flames) {
                double t = Math.max(0, Math.min(0.999, f[4] / f[5]));
                double r = (8 + 34 * t) * k;
                double scaled = t * (HEAT.length - 1);
                int i = (int) scaled;
                Color c = HEAT[i].interpolate(HEAT[i + 1], scaled - i);
                g.setFill(c.deriveColor(0, 1, 1, Math.pow(1 - t, 1.2) * 0.75));
                g.fillOval(f[0] - r, f[1] - r, r * 2, r * 2);
            }
            g.setGlobalBlendMode(BlendMode.SRC_OVER);
        }
    }
}
