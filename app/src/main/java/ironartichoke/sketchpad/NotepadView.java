package ironartichoke.sketchpad;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import ironartichoke.sketchpad.util.AppearanceUtils;
import ironartichoke.sketchpad.util.ChunkUtils;

/**
 * The view that renders the canvas and its contents. A subclass of {@link GLSurfaceView}.
 */
public class NotepadView extends GLSurfaceView
{
	/** A reference to the {@link NotepadActivity} that this view belongs to. */
	private NotepadActivity activity;

	/** The renderer to be used by OpenGL. */
	private GLRenderer renderer;

	/** The current sketchbook. */
	public Project project;

	/** The constant representing the pencil tool. */
	public static final int TOOL_PENCIL = 0;
	/** The constant representing the eraser tool. */
	public static final int TOOL_ERASER = ~TOOL_PENCIL;

	/** The minimum zoom value. */
	private static final float ZOOM_MINIMUM = 0.1f;
	/** The default zoom value. */
	public static final float ZOOM_DEFAULT = 1.0f;
	/** The maximum zoom value. */
	private static final float ZOOM_MAXIMUM = 10.0f;

	/** The current tool. Either {@link #TOOL_PENCIL} or {@link #TOOL_ERASER}. */
	public int tool = TOOL_PENCIL;

	/** The width of the viewport in pixels. */
	public int viewportWidth;
	/** The height of the viewport in pixels. */
	public int viewportHeight;

	/** The camera's X position. */
	public float cameraX;
	/** The camera's Y position. */
	public float cameraY;
	/** The camera's zoom. */
	public float zoom = ZOOM_DEFAULT;
	/** The zoom as it was on the previous render. */
	private float zoomPrev;

	/** The stroke that is currently being drawn by the user. */
	public Stroke latestStroke;
	/** The stroke thickness byte. */
	public byte strokeThickness = Stroke.DEFAULT_THICKNESS;
	/** The stroke color byte. */
	public byte strokeColor = 0;
	/** The length of the latest segment of the stroke that is currently being drawn. */
	private float strokeSegmentLength = 0f;
	/** The number of points so far in the stroke that is currently being drawn. */
	private int strokePointCount = 0;

	/** The current touch operation. Either {@link #TOUCH_NONE}, {@link #TOUCH_DRAG} or {@link #TOUCH_PINCH}. */
	private int touchAction = TOUCH_NONE;
	/** The constant for when no touch operation is in progress. */
	public static final int TOUCH_NONE = 0;
	/** The constant for when the user is dragging with one finger. */
	private static final int TOUCH_DRAG = 1;
	/** The constant for when the user is pinching the view. */
	private static final int TOUCH_PINCH = 2;

	/** The X coordinate of the latest touch. */
	private float touchX;
	/** The Y coordinate of the latest touch. */
	private float touchY;
	/** The X coordinate of the previous touch position. */
	private float touchXPrev;
	/** The Y coordinate of the previous touch position. */
	private float touchYPrev;
	/** The X coordinate of the second finger during a pinch. */
	private float touchX2;
	/** The Y coordinate of the second finger during a pinch. */
	private float touchY2;
	/** The X coordinate of the point between the two fingers during a pinch. */
	private float pinchCenterX;
	/** The Y coordinate of the point between the two fingers during a pinch. */
	private float pinchCenterY;
	/** The previous X coordinate of the center pinch position. */
	private float pinchCenterXPrev;
	/** The previous Y coordinate of the center pinch position. */
	private float pinchCenterYPrev;

	/** The distance between the two fingers at the beginning of a pinch action. */
	private float initialPinchDistance;
	/** The camera zoom at the beginning of a pinch action. */
	private float initialZoom;

	/** An array containing the current chunk boundaries.
	 * Accessed with {@link #BOUND_LEFT}, {@link #BOUND_RIGHT}, {@link #BOUND_TOP} and {@link #BOUND_BOTTOM}. */
	private int[] chunkBounds = new int[4];
	/** The previous chunk boundaries.
	 * Accessed with {@link #BOUND_LEFT}, {@link #BOUND_RIGHT}, {@link #BOUND_TOP} and {@link #BOUND_BOTTOM}. */
	private int[] prevChunkBounds = new int[4];
	/** The index representing the left edge of the chunk boundary.
	 * @see #chunkBounds */
	private static final int BOUND_LEFT = 0;
	/** The index representing the right edge of the chunk boundary.
	 * @see #chunkBounds */
	private static final int BOUND_RIGHT = 1;
	/** The index representing the top edge of the chunk boundary.
	 * @see #chunkBounds */
	private static final int BOUND_TOP = 2;
	/** The index representing the bottom edge of the chunk boundary.
	 * @see #chunkBounds */
	private static final int BOUND_BOTTOM = 3;

