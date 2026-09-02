package snake.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.swing.SwingUtilities;
import snake.Direction;
import snake.Position;
import snake.Snake;
import snake.topology.Gluing;
import snake.topology.Topology;
import static snake.gui.TestSupport.check;
import static snake.gui.TestSupport.equal;
import static snake.gui.TestSupport.expect;

/** Dependency-free regression and exhaustive property tests. */
public final class SnakeTests {
	private SnakeTests() {
	}

	public static void main(final String[] args) {
		System.setProperty("java.awt.headless", "true");
		System.exit(TestSupport.run("SnakeTests", SnakeTests::runAll));
	}

	private static void runAll() {
		check(SwingUtilities.isEventDispatchThread(), "Swing-facing tests run on the EDT");
		testPositionAndDirection();
		testTopologyExamples();
		testTopologyProperties();
		testSnakeValidationAndMovement();
		testFieldValidationAndLifecycle();
		testFieldCollisions();
		testFieldEatingAndWinning();
		testFieldTopologiesAndInput();
	}

	private static List<Position> almostFullBody(final Position head, final Position... freePositions) {
		final List<Position> free = Arrays.asList(freePositions);
		final List<Position> body = new ArrayList<>(SnakeField.CELL_COUNT - free.size());
		body.add(head);
		for (int y = 0; y < SnakeField.BOARD_ROWS; y++) {
			for (int x = 0; x < SnakeField.BOARD_COLUMNS; x++) {
				final Position position = new Position(x, y);
				if (!position.equals(head) && !free.contains(position))
					body.add(position);
			}
		}
		return body;
	}

	/** Every combination of edge gluings, including the rotated duplicates of the presets. */
	private static List<Topology> allTopologies() {
		final List<Topology> topologies = new ArrayList<>();
		for (final Gluing horizontal : Gluing.values())
			for (final Gluing vertical : Gluing.values())
				topologies.add(new Topology(horizontal, vertical));
		return topologies;
	}

	private static boolean isInside(final Position position, final int columns, final int rows) {
		return position.x() >= 0 && position.x() < columns
				&& position.y() >= 0 && position.y() < rows;
	}

	private static Direction opposite(final Direction direction) {
		return switch (direction) {
		case RIGHT -> Direction.LEFT;
		case DOWN -> Direction.UP;
		case LEFT -> Direction.RIGHT;
		case UP -> Direction.DOWN;
		};
	}

	private static Snake ringSnake(final Direction direction) {
		return new Snake(direction,
				List.of(new Position(0, 1), new Position(1, 1), new Position(1, 0), new Position(0, 0)));
	}

	private static void testPositionAndDirection() {
		final Position position = new Position(1, 2);
		equal(1, position.x(), "record x accessor");
		equal(2, position.y(), "record y accessor");
		equal(new Position(1, 2), new Position(1, 2), "record value equality");

		for (final Direction direction : Direction.values())
			equal(position, opposite(direction).move(direction.move(position)),
					"opposite movement round trip for " + direction);
		equal(new Position(2, 2), Direction.RIGHT.move(position), "right movement");
		equal(new Position(1, 3), Direction.DOWN.move(position), "down movement");
		expect(NullPointerException.class, () -> Direction.UP.move(null),
				"direction movement rejects null");
	}

