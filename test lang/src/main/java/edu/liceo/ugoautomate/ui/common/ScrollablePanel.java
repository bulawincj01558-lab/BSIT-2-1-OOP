package edu.liceo.ugoautomate.ui.common;

import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Rectangle;

/**
 * Panel placed inside a scroll pane that follows the viewport width (so its
 * content re-flows instead of scrolling sideways) and scrolls vertically only
 * when the window is too short. It fills the viewport when there is room, so
 * tables still stretch on large screens.
 */
public class ScrollablePanel extends JPanel implements Scrollable {

    public ScrollablePanel(LayoutManager layout) {
        super(layout);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 18;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return Math.max(visibleRect.height - 36, 18);
    }

    /**
     * Always follows the viewport width: pages re-flow (cards stack, button rows
     * wrap) instead of scrolling sideways. Wide tables scroll inside their own box.
     */
    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    /** Stretches to the viewport height when the content is shorter than the window. */
    @Override
    public boolean getScrollableTracksViewportHeight() {
        return getParent() instanceof JViewport viewport && viewport.getHeight() > getPreferredSize().height;
    }
}
