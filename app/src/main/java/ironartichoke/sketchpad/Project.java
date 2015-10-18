package ironartichoke.sketchpad;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.LongSparseArray;
import android.util.Log;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.BufferedInputStream;
import java.io.Externalizable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import ironartichoke.sketchpad.externalizable.ExternalizableArrayList;
import ironartichoke.sketchpad.util.ChunkUtils;
import ironartichoke.sketchpad.util.io.IOUtils;

/**
 * A class that represents a sketchbook.
 * <br\><br\>
 * A sketchbook contains {@link Sheet sheets}, which contain {@link Sheet.Chunk chunks}.
 * When a sketchbook is being worked on, it is stored in the working directory.
 * The sketchbook directory structure looks like this:
 *
 * <pre>
 *     Working directory
 *        Sketchbook metadata file
 *        Sheet folders ...
 *           Sheet metadata file
 *           Sheet thumbnail file
 *           Chunk files ...
 * </pre>
 *
 * When the user saves the sketchbook, the contents of the working directory are
 * stored in a gzipped tarball (TAR.GZ) in the app directory and an accompanying
 * thumbnail is stored alongside it.
 * <br\><br\>
 * When the app is closed, the working directory is deleted to save space. However, in the
 * event of a crash, the directory is left behind and can be used to recover any
 * unsaved work upon the next launch.
 */
public class Project
{
	public static final String DEFAULT_NAME = "";
	private static final String THUMBNAIL_FILE = "thumbnail.png";
	private static final String METADATA = "meta";
	public static final String RECOVERY_NAME = ".recovery";

	private String name = DEFAULT_NAME;

	private File archiveFile;
	private File metadataFile;
	private File thumbnailFile;

	private ArrayList<Sheet> sheets = new ArrayList<>();
	private int totalSheets = 0; // Temporary variable used when loading.
	public long[] sheetOrder; // Temporary variable used when loading.

	private int currentSheet = 0;
	private LongSparseArray<Project.Sheet.Chunk> loadedChunks = new LongSparseArray<>();
	private ArrayList<Stroke> loadedStrokes = new ArrayList<>();
	private long strokeCount = 0;

	private boolean unsavedChanges = false;

	/**
	 * The empty constructor.
	 * Note that the object is <b>not</b> usable by merely instantiating it with this constructor.
	 */
	public Project() {}

	/**
	 * Instantiates a sketchbook with the given name.
	 * @param projectName The name for the sketchbook.
	 *                    If this is <code>null</code>, the sketchbook will be considered new.
	 *                    Otherwise, the sketchbook is assumed to already exist in storage.
	 * @param res A <code>Resources</code> object.
	 */
	public Project(@Nullable String projectName, Resources res)
	{
		if (projectName == null)
		{
			initializeNew(res);
		}
		else
		{
			initializeLoaded(projectName);
		}
	}

	/**
	 * Resets everything.
	 * Resets the name, files, sheets, chunks and strokes.
	 */
	private void reset()
	{
		setName(DEFAULT_NAME);
		setFiles();
		sheets = new ArrayList<>();
		totalSheets = 0;
		sheetOrder = null;

		currentSheet = 0;
		loadedChunks = new LongSparseArray<>();
		loadedStrokes = new ArrayList<>();
		strokeCount = 0;

		unsavedChanges = false;
	}

	/**
	 * Initializes the new sketchbook with one empty sheet. Also clears the working directory.
	 * @param res A <code>Resources</code> object.
	 */
	public void initializeNew(Resources res)
	{
		reset();

		IOUtils.deleteWorkingDirectories();
		setName(DEFAULT_NAME);

		File workingDirectory = IOUtils.getWorkingDirectory();
		if (!workingDirectory.exists()) workingDirectory.mkdir();
		setFiles();

		sheets.add(new Sheet(Sheet.getDefaultName(res)));
		sheets.trimToSize();
	}

	/**
	 * Initializes an existing sketchbook.
	 * @param projectName The name of the sketchbook.
	 */
	public void initializeLoaded(String projectName)
	{
		reset();

		setName(projectName);
		setFiles();
	}

	/**
	 * Sets the sketchbook's metadata file, archive file and thumbnail file.
	 */
	public void setFiles()
	{
		setMetadataFile();
		setArchiveFile();
		setThumbnailFile();

		for (Sheet sheet : sheets)
		{
			sheet.setWorkingFile();
		}

		int chunks = loadedChunks.size();
		for (int c = 0; c < chunks; c++)
		{
			getChunk(c).setWorkingFile(this);
		}
	}

