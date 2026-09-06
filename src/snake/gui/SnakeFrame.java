package snake.gui;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import snake.Direction;
import snake.topology.Topology;

/** Main application window and entry point. */
public final class SnakeFrame {
	private static final String VERSION = "0.6.0";
	private static final String ABOUT_TEXT = "Snake " + VERSION + " by CGH.";

	private final JButton aboutButton = new JButton("About");
	private final JButton exitButton = new JButton("Exit");
	private SnakeField field;
	private Runnable afterAboutFocus;
	private final JScrollPane boardScroll = new JScrollPane();
	private final JPanel detailsPanel = new JPanel();
	private final JToggleButton settingsToggle = new JToggleButton("Settings", true);
	private final JFrame frame = new JFrame("Snake");
	private final ShortcutTracker shortcuts = new ShortcutTracker(frame);
	private final JButton pauseButton = new JButton("Pause");
	private final JLabel pointsLabel = new JLabel("Points 0");
	private final JButton restartButton = new JButton("Restart");
	private final JLabel speedLabel = new JLabel(Integer.toString(SnakeField.DEFAULT_SPEED));
	private final JSlider speedSlider = new JSlider(SwingConstants.HORIZONTAL, 1,
			SnakeField.MOVE_DELAYS_MS.size(), SnakeField.DEFAULT_SPEED);
	private final JButton startButton = new JButton("Start");
	private final JLabel timeLabel = new JLabel("Time 0:00");
	private final JComboBox<Topology> topologyBox = new JComboBox<>(Topology.PRESETS.toArray(Topology[]::new));
	private final JLabel topologyDescription = new JLabel();
	private final JComboBox<String> zoomBox = new JComboBox<>(SnakeField.ZOOM_LEVELS.stream()
			.map(percent -> percent + "%").toArray(String[]::new));

