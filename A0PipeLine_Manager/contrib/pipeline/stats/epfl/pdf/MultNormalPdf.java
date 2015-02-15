package pipeline.stats.epfl.pdf;

/**
 * The multivariate normal probability density function
 *
 * @author Camille Weber <camille.weber@epfl.ch>
 */
public class MultNormalPdf {

	public double[] mu;
	public double sigma;

	/**
	 * Creates a new mutlivariate Normal pdf
	 */
	public MultNormalPdf(double mu[], double sigma) {
		this.mu = mu;
		this.sigma = sigma;
	}

	/** Probability density function */
	public double pdf(double[] x) {
		if (x.length < mu.length)
			throw new IllegalArgumentException("dimension of x is too small : " + x.length + " < " + mu.length);

		int d = mu.length;
		double p = 1.0;

		for (int i = 0; i < d; i++) {
			p *=
					(1.0 / Math.sqrt(2.0 * Math.PI)) * (1.0 / sigma)
							* Math.exp(-1.0 / (2.0 * sigma * sigma) * (x[i] - mu[i]) * (x[i] - mu[i]));
		}
		return p;
	}

	/** Returns the vector space dimension */
	public int dimension() {
		return mu.length;
	}
}
