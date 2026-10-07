package edu.liceo.ugoautomate.ui.common;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.util.Duration;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TankFriend extends Group {
    private static final String FACE = "/edu/liceo/ugoautomate/ui/images/tank-face.png";
    private static final double SHELL_SIZE = 90;
    private static final double SHELL_SPEED = 520;
    private static final double DRIVE_SPEED = 45;
    private static final double TURRET_Y = -78;
    private static final double BARREL_LENGTH = 125;
    private static final double TURN_SPEED = 140;

    private final Image face;
    private final Random random = new Random();
    private final Rotate aim = new Rotate(-45, 0, TURRET_Y);
    private final Translate recoil = new Translate();
    private final List<Rotate> wheelSpin = new ArrayList<>();
    private final Circle muzzleFlash = new Circle(BARREL_LENGTH + 10, TURRET_Y, 0);
    private final List<Shell> shells = new ArrayList<>();

    private double x = -1;
    private int direction = 1;
    private double reload = 3;
    private double smokeTimer;
    private long lastFrame;

    private static final class Shell {
        final Group node;
        double x;
        double y;
        final double vx;
        final double vy;

        Shell(Group node, double x, double y, double vx, double vy) {
            this.node = node;
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
        }
    }

    private final AnimationTimer life = new AnimationTimer() {
        @Override
        public void handle(long now) {
            double dt = lastFrame == 0 ? 1 / 60.0 : Math.max(0, Math.min(0.05, (now - lastFrame) / 1e9));
            lastFrame = now;
            live(dt);
        }
    };

    public static void addTo(Pane sky) {
        URL url = TankFriend.class.getResource(FACE);
        if (url != null) {
            sky.getChildren().add(new TankFriend(new Image(url.toExternalForm(), SHELL_SIZE * 2, SHELL_SIZE * 2, true, true)));
        }
    }

    private TankFriend(Image face) {
        this.face = face;
        setMouseTransparent(true);
        Color olive = Color.rgb(96, 112, 52);
        Color oliveDark = Color.rgb(52, 62, 30);
        LinearGradient armor = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(128, 146, 72)), new Stop(1, oliveDark));

        Rectangle barrel = new Rectangle(0, TURRET_Y - 8, BARREL_LENGTH, 16);
        barrel.setFill(armor);
        barrel.setStroke(Color.rgb(30, 34, 18));
        barrel.setStrokeWidth(2.5);
        Rectangle muzzle = new Rectangle(BARREL_LENGTH - 16, TURRET_Y - 11, 18, 22);
        muzzle.setArcWidth(6);
        muzzle.setArcHeight(6);
        muzzle.setFill(oliveDark);
        muzzleFlash.setFill(new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(255, 250, 210)), new Stop(0.45, Color.rgb(255, 170, 40, 0.9)),
                new Stop(1, Color.TRANSPARENT)));
        muzzleFlash.setBlendMode(BlendMode.ADD);
        Group cannon = new Group(barrel, muzzle, muzzleFlash);
        cannon.getTransforms().addAll(aim, recoil);

        Rectangle tread = new Rectangle(-98, -34, 196, 34);
        tread.setArcWidth(34);
        tread.setArcHeight(34);
        tread.setFill(Color.rgb(48, 50, 42));
        tread.setStroke(Color.rgb(20, 20, 16));
        tread.setStrokeWidth(3);
        Group wheels = new Group();
        for (int i = 0; i < 6; i++) {
            double cx = -72 + i * 28.8;
            Circle wheel = new Circle(cx, -17, 11, Color.rgb(90, 92, 80));
            wheel.setStroke(Color.rgb(25, 25, 20));
            wheel.setStrokeWidth(2);
            Line spokeA = new Line(cx - 8, -17, cx + 8, -17);
            Line spokeB = new Line(cx, -25, cx, -9);
            for (Line spoke : new Line[]{spokeA, spokeB}) {
                spoke.setStroke(Color.rgb(25, 25, 20));
                spoke.setStrokeWidth(2);
            }
            Group spinning = new Group(spokeA, spokeB);
            Rotate turn = new Rotate(0, cx, -17);
            spinning.getTransforms().add(turn);
            wheelSpin.add(turn);
            wheels.getChildren().addAll(wheel, spinning);
        }

        Polygon hull = new Polygon(-102, -34, 102, -34, 86, -64, -84, -64);
        hull.setFill(armor);
        hull.setStroke(Color.rgb(30, 34, 18));
        hull.setStrokeWidth(3);
        Polygon star = star(-48, -49, 11);
        star.setFill(Color.rgb(245, 240, 220));

        Ellipse turret = new Ellipse(0, TURRET_Y + 8, 46, 30);
        turret.setFill(armor);
        turret.setStroke(Color.rgb(30, 34, 18));
        turret.setStrokeWidth(3);
        Circle hatch = new Circle(-10, TURRET_Y - 12, 11, olive);
        hatch.setStroke(Color.rgb(30, 34, 18));
        hatch.setStrokeWidth(2);

        getChildren().addAll(cannon, tread, wheels, hull, star, turret, hatch);
        setEffect(new DropShadow(14, 3, 8, Color.rgb(0, 0, 0, 0.35)));

        sceneProperty().addListener((obs, old, scene) -> {
            if (scene == null) {
                life.stop();
            } else {
                lastFrame = 0;
                life.start();
            }
        });
    }

    private static Polygon star(double cx, double cy, double r) {
        double[] points = new double[20];
        for (int i = 0; i < 10; i++) {
            double radius = i % 2 == 0 ? r : r * 0.45;
            double angle = Math.toRadians(-90 + i * 36);
            points[i * 2] = cx + Math.cos(angle) * radius;
            points[i * 2 + 1] = cy + Math.sin(angle) * radius;
        }
        return new Polygon(points);
    }

    private void live(double dt) {
        if (!(getParent() instanceof Pane sky) || sky.getWidth() <= 0 || sky.getHeight() <= 0) {
            return;
        }
        double left = 130;
        double right = Math.max(left + 1, sky.getWidth() - 130);
        if (x < 0) {
            x = left + random.nextDouble() * (right - left);
        }

        x += direction * DRIVE_SPEED * dt;
        if (x > right) {
            x = right;
            direction = -1;
        } else if (x < left) {
            x = left;
            direction = 1;
        }
        for (Rotate wheel : wheelSpin) {
            wheel.setAngle(wheel.getAngle() + direction * DRIVE_SPEED * dt / 11 * 180 / Math.PI);
        }
        setLayoutX(x);
        setLayoutY(sky.getHeight() - 4);

        recoil.setX(recoil.getX() * Math.pow(0.02, dt));
        muzzleFlash.setRadius(Math.max(0, muzzleFlash.getRadius() - 160 * dt));

        ButterflyFriend target = findTarget(sky);
        if (target != null) {
            aimAndFire(sky, target, dt);
        }
        moveShells(sky, target, dt);
    }

    private static ButterflyFriend findTarget(Pane sky) {
        for (Node node : sky.getChildren()) {
            if (node instanceof ButterflyFriend butterfly) {
                return butterfly;
            }
        }
        return null;
    }

    private void aimAndFire(Pane sky, ButterflyFriend target, double dt) {
        double pivotX = x;
        double pivotY = getLayoutY() + TURRET_Y;
        double tx = target.getLayoutX();
        double ty = target.getLayoutY();

        double flightTime = Math.hypot(tx - pivotX, ty - pivotY) / SHELL_SPEED;
        tx += target.getVelocityX() * flightTime;
        ty += target.getVelocityY() * flightTime;

        double wanted = Math.toDegrees(Math.atan2(ty - pivotY, tx - pivotX));
        wanted = Math.max(-175, Math.min(-5, wanted));
        double difference = wanted - aim.getAngle();
        aim.setAngle(aim.getAngle() + Math.max(-TURN_SPEED * dt, Math.min(TURN_SPEED * dt, difference)));

        reload -= dt;
        if (reload <= 0 && Math.abs(difference) < 4 && target.isTargetable()) {
            fire(sky, pivotX, pivotY);
            reload = 3.5 + random.nextDouble() * 2.5;
        }
    }

    private void fire(Pane sky, double pivotX, double pivotY) {
        double angle = Math.toRadians(aim.getAngle());
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        ImageView shellFace = new ImageView(face);
        shellFace.setFitWidth(SHELL_SIZE);
        shellFace.setFitHeight(SHELL_SIZE);
        shellFace.setLayoutX(-SHELL_SIZE / 2);
        shellFace.setLayoutY(-SHELL_SIZE / 2);
        Circle rim = new Circle(0, 0, SHELL_SIZE / 2, Color.TRANSPARENT);
        rim.setStroke(Color.rgb(200, 40, 30));
        rim.setStrokeWidth(4);
        Group node = new Group(shellFace, rim);
        node.setEffect(new DropShadow(12, Color.rgb(255, 140, 0, 0.8)));
        node.setMouseTransparent(true);
        Shell shell = new Shell(node, pivotX + cos * (BARREL_LENGTH + SHELL_SIZE / 2), pivotY + sin * (BARREL_LENGTH + SHELL_SIZE / 2),
                cos * SHELL_SPEED, sin * SHELL_SPEED);
        node.setLayoutX(shell.x);
        node.setLayoutY(shell.y);
        sky.getChildren().add(node);
        shells.add(shell);

        recoil.setX(-18);
        muzzleFlash.setRadius(34);
    }

    private void moveShells(Pane sky, ButterflyFriend target, double dt) {
        smokeTimer -= dt;
        boolean puff = smokeTimer <= 0;
        if (puff) {
            smokeTimer = 0.035;
        }
        List<Shell> done = new ArrayList<>();
        for (Shell shell : shells) {
            shell.x += shell.vx * dt;
            shell.y += shell.vy * dt;
            shell.node.setLayoutX(shell.x);
            shell.node.setLayoutY(shell.y);
            shell.node.setRotate(shell.node.getRotate() + 540 * dt);
            if (puff) {
                smoke(sky, shell.x, shell.y);
            }
            boolean hit = target != null && target.isTargetable()
                    && Math.hypot(target.getLayoutX() - shell.x, target.getLayoutY() - shell.y) < SHELL_SIZE * 0.8;
            boolean gone = shell.x < -200 || shell.x > sky.getWidth() + 200 || shell.y < -200 || shell.y > sky.getHeight() + 200;
            if (hit) {
                target.hit(shell.vx * 0.35, shell.vy * 0.35);
                boom(sky, shell.x, shell.y);
            }
            if (hit || gone) {
                sky.getChildren().remove(shell.node);
                done.add(shell);
            }
        }
        shells.removeAll(done);
    }

    private void smoke(Pane sky, double sx, double sy) {
        Circle cloud = new Circle(sx, sy, 10, Color.rgb(200, 200, 200, 0.55));
        cloud.setMouseTransparent(true);
        sky.getChildren().add(0, cloud);
        ScaleTransition grow = new ScaleTransition(Duration.millis(700), cloud);
        grow.setToX(3);
        grow.setToY(3);
        FadeTransition fade = new FadeTransition(Duration.millis(700), cloud);
        fade.setToValue(0);
        ParallelTransition puff = new ParallelTransition(grow, fade);
        puff.setOnFinished(e -> sky.getChildren().remove(cloud));
        puff.play();
    }

    private void boom(Pane sky, double bx, double by) {
        Circle fireball = new Circle(bx, by, 10, new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(255, 250, 210)), new Stop(0.35, Color.rgb(255, 190, 50)),
                new Stop(0.7, Color.rgb(240, 90, 20, 0.8)), new Stop(1, Color.TRANSPARENT)));
        fireball.setBlendMode(BlendMode.ADD);
        Group sparks = new Group();
        for (int i = 0; i < 12; i++) {
            double angle = Math.toRadians(i * 30 + random.nextDouble() * 15);
            Line spark = new Line(bx + Math.cos(angle) * 20, by + Math.sin(angle) * 20,
                    bx + Math.cos(angle) * 45, by + Math.sin(angle) * 45);
            spark.setStroke(Color.rgb(255, 210, 60));
            spark.setStrokeWidth(4);
            sparks.getChildren().add(spark);
        }
        Text word = new Text("BOOM!");
        word.setFont(Font.font("System", FontWeight.EXTRA_BOLD, 40));
        word.setFill(Color.rgb(255, 214, 0));
        word.setStroke(Color.rgb(200, 30, 20));
        word.setStrokeWidth(2.5);
        word.setX(bx - 62);
        word.setY(by - 50);
        Group explosion = new Group(fireball, sparks, word);
        explosion.setMouseTransparent(true);
        sky.getChildren().add(explosion);

        Timeline blast = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(fireball.radiusProperty(), 10)),
                new KeyFrame(Duration.millis(350), new KeyValue(fireball.radiusProperty(), 95, Interpolator.EASE_OUT)));
        ScaleTransition burst = new ScaleTransition(Duration.millis(350), sparks);
        burst.setFromX(0.5);
        burst.setFromY(0.5);
        burst.setToX(2.4);
        burst.setToY(2.4);
        ScaleTransition pop = new ScaleTransition(Duration.millis(300), word);
        pop.setFromX(0.2);
        pop.setFromY(0.2);
        pop.setToX(1.3);
        pop.setToY(1.3);
        FadeTransition fade = new FadeTransition(Duration.millis(600), explosion);
        fade.setToValue(0);
        SequentialTransition all = new SequentialTransition(new ParallelTransition(blast, burst, pop),
                new PauseTransition(Duration.millis(250)), fade);
        all.setOnFinished(e -> sky.getChildren().remove(explosion));
        all.play();
    }
}
