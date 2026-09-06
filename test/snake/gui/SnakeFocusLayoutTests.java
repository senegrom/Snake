package snake.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import snake.Position;
import snake.topology.Topology;

/** Regressions for returning with a key held and controls on small, scaled displays. */
public final class SnakeFocusLayoutTests {
	private static int checks;
	private final Robot robot;
	private JFrame frame;
	private JFrame other;
	private boolean dropReleases;
	private int droppedReleases;
	private final AtomicInteger spacePresses = new AtomicInteger();
	// Installed before the game's dispatcher to simulate an OS release which
	// never reaches this JVM. Ordinary same-JVM focus tests would miss that case.
	private final KeyEventDispatcher inputProbe = event -> {
		if (event.getID() == KeyEvent.KEY_PRESSED && event.getKeyCode() == KeyEvent.VK_SPACE)
			spacePresses.incrementAndGet();
		if (dropReleases && event.getID() == KeyEvent.KEY_RELEASED) {
			droppedReleases++;
			event.consume();
			return true;
		}
		return false;
	};

	private SnakeFocusLayoutTests() throws Exception {
		robot = new Robot();
		robot.setAutoDelay(25);
		edt(() -> {
			KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(inputProbe);
			return null;
		});
		SnakeFrame.main(new String[0]);
		await(() -> findFrame() != null, "main window appears");
		frame = edt(SnakeFocusLayoutTests::findFrame);
		activate(frame);
	}

	public static void main(final String[] args) {
		int result = 0;
		SnakeFocusLayoutTests tests = null;
		try {
			if (args.length > 1 || (args.length == 1 && !List.of("focus", "layout", "small-layout").contains(args[0])))
				throw new IllegalArgumentException("Expected focus, layout, small-layout, or no arguments");
			tests = new SnakeFocusLayoutTests();
			if (args.length == 0 || "focus".equals(args[0]))
				tests.testFocusReturn();
			if (args.length == 1 && "small-layout".equals(args[0])) {
				final JFrame window = tests.frame;
				final Rectangle screen = edt(() -> window.getGraphicsConfiguration().getBounds());
				check(screen.width == 512 && screen.height == 384, "HiDPI test really uses a 512x384 logical screen");
			}
			if (args.length == 0 || !"focus".equals(args[0]))
				tests.testLayout();
			System.out.println("SnakeFocusLayoutTests: " + checks + " checks passed, 0 failed");
		} catch (final Exception | AssertionError failure) {
			result = 1;
			failure.printStackTrace();
		} finally {
			final SnakeFocusLayoutTests current = tests;
			try {
				if (current != null) {
					for (final int key : new int[] { KeyEvent.VK_SPACE, KeyEvent.VK_F2, KeyEvent.VK_F3 })
						current.robot.keyRelease(key);
				}
				edt(() -> {
					if (current != null)
						KeyboardFocusManager.getCurrentKeyboardFocusManager()
								.removeKeyEventDispatcher(current.inputProbe);
					for (final Window window : Window.getWindows())
						window.dispose();
					return null;
				});
			} catch (final Exception failure) {
				result = 1;
				failure.printStackTrace();
			}
		}
		System.exit(result);
	}

