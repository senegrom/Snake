package snakeGUI;

import java.awt.EventQueue;
import javax.swing.UIManager;

/**
 * Main class, contains only the start method
 *
 * @author CGH
 */
public class Main {

	static {
		try {
			// Set System L&F
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Main method to start snake
	 *
	 * @param args
	 *            Does not have any effect a.t.m.
	 */
	public static void main(final String[] args) {
		EventQueue.invokeLater(SnakeFrame::new);
	}

}
