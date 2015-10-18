package ironartichoke.sketchpad.base;

import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;

import ironartichoke.sketchpad.NotepadActivity;
import ironartichoke.sketchpad.NotepadView;
import ironartichoke.sketchpad.Prefs;
import ironartichoke.sketchpad.Project;
import ironartichoke.sketchpad.R;
import ironartichoke.sketchpad.util.AppearanceUtils;
import ironartichoke.sketchpad.util.io.DirectoryHelper;
import ironartichoke.sketchpad.util.io.IOUtils;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

public class NotepadTestBase
{
	private TemporaryFolder internalTopDirectory = new TemporaryFolder();
	private TemporaryFolder externalTopDirectory = new TemporaryFolder();
	private ActivityTestRule<NotepadActivity> activityRule = new ActivityTestRule<NotepadActivity>(NotepadActivity.class)
	{
		@Override
		protected Intent getActivityIntent()
		{
			Intent intent = new Intent("android.intent.action.MAIN");
			intent.putExtra(DirectoryHelper.EXTRA_INTERNAL_TOP_DIR, internalTopDirectory.getRoot());
			intent.putExtra(DirectoryHelper.EXTRA_EXTERNAL_TOP_DIR, externalTopDirectory.getRoot());
			return intent;
		}
	};

	@Rule
	public RuleChain ruleChain = RuleChain
			.outerRule(internalTopDirectory)
			.around(externalTopDirectory)
			.around(activityRule);

	protected NotepadActivity activity;
	protected NotepadView view;

	@Before
	public void before()
	{
		// Allow Mockito to use Dexmaker.
		System.setProperty(
				"dexmaker.dexcache",
				InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir().getPath());

		Prefs.edit().putInt(R.string.pref_brightness, AppearanceUtils.THEME_LIGHT)
				.putInt(R.string.pref_handedness, AppearanceUtils.RIGHT_HANDED)
				.putBoolean(R.string.pref_tutorial_done, true)
				.putInt(R.string.pref_storage, IOUtils.INTERNAL_STORAGE).commit();

		activity = activityRule.getActivity();
		view = (NotepadView) activity.findViewById(R.id.view);

		// Wait for a little while to allow the sketchbook to initialize.
		try
		{
			Thread.sleep(500);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}

	@After
	public void after()
	{
		Prefs.edit().putBoolean(R.string.pref_dirty_exit, false)
		            .putInt(R.string.pref_storage, IOUtils.INTERNAL_STORAGE).commit();
	}

	/**
	 * A convenience method for fetching the current project.
	 * @return The project.
	 */
	protected Project project()
	{
		return view.project;
	}

	/**
	 * A convenience method for selecting an item from the main menu.
	 * The main menu will open if it is not already open.
	 */
	protected void openMainMenuItem(int menuItemViewId)
	{
		if (!activity.isMenuOpen(NotepadActivity.MENU_MAIN))
		{
			onView(withId(R.id.imageview_main_button)).perform(click());
		}
		onView(withId(menuItemViewId)).perform(click());
	}

	/**
	 * Restarts the activity.
	 */
	protected void restart()
	{
		activity.exit();
		activity.startActivity(activity.getIntent());
		try
		{
			Thread.sleep(1000);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}
}
