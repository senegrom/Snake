package snake.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.JComponent;
import javax.swing.RepaintManager;
import snake.Direction;
import snake.Position;
import snake.Snake;
import snake.topology.Topology;
import static snake.gui.TestSupport.check;
import static snake.gui.TestSupport.equal;
import static snake.gui.TestSupport.expect;

/** Targeted model, timing and headless-rendering smoke tests. */
public final class SnakeSmokeTests {
	private SnakeSmokeTests() {
	}

	public static void main(final String[] args) {
		System.setProperty("java.awt.headless", "true");
		System.exit(TestSupport.run("SnakeSmokeTests", SnakeSmokeTests::run));
	}

	private static void run() {
		testMovementInvariants();
		testValidationBoundaries();
		testAppleSelection();
		testElapsedTime();
		testFieldEventsAndRepainting();
		testRendering();
	}

	private static void testMovementInvariants() {
		final Snake moving = new Snake(Direction.RIGHT,
				List.of(new Position(2, 0), new Position(1, 0), new Position(0, 0)));
		check(moving.advanceTo(new Position(3, 0), false), "ordinary movement succeeds");
		equal(List.of(new Position(3, 0), new Position(2, 0), new Position(1, 0)),
				List.copyOf(moving.body()), "ordinary movement removes the tail");

		final Snake growingIntoTail = new Snake(Direction.UP,
				List.of(new Position(0, 1), new Position(1, 1),
						new Position(1, 0), new Position(0, 0)));
		final List<Position> beforeCollision = List.copyOf(growingIntoTail.body());
		check(!growingIntoTail.advanceTo(growingIntoTail.body().getLast(), true),
				"growing into the current tail is a collision");
		equal(beforeCollision, List.copyOf(growingIntoTail.body()),
				"a rejected tail-growth collision leaves the body unchanged");

		final Snake turning = new Snake(Direction.RIGHT,
				List.of(new Position(2, 0), new Position(1, 0), new Position(0, 0)));
		check(turning.requestDirection(Direction.UP), "the first queued turn is accepted");
		check(turning.requestDirection(Direction.DOWN),
				"a queued turn may be revised before movement");
		check(!turning.requestDirection(Direction.LEFT),
				"a revision may not reverse the last completed step");
		check(turning.requestDirection(Direction.RIGHT),
				"returning to the current heading cancels the queued turn");
	}

	private static void testValidationBoundaries() {
		final Snake snake = new Snake(Direction.RIGHT,
				List.of(new Position(2, 0), new Position(1, 0), new Position(0, 0)));
		expect(IllegalArgumentException.class, () -> new SnakeField(snake,
				new Position(SnakeField.BOARD_COLUMNS, 1)),
				"apple at the exclusive right bound is rejected");
		expect(IllegalArgumentException.class, () -> new SnakeField(snake,
				new Position(1, SnakeField.BOARD_ROWS)),
				"apple at the exclusive bottom bound is rejected");
		expect(IllegalArgumentException.class, () -> Topology.TORUS.map(new Position(0, 0), 0, 1),
				"zero-column boards are rejected before mapping");
		expect(IllegalArgumentException.class, () -> Topology.TORUS.map(new Position(0, 0), 1, 0),
				"zero-row boards are rejected before mapping");
		expect(NullPointerException.class, () -> new SnakeField(snake, new Position(10, 10), null),
				"field rejects a null clock");
	}

	private static void testAppleSelection() {
		final Snake snake = new Snake(Direction.RIGHT,
				List.of(new Position(2, 0), new Position(1, 0), new Position(0, 0)));
		final int freeCells = SnakeField.CELL_COUNT - snake.length();
		final int selectedFreeCell = new Random(3).nextInt(freeCells);
		equal(nthFreeCell(snake, selectedFreeCell), SnakeField.spawnApple(snake, new Random(3)),
				"apple placement honours the supplied random selection");
		expect(NullPointerException.class, () -> SnakeField.spawnApple(snake, null),
				"apple placement rejects a null random source");
	}

	private static Position nthFreeCell(final Snake snake, final int selectedFreeCell) {
		int remaining = selectedFreeCell;
		for (int y = 0; y < SnakeField.BOARD_ROWS; y++)
			for (int x = 0; x < SnakeField.BOARD_COLUMNS; x++) {
				final Position candidate = new Position(x, y);
				if (!snake.contains(candidate) && remaining-- == 0)
					return candidate;
			}
		throw new AssertionError("selected free cell does not exist");
	}

