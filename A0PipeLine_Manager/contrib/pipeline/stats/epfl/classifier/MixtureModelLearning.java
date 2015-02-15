package pipeline.stats.epfl.classifier;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import pipeline.stats.epfl.io.FeatureSeparator;
import pipeline.stats.epfl.io.VectorDemultiplexer;
import pipeline.stats.epfl.io.VectorPrinter;
import pipeline.stats.epfl.io.VectorReader;
import pipeline.stats.epfl.io.VectorUnknownPeriodReader;
import pipeline.stats.epfl.io.VectorWriter;
import pipeline.stats.epfl.pdf.EMOnNormalKernel;
import pipeline.stats.epfl.pdf.NormalKernelPdf;

/**
 * Learning process for a mixture model classifier based on the EM algorithm.
 *
 * @author Camille Weber <camille.weber@epfl.ch>
 */
public class MixtureModelLearning implements Runnable {

	/** Estimates the normal kernel pdf from one class and one feature */
	static class FeatureLearning implements Runnable {
		VectorReader data;
		EMOnNormalKernel em;
		NormalKernelPdf featurePdf;
		VectorWriter likehoodOut;

		final int nLearningSteps;

		/** Creates a new feature learning process */
		public FeatureLearning(NormalKernelPdf featurePdf, VectorReader data, String likehoodOutName, double minGrowth,
				double minSigma, boolean findMissingValSubst, int nLearningSteps) throws IOException {
			this.data = data;
			this.featurePdf = featurePdf;
			if (likehoodOutName != null) {
				likehoodOut = new VectorPrinter(new PrintWriter(new FileWriter(likehoodOutName + ".data")), 2);
			} else {
				likehoodOut = null;
			}
			em = new EMOnNormalKernel(featurePdf, minGrowth, minSigma, findMissingValSubst, data, likehoodOut);
			this.nLearningSteps = nLearningSteps;
			init();
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

		void init() throws IOException {
			double[] mu;
			double[] sigma;
			int nKernels = featurePdf.nKernels();
			// int nData = data.period();

			// Chooses nKernels between min and max
			double minMu = Double.POSITIVE_INFINITY;
			double maxMu = Double.NEGATIVE_INFINITY;
			double v;
			for (int i = 0; i < data.period(); i++) {
				v = (data.read())[0];
				if (v < minMu) {
					minMu = v;
				}
				if (v >= maxMu) {
					maxMu = v;
				}
			}

			mu = new double[nKernels];
			sigma = new double[nKernels];
			for (int i = 0; i < nKernels; i++) {
				// mu[i] = minMu + Math.random() * (maxMu - minMu);
				mu[i] = (data.read())[0];
				sigma[i] = Double.POSITIVE_INFINITY;
			}

			// Computes the sigma according to K = 1 neigbour
			if (nKernels == 1) {
				sigma[0] = 1.0;
			} else if (nKernels == 2) {
				sigma[0] = Math.abs(mu[0] - mu[1]);
				if (sigma[0] < 0.0001) {
					sigma[0] = 0.0001;
				}
				sigma[1] = sigma[0];
			} else {
				for (int i = 0; i < nKernels; i++) {
					for (int j = i + 1; j < nKernels; j++) {
						double s = Math.abs(mu[i] - mu[j]);
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
				NormalKernelPdf.NormalKernel k = featurePdf.getKernel(i);
				k.mu = mu[i];
				k.sigma = sigma[i];
			}
			em.init();
		}
	}

	/** Estimates the normal kernels pdf for every feature */
	protected static class ClassLearning implements Runnable {

		protected static class MissingValFilter implements VectorReader {

			VectorReader in;
			final double[] missingValCode;

			/** Builds a new missing value filter on the underlying vector reader */
			public MissingValFilter(VectorReader in, double[] missingValCode) {
				this.in = in;
				this.missingValCode = missingValCode;
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
				double[] v;

				v = in.read();
				while (hasMissingVal(v)) {
					v = in.read();
				}
				return v;
			}

			/** Returns the stream size (vector size) */
			@Override
			public int size() {
				return in.size();
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

			boolean hasMissingVal(double[] v) {
				for (int i = 0; i < missingValCode.length; i++) {
					if (v[i] == missingValCode[i])
						return true;
				}
				return false;
			}
		}

		FeatureSeparator featureSeparator;
		MixtureModelClassifier.ClassModel classModel;
		final String className;
		final double minGrowth;
		final double minSigma;
		final boolean findMissingValSubst;
		final int nLearningSteps;

		/** Creates a new class learning process */
		public ClassLearning(MixtureModelClassifier.ClassModel classModel, VectorReader classData, String className,
				double minGrowth, double minSigma, boolean findMissingValSubst, int nLearningSteps) {

			featureSeparator = new FeatureSeparator(classData);
			this.classModel = classModel;
			this.className = className;
			this.minGrowth = minGrowth;
			this.minSigma = minSigma;
			this.findMissingValSubst = findMissingValSubst;
			this.nLearningSteps = nLearningSteps;
		}

		/** Runs the learning process */
		@Override
		public void run() {
			int nFeatures = classModel.nFeatures();

			for (int i = 0; i < nFeatures; i++) {
				try {
					NormalKernelPdf featurePdf = classModel.getFeaturePdf(i);

					VectorReader featureData;
					if (findMissingValSubst) {
						double[] missingValCode = new double[1];
						missingValCode[0] = featurePdf.missingValCode;
						featureData =
								new VectorUnknownPeriodReader(new MissingValFilter(featureSeparator.getVectorReader(i),
										missingValCode), false);
					} else {
						featureData = new VectorUnknownPeriodReader(featureSeparator.getVectorReader(i), false);
					}

					FeatureLearning featureLearning =
							new FeatureLearning(featurePdf, featureData, (className != null ? className + ".feature"
									+ i : null), minGrowth, minSigma, findMissingValSubst, nLearningSteps);
					featureLearning.run();
				} catch (IOException e) {
					System.err.println(e);
				}
			}
		}
	}

	private VectorDemultiplexer classDemultiplexer;
	private MixtureModelClassifier classifier;
	private final boolean outputLikehood;
	private final double minGrowth;
	private final double minSigma;
	private final boolean findMissingValSubst;
	private final int nLearningSteps;

	/** Estimates the normal kernel pdf for every class and every feature */
	public MixtureModelLearning(MixtureModelClassifier classifier, VectorDemultiplexer classDemultiplexer,
			double minGrowth, double minSigma, boolean findMissingValSubst, int nLearningSteps, boolean outputLikehood) {
		this.classDemultiplexer = classDemultiplexer;
		this.classifier = classifier;
		this.outputLikehood = outputLikehood;
		this.minGrowth = minGrowth;
		this.minSigma = minSigma;
		this.findMissingValSubst = findMissingValSubst;
		this.nLearningSteps = nLearningSteps;
	}

	/** Runs the learning process */
	@Override
	public void run() {
		int nClasses = classifier.nClasses();

		for (int i = 0; i < nClasses; i++) {
			ClassLearning classLearning =
					new ClassLearning(classifier.getClassModel(i), classDemultiplexer.getVectorReader(i),
							(outputLikehood ? "class" + i : null), minGrowth, minSigma, findMissingValSubst,
							nLearningSteps);
			classLearning.run();
		}
	}
}
