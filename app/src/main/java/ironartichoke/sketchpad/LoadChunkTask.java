package ironartichoke.sketchpad;

import android.os.AsyncTask;

/**
 * A subclass of <code>AsyncTask</code> that loads chunks from the working directory into memory.
 */
class LoadChunkTask extends AsyncTask<Void, Void, Void>
{
	/** A reference to the current sketchbook. */
	private Project project;
	/** The list of chunk IDs which correspond to the chunks that are to be loaded. */
	private long[] chunkIds;

	/**
	 * Prepares an asynchronous task for loading chunks.
	 * @param project The project.
	 * @param chunkIds The IDs of the chunks to load.
	 */
	public LoadChunkTask(Project project, long[] chunkIds)
	{
		this.project = project;
		this.chunkIds = chunkIds;
	}

	@Override
	protected Void doInBackground(Void... params)
	{
		project.loadChunks(chunkIds);
		return null;
	}
}
