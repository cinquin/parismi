package pipeline.stats.epfl.classifier;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import pipeline.stats.epfl.io.VectorDemultiplexer;
import pipeline.stats.epfl.io.VectorPrinter;
import pipeline.stats.epfl.io.VectorReader;
import pipeline.stats.epfl.io.VectorUnknownPeriodReader;
import pipeline.stats.epfl.io.VectorWriter;
import pipeline.stats.epfl.pdf.EMOnMultNormalKernel;
import pipeline.stats.epfl.pdf.MultNormalKernelPdf;

/**
 * Learning process for a multivariate mixture model classifier based on
 * the EM algorithm.
 *
 * @author Camille Weber <camille.weber@epfl.ch>
 */
public class MultMixtureModelLearning implements Runnable {

	/** Estimates the normal kernels pdf of a class */
	static class MultClassLearning implements Runnable {

		VectorReader data;
		MultNormalKernelPdf featuresPdf;
		EMOnMultNormalKernel em;
		VectorWriter likehoodOut;
		final int nLearningSteps;

		MultClassLearning(MultMixtureModelClassifier.MultClassModel classModel, int nLearningSteps, String className) {
			this.featuresPdf = classModel.getFeaturesPdf();
			this.nLearningSteps = nLearningSteps;
			if (className != null) {
				try {
					likehoodOut = new VectorPrinter(new PrintWriter(new FileWriter(className + ".data")), 2);
				} catch (IOException e) {
					System.err.println(e);
					likehoodOut = null;
				}
			} else {
				likehoodOut = null;
			}
		}

		/** Creates a new class learning process */
		public MultClassLearning(MultMixtureModelClassifier.MultClassModel classModel, VectorReader data,
				String className, double minGrowth, double minSigma, int nLearningSteps) throws IOException {

			this(classModel, nLearningSteps, className);
			this.data = new VectorUnknownPeriodReader(data, false);
			em = new EMOnMultNormalKernel(featuresPdf, minGrowth, minSigma, this.data, likehoodOut);
			init(this.data);
		}

		void init(VectorReader data) throws IOException {
			double[][] mu;
			double[] sigma;
			int nKernels = featuresPdf.nKernels();
			// int nData = data.period();

			// Chooses nKernels in data
			mu = new double[nKernels][];
			sigma = new double[nKernels];
			for (int i = 0; i < nKernels; i++) {
				mu[i] = data.read();
				sigma[i] = Double.POSITIVE_INFINITY;
			}

			// Computes the sigma according to K = 1 neigbour
			if (nKernels == 1) {
				sigma[0] = 1.0;
			} else if (nKernels == 2) {
				sigma[0] = Math.sqrt(squareLength(mu[0], mu[1]));
				if (sigma[0] < 0.0001) {
					sigma[0] = 0.0001;
				}
				sigma[1] = sigma[0];
			} else {
				for (int i = 0; i < nKernels; i++) {
					for (int j = i + 1; j < nKernels; j++) {
						double s = Math.sqrt(squareLength(mu[i], mu[j]));
						if (s < 0.0001) {
							s = 0.0001;
						}
						if (s < sigma[i])
							sigma[i] = s;
						if (s < sigma[j])
							sigma[j] = s;
					}
				}
			}

			// Initializes the kernels with mu and sigma
			for (int i = 0; i < nKernels; i++) {
				MultNormalKernelPdf.MultNormalKernel k = featuresPdf.getKernel(i);
				k.mu = mu[i];
				k.sigma = sigma[i];
			}
			em.init();
		}

		/** Runs the learning process */
		@Override
		public void run() {
			try {
				em.maximizeLikehood(nLearningSteps);
			} catch (IOException e) {
				System.err.println(e);
			}
			try {
				if (likehoodOut != null)
					likehoodOut.close();
			} catch (IOException e) {
			}
		}

		// Utilities

		private static double squareLength(double[] v1, double[] v2) {
			double l = 0.0;
			for (int i = 0; i < v1.length; i++) {
				l += (v1[i] - v2[i]) * (v1[i] - v2[i]);
			}
			return l;
		}
	}

	VectorDemultiplexer classDemultiplexer;
	MultMixtureModelClassifier classifier;
	final boolean outputLikehood;
	final double minGrowth;
	final double minSigma;
	final int nLearningSteps;

	/** Estimates the normal kernel pdf for every class */
	public MultMixtureModelLearning(MultMixtureModelClassifier classifier, VectorDemultiplexer classDemultiplexer,
			double minGrowth, double minSigma, int nLearningSteps, boolean outputLikehood) {
		this.classDemultiplexer = classDemultiplexer;
		this.classifier = classifier;
		this.outputLikehood = outputLikehood;
		this.minGrowth = minGrowth;
		this.minSigma = minSigma;
		this.nLearningSteps = nLearningSteps;
	}

	/** Runs the learning process */
	@Override
	public void run() {
		int nClasses = classifier.nClasses();

		for (int i = 0; i < nClasses; i++) {
			try {
				MultClassLearning classLearning =
						new MultClassLearning(classifier.getClassModel(i), classDemultiplexer.getVectorReader(i),
								(outputLikehood ? "class" + i : null), minGrowth, minSigma, nLearningSteps);
				classLearning.run();
			} catch (IOException e) {
				System.err.println(e);
			}
		}
	}
}