	/** Project and sheet thumbnails are shrunken in size by this factor. */
	private static final int THUMBNAIL_SHRINKING_FACTOR = 10;

	public NotepadView(@NonNull Context context)
	{
		super(context);
		init();
	}

	public NotepadView(@NonNull Context context, @NonNull AttributeSet attrs)
	{
		super(context, attrs);
		init();
	}

	/**
	 * Makes some preparations, including for OpenGL.
	 */
	private void init()
	{
		initOpenGL();
		latestStroke = new Stroke(this, true);
	}

	/**
	 * Sets the view's reference to its parent activity.
	 * @param activity The <code>NotepadActivity</code>.
	 */
	public void setActivityReference(@NonNull NotepadActivity activity)
	{
		this.activity = activity;
	}

	/**
	 * Initializes the OpenGL environment.
	 */
	private void initOpenGL()
	{
		// Create an OpenGL ES 2.0 context.
		setEGLContextClientVersion(2);

		// Set the drawing quality.
		// Alpha isn't needed and the color choice is limited anyway, so we can get away with this setting.
		getHolder().setFormat(PixelFormat.RGB_565);
		setEGLConfigChooser(5, 6, 5, 0, 0, 0);

		// Prepare the renderer.
		renderer = new GLRenderer();
		setRenderer(renderer);

		// Choose to only render the view on demand.
		setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

		// Disable the unneeded depth test and stencil test.
		GLES20.glDisable(GLES20.GL_DEPTH_TEST);
		GLES20.glDisable(GLES20.GL_STENCIL_TEST);

		// Render the canvas for the first time.
		requestRender();
	}

	/**
	 * Retrieves the OpenGL renderer.
	 * @return The OpenGL renderer.
	 */
	public GLRenderer getRenderer()
	{
		return renderer;
	}

	/**
	 * Requests that a screenshot of the current sheet be taken after the next render.
	 */
	public void requestScreenshot(Project project)
	{
		renderer.requestScreenshot(project.getCurrentSheetIndex());
	}

	/**
	 * Requests that a screenshot of the specified sheet be taken after the next render.
	 * @param sheetIndex The index of the sheet (NOT the sheet ID).
	 */
	public void requestScreenshot(int sheetIndex)
	{
		renderer.requestScreenshot(sheetIndex);
	}

	/**
	 * The OpenGL renderer used by <code>NotepadView</code>.
	 */
	class GLRenderer implements Renderer
	{
		/** The model-view-projection matrix.
		 * @see #updateMvpMatrix() */
		private final float[] mvpMatrix = new float[16];

		/** Takes a screenshot of the view during the next render if true. */
		private volatile boolean isScreenshotRequested = false;
		/** The index of the sheet to take a screenshot of. Usually the current sheet.
		 * @see #isScreenshotRequested */
		private volatile int screenshotSheetIndex;

		/** Changes the clear color to match the theme on the next render if true,
		 * i.e. white background for light mode and a black background for dark mode. */
		private volatile boolean isBackgroundChangeRequested = false;

		/** Tracks how many renders have been performed.
		 * @see #resetRenderCount() */
		private volatile int renderCount = 0;
		/** The maximum number of renders between updates of the camera toolbar.
		 * @see #resetRenderCount() */
		private static final int RENDER_COUNT_THRESHOLD = 10;
		/** A convenient <code>Runnable</code> for updating the camera toolbar when the render threshold is reached.
		 * Runs from {@link #resetRenderCount()}.
		 * @see #renderCount
		 * @see #RENDER_COUNT_THRESHOLD */
		private final Runnable updateCameraToolbarRunnable = new Runnable()
		{
			@Override
			public void run()
			{
				// Update the camera toolbar to reflect the new zoom and position.
				activity.updateCameraToolbar();
			}
		};

