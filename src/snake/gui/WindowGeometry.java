package snake.gui;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;

/** Native window placement: one complete bounds request within the screen's work area. */
final class WindowGeometry {
	private WindowGeometry() {
	}

	/** The screen area available to windows, excluding taskbars and panels. */
	static Rectangle workArea(final Window window) {
		final Rectangle screen = window.getGraphicsConfiguration().getBounds();
		final Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(window.getGraphicsConfiguration());
		return new Rectangle(screen.x + insets.left, screen.y + insets.top,
				screen.width - insets.left - insets.right, screen.height - insets.top - insets.bottom);
	}

	/**
	 * Sizes the window to its preferred size clamped to the work area, centred
	 * on first placement and otherwise kept where it is as far as it fits.
	 * Native geometry settles asynchronously, so size and position go out as
	 * one request: a resize followed by a move computed from stale dimensions
	 * can be undone by a delayed configure event on HiDPI X11.
	 */
	static void placeWithinWorkArea(final Window window) {
		final boolean initial = !window.isDisplayable();
		if (initial)
			window.pack(); // creates the peer, so the insets and preferred size are real
		final Dimension preferred = window.getPreferredSize();
		final Rectangle area = workArea(window);
		final int width = Math.min(preferred.width, area.width);
		final int height = Math.min(preferred.height, area.height);
		final int x = initial ? area.x + (area.width - width) / 2
				: Math.max(area.x, Math.min(window.getX(), area.x + area.width - width));
		final int y = initial ? area.y + (area.height - height) / 2
				: Math.max(area.y, Math.min(window.getY(), area.y + area.height - height));
		window.setBounds(x, y, width, height);
	}
}
