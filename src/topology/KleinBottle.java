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
		int x = p.getX();
		int y = p.getY();

		while (x > xSize)
			x -= (xSize + 1);
		y = ySize - y;

		return new Position(x, y);
	}

	@Override
	final protected Position xS(final Position p) {
		int x = p.getX();
		int y = p.getY();

		while (x < 0)
			x += (xSize + 1);
		y = ySize - y;

		return new Position(x, y);
	}

	@Override
	final protected Position yL(final Position p) {
		int x = p.getX();
		int y = p.getY();

		while (y > ySize)
			y -= (ySize + 1);
		x = xSize - x;

		return new Position(x, y);
	}

	@Override
	final protected Position yS(final Position p) {
		int x = p.getX();
		int y = p.getY();

		while (y < 0)
			y += (ySize + 1);
		x = xSize - x;

		return new Position(x, y);
	}
}
