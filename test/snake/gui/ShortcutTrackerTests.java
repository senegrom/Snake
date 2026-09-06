package snake.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.UIManager;
import static snake.gui.RobotSupport.activate;
import static snake.gui.RobotSupport.await;
import static snake.gui.RobotSupport.check;
import static snake.gui.RobotSupport.edt;
import static snake.gui.RobotSupport.focus;

/** Real press/release tests for shortcut ownership across component focus changes. */
public final class ShortcutTrackerTests {
	private final JFrame frame = new JFrame("Shortcut regression");
	private final JPanel board = new JPanel();
	private final JButton button = new JButton("Resume");
	private final JToggleButton settings = new JToggleButton("Settings");
	private final JComboBox<String> zoom = new JComboBox<>(new String[] { "100%", "150%", "Fit" });
	private final ShortcutTracker tracker = new ShortcutTracker(frame);
	private int shortcutActions;
	private int buttonActions;
	private int settingsActions;

	/** Created on the EDT; the test driver and Robot run outside it. */
	private ShortcutTrackerTests() {
		board.setFocusable(true);
		board.setPreferredSize(new Dimension(350, 160));
		final JPanel controls = new JPanel();
		controls.add(button);
		controls.add(settings);
		controls.add(zoom);
		frame.add(board, BorderLayout.CENTER);
		frame.add(controls, BorderLayout.SOUTH);
		button.addActionListener(event -> buttonActions++);
		settings.addActionListener(event -> settingsActions++);
		final InputMap input = frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		final ActionMap actions = frame.getRootPane().getActionMap();
		for (final int key : new int[] { KeyEvent.VK_SPACE, KeyEvent.VK_F2, KeyEvent.VK_F3 })
			tracker.bind(input, actions, key, "shortcut-" + key, () -> shortcutActions++);
		board.getInputMap(JComponent.WHEN_FOCUSED).setParent(input);
		board.getActionMap().setParent(actions);
		frame.addWindowFocusListener(new WindowAdapter() {
			@Override
			public void windowLostFocus(final WindowEvent event) {
				tracker.focusLost();
			}
		});
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setLocationRelativeTo(null);
		tracker.install();
	}

	public static void main(final String[] args) {
		int result = 0;
		Robot robot = null;
		ShortcutTrackerTests tests = null;
		try {
			robot = new Robot();
			robot.setAutoDelay(25);
			tests = edt(() -> {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				return new ShortcutTrackerTests();
			});
			activate(tests.frame);
			tests.run(robot);
			System.out.println("ShortcutTrackerTests: " + RobotSupport.checks() + " checks passed, 0 failed");
		} catch (final Exception | AssertionError failure) {
			result = 1;
			failure.printStackTrace();
		} finally {
			if (robot != null)
				for (final int key : new int[] { KeyEvent.VK_SPACE, KeyEvent.VK_F2, KeyEvent.VK_F3,
						KeyEvent.VK_TAB, KeyEvent.VK_ESCAPE, KeyEvent.VK_SHIFT })
					robot.keyRelease(key);
			final ShortcutTrackerTests current = tests;
			try {
				edt(() -> {
					if (current != null)
						current.tracker.uninstall();
					return null;
				});
				RobotSupport.disposeAllWindows();
			} catch (final Exception failure) {
				result = 1;
				failure.printStackTrace();
			}
		}
		System.exit(result);
	}

	private void run(final Robot robot) throws Exception {
		for (final JComponent target : new JComponent[] { button, settings, zoom })
			testShortcutToControl(robot, target);
		testButtonRelease(robot);
		testControlToBoard(robot);
		testTabTransfer(robot);
		testFreshControls(robot);
		testOtherShortcuts(robot);
	}

	private void testShortcutToControl(final Robot robot, final JComponent target) throws Exception {
		focus(board);
		final int shortcutsBefore = edt(() -> shortcutActions);
		final int buttonsBefore = edt(() -> buttonActions + settingsActions);
		robot.keyPress(KeyEvent.VK_SPACE);
		await(() -> shortcutActions == shortcutsBefore + 1, "board Space acts once");
		// This deliberate focus change reproduces the review's robustness case;
		// Tab navigation is tested separately, without forced focus changes.
		focus(target);
		Thread.sleep(800); // Allow native auto-repeat where it is enabled.
		repeatSpace(robot);
		check(edt(() -> !button.getModel().isPressed() && !settings.getModel().isPressed()
				&& !zoom.isPopupVisible()), "held shortcut cannot arm a newly focused control");
		robot.keyRelease(KeyEvent.VK_SPACE);
		robot.waitForIdle();
		check(edt(() -> shortcutActions == shortcutsBefore + 1 && buttonActions + settingsActions == buttonsBefore),
				"one physical Space press cannot act again on another control's release");
		check(edt(frame::isFocused), "the reproduction never leaves the game window");
	}

