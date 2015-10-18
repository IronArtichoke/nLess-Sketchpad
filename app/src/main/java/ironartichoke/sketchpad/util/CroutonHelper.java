package ironartichoke.sketchpad.util;

import android.app.Activity;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

/**
 * A helper class for displaying croutons.
 * This class is intended to be mocked during testing.
 */
public class CroutonHelper
{
	/**
	 * @see Crouton#showText(Activity, int, Style)
	 */
	public void show(Activity activity, int textId, Style style)
	{
		Crouton.showText(activity, textId, style);
	}
}
