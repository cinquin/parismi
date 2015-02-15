package pipeline.stats.epfl.io;

import java.io.IOException;

/**
 * A double vector output stream
 *
 * @author Camille Weber <camille.weber@epfl.ch>
 */
public interface VectorWriter {

	/**
	 * Writes a vector in stream
	 *
	 * @param v
	 *            vector to write; the vector is truncated if necessary,
	 *            or completed with 0.0
	 * @exception IOException
	 *                if an IO error occurs
	 */
	public void write(double[] v) throws IOException;

	/** Returns the stream size (vector size) */
	public int size();

	/**
	 * Flushes the stream
	 *
	 * @exception IOException
	 *                if an IO error occurs
	 */
	public void flush() throws IOException;

	/**
	 * Closes the stream
	 *
	 * @exception IOException
	 *                if an IO error occurs
	 */
	public void close() throws IOException;
}
