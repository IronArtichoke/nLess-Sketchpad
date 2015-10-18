package ironartichoke.sketchpad.util;

import ironartichoke.sketchpad.R;

/**
 * A utility class that deals with stroke colors.
 */
public final class ColorUtils
{
	/**
	 * A list of resource IDs for each of the colors in the palette.
	 * Element 14, <code>R.color.white</code>, is not normally accessible from the palette, but
	 * is a special case which is used instead of <code>R.color.black</code> when the
	 * application is in dark mode.
	 */
	public static final int[] COLOR_RES_IDS = new int[]{
			R.color.black,
			R.color.grey,
			R.color.red,
			R.color.orange,
			R.color.yellow,
			R.color.chartreuse,
			R.color.green,
			R.color.spring_green,
			R.color.cyan,
			R.color.sky_blue,
			R.color.blue,
			R.color.purple,
			R.color.magenta,
			R.color.pink,
			R.color.white
	};
	/** The list of RGB values to be used for each color in {@link #COLOR_RES_IDS} by OpenGL. */
	public final static float[][] COLORS = new float[][]{
			{0.0f, 0.0f, 0.0f, 1.0f}, // Black         #000000
			{0.5f, 0.5f, 0.5f, 1.0f}, // Grey          #7F7F7F
			{1.0f, 0.0f, 0.0f, 1.0f}, // Red           #FF0000
			{1.0f, 0.5f, 0.0f, 1.0f}, // Orange        #FF7F00
			{1.0f, 1.0f, 0.0f, 1.0f}, // Yellow        #FFFF00
			{0.5f, 1.0f, 0.0f, 1.0f}, // Chartreuse    #7FFF00
			{0.0f, 1.0f, 0.0f, 1.0f}, // Green         #00FF00
			{0.0f, 1.0f, 0.5f, 1.0f}, // Spring Green  #00FF7F
			{0.0f, 1.0f, 1.0f, 1.0f}, // Cyan          #00FFFF
			{0.0f, 0.5f, 1.0f, 1.0f}, // Sky Blue      #007FFF
			{0.0f, 0.0f, 1.0f, 1.0f}, // Blue          #0000FF
			{0.5f, 0.0f, 1.0f, 1.0f}, // Purple        #7F00FF
			{1.0f, 0.0f, 1.0f, 1.0f}, // Magenta       #FF00FF
			{1.0f, 0.0f, 0.5f, 1.0f}, // Pink          #FF007F
			{1.0f, 1.0f, 1.0f, 1.0f}  // White         #FFFFFF
	};
	/** A list of the IDs for the color palette buttons corresponding to {@link #COLOR_RES_IDS}. */
	private final static int[] COLOR_PALETTE_BUTTON_IDS = new int[]{
			R.id.imageview_color_palette_black,
			R.id.imageview_color_palette_grey,
			R.id.imageview_color_palette_red,
			R.id.imageview_color_palette_orange,
			R.id.imageview_color_palette_yellow,
			R.id.imageview_color_palette_chartreuse,
			R.id.imageview_color_palette_green,
			R.id.imageview_color_palette_spring_green,
			R.id.imageview_color_palette_cyan,
			R.id.imageview_color_palette_sky_blue,
			R.id.imageview_color_palette_blue,
			R.id.imageview_color_palette_purple,
			R.id.imageview_color_palette_magenta,
			R.id.imageview_color_palette_pink
	};

	/**
	 * Converts a color byte (between 0 and 13) to the corresponding color resource ID.
	 * @param colorByte A color.
	 * @return The corresponding color resource ID.
	 */
	public static int getColorResId(byte colorByte)
	{
		if (colorByte == 0)
		{
			// Return white if dark mode is active.
			if (AppearanceUtils.isThemeDark())
			{
				return R.color.white;
			}
			// Return black if light mode is active.
			else
			{
				return R.color.black;
			}
		}
		else
		{
			return COLOR_RES_IDS[colorByte];
		}
	}

	/**
	 * Gets the corresponding color palette button ID for the given color (between 0 and 13).
	 * @param color A color.
	 * @return The color palette button ID.
	 */
	public static int getColorPaletteButtonId(int color)
	{
		return COLOR_PALETTE_BUTTON_IDS[color];
	}
}
