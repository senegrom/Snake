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

	private Color				color;

	public Apple() {
		this(null, defColor);
	}

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

	public final void setColor(final Color color) {
		this.color = color;
	}

	@Override
	public String toString() {
		if (applePos == null)
			return "";
		return "[ " + applePos.toString() + " ]";
	}

}