	private static void testTopologyExamples() {
		final int columns = 6;
		final int rows = 4;
		final Position inside = new Position(3, 2);
		for (final Topology topology : allTopologies())
			equal(inside, topology.map(inside, columns, rows), topology + " keeps in-bounds positions");

		equal(null, Topology.PLANE.map(new Position(6, 1), columns, rows), "plane has a right wall");
		equal(null, Topology.PLANE.map(new Position(1, -1), columns, rows), "plane has a top wall");
		equal(new Position(0, 2), Topology.TORUS.map(new Position(6, 2), columns, rows),
				"torus wraps right to left");
		equal(new Position(5, 2), Topology.TORUS.map(new Position(-1, 2), columns, rows),
				"torus wraps left to right");
		equal(new Position(2, 0), Topology.TORUS.map(new Position(2, 4), columns, rows),
				"torus wraps bottom to top");
		equal(new Position(2, 3), Topology.TORUS.map(new Position(2, -1), columns, rows),
				"torus wraps top to bottom");
		equal(new Position(0, 1), Topology.KLEIN_BOTTLE.map(new Position(6, 2), columns, rows),
				"Klein bottle right crossing reflects y");
		equal(new Position(5, 2), Topology.KLEIN_BOTTLE.map(new Position(-1, 1), columns, rows),
				"Klein bottle left crossing reflects y");
		equal(new Position(2, 0), Topology.KLEIN_BOTTLE.map(new Position(2, 4), columns, rows),
				"Klein bottle vertical crossing wraps plainly");
		equal(new Position(0, 1), Topology.PROJECTIVE_PLANE.map(new Position(6, 2), columns, rows),
				"projective plane right crossing reflects y");
		equal(new Position(3, 0), Topology.PROJECTIVE_PLANE.map(new Position(2, 4), columns, rows),
				"projective plane bottom crossing reflects x");
		equal(new Position(3, 3), Topology.PROJECTIVE_PLANE.map(new Position(2, -1), columns, rows),
				"projective plane top crossing reflects x");

		expect(NullPointerException.class, () -> Topology.TORUS.map(null, columns, rows),
				"topology rejects null positions");
		expect(IllegalArgumentException.class, () -> Topology.TORUS.map(inside, 0, rows),
				"topology rejects zero columns");
		expect(IllegalArgumentException.class, () -> Topology.TORUS.map(inside, columns, -1),
				"topology rejects negative rows");
		expect(IllegalArgumentException.class,
				() -> Topology.TORUS.map(new Position(-1, -1), columns, rows),
				"topology rejects a two-edge crossing");
		expect(IllegalArgumentException.class,
				() -> Topology.TORUS.map(new Position(7, 1), columns, rows),
				"topology rejects positions more than one step outside");
		equal("Klein Bottle", Topology.KLEIN_BOTTLE.toString(), "topology display name");

		equal(new Position(0, 2), Topology.MOBIUS_BAND.map(new Position(6, 1), columns, rows),
				"Möbius band right crossing reflects y");
		equal(null, Topology.MOBIUS_BAND.map(new Position(2, 4), columns, rows), "Möbius band has a bottom wall");
		equal(new Position(0, 2), Topology.CYLINDER.map(new Position(6, 2), columns, rows),
				"cylinder wraps right to left");
		equal(null, Topology.CYLINDER.map(new Position(2, -1), columns, rows), "cylinder has a top wall");
		equal(Topology.KLEIN_BOTTLE, new Topology(Gluing.FLIP, Gluing.WRAP), "topologies are value objects");
		equal("Klein Bottle", new Topology(Gluing.WRAP, Gluing.FLIP).toString(),
				"a rotated Klein bottle keeps its name");
		equal("Cylinder", new Topology(Gluing.WALL, Gluing.WRAP).toString(), "a rotated cylinder keeps its name");
		equal("Möbius Band", new Topology(Gluing.WALL, Gluing.FLIP).toString(),
				"a rotated Möbius band keeps its name");
		equal(List.of("Plane", "Cylinder", "Möbius Band", "Torus", "Klein Bottle", "Projective Plane"),
				Topology.PRESETS.stream().map(Topology::toString).toList(),
				"presets cover the six surfaces in order");
		expect(NullPointerException.class, () -> new Topology(null, Gluing.WRAP),
				"topology rejects a null horizontal gluing");
		expect(NullPointerException.class, () -> new Topology(Gluing.WRAP, null),
				"topology rejects a null vertical gluing");
	}

	private static void testTopologyProperties() {
		for (int columns = 1; columns <= 6; columns++) {
			for (int rows = 1; rows <= 5; rows++) {
				for (final Topology topology : allTopologies()) {
					for (int y = 0; y < rows; y++) {
						for (int x = 0; x < columns; x++) {
							final Position origin = new Position(x, y);
							for (final Direction direction : Direction.values()) {
								final Position stepped = direction.move(origin);
								final Position mapped = topology.map(stepped, columns, rows);
								final String context = topology + " " + columns + "x" + rows + " "
										+ origin + " " + direction;
								Gluing crossed = null;
								if (stepped.x() < 0 || stepped.x() >= columns)
									crossed = topology.horizontal();
								else if (stepped.y() < 0 || stepped.y() >= rows)
									crossed = topology.vertical();
								if (crossed == Gluing.WALL) {
									check(mapped == null, context + " stops at a wall");
									continue;
								}
								if (mapped == null) {
									check(false, context + " unexpectedly mapped to null");
									continue;
								}
								check(isInside(mapped, columns, rows), context + " maps inside the board");
								final Position returned = topology.map(opposite(direction).move(mapped),
										columns, rows);
								check(origin.equals(returned), context + " is reversible");
							}
						}
					}
				}
			}
		}
	}

