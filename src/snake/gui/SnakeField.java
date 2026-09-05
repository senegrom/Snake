package snake.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongSupplier;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;
import javax.swing.JPanel;
import javax.swing.Timer;
import snake.Direction;
import snake.Position;
import snake.Snake;
import snake.topology.Topology;

/** Game state, timer and renderer. All live mutation occurs on the Swing EDT. */
@SuppressWarnings("serial")
final class SnakeField extends JPanel {
	enum Status {
		READY, RUNNING, PAUSED, FINISHED
	}

	static final int BOARD_COLUMNS = 41;
	static final int BOARD_ROWS = 31;
	static final int CELL_COUNT = BOARD_COLUMNS * BOARD_ROWS;
	static final int DEFAULT_SPEED = 1;
	/** Milliseconds between steps for speed levels 1 to 9. */
	static final List<Integer> MOVE_DELAYS_MS = List.of(200, 160, 120, 80, 50, 30, 20, 14, 10);
	static final String ERROR_PROPERTY = "gameError";
	static final String FINISHED_PROPERTY = "gameFinished";
	static final String POINTS_PROPERTY = "points";
	static final String STATUS_PROPERTY = "status";
	static final List<Integer> ZOOM_LEVELS = List.of(100, 150, 200);
	static final String TIME_PROPERTY = "elapsedSeconds";

	private static final List<Position> BOARD_CELLS = IntStream.range(0, CELL_COUNT)
			.mapToObj(index -> new Position(index % BOARD_COLUMNS, index / BOARD_COLUMNS))
			.toList();
	private static final List<Position> DEFAULT_BODY = List.of(
			new Position(BOARD_COLUMNS / 2, BOARD_ROWS / 2),
			new Position(BOARD_COLUMNS / 2 - 1, BOARD_ROWS / 2),
			new Position(BOARD_COLUMNS / 2 - 2, BOARD_ROWS / 2),
			new Position(BOARD_COLUMNS / 2 - 3, BOARD_ROWS / 2));

	private Position apple;
	private long elapsedBeforeRunNanos;
	private int displayedSeconds = -1;
	private final Timer moveTimer;
	private final LongSupplier nanoTime;
	private final BoardPainter painter = new BoardPainter();
	private long runStartedNanos;
	private final Snake snake;
	private final int startLength;
	private Status status = Status.READY;
	private Topology topology = Topology.PLANE;
	private boolean won;
	private int zoom = 100;

	SnakeField() {
		this(new Snake(Direction.RIGHT, DEFAULT_BODY));
	}

	private SnakeField(final Snake snake) {
		this(snake, spawnApple(snake));
	}

	SnakeField(final Snake snake, final Position apple) {
		this(snake, apple, System::nanoTime);
	}