	private void testFocusReturn() throws Exception {
		other = edt(() -> {
			final JFrame window = new JFrame("Focus target");
			window.add(new JButton("Other window"));
			window.setBounds(0, 0, 200, 100);
			return window;
		});
		edt(() -> { combo("topology").setSelectedItem(Topology.TORUS); return null; });
		tap(KeyEvent.VK_F2);
		await(() -> field().status() == SnakeField.Status.RUNNING && field().isFocusOwner(), "start focuses board");
		final AtomicInteger transitions = new AtomicInteger();
		edt(() -> {
			field().addPropertyChangeListener(SnakeField.STATUS_PROPERTY, event -> transitions.incrementAndGet());
			return null;
		});

		robot.keyPress(KeyEvent.VK_SPACE);
		try {
			await(() -> field().status() == SnakeField.Status.PAUSED, "first Space press pauses");
			activate(other);
			activate(frame);
			focus(fieldOnEdt());
			final List<Position> body = edt(() -> List.copyOf(field().snake().body()));
			final int seconds = edt(() -> field().elapsedSeconds());
			final int changes = transitions.get();
			final int presses = spacePresses.get();
			// One physical press only: let native auto-repeat continue across focus.
			for (int i = 0; i < 15; i++) {
				Thread.sleep(100);
				check(edt(() -> field().status() == SnakeField.Status.PAUSED), "held Space cannot resume on focus return");
			}
			System.out.println("Native Space repeats after focus return: " + (spacePresses.get() - presses));
			// Also cover displays configured without native repeat.
			robot.keyPress(KeyEvent.VK_SPACE);
			robot.delay(75);
			check(transitions.get() == changes, "no transient pause/resume transitions occurred");
			check(edt(() -> body.equals(List.copyOf(field().snake().body()))), "body stays frozen across held-key return");
			check(edt(() -> seconds == field().elapsedSeconds()), "clock stays frozen across held-key return");
			// Focused Swing buttons must not bypass the held-key suppression.
			focus(edt(() -> button("Resume")));
			robot.keyPress(KeyEvent.VK_SPACE);
			robot.delay(75);
			check(edt(() -> !button("Resume").getModel().isPressed()), "repeat cannot arm a focused Resume button");
		} finally {
			robot.keyRelease(KeyEvent.VK_SPACE);
			robot.delay(75);
		}
		check(edt(() -> field().status() == SnakeField.Status.PAUSED), "releasing the old key does not resume");
		focus(fieldOnEdt());
		tap(KeyEvent.VK_SPACE);
		await(() -> field().status() == SnakeField.Status.RUNNING, "a fresh press resumes normally");

		// The release may be observed in another application window, or missed
		// completely while another process has focus. Exercise both paths.
		for (final boolean missRelease : new boolean[] { false, true }) {
			robot.keyPress(KeyEvent.VK_SPACE);
			await(() -> field().status() == SnakeField.Status.PAUSED, "pause before outside release");
			activate(other);
			edt(() -> { dropReleases = missRelease; return null; });
			try {
				robot.keyRelease(KeyEvent.VK_SPACE);
				robot.delay(75);
			} finally {
				edt(() -> { dropReleases = false; return null; });
			}
			if (missRelease)
				check(edt(() -> droppedReleases > 0), "outside release was actually hidden from game dispatcher");
			activate(frame);
			focus(fieldOnEdt());
			check(edt(() -> field().status() == SnakeField.Status.PAUSED), "outside release never resumes on return");
			if (missRelease) {
				tap(KeyEvent.VK_SPACE);
				check(edt(() -> field().status() == SnakeField.Status.PAUSED), "one safe tap re-arms a missed release");
			}
			tap(KeyEvent.VK_SPACE);
			await(() -> field().status() == SnakeField.Status.RUNNING, "shortcut recovers after outside release");
		}

		robot.keyPress(KeyEvent.VK_F3);
		try {
			await(() -> field().status() == SnakeField.Status.READY, "held F3 resets once");
			final SnakeField fresh = fieldOnEdt();
			activate(other);
			activate(frame);
			focus(fresh);
			Thread.sleep(1200);
			robot.keyPress(KeyEvent.VK_F3);
			robot.delay(75);
			check(edt(() -> field() == fresh), "held F3 cannot reset again after focus return");
		} finally {
			robot.keyRelease(KeyEvent.VK_F3);
		}
		final SnakeField beforeTap = fieldOnEdt();
		tap(KeyEvent.VK_F3);
		await(() -> field() != beforeTap, "fresh F3 press resets normally");

		robot.keyPress(KeyEvent.VK_F2);
		try {
			await(() -> field().status() == SnakeField.Status.RUNNING, "held F2 starts once");
			activate(other);
			activate(frame);
			edt(() -> { button("Restart").doClick(0); return null; });
			focus(fieldOnEdt());
			Thread.sleep(700);
			robot.keyPress(KeyEvent.VK_F2);
			robot.delay(75);
			check(edt(() -> field().status() == SnakeField.Status.READY), "old held F2 cannot start a reset game");
		} finally {
			robot.keyRelease(KeyEvent.VK_F2);
		}
		tap(KeyEvent.VK_F2);
		await(() -> field().status() == SnakeField.Status.RUNNING, "fresh F2 press starts normally");

		// A key originally handled by a focused control must also be tracked.
		focus(edt(() -> button("Pause")));
		robot.keyPress(KeyEvent.VK_SPACE);
		try {
			await(() -> button("Pause").getModel().isPressed(), "focused button received initial Space press");
			activate(other);
			activate(frame);
			focus(fieldOnEdt());
			Thread.sleep(700);
			robot.keyPress(KeyEvent.VK_SPACE);
			robot.delay(75);
			check(edt(() -> field().status() == SnakeField.Status.PAUSED), "held control key cannot become a board shortcut");
		} finally {
			robot.keyRelease(KeyEvent.VK_SPACE);
			robot.delay(75);
		}
		check(edt(() -> field().status() == SnakeField.Status.PAUSED), "releasing held control key preserves pause");
	}

	private void testLayout() throws Exception {
		tap(KeyEvent.VK_F3);
		await(() -> field().status() == SnakeField.Status.READY, "layout test starts ready");
		System.out.println("Logical screen: " + edt(() -> frame.getGraphicsConfiguration().getBounds()));
		for (final Topology topology : Topology.PRESETS)
			for (int zoom = 0; zoom < SnakeField.ZOOM_LEVELS.size(); zoom++) {
				final int index = zoom;
				edt(() -> {
					combo("topology").setSelectedItem(topology);
					combo("zoom").setSelectedIndex(index);
					frame.validate();
					assertControlsVisible();
					check(field().zoom() == SnakeField.ZOOM_LEVELS.get(index), "zoom applied without hiding settings");
					return null;
				});
			}
		// Reach the previously hidden selector through real Tab navigation, then
		// change it without a mouse on the scaled/small-screen CI configuration.
		final JComboBox<?> zoom = edt(() -> combo("zoom"));
		for (int tries = 0; tries < 20 && !edt(zoom::isFocusOwner); tries++)
			tap(KeyEvent.VK_TAB);
		check(edt(zoom::isFocusOwner), "Tab reaches visible zoom selector");
		tap(KeyEvent.VK_SPACE);
		tap(KeyEvent.VK_HOME);
		tap(KeyEvent.VK_ENTER);
		await(() -> field().zoom() == 100, "zoom is usable by keyboard on this display");
		edt(() -> { assertControlsVisible(); return null; });
	}

