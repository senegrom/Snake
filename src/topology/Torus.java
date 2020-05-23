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
		linkedFields = null;
	}

	@Override
	final protected Position xL(final Position p) {
		int x = p.getX();
		final int y = p.getY();

		while (x > xSize)
			x -= xSize + 1;

		return new Position(x, y);
	}

	@Override
	final protected Position xS(final Position p) {
		int x = p.getX();
		final int y = p.getY();

		while (x < 0)
			x += xSize + 1;

		return new Position(x, y);
	}

	@Override
	final protected Position yL(final Position p) {
		final int x = p.getX();
		int y = p.getY();

		while (y > ySize)
			y -= ySize + 1;

		return new Position(x, y);
	}

	@Override
	final protected Position yS(final Position p) {
		final int x = p.getX();
		int y = p.getY();

		while (y < 0)
			y += ySize + 1;

		return new Position(x, y);
	}
}
