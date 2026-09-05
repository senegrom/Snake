package snake.gui;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
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

/** Deterministic regression coverage for view and lifecycle notifications. */
public final class SnakeInteractionTests {
	private SnakeInteractionTests() {
	}

	public static void main(final String[] args) {
		System.exit(TestSupport.run("SnakeInteractionTests", () -> {
			testDirectionRepainting();
			testStatusNotifications();
			testOverlayKeepsWallsVisible();
			testZoomAndTopologyHints();
		}));
	}

	private static SnakeField fixture(final Topology topology) {
		final SnakeField field = new SnakeField(new Snake(Direction.RIGHT,
				List.of(new Position(2, 5), new Position(1, 5), new Position(0, 5))), new Position(10, 10));
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
				for (final int zoom : SnakeField.ZOOM_LEVELS) {
					field.setZoom(zoom);
					equal(zoom, field.zoom(), "zoom accessor");
					equal(new Dimension(490 * zoom / 100, 390 * zoom / 100), field.getPreferredSize(),
							"zoom scales the board and margins");
					final BufferedImage image = render(field);
					check(blueAt(image, 55, 95, zoom), "scaled board preserves snake cell positions");
					final int ghostY = horizontal == Gluing.FLIP ? 295 : 95;
					equal(horizontal != Gluing.WALL, blueAt(image, 465, ghostY, zoom),
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

	private static boolean blueAt(final BufferedImage image, final int x, final int y, final int zoom) {
		final int rgb = image.getRGB(x * zoom / 100, y * zoom / 100);
		return (rgb & 255) - ((rgb >> 16) & 255) >= 60;
	}

	private static boolean differentOverlay(final BufferedImage first, final BufferedImage second) {
		for (int y = 0; y < BoardPainter.BOARD_Y; y++)
			for (int x = 100; x < 390; x++)
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
