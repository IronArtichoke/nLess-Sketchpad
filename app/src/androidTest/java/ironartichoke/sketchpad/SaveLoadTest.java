package ironartichoke.sketchpad;

import android.support.test.runner.AndroidJUnit4;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import ironartichoke.sketchpad.base.NotepadTestBase;
import ironartichoke.sketchpad.util.TestUtils;
import ironartichoke.sketchpad.util.io.IOUtils;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withHint;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withTagKey;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * A test class containing tests that deal with saving and loading sketchbooks.
 */
@RunWith(AndroidJUnit4.class)
public class SaveLoadTest extends NotepadTestBase
{
	/**
	 * Test that sketchbooks are properly saved.
	 */
	@Test
	public void saveProject()
	{
		// Save a sample project.
		saveSampleProject("Sketchbook 1");

		// Check that the project files are all present.
		assertTrue(new File(IOUtils.getDirectory(), "Sketchbook 1.tar.gz").exists());
		assertTrue(new File(IOUtils.getDirectory(), "Sketchbook 1.png").exists());
	}

	/**
	 * Test that the sketchbook that the user was working on during the previous session is
	 * properly reloaded upon relaunching the app.
	 */
	@Test
	public void saveProjectAndLoadOnResume()
	{
		// Save a sample project.
		saveSampleProject("Sketchbook 2");

		// Restart the app.
		restart();

		// Check that the project has been extracted successfully after loading:
		checkProjectIntegrity("Sketchbook 2");
	}

	/**
	 * Test that loading sketchbooks from the Open screen is functioning correctly.
	 * @throws InterruptedException
	 */
	@Test
	public void saveProjectAndLoadFromMenu() throws InterruptedException
	{
		// Save a sample project.
		saveSampleProject("Sketchbook 3");

		// Start a new sketchbook.
		openMainMenuItem(R.id.linearlayout_menu_new_project);

		// Open the sketchbook that was saved.
		openMainMenuItem(R.id.linearlayout_menu_open);
		onData(instanceOf(File.class)).atPosition(0).perform(click());
		Thread.sleep(500);

		// Check that the project is loaded correctly.
		checkProjectIntegrity("Sketchbook 3");
	}

	/**
	 * Test that a sketchbook and its file can be renamed properly.
	 */
	@Test
	public void renameProject()
	{
		// Save a sketchbook.
		saveSampleProject("Original project name");

		// Rename it.
		openMainMenuItem(R.id.linearlayout_menu_open);
		onData(instanceOf(File.class)).atPosition(0).perform(longClick());
		onView(withText(R.string.button_rename)).perform(click());
		onView(withHint("Original project name")).perform(typeText("New project name"));
		onView(withText(R.string.button_ok)).perform(click());

		// Check that the sketchbook has the new name displayed in the Open screen.
		onData(instanceOf(File.class)).atPosition(0).onChildView(withId(R.id.textview_project_name)).check(matches(withText("New project name")));

		// Go back to the main activity.
		pressBack();

		// Check that the project name is correct.
		assertEquals("New project name", project().getName());

		// Check that the project's filename has been changed.
		assertEquals("New project name.tar.gz", project().getArchiveFile().getName());
	}

	/**
	 * Test that a sketchbook can be deleted properly.
	 */
	@Test
	public void deleteProject()
	{
		// Save a sketchbook.
		saveSampleProject("Delete me");

		// Delete it.
		openMainMenuItem(R.id.linearlayout_menu_open);
		onData(instanceOf(File.class)).atPosition(0).perform(longClick());
		onView(withText(R.string.button_delete)).perform(click());
		onView(withText(R.string.button_yes)).perform(click());

		// Check that the sketchbook list no longer contains the project.
		onView(withTagKey(R.id.tag_name, Matchers.<Object>equalTo("Delete me"))).check(doesNotExist());

		// Check that the file is deleted.
		assertFalse(new File(IOUtils.getDirectory(), "Delete me.tar.gz").exists());
	}

	/**
	 * A convenience method for saving a basic sketchbook.
	 * @param projectName The name for the sketchbook.
	 */
	private void saveSampleProject(String projectName)
	{
		// Draw something.
		TestUtils.drawSample(activity, view);

		// Save the project.
		openMainMenuItem(R.id.linearlayout_menu_save);
		onView(withHint(R.string.hint_sketchbook_name)).perform(typeText(projectName));
		onView(withText(R.string.button_ok)).perform(click());

		// Wait for a little bit.
		try
		{
			Thread.sleep(500);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Checks that the specified sketchbook is loaded and exists on the filesystem.
	 * @param projectName The name of the sketchbook.
	 */
	private void checkProjectIntegrity(String projectName)
	{
		// 1. The temporary directory exists.
		assertTrue(IOUtils.getWorkingDirectory().exists());

		// 2. The project metadata file exists.
		assertTrue(new File(IOUtils.getWorkingDirectory(), "meta").exists());

		// 3. The project name is correct.
		assertEquals(projectName, project().getName());

		// 4. For each sheet:
		for (Project.Sheet sheet : project().getSheets())
		{
			// i. The sheet directory exists.
			File sheetDir = new File(IOUtils.getWorkingDirectory(), Long.toString(sheet.getId()));
			assertTrue(sheetDir.exists());

			// ii. The sheet metadata file exists.
			assertTrue(new File(sheetDir, "meta").exists());
		}
	}
}