	private SnakeFrame() {
		configureControls();
		configureLayout();
		bindGameKeys();
		shortcuts.install();
		attachField(new SnakeField());

		frame.addWindowFocusListener(new WindowAdapter() {
			@Override
			public void windowGainedFocus(final WindowEvent event) {
				restoreAboutFocus();
			}

			@Override
			public void windowLostFocus(final WindowEvent event) {
				pauseForFocusLoss();
			}
		});
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowIconified(final WindowEvent event) {
				pauseForFocusLoss();
			}

			@Override
			public void windowClosed(final WindowEvent event) {
				shortcuts.uninstall();
				field.shutdown();
			}
		});
		frame.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(final ComponentEvent event) {
				revealHead();
			}
		});
		frame.setResizable(false);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		packWindow();
		frame.setVisible(true);
		startButton.requestFocusInWindow();
	}

	public static void main(final String[] args) {
		EventQueue.invokeLater(() -> {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (final ReflectiveOperationException | UnsupportedLookAndFeelException exception) {
				exception.printStackTrace();
			}
			new SnakeFrame();
		});
	}

	/** Installs a field and applies the selected speed, topology and view scale. */
	private void attachField(final SnakeField newField) {
		field = newField;
		// Board keys take precedence over the surrounding scroll pane's arrow keys.
		field.getInputMap(JComponent.WHEN_FOCUSED)
				.setParent(frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW));
		field.getActionMap().setParent(frame.getRootPane().getActionMap());
		field.addPropertyChangeListener(SnakeField.POINTS_PROPERTY,
				event -> setPoints((Integer) event.getNewValue()));
		field.addPropertyChangeListener(SnakeField.TIME_PROPERTY,
				event -> setTime((Integer) event.getNewValue()));
		field.addPropertyChangeListener(SnakeField.STATUS_PROPERTY, event -> updateControls());
		field.addPropertyChangeListener(SnakeField.ERROR_PROPERTY,
				event -> showError((RuntimeException) event.getNewValue()));
		boardScroll.setViewportView(field);
		field.setMoveDelay(SnakeField.MOVE_DELAYS_MS.get(speedSlider.getValue() - 1));
		applyZoom();
		updateTopology();
		updateControls();
	}

	private void updateControls() {
		final SnakeField.Status status = field.status();
		final boolean ready = status == SnakeField.Status.READY;
		startButton.setEnabled(ready);
		speedSlider.setEnabled(ready);
		topologyBox.setEnabled(ready);
		pauseButton.setEnabled(status == SnakeField.Status.RUNNING || status == SnakeField.Status.PAUSED);
		pauseButton.setText(status == SnakeField.Status.PAUSED ? "Resume" : "Pause");
	}

	private void setPoints(final int points) {
		pointsLabel.setText("Points " + points);
	}

	private void setTime(final int totalSeconds) {
		timeLabel.setText("Time %d:%02d".formatted(totalSeconds / 60, totalSeconds % 60));
	}

	private void bindGameKeys() {
		final InputMap inputMap = frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		final ActionMap actionMap = frame.getRootPane().getActionMap();
		bindSteer(inputMap, actionMap, KeyEvent.VK_RIGHT, Direction.RIGHT);
		bindSteer(inputMap, actionMap, KeyEvent.VK_DOWN, Direction.DOWN);
		bindSteer(inputMap, actionMap, KeyEvent.VK_LEFT, Direction.LEFT);
		bindSteer(inputMap, actionMap, KeyEvent.VK_UP, Direction.UP);
		shortcuts.bind(inputMap, actionMap, KeyEvent.VK_SPACE, "pause", this::togglePause);
		shortcuts.bind(inputMap, actionMap, KeyEvent.VK_F2, "start", this::startGame);
		shortcuts.bind(inputMap, actionMap, KeyEvent.VK_F3, "restart", this::restartGame);
		shortcuts.bind(inputMap, actionMap, KeyEvent.VK_ESCAPE, "pause-only", () -> field.pauseGame());
	}

	private void bindSteer(final InputMap inputMap, final ActionMap actionMap, final int keyCode,
			final Direction direction) {
		final String actionKey = "steer-" + direction;
		inputMap.put(KeyStroke.getKeyStroke(keyCode, 0), actionKey);
		actionMap.put(actionKey, action(() -> field.requestDirection(direction)));
	}

	private void configureControls() {
		speedSlider.setPaintTicks(true);
		speedSlider.setMinorTickSpacing(1);
		speedSlider.setSnapToTicks(true);
		speedSlider.setName("speed");
		topologyBox.setName("topology");
		zoomBox.setName("zoom");
		zoomBox.addItem("Fit");
		zoomBox.setSelectedItem("Fit");
		zoomBox.setToolTipText("Fit shows the whole board; fixed zoom may require scrolling");
		settingsToggle.setName("settings");
		settingsToggle.setToolTipText("Show or hide setup controls and help");
		settingsToggle.addActionListener(event -> {
			setSettingsVisible(settingsToggle.isSelected());
			if (field.status() == SnakeField.Status.RUNNING)
				field.requestFocusInWindow();
		});
		startButton.setToolTipText("Start a new game (F2)");
		restartButton.setToolTipText("Reset the game, keeping settings (F3)");
		pauseButton.setToolTipText("Pause or resume (Space on the board); Esc pauses");
		startButton.addActionListener(event -> startGame());
		exitButton.addActionListener(event -> frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING)));
		restartButton.addActionListener(event -> restartGame());
		pauseButton.addActionListener(event -> togglePause());
		aboutButton.addActionListener(event -> showAbout());
		speedSlider.addChangeListener(event -> updateSpeed());
		topologyBox.addActionListener(event -> updateTopology());
		zoomBox.addActionListener(event -> {
			applyZoom();
			packWindow();
			if (field.status() == SnakeField.Status.RUNNING)
				field.requestFocusInWindow();
		});
		// Keep the normal focus/Tab behaviour. Focused settings handle their own
		// arrows; starting, pausing and restarting return steering to the board.
	}

	private void configureLayout() {
		final JPanel buttonRow = new JPanel(new GridLayout(1, 0, 4, 0));
		buttonRow.add(startButton);
		buttonRow.add(exitButton);
		buttonRow.add(restartButton);
		buttonRow.add(pauseButton);
		buttonRow.add(aboutButton);
		// Status labels must not squeeze the five buttons into truncated captions.
		final JPanel statusRow = new JPanel(new GridLayout(1, 2, 4, 0));
		statusRow.add(pointsLabel);
		statusRow.add(timeLabel);

		// Explicit rows have a height independent of the screen width. FlowLayout
		// wrapped the zoom selector into a second line without reserving its height.
		final JPanel settings = new JPanel(new GridBagLayout());
		final JPanel speedControl = new JPanel(new BorderLayout(6, 0));
		speedControl.add(speedLabel, BorderLayout.WEST);
		speedControl.add(speedSlider, BorderLayout.CENTER);
		addSetting(settings, 0, labelFor("Speed", speedSlider), speedControl);
		addSetting(settings, 1, labelFor("Topology", topologyBox), topologyBox);
		final JPanel zoomRow = new JPanel(new GridBagLayout());
		final JPanel zoomControl = new JPanel(new BorderLayout(8, 0));
		zoomControl.add(zoomBox, BorderLayout.CENTER);
		zoomControl.add(settingsToggle, BorderLayout.EAST);
		addSetting(zoomRow, 0, labelFor("Zoom", zoomBox), zoomControl);

		final JPanel help = new JPanel(new GridLayout(0, 1, 0, 4));
		help.add(topologyDescription);
		help.add(new JLabel("<html>Arrows: steer · Space: pause/resume · F2: start · F3: restart"
				+ "<br>Tab: controls · Esc: pause · Click the board to return to steering</html>"));
		final JPanel controls = new JPanel();
		controls.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
		controls.add(buttonRow);
		controls.add(statusRow);
		detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
		detailsPanel.add(settings);
		detailsPanel.add(help);
		controls.add(detailsPanel);
		controls.add(zoomRow);

		boardScroll.setBorder(BorderFactory.createEmptyBorder());
		boardScroll.getVerticalScrollBar().setUnitIncrement(20);
		boardScroll.getHorizontalScrollBar().setUnitIncrement(20);
		frame.setLayout(new BorderLayout());
		frame.add(controls, BorderLayout.NORTH);
		frame.add(boardScroll, BorderLayout.CENTER);
	}

	private static void addSetting(final JPanel settings, final int row, final JLabel label,
			final JComponent control) {
		final GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridy = row;
		constraints.anchor = GridBagConstraints.LINE_START;
		constraints.insets = new Insets(3, 0, 3, 8);
		constraints.gridx = 0;
		settings.add(label, constraints);
		constraints.gridx = 1;
		constraints.weightx = 1;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.insets = new Insets(3, 0, 3, 0);
		settings.add(control, constraints);
	}

	private static JLabel labelFor(final String text, final JComponent control) {
		final JLabel label = new JLabel(text);
		label.setLabelFor(control);
		control.getAccessibleContext().setAccessibleName(text);
		return label;
	}

	/** Keeps the enlarged board reachable even on a small display. */
	private void packWindow() {
		WindowGeometry.placeWithinWorkArea(frame);
		revealHead();
	}

	/** Replaces the field in place, keeping the window and all settings. */
	private void restartGame() {
		field.shutdown();
		settingsToggle.setSelected(true);
		detailsPanel.setVisible(true);
		attachField(new SnakeField());
		setPoints(0);
		setTime(0);
		revealHead();
		field.requestFocusInWindow();
	}

	private void applyZoom() {
		final int index = zoomBox.getSelectedIndex();
		if (index == SnakeField.ZOOM_LEVELS.size())
			field.setFitToWindow(true);
		else
			field.setZoom(SnakeField.ZOOM_LEVELS.get(index));
	}

	private void setSettingsVisible(final boolean visible) {
		settingsToggle.setSelected(visible);
		detailsPanel.setVisible(visible);
		revealHead();
	}

	/** Re-layout before scrolling; repeat after queued layout/native resize events. */
	private void revealHead() {
		if (field == null || !frame.isDisplayable())
			return;
		frame.validate();
		field.scrollRectToVisible(field.headBounds());
		final SnakeField current = field;
		EventQueue.invokeLater(() -> {
			if (frame.isDisplayable() && field == current) {
				frame.validate();
				current.scrollRectToVisible(current.headBounds());
			}
		});
	}

	private void showError(final RuntimeException cause) {
		JOptionPane.showMessageDialog(frame, "The game loop crashed:\n" + cause, "Error", JOptionPane.ERROR_MESSAGE);
	}

	private void pauseForFocusLoss() {
		afterAboutFocus = null;
		shortcuts.focusLost();
		field.pauseGame();
	}

	private boolean isGameWindow(final Window window) {
		for (Window current = window; current != null; current = current.getOwner())
			if (current == frame)
				return true;
		return false;
	}

	private void showAbout() {
		final SnakeField dialogField = field;
		final boolean resumeAfterDialog = field.pauseGame();
		final boolean[] leftApplication = { false };
		final boolean[] restoreFocus = { false };
		final JDialog dialog = new JOptionPane(ABOUT_TEXT, JOptionPane.INFORMATION_MESSAGE)
				.createDialog(frame, "About");
		dialog.addWindowFocusListener(new WindowAdapter() {
			@Override
			public void windowGainedFocus(final WindowEvent event) {
				restoreFocus[0] = true;
			}

			@Override
			public void windowLostFocus(final WindowEvent event) {
				if (dialog.isVisible()) {
					restoreFocus[0] = isGameWindow(event.getOppositeWindow());
					if (!restoreFocus[0])
						leftApplication[0] = true;
				}
			}
		});
		try {
			dialog.setVisible(true);
		} finally {
			dialog.dispose();
		}
		// Return focus when the dialog was closed here, but never steal it from
		// another application. Leaving the application cancels automatic resume.
		// Focus events may arrive before or after the modal event loop returns.
		if (field == dialogField && restoreFocus[0] && frame.isDisplayable()) {
			afterAboutFocus = () -> {
				if (field == dialogField) {
					if (resumeAfterDialog && !leftApplication[0])
						field.resumeGame();
					field.requestFocusInWindow();
				}
			};
			frame.requestFocus();
			restoreAboutFocus();
		}
	}

	private void restoreAboutFocus() {
		if (frame.isFocused() && afterAboutFocus != null) {
			final Runnable restore = afterAboutFocus;
			afterAboutFocus = null;
			restore.run();
		}
	}

	private void startGame() {
		if (field.status() != SnakeField.Status.READY)
			return;
		setSettingsVisible(false);
		revealHead();
		field.startGame();
		field.requestFocusInWindow();
	}

	private void togglePause() {
		revealHead();
		field.togglePause();
		field.requestFocusInWindow();
	}

	// Speed and topology are locked once a game starts; a stale change event
	// must update only the labels rather than throw from inside a listener.
	private void updateSpeed() {
		final int speed = speedSlider.getValue();
		speedLabel.setText(Integer.toString(speed));
		if (field.status() == SnakeField.Status.READY)
			field.setMoveDelay(SnakeField.MOVE_DELAYS_MS.get(speed - 1));
	}

	private void updateTopology() {
		final Topology topology = (Topology) topologyBox.getSelectedItem();
		if (field.status() == SnakeField.Status.READY)
			field.setTopology(topology);
		topologyDescription.setText("<html>" + topology.description().replace("; ", ";<br>") + "</html>");
		topologyBox.setToolTipText(topology.description());
	}

	private static AbstractAction action(final Runnable runnable) {
		return new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(final ActionEvent event) {
				runnable.run();
			}
		};
	}
}