	private static void testSnakeValidationAndMovement() {
		expect(NullPointerException.class,
				() -> new Snake(null, List.of(new Position(0, 0))), "snake rejects null direction");
		expect(NullPointerException.class,
				() -> new Snake(Direction.RIGHT, null), "snake rejects null body");
		expect(IllegalArgumentException.class,
				() -> new Snake(Direction.RIGHT, List.of()), "snake rejects an empty body");
		expect(IllegalArgumentException.class,
				() -> new Snake(Direction.RIGHT, List.of(new Position(0, 0), new Position(0, 0))),
				"snake rejects duplicate body positions");
		expect(NullPointerException.class,
				() -> new Snake(Direction.RIGHT, Arrays.asList(new Position(0, 0), null)),
				"snake rejects null body positions");

		final Snake snake = ringSnake(Direction.RIGHT);
		final List<Position> initialBody = List.copyOf(snake.body());
		equal(Direction.RIGHT, snake.direction(), "initial direction");
		equal(4, snake.length(), "snake length follows ordered body");
		equal(initialBody.getFirst(), snake.head(), "first body element is the head");
		equal(initialBody.reversed(), List.copyOf(snake.body().reversed()), "reversed body order is exposed");
		expect(UnsupportedOperationException.class, () -> snake.body().clear(),
				"body view cannot be mutated");
		expect(UnsupportedOperationException.class, () -> snake.body().reversed().clear(),
				"reversed body view cannot be mutated");
		expect(NullPointerException.class, () -> snake.advanceTo(null, false),
				"movement rejects a null destination");
		expect(NullPointerException.class, () -> snake.requestDirection(null),
				"direction requests reject null");

		for (final Direction direction : Direction.values()) {
			final Snake directed = new Snake(direction, List.of(new Position(0, 0)));
			check(!directed.requestDirection(opposite(direction)),
					"direct reversal is rejected for " + direction);
		}

		check(!snake.requestDirection(Direction.LEFT), "direct reversal is rejected");
		check(snake.requestDirection(Direction.UP), "a perpendicular turn is accepted");
		check(!snake.requestDirection(Direction.LEFT),
				"a revised turn may not reverse the last completed step");
		check(snake.requestDirection(Direction.DOWN), "a queued turn may be revised before the step");
		check(snake.requestDirection(Direction.UP), "a revision may be revised again");
		check(snake.requestDirection(Direction.UP), "repeating the pending direction is harmless");
		final Position oldTail = snake.body().getLast();
		check(snake.advanceTo(oldTail, false), "head may enter the departing tail");
		equal(oldTail, snake.head(), "head may enter the tail cell vacated on the same step");
		equal(4, snake.length(), "non-growing movement preserves length");
		equal(snake.length(), Set.copyOf(snake.body()).size(), "body remains duplicate-free");
		check(snake.requestDirection(Direction.LEFT), "a new turn is accepted after movement");
		check(!snake.requestDirection(Direction.DOWN), "rapid input cannot create a hidden reversal");

		final Snake growing = new Snake(Direction.RIGHT,
				List.of(new Position(2, 1), new Position(1, 1), new Position(1, 0), new Position(0, 0)));
		final var liveBodyView = growing.body();
		check(growing.advanceTo(new Position(3, 1), true), "growing movement succeeds on a free cell");
		equal(5, growing.length(), "growing movement retains the tail");
		equal(5, liveBodyView.size(), "body view remains live after movement");
		final List<Position> beforeCollision = List.copyOf(growing.body());
		expect(NullPointerException.class, () -> growing.advanceTo(null, false),
				"movement rejects a null destination");
		check(!growing.advanceTo(new Position(3, 1), true),
				"growth collision is rejected");
		check(!growing.advanceTo(new Position(2, 1), false),
				"body collision is rejected");
		equal(beforeCollision, List.copyOf(growing.body()),
				"rejected movement leaves the body unchanged");
	}

