import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import snake.Apple;
import snake.Direction;
import snake.Position;
import snake.Snake;
import snakeGUI.SnakeField;
import topology.KleinBottle;
import topology.Plane;
import topology.ProjectivePlane;
import topology.Torus;

/** Lightweight regression tests with no third-party test dependency. */
public final class AllTests {
	private static int assertions;

	private AllTests() {}

	public static void main(final String[] args) {
		testPositionValueSemantics();
		testDirections();
		testTopologies();
		testSnakeCollisionRules();
		testSnakeBodyIsReadOnly();
		testTailCellMoveIsLegal();
		testBodyCollisionEndsGame();
		testFullBoardEndsAsWin();
		System.out.println("All tests passed (" + assertions + " assertions).");
	}

	private static void testPositionValueSemantics() {
		final Position p = new Position(3, 4);
		assertEquals(new Position(4, 2), p.add(new Position(1, -2)), "position addition");
		assertEquals(new Position(2, 6), p.subtract(new Position(1, -2)), "position subtraction");
		assertEquals(new Position(3, 4), p, "position equality");
		assertEquals(new Position(3, 4).hashCode(), p.hashCode(), "equal positions hash equally");
	}

	private static void testDirections() {
		assertEquals(new Position(1, 0), new Direction(0).getDirectionAsPos(), "right direction");
		assertEquals(new Position(0, 1), new Direction(1).getDirectionAsPos(), "down direction");
		assertEquals(new Position(-1, 0), new Direction(2).getDirectionAsPos(), "left direction");
		assertEquals(new Position(0, -1), new Direction(3).getDirectionAsPos(), "up direction");
		assertEquals(2, Direction.getDirectionFromPos(new Position(-1, 0)).getDirection(), "direction lookup");
		assertEquals(null, Direction.getDirectionFromPos(new Position(2, 0)), "non-unit vector has no direction");
	}

	private static void testTopologies() {
		final Plane plane = new Plane(4, 3, "Plane");
		assertEquals(null, plane.getLinkFrom(new Position(-1, 1)), "plane has a wall");
		assertEquals(new Position(2, 2), plane.getLinkFrom(new Position(2, 2)), "plane leaves interior unchanged");

		final Torus torus = new Torus(4, 3, "Torus");
		assertEquals(new Position(4, 1), torus.getLinkFrom(new Position(-1, 1)), "torus left wrap");
		assertEquals(new Position(0, 1), torus.getLinkFrom(new Position(5, 1)), "torus right wrap");
		assertEquals(new Position(2, 3), torus.getLinkFrom(new Position(2, -1)), "torus top wrap");
		assertEquals(new Position(2, 0), torus.getLinkFrom(new Position(2, 4)), "torus bottom wrap");

		final KleinBottle klein = new KleinBottle(4, 3, "Klein Bottle");
		assertEquals(new Position(4, 2), klein.getLinkFrom(new Position(-1, 1)), "Klein horizontal wrap mirrors y");
		assertEquals(new Position(1, 3), klein.getLinkFrom(new Position(1, -1)), "Klein vertical wrap does not mirror x");

		final ProjectivePlane projective = new ProjectivePlane(4, 3, "Projective Plane");
		assertEquals(new Position(4, 2), projective.getLinkFrom(new Position(-1, 1)), "projective horizontal wrap mirrors y");
		assertEquals(new Position(3, 3), projective.getLinkFrom(new Position(1, -1)), "projective vertical wrap mirrors x");
	}

	private static void testSnakeCollisionRules() {
		final List<Position> body = Arrays.asList(
				new Position(1, 1), new Position(1, 2), new Position(0, 2), new Position(0, 1));
		final Snake snake = new Snake(new Direction(2), body, Color.BLUE);
		assertFalse(snake.wouldCollideAt(new Position(0, 1), false), "vacating tail is a legal destination");
		assertTrue(snake.wouldCollideAt(new Position(0, 1), true), "tail is occupied while growing");
		assertTrue(snake.wouldCollideAt(new Position(1, 2), false), "non-tail body cell collides");
		assertFalse(snake.wouldCollideAt(new Position(5, 5), false), "free cell does not collide");
	}

