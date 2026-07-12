package topology;

import snake.Position;

/**
 * Topology representing the real projective plane (RP^2): both pairs of
 * opposite edges are glued with a flip (fundamental polygon abab). Correctors
 * are made final.
 *
 * @author CGH
 */

public class ProjectivePlane extends Topology {
	public ProjectivePlane(final int xSize, final int ySize, final String name) {
		super(xSize, ySize, name);
	}

	@Override
	final protected Position xL(final Position p) {
		return new Position(Math.floorMod(p.getX(), xSize + 1), ySize - p.getY());
	}

	@Override
	final protected Position xS(final Position p) {
		return new Position(Math.floorMod(p.getX(), xSize + 1), ySize - p.getY());
	}

	@Override
	final protected Position yL(final Position p) {
		return new Position(xSize - p.getX(), Math.floorMod(p.getY(), ySize + 1));
	}

	@Override
	final protected Position yS(final Position p) {
		return new Position(xSize - p.getX(), Math.floorMod(p.getY(), ySize + 1));
	}
}
