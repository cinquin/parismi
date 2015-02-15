/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.data;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import pipeline.data.PluginIOImage.PixelType;
import pipeline.misc_util.FormatException;
import pipeline.misc_util.SettableBoolean;

/**
 * Allows for transparent reading and writing of image pixels, irrespective of where the image is actually stored (in
 * RAM,
 * on disk, partially cached in RAM, handled by another process, etc.) Includes interfaces to manipulate data caching.
 * An instance of this class represents pixels that are stored together. Different instances can be pieced together in
 * a {@link PluginIOImage} object.
 *
 */
public interface ImageAccessor {

	/**
	 * Writes the contents of buffer as the next slice of an image that has already been opened for sequential writing.
	 * The number of slices written that way cannot exceed the dimensions that must be declared at file opening.
	 * 
	 * @param buffer
	 *            For the resulting image to be stored correctly, if the underlying data is float must be the backing
	 *            buffer for a little-endian float buffer
	 * @param sliceIndex
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void dumpLittleEndianFloatBufferIntoSlice(ByteBuffer buffer, int sliceIndex) throws IOException,
			InterruptedException;

	/**
	 * Writes the contents of buffer as the next slice of an image that has already been opened for sequential writing.
	 * The number of slices written that way cannot exceed the dimensions that must be declared at file opening.
	 * 
	 * @param buffer
	 *            For the resulting image to be stored correctly, must be the backing buffer for a little-endian float
	 *            buffer
	 * @throws IOException
	 * 
	 *             public void dumpLittleEndianFloatBufferIntoNextSlice(ByteBuffer buffer) throws IOException;
	 */
	/**
	 * If possible, make slice sliceIndex in the image point to the pixel array in "pixels". In this case, further
	 * modifications
	 * made to the "pixels" Object after this call may affect the image (although this is not guaranteed). If it is not
	 * possible to just store
	 * a reference to the pixels, a copy of the pixels will be made.
	 * 
	 * @param pixels
	 * @param sliceIndex
	 * @param cachePolicy
	 * @param justCopied
	 *            Upon return, set to true if it was not possible to keep a reference and the pixels were just copied
	 * @throws IOException
	 * @throws FormatException
	 * @throws InterruptedException
	 */
	public void assignPixelsToZSlice(Object pixels, int sliceIndex, int cachePolicy, SettableBoolean justCopied)
			throws FormatException, IOException, InterruptedException;

	public void copyPixelsIntoZSlice(Object pixels, int sliceIndex, int cachePolicy) throws FormatException,
			IOException, InterruptedException;

	/**
	 * If possible, a reference to the requested pixels will be returned without copying. If it is not possible to
	 * return an internal reference to the pixels, a copy of the pixels will be returned. A reference to that copy is
	 * retained,
	 * and changes can be saved by calling {@link #save}.
	 * 
	 * @param sliceIndex
	 *            Indexing begins at 0
	 * @param cachePolicy
	 * @param willModifyPixels
	 *            True if the caller reserves the right to modify the pixels it is passed; changes might need explicit
	 *            saving.
	 * @param changesNeedExplicitSaving
	 *            Upon return, set to true if it was not possible to get a direct reference to the pixels and
	 *            a copy was returned.
	 * @return A 1-dimensional array of pixels; type depends on pixel depth
	 * @throws IOException
	 */
	public Object getReferenceToPixelZSlice(int sliceIndex, int cachePolicy, boolean willModifyPixels,
			SettableBoolean changesNeedExplicitSaving) throws IOException;

	public Object getPixelZSliceCopy(int sliceIndex, int cachePolicy) throws IOException;

	public void openForSequentialRead() throws IOException, FormatException;

	public void openForSequentialWrite() throws IOException;

	public void close() throws IOException;

	public InputOutputObjectDimensions getDimensions();

	/**
	 * Frees all memory used for caching. Discards any cached changes to the image.
	 */
	public void clearCache();

	/**
	 * Writes out any changes to the original image.
	 */
	public void save() throws IOException;

	/**
	 * Used by the pipeline to balance moemory usage.
	 * 
	 * @param size
	 */
	public void setMaximumCacheSize(long size);

	/**
	 * Used by the pipeline under low memory conditions to free some memory.
	 */
	public void cutDownCacheSizeTo(long size);

	/**
	 * Used by the pipeline to figure out how memory is used.
	 */
	public long getCurrentCacheSize();

	/**
	 * Used by the pipeline to figure out how memory is used.
	 */
	public long getLastTimeCacheWasUsed();

	/**
	 * Cache policies
	 */
	public static final int NO_CACHE_PREFERENCE = 0;
	public static final int TRY_TO_CACHE_PIXELS = 1;
	public static final int TRY_HARD_TO_CACHE_PIXELS = 2;
	public static final int DONT_CACHE_PIXELS = 3;

	/**
	 * Set the cache policy for calls that do not explicitly specify one (e.g. calls made through the PluginIOStack
	 * interface).
	 * 
	 * @param policy
	 */
	public void setDefaultCachePolicy(int policy);

	/**
	 * @return cache policy for calls that do not explicitly specify one (e.g. calls made through the PluginIOStack
	 *         interface).
	 */
	public int getDefaultCachePolicy();

	/**
	 * Declares the dimensions of a file before it is written (which at this point is a requirement to write TIFFs to
	 * disk).
	 * 
	 * @param x
	 * @param y
	 * @param z
	 * @param c
	 * @param t
	 */
	void setDimensions(int x, int y, int z, int c, int t);

	/**
	 * File that the ImageAccessor uses for backing to disk, if it exists.
	 * 
	 * @return null if there is no such file
	 */
	public File getBackingFile();

	/**
	 * Original file on disk this accessor is derived from. If the file was compressed
	 * and decompressed to a temporary location, this returns the reference to the original, compressed file.
	 * 
	 * @return Original, possibly-compressed file
	 */
	public File getOriginalSourceFile();

	/**
	 * 
	 * @param f
	 *            Original file on disk this accessor is derived from. If the file was compressed
	 *            and decompressed to a temporary location, this returns the reference to the original, compressed file.
	 */
	public void setOriginalSourceFile(File f);

	/**
	 * @return The image's pixel type (default is currently float).
	 */
	public PixelType getPixelType();

	/**
	 * Used to close image file that is being written to before all declared components (e.g. slices in a
	 * stack) have been written. The file is expected to be adjusted to still be readable despite the
	 * partial data.
	 * 
	 * @throws IOException
	 */
	void closeFileEarly() throws IOException;

	public SliceAccessor getSlicesAccessor() throws IOException;

}
