package ironartichoke.sketchpad.adapter;

import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;

import java.io.File;
import java.text.Collator;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import ironartichoke.sketchpad.BrowserActivity;
import ironartichoke.sketchpad.Prefs;
import ironartichoke.sketchpad.R;
import ironartichoke.sketchpad.util.AppearanceUtils;
import ironartichoke.sketchpad.util.io.IOUtils;

/**
 * The adapter class for sketchbooks as listed in the {@link BrowserActivity}.
 */
public class ProjectAdapter extends BaseAdapter
{
	/** A reference to the activity. */
	private BrowserActivity activity;
	/** The backing array for the adapter. */
	private ArrayList<File> projectFiles;
	/** A list of the sketchbook names, derived from the backing array for convenience. */
	private String[] projectNames;
	/** A Picasso instance for managing the sketchbook thumbnails. */
	private Picasso picasso;
	/** The color integer for the list items. */
	private int textColor;
	/** The name of the currently loaded sketchbook. */
	private String currentProjectName;

	/**
	 * Creates a new <code>ProjectAdapter</code>.
	 * @param activity The <code>BrowserActivity</code>.
	 * @param currentProjectName The name of the currently loaded sketchbook.
	 */
	@SuppressWarnings("deprecated")
	public ProjectAdapter(BrowserActivity activity, String currentProjectName)
	{
		this.activity = activity;
		picasso = Picasso.with(activity);

		Resources res = activity.getResources();
		textColor = res.getColor(AppearanceUtils.isThemeDark() ? R.color.text_white : R.color.text_black);
		this.currentProjectName = currentProjectName;

		populateFileArray();
	}

	/**
	 * Populates the backing array to reflect the list of sketchbook files.
	 */
	public void populateFileArray()
	{
		ProjectFileComparator comparator =
				new ProjectFileComparator(Prefs.getInt(R.string.pref_project_sort_by), Prefs.getInt(R.string.pref_project_sort_order));

		File rootDir = IOUtils.getDirectory();
		if (rootDir != null)
		{
			projectFiles = new ArrayList<>(FileUtils.listFiles(rootDir, new ProjectFileFilter(), null));
			Collections.sort(projectFiles, comparator);
			projectNames = new String[getCount()];
			int size = projectNames.length;
			for (int f = 0; f < size; f++)
			{
				String name = projectFiles.get(f).getName();
				name = name.substring(0, name.length() - IOUtils.TAR_GZ.length()); // Remove .tar.gz extension.
				projectNames[f] = name;
			}
		}

		notifyDataSetChanged();
	}

	@Override
	public int getCount()
	{
		return projectFiles.size();
	}

	@Override
	public File getItem(int position)
	{
		return projectFiles.get(position);
	}

	@Override
	public long getItemId(int position)
	{
		return projectFiles.get(position).hashCode();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		File file = getItem(position);

		if (convertView == null)
		{
			convertView = LayoutInflater.from(activity).inflate(R.layout.project_list_item, parent, false);
		}

		convertView.setOnLongClickListener(activity);
		convertView.setTag(R.id.tag_file, file);

		String fileName = file.getName();
		String fileNameWithNoExt = fileName.substring(0, fileName.length() - IOUtils.TAR_GZ.length());
		convertView.setTag(R.id.tag_name, fileNameWithNoExt);

		ImageView projectThumbnail = (ImageView) convertView.findViewById(R.id.imageview_project_thumbnail);
		File thumbnailFile = new File(IOUtils.getDirectory(), fileNameWithNoExt + IOUtils.PNG);
		if (fileNameWithNoExt.equals(currentProjectName))
		{
			picasso.invalidate(thumbnailFile);
		}
		picasso.load(thumbnailFile).into(projectThumbnail);

		TextView projectName = (TextView) convertView.findViewById(R.id.textview_project_name);
		projectName.setText(projectNames[position]);
		projectName.setTextColor(textColor);

		TextView projectDate = (TextView) convertView.findViewById(R.id.textview_project_modified_date);
		projectDate.setText(formatUnixTime(file.lastModified()));

		return convertView;
	}

	/**
	 * A convenience method for converting Unix time into a time and date formatted according to the device's locale.
	 * @param millis The Unix time in milliseconds.
	 * @return The formatted time and date string.
	 */
	private String formatUnixTime(long millis)
	{
		DateFormat dateFormat = DateFormat.getDateTimeInstance();
		return dateFormat.format(new Date(millis));
	}

