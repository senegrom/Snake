package snake.gui;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import snake.Position;
import snake.topology.Topology;
import static snake.gui.RobotSupport.activate;
import static snake.gui.RobotSupport.await;
import static snake.gui.RobotSupport.awaitStableBounds;
import static snake.gui.RobotSupport.check;
import static snake.gui.RobotSupport.edt;
import static snake.gui.RobotSupport.focus;
import static snake.gui.RobotSupport.launchGame;
import static snake.gui.RobotSupport.openPopup;
import static snake.gui.RobotSupport.tabTo;
import static snake.gui.RobotSupport.tap;
import static snake.gui.TestSupport.button;
import static snake.gui.TestSupport.combo;
import static snake.gui.TestSupport.component;
import static snake.gui.TestSupport.field;

/** Regressions for returning with a key held and controls on small, scaled displays. */
public final class SnakeFocusLayoutTests {
	private final Robot robot;
	private final JFrame frame;
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
		robot = RobotSupport.robot();
		edt(() -> {
			KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(inputProbe);
			return null;
		});
		frame = launchGame();
		activate(frame);
	}

	public static void main(final String[] args) {
		RobotSupport.run("SnakeFocusLayoutTests", () -> {
			if (args.length > 1 || (args.length == 1 && !List.of("focus", "layout", "small-layout").contains(args[0])))
				throw new IllegalArgumentException("Expected focus, layout, small-layout, or no arguments");
			final SnakeFocusLayoutTests tests = new SnakeFocusLayoutTests();
			if (args.length == 0 || "focus".equals(args[0]))
				tests.testFocusReturn();
			if (args.length == 1 && "small-layout".equals(args[0])) {
				final Rectangle screen = edt(() -> tests.frame.getGraphicsConfiguration().getBounds());
				check(screen.width == 512 && screen.height == 384, "HiDPI test really uses a 512x384 logical screen");
			}
			if (args.length == 0 || !"focus".equals(args[0]))
				tests.testLayout();
		});
	}

	private void testFocusReturn() throws Exception {
		other = edt(() -> {
			final JFrame window = new JFrame("Focus target");
			window.add(new JButton("Other window"));
			window.setBounds(0, 0, 200, 100);
			return window;
		});
		edt(() -> { combo(frame, "topology").setSelectedItem(Topology.TORUS); return null; });
		tap(KeyEvent.VK_F2);
		await(() -> field(frame).status() == SnakeField.Status.RUNNING && field(frame).isFocusOwner(),
				"start focuses board");
		final AtomicInteger transitions = new AtomicInteger();
		edt(() -> {
			field(frame).addPropertyChangeListener(SnakeField.STATUS_PROPERTY, event -> transitions.incrementAndGet());
			return null;
		});

		robot.keyPress(KeyEvent.VK_SPACE);
		try {
			await(() -> field(frame).status() == SnakeField.Status.PAUSED, "first Space press pauses");
			activate(other);
			activate(frame);
			focus(edt(() -> field(frame)));
			final List<Position> body = edt(() -> List.copyOf(field(frame).snake().body()));
			final int seconds = edt(() -> field(frame).elapsedSeconds());
			final int changes = transitions.get();
			final int presses = spacePresses.get();
			// One physical press only: let native auto-repeat continue across focus.
			for (int i = 0; i < 15; i++) {
				Thread.sleep(100);
				check(edt(() -> field(frame).status() == SnakeField.Status.PAUSED),
						"held Space cannot resume on focus return");
			}
			System.out.println("Native Space repeats after focus return: " + (spacePresses.get() - presses));
			// Also cover displays configured without native repeat.
			robot.keyPress(KeyEvent.VK_SPACE);
			robot.waitForIdle();
			check(transitions.get() == changes, "no transient pause/resume transitions occurred");
			check(edt(() -> body.equals(List.copyOf(field(frame).snake().body()))),
					"body stays frozen across held-key return");
			check(edt(() -> seconds == field(frame).elapsedSeconds()), "clock stays frozen across held-key return");
			// Focused Swing buttons must not bypass the held-key suppression.
			focus(edt(() -> button(frame, "Resume")));
			robot.keyPress(KeyEvent.VK_SPACE);
			robot.waitForIdle();
			check(edt(() -> !button(frame, "Resume").getModel().isPressed()), "repeat cannot arm a focused Resume button");
		} finally {
			robot.keyRelease(KeyEvent.VK_SPACE);
			robot.waitForIdle();
		}
		check(edt(() -> field(frame).status() == SnakeField.Status.PAUSED), "releasing the old key does not resume");
		focus(edt(() -> field(frame)));
		tap(KeyEvent.VK_SPACE);
		await(() -> field(frame).status() == SnakeField.Status.RUNNING, "a fresh press resumes normally");

		// The release may be observed in another application window, or missed
		// completely while another process has focus. Exercise both paths.
		for (final boolean missRelease : new boolean[] { false, true }) {
			robot.keyPress(KeyEvent.VK_SPACE);
			await(() -> field(frame).status() == SnakeField.Status.PAUSED, "pause before outside release");
			activate(other);
			edt(() -> { dropReleases = missRelease; return null; });
			try {
				robot.keyRelease(KeyEvent.VK_SPACE);
				robot.waitForIdle();
			} finally {
				edt(() -> { dropReleases = false; return null; });
			}
			if (missRelease)
				check(edt(() -> droppedReleases > 0), "outside release was actually hidden from game dispatcher");
			activate(frame);
			focus(edt(() -> field(frame)));
			check(edt(() -> field(frame).status() == SnakeField.Status.PAUSED), "outside release never resumes on return");
			if (missRelease) {
				tap(KeyEvent.VK_SPACE);
				robot.waitForIdle();
				check(edt(() -> field(frame).status() == SnakeField.Status.PAUSED), "one safe tap re-arms a missed release");
			}
			tap(KeyEvent.VK_SPACE);
			await(() -> field(frame).status() == SnakeField.Status.RUNNING, "shortcut recovers after outside release");
		}

		robot.keyPress(KeyEvent.VK_F3);
		try {
			await(() -> field(frame).status() == SnakeField.Status.READY, "held F3 resets once");
			final SnakeField fresh = edt(() -> field(frame));
			activate(other);
			activate(frame);
			focus(fresh);
			Thread.sleep(1200);
			robot.keyPress(KeyEvent.VK_F3);
			robot.waitForIdle();
			check(edt(() -> field(frame) == fresh), "held F3 cannot reset again after focus return");
		} finally {
			robot.keyRelease(KeyEvent.VK_F3);
		}
		final SnakeField beforeTap = edt(() -> field(frame));
		tap(KeyEvent.VK_F3);
		await(() -> field(frame) != beforeTap, "fresh F3 press resets normally");

		robot.keyPress(KeyEvent.VK_F2);
		try {
			await(() -> field(frame).status() == SnakeField.Status.RUNNING, "held F2 starts once");
			activate(other);
			activate(frame);
			edt(() -> { button(frame, "Restart").doClick(0); return null; });
			focus(edt(() -> field(frame)));
			Thread.sleep(700);
			robot.keyPress(KeyEvent.VK_F2);
			robot.waitForIdle();
			check(edt(() -> field(frame).status() == SnakeField.Status.READY), "old held F2 cannot start a reset game");
		} finally {
			robot.keyRelease(KeyEvent.VK_F2);
		}
		tap(KeyEvent.VK_F2);
		await(() -> field(frame).status() == SnakeField.Status.RUNNING, "fresh F2 press starts normally");

		// A key originally handled by a focused control must also be tracked.
		focus(edt(() -> button(frame, "Pause")));
		robot.keyPress(KeyEvent.VK_SPACE);
		try {
			await(() -> button(frame, "Pause").getModel().isPressed(), "focused button received initial Space press");
			activate(other);
			activate(frame);
			focus(edt(() -> field(frame)));
			Thread.sleep(700);
			robot.keyPress(KeyEvent.VK_SPACE);
			robot.waitForIdle();
			check(edt(() -> field(frame).status() == SnakeField.Status.PAUSED),
					"held control key cannot become a board shortcut");
		} finally {
			robot.keyRelease(KeyEvent.VK_SPACE);
			robot.waitForIdle();
		}
		check(edt(() -> field(frame).status() == SnakeField.Status.PAUSED), "releasing held control key preserves pause");
	}

	private void testLayout() throws Exception {
		tap(KeyEvent.VK_F3);
		await(() -> field(frame).status() == SnakeField.Status.READY, "layout test starts ready");
		System.out.println("Logical screen: " + edt(() -> frame.getGraphicsConfiguration().getBounds()));
		for (final Topology topology : Topology.PRESETS)
			for (int zoom = 0; zoom < SnakeField.ZOOM_LEVELS.size(); zoom++) {
				final int index = zoom;
				edt(() -> {
					combo(frame, "topology").setSelectedItem(topology);
					combo(frame, "zoom").setSelectedIndex(index);
					frame.validate();
					return null;
				});
				awaitStableBounds(frame);
				edt(() -> {
					assertControlsVisible();
					check(field(frame).zoom() == SnakeField.ZOOM_LEVELS.get(index), "zoom applied without hiding settings");
					return null;
				});
			}
		// Reach the previously hidden selector through real Tab navigation, then
		// change it without a mouse on the scaled/small-screen CI configuration.
		tabTo(edt(() -> combo(frame, "zoom")));
		openPopup();
		tap(KeyEvent.VK_HOME);
		tap(KeyEvent.VK_ENTER);
		await(() -> field(frame).zoom() == 100, "zoom is usable by keyboard on this display");
		awaitStableBounds(frame);
		edt(() -> { assertControlsVisible(); return null; });
	}

	/** Geometric clipping checks must inspect each control, not just the outer window. */
	private void assertControlsVisible() {
		final Rectangle workArea = WindowGeometry.workArea(frame);
		check(workArea.contains(frame.getBounds()), "window fits available work area: " + frame.getBounds() + " in " + workArea);
		final List<JComponent> controls = new ArrayList<>();
		controls.add(component(frame, JSlider.class, c -> true));
		controls.add(combo(frame, "topology"));
		controls.add(combo(frame, "zoom"));
		for (final String text : List.of("Start", "Exit", "Restart", "Pause", "About"))
			controls.add(button(frame, text));
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
		check(!field(frame).getVisibleRect().isEmpty(), "board viewport remains reachable");
	}
}
