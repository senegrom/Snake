package topology;

import snake.Position;

/**
 * Topology representing an empty plane. Correctors are made final.
 *
 * @author CGH
 */

public class Plane extends Topology {
	public Plane(final int xSize, final int ySize, final String name) {
		super(xSize, ySize, name);
		linkedFields = null;
	}

	@Override
	final protected Position xL(final Position p) {
		return null;
	}

	@Override
	final protected Position xS(final Position p) {
		return null;
	}

	@Override
	final protected Position yL(final Position p) {
		return null;
	}

	@Override
	final protected Position yS(final Position p) {
		return null;
	}
}
