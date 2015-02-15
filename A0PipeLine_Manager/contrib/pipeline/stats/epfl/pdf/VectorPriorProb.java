package pipeline.stats.epfl.pdf;

import java.io.IOException;

import pipeline.stats.epfl.io.VectorReader;

/**
 * A vector reader with prior probabilities.
 * Each vector read in the underlying stream has an associated
 * probability which is stored in priorProb.
 * It uses the last vector component as a probability : <br>
 * in.read() -> v(size components ..., prior probability) <br>
 * and returns a new vector whose size is one less.
 *
 * @author Camille Weber <camille.weber@epfl.ch>
 */
public class VectorPriorProb implements VectorReader {

	private VectorReader in;
	private double priorProb;

	/**
	 * Creates a new vector reader.
	 *
	 * @param in
	 *            the underlying vector reader <br>
	 *            The stream size is equal to in.size() - 1 <br>
	 *            The prior probability is always read in last component.
	 */
	public VectorPriorProb(VectorReader in) {
		this.in = in;
		priorProb = 0.0;
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
		double[] d1, d2;
		int n;

		if (in == null)
			throw new IOException("stream is closed");
		d1 = in.read();
		n = in.size() - 1;
		d2 = new double[n];

		for (int i = 0; i < n; i++) {
			d2[i] = d1[i];
		}
		priorProb = d1[n];

		return d2;
	}

	/** Returns the prior probability from the current read vector */
	public double priorProb() {
		return priorProb;
	}

	/** Returns the stream size (vector size) */
	@Override
	public int size() {
		if (in != null) {
			return in.size() - 1;
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
