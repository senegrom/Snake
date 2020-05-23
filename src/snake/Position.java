package snake;

import java.util.Random;

/**
 * Class which represents a position on the game board.
 *
 * @author CGH
 */
public class Position {
	private final static int defPos = 0;

	public final static Position randomPos(final int xmin, final int xmax, final int ymin, final int ymax) {
		final Random rand = new Random();
		return new Position(xmin + rand.nextInt(xmax - xmin + 1), ymin + rand.nextInt(ymax - ymin + 1));
	}

	private int	x;

	private int	y;

	public Position() {
		this(defPos, defPos);
	}

	public Position(final int x, final int y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * @param x
	 * @param y
	 * @return Adds the position (x,y) to the current position
	 */
	public final Position add(final int x, final int y) {
		return add(new Position(x, y));
	}

	/**
	 * @param P
	 * @return Adds position P to the current position
	 */
	public final Position add(final Position P) {
		return new Position(x + P.getX(), y + P.getY());
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final Position other = (Position) obj;
		if (x != other.x)
			return false;
		if (y != other.y)
			return false;
		return true;
	}

	public final int getX() {
		return x;
	}

	public final int getY() {
		return y;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + x;
		result = prime * result + y;
		return result;
	}

	public final Position scalarProd(final int d) {
		return new Position(d * x, d * y);
	}

	public final void setPosition(final int x, final int y) {
		this.x = x;
		this.y = y;
	}

	public final void setX(final int x) {
		this.x = x;
	}

	public final void setY(final int y) {
		this.y = y;
	}

	public final Position subtract(final Position P) {
		return new Position(x - P.getX(), y - P.getY());
	}

	@Override
	public String toString() {
		return "(" + x + "," + y + ")";
	}

}
