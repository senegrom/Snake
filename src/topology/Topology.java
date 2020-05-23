package topology;

import java.util.HashMap;
import snake.Position;

/**
 * Class representing a game topology on which Snake can be played An empty
 * Topology represents the normal plane.
 *
 * @author CGH
 */

public class Topology {
	/**
	 * First position indicates field outside (must be unique), second position
	 * the linked field on the game field.
	 */
	protected HashMap<Position, Position>	linkedFields;
	private String							name;
	protected int							xSize, ySize;

	public Topology(final int xSize, final int ySize) {
		this(xSize, ySize, null);
	}

	public Topology(final int xSize, final int ySize, final String name) {
		if (xSize <= 0 || ySize <= 0)
			return;
		this.xSize = xSize;
		this.ySize = ySize;
		this.name = name;
		linkedFields = new HashMap<>();
	}

	public final HashMap<Position, Position> getLinkedFields() {
		return linkedFields;
	}

	public final Position getLinkFrom(Position p) {
		if (p == null)
			return null;
		if (linkedFields != null && linkedFields.containsKey(p))
			return linkedFields.get(p);

		int x, y;
		x = p.getX();
		y = p.getY();

		if (x < 0)
			p = xS(p);
		if (y > ySize)
			p = yL(p);
		if (x > xSize)
			p = xL(p);
		if (y < 0)
			p = yS(p);

		return p;
	}

	public final String getName() {
		return name;
	}

	public final int getxSize() {
		return xSize;
	}

	public final int getySize() {
		return ySize;
	}

	/*
	 * public final void setLinkedFields(HashMap<Position, Position>
	 * linkedFields) { this.linkedFields = linkedFields; }
	 */
	public final void setLink(final Position pKey, final Position pValue) {
		if (linkedFields == null)
			return;
		linkedFields.put(pKey, pValue);
	}

	public final void setName(final String name) {
		this.name = name;
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
