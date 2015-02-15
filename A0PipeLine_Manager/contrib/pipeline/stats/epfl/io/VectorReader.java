package pipeline.stats.epfl.io;

import java.io.IOException;

/**
 * A double vector input stream
 *
 * @author Camille Weber <camille.weber@epfl.ch>
 */
public interface VectorReader {

	/**
	 * Reads a vector in stream
	 *
	 * @exception IOException
	 *                if the stream is closed or if it is unable to read
	 *                size doubles
	 */
	public double[] read() throws IOException;

	/** Returns the stream size (vector size) */
	public int size();

	/**
	 * Returns the stream period
	 *
	 * @return 0 if the stream is not periodic
	 */
	public int period();

	/**
	 * Closes the stream
	 *
	 * @exception IOException
	 *                an IO error occurs
	 */
	public void close() throws IOException;
}
