package snake;

import java.awt.Color;

/**
 * Class representing an apple as abstract object.
 *
 * @author CGH
 */

public class Apple {

	private final static Color	defColor	= Color.RED;
	private Position			applePos;

	private final Color			color;

	public Apple(final Position applePos) {
		this(applePos, defColor);
	}

	public Apple(final Position applePos, final Color color) {
		setApplePos(applePos);
		this.color = color;
	}

	public final Position getApplePos() {
		return applePos;
	}

	public final Color getColor() {
		return color;
	}

	public final void setApplePos(final Position applePos) {
		this.applePos = applePos;
	}

}
