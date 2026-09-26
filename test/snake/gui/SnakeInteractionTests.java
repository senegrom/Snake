package snake.gui;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.RepaintManager;
import snake.Direction;
import snake.Position;
import snake.Snake;
import snake.topology.Gluing;
import snake.topology.Topology;
import static snake.gui.TestSupport.check;
import static snake.gui.TestSupport.equal;
import static snake.gui.TestSupport.expect;
import static snake.gui.TestSupport.isBluish;

/** Deterministic regression coverage for view and lifecycle notifications. */
public final class SnakeInteractionTests {
	/** The fixture snake lies along the left edge in this row, head at column 2. */
	private static final int FIXTURE_ROW = 5;

	private SnakeInteractionTests() {
	}

	public static void main(final String[] args) {
		System.exit(TestSupport.run("SnakeInteractionTests", () -> {
			testDirectionRepainting();
			testStatusNotifications();
			testOverlayKeepsWallsVisible();
			testZoomAndTopologyHints();
			testMarginMatchesGluing();
		}));
	}

	private static SnakeField fixture(final Topology topology) {
		final SnakeField field = new SnakeField(new Snake(Direction.RIGHT, List.of(new Position(2, FIXTURE_ROW),
				new Position(1, FIXTURE_ROW), new Position(0, FIXTURE_ROW))), new Position(10, 10));
		field.setTopology(topology);
		field.setMoveDelay(100_000);
		return field;
	}

	private static void testDirectionRepainting() {
		final SnakeField field = fixture(Topology.TORUS);
		final RepaintManager previous = RepaintManager.currentManager(field);
		final CountingRepaints repaints = new CountingRepaints();
		RepaintManager.setCurrentManager(repaints);
		try {
			check(field.requestDirection(Direction.UP), "ready direction change accepted");
			equal(1, repaints.count, "ready direction change repaints immediately");
			check(field.requestDirection(Direction.UP), "repeated direction remains accepted");
			check(!field.requestDirection(Direction.LEFT), "reversal rejected");
			equal(1, repaints.count, "unchanged and rejected directions do not repaint");
			field.startGame();
			field.pauseGame();
			final List<Position> body = List.copyOf(field.snake().body());
			repaints.count = 0;
			check(field.requestDirection(Direction.DOWN), "paused turn accepted");
			equal(1, repaints.count, "paused turn repaints immediately");
			final BufferedImage image = render(field);
			final Point head = BoardPainter.cellCenter(field.snake().head());
			equal(BoardPainter.EYE_COLOR.getRGB(), image.getRGB(head.x - 1, head.y + 1),
					"paused eye displays the queued direction");
			equal(body, List.copyOf(field.snake().body()), "turn and repaint do not move the snake");
			field.shutdown();
			repaints.count = 0;
			check(!field.requestDirection(Direction.UP), "finished direction rejected");
			equal(0, repaints.count, "finished input does not repaint");
		} finally {
			field.shutdown();
			RepaintManager.setCurrentManager(previous);
		}
	}

	private static void testStatusNotifications() {
		final SnakeField centred = new SnakeField();
		centred.startGame();
		centred.pauseGame();
		centred.requestDirection(Direction.UP);
		final Point head = BoardPainter.cellCenter(centred.snake().head());
		equal(BoardPainter.EYE_COLOR.getRGB(), render(centred).getRGB(head.x - 1, head.y - 3),
				"pause banner does not hide the central snake's direction indicator");
		centred.shutdown();
		final SnakeField field = fixture(Topology.TORUS);
		final List<SnakeField.Status> statuses = new ArrayList<>();
		field.addPropertyChangeListener(SnakeField.STATUS_PROPERTY, event -> {
			equal(event.getNewValue(), field.status(), "status event observes a consistent timer and state");
			statuses.add(field.status());
		});
		equal("Ready", field.overlayMessage(), "ready state has an overlay");
		final BufferedImage ready = render(field);
		field.startGame();
		equal(null, field.overlayMessage(), "running state has no overlay");
		final BufferedImage running = render(field);
		check(differentOverlay(ready, running), "ready overlay actually paints");
		field.pauseGame();
		equal("Paused", field.overlayMessage(), "paused state has an overlay");
		check(differentOverlay(render(field), running), "pause overlay actually paints");
		field.pauseGame();
		field.resumeGame();
		field.resumeGame();
		field.shutdown();
		field.shutdown();
		equal(List.of(SnakeField.Status.RUNNING, SnakeField.Status.PAUSED,
				SnakeField.Status.RUNNING, SnakeField.Status.FINISHED), statuses,
				"only actual lifecycle changes publish status events");
		equal("Game Over", field.overlayMessage(), "terminal message preserved");
	}

