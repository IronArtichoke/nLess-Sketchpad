package ironartichoke.sketchpad.util;

import ironartichoke.sketchpad.Prefs;
import ironartichoke.sketchpad.R;

/**
 * A utility class for dealing with the application appearance (light and dark mode, right- and left-handed mode).
 */
public final class AppearanceUtils
{
	/** The constant for light mode. */
	public static final int THEME_LIGHT = 0;
	/** The constant for dark mode. */
	private static final int THEME_DARK  = ~THEME_LIGHT;

	/** The constant for right-handed mode. */
	public static final int RIGHT_HANDED = 0;
	/** The constant for left-handed mode. */
	private static final int LEFT_HANDED = ~RIGHT_HANDED;

	/**
	 * Retrieves the current theme.
	 * @return Either {@link #THEME_LIGHT} or {@link #THEME_DARK}.
	 */
	private static int getTheme()
	{
		return Prefs.getInt(R.string.pref_brightness);
	}

	/**
	 * Toggles between light and dark modes.
	 */
	public static void toggleTheme()
	{
		Prefs.edit().putInt(R.string.pref_brightness, ~Prefs.getInt(R.string.pref_brightness)).commit();
	}

	/**
	 * Checks if the application is currently in light mode.
	 * @return True if light mode is active; false otherwise.
	 */
	public static boolean isThemeLight()
	{
		return getTheme() == THEME_LIGHT;
	}

	/**
	 * Checks if the application is currently in dark mode.
	 * @return True if dark mode is active; false otherwise.
	 */
	public static boolean isThemeDark()
	{
		return getTheme() == THEME_DARK;
	}

	/**
	 * Retrieves the current handedness.
	 * @return Either {@link #RIGHT_HANDED} or {@link #LEFT_HANDED}.
	 */
	private static int getHandedness()
	{
		return Prefs.getInt(R.string.pref_handedness);
	}

	/**
	 * Toggles between right- and left-handed modes.
	 */
	public static void toggleHandedness()
	{
		Prefs.edit().putInt(R.string.pref_handedness, ~Prefs.getInt(R.string.pref_handedness)).commit();
	}

	/**
	 * Checks if the application is currently in right-handed mode.
	 * @return True if right-handed mode is active; false otherwise.
	 */
	public static boolean isRightHanded()
	{
		return getHandedness() == RIGHT_HANDED;
	}

	/**
	 * Checks if the application is currently in left-handed mode.
	 * @return True if left-handed mode is active; false otherwise.
	 */
	public static boolean isLeftHanded()
	{
		return getHandedness() == LEFT_HANDED;
	}
}
