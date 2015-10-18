package ironartichoke.sketchpad.util.io;

import android.os.Environment;

/**
 * A class whose only function is to retrieve the state of the external storage via {@link #getState()}.
 * This class is intended to be mocked during testing.
 */
public class ExternalStorageHelper
{
	/**
	 * Gets the state of the external storage.
	 * @return The value of {@link Environment#getExternalStorageState()}.
	 */
	public String getState()
	{
		return Environment.getExternalStorageState();
	}
}