	private static void testOverlayKeepsWallsVisible() {
		final SnakeField field = fixture(Topology.PLANE);
		try {
			for (final int zoom : SnakeField.ZOOM_LEVELS) {
				field.setZoom(zoom);
				check(topWallVisible(render(field), zoom), "ready banner leaves the full top wall visible");
			}
			field.startGame();
			field.pauseGame();
			for (final int zoom : SnakeField.ZOOM_LEVELS) {
				field.setZoom(zoom);
				check(topWallVisible(render(field), zoom), "pause banner leaves the full top wall visible");
			}
		} finally {
			field.shutdown();
		}
	}

	private static boolean topWallVisible(final BufferedImage image, final int zoom) {
		final int left = BoardPainter.BOARD_X * zoom / 100;
		final int right = (BoardPainter.BOARD_X + BoardPainter.BOARD_WIDTH) * zoom / 100;
		final int top = (BoardPainter.BOARD_Y - BoardPainter.WALL_THICKNESS) * zoom / 100;
		final int bottom = BoardPainter.BOARD_Y * zoom / 100;
		for (int y = top; y < bottom; y++)
			for (int x = left; x < right; x++)
				if (image.getRGB(x, y) != BoardPainter.WALL_COLOR.getRGB())
					return false;
		return true;
	}

	private static void testZoomAndTopologyHints() {
		for (final Gluing horizontal : Gluing.values()) {
			for (final Gluing vertical : Gluing.values()) {
				final Topology topology = new Topology(horizontal, vertical);
				final String hint = topology.description();
				check(hint.startsWith("Left/right edges ") && hint.contains("; top/bottom edges "),
						"hint describes both edge pairs");
				equal(horizontal == Gluing.FLIP, hint.contains("reflect rows"), "horizontal flip hint");
				equal(vertical == Gluing.FLIP, hint.contains("reflect columns"), "vertical flip hint");
				final SnakeField field = fixture(topology);
				final List<Position> body = List.copyOf(field.snake().body());
				// A body cell, and where its copy in the right margin appears: the same
				// row, or the mirrored row across a flipped edge
				final Point bodyCell = BoardPainter.cellCenter(new Position(1, FIXTURE_ROW));
				final Point copyCell = BoardPainter.cellCenter(new Position(1,
						horizontal == Gluing.FLIP ? SnakeField.BOARD_ROWS - 1 - FIXTURE_ROW : FIXTURE_ROW));
				copyCell.translate(BoardPainter.BOARD_WIDTH, 0);
				for (final int zoom : SnakeField.ZOOM_LEVELS) {
					field.setZoom(zoom);
					equal(zoom, field.zoom(), "zoom accessor");
					equal(new Dimension(BoardPainter.PANEL_SIZE.width * zoom / 100,
							BoardPainter.PANEL_SIZE.height * zoom / 100), field.getPreferredSize(),
							"zoom scales the board and margins");
					final BufferedImage image = render(field);
					check(blueAt(image, bodyCell, zoom), "scaled board preserves snake cell positions");
					equal(horizontal != Gluing.WALL, blueAt(image, copyCell, zoom),
							"scaled neighbour uses the correct reflection");
					equal(body, List.copyOf(field.snake().body()), "zoom cannot change game coordinates");
					equal(100_000, field.moveDelay(), "zoom cannot change speed");
				}
				expect(IllegalArgumentException.class, () -> field.setZoom(0), "zero zoom rejected");
				expect(IllegalArgumentException.class, () -> field.setZoom(300), "unsupported zoom rejected");
				field.startGame();
				field.setZoom(100);
				equal(SnakeField.Status.RUNNING, field.status(), "zoom does not interrupt a running game");
				field.shutdown();
			}
		}
	}

