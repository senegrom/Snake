package snake;

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
		while ((rs = fieldPanel.getRunningState()) > 0)
			if (rs == 1) {
				fieldPanel.move();
				try {
					sleep(fieldPanel.getMoveTime());
				} catch (final InterruptedException e) {}

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
				fieldPanel.setTime(minutes, seconds);
			} else if (rs == 2)
				try {
					sleep(fieldPanel.getMoveTime());
				} catch (final InterruptedException e) {}
	}
}