	/**
	 * Sets the sketchbook's metadata file.
	 * This contains general data about the sketchbook such as the sketchbook name, number of sheets,
	 * the order of the sheets, the stroke count, etc.
	 */
	private void setMetadataFile()
	{
		metadataFile = new File(IOUtils.getWorkingDirectory(), METADATA);
	}

	/**
	 * Sets the sketchbook's archive file.
	 * The sketchbook will be saved as a gzipped tarball as described by this file.
	 */
	private void setArchiveFile()
	{
		archiveFile = new File(IOUtils.getDirectory(), getName() + IOUtils.TAR_GZ);
	}

	/**
	 * Sets the sketchbook's thumbnail file.
	 * This appears in the Open screen when browsing sketchbooks.
	 */
	private void setThumbnailFile()
	{
		thumbnailFile = new File(IOUtils.getDirectory(), getName() + IOUtils.PNG);
	}

	/**
	 * Gets the name of the sketchbook.
	 * @return The name of the sketchbook.
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Sets the name of the sketchbook.
	 * @param name The name of the sketchbook.
	 */
	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 * Gets the sketchbook's archive file.
	 * @return The sketchbook's archive file.
	 */
	public File getArchiveFile()
	{
		return archiveFile;
	}

	/**
	 * Gets the sketchbook's metadata file.
	 * @return The sketchbook's metadata file.
	 */
	private File getMetadataFile()
	{
		return metadataFile;
	}

	/**
	 * Gets the sketchbook's thumbnail file.
	 * @return The sketchbook's thumbnail file.
	 */
	private File getThumbnailFile()
	{
		return thumbnailFile;
	}

	/**
	 * Gets the current sheet.
	 * @return The current sheet.
	 */
	public Sheet getCurrentSheet()
	{
		return getSheetAt(currentSheet);
	}

	/**
	 * Gets the index of the current sheet according to the sheet order.
	 * @return The index of the current sheet.
	 */
	public int getCurrentSheetIndex()
	{
		return currentSheet;
	}

	/**
	 * Saves the current sheet and loads another one.
	 * @param index The index of the sheet to load (NOT the sheet ID).
	 */
	public void setCurrentSheet(final NotepadView notepadView, final int index)
	{
		saveAllLoadedChunks(true, false);
		getCurrentSheet().saveMetadata();
		loadedChunks.clear();
		loadedStrokes.clear();
		currentSheet = index;
		notepadView.loadAllVisibleChunks(false, false);
		getCurrentSheet().loadMetadata();
	}

	/**
	 * Gets the sheet at the given index.
	 * @param index The index.
	 * @return The sheet at that index.
	 */
	public Sheet getSheetAt(int index)
	{
		return sheets.get(index);
	}

	/**
	 * Gets the total number of sheets in the sketchbook.
	 * @return The total number of sheets.
	 */
	public int getTotalSheets()
	{
		return sheets.size();
	}

	/**
	 * Adds a new sheet with the given name.
	 * @param name The sheet name.
	 */
	public void addNewSheet(String name)
	{
		Sheet newSheet = new Sheet(name);
		sheets.add(newSheet);
		setUnsavedChanges(true);
	}

	/**
	 * Adds a populated sheet to the end of the list.
	 * @param sheet The sheet to add.
	 */
	private void addLoadedSheet(Sheet sheet)
	{
		sheets.add(sheet);
	}

	/**
	 * Changes the name of the sheet at the given index.
	 * @param index The index of the sheet (NOT the sheet ID).
	 * @param name The new name for the sheet.
	 */
	public void renameSheet(int index, String name)
	{
		getSheetAt(index).setName(name);
		setUnsavedChanges(true);
	}

	/**
	 * Deletes the sheet at the given index.
	 * If the current sheet is deleted, then the next sheet is selected.
	 * @param index The index of the sheet (NOT the sheet ID).
	 */
	public void deleteSheet(int index)
	{
		getSheetAt(index).getFile().delete();
		sheets.remove(index);
		if (currentSheet >= sheets.size())
		{
			currentSheet = sheets.size() - 1;
		}
		setUnsavedChanges(true);
	}

	/**
	 * Swaps the positions of two sheets.
	 * @param index1 The index of the first sheet.
	 * @param index2 The index of the second sheet.
	 */
	public void swapSheets(int index1, int index2)
	{
		// If the current sheet is being swapped, change the current sheet number accordingly.
		if (currentSheet == index1)
		{
			currentSheet = index2;
		}
		else if (currentSheet == index2)
		{
			currentSheet = index1;
		}
		Sheet firstSheet = sheets.set(index1, getSheetAt(index2));
		sheets.set(index2, firstSheet);
		setUnsavedChanges(true);
	}