		@Override
		public void onSurfaceCreated(GL10 unused, EGLConfig config)
		{
			// Set the background color accordingly.
			updateBackgroundColor();

			// Get pointers to the shaders.
			int vertexShader = renderer.loadShader(GLES20.GL_VERTEX_SHADER,	GLProgram.vertexShaderCode);
			int fragmentShader = renderer.loadShader(GLES20.GL_FRAGMENT_SHADER,	GLProgram.fragmentShaderCode);

			// Create a new OpenGL program.
			GLProgram.glProgram = GLES20.glCreateProgram();

			// Add a vertex shader.
			GLES20.glAttachShader(GLProgram.glProgram, vertexShader);

			// Add a fragment shader.
			GLES20.glAttachShader(GLProgram.glProgram, fragmentShader);

			// Compile the program.
			GLES20.glLinkProgram(GLProgram.glProgram);

			// Use it.
			GLES20.glUseProgram(GLProgram.glProgram);

			// Initialize the project if it hasn't been done already.
			// Use a posted Runnable since NotepadActivity.initProject() should run on the UI thread.
			if (project == null)
			{
				post(new Runnable()
				{
					@Override
					public void run()
					{
						project = new Project();
						activity.initProject();
					}
				});
			}
		}

		/**
		 * Sets the background color of the canvas according to the current theme.
		 * White if the theme is {@link AppearanceUtils#THEME_LIGHT}, and black if the theme is {@link AppearanceUtils#THEME_DARK}.
		 */
		private void updateBackgroundColor()
		{
			if (AppearanceUtils.isThemeDark())
			{
				GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f); // Black
			}
			else
			{
				GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f); // White
			}
		}

		/**
		 * Resets the render count to zero.<br\><br\>
		 * The render count is incremented every time the surface is drawn.
		 * When it reaches the threshold as defined by {@link #RENDER_COUNT_THRESHOLD}, this method should be called.
		 * The camera zoom and position views are then updated.
		 * This is done in favor of updating the views after every frame since this causes a significant drop in performance on older devices.
		 */
		public void resetRenderCount()
		{
			// Zero the render count.
			renderCount = 0;

			// Remove any currently scheduled camera toolbar update to avoid clashes.
			removeCallbacks(updateCameraToolbarRunnable);

			// Update another camera toolbar update.
			post(updateCameraToolbarRunnable);
		}

		@Override
		public void onDrawFrame(GL10 unused)
		{
			// Clear the buffer.
			GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

			// Is a background change requested?
			if (isBackgroundChangeRequested)
			{
				isBackgroundChangeRequested = false;

				// Make any black strokes white when in dark mode, or white strokes black in light mode.
				if (project != null)
				{
					int strokes = project.getLoadedStrokes().size();
					for (int s = 0; s < strokes; s++)
					{
						Stroke stroke;
						if ((stroke = project.getLoadedStroke(s)) != null)
						{
							stroke.getTriangleStrip().updateColor();
						}
					}

					// Reset the temporary stroke.
					latestStroke = new Stroke(NotepadView.this, true);
				}

				// Change the background color.
				updateBackgroundColor();

				// Render to reveal the changes.
				requestRender();
			}

			// Increase the render count while a pinch is in progress.
			if (touchAction == TOUCH_PINCH) renderCount++;

			// Check that the project reference exists before trying to draw any strokes!
			if (project != null)
			{
				// Draw each loaded stroke.
				int strokes = project.getLoadedStrokes().size();
				for (int s = 0; s < strokes; s++)
				{
					Stroke stroke;
					if ((stroke = project.getLoadedStroke(s)) != null)
					{
						stroke.draw(mvpMatrix);
					}
				}

				// If a new stroke is currently being drawn by the user, draw it.
				// It needs to be done here since the stroke hasn't been formally added to the project yet.
				if (touchAction == TOUCH_DRAG && latestStroke != null)
				{
					latestStroke.draw(mvpMatrix);
				}

				// Is a screenshot due?
				if (isScreenshotRequested)
				{
					isScreenshotRequested = false;
					takeScreenshot();
				}

				// Is the render count over the threshold?
				if (renderCount >= RENDER_COUNT_THRESHOLD)
				{
					resetRenderCount();
				}
			}
		}

		/**
		 * Called when the viewport changes in size.
		 * This happens when the view is first created and whenever the device orientation is changed.
		 * See {@link Renderer#onSurfaceChanged(GL10, int, int)} for the original description.
		 */
		@Override
		public void onSurfaceChanged(@Nullable GL10 unused, int width, int height)
		{
			// Update the view's width and height fields.
			updateViewport(width, height);

			// Update the OpenGL viewport itself.
			GLES20.glViewport(0, 0, width, height);

			// Update the model-view-projection matrix.
			updateMvpMatrix();
		}

		/**
		 * Sets the model-view-projection matrix to reflect the current viewport size, camera zoom and camera position.
		 */
		public void updateMvpMatrix()
		{
			// Set the matrix, specifying the edges of the visible canvas and the near and far clipping planes.
			Matrix.orthoM(mvpMatrix, 0,
					-viewportWidth / (2f * zoom) + cameraX,
					viewportWidth / (2f * zoom) + cameraX,
					-viewportHeight / (2f * zoom) + cameraY,
					viewportHeight / (2f * zoom) + cameraY, -1, 1);
		}

		/**
		 * Loads the given shader code into the specified shader type.
		 * @param type {@link GLES20#GL_VERTEX_SHADER} for the vertex shader, or {@link GLES20#GL_FRAGMENT_SHADER} for the fragment shader.
		 * @param shaderCode The GLSL code for the shader.
		 * @return The pointer for the loaded shader.
		 */
		public int loadShader(int type, String shaderCode)
		{
			// Create a new shader and retrieve its pointer.
			int shader = GLES20.glCreateShader(type);

			// Load the shader code.
			GLES20.glShaderSource(shader, shaderCode);

			// Compile the shader.
			GLES20.glCompileShader(shader);

			return shader;
		}

		/**
		 * Request that a screenshot be taken on the next render.
		 * @param sheetIndex The index of the sheet of which the screenshot should be taken (usually the current sheet).
		 * @see #takeScreenshot()
		 */
		public void requestScreenshot(int sheetIndex)
		{
			isScreenshotRequested = true;
			screenshotSheetIndex = sheetIndex;
			requestRender();
		}

		/**
		 * Requests that the background color and black/white strokes should be inverted on the next render.
		 * This should be called when light/dark mode is toggled.
		 */
		public void requestBackgroundChange()
		{
			isBackgroundChangeRequested = true;
			requestRender();
		}

		/**
		 * Takes a screenshot of the view and saves it as the thumbnail of the specified sheet.
		 * @see #requestScreenshot(Project)
		 */
		private void takeScreenshot()
		{
			int width = getWidth();
			int height = getHeight();
			int capacity = width * height;
			int[] dataArray = new int[capacity];
			IntBuffer dataBuffer = IntBuffer.allocate(capacity);
			GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, dataBuffer);
			int[] dataArrayTemp = dataBuffer.array();

			// Flip the mirrored image.
			for (int y = 0; y < height; y++)
			{
				System.arraycopy(dataArrayTemp, y * width, dataArray, (height - y - 1) * width, width);
			}

			Bitmap screenshot = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			screenshot.copyPixelsFromBuffer(IntBuffer.wrap(dataArray));
			Bitmap thumbnail = Bitmap.createScaledBitmap(screenshot, width / THUMBNAIL_SHRINKING_FACTOR, height / THUMBNAIL_SHRINKING_FACTOR, true);
			project.getSheetAt(screenshotSheetIndex).saveThumbnail(thumbnail);
			BitmapDrawable thumbnailDrawable = new BitmapDrawable(getResources(), thumbnail);
			project.getSheetAt(screenshotSheetIndex).setThumbnail(thumbnailDrawable);

			// Refresh the sheet panel if the user is not requesting an exit.
			if (activity.taskRequested != NotepadActivity.TASK_EXIT)
			{
				activity.refreshSheetDrawerAfterGeneratingThumbnail();
			}

			// The project thumbnail should be twice the width and height of a sheet thumbnail.
			Bitmap projectThumbnail = Bitmap.createScaledBitmap(
					screenshot, (width * 2) / THUMBNAIL_SHRINKING_FACTOR, (height * 2) / THUMBNAIL_SHRINKING_FACTOR, true);
			project.saveThumbnail(projectThumbnail);

			if (activity.taskRequested > 0)
			{
				post(new Runnable()
				{
					@Override
					public void run()
					{
						activity.performTask();
					}
				});
			}
		}
	}

	/**
	 * Updates the fields for the width and height of the viewport.
	 * @param width The new width.
	 * @param height The new height.
	 */
	private void updateViewport(int width, int height)
	{
		viewportWidth = width;
		viewportHeight = height;
	}

	/**
	 * Checks whether any chunks around the viewpoint need to be loaded or saved/discarded.
	 * A chunk is loaded when it comes into view.
	 * A chunk is saved and discarded when it moves out of view.
	 * This method should be called after the camera is panned.
	 * For other camera movements, call loadAllVisibleChunks() instead.
	 * @see #loadAllVisibleChunks(boolean, boolean)
	 */
	private void checkSurroundingChunks()
	{
		calculateChunkBounds();

		if (chunkBounds[BOUND_RIGHT] > prevChunkBounds[BOUND_RIGHT]) // +X load
		{
			for (int c = prevChunkBounds[BOUND_RIGHT] + 1; c <= chunkBounds[BOUND_RIGHT]; c++)
			{
				project.loadChunkColumn(c, chunkBounds[BOUND_BOTTOM], chunkBounds[BOUND_TOP], true);
			}
		}
		else if (chunkBounds[BOUND_RIGHT] < prevChunkBounds[BOUND_RIGHT]) // -X unload
		{
			for (int c = prevChunkBounds[BOUND_RIGHT]; c > chunkBounds[BOUND_RIGHT]; c--)
			{
				project.saveChunkColumn(c, chunkBounds[BOUND_BOTTOM], chunkBounds[BOUND_TOP], true, true);
			}
		}

		if (chunkBounds[BOUND_LEFT] < prevChunkBounds[BOUND_LEFT]) // -X load
		{
			for (int c = prevChunkBounds[BOUND_LEFT] - 1; c >= chunkBounds[BOUND_LEFT]; c--)
			{
				project.loadChunkColumn(c, chunkBounds[BOUND_BOTTOM], chunkBounds[BOUND_TOP], true);
			}
		}
		else if (chunkBounds[BOUND_LEFT] > prevChunkBounds[BOUND_LEFT]) // +X unload
		{
			for (int c = prevChunkBounds[BOUND_LEFT]; c < chunkBounds[BOUND_LEFT]; c++)
			{
				project.saveChunkColumn(c, chunkBounds[BOUND_BOTTOM], chunkBounds[BOUND_TOP], true, true);
			}
		}

		if (chunkBounds[BOUND_TOP] > prevChunkBounds[BOUND_TOP]) // +Y load
		{
			for (int c = prevChunkBounds[BOUND_TOP] + 1; c <= chunkBounds[BOUND_TOP]; c++)
			{
				project.loadChunkRow(c, chunkBounds[BOUND_LEFT], chunkBounds[BOUND_RIGHT], true);
			}
		}
		else if (chunkBounds[BOUND_TOP] < prevChunkBounds[BOUND_TOP]) // -Y unload
		{
			for (int c = prevChunkBounds[BOUND_TOP]; c > chunkBounds[BOUND_TOP]; c--)
			{
				project.saveChunkRow(c, chunkBounds[BOUND_LEFT], chunkBounds[BOUND_RIGHT], true, true);
			}
		}

		if (chunkBounds[BOUND_BOTTOM] < prevChunkBounds[BOUND_BOTTOM]) // -Y load
		{
			for (int c = prevChunkBounds[BOUND_BOTTOM] - 1; c >= chunkBounds[BOUND_BOTTOM]; c--)
			{
				project.loadChunkRow(c, chunkBounds[BOUND_LEFT], chunkBounds[BOUND_RIGHT], true);
			}
		}
		else if (chunkBounds[BOUND_BOTTOM] > prevChunkBounds[BOUND_BOTTOM]) // +Y unload
		{
			for (int c = prevChunkBounds[BOUND_BOTTOM]; c < chunkBounds[BOUND_BOTTOM]; c++)
			{
				project.saveChunkRow(c, chunkBounds[BOUND_LEFT], chunkBounds[BOUND_RIGHT], true, true);
			}
		}

		persistChunkBounds();
	}

	/**
	 * Unloads all chunks that were previously visible (if requested) and loads all the chunks that are now visible.
	 * This should be called after initialising the project, after setting (<b>not</b> panning) the camera position, etc.
	 * For any other camera movements, checkSurroundingChunks() should be called instead.
	 * @param save Whether the previously visible chunks should be saved and unloaded.
	 *             If true, the task will <b>not</b> be performed asynchronously.
	 * @param async Whether the task will be performed asynchronously.
	 *              This is overridden if <code>save</code> is true.
	 * @see #checkSurroundingChunks()
	 */
	public void loadAllVisibleChunks(boolean save, boolean async)
	{
		if (save)
		{
			project.saveChunks(project.getLoadedChunkIds(), true);
			loadAllVisibleChunks2(false);
		}
		else
		{
			loadAllVisibleChunks2(async);
		}
	}

	/**
	 * Loads all chunks that are within visible range.
	 * This should only be called from loadAllVisibleChunks().
	 * @param async Whether the task should be performed asynchronously.
	 */
	private void loadAllVisibleChunks2(boolean async)
	{
		calculateChunkBounds();

		for (int cy = chunkBounds[BOUND_BOTTOM]; cy <= chunkBounds[BOUND_TOP]; cy++)
		{
			project.loadChunkRow(cy, chunkBounds[BOUND_LEFT], chunkBounds[BOUND_RIGHT], async);
		}

		persistChunkBounds();
		requestRender();
	}

	/**
	 * Updates the values of the chunk bounds array.
	 */
	private void calculateChunkBounds()
	{
		chunkBounds[BOUND_LEFT] = ChunkUtils.convertXCoord((int) (cameraX - (viewportWidth / zoom) / ChunkUtils.LOADING_DISTANCE_FACTOR));
		chunkBounds[BOUND_RIGHT] = ChunkUtils.convertXCoord((int) (cameraX + (viewportWidth/zoom)/ChunkUtils.LOADING_DISTANCE_FACTOR));
		chunkBounds[BOUND_BOTTOM] = ChunkUtils.convertYCoord((int) (cameraY - (viewportHeight/zoom)/ChunkUtils.LOADING_DISTANCE_FACTOR));
		chunkBounds[BOUND_TOP] = ChunkUtils.convertYCoord((int) (cameraY + (viewportHeight/zoom)/ChunkUtils.LOADING_DISTANCE_FACTOR));
	}

	/**
	 * Copies the current values of <code>chunkBounds</code> into <code>prevChunkBounds</code>.
	 */
	private void persistChunkBounds()
	{
		prevChunkBounds[BOUND_LEFT] = chunkBounds[BOUND_LEFT];
		prevChunkBounds[BOUND_RIGHT] = chunkBounds[BOUND_RIGHT];
		prevChunkBounds[BOUND_BOTTOM] = chunkBounds[BOUND_BOTTOM];
		prevChunkBounds[BOUND_TOP] = chunkBounds[BOUND_TOP];
	}

	/**
	 * Adds a point to the latest stroke.
	 * @param x The X screen coordinate.
	 * @param y The Y screen coordinate.
	 */
	private void addPointToStroke(float x, float y)
	{
		strokePointCount++;
		latestStroke.triangleStrip.addPoint(this, x, y, true);
	}

	/**
	 * Pans the camera.
	 * @param dx The X distance.
	 * @param dy The Y distance.
	 */
	public void pan(float dx, float dy)
	{
		cameraX += dx;
		cameraY += dy;

		updateCurrentSheetCameraValues();

		renderer.updateMvpMatrix();
		checkSurroundingChunks();
		requestRender();
	}

	/**
	 * Centers the camera at the given position.
	 * @param x The X coordinate.
	 * @param y The Y coordinate.
	 */
	public void centerCanvasAt(float x, float y)
	{
		cameraX = x;
		cameraY = y;

		updateCurrentSheetCameraValues();

		renderer.updateMvpMatrix();
		loadAllVisibleChunks(true, true);
		requestRender();
	}

	/**
	 * Sets the camera zoom.
	 * @param factor The zoom factor, between {@link #ZOOM_MINIMUM} and {@link #ZOOM_MAXIMUM}.
	 *               Values outside this range are clamped.
	 */
	public void setZoom(float factor)
	{
		zoom = factor;
		clampZoom();

		updateCurrentSheetCameraValues();

		renderer.updateMvpMatrix();
		checkSurroundingChunks();
		requestRender();
	}

	/**
	 * Sets the camera position and zoom.
	 * @param x The X position.
	 * @param y The Y position.
	 * @param zoom The zoom factor, between {@link #ZOOM_MINIMUM} and {@link #ZOOM_MAXIMUM}.
	 *             Values outside this range are clamped.
	 */
	public void setCameraPositionAndZoom(float x, float y, float zoom)
	{
		cameraX = x;
		cameraY = y;
		this.zoom = zoom;
		clampZoom();

		updateCurrentSheetCameraValues();

		renderer.updateMvpMatrix();
		loadAllVisibleChunks(false, true);
		requestRender();
	}

	/**
	 * Clamps the zoom to be inside the acceptable range.
	 * @see #ZOOM_MINIMUM
	 * @see #ZOOM_MAXIMUM
	 */
	private void clampZoom()
	{
		if (zoom < ZOOM_MINIMUM)
		{
			zoom = ZOOM_MINIMUM;
		}
		else if (zoom > ZOOM_MAXIMUM)
		{
			zoom = ZOOM_MAXIMUM;
		}
	}

	/**
	 * Sets the zoom and pans the camera.
	 * @param factor The zoom factor, between {@link #ZOOM_MINIMUM} and {@link #ZOOM_MAXIMUM}.
	 *               Values outside this range are clamped.
	 * @param dx The X pan distance.
	 * @param dy The Y pan distance.
	 */
	public void setZoomAndPan(float factor, float dx, float dy)
	{
		zoom = factor;
		clampZoom();
		cameraX += dx;
		cameraY += dy;

		updateCurrentSheetCameraValues();

		renderer.updateMvpMatrix();
		checkSurroundingChunks();
		requestRender();
	}

	/**
	 * Updates the sheet's camera values to reflect the current camera position and zoom.
	 * The next time this sheet is switched to, the camera will assume these values.
	 * @see #setCameraFromCurrentSheet()
	 */
	public void updateCurrentSheetCameraValues()
	{
		project.getCurrentSheet().setCamera(cameraX, cameraY, zoom);
	}

	/**
	 * Sets the camera position and zoom to reflect the current sheet's camera values.
	 * @see #updateCurrentSheetCameraValues()
	 */
	public void setCameraFromCurrentSheet()
	{
		Project.Sheet currentSheet = project.getCurrentSheet();
		setCameraPositionAndZoom(currentSheet.getCameraX(), currentSheet.getCameraY(), currentSheet.getCameraZoom());
	}

	/**
	 * Gets the current touch action.
	 * @return The current touch action.
	 * @see #TOUCH_NONE
	 * @see #TOUCH_DRAG
	 * @see #TOUCH_PINCH
	 */
	public int getTouchAction()
	{
		return touchAction;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		// Is a single touch detected?
		if (event.getPointerCount() == 1)
		{
			if (event.getAction() != MotionEvent.ACTION_UP)
			{
				touchXPrev = touchX;
				touchYPrev = touchY;

				touchX = event.getX();
				touchY = event.getY();
			}
			else if (touchAction == TOUCH_PINCH)
			{
				strokeSegmentLength = 0;
				strokePointCount = 0;
				latestStroke = new Stroke(this, true);
				touchAction = TOUCH_NONE;
				renderer.resetRenderCount();
				return true;
			}

			switch (tool)
			{
				case TOOL_ERASER:
				{
					// Do nothing here. Let the TOOL_PENCIL case execute.
				}
				case TOOL_PENCIL:
				{
					switch (event.getAction())
					{
						case MotionEvent.ACTION_DOWN:
						{
							if (touchAction == TOUCH_NONE)
							{
								// Start drawing a new path.
								touchAction = TOUCH_DRAG;

								latestStroke.isEraser = tool == TOOL_ERASER;
								latestStroke.setColor(strokeColor);
								latestStroke.setThickness(strokeThickness);

								strokeSegmentLength = 0f;
								strokePointCount = 0;
								addPointToStroke(touchX, touchY);

								return true;
							}
							return false;
						}
						case MotionEvent.ACTION_MOVE:
						{
							if (touchAction == TOUCH_DRAG)
							{
								// Has the stroke reached the maximum number of points? Stop adding any more points.
								if (strokePointCount >= Stroke.TriangleStrip.STROKE_POINT_LIMIT) return true;

								// Continue drawing a new path.
								strokeSegmentLength += Math.sqrt((touchX - touchXPrev) * (touchX - touchXPrev)
										+ (touchY - touchYPrev) * (touchY - touchYPrev));

								// Has the path distance exceeded the threshold? Add a new point.
								if (strokeSegmentLength >= Stroke.SEGMENT_THRESHOLD)
								{
									strokeSegmentLength = 0f;
									strokePointCount++;
									addPointToStroke(touchX, touchY);
									requestRender();
								}
								return true;
							}
							return false;
						}
						case MotionEvent.ACTION_UP:
						{
							if (touchAction == TOUCH_DRAG)
							{
								if (strokePointCount > 2 && latestStroke != null)
								{
									latestStroke.optimize(this);

									project.getChunkFromId(latestStroke.chunkId).addStroke(latestStroke);
									project.addLoadedStroke(latestStroke);

									project.saveChunks(new long[]{latestStroke.getContainingChunk()}, false);
									project.saveMetadata();
									project.getCurrentSheet().saveMetadata();

									project.incrementStrokeCount();
								}
								latestStroke = new Stroke(this, true);
								requestRender();
							}

							touchAction = TOUCH_NONE;
							renderer.resetRenderCount();

							return true;
						}
					}
					break;
				}
			}
		}
		// Is a double touch detected?
		else if (event.getPointerCount() == 2)
		{
			// Prepare the necessary variables for the pinch/drag.
			if (event.getAction() != MotionEvent.ACTION_UP)
			{
				/* For some reason, MotionEvent.getPointerId(int) can throw an IllegalArgumentException
				 * even when MotionEvent.getPointerCount() is 2. If this happens, catch the exception
				 * and discard the touch operation. */
				try
				{
					touchXPrev = touchX;
					touchYPrev = touchY;

					touchX = event.getX(event.getPointerId(0));
					touchY = event.getY(event.getPointerId(0));

					touchX2 = event.getX(event.getPointerId(1));
					touchY2 = event.getY(event.getPointerId(1));

					pinchCenterXPrev = pinchCenterX;
					pinchCenterYPrev = pinchCenterY;

					pinchCenterX = (touchX + touchX2)/2f;
					pinchCenterY = (touchY + touchY2)/2f;
				}
				catch (IllegalArgumentException e)
				{
					touchAction = TOUCH_NONE;
					renderer.resetRenderCount();
					latestStroke = new Stroke(this, true);

					return true;
				}
			}

			switch (event.getAction())
			{
				case MotionEvent.ACTION_DOWN:
				{
					return false; // Nothing should happen here, so reject the event.
				}
				case MotionEvent.ACTION_MOVE:
				{
					if (touchAction != TOUCH_PINCH)
					{
						initialPinchDistance = (float) Math.sqrt((touchX - touchX2) * (touchX - touchX2) + (touchY - touchY2) * (touchY - touchY2));
						initialZoom = zoom;
						zoomPrev = zoom;
						touchAction = TOUCH_PINCH;
					}

					float pinchDistance = (float) Math.sqrt((touchX - touchX2) * (touchX - touchX2) + (touchY - touchY2) * (touchY - touchY2));
					float pinchDistanceRatio = pinchDistance / initialPinchDistance;

					float pinchCenterDiffX = pinchCenterX - pinchCenterXPrev;
					float pinchCenterDiffY = pinchCenterY - pinchCenterYPrev;

					float vectorX = (pinchCenterX - (viewportWidth/2f))/zoom;
					float vectorY = (pinchCenterY - (viewportHeight/2f))/zoom;

					float zoomDiff = zoom - zoomPrev;

					zoomPrev = zoom;
					setZoomAndPan(initialZoom * pinchDistanceRatio, -(pinchCenterDiffX - vectorX * zoomDiff)/zoom, (pinchCenterDiffY - vectorY * zoomDiff)/zoom);

					return true;
				}
				case MotionEvent.ACTION_UP:
				{
					touchAction = TOUCH_NONE;
					renderer.resetRenderCount();

					return true;
				}
			}
		}

		return false;
	}
}
