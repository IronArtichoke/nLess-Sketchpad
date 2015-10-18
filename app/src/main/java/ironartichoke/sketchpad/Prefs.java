package ironartichoke.sketchpad;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.SparseArray;

import ironartichoke.sketchpad.adapter.ProjectAdapter;
import ironartichoke.sketchpad.util.AppearanceUtils;
import ironartichoke.sketchpad.util.io.IOUtils;

/**
 * A convenience class for accessing the shared preferences.
 */
public abstract class Prefs
{
	/** Any context. */
	private static Context ctx;
	/** An array containing the default values for certain prefs. */
	private static SparseArray<Object> defaultValues = new SparseArray<>();

	/** The name of the preferences file. */
	private static final String PREFS_FILE = "prefs";

	/**
	 * Initializes the class.
	 * This should be called before any attempt to read or write preferences.
	 * @param ctx Any context.
	 */
	public static void initialize(@NonNull Context ctx)
	{
		Prefs.ctx = ctx;
		setDefaultValues();
	}

	/**
	 * Sets default values for some preferences where required.
	 */
	private static void setDefaultValues()
	{
		Prefs.setDefaultValue(R.string.pref_brightness, AppearanceUtils.THEME_LIGHT);
		Prefs.setDefaultValue(R.string.pref_handedness, AppearanceUtils.RIGHT_HANDED);
		Prefs.setDefaultValue(R.string.pref_last_project_name, Project.DEFAULT_NAME);
		Prefs.setDefaultValue(R.string.pref_project_sort_by, ProjectAdapter.ProjectFileComparator.SORT_BY_NAME);
		Prefs.setDefaultValue(R.string.pref_project_sort_order, ProjectAdapter.ProjectFileComparator.ORDER_ASCENDING);
		Prefs.setDefaultValue(R.string.pref_storage, IOUtils.INTERNAL_STORAGE);
		Prefs.setDefaultValue(R.string.pref_zoom, NotepadView.ZOOM_DEFAULT);
	}

	/**
	 * Retrieves the default value for the given preference.
	 * Used for testing only.
	 * @param prefStringId The resource ID of the preference string.
	 * @return The default value for the preference, or <code>null</code> if a default hasn't been set.
	 */
	@SuppressWarnings("unused")
	public static Object getDefaultValue(int prefStringId)
	{
		return defaultValues.get(prefStringId);
	}

	/**
	 * Assigns a default value for the given preference.
	 * This means that the default value doesn't need to be repeated in the code whenever the preference is read.
	 * If a default value is reassigned, then it will overwrite the previous one.
	 * @param prefStringId The resource ID of the preference string.
	 * @param defaultValue The default value for the preference.
	 */
	private static void setDefaultValue(int prefStringId, Object defaultValue)
	{
		defaultValues.put(prefStringId, defaultValue);
	}

	/**
	 * Retrieves the context.
	 * @return The context.
	 */
	public static Context getContext()
	{
		return ctx;
	}

	/**
	 * Retrieves an int value from the preferences.
	 * @param prefStringId The resource ID of the preference string.
	 * @return The preference value if it exists; otherwise, it returns the default value set using {@link #setDefaultValue(int, Object)}.
	 */
	@SuppressWarnings("unused")
	public static int getInt(int prefStringId)
	{
		return ctx.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
				.getInt(ctx.getString(prefStringId), (Integer) defaultValues.get(prefStringId, 0));
	}

	/**
	 * Retrieves a float value from the preferences.
	 * @param prefStringId The resource ID of the preference string.
	 * @return The preference value if it exists; otherwise, it returns the default value set using {@link #setDefaultValue(int, Object)}.
	 */
	@SuppressWarnings("unused")
	public static float getFloat(int prefStringId)
	{
		return ctx.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
				.getFloat(ctx.getString(prefStringId), (Float) defaultValues.get(prefStringId, 0f));
	}

	/**
	 * Retrieves a long value from the preferences.
	 * @param prefStringId The resource ID of the preference string.
	 * @return The preference value if it exists; otherwise, it returns the default value set using {@link #setDefaultValue(int, Object)}.
	 */
	@SuppressWarnings("unused")
	public static long getLong(int prefStringId)
	{
		return ctx.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
				.getLong(ctx.getString(prefStringId), (Long) defaultValues.get(prefStringId, 0l));
	}

