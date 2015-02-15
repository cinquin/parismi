package pipeline.stats.epfl.pdf;

import pipeline.stats.epfl.io.VectorReader;

/**
 * The normal probability density function
 *
 * @author Camille Weber <camille.weber@epfl.ch>
 */
public class NormalPdf extends AbstractPdf {

	public double mu;
	public double sigma;

	/**
	 * Creates a new Normal pdf
	 *
	 * @param in
	 *            the underlying vector reader
	 */
	private NormalPdf(VectorReader in, double mu, double sigma) {
		super(in);
		this.mu = mu;
		this.sigma = sigma;
	}

	/** Creates a new Normal pdf without stream connection */
	public NormalPdf(double mu, double sigma) {
		this(null, mu, sigma);
	}

	/** Probability density function */
	@Override
	public double pdf(double x) {
		return (1.0 / Math.sqrt(2.0 * Math.PI)) * (1.0 / sigma)
				* Math.exp((-1.0 / (2.0 * sigma * sigma)) * (x - mu) * (x - mu));
	}
}
