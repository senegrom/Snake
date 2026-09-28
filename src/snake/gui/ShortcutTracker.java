package snake.gui;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;

/**
 * One-shot game shortcuts: a held key acts once per physical press, whatever
 * auto-repeat delivers. A key that is down when the window loses focus stays
 * suppressed until a real release is observed, so returning with the key
 * still held cannot act again; if the outside release was never delivered,
 * one tap clears the latch. Presses in owned dialogs are also tracked so
 * their closing key cannot become a game shortcut after focus returns.
 * Releases are tracked in every application window. Presses are suppressed
 * while the game window is focused, and in owned dialogs only for a key
 * latched by the focus loss, so a key held while a dialog opens cannot act
 * in it. Repeated presses are stopped before they reach a different focused
 * control; a fresh control press still receives its normal release.
 */
final class ShortcutTracker {
	private final Window window;
	private final Map<Integer, HeldKeyAction> shortcuts = new HashMap<>();
	private final KeyEventDispatcher dispatcher = this::dispatch;

	ShortcutTracker(final Window window) {
		this.window = window;
	}

	/** Observes every key event in the application; returns true when the event is swallowed. */
	private boolean dispatch(final KeyEvent event) {
		final HeldKeyAction shortcut = shortcuts.get(event.getKeyCode());
		if (shortcut == null)
			return false;
		if (event.getID() == KeyEvent.KEY_RELEASED) {
			// An action already handled on press must not also activate a button on release.
			final boolean blocked = shortcut.pressed || shortcut.blockedUntilRelease;
			shortcut.release();
			if (blocked && window.isFocused()) {
				event.consume();
				return true;
			}
		} else if (event.getID() == KeyEvent.KEY_PRESSED && ownsFocusedWindow()) {
			// Suppress repeats before Swing can route them to a newly focused control,
			// even if the original press was handled by a button instead of our action.
			final boolean repeated = shortcut.keyDown;
			shortcut.keyDown = true;
			// Owned dialogs keep their native key handling, including the initial close,
			// but not a key held since before they took the focus: its repeats would
			// arm the dialog's focused button and its release fire it.
			if (shortcut.blockedUntilRelease || window.isFocused() && repeated) {
				event.consume();
				return true;
			}
		}
		return false;
	}

	/** Includes nested owned dialogs, but not unrelated windows in the same JVM. */
	private boolean ownsFocusedWindow() {
		return belongsTo(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow(), window);
	}

	/** Whether a window is the root itself or owned by it, directly or through nested dialogs; false for null. */
	static boolean belongsTo(final Window candidate, final Window root) {
		for (Window current = candidate; current != null; current = current.getOwner())
			if (current == root)
				return true;
		return false;
	}

	/** Starts observing key events for the whole application. */
	void install() {
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(dispatcher);
	}

	/** Stops observing key events. */
	void uninstall() {
		KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(dispatcher);
	}

	/** Binds a key to an action that fires once per physical press. */
	void bind(final InputMap inputMap, final ActionMap actionMap, final int keyCode, final String name,
			final Runnable action) {
		final HeldKeyAction press = new HeldKeyAction(action);
		shortcuts.put(keyCode, press);
		inputMap.put(KeyStroke.getKeyStroke(keyCode, 0, false), name);
		actionMap.put(name, press);
	}

	/** Focus loss is not a key release: auto-repeat may continue when focus returns. */
	void focusLost() {
		shortcuts.values().forEach(HeldKeyAction::focusLost);
	}

	/**
	 * Re-arms the keys whose release went to another window, but not a key
	 * latched by focus loss: that one stays suppressed until its own release.
	 */
	void releaseUnlatched() {
		shortcuts.values().forEach(HeldKeyAction::releaseUnlatched);
	}

	/** A held shortcut is one action, not a stream of toggles or restarts. */
	private static final class HeldKeyAction extends AbstractAction {
		private static final long serialVersionUID = 1L;
		private final transient Runnable runnable;
		private boolean pressed;
		private boolean keyDown;
		private boolean blockedUntilRelease;

		HeldKeyAction(final Runnable runnable) {
			this.runnable = runnable;
		}

		@Override
		public void actionPerformed(final ActionEvent event) {
			keyDown = true;
			if (!pressed && !blockedUntilRelease) {
				pressed = true;
				runnable.run();
			}
		}

		void focusLost() {
			// Only an observed release can re-arm this key. If an outside-app
			// release was missed, the next tap's release safely clears the latch.
			blockedUntilRelease |= keyDown;
		}

		void release() {
			pressed = false;
			keyDown = false;
			blockedUntilRelease = false;
		}

		void releaseUnlatched() {
			if (!blockedUntilRelease)
				release();
		}
	}
}
