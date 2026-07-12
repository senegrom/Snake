package snake;

import java.lang.reflect.InvocationTargetException;
import javax.swing.SwingUtilities;
import snakeGUI.SnakeField;

public class SnakeMoveThread extends Thread {
	private final SnakeField	fieldPanel;
	private int					millis	= 0;
	private int					minutes	= 0;
	private int					seconds	= 0;

	public SnakeMoveThread(final SnakeField fieldPanel) {
		this.fieldPanel = fieldPanel;
	}

	@Override
	public void run() {
		int rs;
		while ((rs = fieldPanel.getRunningState()) > 0) {
			if (rs == 1) {
				// Run the game step on the EDT: paint iterates the same snake deque
				try {
					SwingUtilities.invokeAndWait(fieldPanel::move);
				} catch (final InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				} catch (final InvocationTargetException e) {
					e.getCause().printStackTrace();
					return;
				}

				// Set Time in the SnakeFrame
				millis += fieldPanel.getMoveTime();
				if (millis >= 1000) {
					millis -= 1000;
					seconds++;
				}
				if (seconds >= 60) {
					seconds -= 60;
					minutes++;
				}
				final int m = minutes, s = seconds;
				SwingUtilities.invokeLater(() -> fieldPanel.setTime(m, s));
			}
			try {
				sleep(fieldPanel.getMoveTime());
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}
	}
}
