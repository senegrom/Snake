package snakeGUI;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JOptionPane;
import snake.SnakeMoveThread;
import topology.Topology;

class SnakeFrameActionListener implements ActionListener {
	private final SnakeFrame sf;

	public SnakeFrameActionListener(final SnakeFrame sf) {
		super();
		this.sf = sf;
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		if (e.getSource() == sf.getBtnExit())
			System.exit(0);
		else if (e.getSource() == sf.getBtnAbout())
			sf.showMessage(SnakeFrame.aboutText, SnakeFrame.frmAboutTitle, JOptionPane.INFORMATION_MESSAGE);
		else if (e.getSource() == sf.getBtnPause()) {
			sf.getFieldPanel().switchPause();
			sf.requestFocus();
		} else if (e.getSource() == sf.getBtnRestart()) {
			sf.getFieldPanel().setRunningState(SnakeField.RUNNING_STATE_NOT);
			sf.dispose();
			new SnakeFrame();
		} else if (e.getSource() == sf.getBtnStart()) {
			final SnakeField fieldPanel = sf.getFieldPanel();
			fieldPanel.setRunningState(1);
			new SnakeMoveThread(fieldPanel).start();
			sf.getBtnStart().setEnabled(false);
			sf.getSliSpeed().setEnabled(false);
			sf.getCmbTopology().setEnabled(false);
			sf.requestFocus();
		} else if (e.getSource() == sf.getCmbTopology())
			sf.getFieldPanel().setTopology((Topology) sf.getCmbTopology().getSelectedItem());
	}
}
