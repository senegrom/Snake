package snake.gui;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import snake.Direction;
import snake.topology.Topology;

/** Main application window and entry point. */
public final class SnakeFrame {
	private final JButton aboutButton = new JButton("About");
	private final JButton exitButton = new JButton("Exit");
	private final SnakeField field = new SnakeField();
	private final JFrame frame = new JFrame("Snake");
	private final JButton pauseButton = new JButton("Pause");
	private final JLabel pointsLabel = new JLabel("Points 0");
	private final JButton restartButton = new JButton("Restart");
	private final JLabel speedLabel = new JLabel(Integer.toString(SnakeField.DEFAULT_SPEED));
	private final JSlider speedSlider = new JSlider(SwingConstants.HORIZONTAL, 1,
			SnakeField.MOVE_DELAYS_MS.size(), SnakeField.DEFAULT_SPEED);
	private final JButton startButton = new JButton("Start");
	private final JLabel timeLabel = new JLabel("Time 0:00");
	private final JComboBox<Topology> topologyBox = new JComboBox<>(Topology.values());

	private SnakeFrame() {
		configureControls();
		configureLayout();
		bindGameKeys();

		field.addPropertyChangeListener(SnakeField.POINTS_PROPERTY,
				event -> setPoints((Integer) event.getNewValue()));
		field.addPropertyChangeListener(SnakeField.TIME_PROPERTY,
				event -> setTime((Integer) event.getNewValue()));
		field.addPropertyChangeListener(SnakeField.FINISHED_PROPERTY, event -> gameFinished());
		field.addPropertyChangeListener(SnakeField.ERROR_PROPERTY,
				event -> showError((RuntimeException) event.getNewValue()));
		frame.setResizable(false);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
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

	private void gameFinished() {
		pauseButton.setEnabled(false);
		pauseButton.setText("Pause");
	}

	private void setPoints(final int points) {
		pointsLabel.setText("Points " + points);
	}

	private void setTime(final int totalSeconds) {
		timeLabel.setText("Time %d:%02d".formatted(totalSeconds / 60, totalSeconds % 60));
	}

	private void showMessage(final String text, final String title, final int type) {
		JOptionPane.showMessageDialog(frame, text, title, type);
	}

	private void bindGameKeys() {
		final InputMap inputMap = frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		final ActionMap actionMap = frame.getRootPane().getActionMap();
		bindSteer(inputMap, actionMap, KeyEvent.VK_RIGHT, Direction.RIGHT);
		bindSteer(inputMap, actionMap, KeyEvent.VK_DOWN, Direction.DOWN);
		bindSteer(inputMap, actionMap, KeyEvent.VK_LEFT, Direction.LEFT);
		bindSteer(inputMap, actionMap, KeyEvent.VK_UP, Direction.UP);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "pause");
		actionMap.put("pause", action(this::togglePause));
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
		pauseButton.setEnabled(false);

		startButton.addActionListener(event -> startGame());
		exitButton.addActionListener(event -> System.exit(0));
		restartButton.addActionListener(event -> restartGame());
		pauseButton.addActionListener(event -> togglePause());
		aboutButton.addActionListener(event -> showAbout());
		speedSlider.addChangeListener(event -> updateSpeed());
		topologyBox.addActionListener(event -> field.setTopology((Topology) topologyBox.getSelectedItem()));

		// Keep game keys independent of component-specific SPACE/arrow bindings.
		for (final JComponent component : new JComponent[] {
				startButton, exitButton, restartButton, pauseButton, aboutButton, speedSlider, topologyBox })
			component.setFocusable(false);
	}

	private void configureLayout() {
		final JPanel buttonRow = new JPanel(new GridLayout(1, 0));
		buttonRow.add(startButton);
		buttonRow.add(exitButton);
		buttonRow.add(restartButton);
		buttonRow.add(pauseButton);
		buttonRow.add(aboutButton);
		buttonRow.add(pointsLabel);
		buttonRow.add(timeLabel);

		final JPanel settingsRow = new JPanel(new FlowLayout());
		settingsRow.add(new JLabel("Speed"));
		settingsRow.add(speedLabel);
		settingsRow.add(speedSlider);
		settingsRow.add(topologyBox);

		final JPanel controls = new JPanel();
		controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
		controls.add(buttonRow);
		controls.add(settingsRow);

		final JPanel fieldWrapper = new JPanel(new GridBagLayout());
		fieldWrapper.add(field);

		frame.setLayout(new BorderLayout());
		frame.add(controls, BorderLayout.NORTH);
		frame.add(fieldWrapper, BorderLayout.CENTER);
	}

	private void restartGame() {
		field.shutdown();
		frame.dispose();
		new SnakeFrame();
	}

	private void showError(final RuntimeException cause) {
		showMessage("The game loop crashed:\n" + cause, "Error", JOptionPane.ERROR_MESSAGE);
	}

	private void showAbout() {
		final boolean resumeAfterDialog = field.pauseGame();
		showMessage("Snake by CGH.", "About", JOptionPane.INFORMATION_MESSAGE);
		if (resumeAfterDialog)
			field.resumeGame();
	}

	private void startGame() {
		field.startGame();
		startButton.setEnabled(false);
		pauseButton.setEnabled(true);
		speedSlider.setEnabled(false);
		topologyBox.setEnabled(false);
	}

	private void togglePause() {
		field.togglePause();
		pauseButton.setText(field.status() == SnakeField.Status.PAUSED ? "Resume" : "Pause");
	}

	private void updateSpeed() {
		final int speed = speedSlider.getValue();
		speedLabel.setText(Integer.toString(speed));
		field.setMoveDelay(SnakeField.MOVE_DELAYS_MS.get(speed - 1));
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
