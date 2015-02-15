package pipeline.stats.epfl.classifier;

import java.io.IOException;

import pipeline.stats.epfl.io.VectorReader;

/**
 * A double vector class size adapter.
 * For each read vector, the class index is decoded and put in currentClassIndex.
 * Subclasses must implememt the classify method.
 *
 * @author Camille Weber <camille.weber@epfl.ch>
 */
public abstract class VectorClassSizeAdapter implements VectorReader {

	private final int nClasses;
	protected final int size;
	private int currentClass;
	private VectorReader in;

	/**
	 * Creates a new size adapter
	 *
	 * @param in
	 *            the underlying vector reader
	 * @param nClasses
	 *            the number of classes <br>
	 *            The stream size is in.size() - 1
	 */
	protected VectorClassSizeAdapter(VectorReader in, int nClasses) {
		if (nClasses <= 0)
			throw new IllegalArgumentException("nClasses must be positive");
		this.nClasses = nClasses;
		this.size = in.size() - 1;
		this.in = in;
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
		currentClass = classify(d1);
		d2 = new double[size];
		for (int i = 0; i < size; i++) {
			d2[i] = d1[i];
		}
		return d2;
	}

	/** Returns the number of classes */
	public int nClasses() {
		return nClasses;
	}

	/** Returns the current class index 0..nClasses-1 */
	public int currentClassIndex() {
		return currentClass;
	}

	/**
	 * Returns the current class representation :
	 * a binary vector with a 1.0 at the current class index, and 0.0 elsewhere
	 */
	public double[] currentClassRepresentation() {
		return classRepresentation(currentClass);
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

	// Utilities

	/**
	 * Classifies a vector
	 *
	 * @param v
	 *            the vector to classify
	 * @return a number in range 0..nClasses - 1
	 */
	protected abstract int classify(double[] v);

	double[] classRepresentation(int classIndex) {
		double[] rep = new double[nClasses];

		for (int i = 0; i < nClasses; i++) {
			rep[i] = (i == classIndex ? 1.0 : 0.0);
		}
		return rep;
	}
}