	/**
	 * Retrieves a boolean value from the preferences.
	 * @param prefStringId The resource ID of the preference string.
	 * @return The preference value if it exists; otherwise, it returns the default value set using {@link #setDefaultValue(int, Object)}.
	 */
	@SuppressWarnings("unused")
	public static boolean getBoolean(int prefStringId)
	{
		return ctx.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
				.getBoolean(ctx.getString(prefStringId), (Boolean) defaultValues.get(prefStringId, false));
	}

	/**
	 * Retrieves a <code>String</code> value from the preferences.
	 * @param prefStringId The resource ID of the preference string.
	 * @return The preference value if it exists; otherwise, it returns the default value set using {@link #setDefaultValue(int, Object)}.
	 */
	@SuppressWarnings("unused")
	public static String getString(int prefStringId)
	{
		return ctx.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
				.getString(ctx.getString(prefStringId), (String) defaultValues.get(prefStringId, ""));
	}

	/**
	 * Retrieves a preference of the given type.
	 * Used for testing only.
	 * @param prefStringId The resource ID of the preference string.
	 * @param type The type to return (either <code>Integer.class</code>, <code>Float.class</code>, <code>Long.class</code>,
	 *             <code>Boolean.class</code> or <code>String.class</code>).
	 * @return The preference value if it exists; otherwise, it returns the default value set using {@link #setDefaultValue(int, Object)}.
	 */
	@SuppressWarnings("unused")
	public static Object getArbitraryType(int prefStringId, Class type)
	{
		if (type.equals(Integer.class))
		{
			return getInt(prefStringId);
		}
		else if (type.equals(Float.class))
		{
			return getFloat(prefStringId);
		}
		else if (type.equals(Long.class))
		{
			return getLong(prefStringId);
		}
		else if (type.equals(Boolean.class))
		{
			return getBoolean(prefStringId);
		}
		else if (type.equals(String.class))
		{
			return getString(prefStringId);
		}
		return null;
	}

	/**
	 * Gets a <code>Prefs.Editor</code> for the shared preferences.
	 * This class mirrors the function of <code>SharedPreferences.Editor</code>, but it reuses its own <code>Context</code> instance.
	 * Note that {@link #initialize(Context)} must be called first.
	 * @return The editor.
	 */
	public static Editor edit()
	{
		return new Editor();
	}

	/**
	 * A class which mirrors the function of <code>SharedPreferences.Editor</code>, but without the need of a <code>Context</code>.
	 */
	public static class Editor
	{
		/** The backing <code>SharedPreferences.Editor</code> instance. */
		SharedPreferences.Editor editor;

		/**
		 * Instantiates a new preferences editor.
		 */
		private Editor()
		{
			editor = ctx.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE).edit();
		}

		/**
		 * @see SharedPreferences.Editor#apply()
		 */
		public void apply()
		{
			editor.apply();
		}

		/**
		 * @see SharedPreferences.Editor#commit()
		 */
		public void commit()
		{
			editor.commit();
		}

		/**
		 * @see SharedPreferences.Editor#putInt(String, int)
		 */
		@SuppressWarnings("unused")
		public Editor putInt(int prefStringId, int value)
		{
			editor.putInt(ctx.getString(prefStringId), value);
			return this;
		}

		/**
		 * @see SharedPreferences.Editor#putString(String, String)
		 */
		@SuppressWarnings("unused")
		public Editor putFloat(int prefStringId, float value)
		{
			editor.putFloat(ctx.getString(prefStringId), value);
			return this;
		}

		/**
		 * @see SharedPreferences.Editor#putLong(String, long)
		 */
		@SuppressWarnings("unused")
		public Editor putLong(int prefStringId, long value)
		{
			editor.putLong(ctx.getString(prefStringId), value);
			return this;
		}

		/**
		 * @see SharedPreferences.Editor#putBoolean(String, boolean)
		 */
		@SuppressWarnings("unused")
		public Editor putBoolean(int prefStringId, boolean value)
		{
			editor.putBoolean(ctx.getString(prefStringId), value);
			return this;
		}

		/**
		 * @see SharedPreferences.Editor#putString(String, String)
		 */
		@SuppressWarnings("unused")
		public Editor putString(int prefStringId, String value)
		{
			editor.putString(ctx.getString(prefStringId), value);
			return this;
		}
	}
}
