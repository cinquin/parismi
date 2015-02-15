package pipeline.stats.epfl.pdf;

import java.io.IOException;

import pipeline.stats.epfl.io.VectorReader;

/**
 * Normalizes a set of vector
 *
 * @author Camille Weber <camille.weber@epfl.ch>
 */
public class VectorNormalize implements VectorReader {

	private VectorReader in;
	private final double[] mean;
	private final double[] stdDev;

	/**
	 * Creates a new normalizer on the underlying vector reader
	 *
	 * @param in
	 *            the underlying vector reader
	 * @param mean
	 *            the mean
	 * @param stdDev
	 *            the standard deviation
	 */
	public VectorNormalize(VectorReader in, double[] mean, double[] stdDev) {
		this.in = in;
		this.mean = mean;
		this.stdDev = stdDev;
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
		int n1 = Math.min(size(), mean.length);
		int n2 = size();
		double[] v1 = in.read();
		double[] v2 = new double[n2];

		for (int i = 0; i < n1; i++) {
			v2[i] = (v1[i] - mean[i]) / stdDev[i];
		}
		for (int i = n1; i < n2; i++) {
			v2[i] = v1[i];
		}
		return v2;
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
	 *                an IO error occurs
	 */
	@Override
	public void close() throws IOException {
		if (in != null) {
			in.close();
			in = null;
		}
	}
}