	private static void testSnakeBodyIsReadOnly() {
		final Snake snake = new Snake(new Direction(0), Arrays.asList(new Position(1, 1), new Position(0, 1)), Color.BLUE);
		final Collection<Position> body = snake.getSnakePos();
		boolean threw = false;
		try {
			body.clear();
		} catch (final UnsupportedOperationException expected) {
			threw = true;
		}
		assertTrue(threw, "callers cannot desynchronise the body and collision set");
		assertEquals(2, snake.getLength(), "failed external mutation leaves snake intact");
	}

	private static void testTailCellMoveIsLegal() {
		final Snake snake = new Snake(new Direction(2), Arrays.asList(
				new Position(1, 1), new Position(1, 2), new Position(0, 2), new Position(0, 1)), Color.BLUE);
		final SnakeField field = new SnakeField(snake, new Apple(new Position(10, 10)));
		field.setRunningState(SnakeField.RUNNING_STATE_RUNNING);
		field.move();
		assertEquals(SnakeField.RUNNING_STATE_RUNNING, field.getRunningState(), "moving into the old tail keeps the game running");
		assertEquals(new Position(0, 1), snake.getHeadSnakePos(), "head moves into old tail cell");
		assertEquals(4, snake.getLength(), "normal move preserves length");
	}

	private static void testBodyCollisionEndsGame() {
		final Snake snake = new Snake(new Direction(2), Arrays.asList(
				new Position(1, 1), new Position(1, 2), new Position(0, 2), new Position(0, 1), new Position(0, 0)), Color.BLUE);
		final SnakeField field = new SnakeField(snake, new Apple(new Position(10, 10)));
		field.setRunningState(SnakeField.RUNNING_STATE_RUNNING);
		field.move();
		assertEquals(SnakeField.RUNNING_STATE_NOT, field.getRunningState(), "body collision stops the game");
		assertFalse(field.isEnabled(), "body collision disables the field");
		assertEquals(new Position(1, 1), snake.getHeadSnakePos(), "collision does not mutate the body");
	}

	private static void testFullBoardEndsAsWin() {
		final Position lastFreeCell = new Position(1, 0);
		final List<Position> body = new ArrayList<>();
		for (int x = 0; x <= SnakeField.FIELD_WIDTH; x++) {
			for (int y = 0; y <= SnakeField.FIELD_HEIGHT; y++) {
				final Position p = new Position(x, y);
				if (!p.equals(lastFreeCell))
					body.add(p);
			}
		}
		final Snake snake = new Snake(new Direction(0), body, Color.BLUE);
		final SnakeField field = new SnakeField(snake, new Apple(lastFreeCell));
		field.setRunningState(SnakeField.RUNNING_STATE_RUNNING);
		field.move();
		assertTrue(field.isGameWon(), "eating the final free cell wins instead of hanging apple placement");
		assertEquals(SnakeField.RUNNING_STATE_NOT, field.getRunningState(), "win stops the move loop");
		assertEquals((SnakeField.FIELD_WIDTH + 1) * (SnakeField.FIELD_HEIGHT + 1), snake.getLength(), "winning snake fills the board");
	}

	private static void assertTrue(final boolean condition, final String message) {
		assertions++;
		if (!condition)
			throw new AssertionError(message);
	}

	private static void assertFalse(final boolean condition, final String message) {
		assertTrue(!condition, message);
	}

	private static void assertEquals(final Object expected, final Object actual, final String message) {
		assertions++;
		if (expected == null ? actual != null : !expected.equals(actual))
			throw new AssertionError(message + ": expected " + expected + ", got " + actual);
	}
}
