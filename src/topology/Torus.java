package topology;

import snake.Position;

/**
 * Topology representing a Torus (T2). Correctors are made final.
 *
 * @author CGH
 */

public class Torus extends Topology {
	public Torus(final int xSize, final int ySize, final String name) {
		super(xSize, ySize, name);
	}

	@Override
	final protected Position xL(final Position p) {
		return new Position(Math.floorMod(p.getX(), xSize + 1), p.getY());
	}

	@Override
	final protected Position xS(final Position p) {
		return new Position(Math.floorMod(p.getX(), xSize + 1), p.getY());
	}

	@Override
	final protected Position yL(final Position p) {
		return new Position(p.getX(), Math.floorMod(p.getY(), ySize + 1));
	}

	@Override
	final protected Position yS(final Position p) {
		return new Position(p.getX(), Math.floorMod(p.getY(), ySize + 1));
	}
}
