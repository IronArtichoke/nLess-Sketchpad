package ironartichoke.sketchpad.util.io;

import android.os.Environment;
import android.util.Log;

import org.apache.commons.io.FileUtils;
import org.rauschig.jarchivelib.ArchiveFormat;
import org.rauschig.jarchivelib.Archiver;
import org.rauschig.jarchivelib.ArchiverFactory;
import org.rauschig.jarchivelib.CompressionType;

import java.io.File;
import java.io.IOException;

import ironartichoke.sketchpad.Prefs;
import ironartichoke.sketchpad.Project;
import ironartichoke.sketchpad.R;

/**
 * A utility class for IO operations.
 */
public final class IOUtils
{
	/** For working with directories. */
	public static DirectoryHelper directoryHelper;
	/** For checking the state of the external storage. */
	public static ExternalStorageHelper externalStorageHelper;

	/** The current storage location. Either {@link #INTERNAL_STORAGE} or {@link #EXTERNAL_STORAGE}. */
	private static int storage;

	/** The constant for internal storage. */
	public static final int INTERNAL_STORAGE = 0;
	/** The constant for external storage. */
	private static final int EXTERNAL_STORAGE = ~INTERNAL_STORAGE;

	/** The name of the external application directory. */
	public static final String EXTERNAL_DIRECTORY = "IronArtichoke";
	/** The name of the temporary working directory. */
	public static final String WORKING_DIRECTORY = ".tmp";

	/** The file extension for sketchbook files. */
	public static final String TAR_GZ = ".tar.gz";
	/** The file extension for sketchbook thumbnail files. */
	public static final String PNG = ".png";

	/**
	 * Prepares the class for use by reading the storage settings from <code>Prefs</code> and setting the directories accordingly.
	 * <b>Note:</b> This should be called before any IO operations are performed.
	 */
	public static void initialize()
	{
		storage = Prefs.getInt(R.string.pref_storage);
		setDirectories();
	}

	/**
	 * Sets the application and working directories.
	 */
	private static void setDirectories()
	{
		directoryHelper.setDirectories();
	}

	/**
	 * Requests that the internal storage be used.
	 */
	private static void useInternalStorage(Project project)
	{
		if (storage != INTERNAL_STORAGE)
		{
			storage = INTERNAL_STORAGE;
			commitStorageSetting();
			copyWorkingFilesToInternal();
			onStorageChanged(project);
		}
	}

	/**
	 * Requests that the external storage be used, provided that it can be read.
	 * @throws ExternalStorageUnreadableException If the external storage cannot be read.
	 */
	private static void useExternalStorage(Project project) throws ExternalStorageUnreadableException
	{
		if (storage != EXTERNAL_STORAGE)
		{
			if (isExternalStorageReadable())
			{
				storage = EXTERNAL_STORAGE;
				commitStorageSetting();
				copyWorkingFilesToExternal();
				onStorageChanged(project);
			}
			else
			{
				throw new ExternalStorageUnreadableException();
			}
		}
	}

	/**
	 * Toggles the storage setting between internal and external.
	 * If the external storage is unreadable, the operation is cancelled.
	 * @return If switching to internal storage: always true. If switching to external storage: true if successful; false otherwise.
	 */
	public static boolean toggleStorageSetting(Project project)
	{
		int newStorage = ~storage;
		switch (newStorage)
		{
			case EXTERNAL_STORAGE:
			{
				try
				{
					useExternalStorage(project);
					return true;
				}
				catch (ExternalStorageUnreadableException e)
				{
					e.printStackTrace();
					return false;
				}
			}
			default:
			{
				useInternalStorage(project);
				return true;
			}
		}
	}

	/**
	 * Updates the directories and the sketchbook files following a change in the storage setting.
	 * @param project The current sketchbook.
	 */
	private static void onStorageChanged(Project project)
	{
		setDirectories();
		project.setFiles();
		project.setUnsavedChanges(true);
	}

	/**
	 * Commits the current storage setting to the <code>SharedPreferences</code>.
	 */
	private static void commitStorageSetting()
	{
		Prefs.edit().putInt(R.string.pref_storage, storage).commit();
	}

	/**
	 * Check whether the currently selected storage location is internal.
	 * @return True if internal storage is being used; false otherwise.
	 */
	public static boolean isUsingInternalStorage()
	{
		return storage == INTERNAL_STORAGE;
	}

	/**
	 * Check whether the currently selected storage location is external.
	 * @return True if external storage is being used; false otherwise.
	 */
	public static boolean isUsingExternalStorage()
	{
		return storage == EXTERNAL_STORAGE;
	}

	/**
	 * Checks whether the external storage can be written to.
	 * @return Returns true if the external storage can be written to; false otherwise.
	 */
	public static boolean isExternalStorageWritable()
	{
		String state = getExternalStorageState();
		return Environment.MEDIA_MOUNTED.equals(state);
	}

	/**
	 * Checks whether the external storage is readable.
	 * <b>Note:</b> This method makes no distinction between <i>readable</i> and <i>read-only</i>.
	 * @return Returns true if the external storage can be read; false otherwise.
	 */
	public static boolean isExternalStorageReadable()
	{
		String state = getExternalStorageState();
		return Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
	}

