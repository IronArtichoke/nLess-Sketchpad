package ironartichoke.sketchpad;

import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import ironartichoke.sketchpad.base.NotepadTestBase;
import ironartichoke.sketchpad.util.AppearanceUtils;
import ironartichoke.sketchpad.util.ColorUtils;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * A test class containing tests that deal with the user interface.
 */
@RunWith(AndroidJUnit4.class)
public class UITest extends NotepadTestBase
{
	/**
	 * Test that the main menu is opened and closed correctly.
	 */
	@Test
	public void openAndCloseMainMenu()
	{
		openAndCloseMenu(R.id.imageview_main_button, NotepadActivity.MENU_MAIN, R.id.include_menu);
	}

	/**
	 * Test that the sheet menu is opened and closed correctly.
	 */
	@Test
	public void openAndCloseSheetMenu()
	{
		openAndCloseMenu(R.id.imageview_sheet_button, NotepadActivity.MENU_SHEET, R.id.include_sheet);
	}

	/**
	 * Test that the stroke menu is opened and closed correctly.
	 */
	@Test
	public void openAndCloseStrokeMenu()
	{
		openAndCloseMenu(R.id.view_toolbar_stroke_settings, NotepadActivity.MENU_STROKE, R.id.include_color, R.id.include_thickness);
	}

	/**
	 * Test that the pencil tool and eraser tool are toggled correctly.
	 */
	@Test
	public void toggleTool()
	{
		// Click the tool button.
		onView(withId(R.id.imageview_toolbar_pencil_eraser)).perform(click());

		// Is the eraser now the active tool?
		assertEquals(NotepadView.TOOL_ERASER, view.tool);

		// Click the tool button again.
		onView(withId(R.id.imageview_toolbar_pencil_eraser)).perform(click());

		// Is the pencil now the active tool?
		assertEquals(NotepadView.TOOL_PENCIL, view.tool);
	}

	/**
	 * Test that the color selection menu is working.
	 */
	@Test
	public void selectStrokeColor()
	{
		// Open the stroke menu.
		onView(withId(R.id.view_toolbar_stroke_settings)).perform(click());

		// Iterate through each color and check that it is properly selectable.
		for (byte c = 0; c < ColorUtils.COLOR_RES_IDS.length - 1; c++)
		{
			// Click the color button.
			onView(withId(ColorUtils.getColorPaletteButtonId(c))).perform(click());

			// Does the selected color match?
			assertEquals(c, view.strokeColor);

			// Re-open the menu via runnable to speed up the test.
			view.post(new Runnable()
			{
				@Override
				public void run()
				{
					activity.openStrokeSettings();
				}
			});
		}
	}

	/**
	 * Test that the thickness selection menu is working.
	 */
	@Test
	public void selectStrokeThickness()
	{
		// Open the stroke menu.
		onView(withId(R.id.view_toolbar_stroke_settings)).perform(click());

		// Iterate through each thickness and check that it is properly selectable.
		for (byte t = 0; t <= 4; t++)
		{
			// Click the thickest button.
			onView(withId(NotepadActivity.thicknessButtonIds[t])).perform(click());

			// Does the selected thickness match?
			assertEquals(t, view.strokeThickness);

			// Re-open the menu via runnable to speed up the test.
			view.post(new Runnable()
			{
				@Override
				public void run()
				{
					activity.openStrokeSettings();
				}
			});
		}
	}

	/**
	 * Test that the zoom reset button is working.
	 */
	@Test
	public void resetZoom()
	{
		// Set the zoom to an arbitrary value.
		view.setZoom(3.14f);

		// Click the zoom button.
		onView(withId(R.id.imageview_toolbar_button_zoom)).perform(click());

		// Check that the zoom now equals the default.
		assertEquals(Float.compare(view.zoom, NotepadView.ZOOM_DEFAULT), 0);
	}

	/**
	 * Test that the camera position reset button is working.
	 */
	@Test
	public void resetPan()
	{
		// Set the camera position to an arbitrary value.
		view.pan(123, 456);

		// Click the position button.
		onView(withId(R.id.imageview_toolbar_button_position)).perform(click());

		// Check that the position now equals the origin.
		assertEquals(Float.compare(view.cameraX, 0), 0);
		assertEquals(Float.compare(view.cameraY, 0), 0);
	}

	/**
	 * Test that light mode and dark mode are toggled correctly.
	 */
	@Test
	public void toggleBrightness()
	{
		// Select the brightness item in the main menu.
		onView(withId(R.id.imageview_main_button)).perform(click());
		onView(withId(R.id.linearlayout_menu_brightness)).perform(click());

		// Is dark mode active?
		assertTrue(AppearanceUtils.isThemeDark());

		// Select the brightness item again.
		onView(withId(R.id.linearlayout_menu_brightness)).perform(click());

		// Is light mode active?
		assertTrue(AppearanceUtils.isThemeLight());
	}

	/**
	 * Test that right- and left-handed mode are toggled correctly.
	 */
	@Test
	public void toggleHandedness()
	{
		// Select the handedness item in the main menu.
		onView(withId(R.id.imageview_main_button)).perform(click());
		onView(withId(R.id.linearlayout_menu_handedness)).perform(click());

		// Is left-handed mode active?
		assertTrue(AppearanceUtils.isLeftHanded());

		// Select the handedness item again.
		onView(withId(R.id.linearlayout_menu_handedness)).perform(click());

		// Is right-handed mode active?
		assertTrue(AppearanceUtils.isRightHanded());
	}

	/**
	 * A helper method for testing that a menu opens and closes correctly.
	 * Make sure that the menu under test is initially closed.
	 * @param buttonId The ID of the button that should be pressed to toggle the menu.
	 * @param menuConstant The constant that represents the menu.
	 * @param menuViewIds The ID(s) of the menu view(s).
	 * @see NotepadActivity#MENU_MAIN
	 * @see NotepadActivity#MENU_SHEET
	 * @see NotepadActivity#MENU_STROKE
	 */
	private void openAndCloseMenu(int buttonId, int menuConstant, int... menuViewIds)
	{
		// Click the button.
		onView(withId(buttonId)).perform(click());

		// Is the menu open? (1st method)
		assertTrue(activity.isMenuOpen(menuConstant));

		// Is the menu open? (2nd method)
		assertEquals(menuConstant, activity.currentlyOpenPanel);

		// Are the associated menu views visible?
		for (int id : menuViewIds)
		{
			onView(withId(id)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
		}

		// Click the button again.
		onView(withId(buttonId)).perform(click());

		// Is the menu closed? (1st method)
		assertFalse(activity.isMenuOpen(menuConstant));

		// Is the menu closed? (2nd method)
		assertEquals(0, activity.currentlyOpenPanel);

		// Are the associated menu views invisible?
		for (int id : menuViewIds)
		{
			onView(withId(id)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
		}
	}
}
