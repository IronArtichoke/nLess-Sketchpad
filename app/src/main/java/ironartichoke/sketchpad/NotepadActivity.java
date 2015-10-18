package ironartichoke.sketchpad;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import com.nhaarman.listviewanimations.itemmanipulation.DynamicListView;
import com.nhaarman.listviewanimations.itemmanipulation.dragdrop.TouchViewDraggableManager;
import com.wnafee.vector.compat.ResourcesCompat;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import de.psdev.licensesdialog.LicensesDialog;
import ironartichoke.sketchpad.adapter.SheetAdapter;
import ironartichoke.sketchpad.util.AppearanceUtils;
import ironartichoke.sketchpad.util.ColorUtils;
import ironartichoke.sketchpad.util.CroutonHelper;
import ironartichoke.sketchpad.util.LicenseUtils;
import ironartichoke.sketchpad.util.Reflector;
import ironartichoke.sketchpad.util.io.DirectoryHelper;
import ironartichoke.sketchpad.util.io.ExternalStorageHelper;
import ironartichoke.sketchpad.util.io.IOUtils;

public class NotepadActivity extends Activity
{
	/** The root view. */
	private ViewGroup rootView;
	/** The <code>NotepadView</code>. */
	private NotepadView notepadView;

	/** The drawing toolbar. */
	private LinearLayout drawingToolbar;
	/** The line thickness toolbar, opened from the drawing toolbar. */
	private LinearLayout thicknessSelector;
	/** The line color toolbar, opened from the drawing toolbar. */
	private TableLayout colorPalette;
	/** The pencil/eraser button in the drawing toolbar. */
	private ImageView buttonDrawTool;
	/** The camera toolbar. */
	private LinearLayout cameraToolbar;
	/** The sheet menu, accessed from the main toolbar. */
	private LinearLayout sheetMenu;
	/** The adapter for the sheet list, displayed in the sheet menu. */
	private SheetAdapter sheetArrayAdapter;
	/** The X translation of the sheet drawer, in pixels, at the time of the view's creation. */
	private float initialSheetDrawerTranslationX;
	/** The main toolbar. */
	private LinearLayout mainToolbar;
	/** The main menu, accessed from the main toolbar. */
	private RelativeLayout menu;
	/** The Y translation of the main menu, in pixels, at the time of the view's creation. */
	private float initialMenuTranslationY;

	/** A value that indicates which menu is currently open. Either {@link #MENU_STROKE}, {@link #MENU_SHEET} or {@link #MENU_MAIN}. */
	public int currentlyOpenPanel = 0;
	/** A constant representing the stroke settings menu.
	 * @see #currentlyOpenPanel */
	public static final int MENU_STROKE = 1;
	/** A constant representing the sheet menu.
	 * @see #currentlyOpenPanel */
	public static final int MENU_SHEET = 2;
	/** A constant representing the main menu.
	 * @see #currentlyOpenPanel */
	public static final int MENU_MAIN = 4;

	/** The tutorial window. */
	private TutorialView tutorialView;
	/** Whether the tutorial is currently open. */
	private boolean isTutorialOpen = false;

	/** An array containing the actual stroke thicknesses for each thickness byte. */
	private static final int[] thicknessDps = new int[]{6, 15, 24, 33, 42};

	/** The IDs for each of the stroke thickness buttons. */
	public static final int[] thicknessButtonIds = new int[]{
			R.id.imageview_thickness_1,
			R.id.imageview_thickness_2,
			R.id.imageview_thickness_3,
			R.id.imageview_thickness_4,
			R.id.imageview_thickness_5
	};

	/** The IDs for the drawables used for each of the stroke thickness buttons. */
	private static final int[] thicknessButtonShapeIds = new int[]{
			R.drawable.thickness_button_shape_1,
			R.drawable.thickness_button_shape_2,
			R.drawable.thickness_button_shape_3,
			R.drawable.thickness_button_shape_4,
			R.drawable.thickness_button_shape_5
	};

	/** The IDs of each component of the main menu. */
	private static final int[] menuLinearLayoutIds = new int[]{
			R.id.linearlayout_menu_about,
			R.id.linearlayout_menu_help,
			R.id.linearlayout_menu_storage,
			R.id.linearlayout_menu_handedness,
			R.id.linearlayout_menu_brightness,
			R.id.linearlayout_menu_new_project,
			R.id.linearlayout_menu_open,
			R.id.linearlayout_menu_save
	};

	/** The IDs of the text labels for each of the items in the main menu. */
	private static final int[] menuTextViewIds = new int[]{
			R.id.textview_menu_about,
			R.id.textview_menu_help,
			R.id.textview_menu_storage,
			R.id.textview_menu_handedness,
			R.id.textview_menu_brightness,
			R.id.textview_menu_new_project,
			R.id.textview_menu_open,
			R.id.textview_menu_save
	};

	/** The name of the extra containing the request type when opening the project selection screen.
	 * @see #REQUEST_OPEN_PROJECT */
	private static final String INTENT_MODE = "mode";
	/** The name of the extra containing the current project name when opening the project selection screen.
	 * @see Project#getName() */
	public static final String INTENT_CURRENT_PROJECT_NAME = "currentProjectName";
	/** The extra constant for the project selection screen, for use with {@link #INTENT_MODE}. */
	private static final int REQUEST_OPEN_PROJECT = 1;

	/** A value indicating the scheduled task. Either {@link #TASK_NEW}, {@link #TASK_OPEN} or {@link #TASK_EXIT}. */
	public int taskRequested = 0;
	/** The constant for requesting that the app should exit.
	 * @see #taskRequested
	 * @see #performTask() */
	public static final int TASK_EXIT = 1;
	/** The constant for requesting that the project selection menu should be opened.
	 * @see #taskRequested
	 * @see #performTask() */
	private static final int TASK_OPEN = 2;
	/** The constant for requesting that a new project should be created.
	 * @see #taskRequested
	 * @see #performTask() */
	private static final int TASK_NEW = 3;

	/** The tag key for the stroke color. */
	public static final int TAG_COLOR = 1;
	/** The tag key for the stroke thickness. */
	public static final int TAG_THICKNESS = 2;

	/** Conveniently displays croutons. */
	public CroutonHelper croutonHelper = new CroutonHelper();

	/** A receiver for notification of any change in the state of the external storage. */
	private SdCardRemovedReceiver sdCardRemovedReceiver = new SdCardRemovedReceiver();

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		initPreferences();
		initIO();

		setContentView(AppearanceUtils.isLeftHanded() ? R.layout.activity_main_l : R.layout.activity_main);

		notepadView = (NotepadView) findViewById(R.id.view);
		notepadView.setActivityReference(this);

		rootView = (ViewGroup) findViewById(R.id.root);
		drawingToolbar = (LinearLayout) findViewById(R.id.include_toolbar_draw);
		thicknessSelector = (LinearLayout) findViewById(R.id.include_thickness);
		colorPalette = (TableLayout) findViewById(R.id.include_color);
		buttonDrawTool = (ImageView) findViewById(R.id.imageview_toolbar_pencil_eraser);
		mainToolbar = (LinearLayout) findViewById(R.id.include_toolbar_main);
		cameraToolbar = (LinearLayout) findViewById(R.id.include_camera);
		tutorialView = (TutorialView) findViewById(R.id.include_tutorial);

		initToolbars();
		initMainMenu(getResources());
		initSheetMenu();

