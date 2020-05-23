package snakeGUI;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import topology.KleinBottle;
import topology.Plane;
import topology.Topology;
import topology.Torus;

/**
 * Game Frame for Snake
 *
 * @author CGH
 * @version 0.1.2
 */

class SnakeFrame {
	public final static String	A_VERSION				= "0.1.2";
	public final static String	aboutText				= "Snake " + A_VERSION + " by CGH.";
	private final static String	btnAboutText			= "About";
	private final static String	btnExitText				= "Exit";
	private final static String	btnPauseText			= "Pause";
	private final static String	btnRestartText			= "Restart";
	private final static String	btnStartText			= "Start";
	private final static String	DblZeroPlaceHolder		= "00";
	private final static String	frameTitle				= "Snake";
	public final static int		frameWidth				= 600;
	public final static String	frmAboutTitle			= "About";
	private final static String	lblPointsText			= "Points ";
	private final static String	lblSpeedText			= "Speed";
	private final static String	lblTimerText			= "Time ";
	private final static int	snakeFrameSizeOffset	= 3;
	private final static String	timerSeperator			= ":";
	private final static int	topHeightExtra			= 5;
	private final static String	ZeroPlaceHolder			= "0";

	public final static Topology TOPOLOGY_KLEINBTL(final int fieldWidth, final int fieldHeight) {
		return new KleinBottle(fieldWidth, fieldHeight, "Klein Bottle");
	}

	public final static Topology TOPOLOGY_PLANE(final int fieldWidth, final int fieldHeight) {
		return new Plane(fieldWidth, fieldHeight, "Plane");
	}

	public final static Topology TOPOLOGY_TORUS(final int fieldWidth, final int fieldHeight) {
		return new Torus(fieldWidth, fieldHeight, "Torus (Beam Through Walls)");
	}

	private final JButton				btnAbout;
	private final JButton				btnExit;
	private final JButton				btnPause;
	private final JButton				btnRestart;
	private final JButton				btnStart;
	private final JComboBox<Topology>	cmbTopology;
	private final SnakeField			fieldPanel;
	private final JLabel				lblPoints;
	private final JLabel				lblSpeedDisp;
	private final JLabel				lblTimer;
	private final JFrame				myFrame;
	private final JSlider				sliSpeed;
	private final JPanel				topPanel;
	private final JPanel				topPanel2;