	/**
	 * Gets the array of sheets.
	 * @return The array of sheets.
	 */
	public ArrayList<Sheet> getSheets()
	{
		return sheets;
	}

	/**
	 * Checks if a sheet with the given name already exists.
	 * @param sheetName The name to check.
	 * @return True if a sheet already exists with this name; false otherwise.
	 */
	public boolean doesSheetExistWithName(String sheetName)
	{
		for (Sheet s : sheets)
		{
			if (sheetName.equals(s.getName())) return true;
		}
		return false;
	}

	/**
	 * Returns a suggested name for a new sheet.
	 * @param project The project.
	 * @param ctx A context.
	 * @return The sheet name.
	 */
	public static String getSuggestedNewSheetName(Project project, Context ctx)
	{
		int number = project.getTotalSheets() + 1;
		String sheetName = ctx.getString(R.string.term_sheet) + " " + number;
		while (project.doesSheetExistWithName(sheetName))
		{
			sheetName = ctx.getString(R.string.term_sheet) + " " + number;
		}
		return sheetName;
	}

	/**
	 * Gets the array of currently loaded strokes.
	 * @return The array of loaded strokes.
	 */
	public ArrayList<Stroke> getLoadedStrokes()
	{
		return loadedStrokes;
	}

	/**
	 * Gets the loaded stroke at the given index. Not to be confused with the stroke ID or chunk ID!
	 * @param index The index.
	 * @return The stroke at that index.
	 */
	public Stroke getLoadedStroke(int index)
	{
		try
		{
			return loadedStrokes.get(index);
		}
		catch (IndexOutOfBoundsException e)
		{
			return null;
		}
	}

	/**
	 * Returns the IDs of the currently loaded chunks in a list.
	 * @return A list of loaded chunk IDs.
	 */
	public long[] getLoadedChunkIds()
	{
		long[] ids = new long[loadedChunks.size()];
		for (int c = 0; c < ids.length; c++)
		{
			ids[c] = loadedChunks.keyAt(c);
		}
		return ids;
	}

	/**
	 * Returns the loaded chunk at the given index (NOT the chunk ID).
	 * @param index The index.
	 * @return The loaded chunk at the index.
	 * @see #getChunkFromId(long chunkId)
	 */
	private Project.Sheet.Chunk getChunk(int index)
	{
		return loadedChunks.valueAt(index);
	}

	/**
	 * Returns the loaded chunk with the given ID.
	 * @param chunkId The chunk ID.
	 * @return The loaded chunk with that ID.
	 * @see #getChunk(int index)
	 */
	public Project.Sheet.Chunk getChunkFromId(long chunkId)
	{
		return loadedChunks.get(chunkId);
	}

	/**
	 * Checks whether the chunk with the given ID is loaded.
	 * @param chunkId The chunk ID.
	 * @return True if the chunk with the given ID is loaded; false otherwise.
	 */
	private boolean isChunkLoaded(long chunkId)
	{
		return loadedChunks.indexOfKey(chunkId) > -1;
	}

	/**
	 * Adds a chunk to the array of loaded chunks.
	 * @param chunk The chunk to add.
	 * @see LoadChunkTask
	 */
	private void addChunk(Project.Sheet.Chunk chunk)
	{
		loadedChunks.put(chunk.getId(), chunk);
		int strokes = chunk.getTotalStrokes();
		for (int s = 0; s < strokes; s++)
		{
			addLoadedStroke(chunk.getStroke(s));
		}
	}

	/**
	 * Removes a chunk from the array of loaded chunks.
	 * @param chunkId The ID of the chunk to remove.
	 * @see SaveChunkTask
	 */
	private void removeChunk(long chunkId)
	{
		loadedChunks.remove(chunkId);
		removeLoadedStrokesWithChunkId(chunkId);
	}

	/**
	 * Sorts the loaded strokes in back-to-front drawing order.
	 */
	private void sortStrokes()
	{
		Collections.sort(loadedStrokes);
	}

	/**
	 * Loads the chunks on a given column between inclusive vertical bounds.
	 * @param x The column.
	 * @param minY The minimum vertical bound.
	 * @param maxY The maximum vertical bound.
	 * @param async Whether the task should be performed asynchronously.
	 */
	public void loadChunkColumn(int x, int minY, int maxY, boolean async)
	{
		int chunks = maxY - minY + 1;
		long[] chunksToLoad = new long[chunks];
		for (int c = 0; c < chunks; c++)
		{
			chunksToLoad[c] = ChunkUtils.pack(x, minY + c);
		}

		if (async)
		{
			LoadChunkTask lct = new LoadChunkTask(this, chunksToLoad);
			lct.execute();
		}
		else
		{
			loadChunks(chunksToLoad);
		}
	}

