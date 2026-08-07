package snakeGUI;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import javax.swing.JPanel;
import snake.Apple;
import snake.Direction;
import snake.Position;
import snake.Snake;
import topology.Topology;

/**
 * The actual game field for Snake, containing the snake and controlling every
 * aspect of the game.
 *
 * @author CGH
 */
public class SnakeField extends JPanel {

	private final static Color		background				= Color.WHITE;
	/** Move times/Speed levels */
	protected final static int[]	BETWEEN_MOVE_TIMES		= new int[]{200, 160, 120, 80, 50, 30 };
	protected final static int		DEF_SPEED				= 1;
	/** Height of the field in Snaxels (Snake Fields) */
	public final static int			FIELD_HEIGHT			= 30;
	/** Width of the field in Snaxels (Snake Fields) */
	public final static int			FIELD_WIDTH				= 40;
	private final static Color		foreground				= Color.BLACK;
	private final static Color		gameOverColor			= Color.RED;
	private final static Font		gameOverFont			= new Font("Verdana", Font.BOLD, 40);
	private final static int		gameOverLeftOffset		= 120;
	private final static String		gameOverText			= "Game Over";
	private final static String		gameWonText				= "You Win";
	/** Size of one Snaxel in Pixels */
	public final static int			ITEM_SIZE				= 10;
	public final static int			MAX_SPEED				= BETWEEN_MOVE_TIMES.length;
	public final static int			MIN_SPEED				= 1;
	public final static int			RUNNING_STATE_NOT		= 0;
	public final static int			RUNNING_STATE_PAUSE		= 2;
	public final static int			RUNNING_STATE_RUNNING	= 1;
	private static final long		serialVersionUID		= -2204291111959206819L;
	private final static int		startLen				= defStartPos().size();
	private final static Topology	Z_DEF_Topology			= SnakeFrame.TOPOLOGY_PLANE(FIELD_WIDTH, FIELD_HEIGHT);

	public final static List<Position> defStartPos() {
		final List<Position> dSP = new ArrayList<>();
		dSP.add(new Position(FIELD_WIDTH / 2, FIELD_HEIGHT / 2));
		dSP.add(new Position(FIELD_WIDTH / 2 - 1, FIELD_HEIGHT / 2));
		dSP.add(new Position(FIELD_WIDTH / 2 - 2, FIELD_HEIGHT / 2));
		dSP.add(new Position(FIELD_WIDTH / 2 - 3, FIELD_HEIGHT / 2));
		return dSP;
	}

	private final static void paintApple(final Graphics g, final Apple apple) {
		if (g == null || apple == null)
			return;
		if (apple.getColor() == null || apple.getApplePos() == null)
			return;

		final Position pos = apple.getApplePos();
		g.setColor(apple.getColor());
		g.fillOval(pos.getX() * ITEM_SIZE + 1, pos.getY() * ITEM_SIZE + 1, ITEM_SIZE, ITEM_SIZE);
		g.setColor(foreground);
		g.drawOval(pos.getX() * ITEM_SIZE + 1, pos.getY() * ITEM_SIZE + 1, ITEM_SIZE, ITEM_SIZE);
	}

	private final static void paintSnake(final Graphics g, final Snake snake) {
		if (g == null || snake == null)
			return;
		if (snake.getColor() == null || snake.getSnakePos() == null || snake.getHeadSnakePos() == null)
			return;

		final Iterator<Position> it = snake.getSnakePos().iterator();
		Position pos;
		while (it.hasNext()) {
			pos = it.next();
			g.setColor(snake.getColor());
			g.fillRect(pos.getX() * ITEM_SIZE + 1, pos.getY() * ITEM_SIZE + 1, ITEM_SIZE, ITEM_SIZE);
			g.setColor(foreground);
			g.drawRect(pos.getX() * ITEM_SIZE + 1, pos.getY() * ITEM_SIZE + 1, ITEM_SIZE, ITEM_SIZE);
		}
	}

