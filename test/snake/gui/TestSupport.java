package snake.gui;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import javax.swing.SwingUtilities;

/**
 * Shared assertion helpers for the suites. Failures are collected rather than
 * thrown, so one run reports every broken check; a suite that aborts with an
 * exception is reported as a failure as well.
 */
final class TestSupport {
	@FunctionalInterface
	interface CheckedAction {
		void run() throws Exception;
	}

	private static int failed;
	private static int passed;

	private TestSupport() {
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
