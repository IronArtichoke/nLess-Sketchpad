package ironartichoke.sketchpad;

import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import de.keyboardsurfer.android.widget.crouton.Style;
import ironartichoke.sketchpad.base.NotepadTestBase;
import ironartichoke.sketchpad.util.CroutonHelper;
import ironartichoke.sketchpad.util.io.ExternalStorageHelper;
import ironartichoke.sketchpad.util.io.IOUtils;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * A test class containing tests that deal with the external storage.
 */
@RunWith(AndroidJUnit4.class)
public class ExternalStorageTest extends NotepadTestBase
{
	@Override
	public void before()
	{
		super.before();

		IOUtils.externalStorageHelper = mock(ExternalStorageHelper.class);

		Prefs.edit().putInt(R.string.pref_storage, IOUtils.INTERNAL_STORAGE);

		activity.croutonHelper = mock(CroutonHelper.class);
	}

	/**
	 * Test that the external storage state is being mocked correctly.
	 */
	@Test
	public void mockingSanityTest()
	{
		// Test with direct comparison.
		mockState("State test string");
		assertEquals(IOUtils.getExternalStorageState(), "State test string");

		// Test against dedicated methods.
		mockState(Environment.MEDIA_REMOVED);
		assertFalse(IOUtils.isExternalStorageReadable());
		mockState(Environment.MEDIA_MOUNTED_READ_ONLY);
		assertTrue(IOUtils.isExternalStorageReadable());
		assertFalse(IOUtils.isExternalStorageWritable());
	}

	/**
	 * Test that the external storage cannot be selected when it is inaccessible.
	 */
	@Test
	public void attemptToSwitchToExternalStorageWhenUnreadable()
	{
		// Mock the external storage to be unreadable.
		mockState(Environment.MEDIA_UNMOUNTED);

		// Click the external storage menu item.
		clickStorageMenuItem();

		// Verify that the failure crouton was shown.
		verify(activity.croutonHelper).show(activity, R.string.toast_external_storage_failure, Style.ALERT);
	}

	/**
	 * Test that a sketchbook cannot be saved to the external storage when it is read-only.
	 */
	@Test
	public void attemptToSaveProjectWhenExternalStorageUnwritable()
	{
		// Mock the external storage to be read-only.
		mockState(Environment.MEDIA_MOUNTED_READ_ONLY);

		// Click the external storage menu item and confirm the dialog.
		clickStorageMenuItem();
		onView(withText(R.string.button_yes)).perform(click());

		// Attempt to save the project.
		openMainMenuItem(R.id.linearlayout_menu_save);

		// Confirm that the error crouton is shown.
		verify(activity.croutonHelper).show(activity, R.string.toast_cannot_save_read_only, Style.ALERT);
	}

	/**
	 * Confirm that, when the external storage is selected when read-only, a dialog is displayed warning the user
	 * of the inability to save their work.
	 */
	@Test
	public void openDialogWhenStorageIsUnwritable()
	{
		// Mock the external storage to be read-only.
		mockState(Environment.MEDIA_MOUNTED_READ_ONLY);

		// Click the external storage menu item.
		clickStorageMenuItem();

		// Is the dialog shown with the warning that the external storage is read-only?
		onView(withText(R.string.dialog_confirm_external_storage_read_only)).inRoot(isDialog()).check(matches(isDisplayed()));
	}

	/**
	 * Test that the storage switching mechanism is working correctly.
	 */
	@Test
	public void switchToExternalStorageAndBack()
	{
		// Mock the external storage for full access.
		mockState(Environment.MEDIA_MOUNTED);

		// Click the external storage menu item and confirm the dialog.
		clickStorageMenuItem();
		onView(withText(R.string.button_yes)).perform(click());

		// Is the external storage selected?
		assertTrue(IOUtils.isUsingExternalStorage());

		// Click the menu item again to switch back.
		clickStorageMenuItem();
		onView(withText(R.string.button_yes)).perform(click());

		// Is the internal storage selected?
		assertTrue(IOUtils.isUsingInternalStorage());
	}

	/**
	 * Test that, if the SD card somehow becomes inaccessible at runtime when external storage is selected,
	 * the warning dialog is displayed.
	 * @throws InterruptedException
	 */
	@Test
	public void sdCardWarnings() throws InterruptedException
	{
		// In Kitkat and above, system broadcasts cannot be sent.
		// If this test is being run on such a system, fail the test giving the reason for doing so.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
		{
			fail("System broadcasts cannot be sent on this version of Android (Kitkat and above). " +
			     "Please rerun this test on an earlier version.");
		}

		// Make the external storage available and use it.
		mockState(Environment.MEDIA_MOUNTED);
		clickStorageMenuItem();
		onView(withText(R.string.button_yes)).perform(click());

		// Prepare a list of intent actions to be tested against.
		String[] actions = new String[]{
				Intent.ACTION_MEDIA_REMOVED,
				Intent.ACTION_MEDIA_UNMOUNTED,
				Intent.ACTION_MEDIA_EJECT,
				Intent.ACTION_MEDIA_BAD_REMOVAL
		};

		// For each of the actions, check that the warning dialog is shown when the broadcast intent is received.
		for (String action : actions)
		{
			// Broadcast the intent.
			Intent intent = new Intent();
			intent.setAction(action);
			activity.sendBroadcast(intent);
			Thread.sleep(250);

			// Confirm that the warning dialog is shown.
			onView(withText(R.string.dialog_sdcard_removed_message)).check(matches(isDisplayed()));
			onView(withText(R.string.button_ok)).perform(click());
			Thread.sleep(250);
		}
	}

	/**
	 * Mocks the state of the external storage.
	 * @param state Any of the <code>MEDIA</code> constants in {@link android.os.Environment}.
	 */
	private void mockState(String state)
	{
		Mockito.doReturn(state).when(IOUtils.externalStorageHelper).getState();
	}

	/**
	 * A convenience method for selecting the storage item from the main menu.
	 */
	private void clickStorageMenuItem()
	{
		openMainMenuItem(R.id.linearlayout_menu_storage);
	}
}
