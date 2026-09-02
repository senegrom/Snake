package snake.topology;

/** How one pair of opposite board edges is identified. */
public enum Gluing {
	/** The edges are solid walls. */
	WALL,
	/** Crossing an edge re-enters from the opposite edge at the same offset. */
	WRAP,
	/** Crossing an edge re-enters from the opposite edge at the mirrored offset. */
	FLIP
}