	private static void testElapsedTime() {
		final AtomicLong clock = new AtomicLong(1_000_000_000L);
		final Snake snake = new Snake(Direction.RIGHT,
				List.of(new Position(2, 0), new Position(1, 0), new Position(0, 0)));
		final SnakeField field = new SnakeField(snake, new Position(10, 10), clock::get);
		final EventRecorder events = new EventRecorder(field);
		field.setMoveDelay(100_000);
		field.startGame();
		clock.addAndGet(3_400_000_000L);
		check(field.pauseGame(), "running game pauses");
		equal(3, field.elapsedSeconds(), "pause captures elapsed running time");
		equal(3, events.time, "pause publishes elapsed seconds");
		equal(1, events.timeChanges, "first displayed second is published once");

		clock.addAndGet(50_000_000_000L);
		equal(3, field.elapsedSeconds(), "paused time is excluded");
		check(field.resumeGame(), "paused game resumes");
		check(field.pauseGame(), "game can pause again without elapsed time");
		equal(1, events.timeChanges, "unchanged elapsed seconds are not republished");
		check(field.resumeGame(), "game resumes for a second running interval");
		clock.addAndGet(2_200_000_000L);
		check(field.pauseGame(), "second running interval can be paused");
		equal(5, field.elapsedSeconds(), "resumed time continues from the paused total");
		equal(5, events.time, "new elapsed second is published");
		equal(2, events.timeChanges, "only changed elapsed seconds are published");
		check(field.resumeGame(), "game resumes before shutdown");
		clock.addAndGet(1_100_000_000L);
		field.shutdown();
		equal(6, field.elapsedSeconds(), "shutdown preserves the final running interval");

		final AtomicLong endClock = new AtomicLong(10_000_000_000L);
		final Snake wallSnake = new Snake(Direction.RIGHT,
				List.of(new Position(SnakeField.BOARD_COLUMNS - 1, 2),
						new Position(SnakeField.BOARD_COLUMNS - 2, 2)));
		final SnakeField ending = new SnakeField(wallSnake, new Position(0, 0), endClock::get);
		final EventRecorder endingEvents = new EventRecorder(ending);
		ending.setMoveDelay(100_000);
		ending.startGame();
		endClock.addAndGet(2_500_000_000L);
		ending.step();
		equal(SnakeField.Status.FINISHED, ending.status(), "wall collision finishes a running game");
		equal(2, ending.elapsedSeconds(), "game over preserves the final running interval");
		equal(1, endingEvents.finished, "game over publishes one finished event");
		equal(2, endingEvents.time, "game over publishes the final elapsed second");
	}

	private static void testFieldEventsAndRepainting() {
		final Snake eatingSnake = new Snake(Direction.RIGHT,
				List.of(new Position(2, 1), new Position(1, 1), new Position(0, 1)));
		final SnakeField eating = new SnakeField(eatingSnake, new Position(3, 1));
		final EventRecorder eatingEvents = new EventRecorder(eating);

		final RepaintManager originalManager = RepaintManager.currentManager(eating);
		final CountingRepaintManager repaintManager = new CountingRepaintManager();
		RepaintManager.setCurrentManager(repaintManager);
		try {
			eating.step();
		} finally {
			RepaintManager.setCurrentManager(originalManager);
		}
		equal(1, eatingEvents.points, "eating publishes the updated score");
		check(repaintManager.dirtyRegions > 0, "a completed step requests repainting");

		final SnakeField stopped = new SnakeField(new Snake(Direction.RIGHT,
				List.of(new Position(2, 4), new Position(1, 4), new Position(0, 4))),
				new Position(10, 10));
		stopped.shutdown();
		final List<Position> stoppedBody = List.copyOf(stopped.snake().body());
		stopped.step();
		equal(stoppedBody, List.copyOf(stopped.snake().body()),
				"steps after shutdown do not mutate the snake");
	}

