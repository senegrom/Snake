package snake.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import snake.Position;
import snake.topology.Topology;

/** Whole-board visibility and asynchronous native move/resize regressions. */
public final class SnakeViewportTests {
	private static int checks;
	private final JFrame frame;
	private final Robot robot;

	private SnakeViewportTests() throws Exception {
		robot = new Robot();
		robot.setAutoDelay(25);
		SnakeFrame.main(new String[0]);
		await(() -> findFrame() != null, "window appears");
		frame = edt(SnakeViewportTests::findFrame);
		edt(() -> { frame.toFront(); frame.requestFocus(); return null; });
		await(frame::isFocused, "window gains focus");
	}

	public static void main(final String[] args) {
		int result = 0;
		try {
			if (args.length > 1 || args.length == 1 && !List.of("render", "small").contains(args[0]))
				throw new IllegalArgumentException("Expected render, small, or no argument");
			edt(() -> { testFitRendering(); return null; });
			if (args.length == 0 || !"render".equals(args[0])) {
				final SnakeViewportTests tests = new SnakeViewportTests();
				if (args.length == 1) {
					final Rectangle screen = edt(() -> tests.frame.getGraphicsConfiguration().getBounds());
					check(screen.width == 512 && screen.height == 384, "small regression uses 512x384 logical pixels");
				}
				tests.testFitLifecycle();
				tests.testFixedZoomAndRestart();
				tests.testNativeGeometry();
			}
			System.out.println("SnakeViewportTests: " + checks + " checks passed, 0 failed");
		} catch (final Exception | AssertionError failure) {
			result = 1;
			failure.printStackTrace();
		} finally {
			try {
				edt(() -> { for (final Window window : Window.getWindows()) window.dispose(); return null; });
			} catch (final Exception failure) {
				result = 1;
				failure.printStackTrace();
			}
		}
		System.exit(result);
	}

	private static void testFitRendering() {
		for (final Topology topology : Topology.PRESETS) {
			final SnakeField field = new SnakeField();
			field.setTopology(topology);
			field.setFitToWindow(true);
			final List<Position> body = List.copyOf(field.snake().body());
			final Position apple = field.apple();
			// Odd sizes exercise fractional scales and centred letterboxing in both axes.
			for (final Dimension size : List.of(new Dimension(512, 137), new Dimension(317, 231),
					new Dimension(149, 401), new Dimension(980, 781), new Dimension(512, 137))) {
				field.setSize(size);
				final Rectangle panel = new Rectangle(size);
				check(panel.contains(field.boardBounds()), "Fit contains all cells and walls");
				check(panel.contains(field.headBounds()), "Fit contains head");
				final BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
				final Graphics2D graphics = image.createGraphics();
				try {
					field.paint(graphics);
				} finally {
					graphics.dispose();
				}
				boolean blue = false;
				final Rectangle head = field.headBounds();
				for (int y = head.y; y < head.y + head.height; y++)
					for (int x = head.x; x < head.x + head.width; x++) {
						final int rgb = image.getRGB(x, y);
						blue |= (rgb & 255) - ((rgb >> 16) & 255) > 60;
					}
				check(blue, "scaled head is actually drawn at its reported bounds");
				check(body.equals(List.copyOf(field.snake().body())) && apple.equals(field.apple()),
						"Fit never changes game coordinates or apple placement");
			}
			field.setZoom(150);
			check(!field.fitsWindow() && field.zoom() == 150, "fixed zoom exits Fit");
			check(field.getPreferredSize().equals(new Dimension(735, 585)), "fixed zoom retains its dimensions");
			field.shutdown();
		}
	}

	private void testFitLifecycle() throws Exception {
		await(this::wholeBoardVisible, "default Fit shows the entire board before starting");
		check(edt(() -> field().fitsWindow()), "Fit is the initial UI selection");
		edt(() -> { combo("topology").setSelectedItem(Topology.TORUS); return null; });
		final double initialScale = edt(() -> field().viewScale());
		tap(KeyEvent.VK_F2);
		await(() -> field().status() == SnakeField.Status.RUNNING && wholeBoardVisible(),
				"Start exposes whole board, not an empty viewport strip");
		check(edt(() -> !settings().isSelected() && field().viewScale() >= initialScale),
				"starting collapses setup controls and gives the board more room");
		final Position initial = edt(() -> field().snake().head());
		await(() -> !initial.equals(field().snake().head()) && wholeBoardVisible(), "real movement remains visible in Fit");
		tap(KeyEvent.VK_ESCAPE);
		await(() -> field().status() == SnakeField.Status.PAUSED && wholeBoardVisible(), "paused Fit still shows all cells");
		edt(() -> { settings().doClick(0); return null; });
		await(this::wholeBoardVisible, "opening settings refits without cropping the board");
		tap(KeyEvent.VK_F3);
		await(() -> field().status() == SnakeField.Status.READY && field().fitsWindow() && wholeBoardVisible(),
				"Restart preserves Fit and displays new snake with expanded settings");
	}

