package edu.liceo.ugoautomate.ui.common;

import java.awt.Component;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * Breakpoints and helpers that let screens adapt to phone-sized, tablet
 * (portrait/landscape), laptop, and desktop windows.
 */
public final class Responsive {

    /** Below this window width the sidebar collapses into a Menu button. */
    public static final int COMPACT_WIDTH = 780;

    private Responsive() {
    }

    /** Runs {@code action} now and every time {@code component} is resized. */
    public static void onResize(Component component, Runnable action) {
        component.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                action.run();
            }
        });
        action.run();
    }

    /** @return the screen area not covered by the taskbar/dock for the given component's screen */
    public static Rectangle usableScreen(Component component) {
        GraphicsConfiguration gc = component == null ? null : component.getGraphicsConfiguration();
        if (gc == null) {
            gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        }
        Rectangle bounds = gc.getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
        return new Rectangle(bounds.x + insets.left, bounds.y + insets.top,
                bounds.width - insets.left - insets.right, bounds.height - insets.top - insets.bottom);
    }

    /**
     * Packs a window, then shrinks it if needed so it never extends past the
     * screen (small laptops, tablets, portrait monitors) and centers it.
     */
    public static void packToScreen(Window window, Component relativeTo) {
        window.pack();
        Rectangle screen = usableScreen(relativeTo != null ? relativeTo : window);
        window.setSize(Math.min(window.getWidth(), screen.width - 24), Math.min(window.getHeight(), screen.height - 24));
        window.setLocationRelativeTo(relativeTo);
    }

    /** Sizes a main window to a comfortable fraction of the screen, capped at {@code maxWidth x maxHeight}. */
    public static void sizeToScreen(Window window, int maxWidth, int maxHeight) {
        Rectangle screen = usableScreen(window);
        window.setSize(Math.min(maxWidth, (int) (screen.width * 0.94)), Math.min(maxHeight, (int) (screen.height * 0.94)));
        window.setLocationRelativeTo(null);
    }
}