	private static void testRendering() {
		final Snake snake = new Snake(Direction.RIGHT,
				List.of(new Position(2, 1), new Position(1, 1), new Position(0, 1)));
		final SnakeField field = new SnakeField(snake, new Position(3, 1));
		final BufferedImage image = render(field);
		equal(BoardPainter.PANEL_SIZE.width, image.getWidth(), "panel width includes both margins");
		equal(BoardPainter.PANEL_SIZE.height, image.getHeight(), "panel height includes both margins");

		equal(BoardPainter.WALL_MARGIN_COLOR.getRGB(), image.getRGB(0, 0),
				"plane corners paint solid wall margin");
		final int headPixel = pixel(image, BoardPainter.cellCenter(new Position(2, 1)));
		final int bodyPixel = pixel(image, BoardPainter.cellCenter(new Position(1, 1)));
		check(isBluish(bodyPixel), "body cells paint in shaded blue");
		check(isBluish(headPixel), "the head paints in shaded blue");
		check(headPixel != bodyPixel, "the head is shaded differently from the body");
		check(isReddish(pixel(image, BoardPainter.cellCenter(new Position(3, 1)))),
				"the apple paints in shaded red");
		final Point head = BoardPainter.cellCenter(new Position(2, 1));
		equal(BoardPainter.EYE_COLOR.getRGB(), image.getRGB(head.x + 1, head.y - 1),
				"the eye looks in the movement direction");
		check(isLightGrey(pixel(image, BoardPainter.cellCenter(new Position(5, 5)))),
				"empty cells show the light shaded texture");
		check(brightness(pixel(image, BoardPainter.cellCenter(new Position(0, 0))))
				> brightness(pixel(image, BoardPainter.cellCenter(
						new Position(SnakeField.BOARD_COLUMNS - 1, SnakeField.BOARD_ROWS - 1)))),
				"the board is shaded from its top-left corner");

		final int midX = BoardPainter.BOARD_X + BoardPainter.BOARD_WIDTH / 2;
		final int midY = BoardPainter.BOARD_Y + BoardPainter.BOARD_HEIGHT / 2;
		final int wall = BoardPainter.WALL_COLOR.getRGB();
		equal(wall, image.getRGB(BoardPainter.BOARD_X - 2, midY), "plane paints a thick left wall");
		equal(wall, image.getRGB(BoardPainter.BOARD_X + BoardPainter.BOARD_WIDTH + 1, midY),
				"plane paints a thick right wall");
		equal(wall, image.getRGB(midX, BoardPainter.BOARD_Y - 2), "plane paints a thick top wall");
		equal(wall, image.getRGB(midX, BoardPainter.BOARD_Y + BoardPainter.BOARD_HEIGHT + 1),
				"plane paints a thick bottom wall");

		// A snake along the left edge is echoed in the right margin: at the same
		// row on the torus, at the mirrored row on the Klein bottle
		final int ghostX = BoardPainter.BOARD_X + BoardPainter.BOARD_WIDTH + BoardPainter.CELL_SIZE
				+ BoardPainter.CELL_SIZE / 2;
		final int sameRow = BoardPainter.BOARD_Y + 5 * BoardPainter.CELL_SIZE + BoardPainter.CELL_SIZE / 2;
		final int mirroredRow = BoardPainter.BOARD_Y
				+ (SnakeField.BOARD_ROWS - 1 - 5) * BoardPainter.CELL_SIZE + BoardPainter.CELL_SIZE / 2;
		final BufferedImage torusImage = render(edgeField(Topology.TORUS));
		check(isBluish(torusImage.getRGB(ghostX, sameRow)), "torus margin echoes the snake at the same row");
		check(!isBluish(torusImage.getRGB(ghostX, mirroredRow)), "torus margin is not mirrored");
		check(torusImage.getRGB(BoardPainter.BOARD_X - 2, midY) != wall, "torus paints no wall");
		final BufferedImage kleinImage = render(edgeField(Topology.KLEIN_BOTTLE));
		check(isBluish(kleinImage.getRGB(ghostX, mirroredRow)),
				"Klein bottle margin echoes the snake at the mirrored row");
		check(!isBluish(kleinImage.getRGB(ghostX, sameRow)),
				"Klein bottle margin does not echo the snake at the same row");
		final BufferedImage planeImage = render(edgeField(Topology.PLANE));
		check(!isBluish(planeImage.getRGB(ghostX, sameRow)), "plane margin shows no neighbour");

		// A snake near the top edge is echoed in the bottom margin; the
		// projective plane mirrors the columns
		final Snake topSnake = new Snake(Direction.DOWN,
				List.of(new Position(5, 2), new Position(5, 1), new Position(5, 0)));
		final int ghostY = BoardPainter.BOARD_Y + BoardPainter.BOARD_HEIGHT + BoardPainter.CELL_SIZE
				+ BoardPainter.CELL_SIZE / 2;
		final int sameColumn = BoardPainter.BOARD_X + 5 * BoardPainter.CELL_SIZE + BoardPainter.CELL_SIZE / 2;
		final int mirroredColumn = BoardPainter.BOARD_X
				+ (SnakeField.BOARD_COLUMNS - 1 - 5) * BoardPainter.CELL_SIZE + BoardPainter.CELL_SIZE / 2;
		final SnakeField projective = new SnakeField(topSnake, new Position(10, 10));
		projective.setTopology(Topology.PROJECTIVE_PLANE);
		final BufferedImage projectiveImage = render(projective);
		check(isBluish(projectiveImage.getRGB(mirroredColumn, ghostY)),
				"projective plane bottom margin mirrors the columns");
		check(!isBluish(projectiveImage.getRGB(sameColumn, ghostY)),
				"projective plane bottom margin is not a plain wrap");

		// Corner neighbours combine both gluings: a snake near the top-left corner
		// echoes in the bottom-right corner on the torus, in the top-right corner on
		// the Klein bottle (rows mirrored) and in the top-left corner on the
		// projective plane (a half turn)
		final int cornerCellX = 2 * BoardPainter.CELL_SIZE + BoardPainter.CELL_SIZE / 2;
		final int cornerCellY = BoardPainter.CELL_SIZE + BoardPainter.CELL_SIZE / 2;
		final Point bottomRight = new Point(BoardPainter.BOARD_X + BoardPainter.BOARD_WIDTH + cornerCellX,
				BoardPainter.BOARD_Y + BoardPainter.BOARD_HEIGHT + cornerCellY);
		final Point topRight = new Point(BoardPainter.BOARD_X + BoardPainter.BOARD_WIDTH + cornerCellX,
				BoardPainter.BOARD_Y - cornerCellY);
		final Point topLeft = new Point(BoardPainter.BOARD_X - cornerCellX, BoardPainter.BOARD_Y - cornerCellY);
		final BufferedImage torusCorners = render(cornerField(Topology.TORUS));
		check(isBluish(pixel(torusCorners, bottomRight)), "torus corner echoes the snake unchanged");
		check(!isBluish(pixel(torusCorners, topRight)), "torus corner is not mirrored");
		final BufferedImage kleinCorners = render(cornerField(Topology.KLEIN_BOTTLE));
		check(isBluish(pixel(kleinCorners, topRight)), "Klein bottle corner echoes the snake with rows mirrored");
		check(!isBluish(pixel(kleinCorners, bottomRight)), "Klein bottle corner is not a plain copy");
		final BufferedImage projectiveCorners = render(cornerField(Topology.PROJECTIVE_PLANE));
		check(isBluish(pixel(projectiveCorners, topLeft)),
				"projective plane corner echoes the snake turned by a half turn");
		check(!isBluish(pixel(projectiveCorners, bottomRight)), "projective plane corner is not a plain copy");
		equal(BoardPainter.WALL_MARGIN_COLOR.getRGB(), pixel(render(cornerField(Topology.CYLINDER)), topRight),
				"cylinder corners beyond a wall are solid wall margin");

		final Snake wallSnake = new Snake(Direction.RIGHT,
				List.of(new Position(SnakeField.BOARD_COLUMNS - 1, 2),
						new Position(SnakeField.BOARD_COLUMNS - 2, 2)));
		final SnakeField finished = new SnakeField(wallSnake, new Position(0, 0));
		finished.step();
		final BufferedImage endImage = render(finished);
		check(finished.status() == SnakeField.Status.FINISHED,
				"terminal field renders without changing state");
		equal("Game Over", finished.endMessage(), "loss selects the game-over text");
		equal(Color.RED, finished.endMessageColor(), "loss selects the red end color");
		check(containsColor(endImage, Color.RED, endImage.getWidth() / 4, endImage.getHeight() / 3,
				endImage.getWidth() * 3 / 4, endImage.getHeight() * 2 / 3),
				"game-over overlay paints red in the board centre");

		final Position winningApple = new Position(0, 0);
		final Snake winningSnake = new Snake(Direction.LEFT,
				almostFullBody(new Position(1, 0), winningApple));
		final SnakeField winning = new SnakeField(winningSnake, winningApple);
		winning.step();
		equal("You Win!", winning.endMessage(), "win selects the winning text");
		final Color winColor = new Color(0, 128, 0);
		equal(winColor, winning.endMessageColor(), "win selects the green end color");
		final BufferedImage winImage = render(winning);
		check(containsColor(winImage, winColor, winImage.getWidth() / 4, winImage.getHeight() / 3,
				winImage.getWidth() * 3 / 4, winImage.getHeight() * 2 / 3),
				"winning overlay paints green in the board centre");
	}

