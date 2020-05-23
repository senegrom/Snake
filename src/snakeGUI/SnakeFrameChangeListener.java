package snakeGUI;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

class SnakeFrameChangeListener implements ChangeListener {
	private final SnakeFrame sf;

	public SnakeFrameChangeListener(final SnakeFrame sf) {
		super();
		this.sf = sf;
	}

	@Override
	public void stateChanged(final ChangeEvent e) {
		if (e.getSource() == sf.getSliSpeed()) {
			final int i = sf.getSliSpeed().getValue();
			sf.getLblSpeedDisp().setText(Integer.toString(i));
			sf.getFieldPanel().setMoveTime(SnakeField.BETWEEN_MOVE_TIMES[i - 1]);
		}
	}
}
