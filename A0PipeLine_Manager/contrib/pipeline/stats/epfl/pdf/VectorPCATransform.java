package pipeline.stats.epfl.pdf;

import java.io.IOException;

import pipeline.stats.epfl.io.VectorReader;

/**
 * A Principal component analysis transformation on a vector reader
 *
 * @author Camille Weber <camille.weber@epfl.ch>
 */
public class VectorPCATransform implements VectorReader {

	private VectorReader in;
	private final int nComponent;
	private double[][] eigenvectors;
	private double[] eigenvalues;

	/**
	 * Creates a new PCA transformation for the underlying vector reader
	 *
	 * @param in
	 *            the underlying vector reader
	 * @param nComponent
	 *            the number of principal components.
	 * @param eigenvectors
	 *            the eigen vectors
	 * @param eigenvalues
	 *            the eigen values associated
	 */
	public VectorPCATransform(VectorReader in, int nComponent, double[][] eigenvectors, double[] eigenvalues) {
		this.in = in;
		if (nComponent < 0)
			throw new IllegalArgumentException("the number of principal components must be positive");
		this.nComponent = Math.min(nComponent, eigenvectors.length);
		this.eigenvectors = eigenvectors;
		this.eigenvalues = eigenvalues;
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
		int n = size();
		double[] v2 = new double[n];

		for (int i = 0; i < nComponent; i++) {
			v2[i] = 0.0;
			double length = 0.0;
			for (int j = 0; j < eigenvectors[i].length; j++) {
				v2[i] += eigenvectors[i][j] * v1[j];
				length += eigenvectors[i][j] * eigenvectors[i][j];
			}
			v2[i] /= length;
			v2[i] *= Math.sqrt(eigenvalues[i]);
		}
		for (int i = nComponent; i < n; i++) {
			v2[i] = v1[eigenvectors[0].length - nComponent + i];
		}

		return v2;
	}

	/** Returns the stream size (vector size) */
	@Override
	public int size() {
		int s = Math.max(in.size() - eigenvectors[0].length, 0);
		return nComponent + s;
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
