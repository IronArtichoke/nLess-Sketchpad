package ironartichoke.sketchpad.util;

import ironartichoke.sketchpad.NotepadView;

/**
 * A utility class for mathematical functions.
 * For atan2, see {@link Atan2}.
 */
public final class MathUtils
{
	/**
	 * Converts an X coordinate from screen space to canvas space, incorporating the camera position and zoom.
	 * @param view The <code>NotepadView</code>.
	 * @param x The X coordinate.
	 * @return The corresponding X coordinate in canvas space.
	 */
	public static float convertScreenXToCanvas(NotepadView view, float x)
	{
		return (x - view.viewportWidth/2f) / view.zoom + view.cameraX;
	}

	/**
	 * Converts a Y coordinate from screen space to canvas space, incorporating the camera position and zoom.
	 * @param view The <code>NotepadView</code>.
	 * @param y The Y coordinate.
	 * @return The corresponding Y coordinate in canvas space.
	 */
	public static float convertScreenYToCanvas(NotepadView view, float y)
	{
		// NOTE: The negation is done to invert the direction of the Y axis to match that of OpenGL.
		return -((y - view.viewportHeight/2f) / view.zoom - view.cameraY);
	}
}
