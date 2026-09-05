package snake.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import snake.Direction;
import snake.Position;
import snake.topology.Topology;

/** Real input and timer integration tests. The orchestration never blocks the EDT. */
public final class SnakeInputTests {
	private static int checks;
	private final Robot robot;
	private final JFrame frame;
	private final JFrame other;

	private SnakeInputTests() throws Exception {
		robot = new Robot();
		robot.setAutoDelay(25);
		SnakeFrame.main(new String[0]);
		await(() -> findFrame() != null, "main window appears");
		frame = edt(SnakeInputTests::findFrame);
		other = edt(() -> {
			final JFrame window = new JFrame("Focus target");
			window.setModalExclusionType(Dialog.ModalExclusionType.APPLICATION_EXCLUDE);
			window.add(new JButton("Other window"));
			window.setBounds(1000, 10, 200, 100);
			return window;
		});
		activate(frame);
	}

	public static void main(final String[] args) {
		int result = 0;
		try {
			new SnakeInputTests().run();
			System.out.println("SnakeInputTests: " + checks + " checks passed, 0 failed");
		} catch (final Exception | AssertionError failure) {
			result = 1;
			failure.printStackTrace();
			System.err.println("SnakeInputTests: failed after " + checks + " checks");
		} finally {
			try {
				edt(() -> {
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

	private void run() throws Exception {
		check(!SwingUtilities.isEventDispatchThread(), "Robot orchestration runs outside the EDT");
		final JComboBox<?> topology = edt(() -> find(frame, JComboBox.class, c -> "topology".equals(c.getName())));
		final JSlider speed = edt(() -> find(frame, JSlider.class, c -> true));
		final JComboBox<?> zoom = edt(() -> find(frame, JComboBox.class, c -> "zoom".equals(c.getName())));
		tabTo(topology);
		tap(KeyEvent.VK_SPACE);
		tap(KeyEvent.VK_HOME);
		for (int i = 0; i < 3; i++)
			tap(KeyEvent.VK_DOWN);
		tap(KeyEvent.VK_ENTER);
		await(() -> field().topology().equals(Topology.TORUS), "topology chosen entirely by keyboard");
		tabTo(speed);
		tap(KeyEvent.VK_HOME);
		tap(KeyEvent.VK_UP);
		check(edt(() -> speed.getValue() == 2), "focused slider receives arrow input");
		check(edt(() -> field().snake().direction() == Direction.RIGHT), "settings arrows do not steer");
		tabTo(zoom);
		tap(KeyEvent.VK_SPACE);
		tap(KeyEvent.VK_END);
		tap(KeyEvent.VK_ENTER);
		await(() -> field().zoom() == 200, "zoom chosen by keyboard");
		check(edt(() -> frame.getHeight() <= frame.getGraphicsConfiguration().getBounds().height),
				"zoom stays within the screen, with scrolling for small displays");
		tap(KeyEvent.VK_SPACE);
		tap(KeyEvent.VK_HOME);
		tap(KeyEvent.VK_DOWN);
		tap(KeyEvent.VK_ENTER);
		await(() -> field().zoom() == 150, "intermediate zoom selected");

		tap(KeyEvent.VK_F2);
		await(() -> field().status() == SnakeField.Status.RUNNING && field().isFocusOwner(),
				"start shortcut starts the game and focuses the board");
		final Position initial = edt(() -> field().snake().head());
		await(() -> !field().snake().head().equals(initial), "real Swing timer moves the snake");
		testHeldSpace();
		tap(KeyEvent.VK_UP);
		await(() -> field().snake().direction() == Direction.UP, "real arrow event steers while paused");
		tap(KeyEvent.VK_RIGHT);
		click(edt(() -> button("Resume")));
		await(() -> field().status() == SnakeField.Status.RUNNING && field().isFocusOwner(),
				"mouse resume restores board focus");
		click(edt(() -> button("Pause")));
		await(() -> field().status() == SnakeField.Status.PAUSED && field().isFocusOwner(),
				"mouse pause restores board focus");
		tap(KeyEvent.VK_SPACE);
		await(() -> field().status() == SnakeField.Status.RUNNING, "Space also resumes after clicking controls");
		testRestart();
		testFocusLoss();
		testAbout();

		tap(KeyEvent.VK_SPACE);
		await(() -> field().status() == SnakeField.Status.RUNNING, "game can resume after dialog tests");
		final SnakeField last = edt(this::field);
		edt(() -> { frame.dispose(); return null; });
		await(() -> last.status() == SnakeField.Status.FINISHED, "disposing window stops its timer");
		final List<Position> stopped = edt(() -> List.copyOf(last.snake().body()));
		Thread.sleep(400);
		check(edt(() -> stopped.equals(List.copyOf(last.snake().body()))), "disposed field never moves again");
	}

	private void testHeldSpace() throws Exception {
		final AtomicInteger changes = new AtomicInteger();
		edt(() -> {
			field().addPropertyChangeListener(SnakeField.STATUS_PROPERTY, event -> changes.incrementAndGet());
			return null;
		});
		robot.keyPress(KeyEvent.VK_SPACE);
		try {
			await(() -> field().status() == SnakeField.Status.PAUSED, "Space pauses on first press");
			final List<Position> paused = edt(() -> List.copyOf(field().snake().body()));
			final int seconds = edt(() -> field().elapsedSeconds());
			// Longer than the normal keyboard repeat delay; also simulate additional
			// presses for displays with auto-repeat disabled. No release occurs here.
			for (int i = 0; i < 12; i++) {
				Thread.sleep(100);
				robot.keyPress(KeyEvent.VK_SPACE);
				check(edt(() -> field().status() == SnakeField.Status.PAUSED), "held Space stays paused");
			}
			check(changes.get() == 1, "one physical press causes exactly one state transition");
			check(edt(() -> paused.equals(List.copyOf(field().snake().body()))), "paused timer cannot move the snake");
			check(edt(() -> seconds == field().elapsedSeconds()), "paused clock stays fixed");
		} finally {
			robot.keyRelease(KeyEvent.VK_SPACE);
		}
		tap(KeyEvent.VK_SPACE);
		await(() -> field().status() == SnakeField.Status.RUNNING, "release re-arms the Space shortcut");
		tap(KeyEvent.VK_ESCAPE);
		await(() -> field().status() == SnakeField.Status.PAUSED, "Escape pauses without toggling");
		tap(KeyEvent.VK_ESCAPE);
		check(edt(() -> field().status() == SnakeField.Status.PAUSED), "Escape never resumes");
		robot.keyPress(KeyEvent.VK_SPACE);
		await(() -> field().status() == SnakeField.Status.RUNNING, "press before modified release resumes");
		robot.keyPress(KeyEvent.VK_SHIFT);
		robot.keyRelease(KeyEvent.VK_SPACE);
		robot.keyRelease(KeyEvent.VK_SHIFT);
		tap(KeyEvent.VK_SPACE);
		await(() -> field().status() == SnakeField.Status.PAUSED, "modified release re-arms Space");
	}

	private void testRestart() throws Exception {
		final SnakeField old = edt(this::field);
		robot.keyPress(KeyEvent.VK_F3);
		try {
			await(() -> field() != old, "restart installs a fresh field");
			final SnakeField fresh = edt(this::field);
			final List<Position> stopped = edt(() -> List.copyOf(old.snake().body()));
			Thread.sleep(800);
			robot.keyPress(KeyEvent.VK_F3);
			check(edt(() -> field() == fresh), "held restart does not replace the field repeatedly");
			check(edt(() -> old.status() == SnakeField.Status.FINISHED
					&& stopped.equals(List.copyOf(old.snake().body()))), "old timer stays stopped after restart");
			check(edt(() -> fresh.zoom() == 150 && fresh.topology().equals(Topology.TORUS)
					&& fresh.moveDelay() == SnakeField.MOVE_DELAYS_MS.get(1)), "restart retains all three settings");
		} finally {
			robot.keyRelease(KeyEvent.VK_F3);
		}
		// Exercise the actual focused Start button, not just its ActionMap.
		tabTo(edt(() -> button("Start")));
		tap(KeyEvent.VK_SPACE);
		await(() -> field().status() == SnakeField.Status.RUNNING && field().isFocusOwner(),
				"Tab and Space can start without a mouse");
		tap(KeyEvent.VK_UP);
		await(() -> field().snake().direction() == Direction.UP, "board arrows beat scroll-pane bindings");
	}

	private void testFocusLoss() throws Exception {
		activate(other);
		await(() -> field().status() == SnakeField.Status.PAUSED, "losing focus pauses a running game");
		check(edt(() -> button("Resume").isEnabled()), "automatic pause updates controls");
		final List<Position> paused = edt(() -> List.copyOf(field().snake().body()));
		Thread.sleep(400);
		check(edt(() -> paused.equals(List.copyOf(field().snake().body()))), "unfocused game does not move");
		activate(frame);
		focusBoard();
		Thread.sleep(200);
		check(edt(() -> field().status() == SnakeField.Status.PAUSED), "regaining focus does not resume");

		robot.keyPress(KeyEvent.VK_SPACE);
		await(() -> field().status() == SnakeField.Status.RUNNING, "explicit press resumes after focus loss");
		activate(other);
		await(() -> field().status() == SnakeField.Status.PAUSED, "focus loss also pauses with a key held");
		robot.keyRelease(KeyEvent.VK_SPACE); // Released outside the game's window.
		activate(frame);
		focusBoard();
		tap(KeyEvent.VK_SPACE);
		await(() -> field().status() == SnakeField.Status.RUNNING, "focus loss clears a missing key release");
	}

	private void testAbout() throws Exception {
		click(edt(() -> button("About")));
		await(() -> about() != null && about().isFocused(), "About dialog opens");
		check(edt(() -> field().status() == SnakeField.Status.PAUSED), "About pauses gameplay");
		tap(KeyEvent.VK_ESCAPE);
		await(() -> about() == null && frame.isFocused(), "About closes normally");
		await(() -> field().status() == SnakeField.Status.RUNNING, "normal About close restores running game");

		tap(KeyEvent.VK_ESCAPE);
		await(() -> field().status() == SnakeField.Status.PAUSED, "manual pause before About");
		click(edt(() -> button("About")));
		await(() -> about() != null && about().isFocused(), "About opens from paused state");
		tap(KeyEvent.VK_ESCAPE);
		await(() -> about() == null && frame.isFocused(), "paused About closes");
		check(edt(() -> field().status() == SnakeField.Status.PAUSED), "About preserves a manual pause");

		focusBoard();
		tap(KeyEvent.VK_SPACE);
		await(() -> field().status() == SnakeField.Status.RUNNING, "resume before external-focus dialog test");
		click(edt(() -> button("About")));
		await(() -> about() != null && about().isFocused(), "About opens before switching windows");
		activate(other);
		activate(edt(SnakeInputTests::about));
		tap(KeyEvent.VK_ESCAPE);
		await(() -> about() == null && frame.isFocused(), "About closes after an external focus change");
		focusBoard();
		Thread.sleep(200);
		check(edt(() -> field().status() == SnakeField.Status.PAUSED), "external focus loss cancels About auto-resume");
	}

	private void focusBoard() throws Exception {
		edt(() -> { field().requestFocusInWindow(); return null; });
		await(() -> field().isFocusOwner(), "board receives focus");
	}

	private void activate(final Window window) throws Exception {
		edt(() -> {
			window.setVisible(true);
			window.toFront();
			window.requestFocus();
			return null;
		});
		await(window::isFocused, "window gains focus: " + window.getName());
	}

	private void tabTo(final Component target) throws Exception {
		for (int i = 0; i < 30; i++) {
			if (edt(() -> KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() == target)) {
				check(true, "Tab reaches " + target.getClass().getSimpleName());
				return;
			}
			tap(KeyEvent.VK_TAB);
		}
		throw new AssertionError("Tab could not reach " + target);
	}

	private void tap(final int key) {
		robot.keyPress(key);
		robot.keyRelease(key);
		robot.delay(50);
	}

	private void click(final Component component) throws Exception {
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

	private SnakeField field() {
		return find(frame, SnakeField.class, c -> true);
	}

	private JButton button(final String text) {
		return find(frame, JButton.class, button -> text.equals(button.getText()));
	}

	private static JFrame findFrame() {
		return Arrays.stream(Frame.getFrames()).filter(JFrame.class::isInstance).map(JFrame.class::cast)
				.filter(frame -> frame.isVisible() && "Snake".equals(frame.getTitle())).findFirst().orElse(null);
	}

	private static JDialog about() {
		return Arrays.stream(Window.getWindows()).filter(JDialog.class::isInstance).map(JDialog.class::cast)
				.filter(dialog -> dialog.isVisible() && "About".equals(dialog.getTitle())).findFirst().orElse(null);
	}

	private static <T extends Component> T find(final Container root, final Class<T> type,
			final Predicate<T> predicate) {
		for (final Component child : root.getComponents()) {
			if (type.isInstance(child) && predicate.test(type.cast(child)))
				return type.cast(child);
			if (child instanceof Container container) {
				final T found = find(container, type, predicate);
				if (found != null)
					return found;
			}
		}
		return null;
	}

	private static <T> T edt(final Callable<T> action) throws Exception {
		final FutureTask<T> task = new FutureTask<>(action);
		SwingUtilities.invokeLater(task);
		return task.get(5, TimeUnit.SECONDS);
	}

	private static void await(final Callable<Boolean> condition, final String message) throws Exception {
		final long deadline = System.nanoTime() + 5_000_000_000L;
		while (System.nanoTime() < deadline) {
			if (edt(condition)) {
				check(true, message);
				return;
			}
			Thread.sleep(20);
		}
		throw new AssertionError("Timed out: " + message + edt(() -> Arrays.stream(Window.getWindows())
				.map(w -> "\n" + w.getClass().getSimpleName() + " " + w.getName() + " visible=" + w.isVisible()
						+ " focused=" + w.isFocused() + " owner=" + (w.getOwner() == null ? "none" : w.getOwner().getName()))
				.reduce("", String::concat)));
	}

	private static void check(final boolean condition, final String message) {
		if (!condition)
			throw new AssertionError(message);
		checks++;
		System.out.println("PASS " + checks + ": " + message);
	}
}
