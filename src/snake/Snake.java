package snake;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Class representing the snake as an object.
 *
 * @author CGH
 */

public class Snake {

	private final static Color		defColor	= Color.BLUE;
	private final static Direction	defDir		= new Direction(0);
	private Color					color;

	private Direction				direction;
	private Deque<Position>			snakePos;
	private final Set<Position>		snakeSet	= new HashSet<>();

	public Snake() {
		this(defDir, null, defColor);
	}

	public Snake(final Direction direction, final Collection<Position> snakePos, final Color color) {
		setDirection(direction);
		setSnakePos(snakePos);
		this.color = color;
	}

	public Snake(final Collection<Position> snakePos) {
		this(defDir, snakePos, defColor);
	}

	public final boolean contains(final Position p) {
		return snakeSet.contains(p);
	}

	public final void eat() {
		eatTo(getHeadSnakePos().add(getDirectionAsPos()));
	}

	public final void eatTo(final int x, final int y) {
		eatTo(new Position(x, y));
	}

	public final void eatTo(final Position P) {
		snakePos.addFirst(P);
		snakeSet.add(P);
	}

	public final Color getColor() {
		return color;
	}

	public final Direction getDirection() {
		return direction;
	}

	public final Position getDirectionAsPos() {
		return direction.getDirectionAsPos();
	}

	public final Position getHeadSnakePos() {
		return (snakePos == null || snakePos.isEmpty()) ? null : snakePos.getFirst();
	}

	public final int getLength() {
		return snakePos == null ? 0 : snakePos.size();
	}

	public final Direction getRealDirection() {
		if (snakePos == null || snakePos.size() < 2)
			return direction;
		final Iterator<Position> it = snakePos.iterator();
		final Position head = it.next();
		final Position neck = it.next();
		return Direction.getDirectionFromPos(head.subtract(neck));
	}

	public final Collection<Position> getSnakePos() {
		return snakePos;
	}

	public final void move() {
		moveTo(getHeadSnakePos().add(getDirectionAsPos()));
	}

	public final void moveTo(final int x, final int y) {
		moveTo(new Position(x, y));
	}

	public final void moveTo(final Position P) {
		final Position tail = snakePos.removeLast();
		snakeSet.remove(tail);
		snakePos.addFirst(P);
		snakeSet.add(P);
	}

	public final void setColor(final Color color) {
		this.color = color;
	}

	public final void setDirection(final Direction direction) {
		this.direction = direction;
	}

	public final void setDirection(final int d) {
		direction = new Direction(d);
	}

	public final void setSnakePos(final Collection<Position> snakePos) {
		this.snakeSet.clear();
		if (snakePos == null) {
			this.snakePos = null;
			return;
		}
		this.snakePos = new ArrayDeque<>(snakePos);
		this.snakeSet.addAll(snakePos);
	}

	@Override
	public String toString() {
		if (snakePos == null)
			return "";
		final StringBuilder sb = new StringBuilder("[ ");
		for (final Position p : snakePos) {
			sb.append(p.toString()).append(' ');
		}
		sb.append(']');
		return sb.toString();
	}

}
