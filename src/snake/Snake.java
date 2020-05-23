package snake;

import java.awt.Color;
import java.util.Iterator;
import java.util.Vector;

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
	private Vector<Position>		snakePos;

	public Snake() {
		this(defDir, null, defColor);
	}

	public Snake(final Direction direction, final Vector<Position> snakePos, final Color color) {
		setDirection(direction);
		setSnakePos(snakePos);
		this.color = color;
	}

	public Snake(final Vector<Position> snakePos) {
		this(defDir, snakePos, defColor);
	}

	public final void eat() {
		eatTo(getHeadSnakePos().add(getDirectionAsPos()));
	}

	public final void eatTo(final int x, final int y) {
		eatTo(new Position(x, y));
	}

	public final void eatTo(final Position P) {
		snakePos.add(0, P);
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
		return snakePos.firstElement();
	}

	public final int getLength() {
		return snakePos.size();
	}

	public final Direction getRealDirection() {
		return Direction.getDirectionFromPos(snakePos.get(0).subtract(snakePos.get(1)));
	}

	public final Vector<Position> getSnakePos() {
		return snakePos;
	}

	public final Position getSnakePos(final int i) {
		return snakePos.elementAt(i);
	}

	public final void move() {
		moveTo(getHeadSnakePos().add(getDirectionAsPos()));
	}

	public final void moveTo(final int x, final int y) {
		moveTo(new Position(x, y));
	}

	public final void moveTo(final Position P) {
		snakePos.add(0, P);
		snakePos.remove(snakePos.size() - 1);
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

	public final void setSnakePos(final Vector<Position> snakePos) {
		this.snakePos = snakePos;
	}

	@Override
	public String toString() {
		if (snakePos == null)
			return "";
		String s = "[ ";
		final Iterator<Position> it = snakePos.iterator();
		while (it.hasNext()) {
			s += it.next().toString();
			s += " ";
		}
		s += "]";
		return s;
	}

}
