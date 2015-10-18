package ironartichoke.sketchpad;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.InputType;
import android.view.ContextThemeWrapper;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import java.io.File;

import ironartichoke.sketchpad.adapter.ProjectAdapter;
import ironartichoke.sketchpad.util.AppearanceUtils;
import ironartichoke.sketchpad.util.io.IOUtils;

/**
 * The activity that displays the list of sketchbooks that can be opened.
 */
public class BrowserActivity extends Activity implements View.OnLongClickListener, PopupMenu.OnMenuItemClickListener
{
	/** The adapter for the list of sketchbooks. */
	private ProjectAdapter adapter;
	/** The sketchbook file that has just been long-clicked. */
	private File focusedProjectFile;
	/** The name of the sketchbook that has just been long-clicked. */
	private String focusedProjectName;
	/** The <code>TextView</code> containing the name of the sketchbook that has just been long-clicked. */
	private TextView focusedProjectTextView;
	/** The name of the current sketchbook, passed in from {@link NotepadActivity}. */
	private String currentProjectName;

	/**
	 * A flag that is true if the currently open sketchbook has been renamed.
	 * The sketchbook and its file will be renamed once the user navigates away from this activity.
	 */
	private boolean hasCurrentProjectBeenRenamed;

	/** The name of the intent extra containing the name of the sketchbook that has just been opened. */
	public static final String EXTRA_PROJECT_NAME = "projectName";
	/** The name of the intent extra containing the new name for the current sketchbook if it has just been renamed. */
	public static final String CURRENT_PROJECT_NEW_NAME = "currentProjectNewName";

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(AppearanceUtils.isThemeDark() ? R.layout.activity_browser_dark : R.layout.activity_browser);

