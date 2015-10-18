package ironartichoke.sketchpad.util;

/**
 * A utility class for working with chunk IDs.
 * <br\><br\>
 * Chunk IDs are unique, one-to-one, and are derived from a chunk's XY position on the grid.
 * The chunk's X and Y positions are packed into a single long and can be unpacked to return its coordinates.
 * <br\><br\>
 * A chunk ID is defined using the following formula, so that the X coordinate is represented by the first 32 bits
 * and the Y coordinate is represented by the last 32 bits:
 * <pre>
 *     chunkId := (x <<<< 32) + y
 * </pre>
 * Then it can be converted back to coordinates like so:
 * <pre>
 *     x := (chunkId & 0x ffffffff 00000000) >> 32
 *     y :=  chunkId & 0x 00000000 ffffffff
 * </pre>
 */
public final class ChunkUtils
{
	/** The width and height of a chunk in pixels. */
	private static final int CHUNK_SIZE = 4000;
	/** The constant for converting from the canvas coordinate system (with origin at 0,0) to the internal system. */
	private static final int COORD_OFFSET = 32767;
	/** The distance, in screen-widths or screen-heights, that the camera may be far away enough from a chunk to
	 * unload it or close enough to a chunk to load it. */
	public static final float LOADING_DISTANCE_FACTOR = 0.2f;

	/**
	 * Returns the containing chunk of a location on the canvas.
	 * A stroke whose center is (x,y) will be stored in this chunk.
	 * @param x The X canvas component.
	 * @param y The Y canvas component.
	 * @return The packed chunk value.
	 */
	public static long convertCoordsToChunk(int x, int y)
	{
		return pack(convertXCoord(x), convertYCoord(y));
	}

	/**
	 * Converts the X coordinate from the zero-origin system to the internal system.
	 * This method and {@link #convertYCoord(int)} are actually identical in function, but have different names for clarity.
	 * @param x The X coordinate.
	 * @return The X coordinate offset by {@link #COORD_OFFSET}.
	 */
	public static int convertXCoord(int x)
	{
		return x / CHUNK_SIZE - (x < 0 ? 1 : 0) + COORD_OFFSET;
	}

	/**
	 * Converts the Y coordinate from the zero-origin system to the internal system.
	 * This method and {@link #convertXCoord(int)} are actually identical in function, but have different names for clarity.
	 * @param y The Y coordinate.
	 * @return The Y coordinate offset by {@link #COORD_OFFSET}.
	 */
	public static int convertYCoord(int y)
	{
		return y / CHUNK_SIZE - (y < 0 ? 1 : 0) + COORD_OFFSET;
	}

	/**
	 * Combines the given chunk coordinates into a unique long.
	 * @param x The first coordinate (0 <= x <= 65535).
	 * @param y The second coordinate (0 <= y <= 65535).
	 * @return The packed integer representation.
	 * @throws IllegalArgumentException If the coordinates are outwith the stated range.
	 */
	public static long pack(int x, int y)
	{
		if (!coordRangeCheck(x, y))
		{
			throw new IllegalArgumentException("x=" + x + ", y=" + y);
		}
		return ((long) (x) << 32) + (long) y;
	}

	/**
	 * Splits a packed long into the equivalent chunk coordinates.
	 * @param packed The packed long representation.
	 * @return A two-element array where element 0 is the X coordinate and element 1 is the Y coordinate.
	 * @throws IllegalArgumentException If the given long does not correspond to a valid pair of coordinates.
	 */
	public static int[] unpack(long packed)
	{
		int[] coords = new int[]{(int) ((packed & -4294967296L) >> 32), (int) (packed & 4294967295L)};
		if (!coordRangeCheck(coords[0], coords[1]))
		{
			throw new IllegalArgumentException("x=" + coords[0] + ", y=" + coords[1]);
		}
		return coords;
	}

	/**
	 * Checks whether the given chunk coordinates are within the accepted range.
	 * @param x The X coordinate.
	 * @param y The Y coordinate.
	 * @return True if the coordinates are within the range; false otherwise.
	 */
	private static boolean coordRangeCheck(int x, int y)
	{
		return (x >= 0 && x <= 65535 && y >= 0 && y <= 65535);
	}
}
