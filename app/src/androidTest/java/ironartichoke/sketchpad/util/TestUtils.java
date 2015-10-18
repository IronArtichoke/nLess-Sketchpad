package ironartichoke.sketchpad.util;

import android.app.Activity;
import android.graphics.Point;
import android.support.test.espresso.action.CoordinatesProvider;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;

public class TestUtils
{
	public static final long DRAG_DELAY = 50;

	public static void dispatchDrag(Activity activity, View view, long downTime, long diffTime, float... xys)
	{
		Display display = activity.getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		int halfWidth = size.x / 2;
		int halfHeight = size.y / 2;

		if (xys.length == 0 || xys.length % 2 != 0)
		{
			throw new IllegalArgumentException("xys must have a length greater than 0 and divisible by 2.");
		}

		MotionEvent[] events = new MotionEvent[xys.length/2 + 1];

		int action = MotionEvent.ACTION_DOWN;
		long currentDownTime = downTime;
		long currentEventTime = downTime + diffTime;
		for (int e = 0; e < xys.length; e += 2)
		{
			events[e/2] = MotionEvent.obtain(currentDownTime, currentEventTime, action, xys[e] + halfWidth, xys[e+1] + halfHeight, 0);
			currentDownTime += diffTime;
			currentEventTime += diffTime;
			if (e == 0) action = MotionEvent.ACTION_MOVE;
		}
		events[events.length - 1] = MotionEvent.obtain(
				currentDownTime, currentEventTime, MotionEvent.ACTION_UP, xys[xys.length - 2] + halfWidth, xys[xys.length - 1] + halfHeight, 0);

		for (MotionEvent event : events)
		{
			view.dispatchTouchEvent(event);
			try
			{
				Thread.sleep(diffTime);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}

	public static void drawSample(Activity activity, View view)
	{
		// Draw a stroke in each of the four center chunks.
		TestUtils.dispatchDrag(activity, view, System.currentTimeMillis(), TestUtils.DRAG_DELAY,
				-100, -100,
				-50,  -50,
				0,    0);
		TestUtils.dispatchDrag(activity, view, System.currentTimeMillis(), TestUtils.DRAG_DELAY,
				100, -100,
				50,  -50,
				0,    0);
		TestUtils.dispatchDrag(activity, view, System.currentTimeMillis(), TestUtils.DRAG_DELAY,
				-100, 100,
				-50,  50,
				0,   0);
		TestUtils.dispatchDrag(activity, view, System.currentTimeMillis(), TestUtils.DRAG_DELAY,
				100, 100,
				50,  50,
				0,   0);
	}

	public static CoordinatesProvider translateEspressoCoordinate(final CoordinatesProvider coords, final float dx, final float dy)
	{
		return new CoordinatesProvider() {
			public float[] calculateCoordinates(View view) {
				float[] xy = coords.calculateCoordinates(view);
				xy[0] += dx * (float)view.getWidth();
				xy[1] += dy * (float)view.getHeight();
				return xy;
			}
		};
	}
}