		currentProjectName = getIntent().getStringExtra(NotepadActivity.INTENT_CURRENT_PROJECT_NAME);
		updateHeaders();
		initProjectAdapter();
	}

	/**
	 * Initialize the sketchbook adapter.
	 */
	private void initProjectAdapter()
	{
		adapter = new ProjectAdapter(this, getIntent().getStringExtra(NotepadActivity.INTENT_CURRENT_PROJECT_NAME));
		ListView projectList = (ListView) findViewById(R.id.listview_project_list);
		projectList.setAdapter(adapter);
	}

	/**
	 * Toggle the sort order for sorting the sketchbooks by name.
	 * @param v Not used.
	 */
	public void toggleSortByName(@Nullable View v)
	{
		int sortBy = Prefs.getInt(R.string.pref_project_sort_by);
		int order = Prefs.getInt(R.string.pref_project_sort_order);
		if (sortBy == ProjectAdapter.ProjectFileComparator.SORT_BY_DATE) // Sort by date
		{
			setSort(ProjectAdapter.ProjectFileComparator.SORT_BY_NAME, ProjectAdapter.ProjectFileComparator.ORDER_ASCENDING);
		}
		else // Sort by name
		{
			setSort(ProjectAdapter.ProjectFileComparator.SORT_BY_NAME, ~order);
		}
	}

	/**
	 * Toggle the sort order for sorting the sketchbooks by date.
	 * @param v Not used.
	 */
	public void toggleSortByDate(@Nullable View v)
	{
		int sortBy = Prefs.getInt(R.string.pref_project_sort_by);
		int order = Prefs.getInt(R.string.pref_project_sort_order);
		if (sortBy == ProjectAdapter.ProjectFileComparator.SORT_BY_DATE) // Sort by date
		{
			setSort(ProjectAdapter.ProjectFileComparator.SORT_BY_DATE, ~order);
		}
		else // Sort by name
		{
			setSort(ProjectAdapter.ProjectFileComparator.SORT_BY_DATE, ProjectAdapter.ProjectFileComparator.ORDER_DESCENDING);
		}
	}

	/**
	 * Sets the sorting convention and the sorting order.
	 * @param sortBy The sorting convention.
	 *               Either {@link ProjectAdapter.ProjectFileComparator#SORT_BY_NAME} or {@link ProjectAdapter.ProjectFileComparator#SORT_BY_DATE}.
	 * @param order The sorting order.
	 *              Either {@link ProjectAdapter.ProjectFileComparator#ORDER_ASCENDING} or {@link ProjectAdapter.ProjectFileComparator#ORDER_DESCENDING}.
	 */
	private void setSort(int sortBy, int order)
	{
		Prefs.edit().putInt(R.string.pref_project_sort_by, sortBy)
				     .putInt(R.string.pref_project_sort_order, order)
					 .commit();

		updateHeaders(sortBy, order);
		adapter.populateFileArray();
	}

	/**
	 * Updates the list headers to reflect the current sorting settings.
	 */
	private void updateHeaders()
	{
		int sortBy = Prefs.getInt(R.string.pref_project_sort_by);
		int order = Prefs.getInt(R.string.pref_project_sort_order);

		updateHeaders(sortBy, order);
	}

	/**
	 * Updates the list headers to reflect the given sorting parameters.
	 * @param sortBy The sorting convention.
	 * @param order The sorting order.
	 */
	private void updateHeaders(int sortBy, int order)
	{
		TextView headerName = (TextView) findViewById(R.id.textview_project_header_name);
		if (sortBy == ProjectAdapter.ProjectFileComparator.SORT_BY_DATE) // Sort by date
		{
			headerName.setText(getString(R.string.category_name));
		}
		else if (order == ProjectAdapter.ProjectFileComparator.ORDER_ASCENDING) // Sort by name ascending
		{
			headerName.setText(getString(R.string.category_name_ascending));
		}
		else // Sort by name descending
		{
			headerName.setText(getString(R.string.category_name_descending));
		}

		TextView headerDate = (TextView) findViewById(R.id.textview_project_header_modified_date);
		if (sortBy == ProjectAdapter.ProjectFileComparator.SORT_BY_NAME) // Sort by name
		{
			headerDate.setText(getString(R.string.category_modified_date));
		}
		else if (order == ProjectAdapter.ProjectFileComparator.ORDER_ASCENDING) // Sort by date ascending
		{
			headerDate.setText(getString(R.string.category_modified_date_ascending));
		}
		else // Sort by date descending
		{
			headerDate.setText(getString(R.string.category_modified_date_descending));
		}
	}

	/**
	 * Opens the selected sketchbook and finishes the activity.
	 * @param v The list item view corresponding to the sketchbook.
	 */
	public void selectProject(View v)
	{
		Intent selectedProjectIntent = new Intent();
		selectedProjectIntent.putExtra(EXTRA_PROJECT_NAME, (String) v.getTag(R.id.tag_name));
		setResult(RESULT_OK, selectedProjectIntent);
		finish();
	}

	@Override
	public void onBackPressed()
	{
		Intent intent = new Intent();
		if (hasCurrentProjectBeenRenamed) intent.putExtra(CURRENT_PROJECT_NEW_NAME, currentProjectName);
		setResult(RESULT_CANCELED, intent);
		super.onBackPressed();
	}

	@Override
	public boolean onLongClick(View v)
	{
		focusedProjectFile = (File) v.getTag(R.id.tag_file);
		focusedProjectName = (String) v.getTag(R.id.tag_name);
		focusedProjectTextView = (TextView) v.findViewById(R.id.textview_project_name);

		PopupMenu popupMenu = new PopupMenu(this, v);
		popupMenu.inflate(R.menu.menu_browser);
		popupMenu.setOnMenuItemClickListener(this);
		popupMenu.show();

		return true;
	}

	@Override
	public boolean onMenuItemClick(MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.action_rename:
			{
				renameProject();
				return true;
			}
			case R.id.action_delete:
			{
				deleteProject();
				return true;
			}
		}
		return false;
	}

	/**
	 * Opens a dialog to rename the selected sketchbook.
	 */
	@SuppressWarnings("deprecated")
	private void renameProject()
	{
		final EditText input = new EditText(this);
		input.setInputType(InputType.TYPE_CLASS_TEXT);
		input.setText("");
		input.setHint(focusedProjectName);

		int dialogTheme = AppearanceUtils.isThemeDark() ? android.R.style.Theme_Holo_Dialog : android.R.style.Theme_Holo_Light_Dialog;
		AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, dialogTheme));
		builder.setMessage(getString(R.string.dialog_rename_project_msg))
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
						String projectName = input.getText().toString();
						if (!adapter.doesProjectWithNameExist(projectName))
						{
							focusedProjectFile.renameTo(new File(IOUtils.getDirectory(), projectName + IOUtils.TAR_GZ));
							File thumbnailFile = new File(IOUtils.getDirectory(), focusedProjectName + IOUtils.PNG);
							thumbnailFile.renameTo(new File(IOUtils.getDirectory(), projectName + IOUtils.PNG));
							focusedProjectTextView.setText(projectName);
							if (currentProjectName.equals(focusedProjectName)) // Renaming the current project?
							{
								Prefs.edit().putString(R.string.pref_last_project_name, projectName).apply();
								hasCurrentProjectBeenRenamed = true;
								currentProjectName = projectName;
							}
							adapter.populateFileArray();
							dialog.dismiss();
						}
					}
				});
			}
		});

		prompt.show();
	}

	/**
	 * Opens a dialog confirming that the user wants to delete the selected sketchbook.
	 */
	private void deleteProject()
	{
		DialogInterface.OnClickListener yesListener = new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				focusedProjectFile.delete();
				File thumbnailFile = new File(IOUtils.getDirectory(), focusedProjectName + IOUtils.PNG);
				thumbnailFile.delete();
				dialog.dismiss();
				adapter.populateFileArray();
			}
		};

		int dialogTheme = AppearanceUtils.isThemeDark() ? android.R.style.Theme_Holo_Dialog : android.R.style.Theme_Holo_Light_Dialog;
		AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, dialogTheme));
		builder.setMessage(getString(R.string.dialog_delete_project_msg, focusedProjectName))
				.setCancelable(true)
				.setPositiveButton(R.string.button_yes, yesListener)
				.setNegativeButton(R.string.button_no, null);

		builder.show();
	}
}
