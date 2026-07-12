package topology;

import snake.Position;

/**
 * Class representing a game topology on which Snake can be played An empty
 * Topology represents the normal plane.
 *
 * @author CGH
 */

public class Topology {
	private String	name;
	protected int	xSize, ySize;

	public Topology(final int xSize, final int ySize, final String name) {
		if (xSize <= 0 || ySize <= 0)
			return;
		this.xSize = xSize;
		this.ySize = ySize;
		this.name = name;
	}

	public final Position getLinkFrom(final Position p) {
		if (p == null)
			return null;
		// At most one coordinate can be out of bounds after a single step,
		// so return immediately instead of chaining stale checks
		if (p.getX() < 0)
			return xS(p);
		if (p.getY() > ySize)
			return yL(p);
		if (p.getX() > xSize)
			return xL(p);
		if (p.getY() < 0)
			return yS(p);
		return p;
	}

	public final String getName() {
		return name;
	}

	@Override
	public final String toString() {
		return getName();
	}

	protected Position xL(final Position p) {
		return null;
	}

	protected Position xS(final Position p) {
		return null;
	}

	protected Position yL(final Position p) {
		return null;
	}

	protected Position yS(final Position p) {
		return null;
	}
}
