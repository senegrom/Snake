package snake.gui;

import java.awt.Component;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Orchestration helpers for the Robot-driven suites, which run off the EDT and
 * drive the real window: bounded EDT calls, bounded waits, focus and input.
 * Checks fail fast, because these suites cannot continue meaningfully after a
 * broken assumption about focus or geometry.
 */
final class RobotSupport {
	private static final long TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(5);
	private static final long STABLE_NANOS = TimeUnit.MILLISECONDS.toNanos(300);
	private static int checks;

	private RobotSupport() {
	}

	static int checks() {
		return checks;
	}

	static void check(final boolean condition, final String message) {
		if (!condition)
			throw new AssertionError(message);
		checks++;
	}

	/** Runs the action on the EDT and returns its result within the timeout. */
	static <T> T edt(final Callable<T> action) throws Exception {
		final FutureTask<T> task = new FutureTask<>(action);
		SwingUtilities.invokeLater(task);
		return task.get(TIMEOUT_NANOS, TimeUnit.NANOSECONDS);
	}

	/**
	 * Like {@link #edt}, but without a deadline, for building a window: a cold
	 * start (class loading, the look and feel, the first layout) can outlast
	 * the checks' timeout on a loaded machine.
	 */
	static <T> T edtWithoutDeadline(final Callable<T> action) throws Exception {
		final FutureTask<T> task = new FutureTask<>(action);
		SwingUtilities.invokeLater(task);
		return task.get();
	}

	/** Starts the game and returns its window once built, however long a cold start takes. */
	static JFrame launchGame() throws Exception {
		SnakeFrame.main(new String[0]);
		// main queues the window's construction; this waits until that has run
		edtWithoutDeadline(() -> null);
		await(() -> gameFrame() != null, "main window appears");
		return edt(RobotSupport::gameFrame);
	}

	/** Polls the condition on the EDT until it holds; a timeout reports every window's state. */
	static void await(final Callable<Boolean> condition, final String message) throws Exception {
		final long deadline = System.nanoTime() + TIMEOUT_NANOS;
		while (System.nanoTime() < deadline) {
			if (edt(condition)) {
				check(true, message);
				return;
			}
			Thread.sleep(20);
		}
		throw new AssertionError("Timed out: " + message + edt(RobotSupport::describeWindows));
	}

	private static String describeWindows() {
		final StringBuilder report = new StringBuilder();
		for (final Window window : Window.getWindows())
			report.append('\n').append(window.getClass().getSimpleName()).append(' ').append(window.getName())
					.append(" visible=").append(window.isVisible()).append(" focused=").append(window.isFocused())
					.append(" owner=").append(window.getOwner() == null ? "none" : window.getOwner().getName());
		return report.toString();
	}

	/** Native move and resize events settle asynchronously; waits until the bounds stop changing. */
	static void awaitStableBounds(final Window window) throws Exception {
		final long deadline = System.nanoTime() + TIMEOUT_NANOS;
		Rectangle previous = null;
		long stableSince = System.nanoTime();
		while (System.nanoTime() < deadline) {
			final Rectangle bounds = edt(window::getBounds);
			if (!bounds.equals(previous)) {
				previous = bounds;
				stableSince = System.nanoTime();
			} else if (System.nanoTime() - stableSince >= STABLE_NANOS) {
				return;
			}
			Thread.sleep(20);
		}
		throw new AssertionError("Window geometry did not settle");
	}

	static void activate(final Window window) throws Exception {
		edt(() -> {
			window.setVisible(true);
			window.toFront();
			window.requestFocus();
			return null;
		});
		await(window::isFocused, "window gains focus: " + describe(window));
	}

	private static String describe(final Window window) {
		return window instanceof Frame frame ? frame.getTitle() : window.getName();
	}

	static void focus(final JComponent component) throws Exception {
		edt(() -> {
			component.requestFocusInWindow();
			return null;
		});
		await(component::isFocusOwner, "component gains focus");
	}

	static void tap(final Robot robot, final int key) {
		robot.keyPress(key);
		robot.keyRelease(key);
		robot.delay(75);
	}

	/** Opens a focused selector's popup with Alt+Down, which every look and feel binds; Space only Metal and GTK. */
	static void openPopup(final Robot robot) {
		robot.keyPress(KeyEvent.VK_ALT);
		try {
			tap(robot, KeyEvent.VK_DOWN);
		} finally {
			robot.keyRelease(KeyEvent.VK_ALT);
		}
		robot.delay(75);
	}

	static void click(final Robot robot, final Component component) throws Exception {
		final Point point = edt(() -> {
			final Point location = component.getLocationOnScreen();
			location.translate(component.getWidth() / 2, component.getHeight() / 2);
			return location;
		});
		robot.mouseMove(point.x, point.y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.delay(50);
	}

	/** The visible game window, or null before it appears. */
	static JFrame gameFrame() {
		return Arrays.stream(Frame.getFrames()).filter(JFrame.class::isInstance).map(JFrame.class::cast)
				.filter(frame -> frame.isVisible() && "Snake".equals(frame.getTitle())).findFirst().orElse(null);
	}

	static void disposeAllWindows() throws Exception {
		edt(() -> {
			for (final Window window : Window.getWindows())
				window.dispose();
			return null;
		});
	}
}