	private static final class EventRecorder {
		private int finished;
		private int points = -1;
		private int time = -1;
		private int timeChanges;

		EventRecorder(final SnakeField field) {
			field.addPropertyChangeListener(SnakeField.POINTS_PROPERTY,
					event -> points = (Integer) event.getNewValue());
			field.addPropertyChangeListener(SnakeField.TIME_PROPERTY, event -> {
				time = (Integer) event.getNewValue();
				timeChanges++;
			});
			field.addPropertyChangeListener(SnakeField.FINISHED_PROPERTY, event -> finished++);
		}
	}

	private static final class CountingRepaintManager extends RepaintManager {
		private int dirtyRegions;

		@Override
		public void addDirtyRegion(final JComponent component, final int x, final int y,
				final int width, final int height) {
			if (component instanceof SnakeField)
				dirtyRegions++;
			super.addDirtyRegion(component, x, y, width, height);
		}
	}

	private static List<Position> almostFullBody(final Position head, final Position freeCell) {
		final List<Position> body = new ArrayList<>(SnakeField.CELL_COUNT - 1);
		body.add(head);
		for (int y = 0; y < SnakeField.BOARD_ROWS; y++)
			for (int x = 0; x < SnakeField.BOARD_COLUMNS; x++) {
				final Position position = new Position(x, y);
				if (!position.equals(head) && !position.equals(freeCell))
					body.add(position);
			}
		return body;
	}