	private static void testFieldValidationAndLifecycle() {
		expect(NullPointerException.class, () -> new SnakeField(null, new Position(0, 0)),
				"field rejects a null snake");
		expect(NullPointerException.class, () -> new SnakeField(ringSnake(Direction.UP), null),
				"field rejects a null apple");
		expect(IllegalArgumentException.class,
				() -> new SnakeField(ringSnake(Direction.UP), new Position(0, 0)),
				"field rejects an apple on the snake, including the tail");
		expect(IllegalArgumentException.class,
				() -> new SnakeField(ringSnake(Direction.UP), new Position(-1, 0)),
				"field rejects an out-of-bounds apple");
		expect(IllegalArgumentException.class,
				() -> new SnakeField(new Snake(Direction.RIGHT, List.of(new Position(-1, 0))),
						new Position(2, 0)),
				"field rejects an out-of-bounds snake");
		expect(IllegalArgumentException.class,
				() -> new SnakeField(new Snake(Direction.RIGHT, almostFullBody(new Position(0, 0))),
						new Position(0, 0)),
				"field rejects a board with no free cell");

		final SnakeField field = new SnakeField();
		equal(SnakeField.Status.READY, field.status(), "default field starts ready");
		check(!field.snake().contains(field.apple()), "default apple is on a free cell");
		check(!field.pauseGame(), "a ready game cannot be paused");
		check(!field.resumeGame(), "a ready game cannot be resumed");
		expect(IllegalArgumentException.class, () -> field.setMoveDelay(0),
				"zero move delay is rejected");
		expect(NullPointerException.class, () -> field.setTopology(null), "null topology is rejected");
		field.setTopology(Topology.TORUS);
		field.setMoveDelay(100_000);
		equal(100_000, field.moveDelay(), "positive move delay is accepted");

		field.startGame();
		equal(SnakeField.Status.RUNNING, field.status(), "start enters running state");
		expect(IllegalStateException.class, field::startGame, "a game cannot start twice");
		expect(IllegalStateException.class, () -> field.setMoveDelay(30),
				"speed is locked after start");
		expect(IllegalStateException.class, () -> field.setTopology(Topology.PLANE),
				"topology is locked after start");
		field.togglePause();
		equal(SnakeField.Status.PAUSED, field.status(), "pause toggle enters paused state");
		field.togglePause();
		equal(SnakeField.Status.RUNNING, field.status(), "pause toggle resumes the game");
		check(field.pauseGame(), "running game pauses directly");
		check(field.resumeGame(), "paused game resumes directly");
		field.shutdown();
		field.shutdown();
		equal(SnakeField.Status.FINISHED, field.status(), "shutdown is idempotent");
		check(!field.requestDirection(Direction.UP), "finished field rejects direction input");
	}

	private static void testFieldCollisions() {
		final Snake wallSnake = new Snake(Direction.RIGHT,
				List.of(new Position(SnakeField.BOARD_COLUMNS - 1, 15),
						new Position(SnakeField.BOARD_COLUMNS - 2, 15),
						new Position(SnakeField.BOARD_COLUMNS - 3, 15)));
		final SnakeField wallField = new SnakeField(wallSnake, new Position(10, 10));
		wallField.step();
		equal(SnakeField.Status.FINISHED, wallField.status(), "wall collision finishes the game");
		check(!wallField.won(), "wall collision is not a win");
		final Position deadHead = wallSnake.head();
		wallField.step();
		equal(deadHead, wallSnake.head(), "steps after game over do nothing");

		final SnakeField neckField = new SnakeField(ringSnake(Direction.RIGHT), new Position(10, 10));
		neckField.step();
		equal(SnakeField.Status.FINISHED, neckField.status(), "running into the neck ends the game");

		final Snake tailChaser = ringSnake(Direction.UP);
		final SnakeField tailField = new SnakeField(tailChaser, new Position(10, 10));
		tailField.step();
		equal(new Position(0, 0), tailChaser.head(), "field permits movement into the departing tail");
		equal(4, tailChaser.length(), "tail chase preserves length");
		equal(SnakeField.Status.READY, tailField.status(), "legal manual step remains playable");
	}