	/**
	 * Every margin cell of every gluing, corners included, copies the board cell
	 * that a walk across the glued edges reaches, at every fixed zoom: blue
	 * exactly where that cell belongs to a random body, solid wall margin beyond
	 * a wall. Corner cells are reached both ways round, which must agree.
	 */
	private static void testMarginMatchesGluing() {
		final int columns = SnakeField.BOARD_COLUMNS;
		final int rows = SnakeField.BOARD_ROWS;
		final int margin = BoardPainter.BOARD_X / BoardPainter.CELL_SIZE;
		final Random random = new Random(12345);
		for (final Gluing horizontal : Gluing.values()) {
			for (final Gluing vertical : Gluing.values()) {
				final Topology topology = new Topology(horizontal, vertical);
				final Set<Position> body = new LinkedHashSet<>();
				body.add(new Position(columns / 2, rows / 2));
				for (int y = 0; y < rows; y++)
					for (int x = 0; x < columns; x++)
						if ((x < margin || x >= columns - margin || y < margin || y >= rows - margin)
								&& random.nextBoolean())
							body.add(new Position(x, y));
				final SnakeField field = new SnakeField(new Snake(Direction.RIGHT, List.copyOf(body)),
						new Position(columns / 2 + 2, rows / 2));
				field.setTopology(topology);
				// The end message sits over the board, while the ready banner would cover the top margin
				field.shutdown();
				for (final int zoom : SnakeField.ZOOM_LEVELS) {
					field.setZoom(zoom);
					final BufferedImage image = render(field);
					final List<String> wrong = new ArrayList<>();
					for (int y = -margin; y < rows + margin; y++) {
						for (int x = -margin; x < columns + margin; x++) {
							final boolean outsideX = x < 0 || x >= columns;
							final boolean outsideY = y < 0 || y >= rows;
							if (!outsideX && !outsideY)
								continue;
							// The centre of the cell, scaled like the whole board
							final int rgb = image.getRGB((BoardPainter.BOARD_X + x * BoardPainter.CELL_SIZE
									+ BoardPainter.CELL_SIZE / 2) * zoom / 100, (BoardPainter.BOARD_Y
									+ y * BoardPainter.CELL_SIZE + BoardPainter.CELL_SIZE / 2) * zoom / 100);
							final String cell = "(" + x + "," + y + ")";
							if ((outsideX && horizontal == Gluing.WALL) || (outsideY && vertical == Gluing.WALL)) {
								if (rgb != BoardPainter.WALL_MARGIN_COLOR.getRGB())
									wrong.add(cell + " is not wall margin");
								continue;
							}
							final Position source = develop(topology, x, y, true);
							if (source == null || !source.equals(develop(topology, x, y, false)))
								wrong.add(cell + " develops differently along the two axes");
							else if (isBluish(rgb) != body.contains(source))
								wrong.add(cell + " does not copy " + source);
						}
					}
					check(wrong.isEmpty(), topology.description() + " margin copies the glued cells at " + zoom
							+ "% (" + wrong.size() + " wrong, first " + wrong.stream().limit(5).toList() + ")");
				}
			}
		}
	}

	/**
	 * The board cell shown at a margin position: walk there cell by cell from the
	 * nearest board cell, along one axis first. Crossing a reflecting edge
	 * reverses the walk's direction along the other axis from then on.
	 */
	private static Position develop(final Topology topology, final int targetX, final int targetY,
			final boolean horizontalFirst) {
		final int columns = SnakeField.BOARD_COLUMNS;
		final int rows = SnakeField.BOARD_ROWS;
		int x = Math.clamp(targetX, 0, columns - 1);
		int y = Math.clamp(targetY, 0, rows - 1);
		Position cell = new Position(x, y);
		int signX = 1;
		int signY = 1;
		for (final boolean horizontal : new boolean[] { horizontalFirst, !horizontalFirst }) {
			while (horizontal ? x != targetX : y != targetY) {
				final int step = horizontal ? Integer.signum(targetX - x) : Integer.signum(targetY - y);
				final Position stepped = horizontal ? cell.translate(signX * step, 0) : cell.translate(0, signY * step);
				cell = topology.map(stepped, columns, rows);
				if (cell == null)
					return null;
				if ((stepped.x() < 0 || stepped.x() >= columns) && topology.horizontal() == Gluing.FLIP)
					signY = -signY;
				if ((stepped.y() < 0 || stepped.y() >= rows) && topology.vertical() == Gluing.FLIP)
					signX = -signX;
				if (horizontal)
					x += step;
				else
					y += step;
			}
		}
		return cell;
	}

	private static boolean blueAt(final BufferedImage image, final Point point, final int zoom) {
		return isBluish(image.getRGB(point.x * zoom / 100, point.y * zoom / 100));
	}

	/** Compares the top margin above the board, where the status banner is drawn. */
	private static boolean differentOverlay(final BufferedImage first, final BufferedImage second) {
		for (int y = 0; y < BoardPainter.BOARD_Y; y++)
			for (int x = BoardPainter.BOARD_X; x < BoardPainter.BOARD_X + BoardPainter.BOARD_WIDTH; x++)
				if (first.getRGB(x, y) != second.getRGB(x, y))
					return true;
		return false;
	}

	private static BufferedImage render(final SnakeField field) {
		field.setSize(field.getPreferredSize());
		final BufferedImage image = new BufferedImage(field.getWidth(), field.getHeight(), BufferedImage.TYPE_INT_RGB);
		final Graphics2D graphics = image.createGraphics();
		try {
			field.paint(graphics);
		} finally {
			graphics.dispose();
		}
		return image;
	}

	private static final class CountingRepaints extends RepaintManager {
		private int count;

		@Override
		public void addDirtyRegion(final JComponent component, final int x, final int y,
				final int width, final int height) {
			if (component instanceof SnakeField)
				count++;
		}
	}
}
