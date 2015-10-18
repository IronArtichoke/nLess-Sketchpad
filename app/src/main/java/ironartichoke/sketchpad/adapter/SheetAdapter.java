package ironartichoke.sketchpad.adapter;

import android.content.res.Resources;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.nhaarman.listviewanimations.itemmanipulation.DynamicListView;
import com.nhaarman.listviewanimations.util.Swappable;
import com.wnafee.vector.compat.ResourcesCompat;

import ironartichoke.sketchpad.NotepadActivity;
import ironartichoke.sketchpad.Project;
import ironartichoke.sketchpad.R;
import ironartichoke.sketchpad.util.AppearanceUtils;

/**
 * The adapter class for the sheets in the currently open sketchbook.
 * Reordering the sheets is allowed through the {@link Swappable} interface.
 */
public class SheetAdapter extends BaseAdapter implements Swappable
{
	private final NotepadActivity activity;
	private Project project;

	public SheetAdapter(NotepadActivity activity, Project project)
	{
		this.activity = activity;
		this.project = project;
	}

	@Override
	public int getCount()
	{
		return project.getTotalSheets();
	}

	@Override
	public Object getItem(int position)
	{
		return project.getSheetAt(position);
	}

	@Override
	public long getItemId(int position)
	{
		return project.getSheetAt(position).getId();
	}

	@Override
	@SuppressWarnings("deprecated")
	public View getView(final int position, View convertView, ViewGroup parent)
	{
		Project.Sheet sheet = (Project.Sheet) getItem(position);
		Resources res = activity.getResources();

		if (convertView == null)
		{
			convertView = LayoutInflater.from(activity).inflate(R.layout.sheet_list_item, parent, false);
		}

		boolean isCurrentSheet = position == project.getCurrentSheetIndex();

		final TextView sheetName = (TextView) convertView.findViewById(R.id.textview_sheet_name);
		ImageView buttonRename = (ImageView) convertView.findViewById(R.id.imageview_sheet_button_rename);
		ImageView buttonDelete = (ImageView) convertView.findViewById(R.id.imageview_sheet_button_delete);
		ImageView thumbnailView = (ImageView) convertView.findViewById(R.id.imageview_sheet_thumbnail);
		ImageView dragHandle = (ImageView) convertView.findViewById(R.id.imageview_sheet_drag_handle);

		if (AppearanceUtils.isThemeDark())
		{
			convertView.setBackgroundColor(res.getColor(isCurrentSheet ? R.color.highlight_dark : R.color.black));
			sheetName.setTextColor(res.getColor(R.color.text_white));
		}
		else
		{
			convertView.setBackgroundColor(res.getColor(isCurrentSheet ? R.color.highlight : R.color.white));
			sheetName.setTextColor(res.getColor(isCurrentSheet ? R.color.text_white : R.color.text_black));
		}

		sheetName.setText(sheet.getName());
		sheetName.setTypeface(isCurrentSheet ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
		if (!isCurrentSheet)
		{
			sheetName.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					activity.changeSheet(position);
					activity.closeSheetMenu();
				}
			});
		}

		buttonRename.setImageDrawable(ResourcesCompat.getDrawable(activity, isCurrentSheet ? R.drawable.rename_box_white : R.drawable.rename_box));
		buttonRename.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				activity.renameSheet(position, sheetName);
			}
		});

		buttonDelete.setImageDrawable(ResourcesCompat.getDrawable(activity, isCurrentSheet ? R.drawable.delete_white : R.drawable.delete));
		buttonDelete.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				activity.deleteSheet(position);
			}
		});

		thumbnailView.setImageDrawable(sheet.getThumbnail());

		dragHandle.setImageDrawable(ResourcesCompat.getDrawable(activity, isCurrentSheet ? R.drawable.drag_vertical_white : R.drawable.drag_vertical));

		return convertView;
	}

	@Override
	public void swapItems(int index1, int index2)
	{
		project.swapSheets(index1, index2);
		notifyDataSetChanged();
	}

	@SuppressWarnings("deprecated")
	public void updateTheme(View drawer)
	{
		Resources res = activity.getResources();
		DynamicListView sheetList = (DynamicListView) drawer.findViewById(R.id.dynamiclistview_sheet_list);
		sheetList.setBackgroundColor(res.getColor(AppearanceUtils.isThemeDark() ? R.color.black : R.color.white));
		ImageView iconNewSheet = (ImageView) drawer.findViewById(R.id.imageview_icon_new_sheet);
		iconNewSheet.setImageDrawable(ResourcesCompat.getDrawable(activity, R.drawable.plus_box));
		TextView labelNewSheet = (TextView) drawer.findViewById(R.id.textview_button_new_sheet);
		labelNewSheet.setTextColor(res.getColor(AppearanceUtils.isThemeDark() ? R.color.text_white : R.color.text_black));
	}

	@Override
	public boolean hasStableIds()
	{
		return true;
	}
}
