package snake;

/** The four movement directions. Screen coordinates grow downwards. */
public enum Direction {
	RIGHT(1, 0), DOWN(0, 1), LEFT(-1, 0), UP(0, -1);

	private final int deltaX;
	private final int deltaY;

	Direction(final int deltaX, final int deltaY) {
		this.deltaX = deltaX;
		this.deltaY = deltaY;
	}

	public Position move(final Position position) {
		return new Position(position.x() + deltaX, position.y() + deltaY);
	}

	Direction opposite() {
		return switch (this) {
		case RIGHT -> LEFT;
		case DOWN -> UP;
		case LEFT -> RIGHT;
		case UP -> DOWN;
		};
	}
}
