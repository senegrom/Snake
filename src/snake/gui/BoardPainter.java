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
import java.awt.Paint;
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
	private static final Font STATUS_FONT = new Font("Dialog", Font.BOLD, 18);

	private static final int STRIPE_PERIOD = 12;
	private static final float[] SHADE_STOPS = { 0f, 0.6f, 1f };
	private static final Color GLINT_COLOR = new Color(255, 255, 255, 190);
	private static final BasicStroke EDGE_STROKE = new BasicStroke(1f, BasicStroke.CAP_BUTT,
			BasicStroke.JOIN_MITER, 10f, new float[] { 4f, 4f }, 0f);
	private static final GradientPaint SHADING = new GradientPaint(0, 0, BOARD_LIGHT_COLOR,
			BOARD_WIDTH, BOARD_HEIGHT, BOARD_SHADE_COLOR);
	// Cell paints are defined at the origin and reused by translating the graphics to each cell
	private static final RadialGradientPaint APPLE_SHADING = cellShading(APPLE_COLOR);
	private static final RadialGradientPaint HEAD_SHADING = cellShading(HEAD_COLOR);
	private static final RadialGradientPaint SNAKE_SHADING = cellShading(SNAKE_COLOR);
	private static final Color APPLE_OUTLINE = blend(APPLE_COLOR, Color.BLACK, 0.55f);
	private static final Color HEAD_OUTLINE = blend(HEAD_COLOR, Color.BLACK, 0.6f);
	private static final Color SNAKE_OUTLINE = blend(SNAKE_COLOR, Color.BLACK, 0.6f);

	private BufferedImage board = new BufferedImage(BOARD_WIDTH, BOARD_HEIGHT, BufferedImage.TYPE_INT_RGB);

	/** Pixel centre of a board cell in panel coordinates. */
	static Point cellCenter(final Position cell) {
		return new Point(BOARD_X + cell.x() * CELL_SIZE + CELL_SIZE / 2,
				BOARD_Y + cell.y() * CELL_SIZE + CELL_SIZE / 2);
	}

	void paint(final Graphics2D graphics, final int width, final int height, final Snake snake,
			final Position apple, final Topology topology, final String endMessage, final Color endColor,
			final boolean terminal) {
		resizeBuffer(graphics);
		renderBoard(snake, apple);
		graphics.setColor(MARGIN_COLOR);
		graphics.fillRect(0, 0, width, height);
		paintNeighbours(graphics, topology);
		graphics.drawImage(board, BOARD_X, BOARD_Y, BOARD_WIDTH, BOARD_HEIGHT, null);
		paintEdges(graphics, topology);
		if (endMessage != null)
			paintOverlay(graphics, endMessage, endColor, terminal);
	}

	private void resizeBuffer(final Graphics2D graphics) {
		final AffineTransform transform = graphics.getTransform();
		final int width = Math.max(1, (int) Math.ceil(BOARD_WIDTH
				* Math.hypot(transform.getScaleX(), transform.getShearY())));
		final int height = Math.max(1, (int) Math.ceil(BOARD_HEIGHT
				* Math.hypot(transform.getScaleY(), transform.getShearX())));
		if (board.getWidth() != width || board.getHeight() != height)
			board = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
	}

	private void renderBoard(final Snake snake, final Position apple) {
		final Graphics2D g = board.createGraphics();
		try {
			g.scale(board.getWidth() / (double) BOARD_WIDTH, board.getHeight() / (double) BOARD_HEIGHT);
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
			final boolean isHead = cell.equals(head);
			paintCell(g, cell, isHead ? HEAD_SHADING : SNAKE_SHADING, isHead ? HEAD_OUTLINE : SNAKE_OUTLINE);
		}
		// One eye, two pixels ahead of the head centre in the queued direction
		final Position ahead = snake.direction().move(head);
		final int centreX = head.x() * CELL_SIZE + CELL_SIZE / 2;
		final int centreY = head.y() * CELL_SIZE + CELL_SIZE / 2;
		g.setColor(EYE_COLOR);
		g.fillRect(centreX + 2 * (ahead.x() - head.x()) - 1, centreY + 2 * (ahead.y() - head.y()) - 1, 2, 2);
	}

	private static void paintCell(final Graphics2D g, final Position cell, final Paint shading, final Color outline) {
		final int x = cell.x() * CELL_SIZE;
		final int y = cell.y() * CELL_SIZE;
		g.translate(x, y);
		try {
			g.setPaint(shading);
			g.fillRoundRect(1, 1, CELL_SIZE - 2, CELL_SIZE - 2, 4, 4);
			g.setColor(outline);
			g.drawRoundRect(1, 1, CELL_SIZE - 3, CELL_SIZE - 3, 4, 4);
		} finally {
			g.translate(-x, -y);
		}
	}

	private static void paintApple(final Graphics2D g, final Position apple) {
		final int x = apple.x() * CELL_SIZE;
		final int y = apple.y() * CELL_SIZE;
		g.translate(x, y);
		try {
			g.setPaint(APPLE_SHADING);
			g.fillOval(1, 1, CELL_SIZE - 2, CELL_SIZE - 2);
			g.setColor(APPLE_OUTLINE);
			g.drawOval(1, 1, CELL_SIZE - 3, CELL_SIZE - 3);
			g.setColor(GLINT_COLOR);
			g.fillOval(3, 3, 2, 2);
		} finally {
			g.translate(-x, -y);
		}
	}

	/** Radial shading of one cell at the origin, lit from the top-left for a rounded, raised look. */
	private static RadialGradientPaint cellShading(final Color base) {
		return new RadialGradientPaint(
				new Point2D.Float(CELL_SIZE / 2f, CELL_SIZE / 2f), CELL_SIZE * 0.6f,
				new Point2D.Float(CELL_SIZE * 0.35f, CELL_SIZE * 0.35f), SHADE_STOPS,
				new Color[] { blend(base, Color.WHITE, 0.45f), base, blend(base, Color.BLACK, 0.45f) },
				MultipleGradientPaint.CycleMethod.NO_CYCLE);
	}

	private static Color blend(final Color from, final Color to, final float amount) {
		return new Color(
				Math.round(from.getRed() + (to.getRed() - from.getRed()) * amount),
				Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * amount),
				Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * amount));
	}

	/**
	 * Draws the eight neighbouring copies of the board around it. A side
	 * neighbour is reached by crossing one edge; a corner neighbour by
	 * crossing one edge of each pair, so it combines both gluings (on the
	 * projective plane, two reflections make a half turn). A neighbour
	 * beyond a wall is drawn as solid wall margin instead.
	 */
	private void paintNeighbours(final Graphics2D graphics, final Topology topology) {
		for (int column = -1; column <= 1; column++)
			for (int row = -1; row <= 1; row++) {
				if (column == 0 && row == 0)
					continue;
				final Rectangle strip = new Rectangle(
						column == 0 ? BOARD_X : column < 0 ? BOARD_X - MARGIN : BOARD_X + BOARD_WIDTH,
						row == 0 ? BOARD_Y : row < 0 ? BOARD_Y - MARGIN : BOARD_Y + BOARD_HEIGHT,
						column == 0 ? BOARD_WIDTH : MARGIN,
						row == 0 ? BOARD_HEIGHT : MARGIN);
				final Gluing acrossX = column == 0 ? Gluing.WRAP : topology.horizontal();
				final Gluing acrossY = row == 0 ? Gluing.WRAP : topology.vertical();
				if (acrossX == Gluing.WALL || acrossY == Gluing.WALL) {
					graphics.setColor(WALL_MARGIN_COLOR);
					graphics.fill(strip);
				} else {
					paintNeighbour(graphics, strip, BOARD_X + column * BOARD_WIDTH, BOARD_Y + row * BOARD_HEIGHT,
							acrossX == Gluing.FLIP, acrossY == Gluing.FLIP);
				}
			}
	}

	/**
	 * Draws the copy of the board whose corner sits at (tileX, tileY),
	 * clipped to the given margin strip. Crossing a flipped left or right
	 * edge mirrors the copy top to bottom; crossing a flipped top or bottom
	 * edge mirrors it left to right. The two reflections commute.
	 */
	private void paintNeighbour(final Graphics2D graphics, final Rectangle strip, final int tileX, final int tileY,
			final boolean mirrorRows, final boolean mirrorColumns) {
		final AffineTransform transform = AffineTransform.getTranslateInstance(tileX, tileY);
		if (mirrorRows) {
			transform.translate(0, BOARD_HEIGHT);
			transform.scale(1, -1);
		}
		if (mirrorColumns) {
			transform.translate(BOARD_WIDTH, 0);
			transform.scale(-1, 1);
		}
		transform.scale(BOARD_WIDTH / (double) board.getWidth(), BOARD_HEIGHT / (double) board.getHeight());
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

	private static void paintOverlay(final Graphics2D graphics, final String text, final Color color,
			final boolean terminal) {
		final Graphics2D g = (Graphics2D) graphics.create();
		try {
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setFont(terminal ? END_FONT : STATUS_FONT);
			final FontMetrics metrics = g.getFontMetrics();
			final int textWidth = metrics.stringWidth(text);
			final int x = BOARD_X + (BOARD_WIDTH - textWidth) / 2;
			// Keep non-terminal banners above both the board and its wall band.
			// Clipping also protects the wall when a platform substitutes a taller font.
			final int marginHeight = BOARD_Y - WALL_THICKNESS;
			if (!terminal)
				g.clipRect(BOARD_X, 0, BOARD_WIDTH, marginHeight);
			final int baseline = terminal ? BOARD_Y + BOARD_HEIGHT / 2
					: marginHeight / 2 + (metrics.getAscent() - metrics.getDescent()) / 2;
			final int padding = terminal ? 8 : 4;
			g.setColor(new Color(255, 255, 255, 225));
			g.fillRoundRect(x - 12, baseline - metrics.getAscent() - padding,
					textWidth + 24, metrics.getHeight() + 2 * padding, 14, 14);
			g.setColor(color);
			g.drawString(text, x, baseline);
		} finally {
			g.dispose();
		}
	}
}
