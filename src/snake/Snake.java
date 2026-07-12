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

	private Color				color;

	private Direction			direction;
	private Deque<Position>		snakePos;
	private final Set<Position>	snakeSet	= new HashSet<>();

	public Snake(final Direction direction, final Collection<Position> snakePos, final Color color) {
		setDirection(direction);
		setSnakePos(snakePos);
		this.color = color;
	}

	public final boolean contains(final Position p) {
		return snakeSet.contains(p);
	}

	public final void eatTo(final Position P) {
		snakePos.addFirst(P);
		snakeSet.add(P);
	}

	public final Color getColor() {
		return color;
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
		// After a wrap move head-neck is not a unit vector; fall back to the
		// commanded direction instead of returning null
		final Direction d = Direction.getDirectionFromPos(head.subtract(neck));
		return d != null ? d : direction;
	}

	public final Collection<Position> getSnakePos() {
		return snakePos;
	}

	public final void moveTo(final Position P) {
		final Position tail = snakePos.removeLast();
		snakeSet.remove(tail);
		snakePos.addFirst(P);
		snakeSet.add(P);
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

}
