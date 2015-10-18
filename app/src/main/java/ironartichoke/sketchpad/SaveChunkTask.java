package ironartichoke.sketchpad;

import android.os.AsyncTask;

/**
 * A subclass of <code>AsyncTask</code> that saves chunks to the working directory and optionally unloads them.
 */
class SaveChunkTask extends AsyncTask<Void, Void, Void>
{
	/** A reference to the current sketchbook. */
	private Project project;
	/** The list of chunk IDs which correspond to the chunks that are to be saved. */
	private long[] chunkIds;
	/** Whether the chunks should be unloaded after saving. */
	private boolean unload;

	/**
	 * Prepares an asynchronous task for saving chunks.
	 * @param project The project.
	 * @param chunkIds The IDs of the chunks to load.
	 * @param unload Whether to unload the chunks after saving them.
	 */
	public SaveChunkTask(Project project, long[] chunkIds, boolean unload)
	{
		this.project = project;
		this.chunkIds = chunkIds;
		this.unload = unload;
	}

	@Override
	protected Void doInBackground(Void... params)
	{
		project.saveChunks(chunkIds, unload);
		return null;
	}
}
