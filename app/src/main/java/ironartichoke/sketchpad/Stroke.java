package ironartichoke.sketchpad;

import android.opengl.GLES20;
import android.support.annotation.NonNull;

import com.carrotsearch.hppc.IntArrayList;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import ironartichoke.sketchpad.externalizable.ExternalizableFloatArrayList;
import ironartichoke.sketchpad.util.AppearanceUtils;
import ironartichoke.sketchpad.util.Atan2;
import ironartichoke.sketchpad.util.ChunkUtils;
import ironartichoke.sketchpad.util.ColorUtils;
import ironartichoke.sketchpad.util.MathUtils;

/**
 * A class representing a single canvas stroke.
 */
public class Stroke implements Externalizable, Comparable<Stroke>
{
	/** The unique ID of this stroke. */
	private long id;

	/** The ID of the chunk to which this stroke belongs. */
	public long chunkId;

	/** Whether this stroke was drawn with the eraser tool. */
	public boolean isEraser = false;

	/** The default stroke thickness. 0 is the thinnest and 4 is the thickest. */
	public static final byte DEFAULT_THICKNESS = 2;
	/** The thickness of the stroke as a byte. */
	private byte thicknessByte = DEFAULT_THICKNESS;
	/** The thickness of the stroke as drawn by OpenGL. */
	private float thickness = THICKNESSES[thicknessByte];
	/** The color of the stroke as a byte. */
	private byte colorByte = 0;
	/** The color of the stroke as drawn by OpenGL. */
	private float[] color = ColorUtils.COLORS[colorByte];

	/** The array of coordinates that make up the stroke. Contains alternating X and Y coordinates. */
	private ExternalizableFloatArrayList coords = new ExternalizableFloatArrayList();
	/** The number of points in this stroke. */
	private int numberOfPoints = 0;
	/** The left bound of the stroke, i.e. the smallest X coordinate of all points on the stroke. */
	private int boundLeft = Integer.MAX_VALUE;
	/** The right bound of the stroke, i.e. the greatest X coordinate of all points on the stroke. */
	private int boundRight = Integer.MIN_VALUE;
	/** The top bound of the stroke, i.e. the smallest Y coordinate of all points on the stroke. */
	private int boundTop = Integer.MAX_VALUE;
	/** The bottom bound of the stroke, i.e. the greatest Y coordinate of all points on the stroke. */
	private int boundBottom = Integer.MIN_VALUE;
	/** The triangle strip belonging to the stroke, drawn by OpenGL. */
	public TriangleStrip triangleStrip;

	/** The list of possible stroke thicknesses corresponding to the value of {@link #thicknessByte}. */
	private final static float[] THICKNESSES = new float[]{1.0f, 2.5f, 4.0f, 5.5f, 7.0f};

	/** The thickness of eraser strokes compared to the thickness of pen strokes. */
	private final static float THICKNESS_ERASER_MODIFIER = 8.0f;

	/** The RGBA values for the color black. */
	private final static float[] COLOR_BLACK = new float[]{0.0f, 0.0f, 0.0f, 1.0f};
	/** The RGBA values for the color white. */
	private final static float[] COLOR_WHITE = new float[]{1.0f, 1.0f, 1.0f, 1.0f};

	/** The minimum length of the individual segments of a stroke. */
	public static final float SEGMENT_THRESHOLD = 0.2f;
	/** The angle threshold between to segments of a stroke.
	 * Angles below this will prompt optimization.
	 * @see #optimize(NotepadView) */
	private final static float SEGMENT_ANGLE_THRESHOLD = 0.01f;

	/**
	 * Used for serialization only.
	 */
	public Stroke()	{}

	/**
	 * Instantiates a stroke.
	 * @param view The <code>NotepadView</code>.
	 * @param isTemporary True if this stroke has not been (fully) drawn yet.
	 */
	public Stroke(NotepadView view, boolean isTemporary)
	{
		id = (view.project == null ? 0 : view.project.getStrokeCount());
		setColor(view.strokeColor);
		setThickness(view.strokeThickness);
		triangleStrip = new TriangleStrip(view, this, isTemporary);
	}

