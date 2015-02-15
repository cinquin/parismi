package pipeline.stats.epfl.experiment;

import pipeline.stats.epfl.classifier.VectorClassSizeAdapter;
import pipeline.stats.epfl.io.VectorReader;

/**
 * A double vector class size adapter for problem P1.
 * For each read vector, the class index is decoded and put in currentClassIndex.
 *
 * @author Camille Weber <camille.weber@epfl.ch>
 */
public class P1VectorClassSizeAdapter extends VectorClassSizeAdapter {

	/**
	 * Creates a new size adapter
	 *
	 * @param in
	 *            the underlying vector reader
	 *            The stream size is in.size() - 1
	 */
	public P1VectorClassSizeAdapter(VectorReader in) {
		// Builds a new class size adapter for 2 classes
		super(in, 2);
	}

	// Utilities

	/**
	 * Classifies a vector
	 *
	 * @param v
	 *            the vector to classify
	 * @return a number in range 0..nClasses - 1
	 */
	@Override
	protected int classify(double[] v) {
		// The last vector component is the class index
		return (int) v[size];
	}

}
