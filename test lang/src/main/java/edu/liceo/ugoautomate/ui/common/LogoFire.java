package edu.liceo.ugoautomate.ui.common;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LogoFire extends Canvas {
    private static final Color[] HEAT = {
            Color.rgb(255, 236, 140), Color.rgb(255, 196, 60), Color.rgb(255, 130, 20),
            Color.rgb(230, 70, 10), Color.rgb(150, 25, 10)};
    private static final int MAX_PARTICLES = 700;

    private final Random random = new Random();
    private final List<Particle> particles = new ArrayList<>();
    private final double logoSize;
    private long lastFrame;

    private double turn = 1;

    private final AnimationTimer timer = new AnimationTimer() {
        @Override
        public void handle(long now) {
            double seconds = lastFrame == 0 ? 1 / 60.0 : Math.max(0, Math.min(0.05, (now - lastFrame) / 1e9));
            lastFrame = now;
            step(seconds);
            draw();
        }
    };

    private static final class Particle {
        double x;
        double y;
        double vx;
        double vy;
        double age;
        double life;
        double radius;
        double swayPhase;
    }

    public LogoFire(double logoSize, double width, double height) {
        super(width, height);
        this.logoSize = logoSize;
        setMouseTransparent(true);

        setEffect(new GaussianBlur(logoSize / 28));

        sceneProperty().addListener((obs, old, scene) -> {
            if (scene == null) {
                timer.stop();
            } else {
                lastFrame = 0;
                timer.start();
            }
        });
    }

    public void setTurn(double angleDegrees) {
        double cos = Math.cos(Math.toRadians(angleDegrees));

        turn = Math.copySign(Math.max(Math.abs(cos), 0.14), cos);
    }

    private void step(double dt) {
        int toSpawn = (int) Math.round(dt * 520);
        for (int i = 0; i < toSpawn && particles.size() < MAX_PARTICLES; i++) {
            particles.add(spawn());
        }
        double scale = logoSize / 220;
        for (Particle p : particles) {
            p.age += dt;
            p.swayPhase += dt * 6;
            p.vy -= 90 * scale * dt;

            p.vx -= p.x * 0.9 * dt;
            p.x += (p.vx + Math.sin(p.swayPhase) * 14 * scale) * dt;
            p.y += p.vy * dt;
        }
        particles.removeIf(p -> p.age >= p.life);
    }

    private Particle spawn() {
        double scale = logoSize / 220;
        double bottom = getHeight() - logoSize * 0.3;
        Particle p = new Particle();
        if (random.nextDouble() < 0.65) {
            double side = random.nextBoolean() ? 1 : -1;
            p.x = side * logoSize * (0.34 + random.nextDouble() * 0.14);
            p.y = bottom - random.nextDouble() * logoSize * 0.75;
        } else {
            p.x = (random.nextDouble() - 0.5) * logoSize * 0.7;
            p.y = bottom + random.nextDouble() * 10 * scale;
        }
        p.vx = (random.nextDouble() - 0.5) * 16 * scale;
        p.vy = -(70 + random.nextDouble() * 90) * scale;
        p.life = 0.55 + random.nextDouble() * 0.75;
        p.radius = (6 + random.nextDouble() * 9) * scale;
        p.swayPhase = random.nextDouble() * Math.PI * 2;
        return p;
    }

    private void draw() {
        GraphicsContext g = getGraphicsContext2D();
        g.clearRect(0, 0, getWidth(), getHeight());
        g.setGlobalBlendMode(BlendMode.ADD);
        double centerX = getWidth() / 2;
        for (Particle p : particles) {
            double x = centerX + p.x * turn;
            double t = p.age / p.life;
            double alpha = Math.pow(1 - t, 1.4) * 0.42;
            double r = p.radius * (1 - 0.55 * t);
            g.setFill(heat(t).deriveColor(0, 1, 1, alpha));
            g.fillOval(x - r, p.y - r, r * 2, r * 2);
        }
        g.setGlobalBlendMode(BlendMode.SRC_OVER);
    }

    private static Color heat(double t) {
        double scaled = Math.max(0, Math.min(0.999, t)) * (HEAT.length - 1);
        int i = (int) scaled;
        return HEAT[i].interpolate(HEAT[i + 1], scaled - i);
    }
}
