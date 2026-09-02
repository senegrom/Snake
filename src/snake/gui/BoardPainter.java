package snake.gui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import snake.Position;
import snake.Snake;
import snake.topology.Gluing;
import snake.topology.Topology;

/**
 * Draws the board with a faint copy of each glued neighbour in the margin, so
 * a plain wrap shows the board continuing and a flipped gluing shows it
 * mirrored. The board is shaded from its top-left corner and striped
 * diagonally, so a mirrored copy is recognisable even when the snake is far
 * from the edge.
 */
final class BoardPainter {
	static final int CELL_SIZE = 10;
	static final int MARGIN_CELLS = 4;
	static final int MARGIN = MARGIN_CELLS * CELL_SIZE;
	static final int BOARD_WIDTH = SnakeField.BOARD_COLUMNS * CELL_SIZE;
	static final int BOARD_HEIGHT = SnakeField.BOARD_ROWS * CELL_SIZE;
	static final int BOARD_X = MARGIN;
	static final int BOARD_Y = MARGIN;
	static final Dimension PANEL_SIZE = new Dimension(BOARD_WIDTH + 2 * MARGIN, BOARD_HEIGHT + 2 * MARGIN);
	static final int WALL_THICKNESS = 4;
	static final float GHOST_ALPHA = 0.5f;

	static final Color APPLE_COLOR = Color.RED;
	static final Color BOARD_LIGHT_COLOR = new Color(254, 254, 254);
	static final Color BOARD_SHADE_COLOR = new Color(230, 230, 230);
	static final Color EYE_COLOR = Color.WHITE;
	static final Color FLIP_EDGE_COLOR = new Color(214, 108, 0);
	static final Color HEAD_COLOR = new Color(40, 140, 255);
	static final Color MARGIN_COLOR = new Color(230, 230, 230);
	static final Color SNAKE_COLOR = Color.BLUE;
	static final Color STRIPE_COLOR = new Color(0, 0, 0, 16);
	static final Color WALL_COLOR = Color.BLACK;
	static final Color WALL_MARGIN_COLOR = new Color(204, 204, 204);
	static final Color WIN_COLOR = new Color(0, 128, 0);
	static final Color WRAP_EDGE_COLOR = new Color(112, 112, 112);
	static final Font END_FONT = new Font("Verdana", Font.BOLD, 40);

	private static final int STRIPE_PERIOD = 12;
	private static final float[] SHADE_STOPS = { 0f, 0.6f, 1f };
	private static final Color GLINT_COLOR = new Color(255, 255, 255, 190);
	private static final BasicStroke EDGE_STROKE = new BasicStroke(1f, BasicStroke.CAP_BUTT,
			BasicStroke.JOIN_MITER, 10f, new float[] { 4f, 4f }, 0f);
	private static final GradientPaint SHADING = new GradientPaint(0, 0, BOARD_LIGHT_COLOR,
			BOARD_WIDTH, BOARD_HEIGHT, BOARD_SHADE_COLOR);

	private final BufferedImage board = new BufferedImage(BOARD_WIDTH, BOARD_HEIGHT, BufferedImage.TYPE_INT_RGB);

	/** Pixel centre of a board cell in panel coordinates. */
	static Point cellCenter(final Position cell) {
		return new Point(BOARD_X + cell.x() * CELL_SIZE + CELL_SIZE / 2,
				BOARD_Y + cell.y() * CELL_SIZE + CELL_SIZE / 2);
	}

	void paint(final Graphics2D graphics, final int width, final int height, final Snake snake,
			final Position apple, final Topology topology, final String endMessage, final Color endColor) {
		renderBoard(snake, apple);
		graphics.setColor(MARGIN_COLOR);
		graphics.fillRect(0, 0, width, height);
		paintNeighbours(graphics, topology);
		graphics.drawImage(board, BOARD_X, BOARD_Y, null);
		paintEdges(graphics, topology);
		if (endMessage != null)
			paintEndMessage(graphics, endMessage, endColor);
	}