	/**
	 * Gets the real thickness.
	 * @return The real thickness.
	 */
	private float getThickness()
	{
		return thickness;
	}

	/**
	 * Gets the thickness as a byte.
	 * @return The thickness byte value.
	 */
	private byte getThicknessByte()
	{
		return thicknessByte;
	}

	/**
	 * Sets the thickness.
	 * @param thicknessByte The thickness byte value.
	 */
	public void setThickness(byte thicknessByte)
	{
		this.thicknessByte = thicknessByte;
		thickness = THICKNESSES[thicknessByte];
		if (triangleStrip != null) triangleStrip.updateThickness();
	}

	/**
	 * Gets the RGBA color values.
	 * @return The RGBA color.
	 */
	public float[] getColor()
	{
		return color;
	}

	/**
	 * Gets the color as a byte.
	 * @return The color byte value.
	 */
	private byte getColorByte()
	{
		return colorByte;
	}

	/**
	 * Sets the color.
	 * @param colorByte The color byte value.
	 */
	public void setColor(byte colorByte)
	{
		this.colorByte = colorByte;
		color = ColorUtils.COLORS[colorByte];
		if (triangleStrip != null) triangleStrip.updateColor();
	}

	/**
	 * Renders the stroke.
	 * @param mvpMatrix The model-view-projection matrix.
	 */
	public void draw(float[] mvpMatrix)
	{
		triangleStrip.draw(mvpMatrix);
	}

	/**
	 * Gets the X coordinate of a point on the stroke.
	 * @param index The index of the point.
	 * @return The X coordinate of that point.
	 */
	private float getCoordX(int index)
	{
		return coords.get(index * 2);
	}

	/**
	 * Gets the Y coordinate of a point on the stroke.
	 * @param index The index of the point.
	 * @return The Y coordinate of that point.
	 */
	private float getCoordY(int index)
	{
		return coords.get(index * 2 + 1);
	}

	/**
	 * Gets the total number of points on the stroke.
	 * @return The total points.
	 */
	public int getNumberOfPoints()
	{
		return coords.size() / 2;
	}

	/**
	 * Gets the triangle strip for this stroke.
	 * @return The triangle strip.
	 */
	public TriangleStrip getTriangleStrip()
	{
		return triangleStrip;
	}

	/**
	 * Adds a point to the end of the stroke described by the given canvas-space coordinates.
	 * @param realX The canvas X coordinate.
	 * @param realY The canvas Y coordinate.
	 */
	@SuppressWarnings("unused")
	public void addRealCoords(float realX, float realY)
	{
		coords.add(realX);
		coords.add(realY);
		numberOfPoints++;
	}

	/**
	 * Adds a point to the end of the stroke described by the given screen-space coordinates.
	 * @param view The <code>NotepadView</code>.
	 * @param screenX The screen X coordinate.
	 * @param screenY The screen Y coordinate.
	 */
	private void addScreenCoords(NotepadView view, float screenX, float screenY)
	{
		coords.add(MathUtils.convertScreenXToCanvas(view, screenX));
		coords.add(MathUtils.convertScreenYToCanvas(view, screenY));
		numberOfPoints++;
	}

	/**
	 * Updates the bounding box of the stroke, i.e. the minimum/maximum X/Y coordinates.
	 */
	private void updateBounds()
	{
		boundLeft = Integer.MAX_VALUE;
		boundRight = Integer.MIN_VALUE;
		boundTop = Integer.MAX_VALUE;
		boundBottom = Integer.MIN_VALUE;

		int points = getNumberOfPoints();
		for (int c = 0; c < points; c++)
		{
			int x = (int) getCoordX(c);
			int y = (int) getCoordY(c);

			if (x < boundLeft)
			{
				boundLeft = x;
			}

			if (x > boundRight)
			{
				boundRight = x;
			}

			if (y < boundTop)
			{
				boundTop = y;
			}

			if (y > boundBottom)
			{
				boundBottom = y;
			}
		}

		chunkId = getContainingChunk();
	}

