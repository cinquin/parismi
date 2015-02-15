package pipeline.stats.epfl.io;

import java.io.IOException;

/**
 * A double vector manual periodic reader.
 * Period vectors can be read and then the reader must be reset.
 *
 * @author Camille Weber <camille.weber@epfl.ch>
 */
public class VectorManualPeriodReader extends VectorUnknownPeriodReader {

	private int nRead;

	/**
	 * Creates a new periodic reader
	 *
	 * @param in
	 *            the underlying vector reader
	 * @param makePeriodicCopy
	 *            flag saying if on each period a new vector is built
	 */
	public VectorManualPeriodReader(VectorReader in, boolean makePeriodicCopy) {
		super(in, makePeriodicCopy);
		nRead = 0;
	}

	/**
	 * Creates a new periodic reader. Each period a new vector is built
	 *
	 * @param in
	 *            the underlying vector reader
	 */
	VectorManualPeriodReader(VectorReader in) {
		this(in, true);
	}

	/**
	 * Reads a vector in stream
	 *
	 * @exception IOException
	 *                if period vectors have been read or
	 *                the stream is closed or
	 *                it is unable to read size doubles
	 */
	@Override
	public double[] read() throws IOException {
		double[] v;

		if (nRead >= period())
			throw new IOException("period vectors have been read, the reader must be reset");
		v = super.read();
		nRead++;
		return v;
	}

	/** Resets the reader for a new run (period vectors can be read) */
	public void reset() {
		nRead = 0;
	}

}