	private void renderBoard(final Snake snake, final Position apple) {
		final Graphics2D g = board.createGraphics();
		try {
			g.setPaint(SHADING);
			g.fillRect(0, 0, BOARD_WIDTH, BOARD_HEIGHT);
			g.setColor(STRIPE_COLOR);
			for (int offset = -BOARD_HEIGHT; offset < BOARD_WIDTH; offset += STRIPE_PERIOD)
				g.drawLine(offset, BOARD_HEIGHT - 1, offset + BOARD_HEIGHT - 1, 0);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			paintSnake(g, snake);
			if (apple != null)
				paintApple(g, apple);
		} finally {
			g.dispose();
		}
	}

	private static void paintSnake(final Graphics2D g, final Snake snake) {
		final Position head = snake.head();
		for (final Position cell : snake.body()) {
			final int x = cell.x() * CELL_SIZE;
			final int y = cell.y() * CELL_SIZE;
			final Color base = cell.equals(head) ? HEAD_COLOR : SNAKE_COLOR;
			g.setPaint(shading(x, y, base));
			g.fillRoundRect(x + 1, y + 1, CELL_SIZE - 2, CELL_SIZE - 2, 4, 4);
			g.setColor(blend(base, Color.BLACK, 0.6f));
			g.drawRoundRect(x + 1, y + 1, CELL_SIZE - 3, CELL_SIZE - 3, 4, 4);
		}
		// One eye, two pixels ahead of the head centre in the queued direction
		final Position ahead = snake.direction().move(head);
		final int centreX = head.x() * CELL_SIZE + CELL_SIZE / 2;
		final int centreY = head.y() * CELL_SIZE + CELL_SIZE / 2;
		g.setColor(EYE_COLOR);
		g.fillRect(centreX + 2 * (ahead.x() - head.x()) - 1, centreY + 2 * (ahead.y() - head.y()) - 1, 2, 2);
	}

	private static void paintApple(final Graphics2D g, final Position apple) {
		final int x = apple.x() * CELL_SIZE;
		final int y = apple.y() * CELL_SIZE;
		g.setPaint(shading(x, y, APPLE_COLOR));
		g.fillOval(x + 1, y + 1, CELL_SIZE - 2, CELL_SIZE - 2);
		g.setColor(blend(APPLE_COLOR, Color.BLACK, 0.55f));
		g.drawOval(x + 1, y + 1, CELL_SIZE - 3, CELL_SIZE - 3);
		g.setColor(GLINT_COLOR);
		g.fillOval(x + 3, y + 3, 2, 2);
	}

	/** Radial shading lit from the top-left, which gives a cell a rounded, raised look. */
	private static RadialGradientPaint shading(final int x, final int y, final Color base) {
		return new RadialGradientPaint(
				new Point2D.Float(x + CELL_SIZE / 2f, y + CELL_SIZE / 2f), CELL_SIZE * 0.6f,
				new Point2D.Float(x + CELL_SIZE * 0.35f, y + CELL_SIZE * 0.35f), SHADE_STOPS,
				new Color[] { blend(base, Color.WHITE, 0.45f), base, blend(base, Color.BLACK, 0.45f) },
				MultipleGradientPaint.CycleMethod.NO_CYCLE);
	}