	/**
	 * Attempts to optimize the stroke by removing vertices that are collinear, or
	 * near-collinear, to neighboring vertices. The triangle strip is regenerated to
	 * reflect the new geometry.
	 * @param view The <code>NotepadView</code>.
	 */
	public void optimize(NotepadView view)
	{
		int initialVertices = getNumberOfPoints();

		// Optimization is unnecessary if there are less than 3 vertices.
		if (initialVertices > 2)
		{
			IntArrayList vertexOffsetsToRemove = new IntArrayList();
			IntArrayList vertexRangesToRemove = new IntArrayList();

			int currentVertex = 1;
			int startVertexToBeRemoved = 0;
			float angle;
			float initialAngle = 0.0f;
			boolean checkAngle = false;
			int verticesToBeRemoved = 0;
			int totalVertices = getNumberOfPoints();

			while (currentVertex < totalVertices - 1 && totalVertices - verticesToBeRemoved > 2)
			{
				angle = Atan2.atan2(getCoordY(currentVertex + 1) - getCoordY(currentVertex),
						getCoordX(currentVertex + 1) - getCoordX(currentVertex));

				if (!checkAngle)
				{
					initialAngle = Atan2.atan2(getCoordY(currentVertex) - getCoordY(currentVertex - 1),
												   getCoordX(currentVertex) - getCoordX(currentVertex - 1));
				}

				float delta = Math.abs(initialAngle - angle);

				if (checkAngle)
				{
					if (delta > SEGMENT_ANGLE_THRESHOLD)
					{
						if (verticesToBeRemoved < 1)
						{
							initialAngle = angle;
						}
						else
						{
							vertexOffsetsToRemove.add(startVertexToBeRemoved);
							vertexRangesToRemove.add(startVertexToBeRemoved + verticesToBeRemoved - 1 > initialVertices - 2 ? initialVertices - 1 - startVertexToBeRemoved : verticesToBeRemoved);
							checkAngle = false;
							verticesToBeRemoved = 0;
						}
					}
					else
					{
						verticesToBeRemoved++;
						startVertexToBeRemoved = currentVertex;
					}
				}
				else if (delta < SEGMENT_ANGLE_THRESHOLD)
				{
					checkAngle = true;
					initialAngle = angle;
					startVertexToBeRemoved = currentVertex;
					verticesToBeRemoved++;
				}

				currentVertex++;
			}

			int elementsRemoved = 0;
			int offset;
			int count;
			for (int v = 0; v < vertexOffsetsToRemove.size(); v++)
			{
				offset = vertexOffsetsToRemove.get(v) * 2;
				count = vertexRangesToRemove.get(v) * 2;
				coords.removeRange(offset - elementsRemoved, offset - elementsRemoved + count);
				elementsRemoved += count;
			}
			numberOfPoints -= elementsRemoved / 2;
			coords.ensureCapacity(numberOfPoints * 2);
		}

		updateBounds();
		triangleStrip = new TriangleStrip(view, this, false);
	}

	/**
	 * Returns the ID of the chunk that the stroke should belong to.
	 * The chunk chosen is where the average centre of the stroke is located.
	 * @return The chunk ID that should contain this stroke.
	 */
	public long getContainingChunk()
	{
		int centerX = (boundLeft + boundRight) / 2;
		int centerY = (boundBottom + boundTop) / 2;
		return ChunkUtils.convertCoordsToChunk(centerX, centerY);
	}

	/**
	 * A class that represents a stroke's triangle strip.
	 * This contains the geometry needed by OpenGL to render the stroke.
	 */
	public static class TriangleStrip
	{
		/** The stroke that this triangle strip belongs to. */
		Stroke stroke;

		/** A buffer of coordinates. */
		FloatBuffer vertexBuffer;
		/** The size of the vertex buffer. */
		int vertexBufferSize = 0;
		/** The number of points on the stroke (not vertices). */
		int pointCount = 0;