	/** Geometric clipping checks must inspect each control, not just the outer window. */
	private void assertControlsVisible() {
		final Rectangle screen = frame.getGraphicsConfiguration().getBounds();
		final Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(frame.getGraphicsConfiguration());
		final Rectangle workArea = new Rectangle(screen.x + insets.left, screen.y + insets.top,
				screen.width - insets.left - insets.right, screen.height - insets.top - insets.bottom);
		check(workArea.contains(frame.getBounds()), "window fits available work area: " + frame.getBounds() + " in " + workArea);
		final List<JComponent> controls = new ArrayList<>();
		controls.add(find(frame, JSlider.class, c -> true));
		controls.add(combo("topology"));
		controls.add(combo("zoom"));
		for (final String text : List.of("Start", "Exit", "Restart", "Pause", "About"))
			controls.add(button(text));
		final List<Rectangle> bounds = new ArrayList<>();
		for (final JComponent control : controls) {
			final Rectangle full = new Rectangle(0, 0, control.getWidth(), control.getHeight());
			check(!full.isEmpty() && control.getVisibleRect().equals(full),
					"entire control is visible: " + control.getAccessibleContext().getAccessibleName());
			check(control.getHeight() >= control.getPreferredSize().height, "control is not vertically compressed");
			if (control instanceof JButton)
				check(control.getWidth() >= control.getPreferredSize().width, "button caption is not truncated");
			final Rectangle rectangle = SwingUtilities.convertRectangle(control.getParent(), control.getBounds(), frame);
			for (final Rectangle previous : bounds)
				check(!rectangle.intersects(previous), "controls do not overlap");
			bounds.add(rectangle);
		}
		check(!field().getVisibleRect().isEmpty(), "board viewport remains reachable");
	}

	private SnakeField fieldOnEdt() throws Exception {
		return edt(this::field);
	}

	private SnakeField field() {
		return find(frame, SnakeField.class, c -> true);
	}

	private JComboBox<?> combo(final String name) {
		return find(frame, JComboBox.class, c -> name.equals(c.getName()));
	}

	private JButton button(final String text) {
		return find(frame, JButton.class, c -> text.equals(c.getText()));
	}

	private void activate(final JFrame window) throws Exception {
		edt(() -> { window.setVisible(true); window.toFront(); window.requestFocus(); return null; });
		await(window::isFocused, "window gains focus: " + edt(window::getTitle));
	}

	private void focus(final JComponent component) throws Exception {
		edt(() -> { component.requestFocusInWindow(); return null; });
		await(component::isFocusOwner, "component gains focus");
	}

	private void tap(final int key) {
		robot.keyPress(key);
		robot.keyRelease(key);
		robot.delay(75);
	}

	private static JFrame findFrame() {
		return Arrays.stream(Frame.getFrames()).filter(JFrame.class::isInstance).map(JFrame.class::cast)
				.filter(f -> f.isVisible() && "Snake".equals(f.getTitle())).findFirst().orElse(null);
	}

	private static <T extends Component> T find(final Container root, final Class<T> type,
			final Predicate<T> predicate) {
		final T found = findOrNull(root, type, predicate);
		if (found == null)
			throw new AssertionError("Missing component: " + type.getSimpleName());
		return found;
	}

	private static <T extends Component> T findOrNull(final Container root, final Class<T> type,
			final Predicate<T> predicate) {
		for (final Component child : root.getComponents()) {
			if (type.isInstance(child) && predicate.test(type.cast(child)))
				return type.cast(child);
			if (child instanceof Container container) {
				final T found = findOrNull(container, type, predicate);
				if (found != null)
					return found;
			}
		}
		return null;
	}

	private static <T> T edt(final Callable<T> callable) throws Exception {
		final FutureTask<T> task = new FutureTask<>(callable);
		SwingUtilities.invokeLater(task);
		return task.get(5, TimeUnit.SECONDS);
	}

	private static void await(final Callable<Boolean> condition, final String message) throws Exception {
		final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (!edt(condition)) {
			if (System.nanoTime() >= deadline)
				throw new AssertionError("Timed out: " + message);
			Thread.sleep(20);
		}
		check(true, message);
	}

	private static void check(final boolean condition, final String message) {
		if (!condition)
			throw new AssertionError(message);
		checks++;
	}
}
