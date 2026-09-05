package snake.topology;

import java.util.List;
import java.util.Objects;
import snake.Position;

/**
 * A surface obtained by identifying opposite edges of the rectangular board:
 * {@code horizontal} governs the left and right edges, {@code vertical} the
 * top and bottom edges. Every combination is a valid surface; rotated
 * variants (for example a cylinder glued top to bottom) share their name.
 */
public record Topology(Gluing horizontal, Gluing vertical) {
	public static final Topology PLANE = new Topology(Gluing.WALL, Gluing.WALL);
	public static final Topology CYLINDER = new Topology(Gluing.WRAP, Gluing.WALL);
	public static final Topology MOBIUS_BAND = new Topology(Gluing.FLIP, Gluing.WALL);
	public static final Topology TORUS = new Topology(Gluing.WRAP, Gluing.WRAP);
	public static final Topology KLEIN_BOTTLE = new Topology(Gluing.FLIP, Gluing.WRAP);
	public static final Topology PROJECTIVE_PLANE = new Topology(Gluing.FLIP, Gluing.FLIP);
	/** The six distinct surfaces, in order of increasing strangeness. */
	public static final List<Topology> PRESETS = List.of(
			PLANE, CYLINDER, MOBIUS_BAND, TORUS, KLEIN_BOTTLE, PROJECTIVE_PLANE);

	public Topology {
		Objects.requireNonNull(horizontal, "horizontal");
		Objects.requireNonNull(vertical, "vertical");
	}

	/**
	 * Maps the result of one cardinal step from an in-bounds cell.
	 *
	 * @return the mapped board position, or {@code null} when a wall was hit
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

		if (outsideX)
			return switch (horizontal) {
			case WALL -> null;
			case WRAP -> new Position(Math.floorMod(position.x(), columns), position.y());
			case FLIP -> new Position(Math.floorMod(position.x(), columns), rows - 1 - position.y());
			};
		return switch (vertical) {
		case WALL -> null;
		case WRAP -> new Position(position.x(), Math.floorMod(position.y(), rows));
		case FLIP -> new Position(columns - 1 - position.x(), Math.floorMod(position.y(), rows));
		};
	}

	/** Describes the actual edge rules, including rotated variants. */
	public String description() {
		return "Left/right edges " + describe(horizontal, "rows")
				+ "; top/bottom edges " + describe(vertical, "columns") + ".";
	}

	private static String describe(final Gluing gluing, final String reflectedAxis) {
		return switch (gluing) {
		case WALL -> "are walls";
		case WRAP -> "wrap normally";
		case FLIP -> "wrap and reflect " + reflectedAxis;
		};
	}

	/** The surface's conventional name; rotated variants share it. */
	@Override
	public String toString() {
		final int walls = count(Gluing.WALL);
		final int flips = count(Gluing.FLIP);
		if (walls == 2)
			return "Plane";
		if (walls == 1)
			return flips == 1 ? "Möbius Band" : "Cylinder";
		return switch (flips) {
		case 0 -> "Torus";
		case 1 -> "Klein Bottle";
		default -> "Projective Plane";
		};
	}

	private int count(final Gluing gluing) {
		return (horizontal == gluing ? 1 : 0) + (vertical == gluing ? 1 : 0);
	}
}
