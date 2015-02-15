package pipeline.stats.epfl.io;

import java.io.IOException;

/**
 * An object which looks at all what is passing on the underlying stream.
 *
 * @author Camille Weber <camille.weber@epfl.ch>
 */
public class VectorSniffer implements VectorReader {

	private VectorReader in;
	private double[] lastVector;

	/**
	 * Creates a new sniffer
	 *
	 * @param in
	 *            the underlying vector reader
	 */
	public VectorSniffer(VectorReader in) {
		this.in = in;
		lastVector = null;
	}

	/** Returns the last vector read */
	public double[] lastVector() {
		return this.lastVector;
	}

	/**
	 * Reads a vector in stream
	 *
	 * @exception IOException
	 *                if the stream is closed or if it is unable to read
	 *                size doubles
	 */
	@Override
	public double[] read() throws IOException {
		if (in == null)
			throw new IOException("stream is closed");
		lastVector = in.read();
		return lastVector;
	}

	/** Returns the stream size (vector size) */
	@Override
	public int size() {
		if (in != null) {
			return in.size();
		} else {
			return 0;
		}
	}

	/**
	 * Returns the stream period
	 *
	 * @return 0 if the stream is not periodic
	 */
	@Override
	public int period() {
		if (in != null) {
			return in.period();
		} else {
			return 0;
		}
	}

	/**
	 * Closes the stream
	 *
	 * @exception IOException
	 *                if an IO error occurs
	 */
	@Override
	public void close() throws IOException {
		if (in != null) {
			in.close();
			in = null;
		}
	}
}