	/**
	 * A helper method for getting the external storage state.
	 * @return The state of external storage.
	 */
	public static String getExternalStorageState()
	{
		return externalStorageHelper.getState();
	}

	/**
	 * Gets the appropriate application directory for the current storage setting.
	 */
	public static File getDirectory()
	{
		return directoryHelper.getTopDirectory();
	}

	/**
	 * Gets the working directory where temporary data is stored, according to the current storage setting.
	 * @return The directory.
	 */
	public static File getWorkingDirectory()
	{
		return directoryHelper.getWorkingDirectory();
	}

	/**
	 * Gets the working directory in the internal storage, regardless of the current storage setting.
	 * @return The directory.
	 */
	private static File getWorkingDirectoryInternal()
	{
		return directoryHelper.getWorkingDirectoryInternal();
	}

	/**
	 * Gets the working directory in the external storage, if accessible, regardless of the current storage setting.
	 * @return The directory.
	 */
	private static File getWorkingDirectoryExternal()
	{
		return directoryHelper.getWorkingDirectoryExternal();
	}

	/**
	 * Copies the temporary sketchbook files from the internal working directory to the external working directory.
	 */
	private static void copyWorkingFilesToExternal()
	{
		if (!isExternalStorageWritable())
		{
			return;
		}
		try
		{
			IOUtils.deleteDirectory(getWorkingDirectoryExternal(), false);
			FileUtils.copyDirectory(getWorkingDirectoryInternal(), getWorkingDirectoryExternal());
			IOUtils.deleteDirectory(getWorkingDirectoryInternal(), false);
		}
		catch (IOException e)
		{
			// Ignore exception.
		}
	}

	/**
	 * Copies the temporary sketchbook files from the external working directory to the internal working directory.
	 */
	private static void copyWorkingFilesToInternal()
	{
		if (!isExternalStorageReadable())
		{
			return;
		}
		try
		{
			IOUtils.deleteDirectory(getWorkingDirectoryInternal(), false);
			FileUtils.copyDirectory(getWorkingDirectoryExternal(), getWorkingDirectoryInternal());
			IOUtils.deleteDirectory(getWorkingDirectoryExternal(), false);
		}
		catch (IOException e)
		{
			// Ignore exception.
		}
	}

	/**
	 * A convenience method for recursively deleting a directory.
	 * @param file The top directory.
	 * @param deleteRoot Whether the top directory should also be deleted.
	 */
	public static void deleteDirectory(File file, boolean deleteRoot)
	{
		if (file.isDirectory())
		{
			File[] files = file.listFiles();
			if (files != null)
			{
				for (File f : files)
				{
					deleteDirectory(f, true);
				}
			}
		}
		if (deleteRoot) file.delete();
	}

	/**
	 * Clears the working directories on both internal storage and, if accessible, external storage.
	 */
	public static void deleteWorkingDirectories()
	{
		IOUtils.deleteDirectory(getWorkingDirectoryInternal(), true);
		IOUtils.deleteDirectory(getWorkingDirectoryExternal(), true);
	}

	/**
	 * Creates a gzipped tarball.
	 * @param name The name of the archive.
	 * @param destination The directory where it should be stored.
	 * @param source The contents of the archive.
	 * @throws IOException
	 */
	public static void archive(String name, File destination, File source) throws IOException
	{
		Archiver archiver = ArchiverFactory.createArchiver(ArchiveFormat.TAR, CompressionType.GZIP);
		archiver.create(name, destination, source);
	}

	/**
	 * Extracts the files from a gzipped tarball.
	 * @param archive The archive to extract.
	 * @param destination The directory into which to extract the contents.
	 * @throws IOException
	 */
	public static void extract(File archive, File destination) throws IOException
	{
		Archiver archiver = ArchiverFactory.createArchiver(ArchiveFormat.TAR, CompressionType.GZIP);
		archiver.extract(archive, destination);
	}

	/**
	 * Checks if the given filename is valid according to the rules of the OS.
	 * @param name The filename.
	 * @return Whether the filename is valid.
	 */
	public static boolean isFilenameValid(String name)
	{
		return isFilenameValid(new File(getWorkingDirectory(), name));
	}

	/**
	 * Checks if the given file has a valid name according to the rules of the OS.
	 * @param file The file.
	 * @return Whether the file's name is valid.
	 */
	private static boolean isFilenameValid(File file)
	{
		try
		{
			file.getCanonicalPath();
			return true;
		}
		catch (IOException e)
		{
			return false;
		}
	}

	/**
	 * A base class for exceptions concerning external storage access.
	 * Only its subclasses should be thrown.
	 * @see ExternalStorageUnreadableException
	 * @see ExternalStorageUnwritableException
	 */
	public abstract static class ExternalStorageException extends IOException {}

	/**
	 * An exception that should be thrown if reading the external storage is attempted when it is unreadable.
	 * @see ExternalStorageUnwritableException
	 */
	private static class ExternalStorageUnreadableException extends ExternalStorageException {}

	/**
	 * An exception that should be thrown if writing to the external storage is attempted when it is unwritable.
	 * @see ExternalStorageUnreadableException
	 */
	public static class ExternalStorageUnwritableException extends ExternalStorageException {}
}