	/**
	 * Default constructor for SnakeFrame
	 */
	public SnakeFrame() {
		myFrame = new JFrame(frameTitle);
		int snakeHeight, topHeight;

		fieldPanel = new SnakeField();
		fieldPanel.setMoveTime(SnakeField.BETWEEN_MOVE_TIMES[SnakeField.DEF_SPEED - 1]);
		snakeHeight = fieldPanel.getHeight();
		topPanel = new JPanel();
		topPanel2 = new JPanel();
		btnStart = new JButton(btnStartText);
		btnExit = new JButton(btnExitText);
		btnRestart = new JButton(btnRestartText);
		btnPause = new JButton(btnPauseText);
		btnAbout = new JButton(btnAboutText);
		sliSpeed = new JSlider(SwingConstants.HORIZONTAL, SnakeField.MIN_SPEED, SnakeField.MAX_SPEED, SnakeField.DEF_SPEED);
		sliSpeed.setPaintTicks(true);
		sliSpeed.setMinorTickSpacing(1);
		sliSpeed.setSnapToTicks(true);
		lblSpeedDisp = new JLabel(ZeroPlaceHolder);
		lblPoints = new JLabel(lblPointsText + ZeroPlaceHolder);
		lblTimer = new JLabel(lblTimerText + ZeroPlaceHolder + timerSeperator + DblZeroPlaceHolder);

		final int fieldWidth = SnakeField.FIELD_WIDTH;
		final int fieldHeight = SnakeField.FIELD_HEIGHT;
		cmbTopology = new JComboBox<>();
		final Topology topPlane = TOPOLOGY_PLANE(fieldWidth, fieldHeight);
		final Topology topTorus = TOPOLOGY_TORUS(fieldWidth, fieldHeight);
		final Topology topKleinBtl = TOPOLOGY_KLEINBTL(fieldWidth, fieldHeight);
		cmbTopology.addItem(topPlane);
		cmbTopology.addItem(topTorus);
		cmbTopology.addItem(topKleinBtl);

		topPanel.setLayout(new GridLayout(1, 0));
		topPanel2.setLayout(new FlowLayout());
		topPanel.add(btnStart);
		topPanel.add(btnExit);
		topPanel.add(btnRestart);
		topPanel.add(btnPause);
		topPanel.add(btnAbout);
		topPanel.add(lblPoints);
		topPanel.add(lblTimer);

		topPanel2.add(new JLabel(lblSpeedText));
		topPanel2.add(lblSpeedDisp);
		topPanel2.add(sliSpeed);
		topPanel2.add(cmbTopology);

		topPanel.setSize(topPanel.getMinimumSize());
		topHeight = topPanel.getHeight() + topHeightExtra;
		topPanel.setSize(frameWidth, topHeight);
		topPanel2.setSize(topPanel.getSize());

		myFrame.setLayout(null);
		myFrame.add(topPanel);
		topPanel.setLocation(0, 0);
		myFrame.add(topPanel2);
		topPanel2.setLocation(0, topHeight + 1);
		myFrame.add(fieldPanel);
		fieldPanel.setLocation((frameWidth - fieldPanel.getWidth()) / 2, 2 * (topHeight + 1));
		fieldPanel.setParentFrame(this);

		myFrame.setResizable(false);
		myFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		final SnakeFrameActionListener snakeFrameAL = new SnakeFrameActionListener(this);
		final SnakeFrameChangeListener snakeFrameCL = new SnakeFrameChangeListener(this);

		btnStart.addActionListener(snakeFrameAL);
		btnExit.addActionListener(snakeFrameAL);
		btnRestart.addActionListener(snakeFrameAL);
		btnPause.addActionListener(snakeFrameAL);
		btnAbout.addActionListener(snakeFrameAL);
		sliSpeed.addChangeListener(snakeFrameCL);
		cmbTopology.addActionListener(snakeFrameAL);
		myFrame.addKeyListener(new SnakeFrameKeyListener(getFieldPanel()));

		myFrame.setVisible(true);
		myFrame.setSize(frameWidth,
				2 * topHeight + snakeHeight + myFrame.getInsets().top + myFrame.getInsets().bottom + snakeFrameSizeOffset);
		myFrame.setLocationRelativeTo(null);
		lblSpeedDisp.setText(Integer.toString(sliSpeed.getValue()));
	}

	public final void dispose() {
		myFrame.setVisible(false);
		myFrame.dispose();
	}

	public final JButton getBtnAbout() {
		return btnAbout;
	}

	public final JButton getBtnExit() {
		return btnExit;
	}

	public final JButton getBtnPause() {
		return btnPause;
	}

	public final JButton getBtnRestart() {
		return btnRestart;
	}

	public final JButton getBtnStart() {
		return btnStart;
	}

	public final JComboBox<Topology> getCmbTopology() {
		return cmbTopology;
	}

	/**
	 * @return The SnakeField displaying the game
	 */
	public final SnakeField getFieldPanel() {
		return fieldPanel;
	}

	public final JLabel getLblPoints() {
		return lblPoints;
	}

	public final JLabel getLblSpeedDisp() {
		return lblSpeedDisp;
	}

	public final JLabel getLblTimer() {
		return lblTimer;
	}

	public final JSlider getSliSpeed() {
		return sliSpeed;
	}

	public final JPanel getTopPanel() {
		return topPanel;
	}

	public final JPanel getTopPanel2() {
		return topPanel2;
	}

	public final void requestFocus() {
		myFrame.requestFocus();
	}

	public final void setPoints(final int i) {
		lblPoints.setText(lblPointsText + i);
	}

	public final void setTime(final int min, final int sec) {
		String s;
		if (sec < 10)
			s = ZeroPlaceHolder + sec;
		else
			s = String.valueOf(sec);
		lblTimer.setText(lblTimerText + min + timerSeperator + s);
	}

	public final void showMessage(final String text, final String title, final int type) {
		JOptionPane.showMessageDialog(myFrame, text, title, type);
	}

}