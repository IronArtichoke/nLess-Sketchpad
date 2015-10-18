package ironartichoke.sketchpad;

import android.support.test.espresso.action.GeneralLocation;
import android.support.test.espresso.action.GeneralSwipeAction;
import android.support.test.espresso.action.Press;
import android.support.test.espresso.action.Swipe;
import android.support.test.espresso.action.ViewActions;

import org.junit.Test;

import ironartichoke.sketchpad.base.NotepadTestBase;
import ironartichoke.sketchpad.util.ChunkUtils;
import ironartichoke.sketchpad.util.TestUtils;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.matcher.ViewMatchers.withHint;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * A test class containing tests that deal with various aspects of sketchbook modification, like
 * drawing strokes, adding/removing sheets and saving/loading chunks.
 */
public class ProjectTest extends NotepadTestBase
{
	/**
	 * Test that strokes are properly added to the sketchbook when drawn.
	 */
	@Test
	public void drawSomething()
	{
		// Reset the camera.
		view.setZoomAndPan(NotepadView.ZOOM_DEFAULT, 0, 0);

		// Simulate a drawing action with 5 points.
		TestUtils.dispatchDrag(activity, view, System.currentTimeMillis(), TestUtils.DRAG_DELAY,
				   0,    0,
				 100,    0,
				   0,  100,
				-100,    0,
				   0, -100);

		// Does there exist a single loaded stroke?
		assertEquals(1, project().getLoadedStrokes().size());

		// Is the stroke non-null?
		assertNotNull(project().getLoadedStroke(0));

		// Is the stroke defined by 5 points?
		assertEquals(5, project().getLoadedStroke(0).getNumberOfPoints());
	}

	/**
	 * Test that chunk saving and loading are working correctly.
	 * @throws InterruptedException
	 */
	@Test
	public void chunkSavingAndReloading() throws InterruptedException
	{
		// Draw a stroke in each of the four center chunks.
		TestUtils.drawSample(activity, view);

		// Check that the number of strokes in each of the four center chunks is correct.
		assertEquals(1, project().getChunkFromId(ChunkUtils.convertCoordsToChunk(-75, -75)).getTotalStrokes());
		assertEquals(1, project().getChunkFromId(ChunkUtils.convertCoordsToChunk(75, -75)).getTotalStrokes());
		assertEquals(1, project().getChunkFromId(ChunkUtils.convertCoordsToChunk(-75, 75)).getTotalStrokes());
		assertEquals(1, project().getChunkFromId(ChunkUtils.convertCoordsToChunk(75, 75)).getTotalStrokes());

		// Ensure that there are 4 strokes currently in memory.
		assertEquals(4, project().getLoadedStrokes().size());

		// Pan the camera in the +X,-Y direction.
		for (int i = 0; i < 25; i++)
		{
			view.pan(100*i, 100*i);
			Thread.sleep(25);
		}
		Thread.sleep(500);

		// Check that no strokes are loaded.
		assertEquals(0, project().getLoadedStrokes().size());

		// Pan back to the center in the -X,+Y direction.
		for (int i = 0; i < 25; i++)
		{
			view.pan(-100*i, -100*i);
			Thread.sleep(25);
		}
		Thread.sleep(500);

		// Check that the strokes are present and loaded.
		assertEquals(4, project().getLoadedStrokes().size());

		// Pan the camera in the -X,+Y direction.
		for (int i = 0; i < 25; i++)
		{
			view.pan(-100*i, 100*i);
			Thread.sleep(25);
		}
		Thread.sleep(500);

		// Check that no strokes are loaded.
		assertEquals(0, project().getLoadedStrokes().size());

		// Pan back to the center in the +X,-Y direction.
		for (int i = 0; i < 25; i++)
		{
			view.pan(100*i, -100*i);
			Thread.sleep(25);
		}
		Thread.sleep(500);

		// Check that the strokes are present and loaded.
		assertEquals(4, project().getLoadedStrokes().size());

		// Set the camera location to somewhere far away.
		view.centerCanvasAt(123456, 789012);
		Thread.sleep(300);

		// Check for no strokes.
		assertEquals(0, project().getLoadedStrokes().size());

		// Reset the camera position.
		view.centerCanvasAt(0, 0);
		Thread.sleep(300);

		// Check four 4 strokes again.
		assertEquals(4, project().getLoadedStrokes().size());
	}

