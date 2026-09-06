package snake.gui;

import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import javax.swing.SwingUtilities;
import snake.Direction;
import snake.Position;
import snake.Snake;

/**
 * Shared assertion helpers, fixtures and component finders for the suites.
 * Failures are collected rather than thrown, so one run reports every broken
 * check; a suite that aborts with an exception is reported as a failure as
 * well.
 */
final class TestSupport {
	@FunctionalInterface
	interface CheckedAction {
		void run() throws Exception;
	}

	/** An apple position that no fixture snake occupies. */
	static final Position SAFE_APPLE = new Position(10, 10);

	private static int failed;
	private static int passed;

	private TestSupport() {
	}

	/** A fresh three-cell snake heading right along the top row, head at (2, 0). */
	static Snake shortSnake() {
		return new Snake(Direction.RIGHT, List.of(new Position(2, 0), new Position(1, 0), new Position(0, 0)));
	}

	/** Every board cell except the given free ones, with the head first. */
	static List<Position> almostFullBody(final Position head, final Position... freePositions) {
		final List<Position> free = Arrays.asList(freePositions);
		final List<Position> body = new ArrayList<>(SnakeField.CELL_COUNT - free.size());
		body.add(head);
		for (int y = 0; y < SnakeField.BOARD_ROWS; y++)
			for (int x = 0; x < SnakeField.BOARD_COLUMNS; x++) {
				final Position position = new Position(x, y);
				if (!position.equals(head) && !free.contains(position))
					body.add(position);
			}
		return body;
	}

	/** Depth-first search for the first matching component under the root, or null. */
	static <T extends Component> T find(final Container root, final Class<T> type, final Predicate<T> predicate) {
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

	/** Like find, but a missing component fails the test. */
	static <T extends Component> T component(final Container root, final Class<T> type,
			final Predicate<T> predicate) {
		final T found = find(root, type, predicate);
		if (found == null)
			throw new AssertionError("Missing component: " + type.getSimpleName());
		return found;
	}

	/** Runs the suite body on the EDT, prints the summary and returns the process exit code. */
	static int run(final String suiteName, final Runnable suite) {
		try {
			SwingUtilities.invokeAndWait(suite);
		} catch (final InterruptedException | InvocationTargetException exception) {
			failed++;
			final Throwable cause = exception.getCause() == null ? exception : exception.getCause();
			System.err.println("FAIL: " + suiteName + " aborted: " + cause);
			cause.printStackTrace();
		}
		System.out.println(suiteName + ": " + passed + " checks passed, " + failed + " failed");
		return failed == 0 ? 0 : 1;
	}

	static void check(final boolean condition, final String message) {
		if (condition) {
			passed++;
		} else {
			failed++;
			System.err.println("FAIL: " + message);
		}
	}

	static void equal(final Object expected, final Object actual, final String message) {
		check(Objects.equals(expected, actual), message + " (expected " + expected + ", got " + actual + ")");
	}

	static void expect(final Class<? extends Exception> type, final CheckedAction action, final String message) {
		try {
			action.run();
			check(false, message + " (nothing was thrown)");
		} catch (final Exception thrown) {
			check(type.isInstance(thrown), message + " (expected " + type.getSimpleName() + ", got "
					+ thrown.getClass().getSimpleName() + ")");
		}
	}
}
