package snake;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.SequencedSet;

/** Ordered snake body with constant-time occupancy checks. */
public final class Snake {
	private final SequencedSet<Position> body;
	private final SequencedSet<Position> bodyView;
	private Direction direction;
	private Direction heading;

	public Snake(final Direction initialDirection, final Collection<Position> positions) {
		heading = Objects.requireNonNull(initialDirection, "initialDirection");
		direction = initialDirection;
		Objects.requireNonNull(positions, "positions");
		if (positions.isEmpty())
			throw new IllegalArgumentException("A snake must contain at least one position");

		body = LinkedHashSet.newLinkedHashSet(positions.size());
		for (final Position position : positions) {
			Objects.requireNonNull(position, "positions must not contain null");
			if (!body.add(position))
				throw new IllegalArgumentException("Duplicate snake position: " + position);
		}
		bodyView = Collections.unmodifiableSequencedSet(body);
	}

	/**
	 * Advances the snake without mutating it when the destination collides.
	 *
	 * @return whether the move was completed
	 */
	public boolean advanceTo(final Position position, final boolean grow) {
		Objects.requireNonNull(position, "position");
		if (body.contains(position) && (grow || !position.equals(body.getLast())))
			return false;

		if (!grow)
			body.removeLast();
		body.addFirst(position);
		heading = direction;
		return true;
	}

	public SequencedSet<Position> body() {
		return bodyView;
	}

	public boolean contains(final Position position) {
		return body.contains(position);
	}

	/** Direction that will be used by the next step. */
	public Direction direction() {
		return direction;
	}

	public Position head() {
		return body.getFirst();
	}

	public int length() {
		return body.size();
	}

	/**
	 * Queues the direction for the next step. The queued turn may be revised
	 * until the step executes; a reversal of the last completed step is
	 * always rejected, so rapid input cannot fold the snake onto itself.
	 *
	 * @return whether the request is active (or was already active)
	 */
	public boolean requestDirection(final Direction requestedDirection) {
		final Direction requested = Objects.requireNonNull(requestedDirection, "requestedDirection");
		if (requested == direction)
			return true;
		if (requested == heading.opposite())
			return false;
		direction = requested;
		return true;
	}

}