		FrameLayout touchCatcher = (FrameLayout) findViewById(R.id.linearlayout_catch_touch_outside_toolbar);
		touchCatcher.setOnTouchListener(new View.OnTouchListener()
		{
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				if (isTutorialOpen)
				{
					return true;
				}
				else if (event.getAction() == MotionEvent.ACTION_DOWN)
				{
					closeAllMenus();
					return true;
				}
				return false;
			}
		});

		applyTheme();

		LicenseUtils.registerExtraLicenses(this);

		// Has this app been launched for the first time? Open the tutorial.
		if (!Prefs.getBoolean(R.string.pref_tutorial_done))
		{
			notepadView.postDelayed(new Runnable()
			{
				@Override
				public void run()
				{
					openTutorial(null);
				}
			}, 200);
		}
	}

	/**
	 * Initializes the IO utility class.
	 * This should be called before any IO or sketchbook-related operations are performed.
	 */
	private void initIO()
	{
		IOUtils.externalStorageHelper = new ExternalStorageHelper();
		IOUtils.directoryHelper = new DirectoryHelper(getIntent());
		IOUtils.initialize();
	}

	/**
	 * Gets the current sketchbook.
	 * @return The current sketchbook.
	 */
	private Project project()
	{
		return notepadView.project;
	}

	/**
	 * Initializes the preferences utility class.
	 */
	private void initPreferences()
	{
		Prefs.initialize(this);
	}

	/**
	 * Initializes the toolbars.
	 */
	private void initToolbars()
	{
		Resources res = getResources();
		initDrawingToolbar(res);
		initBottomToolbar(res);
		initCameraToolbar(res);
	}

	/**
	 * Initializes the drawing toolbar containing the tool button and the stroke button.
	 * @param res A <code>Resources</code> instance.
	 */
	private void initDrawingToolbar(final Resources res)
	{
		final ImageView buttonPencilEraser = (ImageView) findViewById(R.id.imageview_toolbar_pencil_eraser);
		updateDrawingToolButton(buttonPencilEraser, res);
		updateStrokeSettingsButton(res);
	}

	/**
	 * Initializes the main toolbar containing the main menu button and the sheet menu button.
	 * @param res A <code>Resources</code> instance.
	 */
	private void initBottomToolbar(Resources res)
	{
		ImageView buttonSheet = (ImageView) findViewById(R.id.imageview_sheet_button);
		buttonSheet.setImageDrawable(ResourcesCompat.getDrawable(this, R.drawable.book_open));

		ImageView buttonMain = (ImageView) findViewById(R.id.imageview_main_button);
		buttonMain.setImageDrawable(ResourcesCompat.getDrawable(this, R.drawable.settings));
	}

	/**
	 * Initializes the camera toolbar containing the camera information and reset buttons.
	 * @param res A <code>Resources</code> instance.
	 */
	private void initCameraToolbar(Resources res)
	{
		ImageView buttonZoom = (ImageView) findViewById(R.id.imageview_toolbar_button_zoom);
		buttonZoom.setImageDrawable(ResourcesCompat.getDrawable(this, R.drawable.magnify));

		ImageView buttonPosition = (ImageView) findViewById(R.id.imageview_toolbar_button_position);
		buttonPosition.setImageDrawable(ResourcesCompat.getDrawable(this, R.drawable.position));

		updateCameraToolbar();
	}

	/**
	 * Updates the camera information in the camera toolbar.
	 */
	public void updateCameraToolbar()
	{
		TextView displayZoom = (TextView) findViewById(R.id.textview_display_zoom);
		displayZoom.setText(Integer.toString((int) (Math.ceil(notepadView.zoom * 100))) + "%");

		TextView displayPosition = (TextView) findViewById(R.id.textview_display_position);
		displayPosition.setText((int) (notepadView.cameraX * 0.1f) + "  " + (int) (notepadView.cameraY * 0.1f));
	}

	/**
	 * Initializes the main menu.
	 * @param res A <code>Resources</code> instance.
	 */
	private void initMainMenu(Resources res)
	{
		menu = (RelativeLayout) findViewById(R.id.include_menu);
		initialMenuTranslationY = menu.getTranslationY();
		updateMainMenu(res);
	}

	/**
	 * Refreshes the state of the main menu.
	 * The drawables in the theme, handedness and storage items change to reflect the current settings.
	 * Note that Mr. Vector drawables cannot be specified statically and so must be set programmatically.
	 * @param res A <code>Resources</code> instance.
	 */
	private void updateMainMenu(Resources res)
	{
		ImageView iconAbout = (ImageView) findViewById(R.id.imageview_menu_about);
		iconAbout.setImageDrawable(ResourcesCompat.getDrawable(this, R.drawable.info));

		ImageView iconHelp = (ImageView) findViewById(R.id.imageview_menu_help);
		iconHelp.setImageDrawable(ResourcesCompat.getDrawable(this, R.drawable.help_circle));

		ImageView iconStorage = (ImageView) findViewById(R.id.imageview_menu_storage);
		iconStorage.setImageDrawable(ResourcesCompat.getDrawable(this, IOUtils.isUsingExternalStorage() ? R.drawable.sd : R.drawable.internal_icon));
		TextView labelStorage = (TextView) findViewById(R.id.textview_menu_storage);
		labelStorage.setText(IOUtils.isUsingExternalStorage() ? R.string.menu_using_external_storage : R.string.menu_using_internal_storage);

		ImageView iconHandedness = (ImageView) findViewById(R.id.imageview_menu_handedness);
		iconHandedness.setImageDrawable(ResourcesCompat.getDrawable(this, AppearanceUtils.isLeftHanded() ? R.drawable.hand_pointing_left : R.drawable.hand_pointing_right));

		ImageView iconBrightness = (ImageView) findViewById(R.id.imageview_menu_brightness);
		iconBrightness.setImageDrawable(ResourcesCompat.getDrawable(this, R.drawable.weather_sunny));

		ImageView iconNew = (ImageView) findViewById(R.id.imageview_menu_new_project);
		iconNew.setImageDrawable(ResourcesCompat.getDrawable(this, R.drawable.file));

		ImageView iconOpen = (ImageView) findViewById(R.id.imageview_menu_open);
		iconOpen.setImageDrawable(ResourcesCompat.getDrawable(this, R.drawable.open_in_app));

		ImageView iconSave = (ImageView) findViewById(R.id.imageview_menu_save);
		iconSave.setImageDrawable(ResourcesCompat.getDrawable(this, R.drawable.content_save));
	}

	/**
	 * Initializes the sheet menu.
	 */
	private void initSheetMenu()
	{
		sheetMenu = (LinearLayout) findViewById(R.id.include_sheet);
		updateSheetMenu();
	}

	/**
	 * Refreshes the sheet menu to reflect the current theme and handedness settings, as well as updating the sheet list.
	 */
	@SuppressWarnings("deprecated")
	private void updateSheetMenu()
	{
		DynamicListView sheetListView = (DynamicListView) sheetMenu.findViewById(R.id.dynamiclistview_sheet_list);
		sheetListView.enableDragAndDrop();
		sheetListView.setDraggableManager(new TouchViewDraggableManager(R.id.imageview_sheet_drag_handle));

		LinearLayout sheetFooter = (LinearLayout) sheetMenu.findViewById(R.id.sheet_drawer_footer);
		if (AppearanceUtils.isThemeDark())
		{
			if (AppearanceUtils.isLeftHanded())
			{
				sheetFooter.setBackgroundDrawable(getResources().getDrawable(R.drawable.statelist_menu_item_dark_l));
			}
			else
			{
				sheetFooter.setBackgroundDrawable(getResources().getDrawable(R.drawable.statelist_menu_item_dark));
			}
		}
		else
		{
			if (AppearanceUtils.isLeftHanded())
			{
				sheetFooter.setBackgroundDrawable(getResources().getDrawable(R.drawable.statelist_menu_item_l));
			}
			else
			{
				sheetFooter.setBackgroundDrawable(getResources().getDrawable(R.drawable.statelist_menu_item));
			}
		}

		if (sheetArrayAdapter != null) sheetArrayAdapter.notifyDataSetChanged();
		initialSheetDrawerTranslationX = Math.abs(sheetMenu.getTranslationX());
	}

	/**
	 * Updates the tool button to reflect the current tool.
	 * @param button The button <code>ImageView</code>.
	 * @param res A <code>Resources</code> instance.
	 */
	private void updateDrawingToolButton(ImageView button, Resources res)
	{
		if (notepadView.tool == NotepadView.TOOL_PENCIL)
		{
			button.setImageDrawable(ResourcesCompat.getDrawable(this, R.drawable.pencil));
		}
		else if (notepadView.tool == NotepadView.TOOL_ERASER)
		{
			button.setImageDrawable(ResourcesCompat.getDrawable(this, R.drawable.eraser));
		}
	}

	/**
	 * Updates the stroke button to reflect the current stroke settings.
	 * @param res A <code>Resources</code> instance.
	 */
	@SuppressWarnings("deprecated") // Uses deprecated methods to support devices below API 22/23.
	private void updateStrokeSettingsButton(Resources res)
	{
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		float density = metrics.density;

		ImageView buttonStrokeSettings = (ImageView) findViewById(R.id.view_toolbar_stroke_settings);
		GradientDrawable indicator = (GradientDrawable) res.getDrawable(R.drawable.color_button_shape);
		int thicknessPx = (int) Math.ceil(thicknessDps[notepadView.strokeThickness] * density);
		indicator.setSize(thicknessPx, thicknessPx);
		indicator.setColor(res.getColor(ColorUtils.getColorResId(notepadView.strokeColor)));
		buttonStrokeSettings.setImageDrawable(indicator);

		for (int t = 0; t < 5; t++)
		{
			GradientDrawable thicknessButtonShape = (GradientDrawable) res.getDrawable(thicknessButtonShapeIds[t]);
			thicknessButtonShape.setColor(res.getColor(ColorUtils.getColorResId(notepadView.strokeColor)));
			((ImageView) findViewById(thicknessButtonIds[t])).setImageDrawable(thicknessButtonShape);
		}
	}

	/**
	 * Toggles the stroke menu.
	 * @param v Not used.
	 */
	public void toggleStrokeSettings(View v)
	{
		if (isMenuOpen(MENU_STROKE))
		{
			closeStrokeSettings();
		}
		else
		{
			openStrokeSettings();
		}
	}

	/**
	 * Enables the view that covers the canvas which, when touched, closes any open toolbars or menus.
	 * @see #deactivateTouchCatcher()
	 */
	private void activateTouchCatcher()
	{
		FrameLayout touchCatcher = (FrameLayout) findViewById(R.id.linearlayout_catch_touch_outside_toolbar);
		touchCatcher.setVisibility(View.VISIBLE);

		drawingToolbar.bringToFront();
		mainToolbar.bringToFront();
	}

	/**
	 * Disables the touch catcher view.
	 * @see #activateTouchCatcher()
	 */
	private void deactivateTouchCatcher()
	{
		FrameLayout touchCatcher = (FrameLayout) findViewById(R.id.linearlayout_catch_touch_outside_toolbar);
		touchCatcher.setVisibility(View.GONE);
	}

	/**
	 * Initialises the current sketchbook.
	 */
	public void initProject()
	{
		String lastProjectName = Prefs.getString(R.string.pref_last_project_name);
		boolean dirtyExit = Prefs.getBoolean(R.string.pref_dirty_exit);

		// Did the app not close properly last time? Start data recovery.
		if (IOUtils.getWorkingDirectory().exists() && dirtyExit)
		{
			startCrashRecovery();
		}
		// Is there no previous sketchbook? Make a new one.
		else if (Project.DEFAULT_NAME.equals(lastProjectName))
		{
			project().initializeNew(getResources());
			notepadView.setCameraPositionAndZoom(0f, 0f, 1f);
		}
		// Was the user in the middle of working on a project? Load it.
		else
		{
			project().initializeLoaded(lastProjectName);
			if (project().getArchiveFile().exists())
			{
				attemptProjectLoad(false);
				notepadView.setCameraFromCurrentSheet();
			}
			else
			{
				project().initializeNew(getResources());
				notepadView.setCameraPositionAndZoom(0f, 0f, 1f);
			}
		}

		Prefs.edit().putBoolean(R.string.pref_dirty_exit, true).apply();
	}

	/**
	 * Toggles between the pencil and the eraser.
	 * @param v Not used.
	 */
	public void toggleTool(View v)
	{
		closeAllMenus();

		notepadView.tool = ~notepadView.tool;
		updateDrawingToolButton((ImageView) findViewById(R.id.imageview_toolbar_pencil_eraser), getResources());
	}

	/**
	 * Checks if the specified menu is currently open.
	 * @param menu The corresponding value of the menu.
	 * @return True if the specified menu is open; false otherwise.
	 * @throws IllegalArgumentException if <code>menu</code> is not a valid <code>MENU</code> value.
	 * @see #MENU_STROKE
	 * @see #MENU_SHEET
	 * @see #MENU_MAIN
	 */
	public boolean isMenuOpen(int menu)
	{
		if (menu != MENU_STROKE && menu != MENU_SHEET && menu != MENU_MAIN)
		{
			throw new IllegalArgumentException("Argument should be a MENU value; received " + menu + ".");
		}

		return currentlyOpenPanel == menu;
	}

	/**
	 * Opens the stroke menu.
	 */
	public void openStrokeSettings()
	{
		if (!isMenuOpen(MENU_STROKE))
		{
			closeMenus(MENU_SHEET | MENU_MAIN);

			currentlyOpenPanel = MENU_STROKE;

			activateTouchCatcher();

			thicknessSelector.setVisibility(View.VISIBLE);
			thicknessSelector.bringToFront();
			colorPalette.setVisibility(View.VISIBLE);
			colorPalette.bringToFront();
		}
	}

	/**
	 * Closes the stroke menu.
	 * @return True if the stroke menu was previously open; false otherwise.
	 */
	public boolean closeStrokeSettings()
	{
		if (isMenuOpen(MENU_STROKE))
		{
			currentlyOpenPanel = 0;

			deactivateTouchCatcher();

			thicknessSelector.setVisibility(View.GONE);
			colorPalette.setVisibility(View.GONE);

			return true;
		}
		return false;
	}

	/**
	 * Toggles the sheet menu.
	 * @param v Not used.
	 */
	public void toggleSheetDrawer(View v)
	{
		if (!isMenuOpen(MENU_SHEET))
		{
			openSheetMenu();
		}
		else
		{
			closeSheetMenu();
		}
	}

	/**
	 * Closes the menus specified in the bitmask.
	 * @param mask A bitmask containing the sum of any of the <code>MENU</code> values.
	 * @return True if at least one menu was closed.
	 * @throws IllegalArgumentException If the mask is malformed.
	 * @see #MENU_STROKE
	 * @see #MENU_SHEET
	 * @see #MENU_MAIN
	 */
	private boolean closeMenus(int mask)
	{
		if (mask < 0 || mask > MENU_STROKE + MENU_SHEET + MENU_MAIN)
		{
			throw new IllegalArgumentException("Argument must be bitmask of MENU values; received " + mask + ".");
		}

		boolean panelWasClosed = false;
		if ((mask & MENU_STROKE) > 0)
		{
			panelWasClosed = closeStrokeSettings();
		}
		if ((mask & MENU_SHEET) > 0)
		{
			panelWasClosed = closeSheetMenu() || panelWasClosed;
		}
		if ((mask & MENU_MAIN) > 0)
		{
			panelWasClosed = closeMainMenu() || panelWasClosed;
		}
		return panelWasClosed;
	}

	/**
	 * Opens the sheet menu.
	 */
	private void openSheetMenu()
	{
		if (isTutorialOpen && tutorialView.getStep() != 6) return;

		generateThumbnail(project().getCurrentSheetIndex());

		if (!isMenuOpen(MENU_SHEET))
		{
			closeMenus(MENU_STROKE | MENU_MAIN);

			if (sheetArrayAdapter == null)
			{
				sheetArrayAdapter = new SheetAdapter(this, project());
				DynamicListView sheetListView = (DynamicListView) sheetMenu.findViewById(R.id.dynamiclistview_sheet_list);
				sheetListView.setAdapter(sheetArrayAdapter);
			}

			sheetArrayAdapter.updateTheme(sheetMenu);

			sheetMenu.clearAnimation();
			currentlyOpenPanel = MENU_SHEET;
			activateTouchCatcher();

			float startingTranslationX = initialSheetDrawerTranslationX * (AppearanceUtils.isLeftHanded() ? -1 : 1);
			ObjectAnimator animSlideIn = ObjectAnimator.ofFloat(sheetMenu, "translationX", startingTranslationX, 0f);
			animSlideIn.setInterpolator(new DecelerateInterpolator());
			sheetMenu.setVisibility(View.VISIBLE);
			sheetMenu.bringToFront();
			animSlideIn.start();
		}

		if (isTutorialOpen)
		{
			tutorialView.nextStep();
		}
	}

	/**
	 * Refreshes the sheet menu after the current sheet's thumbnail has been generated.
	 */
	public void refreshSheetDrawerAfterGeneratingThumbnail()
	{
		notepadView.post(new Runnable()
		{
			@Override
			public void run()
			{
				if (sheetArrayAdapter != null) sheetArrayAdapter.notifyDataSetChanged();
			}
		});
	}

	/**
	 * Changes the currently selected sheet.
	 * @param index The index of the sheet to open (NOT the sheet ID).
	 */
	public void changeSheet(final int index)
	{
		notepadView.updateCurrentSheetCameraValues();
		project().setCurrentSheet(notepadView, index);
		notepadView.setCameraFromCurrentSheet();
		updateCameraToolbar();
		sheetArrayAdapter.notifyDataSetChanged();
		notepadView.requestRender();
	}

	/**
	 * Prompts the user to enter a new name for the specified sheet, which is then renamed upon confirmation of the dialog.
	 * @param index The index of the sheet (NOT the sheet ID).
	 * @param view The TextView belonging to the sheet's list item.
	 */
	public void renameSheet(final int index, final TextView view)
	{
		final EditText input = new EditText(this);
		input.setInputType(InputType.TYPE_CLASS_TEXT);
		input.setText("");
		input.setHint(project().getSheetAt(index).getName());

		int dialogTheme = AppearanceUtils.isThemeDark() ? android.R.style.Theme_Holo_Dialog : android.R.style.Theme_Holo_Light_Dialog;
		AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, dialogTheme));
		builder.setTitle(getString(R.string.dialog_rename_sheet))
				.setMessage(getString(R.string.dialog_rename_sheet_msg))
				.setCancelable(true)
				.setPositiveButton(R.string.button_ok, null)
				.setNegativeButton(R.string.button_cancel, null)
				.setView(input);
		input.setTextColor(getResources().getColor(AppearanceUtils.isThemeDark() ? R.color.text_white : R.color.text_black));

		final AlertDialog prompt = builder.create();
		prompt.setOnShowListener(new DialogInterface.OnShowListener()
		{
			@Override
			public void onShow(final DialogInterface dialog)
			{
				Button okButton = prompt.getButton(AlertDialog.BUTTON_POSITIVE);
				okButton.setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						String sheetName = input.getText().toString();
						Project.Sheet sheet = project().getSheetAt(index);
						if (sheetName.equals(sheet.getName()) || sheetName.isEmpty())
						{
							dialog.dismiss();
						}
						else if (!IOUtils.isFilenameValid(sheetName) || Project.RECOVERY_NAME.equals(sheetName))
						{
							croutonHelper.show(NotepadActivity.this, R.string.toast_invalid_name, Style.ALERT);
						}
						else if (!project().doesSheetExistWithName(sheetName))
						{
							project().renameSheet(index, sheetName);
							view.setText(sheetName);
							dialog.dismiss();
						}
						else
						{
							croutonHelper.show(NotepadActivity.this, R.string.toast_sheet_already_exists_with_name, Style.ALERT);
						}
					}
				});
			}
		});

		prompt.show();
	}

	/**
	 * Prompts the user to confirm the deletion of the specified sheet.
	 * @param index The index of the sheet (NOT the sheet ID).
	 */
	public void deleteSheet(final int index)
	{
		// Is there only one sheet?
		if (project().getTotalSheets() == 1)
		{
			// Can't delete it.
			croutonHelper.show(this, R.string.toast_sheet_cant_delete_only_one, Style.ALERT);
			return;
		}

		DialogInterface.OnClickListener yesListener = new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				dialog.dismiss();
				project().deleteSheet(index);
				sheetArrayAdapter.notifyDataSetChanged();
				changeSheet(project().getCurrentSheetIndex());
			}
		};

		int dialogTheme = AppearanceUtils.isThemeDark() ? android.R.style.Theme_Holo_Dialog : android.R.style.Theme_Holo_Light_Dialog;
		AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, dialogTheme));
		builder.setTitle(getString(R.string.dialog_delete_sheet))
				.setMessage(getString(R.string.dialog_delete_sheet_msg, project().getSheetAt(index).getName()))
				.setCancelable(true)
				.setPositiveButton(R.string.button_yes, yesListener)
				.setNegativeButton(R.string.button_no, null);

		builder.show();
	}

	/**
	 * Generates a thumbnail for the specified sheet.
	 * @param sheetIndex The index of the sheet (NOT the sheet ID).
	 */
	private void generateThumbnail(int sheetIndex)
	{
		notepadView.requestScreenshot(sheetIndex);
	}

	/**
	 * Closes the sheet menu.
	 */
	public boolean closeSheetMenu()
	{
		if (isMenuOpen(MENU_SHEET))
		{
			sheetMenu.clearAnimation();
			currentlyOpenPanel = 0;
			deactivateTouchCatcher();

			float endingTranslationX = initialSheetDrawerTranslationX * (AppearanceUtils.isLeftHanded() ? -1 : 1);
			ObjectAnimator animSlideOut = ObjectAnimator.ofFloat(sheetMenu, "translationX", endingTranslationX);
			animSlideOut.addListener(new AnimatorListenerAdapter()
			{
				@Override
				public void onAnimationEnd(Animator animation)
				{
					super.onAnimationEnd(animation);
					sheetMenu.setVisibility(View.GONE);
				}
			});
			animSlideOut.setInterpolator(new DecelerateInterpolator());
			animSlideOut.start();

			return true;
		}
		return false;
	}

	/**
	 * Closes all menus.
	 * @return True if at least one menu was previously open; false otherwise.
	 */
	private boolean closeAllMenus()
	{
		return closeMenus(MENU_STROKE | MENU_SHEET | MENU_MAIN);
	}

	/**
	 * Toggles the main menu.
	 * @param v Not used.
	 */
	public void toggleMainMenu(View v)
	{
		if (!isMenuOpen(MENU_MAIN))
		{
			openMainMenu();
		}
		else
		{
			closeMainMenu();
		}
	}

	/**
	 * Opens the main menu.
	 */
	private void openMainMenu()
	{
		if (isTutorialOpen && tutorialView.getStep() != 8) return;

		if (!isMenuOpen(MENU_MAIN))
		{
			closeMenus(MENU_STROKE | MENU_SHEET);

			activateTouchCatcher();
			menu.bringToFront();
			mainToolbar.bringToFront();
			menu.clearAnimation();
			menu.setVisibility(View.VISIBLE);
			currentlyOpenPanel = MENU_MAIN;

			ObjectAnimator animSlideIn = ObjectAnimator.ofFloat(menu, "translationY", 0f);
			ObjectAnimator animFadeIn = ObjectAnimator.ofFloat(menu, "alpha", 1f);

			AnimatorSet animSet = new AnimatorSet();
			animSet.play(animSlideIn).with(animFadeIn);
			animSet.setInterpolator(new DecelerateInterpolator());
			animSet.start();
		}

		if (isTutorialOpen)
		{
			tutorialView.nextStep();
		}
	}

	/**
	 * Closes the main menu.
	 * @return True if the main menu was previously open; false otherwise.
	 */
	public boolean closeMainMenu()
	{
		if (isMenuOpen(MENU_MAIN))
		{
			mainToolbar.bringToFront();
			menu.clearAnimation();
			deactivateTouchCatcher();
			currentlyOpenPanel = 0;

			ObjectAnimator animSlideOut = ObjectAnimator.ofFloat(menu, "translationY", initialMenuTranslationY);
			ObjectAnimator animFadeOut = ObjectAnimator.ofFloat(menu, "alpha", 0f);

			AnimatorSet animSet = new AnimatorSet();
			animSet.play(animSlideOut).with(animFadeOut);
			animSet.setInterpolator(new AccelerateInterpolator());
			animSet.addListener(new AnimatorListenerAdapter()
			{
				@Override
				public void onAnimationEnd(Animator animation)
				{
					super.onAnimationEnd(animation);
					menu.setVisibility(View.GONE);
				}
			});
			animSet.start();

			return true;
		}
		return false;
	}

	/**
	 * Sets the current stroke color.
	 * @param v Not used.
	 */
	public void setStrokeColor(View v)
	{
		byte colorByte = Byte.valueOf((String) v.getTag());
		notepadView.strokeColor = colorByte;
		notepadView.latestStroke.setColor(colorByte);
		closeStrokeSettings();
		updateStrokeSettingsButton(getResources());
	}

	/**
	 * Sets the current stroke thickness.
	 * @param v Not used.
	 */
	public void setStrokeThickness(View v)
	{
		byte thicknessByte = Byte.valueOf((String) v.getTag());
		notepadView.strokeThickness = thicknessByte;
		notepadView.latestStroke.setThickness(thicknessByte);
		closeStrokeSettings();
		updateStrokeSettingsButton(getResources());
	}

	/**
	 * Prompts the user to enter a name for a new sheet, which is then added upon confirmation of the dialog.
	 * @param v Not used.
	 */
	@SuppressWarnings("deprecated")
	public void addNewSheet(View v)
	{
		final LinearLayout dialogView = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_new_sheet, null);
		final EditText input = (EditText) dialogView.findViewById(R.id.edittext_new_sheet_name);
		input.setText(Project.getSuggestedNewSheetName(project(), this));
		input.selectAll();

		int dialogTheme = AppearanceUtils.isThemeDark() ? android.R.style.Theme_Holo_Dialog : android.R.style.Theme_Holo_Light_Dialog;
		AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, dialogTheme));
		builder.setTitle(getString(R.string.dialog_new_sheet))
				.setCancelable(false)
				.setPositiveButton(R.string.button_ok, null)
				.setNegativeButton(R.string.button_cancel, null)
				.setView(dialogView);
		input.setTextColor(getResources().getColor(AppearanceUtils.isThemeDark() ? R.color.text_white : R.color.text_black));

		final AlertDialog prompt = builder.create();
		prompt.setOnShowListener(new DialogInterface.OnShowListener()
		{
			@Override
			public void onShow(final DialogInterface dialog)
			{
				Button okButton = prompt.getButton(AlertDialog.BUTTON_POSITIVE);
				okButton.setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						String sheetName = input.getText().toString();

						if (sheetName.isEmpty())
						{
							croutonHelper.show(NotepadActivity.this, R.string.toast_sheet_must_have_a_name, Style.ALERT);
						}
						else if (!IOUtils.isFilenameValid(sheetName) || Project.RECOVERY_NAME.equals(sheetName))
						{
							croutonHelper.show(NotepadActivity.this, R.string.toast_invalid_name, Style.ALERT);
						}
						else if (!project().doesSheetExistWithName(sheetName))
						{
							project().addNewSheet(sheetName);
							changeSheet(project().getSheets().size() - 1);
							closeSheetMenu();
							dialog.dismiss();
						}
						else
						{
							croutonHelper.show(NotepadActivity.this, R.string.toast_sheet_already_exists_with_name, Style.ALERT);
						}
					}
				});
			}
		});

		prompt.show();
	}

	/**
	 * Shows the Notices dialog that lists the libraries and their licenses.
	 * @param v Not used.
	 */
	public void showNotices(View v)
	{
		if (isTutorialOpen) return;

		closeMainMenu();

		LicensesDialog.Builder builder = new LicensesDialog.Builder(this);
		builder.setNotices(R.raw.notices)
				.setIncludeOwnLicense(true)
				.setThemeResourceId(AppearanceUtils.isThemeDark() ? android.R.style.Theme_Holo_Dialog : android.R.style.Theme_Holo_Light_Dialog);
		if (AppearanceUtils.isThemeDark()) builder.setNoticesCssStyle(R.string.notices_dark_style);

		builder.build().show();
	}

	/**
	 * Opens the tutorial.
	 * @param v Not used.
	 */
	public void openTutorial(@Nullable View v)
	{
		if (isTutorialOpen) return;

		closeMainMenu();

		tutorialView.setActivityReference(this);
		tutorialView.updateAppearance(getResources());
		tutorialView.startTutorial();
		isTutorialOpen = true;
	}

	/**
	 * Closes the tutorial.
	 */
	public void closeTutorial()
	{
		deactivateTouchCatcher();
		closeAllMenus();
		isTutorialOpen = false;
	}

	/**
	 * Opens a dialog confirming the change of storage (internal/external).
	 * @param v Not used.
	 */
	public void toggleStorage(@Nullable View v)
	{
		if (isTutorialOpen) return;

		closeMainMenu();

		int dialogMessage;
		if (IOUtils.isUsingInternalStorage())
		{
			if (IOUtils.isExternalStorageReadable())
			{
				if (!IOUtils.isExternalStorageWritable())
				{
					dialogMessage = R.string.dialog_confirm_external_storage_read_only;
				}
				else
				{
					dialogMessage = R.string.dialog_confirm_external_storage;
				}
			}
			else
			{
				croutonHelper.show(this, R.string.toast_external_storage_failure, Style.ALERT);
				return;
			}
		}
		else
		{
			dialogMessage = R.string.dialog_confirm_internal_storage;
		}

		DialogInterface.OnClickListener yesListener = new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				IOUtils.toggleStorageSetting(project());
				dialog.dismiss();
				if (IOUtils.isUsingExternalStorage())
				{
					registerSdCardRemovedReceiver();
				}
				updateMainMenu(getResources());
			}
		};

		int dialogTheme = AppearanceUtils.isThemeDark() ? android.R.style.Theme_Holo_Dialog : android.R.style.Theme_Holo_Light_Dialog;
		AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, dialogTheme));
		builder.setMessage(getString(dialogMessage))
				.setCancelable(true)
				.setPositiveButton(R.string.button_yes, yesListener)
				.setNegativeButton(R.string.button_no, null);

		builder.show();
	}

	/**
	 * Toggles right-handed or left-handed mode.
	 * @param v Not used.
	 */
	public void toggleHandedness(View v)
	{
		if (isTutorialOpen) return;

		AppearanceUtils.toggleHandedness();
		updateHandedness();
	}

	/**
	 * Updates layouts to reflect the current handedness setting.
	 */
	private void updateHandedness()
	{
		boolean isLeft = AppearanceUtils.isLeftHanded();

		// Drawing toolbar
		int drawingToolbarIndex = rootView.indexOfChild(drawingToolbar);
		rootView.removeView(drawingToolbar);
		drawingToolbar = (LinearLayout) getLayoutInflater().inflate(isLeft ? R.layout.toolbar_pencil_l : R.layout.toolbar_pencil_right, rootView, false);
		drawingToolbar.setId(R.id.include_toolbar_draw);
		rootView.addView(drawingToolbar, drawingToolbarIndex);
		initDrawingToolbar(getResources());

		// Color palette
		int colorPaletteIndex = rootView.indexOfChild(colorPalette);
		rootView.removeView(colorPalette);
		colorPalette = (TableLayout) getLayoutInflater().inflate(isLeft ? R.layout.color_palette_l : R.layout.color_palette, rootView, false);
		colorPalette.setId(R.id.include_color);
		rootView.addView(colorPalette, colorPaletteIndex);

		// Line thickness selector
		int thicknessSelectorIndex = rootView.indexOfChild(thicknessSelector);
		rootView.removeView(thicknessSelector);
		thicknessSelector = (LinearLayout) getLayoutInflater().inflate(
				isLeft ? R.layout.line_thickness_selector_l : R.layout.line_thickness_selector, rootView, false);
		thicknessSelector.setId(R.id.include_thickness);
		rootView.addView(thicknessSelector, thicknessSelectorIndex);

		// Sheet menu
		int sheetPanelIndex = rootView.indexOfChild(sheetMenu);
		rootView.removeView(sheetMenu);
		sheetMenu = (LinearLayout) getLayoutInflater().inflate(isLeft ? R.layout.sheet_drawer_l : R.layout.sheet_drawer, rootView, false);
		sheetMenu.setId(R.id.include_sheet);
		rootView.addView(sheetMenu, sheetPanelIndex);
		updateSheetMenu();
		sheetArrayAdapter = null;

		// Camera toolbar
		int cameraToolbarIndex = rootView.indexOfChild(cameraToolbar);
		rootView.removeView(cameraToolbar);
		cameraToolbar = (LinearLayout) getLayoutInflater().inflate(isLeft ? R.layout.toolbar_camera_l : R.layout.toolbar_camera, rootView, false);
		cameraToolbar.setId(R.id.include_camera);
		rootView.addView(cameraToolbar, cameraToolbarIndex);
		initCameraToolbar(getResources());
		updateCameraToolbar();

		// Main toolbar
		int mainToolbarIndex = rootView.indexOfChild(mainToolbar);
		rootView.removeView(mainToolbar);
		mainToolbar = (LinearLayout) getLayoutInflater().inflate(isLeft ? R.layout.toolbar_lower_l : R.layout.toolbar_lower_right, rootView, false);
		mainToolbar.setId(R.id.include_toolbar_main);
		rootView.addView(mainToolbar, mainToolbarIndex);
		initBottomToolbar(getResources());

		// Main menu
		int menuIndex = rootView.indexOfChild(menu);
		rootView.removeView(menu);
		menu = (RelativeLayout) getLayoutInflater().inflate(isLeft ? R.layout.menu_l : R.layout.menu, rootView, false);
		menu.setId(R.id.include_menu);
		rootView.addView(menu, menuIndex);
		updateMainMenu(getResources());
		if (isMenuOpen(MENU_MAIN))
		{
			menu.setTranslationY(0f);
			menu.setVisibility(View.VISIBLE);
			menu.setAlpha(1f);
		}

		applyTheme();
	}

	/**
	 * Toggle between the light and dark themes.
	 * @param v Not used.
	 */
	public void toggleBrightness(View v)
	{
		AppearanceUtils.toggleTheme();
		applyTheme();
	}

	/**
	 * Updates the light/dark mode button in the main menu to reflect the current theme.
	 */
	private void updateBrightnessMenuItem()
	{
		TextView labelBrightness = (TextView) menu.findViewById(R.id.textview_menu_brightness);
		labelBrightness.setText(AppearanceUtils.isThemeDark() ? R.string.menu_mode_dark : R.string.menu_mode_light);

		ImageView iconBrightness = (ImageView) menu.findViewById(R.id.imageview_menu_brightness);
		iconBrightness.setImageDrawable(ResourcesCompat.getDrawable(this, AppearanceUtils.isThemeDark() ? R.drawable.weather_night : R.drawable.weather_sunny));
	}

	/**
	 * Updates the UI to reflect the current theme.
	 */
	@SuppressWarnings("deprecated")
	private void applyTheme()
	{
		notepadView.getRenderer().requestBackgroundChange();
		updateBrightnessMenuItem();

		Resources res = getResources();

		// Update the root view.
		rootView.setBackgroundColor(res.getColor(AppearanceUtils.isThemeDark() ? R.color.black : R.color.white));

		// Update the color palette.
		for (int color = 0; color < 14; color++)
		{
			ImageView colorButton = (ImageView) findViewById(ColorUtils.getColorPaletteButtonId(color));
			colorButton.setImageDrawable(res.getDrawable(AppearanceUtils.isThemeDark() ? R.drawable.color_edge_dark : R.drawable.color_edge));
			if (color == 0)
			{
				colorButton.setBackgroundColor(res.getColor(AppearanceUtils.isThemeDark() ? R.color.white : R.color.black));
			}
		}

		// Update the stroke thickness buttons.
		updateStrokeSettingsButton(res);

		// Update the sheet menu.
		updateSheetMenu();

		// Update the main menu.
		menu.setBackgroundColor(res.getColor(AppearanceUtils.isThemeDark() ? R.color.black : R.color.white));
		for (int menuLinearLayoutId : menuLinearLayoutIds)
		{
			if (AppearanceUtils.isThemeDark())
			{
				if (AppearanceUtils.isLeftHanded())
				{
					menu.findViewById(menuLinearLayoutId).setBackgroundDrawable(res.getDrawable(R.drawable.statelist_menu_item_dark_l));
				}
				else
				{
					menu.findViewById(menuLinearLayoutId).setBackgroundDrawable(res.getDrawable(R.drawable.statelist_menu_item_dark));
				}
			}
			else
			{
				if (AppearanceUtils.isLeftHanded())
				{
					menu.findViewById(menuLinearLayoutId).setBackgroundDrawable(res.getDrawable(R.drawable.statelist_menu_item_l));
				}
				else
				{
					menu.findViewById(menuLinearLayoutId).setBackgroundDrawable(res.getDrawable(R.drawable.statelist_menu_item));
				}
			}
		}
		for (int menuTextViewId : menuTextViewIds)
		{
			((TextView) menu.findViewById(menuTextViewId)).setTextColor(res.getColor(AppearanceUtils.isThemeDark() ? R.color.text_white : R.color.text_black));
		}

		// Update the tutorial view.
		if (isTutorialOpen) tutorialView.updateAppearance(getResources());
	}

	/**
	 * Calls the appropriate methods to exit, open a project or start a new project, depending on the value of {@link #taskRequested}.
	 */
	public void performTask()
	{
		switch (taskRequested)
		{
			case TASK_EXIT:
			{
				exit();
				return;
			}
			case TASK_OPEN:
			{
				startOpenActivity();
				break;
			}
			case TASK_NEW:
			{
				resetProject();
				break;
			}
			default:
			{
				throw new IllegalArgumentException("taskRequested should be a TASK value; received" + taskRequested + ".");
			}
		}
		taskRequested = 0;
	}

	/**
	 * Begins a new sketchbook.
	 * If the current sketchbook has unsaved changes, the user is prompted first.
	 * @param v Not used.
	 */
	public void startNewProject(@Nullable View v)
	{
		if (isTutorialOpen) return;

		closeMainMenu();

		if (project().hasUnsavedChanges())
		{
			showUnsavedChangesDialog(TASK_NEW);
		}
		else
		{
			taskRequested = TASK_NEW;
			notepadView.requestScreenshot(project());
		}
	}

	/**
	 * Resets the current sketchbook.
	 */
	private void resetProject()
	{
		project().initializeNew(getResources());
		notepadView.setCameraFromCurrentSheet();
		updateCameraToolbar();
	}

	/**
	 * Opens the sketchbook selector.
	 * If the current sketchbook has unsaved changes, the user is prompted first.
	 * @param v Not used.
	 */
	public void openProject(View v)
	{
		if (isTutorialOpen) return;

		closeMainMenu();

		if (project().hasUnsavedChanges())
		{
			showUnsavedChangesDialog(TASK_OPEN);
		}
		else
		{
			startOpenActivity();
		}
	}

	/**
	 * Launches a {@link BrowserActivity} where the user can choose a sketchbook to open.
	 * In the case that the external storage is selected and it cannot be accessed at the current
	 * moment, an error is displayed instead.
	 */
	private void startOpenActivity()
	{
		if (IOUtils.isUsingExternalStorage())
		{
			if (IOUtils.isExternalStorageReadable())
			{
				startOpenActivitySafe();
			}
			else
			{
				croutonHelper.show(this, R.string.toast_external_storage_failure, Style.ALERT);
			}
		}
		else
		{
			startOpenActivitySafe();
		}
	}

	/**
	 * Launches a {@link BrowserActivity} after checking that it is safe to do so in {@link #startOpenActivity()}.
	 */
	private void startOpenActivitySafe()
	{
		Intent intent = new Intent(this, BrowserActivity.class);
		intent.putExtra(INTENT_MODE, REQUEST_OPEN_PROJECT);
		intent.putExtra(INTENT_CURRENT_PROJECT_NAME, project().getName());
		startActivityForResult(intent, REQUEST_OPEN_PROJECT);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);

		if (data.hasExtra(BrowserActivity.CURRENT_PROJECT_NEW_NAME))
		{
			project().setName(data.getStringExtra(BrowserActivity.CURRENT_PROJECT_NEW_NAME));
			project().setFiles();
		}

		if (resultCode == RESULT_OK)
		{
			project().initializeLoaded(data.getStringExtra(BrowserActivity.EXTRA_PROJECT_NAME));
			if (requestCode == REQUEST_OPEN_PROJECT)
			{
				attemptProjectLoad(true);
			}
		}

		if (taskRequested == TASK_EXIT)
		{
			taskRequested = 0;
			if (resultCode == RESULT_OK)
			{
				exit();
			}
		}
	}

	/**
	 * Saves the current sketchbook.
	 * If the sketchbook has never been saved before, the user is prompted to provide a name.
	 * @param v Not used.
	 */
	public void saveProject(@Nullable View v)
	{
		// Disable save functionality during the tutorial.
		if (isTutorialOpen) return;

		// Disable save functionality when external storage is in use and is read-only.
		if (IOUtils.isUsingExternalStorage() && !IOUtils.isExternalStorageWritable())
		{
			croutonHelper.show(this, R.string.toast_cannot_save_read_only, Style.ALERT);
		}
		else
		{
			// Has this project never been saved before? Show a dialog to enter a name for the project.
			if (Project.DEFAULT_NAME.equals(project().getName()) || !project().getArchiveFile().exists())
			{
				saveProjectAs();
			}
			// Has this project been saved before? Save with the same name.
			else
			{
				attemptProjectSave();
			}
		}

		closeMainMenu();
	}

	/**
	 * Prompts the user to enter a name for the sketchbook to be saved.
	 */
	@SuppressWarnings("deprecated")
	private void saveProjectAs()
	{
		final EditText input = new EditText(this);
		input.setInputType(InputType.TYPE_CLASS_TEXT);
		input.setHint(R.string.hint_sketchbook_name);
		input.setText("");

		int dialogTheme = AppearanceUtils.isThemeDark() ? android.R.style.Theme_Holo_Dialog : android.R.style.Theme_Holo_Light_Dialog;
		AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, dialogTheme));
		builder.setMessage(getString(R.string.dialog_save_as_msg))
				.setCancelable(false)
				.setPositiveButton(R.string.button_ok, null)
				.setNegativeButton(R.string.button_cancel, null)
				.setView(input);
		input.setTextColor(getResources().getColor(AppearanceUtils.isThemeDark() ? R.color.text_white : R.color.text_black));

		final AlertDialog prompt = builder.create();
		prompt.setOnShowListener(new DialogInterface.OnShowListener()
		{
			@Override
			public void onShow(final DialogInterface dialog)
			{
				prompt.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						project().setName(input.getText().toString());
						attemptProjectSave();
						dialog.dismiss();
					}
				});
				prompt.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						dialog.dismiss();
					}
				});
			}
		});
		prompt.show();
	}

	/**
	 * Shows a dialog asking the user whether to save the current project.
	 * @param task Determines the button strings.
	 *             This should be either {@link #TASK_EXIT}, {@link #TASK_OPEN} or {@link #TASK_NEW}.
	 * @throws IllegalArgumentException if <code>task</code> doesn't match any of the above values.
	 */
	private void showUnsavedChangesDialog(final int task)
	{
		taskRequested = task;

		int buttonTextSave, buttonTextDiscard;
		switch (task)
		{
			case TASK_EXIT:
			{
				buttonTextSave = R.string.button_save_and_exit;
				buttonTextDiscard = R.string.button_exit_without_saving;
				break;
			}
			case TASK_OPEN:
			{
				buttonTextSave = R.string.button_save_and_open;
				buttonTextDiscard = R.string.button_open_without_saving;
				break;
			}
			case TASK_NEW:
			{
				buttonTextSave = R.string.button_save;
				buttonTextDiscard = R.string.button_dont_save;
				break;
			}
			default:
			{
				throw new IllegalArgumentException("Argument must be TASK_EXIT, TASK_OPEN or TASK_NEW; received " + task + ".");
			}
		}

		int dialogTheme = AppearanceUtils.isThemeDark() ? android.R.style.Theme_Holo_Dialog : android.R.style.Theme_Holo_Light_Dialog;

		AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, dialogTheme));
		builder.setMessage(getString(R.string.dialog_unsaved_work))
				.setPositiveButton(buttonTextSave, null)
				.setNeutralButton(buttonTextDiscard, null)
				.setNegativeButton(R.string.button_cancel, null)
				.setCancelable(true);

		final AlertDialog prompt = builder.create();
		prompt.setOnShowListener(new DialogInterface.OnShowListener()
		{
			@Override
			public void onShow(final DialogInterface dialog)
			{
				prompt.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v) // Save
					{
						saveProject(null);
						dialog.dismiss();
					}
				});
				prompt.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v) // Don't save
					{
						dialog.dismiss();
						performTask();
					}
				});
				prompt.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v) // Cancel
					{
						dialog.dismiss();
						taskRequested = 0;
					}
				});
			}
		});
		prompt.setOnCancelListener(new DialogInterface.OnCancelListener()
		{
			@Override
			public void onCancel(DialogInterface dialog)
			{
				taskRequested = 0;
			}
		});

		prompt.show();
	}

	/**
	 * Resets the camera zoom to the default.
	 * @param v Not used.
	 * @see NotepadView#ZOOM_DEFAULT
	 */
	public void resetZoom(View v)
	{
		notepadView.setZoom(NotepadView.ZOOM_DEFAULT);
		updateCameraToolbar();
	}

	/**
	 * Resets the camera position to the origin.
	 * @param v Not used.
	 */
	public void resetPosition(View v)
	{
		notepadView.centerCanvasAt(0, 0);
		updateCameraToolbar();
	}

	/**
	 * Attempts to save the current sketchbook.
	 */
	private void attemptProjectSave()
	{
		if (project().save(false))
		{
			Prefs.edit().putString(R.string.pref_last_project_name, project().getName()).commit();
			project().setFiles();
			notepadView.requestScreenshot(project());
			croutonHelper.show(this, R.string.toast_project_saved, Style.CONFIRM);
		}
		else
		{
			croutonHelper.show(this, R.string.toast_project_save_failed, Style.ALERT);
			taskRequested = 0;
		}
	}

	/**
	 * Attempts to load the current sketchbook.
	 * @param showCrouton True if the confirmation crouton should be shown, given that the
	 *                    sketchbook loads successfully.
	 */
	private void attemptProjectLoad(boolean showCrouton)
	{
		if (project().load(getResources()))
		{
			Prefs.edit().putString(R.string.pref_last_project_name, project().getName()).apply();
			loadProjectFromWorkingDirectory();
			if (showCrouton) croutonHelper.show(this, R.string.toast_project_opened, Style.CONFIRM);
		}
		else
		{
			croutonHelper.show(this, R.string.toast_project_open_failed, Style.ALERT);
		}
	}

	/**
	 * Loads the sketchbook data from the working directory.
	 */
	private void loadProjectFromWorkingDirectory()
	{
		notepadView.loadAllVisibleChunks(false, false);
		notepadView.setCameraFromCurrentSheet();
		updateCameraToolbar();
		notepadView.getRenderer().requestBackgroundChange();
	}

	/**
	 * Prompts the user if they want to attempt to recover any working data after a hard exit or a crash.
	 */
	private void startCrashRecovery()
	{
		DialogInterface.OnClickListener yesListener = new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				project().initializeLoaded(Project.RECOVERY_NAME);
				if (!project().loadMetadata()) project().determineMetadataWithoutFile();
				for (long sheetId : project().sheetOrder)
				{
					project().loadSheet(getResources(), sheetId);
				}
				project().setName(Project.RECOVERY_NAME);
				project().setFiles();
				project().save(false);
				project().load(getResources());
				notepadView.loadAllVisibleChunks(false, false);
				project().getArchiveFile().delete();
				project().setName(Project.DEFAULT_NAME);
				project().setFiles();
				dialog.dismiss();
			}
		};

		DialogInterface.OnClickListener noListener = new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				Prefs.edit().putString(R.string.pref_last_project_name, Project.DEFAULT_NAME)
						.putBoolean(R.string.pref_dirty_exit, false)
						.commit();
				initProject();
				dialog.dismiss();
			}
		};

		int dialogTheme = AppearanceUtils.isThemeDark() ? android.R.style.Theme_Holo_Dialog : android.R.style.Theme_Holo_Light_Dialog;
		AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, dialogTheme));
		builder.setTitle(getString(R.string.dialog_crash_recovery))
				.setMessage(getString(R.string.dialog_crash_recovery_msg, getString(R.string.app_name)))
				.setCancelable(false)
				.setPositiveButton(R.string.button_yes, yesListener)
				.setNegativeButton(R.string.button_no, noListener);

		builder.show();
	}

	/**
	 * Registers the {@link NotepadActivity.SdCardRemovedReceiver SdCardRemovedReceiver}.
	 * It listens for any change in the state of the external storage if it is in use, and warns the user
	 * to switch back to internal storage to prevent data loss.
	 * @see #showSdCardRemovalWarning()
	 */
	private void registerSdCardRemovedReceiver()
	{
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
		intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
		intentFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
		intentFilter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
		registerReceiver(sdCardRemovedReceiver, intentFilter);
	}

	/**
	 * Unregisters the {@link NotepadActivity.SdCardRemovedReceiver SdCardRemovedReceiver}.
	 * This must be called from {@link #onPause()} or an error will be thrown upon finishing the activity.
	 */
	private void unregisterSdCardRemovedReceiver()
	{
		try
		{
			unregisterReceiver(sdCardRemovedReceiver);
		}
		catch (IllegalArgumentException e)
		{
			// Disregard exception.
		}
	}

	/**
	 * Checks whether the external storage is accessible if it is in use.
	 * This is called from {@link #onResume()} in case the external storage has been removed while the app was in the background
	 * or the device was in sleep mode.
	 * If the external storage can't be found, the user is warned.
	 * @see #showSdCardRemovalWarning()
	 */
	private void checkSdCard()
	{
		if (IOUtils.isUsingExternalStorage())
		{
			if (!IOUtils.isExternalStorageReadable())
			{
				showSdCardRemovalWarning();
			}
		}
	}

	/**
	 * Warns the user that the external storage has become inaccessible while in use and that they should switch to
	 * internal storage to prevent data loss.
	 */
	private void showSdCardRemovalWarning()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(NotepadActivity.this);
		builder.setTitle(R.string.dialog_sdcard_removed_title)
				.setMessage(R.string.dialog_sdcard_removed_message)
				.setPositiveButton(R.string.button_ok, null);
		builder.create().show();
	}

	/**
	 * Cleans up the working directory and finishes the activity.
	 * This method should be called instead of {@link #finish()} or else the app will falsely detect a
	 * dirty exit upon the next launch.
	 */
	public void exit()
	{
		Prefs.edit().putBoolean(R.string.pref_dirty_exit, false).commit();
		IOUtils.deleteWorkingDirectories();
		finish();
	}

	@Override
	protected void onPause()
	{
		super.onPause();

		try
		{
			unregisterSdCardRemovedReceiver();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		notepadView.onPause();
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		initPreferences();
		initIO();

		if (IOUtils.isUsingExternalStorage())
		{
			registerSdCardRemovedReceiver();
			checkSdCard();
		}

		Resources res = getResources();
		updateDrawingToolButton((ImageView) findViewById(R.id.imageview_toolbar_pencil_eraser), res);
		updateStrokeSettingsButton(res);

		notepadView.onResume();
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		Prefs.edit().putBoolean(R.string.pref_dirty_exit, false).commit();
		Crouton.cancelAllCroutons();
		fixInputMethodManager();
	}

	/**
	 * A fix for a memory leak involving WindowInputEventReceiver.
	 * See http://code.google.com/p/android/issues/detail?id=34731
	 * Written by Denis Gladkiy.
	 * http://stackoverflow.com/a/23889598/5401587
	 */
	private void fixInputMethodManager()
	{
		final Object imm = getSystemService(Context.INPUT_METHOD_SERVICE);

		final Reflector.TypedObject windowToken
				= new Reflector.TypedObject(getWindow().getDecorView().getWindowToken(), IBinder.class);

		Reflector.invokeMethodExceptionSafe(imm, "windowDismissed", windowToken);

		final Reflector.TypedObject view
				= new Reflector.TypedObject(null, View.class);

		Reflector.invokeMethodExceptionSafe(imm, "startGettingWindowFocus", view);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);

		if (isTutorialOpen)
		{
			rootView.postDelayed(new Runnable()
			{
				@Override
				public void run()
				{
					tutorialView.update();
				}
			}, 500);
		}
	}

	@Override
	public void onBackPressed()
	{
		if (isTutorialOpen)
		{
			tutorialView.closeTutorial();
		}
		else if (!closeAllMenus() && notepadView.getTouchAction() == NotepadView.TOUCH_NONE)
		{
			if (project().hasUnsavedChanges())
			{
				showUnsavedChangesDialog(TASK_EXIT);
			}
			else
			{
				exit();
			}
		}
	}

	@Override
	protected void onStop()
	{
		super.onStop();
	}

	/**
	 * A subclass of <code>BroadcastReceiver</code> to detect a change in the state of the external storage.
	 * If the user has selected external storage and the SD card somehow becomes inaccessible, this class
	 * will detect the change and warn the user about potential data loss.
	 */
	class SdCardRemovedReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			showSdCardRemovalWarning();
		}
	}
}
