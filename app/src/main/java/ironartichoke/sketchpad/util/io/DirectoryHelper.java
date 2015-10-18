package ironartichoke.sketchpad.util.io;

import android.content.Intent;
import android.os.Environment;

import java.io.File;

import ironartichoke.sketchpad.Prefs;

/**
 * A class that keeps and fetches the directories for storing sketchbook data.
 * This class is intended to be mocked during testing.
 */
public class DirectoryHelper
{
	/** The intent for the activity that this instance is created from. */
	private Intent activityIntent;

	/** The top application directory. */
	private File topDirectory;
	/** The directory for storing the current, unarchived sketchbook files. */
	private File workingDirectory;

	/** The name of the intent extra containing the top directory on internal storage. Used for testing. */
	public static final String EXTRA_INTERNAL_TOP_DIR = "internalTopDir";
	/** The name of the intent extra containing the top directory on external storage. Used for testing. */
	public static final String EXTRA_EXTERNAL_TOP_DIR = "externalTopDir";

	/**
	 * Instantiates a new <code>DirectoryHelper</code>.
	 * @param activityIntent The intent of the activity that this instance should be created from.
	 */
	public DirectoryHelper(Intent activityIntent)
	{
		this.activityIntent = activityIntent;
	}

	/**
	 * Set the directories appropriately.
	 */
	public void setDirectories()
	{
		setTopDirectory();
		setWorkingDirectory();
	}

	/**
	 * Gets the application directory.
	 * @return The application directory.
	 */
	public File getTopDirectory()
	{
		return topDirectory;
	}

	/**
	 * Sets the application directory according to which storage (internal or external) is in use.
	 */
	private void setTopDirectory()
	{
		if (IOUtils.isUsingExternalStorage())
		{
			try
			{
				topDirectory = getExternalDirectory();
				if (!topDirectory.exists())
				{
					if (IOUtils.isExternalStorageWritable())
					{
						topDirectory.mkdir();
					}
					else
					{
						throw new IOUtils.ExternalStorageUnwritableException();
					}
				}
			}
			catch (IOUtils.ExternalStorageException e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			topDirectory = getInternalDirectory();
			topDirectory.mkdir();
		}
	}

	/**
	 * Gets the temporary working directory.
	 * @return The working directory.
	 */
	public File getWorkingDirectory()
	{
		return workingDirectory;
	}

	/**
	 * Sets the working directory according to which storage (internal or external) is in use.
	 */
	private void setWorkingDirectory()
	{
		if (IOUtils.isUsingExternalStorage())
		{
			try
			{
				workingDirectory = getWorkingDirectoryExternal();
				if (!workingDirectory.exists())
				{
					if (IOUtils.isExternalStorageWritable())
					{
						workingDirectory.mkdir();
					}
					else
					{
						throw new IOUtils.ExternalStorageUnwritableException();
					}
				}
			}
			catch (IOUtils.ExternalStorageException e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			workingDirectory = getWorkingDirectoryInternal();
			workingDirectory.mkdir();
		}
	}

	/**
	 * Specifically gets the internal application directory, regardless of the current storage selection.
	 * @return The internal application directory.
	 */
	private File getInternalDirectory()
	{
		// A JUnit test may specify a particular directory to use.
		if (activityIntent.hasExtra(EXTRA_INTERNAL_TOP_DIR))
		{
			return (File) activityIntent.getSerializableExtra(EXTRA_INTERNAL_TOP_DIR);
		}
		// When not testing, just use the real directory.
		else
		{
			return Prefs.getContext().getFilesDir();
		}
	}

	/**
	 * Specifically gets the external application directory, regardless of the current storage selection.
	 * @return The external application directory.
	 */
	private File getExternalDirectory()
	{
		// A JUnit test may specify a particular directory to use.
		if (activityIntent.hasExtra(EXTRA_EXTERNAL_TOP_DIR))
		{
			return getExternalDirectory((File) activityIntent.getSerializableExtra(EXTRA_EXTERNAL_TOP_DIR));
		}
		// When not testing, just use the real directory.
		else
		{
			return getExternalDirectory(Prefs.getContext().getExternalFilesDir(null));
		}
	}

	/**
	 * Ensures that the specified external directory is fully readable and writeable before returning it.
	 * @param externalDirectory The external directory.
	 * @return <code>externalDirectory</code> if it is fully readable and writeable; null otherwise.
	 */
	private File getExternalDirectory(File externalDirectory)
	{
		if (IOUtils.isExternalStorageReadable())
		{
			if (!externalDirectory.exists())
			{
				if (IOUtils.isExternalStorageWritable())
				{
					externalDirectory.mkdir();
				}
				else
				{
					return null;
				}
			}
			return externalDirectory;
		}
		else
		{
			return null;
		}
	}

	/**
	 * Specifically gets the internal working directory, regardless of the current storage selection.
	 * @return The internal application directory.
	 */
	public File getWorkingDirectoryInternal()
	{
		return new File(getInternalDirectory(), IOUtils.WORKING_DIRECTORY);
	}

	/**
	 * Specifically gets the external working directory, regardless of the current storage selection.
	 * @return The internal application directory.
	 */
	public File getWorkingDirectoryExternal()
	{
		return new File(getExternalDirectory(), IOUtils.WORKING_DIRECTORY);
	}
}
