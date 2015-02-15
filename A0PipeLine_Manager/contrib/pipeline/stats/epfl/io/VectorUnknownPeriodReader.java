package pipeline.stats.epfl.io;

import java.io.IOException;
import java.util.Vector;

/**
 * A double vector periodic reader.
 * The period is fixed by the maximum readable size in underlying stream.
 *
 * @author Camille Weber <camille.weber@epfl.ch>
 */
public class VectorUnknownPeriodReader implements VectorReader {

	private VectorReader in;
	private final int period;
	private final boolean makePeriodicCopy;
	Vector<double[]> vectors;
	private int nextVector;

	/**
	 * Creates a new periodic reader
	 *
	 * @param in
	 *            the underlying vector reader
	 * @param makePeriodicCopy
	 *            flag saying if on each period a new vector is built
	 */
	public VectorUnknownPeriodReader(VectorReader in, boolean makePeriodicCopy) {
		this.in = in;
		this.makePeriodicCopy = makePeriodicCopy;
		nextVector = 0;
		vectors = new Vector<>();

		int searchPeriod = 0;
		try {
			if (in.period() > 0) {
				for (searchPeriod = 0; searchPeriod < in.period(); searchPeriod++) {
					vectors.add(in.read());
				}
			} else {
				while (true) {
					vectors.add(in.read());
					searchPeriod++;
				}
			}
		} catch (IOException e) {
		}
		period = searchPeriod;
	}

	/**
	 * Creates a new periodic reader. Each period a new vector is built
	 *
	 * @param in
	 *            the underlying vector reader
	 */
	public VectorUnknownPeriodReader(VectorReader in) {
		this(in, true);
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
		double[] v;

		if (makePeriodicCopy) {
			v = (vectors.get(nextVector)).clone();
		} else {
			v = vectors.get(nextVector);
		}
		nextVector = (nextVector + 1) % period;
		return v;
	}

	/** Returns the stream size (vector size) */
	@Override
	public int size() {
		return in.size();
	}

	/** Returns the stream period */
	@Override
	public int period() {
		return period;
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
			vectors = null;
			in.close();
			in = null;
		}
	}
}
