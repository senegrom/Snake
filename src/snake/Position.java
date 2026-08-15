package snake;

/** Immutable board coordinate. */
public record Position(int x, int y) {
	public Position translate(final int deltaX, final int deltaY) {
		return new Position(x + deltaX, y + deltaY);
	}

	@Override
	public String toString() {
		return "(" + x + "," + y + ")";
	}
}