	/**
	 * Picks uniformly from all currently free cells. This stays bounded even
	 * when the board is almost full, unlike rejection sampling.
	 */
	private final static Apple randomApple(final Collection<Position> exclude) {
		if (exclude == null)
			return null;
		Position selected = null;
		int freeCount = 0;
		for (int x = 0; x <= FIELD_WIDTH; x++) {
			for (int y = 0; y <= FIELD_HEIGHT; y++) {
				final Position candidate = new Position(x, y);
				if (exclude.contains(candidate))
					continue;
				freeCount++;
				if (ThreadLocalRandom.current().nextInt(freeCount) == 0)
					selected = candidate;
			}
		}
		return selected == null ? null : new Apple(selected);
	}

	private Apple			apple;
	private boolean			gameWon;
	/** Volatile: written from the EDT, read by the SnakeMoveThread */
	private volatile int	moveTime;
	private SnakeFrame		parentFrame;
	private volatile int	runningState	= RUNNING_STATE_NOT;
	private Snake			snake;
	private Topology		topology;

	public SnakeField() {
		this(new Snake(new Direction(0), defStartPos(), Color.BLUE), randomApple(defStartPos()));
	}

	public SnakeField(final Snake snake, final Apple apple) {
		if (snake == null || apple == null)
			throw new IllegalArgumentException("snake and apple must not be null");
		this.snake = snake;
		this.apple = apple;
		runningState = RUNNING_STATE_NOT;
		topology = Z_DEF_Topology;

		setBackground(background);
		this.setSize((FIELD_WIDTH + 1) * ITEM_SIZE + 2, (FIELD_HEIGHT + 1) * ITEM_SIZE + 2);

		parentFrame = null;
	}

	public final int getMoveTime() {
		return moveTime;
	}

	public final int getRunningState() {
		return runningState;
	}

	public final Snake getSnake() {
		return snake;
	}

	public final Topology getTopology() {
		return topology;
	}

	public final boolean isGameWon() {
		return gameWon;
	}

	public void move() {
		Position headPos = snake.getHeadSnakePos().add(snake.getDirectionAsPos());

		headPos = topology.getLinkFrom(headPos);
		final boolean eating = headPos != null && apple != null && headPos.equals(apple.getApplePos());
		if (headPos == null || snake.wouldCollideAt(headPos, eating)) {
			gameWon = false;
			setRunningState(RUNNING_STATE_NOT);
			setEnabled(false);
		} else if (eating) {
			snake.eatTo(headPos);
			apple = randomApple(snake.getSnakePos());
			if (apple == null) {
				gameWon = true;
				setRunningState(RUNNING_STATE_NOT);
				setEnabled(false);
			}
		} else {
			snake.moveTo(headPos);
		}
		if (parentFrame != null)
			parentFrame.setPoints(snake.getLength() - startLen);

		repaint();
	}

	@Override
	protected void paintComponent(final Graphics g) {
		super.paintComponent(g);
		g.setColor(foreground);
		g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
		paintSnake(g, snake);
		paintApple(g, apple);
		if (!isEnabled()) {
			g.setFont(gameOverFont);
			g.setColor(gameOverColor);
			g.drawString(gameWon ? gameWonText : gameOverText, getWidth() / 2 - gameOverLeftOffset, getHeight() / 2);
		}
	}

	public final void setMoveTime(final int moveTime) {
		this.moveTime = moveTime;
	}

	public final void setParentFrame(final SnakeFrame SF) {
		parentFrame = SF;
	}

	public final void setRunningState(final int runningState) {
		this.runningState = runningState;
	}

	public final void setTime(final int min, final int sec) {
		if (parentFrame != null)
			parentFrame.setTime(min, sec);
	}

	public final void setTopology(final Topology topology) {
		if (topology == null)
			return;
		this.topology = topology;
	}

	public final void switchPause() {
		if (runningState == RUNNING_STATE_RUNNING)
			runningState = RUNNING_STATE_PAUSE;
		else if (runningState == RUNNING_STATE_PAUSE)
			runningState = RUNNING_STATE_RUNNING;
	}

}