	private void testButtonRelease(final Robot robot) throws Exception {
		focus(button);
		final int buttonsBefore = edt(() -> buttonActions);
		final int shortcutsBefore = edt(() -> shortcutActions);
		robot.keyPress(KeyEvent.VK_SPACE);
		await(() -> button.getModel().isPressed(), "fresh Space still presses a focused button");
		repeatSpace(robot);
		check(edt(() -> buttonActions == buttonsBefore), "focused button waits for release");
		robot.keyRelease(KeyEvent.VK_SPACE);
		await(() -> buttonActions == buttonsBefore + 1 && !button.getModel().isPressed(),
				"focused button receives its release and activates exactly once");
		check(edt(() -> shortcutActions == shortcutsBefore), "focused button never also triggers the board action");
	}

	private void testControlToBoard(final Robot robot) throws Exception {
		focus(button);
		final int shortcutsBefore = edt(() -> shortcutActions);
		final int buttonsBefore = edt(() -> buttonActions);
		robot.keyPress(KeyEvent.VK_SPACE);
		await(() -> button.getModel().isPressed(), "control receives the original key press");
		focus(board);
		// Some look-and-feels commit the original button press on focus loss.
		// Preserve that behaviour, but do not let repeat add a second action.
		final int afterFocus = edt(() -> buttonActions);
		check(afterFocus >= buttonsBefore && afterFocus <= buttonsBefore + 1,
				"focus loss can act only on the original button press");
		repeatSpace(robot);
		robot.keyRelease(KeyEvent.VK_SPACE);
		robot.waitForIdle();
		check(edt(() -> shortcutActions == shortcutsBefore && buttonActions == afterFocus),
				"control press cannot become a second action on the board through repeat");
		RobotSupport.tap(robot, KeyEvent.VK_SPACE);
		await(() -> shortcutActions == shortcutsBefore + 1, "fresh board press works after control focus transfer");
	}

	private void testTabTransfer(final Robot robot) throws Exception {
		focus(board);
		final int shortcutsBefore = edt(() -> shortcutActions);
		final int buttonsBefore = edt(() -> buttonActions);
		robot.keyPress(KeyEvent.VK_SPACE);
		await(() -> shortcutActions == shortcutsBefore + 1, "pause before real Tab navigation");
		for (int tries = 0; tries < 20 && !edt(button::isFocusOwner); tries++)
			RobotSupport.tap(robot, KeyEvent.VK_TAB);
		check(edt(button::isFocusOwner), "Tab can reach a control while Space is held");
		repeatSpace(robot);
		robot.keyRelease(KeyEvent.VK_SPACE);
		robot.waitForIdle();
		check(edt(() -> shortcutActions == shortcutsBefore + 1 && buttonActions == buttonsBefore),
				"Tab transfer does not turn a held Space into a second action");
	}

	private void testFreshControls(final Robot robot) throws Exception {
		focus(settings);
		final boolean selected = edt(settings::isSelected);
		final int settingsBefore = edt(() -> settingsActions);
		RobotSupport.tap(robot, KeyEvent.VK_SPACE);
		await(() -> settings.isSelected() != selected && settingsActions == settingsBefore + 1,
				"fresh Space toggles Settings normally");
		focus(zoom);
		RobotSupport.tap(robot, KeyEvent.VK_SPACE);
		await(zoom::isPopupVisible, "fresh Space still opens the zoom selector");
		RobotSupport.tap(robot, KeyEvent.VK_DOWN);
		RobotSupport.tap(robot, KeyEvent.VK_ENTER);
		await(() -> !zoom.isPopupVisible() && zoom.getSelectedIndex() == 1, "zoom keeps normal keyboard selection");
		focus(board);
		final int shortcutsBefore = edt(() -> shortcutActions);
		robot.keyPress(KeyEvent.VK_SPACE);
		await(() -> shortcutActions == shortcutsBefore + 1, "shortcut fires before modified release");
		robot.keyPress(KeyEvent.VK_SHIFT);
		robot.keyRelease(KeyEvent.VK_SPACE);
		robot.keyRelease(KeyEvent.VK_SHIFT);
		RobotSupport.tap(robot, KeyEvent.VK_SPACE);
		await(() -> shortcutActions == shortcutsBefore + 2, "modified release still re-arms shortcuts");
	}

	private void testOtherShortcuts(final Robot robot) throws Exception {
		for (final int key : new int[] { KeyEvent.VK_F2, KeyEvent.VK_F3 }) {
			focus(board);
			final int before = edt(() -> shortcutActions);
			robot.keyPress(key);
			await(() -> shortcutActions == before + 1, "function-key shortcut fires initially");
			focus(button);
			robot.keyPress(key);
			robot.keyPress(key);
			robot.keyRelease(key);
			robot.waitForIdle();
			check(edt(() -> shortcutActions == before + 1), "held function key still fires once across focus changes");
			RobotSupport.tap(robot, key);
			await(() -> shortcutActions == before + 2, "released function key can fire again");
		}
	}

	private static void repeatSpace(final Robot robot) {
		// Deterministic repeat events also cover desktops with auto-repeat disabled.
		for (int repeat = 0; repeat < 3; repeat++) {
			robot.keyPress(KeyEvent.VK_SPACE);
			robot.delay(75);
		}
		robot.waitForIdle();
	}
}
