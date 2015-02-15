package pipeline.stats.epfl.pdf;

import java.io.IOException;

import pipeline.stats.epfl.io.VectorReader;
import pipeline.stats.epfl.io.VectorWriter;

/**
 * A probability density function
 *
 * @author Camille Weber <camille.weber@epfl.ch>
 */
public interface Pdf extends VectorReader {

	/** Probability density function */
	public double pdf(double x);

	/**
	 * Plots the pdf in a vector writer
	 *
	 * @exception IOException
	 *                if an io error occurs
	 */
	public void plot(double xMin, double xMax, int nPoints, VectorWriter out) throws IOException;

}
