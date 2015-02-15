package pipeline.stats.epfl.pdf;

import java.io.IOException;

import pipeline.stats.epfl.io.VectorReader;
import pipeline.stats.epfl.io.VectorWriter;

/**
 * EM algorithm on multivariate normal kernel pdf with prior probabilities
 *
 * @author Camille Weber <camille.weber@epfl.ch>
 */
public class PEMOnMultNormalKernel extends EMOnMultNormalKernel {

	/**
	 * Creates a new EM algorithm on the underlying MultNormalKernel pdf
	 *
	 * @param workingPdf
	 *            the underlying MultNormalKernel pdf
	 * @param minGrowth
	 *            convergence criteria
	 * @param minSigma
	 *            min value for sigma
	 * @param data
	 *            the data for building a pdf <br>
	 *            data dimension = working pdf dimension + 1 for the prior probability
	 * @param likehoodOut
	 *            the output stream for likehood maximization
	 */
	public PEMOnMultNormalKernel(MultNormalKernelPdf workingPdf, double minGrowth, double minSigma, VectorReader data,
			VectorWriter likehoodOut) {
		super(minGrowth, minSigma, likehoodOut);
		if (workingPdf.dimension() != data.size() - 1)
			throw new IllegalArgumentException(
					"the data size must equal the working pdf dimension plus one for the prior probability");
		this.workingPdf = workingPdf;
		this.data = new VectorPriorProb(data);
	}

	/**
	 * Creates a new EM algorithm on the underlying NormalKernel pdf
	 *
	 * @param workingPdf
	 *            the underlying NormalKernel pdf
	 * @param minGrowth
	 *            convergence criteria
	 * @param minSigma
	 *            min value for sigma
	 * @param data
	 *            the data for building a pdf <br>
	 *            data dimension = working pdf dimension + 1 for the prior probability
	 */
	public PEMOnMultNormalKernel(MultNormalKernelPdf workingPdf, double minGrowth, double minSigma, VectorReader data) {
		this(workingPdf, minGrowth, minSigma, data, null);
	}

	/** M step */
	@Override
	protected void maximization() throws IOException {
		int nValues;

		double[] muK;
		double[] x;
		double[] pKF;
		int nKernels = workingPdf.nKernels();
		int d = workingPdf.dimension();
		double[][] mu = new double[nKernels][d];
		double[] sigma = new double[nKernels];
		double[] sum = new double[nKernels];
		double likehood = 0.0;

		for (int k = 0; k < nKernels; k++) {
			for (int j = 0; j < d; j++) {
				mu[k][j] = 0.0;
			}
			sigma[k] = 0.0;
			sum[k] = 0.0;
		}

		VectorPriorProb pdata = (VectorPriorProb) data;
		nValues = pdata.period();
		for (int i = 0; i < nValues; i++) {
			x = pdata.read();
			double q = pdata.priorProb();
			pKF = expectation(x);
			likehood += q * Math.log(pKF[nKernels]);

			for (int k = 0; k < nKernels; k++) {
				muK = workingPdf.getKernel(k).mu;
				double l = 0.0;
				for (int j = 0; j < d; j++) {
					mu[k][j] += q * pKF[k] * x[j];
					l += (x[j] - muK[j]) * (x[j] - muK[j]);
				}
				sigma[k] += q * pKF[k] * l;
				sum[k] += q * pKF[k];
			}
		}

		MultNormalKernelPdf.MultNormalKernel nK;

		for (int k = 0; k < nKernels; k++) {
			nK = workingPdf.getKernel(k);
			for (int j = 0; j < d; j++) {
				nK.mu[j] = mu[k][j] / sum[k];
			}
			nK.sigma = Math.sqrt(sigma[k] / (sum[k] * d));
			if (nK.sigma < minSigma) {
				nK.sigma = minSigma;
			}
			nK.weight = 1.0 / nValues * sum[k];
		}

		currentLikehood = likehood;
	}
}