	private void testFixedZoomAndRestart() throws Exception {
		selectZoom("200%");
		// A deliberately constrained viewport reproduces scrolling even on the larger CI display.
		edt(() -> { frame.setBounds(frame.getX(), frame.getY(), 500, 360); return null; });
		await(() -> field().getVisibleRect().contains(field().headBounds()), "native resize reveals the head");
		settle();
		edt(() -> { scroll().getViewport().setViewPosition(new Point(0, 0)); return null; });
		check(edt(() -> !field().getVisibleRect().contains(field().headBounds())), "fixture really scrolls head off-screen");
		tap(KeyEvent.VK_F2);
		await(() -> field().status() == SnakeField.Status.RUNNING
				&& field().getVisibleRect().contains(field().headBounds()), "Start reveals head in explicit zoom mode");
		tap(KeyEvent.VK_ESCAPE);
		await(() -> field().status() == SnakeField.Status.PAUSED, "pause before scrolled restart");
		edt(() -> { scroll().getViewport().setViewPosition(new Point(0, 0)); return null; });
		final SnakeField old = edt(this::field);
		tap(KeyEvent.VK_F3);
		await(() -> field() != old && field().getVisibleRect().contains(field().headBounds()),
				"Restart discards stale scroll position and shows the new snake");
		check(edt(() -> field().zoom() == 200 && !field().fitsWindow()), "Restart preserves explicit zoom selection");
		selectZoom("150%");
		await(() -> field().getVisibleRect().contains(field().headBounds()), "zoom change reveals head");
		// Select Fit by actual keyboard input, including after a scrolled fixed view.
		edt(() -> { combo("zoom").requestFocusInWindow(); return null; });
		await(() -> combo("zoom").isFocusOwner(), "zoom has keyboard focus");
		tap(KeyEvent.VK_SPACE);
		tap(KeyEvent.VK_END);
		tap(KeyEvent.VK_ENTER);
		await(() -> field().fitsWindow() && wholeBoardVisible(), "keyboard-selected Fit restores whole board");
		check(edt(() -> !scroll().getVerticalScrollBar().isVisible() && !scroll().getHorizontalScrollBar().isVisible()),
				"Fit does not leave stale scrollbars");
	}

	private void testNativeGeometry() throws Exception {
		selectZoom("100%");
		edt(() -> {
			final Rectangle work = workArea();
			frame.setLocation(work.x + work.width - frame.getWidth(), work.y + work.height - frame.getHeight());
			return null;
		});
		settle();
		for (final String zoom : List.of("150%", "200%", "100%", "200%", "Fit")) {
			selectZoom(zoom);
			final Dimension requested = edt(() -> {
				final Dimension preferred = frame.getPreferredSize();
				final Rectangle work = workArea();
				return new Dimension(Math.min(preferred.width, work.width), Math.min(preferred.height, work.height));
			});
			await(() -> frame.getSize().equals(requested) && workArea().contains(frame.getBounds()),
					"combined resize/reposition settles to requested dimensions for " + zoom);
			settle();
			check(edt(() -> frame.getSize().equals(requested) && workArea().contains(frame.getBounds())),
					"native events do not restore an earlier window size for " + zoom);
		}
		await(this::wholeBoardVisible, "Fit follows final settled native geometry");
	}

	private void selectZoom(final String label) throws Exception {
		edt(() -> { combo("zoom").setSelectedItem(label); return null; });
		settle();
	}

	/** A bounded stable interval, not an immediate assertion after an asynchronous native request. */
	private void settle() throws Exception {
		final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		Rectangle previous = null;
		long stableSince = System.nanoTime();
		while (System.nanoTime() < deadline) {
			final Rectangle current = edt(frame::getBounds);
			if (!current.equals(previous)) {
				previous = current;
				stableSince = System.nanoTime();
			} else if (System.nanoTime() - stableSince >= TimeUnit.MILLISECONDS.toNanos(300)) {
				return;
			}
			Thread.sleep(20);
		}
		throw new AssertionError("Native geometry did not settle");
	}

	private boolean wholeBoardVisible() {
		return field().getVisibleRect().contains(field().boardBounds())
				&& field().getVisibleRect().contains(field().headBounds());
	}

	private Rectangle workArea() {
		final Rectangle screen = frame.getGraphicsConfiguration().getBounds();
		final Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(frame.getGraphicsConfiguration());
		return new Rectangle(screen.x + insets.left, screen.y + insets.top,
				screen.width - insets.left - insets.right, screen.height - insets.top - insets.bottom);
	}

	private SnakeField field() { return find(frame, SnakeField.class, null); }
	private JScrollPane scroll() { return find(frame, JScrollPane.class, null); }
	private JToggleButton settings() { return find(frame, JToggleButton.class, "settings"); }
	private JComboBox<?> combo(final String name) { return find(frame, JComboBox.class, name); }

	private void tap(final int key) {
		robot.keyPress(key);
		robot.keyRelease(key);
		robot.delay(75);
	}

	private static JFrame findFrame() {
		return Arrays.stream(Frame.getFrames()).filter(JFrame.class::isInstance).map(JFrame.class::cast)
				.filter(f -> f.isVisible() && "Snake".equals(f.getTitle())).findFirst().orElse(null);
	}

	private static <T extends Component> T find(final Container root, final Class<T> type, final String name) {
		for (final Component child : root.getComponents()) {
			if (type.isInstance(child) && (name == null || name.equals(child.getName())))
				return type.cast(child);
			if (child instanceof Container container) {
				final T found = find(container, type, name);
				if (found != null) return found;
			}
		}
		return null;
	}

	private static <T> T edt(final Callable<T> callable) throws Exception {
		final FutureTask<T> task = new FutureTask<>(callable);
		SwingUtilities.invokeLater(task);
		return task.get(5, TimeUnit.SECONDS);
	}

	private static void await(final Callable<Boolean> condition, final String message) throws Exception {
		final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (!edt(condition)) {
			if (System.nanoTime() >= deadline) throw new AssertionError("Timed out: " + message);
			Thread.sleep(20);
		}
		check(true, message);
	}

	private static void check(final boolean condition, final String message) {
		if (!condition) throw new AssertionError(message);
		checks++;
	}
}
