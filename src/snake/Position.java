package snake;

import java.util.Random;

/**
 * Class which represents a position on the game board.
 *
 * @author CGH
 */
public class Position {
	private final static Random rand = new Random();

	public final static Position randomPos(final int xmin, final int xmax, final int ymin, final int ymax) {
		return new Position(xmin + rand.nextInt(xmax - xmin + 1), ymin + rand.nextInt(ymax - ymin + 1));
	}

	/** Immutable: positions are used as HashSet keys */
	private final int	x;

	private final int	y;

	public Position(final int x, final int y) {
		this.x = x;
		this.y = y;
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

	public final Position subtract(final Position P) {
		return new Position(x - P.getX(), y - P.getY());
	}

	@Override
	public String toString() {
		return "(" + x + "," + y + ")";
	}

}
