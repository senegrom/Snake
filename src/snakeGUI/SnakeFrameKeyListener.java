package snakeGUI;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import snake.Snake;

class SnakeFrameKeyListener implements KeyListener {

	private final static int	keyDown		= KeyEvent.VK_DOWN;
	private final static int	keyLeft		= KeyEvent.VK_LEFT;
	private final static int	keyPause	= KeyEvent.VK_SPACE;
	private final static int	keyRight	= KeyEvent.VK_RIGHT;
	private final static int	keyUp		= KeyEvent.VK_UP;

	private final SnakeField	sf;
	private final Snake			snake;

	public SnakeFrameKeyListener(final SnakeField sf) {
		snake = sf.getSnake();
		this.sf = sf;
	}

	@Override
	public void keyPressed(final KeyEvent e) {
		try {
			final int keycode = e.getKeyCode();
			final int dir = snake.getRealDirection().getDirection();
			switch (keycode) {
			case keyRight:
				if (dir != 2)
					snake.setDirection(0);
				break;
			case keyDown:
				if (dir != 3)
					snake.setDirection(1);
				break;
			case keyLeft:
				if (dir != 0)
					snake.setDirection(2);
				break;
			case keyUp:
				if (dir != 1)
					snake.setDirection(3);
				break;
			case keyPause:
				sf.switchPause();
				break;
			}
		} catch (final NullPointerException ex) {}
	}

	@Override
	public void keyReleased(final KeyEvent e) {}

	@Override
	public void keyTyped(final KeyEvent e) {}

}