	/**
	 * Test that sheets can be added and removed.
	 * @throws InterruptedException
	 */
	@Test
	public void addAndRemoveSheet() throws InterruptedException
	{
		// Add a sheet.
		addSampleSheet();

		// Check that the current sheet is correct.
		assertEquals(activity.getString(R.string.term_sheet) + " 2", project().getCurrentSheet().getName());

		// Check that there are two sheets in the sketchbook.
		assertEquals(2, project().getTotalSheets());

		// Remove the new sheet.
		onView(withId(R.id.imageview_sheet_button)).perform(click());
		onData(instanceOf(Project.Sheet.class)).atPosition(1).onChildView(withId(R.id.imageview_sheet_button_delete)).perform(click());
		onView(withText(R.string.button_yes)).perform(click());
		Thread.sleep(500);

		// Check that the current sheet is back to the original.
		assertEquals(activity.getString(R.string.term_sheet), project().getCurrentSheet().getName());

		// Check that there is only one sheet in the sketchbook.
		assertEquals(1, project().getTotalSheets());
	}

	/**
	 * Test that sheets can be renamed properly.
	 * @throws InterruptedException
	 */
	@Test
	public void renameSheet() throws InterruptedException
	{
		// Rename the current sheet.
		onView(withId(R.id.imageview_sheet_button)).perform(click());
		onData(instanceOf(Project.Sheet.class)).atPosition(0).onChildView(withId(R.id.imageview_sheet_button_rename)).perform(click());
		onView(withHint(project().getSheetAt(0).getName())).perform(typeText("Renamed sheet"));
		onView(withText(activity.getString(R.string.button_ok))).perform(click());
		Thread.sleep(500);

		// Check that the sheet has the new name.
		assertEquals("Renamed sheet", project().getCurrentSheet().getName());
	}

	/**
	 * Test that sheets can be reordered correctly.
	 * @throws InterruptedException
	 */
	@Test
	public void reorderSheets() throws InterruptedException
	{
		// Add a sheet.
		addSampleSheet();

		// Swap the sheets.
		onView(withId(R.id.imageview_sheet_button)).perform(click());
		onData(instanceOf(Project.Sheet.class)).atPosition(0).onChildView(withId(R.id.imageview_sheet_drag_handle)).perform(
				ViewActions.actionWithAssertions(new GeneralSwipeAction(
						Swipe.FAST,
						TestUtils.translateEspressoCoordinate(GeneralLocation.TOP_CENTER, 0f, 0f),
						TestUtils.translateEspressoCoordinate(GeneralLocation.BOTTOM_CENTER, 0f, 2f),
						Press.FINGER))
		);
		Thread.sleep(500);

		// Check the new order.
		assertEquals(0, project().getCurrentSheetIndex());
		assertEquals(activity.getString(R.string.term_sheet) + " 2", project().getCurrentSheet().getName());

		// Select the other sheet.
		onData(instanceOf(Project.Sheet.class)).atPosition(1).perform(click());
		Thread.sleep(500);

		// Check the current sheet.
		assertEquals(1, project().getCurrentSheetIndex());
		assertEquals(activity.getString(R.string.term_sheet), project().getCurrentSheet().getName());
	}

	/**
	 * Test that sheets can be deleted properly.
	 * @throws InterruptedException
	 */
	@Test
	public void deleteSheet() throws InterruptedException
	{
		// Add a sheet.
		addSampleSheet();

		// Delete the new sheet.
		onView(withId(R.id.imageview_sheet_button)).perform(click());
		onData(instanceOf(Project.Sheet.class)).atPosition(1).onChildView(withId(R.id.imageview_sheet_button_delete)).perform(click());
		onView(withText(activity.getString(R.string.button_yes))).perform(click());
		Thread.sleep(500);

		// Check that the sheet is no longer in the list.
		onView(withText(activity.getString(R.string.term_sheet) + " 2")).check(doesNotExist());

		// Check that the current sheet is correct.
		assertEquals(0, project().getCurrentSheetIndex());
		assertEquals(activity.getString(R.string.term_sheet), project().getCurrentSheet().getName());
	}

	/**
	 * A convenience method for adding a new sheet.
	 * @throws InterruptedException
	 */
	private void addSampleSheet() throws InterruptedException
	{
		onView(withId(R.id.imageview_sheet_button)).perform(click());
		onView(withId(R.id.sheet_drawer_footer)).perform(click());
		Thread.sleep(1000);
		onView(withText(activity.getString(R.string.button_ok))).perform(click());
		Thread.sleep(500);
	}
}
