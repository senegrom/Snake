package snake.topology;

import snake.Position;

/** The four supported rectangular edge identifications. */
public enum Topology {
	PLANE("Plane"),
	TORUS("Torus"),
	KLEIN_BOTTLE("Klein Bottle"),
	PROJECTIVE_PLANE("Projective Plane");

	private final String displayName;

	Topology(final String displayName) {
		this.displayName = displayName;
	}

	/**
	 * Maps the result of one cardinal step from an in-bounds cell.
	 *
	 * @return the mapped board position, or {@code null} when a plane wall was hit
	 */
	public Position map(final Position position, final int columns, final int rows) {
		if (columns <= 0 || rows <= 0)
			throw new IllegalArgumentException("Board dimensions must be positive");

		final boolean outsideX = position.x() < 0 || position.x() >= columns;
		final boolean outsideY = position.y() < 0 || position.y() >= rows;
		if (!outsideX && !outsideY)
			return position;
		if (outsideX && outsideY)
			throw new IllegalArgumentException("A cardinal step cannot cross two board edges: " + position);
		if (position.x() < -1 || position.x() > columns || position.y() < -1 || position.y() > rows)
			throw new IllegalArgumentException("Position is not one step outside the board: " + position);

		final int wrappedX = Math.floorMod(position.x(), columns);
		final int wrappedY = Math.floorMod(position.y(), rows);
		return switch (this) {
		case PLANE -> null;
		case TORUS -> new Position(wrappedX, wrappedY);
		case KLEIN_BOTTLE -> new Position(wrappedX, outsideX ? rows - 1 - wrappedY : wrappedY);
		case PROJECTIVE_PLANE -> new Position(
				outsideY ? columns - 1 - wrappedX : wrappedX,
				outsideX ? rows - 1 - wrappedY : wrappedY);
		};
	}

	@Override
	public String toString() {
		return displayName;
	}
}