		/** The thickness of the stroke. */
		float thickness;
		/** The RGBA color of the stroke. */
		float[] color;

		/** The pointer to the position variable in the vertex shader. */
		int positionHandle;
		/** The pointer to the color variable in the fragment shader. */
		int colorHandle;
		/** The pointer to the model-view-projection matrix in the vertex shader. */
		int mvpMatrixHandle;

		/** The number of coordinates per vertex. */
		private static final int COORDS_PER_VERTEX = 3;
		/** The number of vertices per point, i.e. per line segment (two triangles). */
		private static final int VERTICES_PER_POINT = COORDS_PER_VERTEX * 2;
		/** The vertex stride, i.e. 4 unique vertices per line segment. */
		private static final int VERTEX_STRIDE = COORDS_PER_VERTEX * 4;
		/** The number of bytes per float. */
		private static final int BYTES_PER_FLOAT = 4;
		/** The upper bound on the number of points in a stroke. */
		public  static final int STROKE_POINT_LIMIT = 10000;
		/** The upper bound on the number of floats in the vertex buffer. */
		private static final int FLOAT_LIMIT = STROKE_POINT_LIMIT * VERTICES_PER_POINT;

		/**
		 * Instantiates a triangle strip, optionally containing the given stroke's geometry.
		 * @param view The <code>NotepadView</code>.
		 * @param stroke The stroke belonging to this triangle strip.
		 * @param isTemporary True if the stroke belonging to this triangle strip has not been (fully) drawn yet.
		 */
		public TriangleStrip(NotepadView view, Stroke stroke, boolean isTemporary)
		{
			this.stroke = stroke;
			updateThickness();
			updateColor();
			ByteBuffer bb = ByteBuffer.allocateDirect(isTemporary ? FLOAT_LIMIT * BYTES_PER_FLOAT : stroke.coords.size() * COORDS_PER_VERTEX * BYTES_PER_FLOAT);
			bb.order(ByteOrder.nativeOrder());
			vertexBuffer = bb.asFloatBuffer();
			vertexBuffer.position(0);

			if (!isTemporary)
			{
				int points = stroke.getNumberOfPoints();
				for (int c = 0; c < points; c++)
				{
					addPoint(view, stroke.getCoordX(c), stroke.getCoordY(c), false);
				}
			}
		}

		/**
		 * Copies the backing stroke's thickness.
		 */
		public void updateThickness()
		{
			thickness = THICKNESSES[stroke.getThicknessByte()];
		}

		/**
		 * Copies the backing stroke's color, incorporating the current theme.
		 */
		public void updateColor()
		{
			byte colorByte = stroke.getColorByte();
			if (stroke.isEraser)
			{
				color = AppearanceUtils.isThemeDark() ? COLOR_BLACK : COLOR_WHITE;
			}
			else if (colorByte == 0 && AppearanceUtils.isThemeDark())
			{
				color = COLOR_WHITE;
			}
			else
			{
				color = ColorUtils.COLORS[colorByte];
			}
		}

