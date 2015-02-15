package pipeline.stats.epfl.io;

import java.io.IOException;

/**
 * A stream which replaces coded values with a specific value
 *
 * @author Camille Weber <camille.weber@epfl.ch>
 */
public class VectorReplace implements VectorReader {

	private VectorReader in;
	private double[][] replacement;

	/**
	 * Creates a new replacer
	 *
	 * @param in
	 *            the underlying vector reader
	 * @param replacement
	 *            the replacement code book <br>
	 *            replacement[i][0] : the coded value <br>
	 *            replacement[i][1] : the replacement value <br>
	 *            i in range 0..size-1
	 */
	public VectorReplace(VectorReader in, double[][] replacement) {
		this.in = in;
		this.replacement = replacement;
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
		double[] v1 = in.read();
		double[] v2 = new double[v1.length];
		int n = replacement.length;

		for (int i = 0; i < n; i++) {
			if (v1[i] == replacement[i][0]) {
				v2[i] = replacement[i][1];
			} else {
				v2[i] = v1[i];
			}
		}
		for (int i = n; i < v1.length; i++) {
			v2[i] = v1[i];
		}
		return v2;
	}

	/** Returns the stream size (vector size) */
	@Override
	public int size() {
		return in.size();
	}

	/**
	 * Returns the stream period
	 *
	 * @return 0 if the stream is not periodic
	 */
	@Override
	public int period() {
		return in.period();
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
