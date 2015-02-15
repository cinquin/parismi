package pipeline.stats.epfl.pdf;

import java.io.IOException;

import pipeline.stats.epfl.io.VectorReader;
import pipeline.stats.epfl.io.VectorWriter;

/**
 * EM algorithm on multivariate normal kernel pdf
 *
 * @author Camille Weber <camille.weber@epfl.ch>
 */
public class EMOnMultNormalKernel {

	/** Convergence criteria : min growth */
	private double minGrowth;

	MultNormalKernelPdf workingPdf;
	private long currentStep;
	double currentLikehood;
	private double currentGrowth;
	VectorReader data;
	private VectorWriter likehoodOut;
	final double minSigma;

	EMOnMultNormalKernel(double minGrowth, double minSigma, VectorWriter likehoodOut) {
		this.minGrowth = minGrowth;
		this.minSigma = minSigma;
		this.likehoodOut = likehoodOut;
	}

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
	 *            the data for building a pdf
	 * @param likehoodOut
	 *            the output stream for likehood maximization
	 */
	public EMOnMultNormalKernel(MultNormalKernelPdf workingPdf, double minGrowth, double minSigma, VectorReader data,
			VectorWriter likehoodOut) {
		this(minGrowth, minSigma, likehoodOut);
		if (workingPdf.dimension() != data.size())
			throw new IllegalArgumentException("the data dimension must equal the working pdf dimension");
		this.workingPdf = workingPdf;
		this.data = data;

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
	 *            the data for building a pdf
	 */
	public EMOnMultNormalKernel(MultNormalKernelPdf workingPdf, double minGrowth, double minSigma, VectorReader data) {
		this(workingPdf, minGrowth, minSigma, data, null);
	}

	/** Initializes the algorithm */
	public void init() {
		currentStep = 0;
		currentLikehood = Double.NEGATIVE_INFINITY;
		currentGrowth = Double.POSITIVE_INFINITY;
		int nKernels = workingPdf.nKernels();
		if (!workingPdf.areKernelWeightsValid()) {
			for (int k = 0; k < nKernels; k++) {
				workingPdf.getKernel(k).weight = 1.0 / nKernels;
			}
		}
	}

	/** Searches for the maximum likehood in maxIterations */
	public double maximizeLikehood(long maxIterations) throws IOException {
		long flushRate = (maxIterations > 200 ? maxIterations / 200 : 1);
		int confirmConvergence = 0;

		if (maxIterations < 0)
			throw new IllegalArgumentException("maxIteration must be positive");

		while ((currentStep < maxIterations) && (confirmConvergence < 5)) {

			nextStep();
			if (currentGrowth < minGrowth) {
				confirmConvergence++;
			} else {
				confirmConvergence = 0;
			}
			if (currentStep % flushRate == 0)
				flush();
		}
		flush();

		return currentLikehood;
	}

	/** Executes one step from the EM algorithm */
	double nextStep() throws IOException {
		double pastLikehood = currentLikehood;
		double[] out = new double[2];

		maximization();
		if (currentStep >= 1)
			currentGrowth = (currentLikehood - pastLikehood) / Math.abs(pastLikehood);
		out[0] = currentStep;
		out[1] = currentLikehood;
		if (likehoodOut != null)
			likehoodOut.write(out);
		currentStep++;

		return currentLikehood;
	}

	/** M step */
	void maximization() throws IOException {
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

		nValues = data.period();
		for (int i = 0; i < nValues; i++) {
			x = data.read();

			pKF = expectation(x);
			likehood += Math.log(pKF[nKernels]);

			for (int k = 0; k < nKernels; k++) {
				muK = workingPdf.getKernel(k).mu;
				double l = 0.0;
				for (int j = 0; j < d; j++) {
					mu[k][j] += pKF[k] * x[j];
					l += (x[j] - muK[j]) * (x[j] - muK[j]);
				}
				sigma[k] += pKF[k] * l;
				sum[k] += pKF[k];
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

	/** E step : Returns p(kernel k | feature x) for all kernels */
	double[] expectation(double[] x) {
		int nKernels = workingPdf.nKernels();
		MultNormalKernelPdf.MultNormalKernel nK;

		double[] pFK = new double[nKernels]; // p(feature | kernel k) * p(kernel k)
		double[] pKF = new double[nKernels + 1]; // p(kernel k | feature)
		double sumPFK = 0.0; // sum on all kernels pFK

		for (int k = 0; k < nKernels; k++) {
			nK = workingPdf.getKernel(k);
			pFK[k] = nK.pdf(x) * nK.weight;
			sumPFK += pFK[k];
		}

		for (int k = 0; k < nKernels; k++) {
			pKF[k] = pFK[k] / sumPFK; // Bayes rule
			if (Double.isNaN(pKF[k])) {
				pKF[k] = 1.0 / nKernels;
			}
		}

		pKF[nKernels] = sumPFK;
		return pKF;
	}

	/** Returns the current number of steps */
	public long currentStep() {
		return currentStep;
	}

	/** Returns the currentLikehood */
	public double currentLikehood() {
		return currentLikehood;
	}

	/** Returns the current growth (L(t+1) - L(t)) / L(t) where L = likehood */
	public double currentGrowth() {
		return currentGrowth;
	}

	/** Returns a reference on the NormalKernelPdf under maximization */
	public MultNormalKernelPdf getNormalKernelPdf() {
		return workingPdf;
	}

	/** Flushes the likehood output stream */
	void flush() throws IOException {
		if (likehoodOut != null)
			likehoodOut.flush();
	}

	/** Closes the likehood output stream */
	public void close() throws IOException {
		if (likehoodOut != null)
			likehoodOut.close();
	}
}