	private static Color blend(final Color from, final Color to, final float amount) {
		return new Color(
				Math.round(from.getRed() + (to.getRed() - from.getRed()) * amount),
				Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * amount),
				Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * amount));
	}

	private void paintNeighbours(final Graphics2D graphics, final Topology topology) {
		paintNeighbour(graphics, topology.horizontal(),
				new Rectangle(BOARD_X - MARGIN, BOARD_Y, MARGIN, BOARD_HEIGHT), BOARD_X - BOARD_WIDTH, BOARD_Y, true);
		paintNeighbour(graphics, topology.horizontal(),
				new Rectangle(BOARD_X + BOARD_WIDTH, BOARD_Y, MARGIN, BOARD_HEIGHT), BOARD_X + BOARD_WIDTH, BOARD_Y, true);
		paintNeighbour(graphics, topology.vertical(),
				new Rectangle(BOARD_X, BOARD_Y - MARGIN, BOARD_WIDTH, MARGIN), BOARD_X, BOARD_Y - BOARD_HEIGHT, false);
		paintNeighbour(graphics, topology.vertical(),
				new Rectangle(BOARD_X, BOARD_Y + BOARD_HEIGHT, BOARD_WIDTH, MARGIN), BOARD_X, BOARD_Y + BOARD_HEIGHT, false);
	}

	/**
	 * Draws the neighbouring copy of the board whose corner sits at
	 * (tileX, tileY), clipped to the given margin strip. A flipped gluing
	 * mirrors the copy across the axis of the crossed edge.
	 */
	private void paintNeighbour(final Graphics2D graphics, final Gluing gluing, final Rectangle strip,
			final int tileX, final int tileY, final boolean sideEdge) {
		if (gluing == Gluing.WALL) {
			graphics.setColor(WALL_MARGIN_COLOR);
			graphics.fill(strip);
			return;
		}
		final AffineTransform transform = AffineTransform.getTranslateInstance(tileX, tileY);
		if (gluing == Gluing.FLIP) {
			if (sideEdge) {
				transform.translate(0, BOARD_HEIGHT);
				transform.scale(1, -1);
			} else {
				transform.translate(BOARD_WIDTH, 0);
				transform.scale(-1, 1);
			}
		}
		final Graphics2D g = (Graphics2D) graphics.create();
		try {
			g.clip(strip);
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, GHOST_ALPHA));
			g.drawImage(board, transform, null);
		} finally {
			g.dispose();
		}
	}

	private static void paintEdges(final Graphics2D graphics, final Topology topology) {
		final int left = BOARD_X;
		final int right = BOARD_X + BOARD_WIDTH;
		final int top = BOARD_Y;
		final int bottom = BOARD_Y + BOARD_HEIGHT;
		final int reach = WALL_THICKNESS;
		paintEdge(graphics, topology.horizontal(),
				new Rectangle(left - reach, top - reach, reach, BOARD_HEIGHT + 2 * reach),
				left - 1, top, left - 1, bottom - 1);
		paintEdge(graphics, topology.horizontal(),
				new Rectangle(right, top - reach, reach, BOARD_HEIGHT + 2 * reach),
				right, top, right, bottom - 1);
		paintEdge(graphics, topology.vertical(),
				new Rectangle(left - reach, top - reach, BOARD_WIDTH + 2 * reach, reach),
				left, top - 1, right - 1, top - 1);
		paintEdge(graphics, topology.vertical(),
				new Rectangle(left - reach, bottom, BOARD_WIDTH + 2 * reach, reach),
				left, bottom, right - 1, bottom);
	}

	private static void paintEdge(final Graphics2D graphics, final Gluing gluing, final Rectangle wallBand,
			final int x1, final int y1, final int x2, final int y2) {
		if (gluing == Gluing.WALL) {
			graphics.setColor(WALL_COLOR);
			graphics.fill(wallBand);
			return;
		}
		final Graphics2D g = (Graphics2D) graphics.create();
		try {
			g.setStroke(EDGE_STROKE);
			g.setColor(gluing == Gluing.FLIP ? FLIP_EDGE_COLOR : WRAP_EDGE_COLOR);
			g.drawLine(x1, y1, x2, y2);
		} finally {
			g.dispose();
		}
	}

	private static void paintEndMessage(final Graphics2D graphics, final String text, final Color color) {
		graphics.setFont(END_FONT);
		graphics.setColor(color);
		final FontMetrics metrics = graphics.getFontMetrics();
		graphics.drawString(text, BOARD_X + (BOARD_WIDTH - metrics.stringWidth(text)) / 2,
				BOARD_Y + BOARD_HEIGHT / 2);
	}
}
