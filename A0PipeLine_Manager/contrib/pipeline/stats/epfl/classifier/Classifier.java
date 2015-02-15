package pipeline.stats.epfl.classifier;

import pipeline.stats.epfl.io.VectorReader;

/**
 * A classifier.
 *
 * @author Camille Weber <camille.weber@epfl.ch>
 */
public interface Classifier extends VectorReader {

	/** Classification function */
	public double[] classify(double[] features);

	/** Returns the number of classes */
	public int nClasses();
}