	SnakeField(final Snake snake, final Position apple, final LongSupplier nanoTime) {
		this.snake = Objects.requireNonNull(snake, "snake");
		this.apple = Objects.requireNonNull(apple, "apple");
		this.nanoTime = Objects.requireNonNull(nanoTime, "nanoTime");
		validateBoardState(snake, apple);
		startLength = snake.length();

		setBackground(BoardPainter.MARGIN_COLOR);
		setPreferredSize(new Dimension(BoardPainter.PANEL_SIZE));
		setFocusable(true);
		getAccessibleContext().setAccessibleName("Snake board");
		getAccessibleContext().setAccessibleDescription("Arrow keys steer; Space pauses or resumes.");
		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(final MouseEvent event) {
				requestFocusInWindow();
			}
		});

		moveTimer = new Timer(MOVE_DELAYS_MS.get(DEFAULT_SPEED - 1), event -> onTimerTick());
	}

	Position apple() {
		return apple;
	}

	int moveDelay() {
		assert moveTimer.getDelay() == moveTimer.getInitialDelay() : "Timer delays diverged";
		return moveTimer.getDelay();
	}

	boolean requestDirection(final Direction direction) {
		final Direction previous = snake.direction();
		final boolean accepted = status != Status.FINISHED && snake.requestDirection(direction);
		if (accepted && previous != snake.direction())
			repaint();
		return accepted;
	}

	Snake snake() {
		return snake;
	}

	Status status() {
		assert (status == Status.RUNNING) == moveTimer.isRunning() : "Timer and status diverged";
		return status;
	}

	Topology topology() {
		return topology;
	}

	int elapsedSeconds() {
		return Math.toIntExact(elapsedNanos() / 1_000_000_000L);
	}

	boolean won() {
		return won;
	}

	String endMessage() {
		return won() ? "You Win!" : "Game Over";
	}

	Color endMessageColor() {
		return won() ? BoardPainter.WIN_COLOR : BoardPainter.APPLE_COLOR;
	}

	void startGame() {
		if (status != Status.READY)
			throw new IllegalStateException("The game can only be started once");
		runStartedNanos = nanoTime.getAsLong();
		moveTimer.start();
		setStatus(Status.RUNNING);
	}

	boolean pauseGame() {
		if (status != Status.RUNNING)
			return false;
		captureElapsedTime();
		moveTimer.stop();
		setStatus(Status.PAUSED);
		updateElapsedDisplay();
		return true;
	}

	boolean resumeGame() {
		if (status != Status.PAUSED)
			return false;
		runStartedNanos = nanoTime.getAsLong();
		moveTimer.start();
		setStatus(Status.RUNNING);
		return true;
	}

	void togglePause() {
		if (!pauseGame())
			resumeGame();
	}

	void shutdown() {
		if (status == Status.RUNNING)
			captureElapsedTime();
		moveTimer.stop();
		setStatus(Status.FINISHED);
	}

	/** Performs exactly one deterministic game step for the timer and tests. */
	void step() {
		if (status == Status.FINISHED)
			return;

		final Position next = topology.map(snake.direction().move(snake.head()), BOARD_COLUMNS, BOARD_ROWS);
		if (next == null) {
			endGame();
			return;
		}

		final boolean growing = next.equals(apple);
		if (!snake.advanceTo(next, growing)) {
			endGame();
			return;
		}
		if (growing) {
			if (snake.length() == CELL_COUNT) {
				won = true;
				apple = null;
				endGame();
			} else {
				apple = spawnApple(snake);
			}
		}

		if (growing) {
			final int points = snake.length() - startLength;
			firePropertyChange(POINTS_PROPERTY, points - 1, points);
		}
		repaint();
	}

	void setMoveDelay(final int moveDelayMillis) {
		if (moveDelayMillis <= 0)
			throw new IllegalArgumentException("Move delay must be positive");
		if (status != Status.READY)
			throw new IllegalStateException("Move speed cannot change after the game starts");
		moveTimer.setDelay(moveDelayMillis);
		moveTimer.setInitialDelay(moveDelayMillis);
	}

	void setTopology(final Topology topology) {
		if (status != Status.READY)
			throw new IllegalStateException("Topology cannot change after the game starts");
		this.topology = Objects.requireNonNull(topology, "topology");
		repaint();
	}

	int zoom() {
		return zoom;
	}

	/** Changes only the view; game coordinates and the timer are unaffected. */
	void setZoom(final int percent) {
		if (!ZOOM_LEVELS.contains(percent))
			throw new IllegalArgumentException("Unsupported zoom: " + percent);
		zoom = percent;
		setPreferredSize(new Dimension(BoardPainter.PANEL_SIZE.width * percent / 100,
				BoardPainter.PANEL_SIZE.height * percent / 100));
		revalidate();
		repaint();
	}

	String overlayMessage() {
		return switch (status) {
		case READY -> "Ready";
		case PAUSED -> "Paused";
		case RUNNING -> null;
		case FINISHED -> endMessage();
		};
	}

	private void setStatus(final Status next) {
		final Status previous = status;
		status = next;
		if (previous != next) {
			firePropertyChange(STATUS_PROPERTY, previous, next);
			repaint();
		}
	}

	@Override
	protected void paintComponent(final Graphics graphics) {
		super.paintComponent(graphics);
		final Graphics2D g = (Graphics2D) graphics.create();
		try {
			g.scale(zoom / 100.0, zoom / 100.0);
			painter.paint(g, BoardPainter.PANEL_SIZE.width, BoardPainter.PANEL_SIZE.height,
					snake, apple, topology, overlayMessage(),
					status == Status.FINISHED ? endMessageColor() : Color.DARK_GRAY, status == Status.FINISHED);
		} finally {
			g.dispose();
		}
	}

	private void abortGame(final RuntimeException cause) {
		endGame();
		firePropertyChange(ERROR_PROPERTY, null, cause);
	}

	private void captureElapsedTime() {
		elapsedBeforeRunNanos += nanoTime.getAsLong() - runStartedNanos;
	}

	private void endGame() {
		if (status == Status.RUNNING)
			captureElapsedTime();
		moveTimer.stop();
		setStatus(Status.FINISHED);
		updateElapsedDisplay();
		firePropertyChange(FINISHED_PROPERTY, false, true);
		repaint();
	}

	private long elapsedNanos() {
		return elapsedBeforeRunNanos
				+ (status == Status.RUNNING ? nanoTime.getAsLong() - runStartedNanos : 0L);
	}

	private static boolean isInsideBoard(final Position position) {
		return position.x() >= 0 && position.x() < BOARD_COLUMNS
				&& position.y() >= 0 && position.y() < BOARD_ROWS;
	}

	private void onTimerTick() {
		if (status != Status.RUNNING)
			return;
		try {
			step();
			if (status == Status.RUNNING)
				updateElapsedDisplay();
		} catch (final RuntimeException cause) {
			abortGame(cause);
		}
	}

	private static Position spawnApple(final Snake snake) {
		return spawnApple(snake, ThreadLocalRandom.current());
	}

	static Position spawnApple(final Snake snake, final RandomGenerator random) {
		Objects.requireNonNull(snake, "snake");
		Objects.requireNonNull(random, "random");
		final int freeCells = CELL_COUNT - snake.length();
		if (freeCells <= 0)
			return null;

		int selectedFreeCell = random.nextInt(freeCells);
		for (final Position candidate : BOARD_CELLS)
			if (!snake.contains(candidate) && selectedFreeCell-- == 0)
				return candidate;
		throw new IllegalStateException("Snake occupancy is inconsistent with its length");
	}

	private void updateElapsedDisplay() {
		final int seconds = elapsedSeconds();
		final int previousSeconds = displayedSeconds;
		displayedSeconds = seconds;
		firePropertyChange(TIME_PROPERTY, previousSeconds, seconds);
	}

	private static void validateBoardState(final Snake snake, final Position apple) {
		if (snake.length() >= CELL_COUNT)
			throw new IllegalArgumentException("A playable board must contain at least one free cell");
		for (final Position position : snake.body())
			if (!isInsideBoard(position))
				throw new IllegalArgumentException("Snake position is outside the board: " + position);
		if (!isInsideBoard(apple))
			throw new IllegalArgumentException("Apple is outside the board: " + apple);
		if (snake.contains(apple))
			throw new IllegalArgumentException("Apple overlaps the snake: " + apple);
	}
}
