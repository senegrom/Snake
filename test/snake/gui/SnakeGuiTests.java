package snake.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JSlider;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import snake.Direction;

/** End-to-end smoke tests for the real Swing window under a virtual display. */
public final class SnakeGuiTests {
	private static int checks;

	private SnakeGuiTests() {
	}

	public static void main(final String[] args) throws Exception {
		SnakeFrame.main(args);
		try {
			SwingUtilities.invokeAndWait(SnakeGuiTests::run);
			System.out.println(checks + " GUI checks passed");
		} catch (final Exception exception) {
			final Throwable cause = exception.getCause() == null ? exception : exception.getCause();
			cause.printStackTrace();
			System.exit(1);
		}
		System.exit(0);
	}

	private static void run() {
		final JFrame originalFrame = visibleSnakeFrame();
		check(originalFrame.isVisible(), "main window is visible");
		check(!originalFrame.isResizable(), "main window is fixed-size");
		equal("Snake", originalFrame.getTitle(), "main window title");

		final JButton start = button(originalFrame, "Start");
		final JButton pause = button(originalFrame, "Pause");
		final JButton restart = button(originalFrame, "Restart");
		final JSlider speed = component(originalFrame, JSlider.class, ignored -> true);
		final JComboBox<?> topology = component(originalFrame, JComboBox.class, ignored -> true);
		final SnakeField field = component(originalFrame, SnakeField.class, ignored -> true);

		check(start.isEnabled(), "start button begins enabled");
		check(!pause.isEnabled(), "pause button begins disabled");
		check(speed.isEnabled(), "speed begins editable");
		check(topology.isEnabled(), "topology begins editable");
		equal(SnakeField.Status.READY, field.status(), "field begins ready");

		start.doClick(0);
		check(!start.isEnabled(), "starting disables the start button");
		check(pause.isEnabled(), "starting enables pause");
		check(!speed.isEnabled(), "starting locks speed");
		check(!topology.isEnabled(), "starting locks topology");
		equal(SnakeField.Status.RUNNING, field.status(), "start button starts the field");

		pause.doClick(0);
		equal(SnakeField.Status.PAUSED, field.status(), "pause button pauses the field");
		equal("Resume", pause.getText(), "pause button changes to Resume");

		invokeKey(originalFrame, KeyEvent.VK_UP);
		equal(Direction.UP, field.snake().direction(), "window arrow binding steers the snake");
		invokeKey(originalFrame, KeyEvent.VK_SPACE);
		equal(SnakeField.Status.RUNNING, field.status(), "window Space binding resumes");
		equal("Pause", pause.getText(), "Space updates the pause-button label");
		invokeKey(originalFrame, KeyEvent.VK_SPACE);
		equal(SnakeField.Status.PAUSED, field.status(), "window Space binding pauses");

		for (int steps = 0; field.status() != SnakeField.Status.FINISHED
				&& steps <= SnakeField.BOARD_ROWS; steps++)
			field.step();
		equal(SnakeField.Status.FINISHED, field.status(), "wall collision finishes the field");
		check(!pause.isEnabled(), "finish event disables pause in the window");
		equal("Pause", pause.getText(), "finish event resets the pause-button label");

		restart.doClick(0);
		check(!originalFrame.isDisplayable(), "restart disposes the old window");
		final JFrame restartedFrame = visibleSnakeFrame();
		check(restartedFrame != originalFrame, "restart creates a new window");
		final SnakeField restartedField = component(restartedFrame, SnakeField.class, ignored -> true);
		equal(SnakeField.Status.READY, restartedField.status(), "restarted field is ready");
		check(button(restartedFrame, "Start").isEnabled(), "restarted window can start");
		check(!button(restartedFrame, "Pause").isEnabled(), "restarted pause button is disabled");

		restartedField.shutdown();
		restartedFrame.dispose();
	}

	private static void invokeKey(final JFrame frame, final int keyCode) {
		final KeyStroke keyStroke = KeyStroke.getKeyStroke(keyCode, 0);
		final Object actionKey = frame.getRootPane()
				.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).get(keyStroke);
		check(actionKey != null, "key is present in the window input map: " + keyCode);
		final Action action = frame.getRootPane().getActionMap().get(actionKey);
		check(action != null, "key has a window action: " + keyCode);
		action.actionPerformed(new ActionEvent(frame, ActionEvent.ACTION_PERFORMED, actionKey.toString()));
	}

	private static JFrame visibleSnakeFrame() {
		return Arrays.stream(Frame.getFrames())
				.filter(JFrame.class::isInstance)
				.map(JFrame.class::cast)
				.filter(Frame::isVisible)
				.filter(frame -> Objects.equals("Snake", frame.getTitle()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("No visible Snake window"));
	}

	private static JButton button(final Container root, final String text) {
		return component(root, JButton.class, button -> text.equals(button.getText()));
	}

	private static <T extends Component> T component(final Container root, final Class<T> type,
			final Predicate<T> predicate) {
		for (final Component child : root.getComponents()) {
			if (type.isInstance(child)) {
				final T typedChild = type.cast(child);
				if (predicate.test(typedChild))
					return typedChild;
			}
			if (child instanceof Container container) {
				try {
					return component(container, type, predicate);
				} catch (final AssertionError ignored) {
					// Continue searching sibling branches.
				}
			}
		}
		throw new AssertionError("Missing component: " + type.getSimpleName());
	}

	private static void equal(final Object expected, final Object actual, final String message) {
		check(Objects.equals(expected, actual),
				message + " (expected " + expected + ", got " + actual + ")");
	}

	private static void check(final boolean condition, final String message) {
		if (!condition)
			throw new AssertionError(message);
		checks++;
	}
}
