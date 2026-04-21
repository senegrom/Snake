package topology;

import snake.Position;

/**
 * Topology representing a Klein-Bottle. Correctors are made final.
 *
 * @author CGH
 */

public class KleinBottle extends Topology {
	public KleinBottle(final int xSize, final int ySize, final String name) {
		super(xSize, ySize, name);
		linkedFields = null;
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
