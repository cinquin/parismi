package pipeline.stats.epfl.pdf;

import java.io.IOException;

import pipeline.stats.epfl.io.VectorReader;
import pipeline.stats.epfl.io.VectorWriter;

/**
 * EM algorithm on normal kernel pdf
 *
 * @author Camille Weber <camille.weber@epfl.ch>
 */
public class EMOnNormalKernel {

	/** Convergence criteria : min growth */
	private double minGrowth;

	/** Flag permitting the change of the missing value substitute */
	private boolean findMissingValSubst;

	private NormalKernelPdf workingPdf;
	private long currentStep;
	private double currentLikehood;
	private double currentGrowth;
	private VectorReader data;
	private VectorWriter likehoodOut;
	private final double minSigma;

	/**
	 * Creates a new EM algorithm on the underlying NormalKernel pdf
	 *
	 * @param workingPdf
	 *            the underlying NormalKernel pdf
	 * @param minGrowth
	 *            convergence criteria
	 * @param minSigma
	 *            min value for sigma
	 * @param findMissingValSubst
	 *            flag permitting the change of the missing
	 *            value substitute in workingPdf
	 * @param data
	 *            the data for building a pdf
	 * @param likehoodOut
	 *            the output stream for likehood maximization
	 */
	public EMOnNormalKernel(NormalKernelPdf workingPdf, double minGrowth, double minSigma, boolean findMissingValSubst,
			VectorReader data, VectorWriter likehoodOut) {
		this.workingPdf = workingPdf;
		this.minGrowth = minGrowth;
		this.minSigma = minSigma;
		this.findMissingValSubst = findMissingValSubst;
		this.data = data;
		this.likehoodOut = likehoodOut;
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
	 * @param findMissingValSubst
	 *            flag permitting the change of the missing
	 *            value substitute in workingPdf
	 * @param data
	 *            the data for building a pdf
	 */
	public EMOnNormalKernel(NormalKernelPdf workingPdf, double minGrowth, double minSigma, boolean findMissingValSubst,
			VectorReader data) {
		this(workingPdf, minGrowth, minSigma, findMissingValSubst, data, null);
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

		double muK;
		double[] x;
		double[] pKF;
		int nKernels = workingPdf.nKernels();
		double[] mu = new double[nKernels];
		double[] sigma = new double[nKernels];
		double[] sum = new double[nKernels];
		double likehood = 0.0;

		for (int k = 0; k < nKernels; k++) {
			mu[k] = 0.0;
			sigma[k] = 0.0;
			sum[k] = 0.0;
		}

		nValues = data.period();
		for (int i = 0; i < nValues; i++) {
			x = data.read();
			pKF = expectation(x[0]);
			likehood += Math.log(pKF[nKernels]);

			for (int k = 0; k < nKernels; k++) {
				mu[k] += pKF[k] * x[0];
				muK = workingPdf.getKernel(k).mu;
				sigma[k] += pKF[k] * (x[0] - muK) * (x[0] - muK);
				sum[k] += pKF[k];
			}
		}

		NormalKernelPdf.NormalKernel nK;
		NormalKernelPdf.NormalKernel firstKernel = (nKernels > 0 ? workingPdf.getKernel(0) : null);

		for (int k = 0; k < nKernels; k++) {
			nK = workingPdf.getKernel(k);
			nK.mu = mu[k] / sum[k];
			nK.sigma = Math.sqrt(sigma[k] / sum[k]);
			if (nK.sigma < minSigma) {
				nK.sigma = minSigma;
			}
			nK.weight = 1.0 / nValues * sum[k];
			if (nK.weight > firstKernel.weight) {
				firstKernel = nK;
			}
		}
		if (findMissingValSubst && (firstKernel != null)) {
			workingPdf.missingValSubst = firstKernel.mu;
			// -firstKernel.sigma/2.0 + Math.random() * 2.0 * firstKernel.sigma/2.0;
		}

		currentLikehood = likehood;
	}

	/** E step : Returns p(kernel k | feature x) for all kernels */
	double[] expectation(double x) {
		int nKernels = workingPdf.nKernels();
		NormalKernelPdf.NormalKernel nK;

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
	public NormalKernelPdf getNormalKernelPdf() {
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