	/**
	 * Checks whether a sketchbook with the given name already exists.
	 * @param name The name to check.
	 * @return True if a sketchbook with the given name exists; false otherwise.
	 */
	public boolean doesProjectWithNameExist(String name)
	{
		for (String p : projectNames)
		{
			if (name.equals(p)) return true;
		}
		return false;
	}

	@Override
	public boolean hasStableIds()
	{
		return true;
	}

	/**
	 * A comparator used for sorting the sketchbooks.
	 * Sketchbooks can be sorted either by name or by last edit time, in either ascending or descending order.
	 */
	public class ProjectFileComparator implements Comparator<File>
	{
		/** The constant for sorting by name. */
		public static final int SORT_BY_NAME = 0;
		/** The constant for sorting by date. */
		public static final int SORT_BY_DATE = ~SORT_BY_NAME;

		/** The constant for sorting in ascending order. */
		public static final int ORDER_ASCENDING = 0;
		/** The constant for sorting in descending order. */
		public static final int ORDER_DESCENDING = ~ORDER_ASCENDING;

		/** The current sorting convention. Either {@link ProjectFileComparator#SORT_BY_NAME} or {@link ProjectFileComparator#SORT_BY_DATE}. */
		private int sortBy = SORT_BY_NAME;
		/** The current sorting order. Either {@link ProjectFileComparator#ORDER_ASCENDING} or {@link ProjectFileComparator#ORDER_DESCENDING}. */
		private int order = ORDER_ASCENDING;
		/** A Collator instance for locale-sensitive sorting. */
		private Collator collator;

		/**
		 * Instantiates a new <code>ProjectFileComparator</code> with the given parameters.
		 * @param sortBy The sorting convention.
		 * @param order The sorting order.
		 */
		public ProjectFileComparator(int sortBy, int order)
		{
			if ((sortBy != SORT_BY_NAME && sortBy != SORT_BY_DATE) || (order != ORDER_ASCENDING && order != ORDER_DESCENDING))
			{
				throw new IllegalArgumentException("Sorting constants invalid.");
			}

			this.sortBy = sortBy;
			this.order = order;
			collator = Collator.getInstance();
		}

		/**
		 * Checks that the current sorting convention matches the given.
		 * @param sortBy The sorting convention to check for.
		 * @return True if the sorting convention matches; false otherwise.
		 */
		public boolean isSortBy(int sortBy)
		{
			return this.sortBy == sortBy;
		}

		/**
		 * Checks that the current sorting order matches the given.
		 * @param order The sorting order to check for.
		 * @return True if the sorting order matches; false otherwise.
		 */
		public boolean isOrder(int order)
		{
			return this.order == order;
		}

		/**
		 * Compares the given sketchbook files according to the current sorting parameters.
		 * @param lhs The first file.
		 * @param rhs The second file.
		 * @return A positive int if <code>lhs</code> comes before <code>rhs</code>,
		 *         a negative int if <code>rhs</code> comes before <code>lhs</code>, or
		 *         0 if <code>lhs</code> and <code>rhs</code> are identical.
		 */
		@Override
		public int compare(File lhs, File rhs)
		{
			String lName = lhs.getName();
			String rName = rhs.getName();
			long lDate = lhs.lastModified();
			long rDate = rhs.lastModified();

			int comp;

			if (isSortBy(SORT_BY_DATE))
			{
				comp = lDate > rDate ? -1 : 1;

			}
			else // Sort by name
			{
				comp = -collator.compare(lName, rName);
			}

			comp *= isOrder(SORT_BY_DATE) ? 1 : -1;
			return comp;
		}
	}

	/**
	 * A subclass of {@link IOFileFilter} that allows the sketchbook file type (.TAR.GZ).
	 */
	private class ProjectFileFilter implements IOFileFilter
	{
		@Override
		public boolean accept(File file)
		{
			return file.getName().endsWith(IOUtils.TAR_GZ);
		}

		@Override
		public boolean accept(File dir, String name)
		{
			return (new File(dir, name)).getName().endsWith(IOUtils.TAR_GZ);
		}
	}
}