	private static void testFieldEatingAndWinning() {
		final Snake snake = new Snake(Direction.RIGHT,
				List.of(new Position(2, 1), new Position(1, 1), new Position(1, 0), new Position(0, 0)));
		final SnakeField field = new SnakeField(snake, new Position(3, 1));
		field.step();
		equal(5, snake.length(), "eating grows the snake");
		equal(new Position(3, 1), snake.head(), "head occupies the eaten apple");
		check(field.apple() != null, "a new apple is spawned");
		check(!snake.contains(field.apple()), "the new apple is on a free cell");

		final Position eaten = new Position(0, 0);
		final Position soleFreeCell = new Position(2, 0);
		final Snake nearlyFull = new Snake(Direction.LEFT,
				almostFullBody(new Position(1, 0), eaten, soleFreeCell));
		equal(SnakeField.CELL_COUNT - 2, nearlyFull.length(), "near-full fixture length");
		final SnakeField nearFullField = new SnakeField(nearlyFull, eaten);
		nearFullField.step();
		equal(soleFreeCell, nearFullField.apple(), "apple selection finds the only free cell");

		final Snake winningSnake = new Snake(Direction.LEFT,
				almostFullBody(new Position(1, 0), eaten));
		equal(SnakeField.CELL_COUNT - 1, winningSnake.length(), "winning fixture length");
		final SnakeField winningField = new SnakeField(winningSnake, eaten);
		winningField.step();
		check(winningField.won(), "filling the board wins");
		equal(SnakeField.Status.FINISHED, winningField.status(), "win finishes the game");
		equal(null, winningField.apple(), "no apple remains after a win");
		equal(SnakeField.CELL_COUNT, winningSnake.length(), "winning snake fills every cell");
	}

	private static void testFieldTopologiesAndInput() {
		final Snake wrappingSnake = new Snake(Direction.RIGHT,
				List.of(new Position(SnakeField.BOARD_COLUMNS - 1, 15),
						new Position(SnakeField.BOARD_COLUMNS - 2, 15),
						new Position(SnakeField.BOARD_COLUMNS - 3, 15)));
		final SnakeField wrappingField = new SnakeField(wrappingSnake, new Position(10, 10));
		wrappingField.setTopology(Topology.TORUS);
		wrappingField.step();
		equal(new Position(0, 15), wrappingSnake.head(), "torus wraps across the right edge");
		check(wrappingField.requestDirection(Direction.UP), "turn after a wrap is accepted");
		check(!wrappingField.requestDirection(Direction.LEFT),
				"rapid second turn after a wrap cannot create a hidden reversal");
		wrappingField.step();
		equal(new Position(0, 14), wrappingSnake.head(), "accepted post-wrap turn is applied");
		equal(SnakeField.Status.READY, wrappingField.status(), "post-wrap input remains playable");

		for (final Topology topology : List.of(Topology.MOBIUS_BAND, Topology.KLEIN_BOTTLE,
				Topology.PROJECTIVE_PLANE)) {
			final Snake topologicalSnake = new Snake(Direction.RIGHT,
					List.of(new Position(SnakeField.BOARD_COLUMNS - 1, 10),
							new Position(SnakeField.BOARD_COLUMNS - 2, 10)));
			final SnakeField topologicalField = new SnakeField(topologicalSnake, new Position(5, 5));
			topologicalField.setTopology(topology);
			topologicalField.step();
			equal(new Position(0, SnakeField.BOARD_ROWS - 1 - 10), topologicalSnake.head(),
					topology + " reflects the row across a left/right edge");
		}

		final Snake projectiveSnake = new Snake(Direction.UP,
				List.of(new Position(5, 0), new Position(5, 1), new Position(5, 2)));
		final SnakeField projectiveField = new SnakeField(projectiveSnake, new Position(10, 10));
		projectiveField.setTopology(Topology.PROJECTIVE_PLANE);
		projectiveField.step();
		equal(new Position(SnakeField.BOARD_COLUMNS - 1 - 5, SnakeField.BOARD_ROWS - 1),
				projectiveSnake.head(), "projective plane reflects the column across a top/bottom edge");

		final Snake cylinderSnake = new Snake(Direction.UP,
				List.of(new Position(5, 0), new Position(5, 1), new Position(5, 2)));
		final SnakeField cylinderField = new SnakeField(cylinderSnake, new Position(10, 10));
		cylinderField.setTopology(Topology.CYLINDER);
		cylinderField.step();
		equal(SnakeField.Status.FINISHED, cylinderField.status(), "cylinder keeps its top wall");
	}
}