	/** A short snake lying along the left edge in row 5, on the given topology. */
	private static SnakeField edgeField(final Topology topology) {
		final Snake snake = new Snake(Direction.RIGHT,
				List.of(new Position(2, 5), new Position(1, 5), new Position(0, 5)));
		final SnakeField field = new SnakeField(snake, new Position(10, 10));
		field.setTopology(topology);
		return field;
	}

	/** A short snake near the top-left corner, on the given topology. */
	private static SnakeField cornerField(final Topology topology) {
		final Snake snake = new Snake(Direction.LEFT,
				List.of(new Position(1, 1), new Position(2, 1), new Position(3, 1)));
		final SnakeField field = new SnakeField(snake, new Position(10, 10));
		field.setTopology(topology);
		return field;
	}

	private static int pixel(final BufferedImage image, final Point point) {
		return image.getRGB(point.x, point.y);
	}

	/** True for the faint blended echo of a blue snake cell, false for any grey or texture pixel. */
	private static boolean isBluish(final int rgb) {
		return (rgb & 0xFF) - ((rgb >> 16) & 0xFF) >= 60;
	}

	private static boolean isReddish(final int rgb) {
		final int red = (rgb >> 16) & 0xFF;
		return red - ((rgb >> 8) & 0xFF) >= 60 && red - (rgb & 0xFF) >= 60;
	}

	private static int brightness(final int rgb) {
		return (rgb & 0xFF) + ((rgb >> 8) & 0xFF) + ((rgb >> 16) & 0xFF);
	}

	/** True for the near-white, neutral shades of an empty board cell. */
	private static boolean isLightGrey(final int rgb) {
		final int red = (rgb >> 16) & 0xFF;
		final int green = (rgb >> 8) & 0xFF;
		final int blue = rgb & 0xFF;
		return Math.min(red, Math.min(green, blue)) >= 200
				&& Math.max(red, Math.max(green, blue)) - Math.min(red, Math.min(green, blue)) <= 6;
	}

	private static BufferedImage render(final SnakeField field) {
		final Dimension size = field.getPreferredSize();
		field.setSize(size);
		final BufferedImage image = new BufferedImage(size.width, size.height,
				BufferedImage.TYPE_INT_ARGB);
		final Graphics2D graphics = image.createGraphics();
		try {
			field.paint(graphics);
		} finally {
			graphics.dispose();
		}
		return image;
	}

	private static boolean containsColor(final BufferedImage image, final Color color,
			final int minX, final int minY, final int maxX, final int maxY) {
		final int rgb = color.getRGB();
		for (int y = minY; y < maxY; y++)
			for (int x = minX; x < maxX; x++)
				if (image.getRGB(x, y) == rgb)
					return true;
		return false;
	}

}