	/**
	 * Loads the chunks on a given row between inclusive horizontal bounds.
	 * @param y The row.
	 * @param minX The minimum horizontal bound.
	 * @param maxX The maximum horizontal bound.
	 * @param async Whether the task should be performed asynchronously.
	 */
	public void loadChunkRow(int y, int minX, int maxX, boolean async)
	{
		int chunks = maxX - minX + 1;
		long[] chunksToLoad = new long[chunks];
		for (int c = 0; c < chunks; c++)
		{
			chunksToLoad[c] = ChunkUtils.pack(minX + c, y);
		}

		if (async)
		{
			LoadChunkTask lct = new LoadChunkTask(this, chunksToLoad);
			lct.execute();
		}
		else
		{
			loadChunks(chunksToLoad);
		}
	}

	/**
	 * Saves, and optionally unloads, the chunks on a given column between inclusive vertical bounds.
	 * @param x The column.
	 * @param minY The minimum vertical bound.
	 * @param maxY The maximum vertical bound.
	 * @param unload Whether the chunks should be unloaded after saving.
	 * @param async Whether the task should be performed asynchronously.
	 */
	public void saveChunkColumn(int x, int minY, int maxY, boolean unload, boolean async)
	{
		int chunks = maxY - minY + 1;
		long[] chunksToSave = new long[chunks];
		for (int c = 0; c < chunks; c++)
		{
			chunksToSave[c] = ChunkUtils.pack(x, minY + c);
		}

		if (async)
		{
			SaveChunkTask sct = new SaveChunkTask(this, chunksToSave, unload);
			sct.execute();
		}
		else
		{
			saveChunks(chunksToSave, unload);
		}
	}

	/**
	 * Saves, and optionally unloads, the chunks on a given row between inclusive horizontal bounds.
	 * @param y The row.
	 * @param minX The minimum horizontal bound.
	 * @param maxX The maximum horizontal bound.
	 * @param unload Whether the chunks should be unloaded after saving.
	 * @param async Whether the task should be performed asynchronously.
	 */
	public void saveChunkRow(int y, int minX, int maxX, boolean unload, boolean async)
	{
		int chunks = maxX - minX + 1;
		long[] chunksToSave = new long[chunks];
		for (int c = 0; c < chunks; c++)
		{
			chunksToSave[c] = ChunkUtils.pack(minX + c, y);
		}

		if (async)
		{
			SaveChunkTask sct = new SaveChunkTask(this, chunksToSave, unload);
			sct.execute();
		}
		else
		{
			saveChunks(chunksToSave, unload);
		}
	}

	/**
	 * Saves, and optionally unloads, all of the chunks currently loaded.
	 * This should be called as part of saving the project to one file.
	 * @param unload Whether the chunks should be unloaded after saving.
	 * @param async Whether the task should be performed asynchronously.
	 */
	private void saveAllLoadedChunks(boolean unload, boolean async)
	{
		int chunks = loadedChunks.size();
		long[] chunksToSave = new long[chunks];
		for (int c = 0; c < chunks; c++)
		{
			chunksToSave[c] = loadedChunks.valueAt(c).getId();
		}

		if (async)
		{
			SaveChunkTask sct = new SaveChunkTask(this, chunksToSave, unload);
			sct.execute();
		}
		else
		{
			saveChunks(chunksToSave, unload);
		}
	}

	/**
	 * Saves a single chunk.
	 * A convenience method for {@link #saveChunks(long[], boolean)}.
	 * @param chunkId The ID of the chunk to save.
	 * @param unload Whether the chunk should be unloaded after saving.
	 */
	@SuppressWarnings("unused")
	public void saveChunk(long chunkId, boolean unload)
	{
		saveChunks(new long[]{chunkId}, unload);
	}

