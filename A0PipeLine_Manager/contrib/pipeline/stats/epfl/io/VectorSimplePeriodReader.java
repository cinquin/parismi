package pipeline.stats.epfl.io;

import java.io.IOException;

/**
 * A double vector periodic reader.
 * The period is known and specified in constructor
 *
 * @author Camille Weber <camille.weber@epfl.ch>
 */
public class VectorSimplePeriodReader implements VectorReader {

	private VectorReader in;
	private final int period;
	private final boolean makePeriodicCopy;
	private double[][] vectors;
	private int nextVector;

	/**
	 * Creates a new periodic reader with the given period
	 *
	 * @param in
	 *            the underlying vector reader
	 * @param period
	 *            the reader period
	 * @param makePeriodicCopy
	 *            flag saying if on each period a new vector is built
	 * @exception IOException
	 *                if it can not read period vectors
	 */
	public VectorSimplePeriodReader(VectorReader in, int period, boolean makePeriodicCopy) throws IOException {
		this.in = in;
		this.period = period;
		this.makePeriodicCopy = makePeriodicCopy;
		nextVector = 0;
		vectors = new double[period][];

		for (int i = 0; i < period; i++) {
			vectors[i] = in.read();
		}
	}

	/**
	 * Creates a new periodic reader with the given period. Each period a new vector is built
	 *
	 * @param in
	 *            the underlying vector reader
	 * @param period
	 *            the reader period
	 * @exception IOException
	 *                if it can not read period vectors
	 */
	public VectorSimplePeriodReader(VectorReader in, int period) throws IOException {
		this(in, period, true);
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
			v = vectors[nextVector].clone();
		} else {
			v = vectors[nextVector];
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
