package snake;

/**
 * Class which represents a direction on the gameboard.
 *
 * @author CGH
 */
public class Direction {
	public final static Position	A_DIRECTION_0	= new Position(1, 0);
	public final static Position	A_DIRECTION_1	= new Position(0, 1);
	public final static Position	A_DIRECTION_2	= new Position(-1, 0);
	public final static Position	A_DIRECTION_3	= new Position(0, -1);
	public final static Position	DIR_DOWN		= A_DIRECTION_1;
	public final static Position	DIR_LEFT		= A_DIRECTION_2;
	public final static Position	DIR_RIGHT		= A_DIRECTION_0;
	public final static Position	DIR_UP			= A_DIRECTION_3;
	private final static int		maxDir			= 3;
	private final static int		minDir			= 0;

	public final static Direction getDirectionFromPos(final Position P) {
		if (P == null)
			return null;
		if (P.equals(A_DIRECTION_0))
			return new Direction(0);
		if (P.equals(A_DIRECTION_1))
			return new Direction(1);
		if (P.equals(A_DIRECTION_2))
			return new Direction(2);
		if (P.equals(A_DIRECTION_3))
			return new Direction(3);
		return null;
	}

	private int	direction;

	public Direction() {
		this(minDir);
	}

	public Direction(final int direction) {
		if (direction >= minDir && direction <= maxDir)
			this.direction = direction;
		else
			this.direction = minDir;
	}

	public final int getDirection() {
		return direction;
	}

	public final Position getDirectionAsPos() {
		switch (direction) {
		case 0:
			return A_DIRECTION_0;
		case 1:
			return A_DIRECTION_1;
		case 2:
			return A_DIRECTION_2;
		case 3:
			return A_DIRECTION_3;
		default:
			return null;
		}
	}

	public final void setDirection(final int direction) {
		if (direction >= minDir && direction <= maxDir)
			this.direction = direction;
		else
			this.direction = minDir;
	}

}