	/**
	 * Saves the chunks specified by the given chunk IDs, optionally unloading them.
	 * @param chunkIds The IDs of the chunks to save.
	 * @param unload Whether the chunks should be unloaded after saving.
	 * @see #saveChunk(long, boolean)
	 */
	public void saveChunks(long[] chunkIds, boolean unload)
	{
		int nonEmptyChunksProcessed = 0;
		int totalChunksProcessed = 0;
		int totalStrokes = 0;

		Project.Sheet.Chunk chunk;
		for (long chunkId : chunkIds)
		{
			if (!isChunkLoaded(chunkId)) continue;

			totalChunksProcessed++;

			chunk = getChunkFromId(chunkId);
			if (chunk.getTotalStrokes() == 0)
			{
				if (chunk.getFile().exists())
				{
					// If a file has existed for this chunk in the past, delete it.
					chunk.getFile().delete();
				}
				// Skip processing the empty chunk.
				continue;
			}

			nonEmptyChunksProcessed++;
			totalStrokes += chunk.getTotalStrokes();

			chunk = getChunkFromId(chunkId);
			ObjectOutputStream oos = null;
			try
			{
				FileOutputStream fos = new FileOutputStream(chunk.getFile());
				oos = new ObjectOutputStream(fos);
				oos.writeObject(chunk);
				oos.flush();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			finally
			{
				if (oos != null)
				{
					try
					{
						oos.close();
					}
					catch (IOException e)
					{
						// Disregard exception.
					}
				}
			}

			if (unload) removeChunk(chunkId);
		}
	}

	/**
	 * Loads the specified chunks.
	 * @param chunkIds The IDs of the chunks to load.
	 */
	public void loadChunks(long[] chunkIds)
	{
		int nonEmptyChunksProcessed = 0;
		int totalChunksProcessed = 0;
		int totalStrokes = 0;

		for (long chunkId : chunkIds)
		{
			if (isChunkLoaded(chunkId))
			{
				continue;
			}

			totalChunksProcessed++;

			File chunkFile = new File(getCurrentSheet().getFile(), Long.toString(chunkId));

			Project.Sheet.Chunk chunk = null;

			// Does a file exist for this chunk? Load it.
			if (chunkFile.exists())
			{
				nonEmptyChunksProcessed++;

				ObjectInputStream ois = null;
				try
				{
					FileInputStream fis = new FileInputStream(chunkFile);
					ois = new ObjectInputStream(fis);
					chunk = (Project.Sheet.Chunk) ois.readObject();
				}
				catch (IOException | ClassNotFoundException e)
				{
					e.printStackTrace();
				}
				finally
				{
					if (ois != null)
					{
						try
						{
							ois.close();
						}
						catch (IOException e)
						{
							// Disregard exception.
						}
					}
				}
			}
			// Otherwise, it must be an empty chunk. Make a new one.
			else
			{
				chunk = new Project.Sheet.Chunk(this, chunkId);
			}

			if (chunk == null)
			{
				chunk = new Project.Sheet.Chunk(this, chunkId);
			}

			chunk.setWorkingFile(this);
			addChunk(chunk);
			totalStrokes += chunk.getTotalStrokes();
		}

		sortStrokes();
	}

	/**
	 * Adds a stroke to the list of loaded strokes.
	 * @param stroke The stroke to add.
	 */
	public void addLoadedStroke(Stroke stroke)
	{
		loadedStrokes.add(stroke);
	}

	/**
	 * Removes a stroke from the list of loaded strokes.
	 * @param stroke The stroke to remove.
	 */
	@SuppressWarnings("unused")
	public void removeLoadedStroke(Stroke stroke)
	{
		loadedStrokes.remove(stroke);
	}

	/**
	 * Removes all strokes from the loaded strokes list that are contained in the chunk with the given ID.
	 * @param chunkId The chunk ID that contains the strokes.
	 */
	private void removeLoadedStrokesWithChunkId(long chunkId)
	{
		int strokes = loadedStrokes.size();
		for (int s = 0; s < strokes; /* do nothing */)
		{
			if (loadedStrokes.get(s).chunkId == chunkId)
			{
				loadedStrokes.remove(s);
				strokes--;
				continue;
			}
			s++;
		}
	}

	/**
	 * Loads the sketchbook details from the metadata file.
	 * @return True if the data could be read; false otherwise.
	 */
	public boolean loadMetadata()
	{
		ObjectInputStream ois = null;
		try
		{
			FileInputStream fis = new FileInputStream(getMetadataFile());
			ois = new ObjectInputStream(fis);
			setName((String) ois.readObject()); // Name of project
			totalSheets = ois.readInt(); // Total sheets
			currentSheet = ois.readInt(); // Current sheet
			strokeCount = ois.readLong(); // Stroke count
			sheetOrder = (long[]) ois.readObject(); // Order of sheets
			return true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
		finally
		{
			if (ois != null)
			{
				try
				{
					ois.close();
				}
				catch (IOException e)
				{
					// Disregard exception.
				}
			}
		}
	}

	/**
	 * Writes the sketchbook details to the metadata file.
	 * @return True if the data was written successfully; false otherwise.
	 */
	public boolean saveMetadata()
	{
		ObjectOutputStream oos = null;
		try
		{
			FileOutputStream fos = new FileOutputStream(getMetadataFile());
			oos = new ObjectOutputStream(fos);
			oos.writeObject(getName()); // Name of project
			oos.writeInt(getTotalSheets()); // Number of sheets
			oos.writeInt(getCurrentSheetIndex()); // Current sheet
			oos.writeLong(getStrokeCount()); // Stroke count
			oos.writeObject(generateSheetOrder()); // Order of sheets
			oos.flush();
			return true;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return false;
		}
		finally
		{
			if (oos != null)
			{
				try
				{
					oos.close();
				}
				catch (IOException e)
				{
					// Disregard exception.
				}
			}
		}
	}

	/**
	 * Works out the minimum metadata necessary without using the metadata file.
	 * Called during crash recovery.
	 */
	public void determineMetadataWithoutFile()
	{
		Object[] sheetDirs = FileUtils.listFilesAndDirs(IOUtils.getWorkingDirectory(), TrueFileFilter.INSTANCE, null).toArray();
		totalSheets = sheetDirs.length;
		sheetOrder = new long[totalSheets];
		for (int s = 0; s < sheetDirs.length; s++)
		{
			try
			{
				sheetOrder[s] = Long.parseLong(((File) sheetDirs[s]).getName());
			}
			catch (NumberFormatException e)
			{
				totalSheets--;
			}
		}
		strokeCount = Prefs.getLong(R.string.pref_stroke_count);
	}

	/**
	 * Saves the sketchbook thumbnail.
	 * @param bitmap The thumbnail bitmap.
	 */
	public void saveThumbnail(Bitmap bitmap)
	{
		setThumbnailFile();
		FileOutputStream fos = null;

		try
		{
			fos = new FileOutputStream(getThumbnailFile());
			boolean success = bitmap.compress(Bitmap.CompressFormat.PNG, 0, fos);
			fos.flush();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (fos != null)
			{
				try
				{
					fos.close();
				}
				catch (IOException e)
				{
					// Disregard exception.
				}
			}
		}
	}

	/**
	 * Loads the sketchbook from its archive file.
	 * @return True if the project was loaded successfully; false otherwise.
	 */
	public boolean load(Resources res)
	{
		IOUtils.deleteDirectory(IOUtils.getWorkingDirectory(), false);
		try
		{
			extract();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return false;
		}
		loadMetadata();
		for (long sheetId : sheetOrder)
		{
			loadSheet(res, sheetId);
		}
		setUnsavedChanges(false);
		return true;
	}

	/**
	 * Saves the project as an archive file.
	 * @param unload Whether the currently loaded chunks should be unloaded after saving.
	 *               This should be true if the user is closing the sketchbook or the app.
	 * @return True if the project was saved successfully; false otherwise.
	 */
	public boolean save(boolean unload)
	{
		saveMetadata();
		saveAllLoadedChunks(unload, false);
		for (Sheet sheet : sheets)
		{
			sheet.saveMetadata();
		}
		try
		{
			archive();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return false;
		}
		setUnsavedChanges(false);

		return true;
	}

	/**
	 * Archives the sketchbook as a gzipped tarball.
	 * Called from {@link #save(boolean)}.
	 * @throws IOException
	 */
	private void archive() throws IOException
	{
		IOUtils.archive(getName(), IOUtils.getDirectory(), IOUtils.getWorkingDirectory());
	}

	/**
	 * Extracts the sketchbook from its gzipped tarball.
	 * Called from {@link #load(Resources)}.
	 * @throws IOException
	 */
	private void extract() throws IOException
	{
		IOUtils.extract(getArchiveFile(), IOUtils.getWorkingDirectory());
	}

	/**
	 * Generates a list of sheet IDs that reflects the project's sheet ordering.
	 * @return A list of long sheet IDs.
	 */
	private long[] generateSheetOrder()
	{
		long[] sheetIds = new long[getTotalSheets()];
		for (int s = 0; s < sheetIds.length; s++)
		{
			sheetIds[s] = getSheetAt(s).getId();
		}
		return sheetIds;
	}

	/**
	 * Gets the number of strokes in the sketchbook.
	 * Beware that this number may be greater than the actual number of strokes, and is
	 * merely used to ensure that all stroke IDs are unique and that all newer strokes have
	 * greater IDs than older ones.
	 * @return A value greater than or equal to the number of strokes in the sketchbook.
	 */
	public long getStrokeCount()
	{
		return strokeCount;
	}

	/**
	 * Increments the stroke count by 1.
	 */
	public void incrementStrokeCount()
	{
		strokeCount++;
		setUnsavedChanges(true);
		Prefs.edit().putLong(R.string.pref_stroke_count, strokeCount).apply();
	}

	/**
	 * Checks whether the sketchbook has unsaved changes.
	 * @return True if the sketchbook has unsaved changes; false otherwise.
	 */
	public boolean hasUnsavedChanges()
	{
		return unsavedChanges;
	}

	/**
	 * Sets the flag indicating the presence of unsaved changes.
	 * @param unsavedChanges Whether unsaved changes should be present.
	 * @see #hasUnsavedChanges()
	 */
	public void setUnsavedChanges(boolean unsavedChanges)
	{
		this.unsavedChanges = unsavedChanges;
	}

	/**
	 * Loads a sheet with the given ID from its file in the working directory and adds it to the sketchbook.
	 * @param res A <code>Resources</code> instance.
	 * @param sheetId The ID of the sheet to load.
	 */
	public void loadSheet(Resources res, long sheetId)
	{
		Sheet sheet = new Sheet(sheetId, res);
		sheet.setWorkingFile();
		sheet.loadMetadata();
		sheet.loadThumbnail(res);
		addLoadedSheet(sheet);
	}

	/**
	 * A sheet consisting of {@link Project.Sheet.Chunk chunks}.
	 */
	public static class Sheet
	{
		/** The default sheet name. */
		private static final String DEFAULT_NAME = "Sheet";

		/** A unique ID for the sheet. */
		private long id;
		/** The name of the sheet. */
		private String name = DEFAULT_NAME;
		/** The latest known camera X position when viewing this sheet. */
		private float cameraX = 0f;
		/** The latest known camera Y position when viewing this sheet. */
		private float cameraY = 0f;
		/** The latest known camera zoom factor when viewing this sheet. */
		private float cameraZoom = 1f;
		/** The sheet's file in the working directory. */
		private File file;
		/** The sheet's thumbnail. */
		private BitmapDrawable thumbnail;

		/**
		 * Creates a sheet with the given ID.
		 * Its name is set to the default name according to the locale.
		 * @param id The ID of the new sheet.
		 * @param res A <code>Resources</code> object.
		 */
		public Sheet(long id, Resources res)
		{
			this.id = id;
			this.name = Sheet.getDefaultName(res);
		}

		/**
		 * Creates a sheet with the given name.
		 * Its ID is taken from the current system time.
		 * @param name The sheet name.
		 */
		public Sheet(String name)
		{
			id = System.currentTimeMillis();
			setName(name);
			setWorkingFile();
			getFile().mkdir();
			saveMetadata();
		}

		/**
		 * Reads the associated sheet folder in the temporary directory and adds the sheet to the project.
		 * @param sheetId The sheet ID.
		 */
		public void load(Project project, Resources res, long sheetId)
		{
			id = sheetId;
			setWorkingFile();
			loadMetadata();
			loadThumbnail(res);
			project.addLoadedSheet(this);
		}

		public long getId()
		{
			return id;
		}

		public String getName()
		{
			return name;
		}

		public void setName(String name)
		{
			this.name = name;
		}

		public float getCameraX()
		{
			return cameraX;
		}

		public void setCameraX(float cameraX)
		{
			this.cameraX = cameraX;
		}

		public float getCameraY()
		{
			return cameraY;
		}

		public void setCameraY(float cameraY)
		{
			this.cameraY = cameraY;
		}

		public void setCameraPosition(float x, float y)
		{
			cameraX = x;
			cameraY = y;
		}

		public float getCameraZoom()
		{
			return cameraZoom;
		}

		public void setCameraZoom(float cameraZoom)
		{
			this.cameraZoom = cameraZoom;
		}

		public void setCamera(float x, float y, float zoom)
		{
			cameraX = x;
			cameraY = y;
			cameraZoom = zoom;
		}

		public File getFile()
		{
			return file;
		}

		public void setFile(File file)
		{
			this.file = file;
		}

		public void setWorkingFile()
		{
			setFile(new File(IOUtils.getWorkingDirectory(), Long.toString(id)));
		}

		public File getMetadataFile()
		{
			return new File(getFile(), METADATA);
		}

		public BitmapDrawable getThumbnail()
		{
			return thumbnail;
		}

		public void setThumbnail(BitmapDrawable thumbnail)
		{
			this.thumbnail = thumbnail;
		}

		public void saveMetadata()
		{
			ObjectOutputStream oos = null;
			try
			{
				FileOutputStream fos = new FileOutputStream(getMetadataFile());
				oos = new ObjectOutputStream(fos);
				oos.writeObject(getName()); // Name of sheet
				oos.writeFloat(getCameraX()); oos.writeFloat(getCameraY()); // Camera position
				oos.writeFloat(getCameraZoom()); // Camera zoom
				oos.flush();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			finally
			{
				if (oos != null)
				{
					try
					{
						oos.close();
					}
					catch (IOException e)
					{
						// Disregard exception.
					}
				}
			}
		}

		public void loadMetadata()
		{
			ObjectInputStream ois = null;
			try
			{
				FileInputStream fis = new FileInputStream(getMetadataFile());
				ois = new ObjectInputStream(fis);
				setName((String) ois.readObject()); // Name of sheet
				setCamera(ois.readFloat(), ois.readFloat(), ois.readFloat()); // Camera position and zoom
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				if (ois != null)
				{
					try
					{
						ois.close();
					}
					catch (IOException e)
					{
						// Disregard exception.
					}
				}
			}
		}

		/**
		 * Saves a thumbnail to the sheet folder.
		 * @param bitmap The thumbnail bitmap.
		 */
		public void saveThumbnail(Bitmap bitmap)
		{
			FileOutputStream fos = null;

			try
			{
				fos = new FileOutputStream(getThumbnailFile());
				bitmap.compress(Bitmap.CompressFormat.PNG, 0, fos);
				fos.flush();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			finally
			{
				if (fos != null)
				{
					try
					{
						fos.close();
					}
					catch (IOException e)
					{
						// Disregard exception.
					}
				}
			}
		}

		/**
		 * Loads the thumbnail from the sheet folder.
		 */
		public void loadThumbnail(Resources res)
		{
			BufferedInputStream bis = null;
			try
			{
				FileInputStream fis = new FileInputStream(getThumbnailFile());
				bis = new BufferedInputStream(fis);
				Bitmap thumbnailBitmap = BitmapFactory.decodeStream(bis);
				setThumbnail(new BitmapDrawable(res, thumbnailBitmap));
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				if (bis != null)
				{
					try
					{
						bis.close();
					}
					catch (IOException e)
					{
						// Disregard exception.
					}
				}
			}
		}

		public File getThumbnailFile()
		{
			return new File(getFile(), THUMBNAIL_FILE);
		}

		public static String getDefaultName(Resources res)
		{
			return res.getString(R.string.term_sheet);
		}

		public static class Chunk implements Externalizable
		{
			private long id;
			private File file;
			private ExternalizableArrayList<Stroke> strokes = new ExternalizableArrayList<>();

			/**
			 * Used for serialisation only.
			 */
			public Chunk() {}

			public Chunk(Project project, int x, int y)
			{
				this(project, ChunkUtils.convertCoordsToChunk(x, y));
			}

			public Chunk(Project project, long id)
			{
				setId(id);
				setWorkingFile(project);
			}

			public long getId()
			{
				return id;
			}

			/**
			 * <b>This method should only be called by a constructor or by readExternal()!</b>
			 * Calling this method on any other occasion will destabilise the project's file structure.
			 * @param id The chunk ID.
			 */
			private void setId(long id)
			{
				this.id = id;
			}

			public File getFile()
			{
				return file;
			}

			public void setFile(File file)
			{
				this.file = file;
			}

			public void setWorkingFile(Project project)
			{
				setFile(new File(project.getCurrentSheet().getFile(), Long.toString(id)));
			}

			public Iterator<Stroke> getStrokeIterator()
			{
				return strokes.iterator();
			}

			public ExternalizableArrayList<Stroke> getStrokes()
			{
				return strokes;
			}

			public void setStrokes(ExternalizableArrayList<Stroke> strokes)
			{
				this.strokes = strokes;
			}

			public int getTotalStrokes()
			{
				return strokes.size();
			}

			/**
			 * Adds a stroke to this chunk.
			 * @param stroke The stroke to add.
			 */
			public void addStroke(@NonNull Stroke stroke)
			{
				strokes.add(stroke);
			}

			/**
			 * Gets the stroke at the given index.
			 * Don't confuse this with the stroke ID!
			 * @param index The index.
			 * @return The stroke at that index.
			 */
			public Stroke getStroke(int index)
			{
				return strokes.get(index);
			}

			public boolean isChunkEmpty()
			{
				return strokes.size() == 0;
			}

			@Override
			@SuppressWarnings("unchecked")
			public void readExternal(ObjectInput input) throws IOException, ClassNotFoundException
			{
				setId(input.readLong()); // Chunk ID
				setStrokes((ExternalizableArrayList<Stroke>) input.readObject()); // Strokes array
			}

			@Override
			public void writeExternal(ObjectOutput output) throws IOException
			{
				output.writeLong(id); // Chunk ID
				output.writeObject(strokes); // Stroke array
			}
		}
	}
}
