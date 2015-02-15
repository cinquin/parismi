package pipeline.stats.epfl.classifier;

import java.io.IOException;

import pipeline.stats.epfl.io.VectorReader;

/**
 * A double vector class size adapter.
 * For each read vector, the class distribution is stored. <br>
 * A vector read has two parts : <br>
 * - size components which are standard components; <br>
 * - the nClasses last components which are the class distribution. <br>
 * This distribution is stored in the currentClassDist. <br>
 * v(size components..., p(class 0), p(class 1), ..., p(class nClasses-1))
 *
 * @author Camille Weber <camille.weber@epfl.ch>
 */
public class VectorClassSizeAdapter2 implements VectorReader {

	private final int nClasses;
	private final int size;
	private double[] currentClassDist;
	private VectorReader in;

	/**
	 * Creates a new size adapter
	 *
	 * @param in
	 *            the underlying vector reader
	 * @param nClasses
	 *            the number of classes
	 *            The stream size is in.size() - nClasses
	 */
	public VectorClassSizeAdapter2(VectorReader in, int nClasses) {
		if (nClasses <= 0)
			throw new IllegalArgumentException("nClasses must be positive");
		this.nClasses = nClasses;
		this.size = in.size() - nClasses;
		this.in = in;
		currentClassDist = new double[nClasses];
		for (int i = 0; i < nClasses; i++) {
			currentClassDist[i] = 0.0;
		}
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

		if (in == null)
			throw new IOException("stream is closed");
		d1 = in.read();
		d2 = new double[size];
		for (int i = 0; i < size; i++) {
			d2[i] = d1[i];
		}
		for (int i = 0; i < nClasses; i++) {
			currentClassDist[i] = d1[size + i];
		}
		return d2;
	}

	/** Returns the number of classes */
	public int nClasses() {
		return nClasses;
	}

	/** Returns the current class distribution */
	public double[] currentClassDist() {
		return currentClassDist;
	}

	/** Returns the stream size (vector size) */
	@Override
	public int size() {
		return size;
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