		/**
		 * Renders the triangle strip.
		 * @param mvpMatrix The model-view-projection matrix.
		 */
		public void draw(float[] mvpMatrix)
		{
			vertexBuffer.position(0);
			positionHandle = GLES20.glGetAttribLocation(GLProgram.glProgram, "vPosition");
			GLES20.glEnableVertexAttribArray(positionHandle);
			GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, VERTEX_STRIDE, vertexBuffer);
			colorHandle = GLES20.glGetUniformLocation(GLProgram.glProgram, "vColor");
			GLES20.glUniform4fv(colorHandle, 1, color, 0);
			mvpMatrixHandle = GLES20.glGetUniformLocation(GLProgram.glProgram, "uMVPMatrix");
			GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
			GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, pointCount * 2);
			GLES20.glDisableVertexAttribArray(positionHandle);
		}

		/**
		 * Adds the given vertices to the end of the triangle strip.
		 * Note that vertices have 3 components.
		 * @param vertices The vertices as a strided list of X,Y,Z coordinates.
		 *                 The Z values are irrelevant due to the projection.
		 */
		public void addVertices(float[] vertices)
		{
			vertexBuffer.position(vertexBufferSize);
			vertexBuffer.put(vertices);
			vertexBufferSize += vertices.length;
		}

		/**
		 * Adds a point to the end of the triangle strip.
		 * @param view The <code>NotepadView</code>.
		 * @param x The screen X coordinate.
		 * @param y The screen Y coordinate.
		 * @param addCoordsToStroke Whether the point should also be added to the backing stroke.
		 */
		public void addPoint(NotepadView view, float x, float y, boolean addCoordsToStroke)
		{
			if (addCoordsToStroke)
			{
				stroke.addScreenCoords(view, x, y);
			}

			float rightHandVectorX;
			float rightHandVectorY;
			float vectorMagnitude;

			// Has a point already been added?
			if (pointCount > 0)
			{
				// NOTE: The X and Y values are swapped deliberately!
				rightHandVectorX = stroke.getCoordY(pointCount) - stroke.getCoordY(pointCount - 1);
				rightHandVectorY = stroke.getCoordX(pointCount) - stroke.getCoordX(pointCount - 1);

				vectorMagnitude = (float) Math.sqrt(rightHandVectorX * rightHandVectorX + rightHandVectorY * rightHandVectorY);

				// Set the vector's magnitude to half of the stroke thickness
				rightHandVectorX = (rightHandVectorX / vectorMagnitude) * stroke.getThickness() * (stroke.isEraser ? THICKNESS_ERASER_MODIFIER : 1) * 0.5f;
				rightHandVectorY = (rightHandVectorY / vectorMagnitude) * stroke.getThickness() * (stroke.isEraser ? THICKNESS_ERASER_MODIFIER : 1) * -0.5f;

				float[] vertices;
				// Will this point form the first stroke segment?
				if (pointCount == 1)
				{
					vertices = new float[]{
							stroke.getCoordX(0) + rightHandVectorX,
							stroke.getCoordY(0) + rightHandVectorY,
							0,
							stroke.getCoordX(0) - rightHandVectorX,
							stroke.getCoordY(0) - rightHandVectorY,
							0,
							stroke.getCoordX(1) + rightHandVectorX,
							stroke.getCoordY(1) + rightHandVectorY,
							0,
							stroke.getCoordX(1) - rightHandVectorX,
							stroke.getCoordY(1) - rightHandVectorY,
							0
					};
				}
				// Has the first stroke segment already been formed?
				else
				{
					vertices = new float[]{
							stroke.getCoordX(pointCount) + rightHandVectorX,
							stroke.getCoordY(pointCount) + rightHandVectorY,
							0,
							stroke.getCoordX(pointCount) - rightHandVectorX,
							stroke.getCoordY(pointCount) - rightHandVectorY,
							0
					};
				}
				addVertices(vertices);
			}

			pointCount++;
		}
	}

	@Override
	public void writeExternal(ObjectOutput output) throws IOException
	{
		output.writeLong(id); // ID
		output.writeLong(chunkId); // Chunk ID
		output.writeObject(coords); // Vertices
		output.writeByte(colorByte); // Colour
		output.writeByte(thicknessByte); // Thickness
		output.writeBoolean(isEraser); // Is eraser stroke
	}

	@Override
	public void readExternal(ObjectInput input) throws IOException, ClassNotFoundException
	{
		id = input.readLong(); // ID
		chunkId = input.readLong(); // Chunk ID
		coords = (ExternalizableFloatArrayList) input.readObject(); // Vertices
		setColor(input.readByte()); // Color
		setThickness(input.readByte()); // Thickness
		isEraser = input.readBoolean(); // Is eraser stroke
		triangleStrip = new TriangleStrip(null, this, false);
		updateBounds();
	}

	@Override
	public int compareTo(@NonNull Stroke another)
	{
		return Long.valueOf(id).compareTo(another.id);
	}
}