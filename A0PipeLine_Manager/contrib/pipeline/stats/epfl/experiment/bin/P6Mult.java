package pipeline.stats.epfl.experiment.bin;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import pipeline.stats.epfl.classifier.Classifier;
import pipeline.stats.epfl.classifier.MultMixtureModelClassifier;
import pipeline.stats.epfl.classifier.MultMixtureModelLearning;
import pipeline.stats.epfl.classifier.VectorClassSizeAdapter;
import pipeline.stats.epfl.experiment.P6VectorClassSizeAdapter;
import pipeline.stats.epfl.experiment.P6VectorDemultiplexer;
import pipeline.stats.epfl.io.TagReader;
import pipeline.stats.epfl.io.TagVectorReader;
import pipeline.stats.epfl.io.VectorDemultiplexer;
import pipeline.stats.epfl.io.VectorManualPeriodReader;
import pipeline.stats.epfl.io.VectorPrinter;
import pipeline.stats.epfl.io.VectorReader;
import pipeline.stats.epfl.io.VectorSniffer;
import pipeline.stats.epfl.io.VectorUnionReader;
import pipeline.stats.epfl.io.VectorUnknownPeriodReader;
import pipeline.stats.epfl.io.VectorWriter;
import pipeline.stats.epfl.pdf.MultNormalKernelPdf;
import pipeline.stats.epfl.pdf.PCA;
import pipeline.stats.epfl.pdf.VectorNormalize;
import pipeline.stats.epfl.pdf.VectorPCATransform;

/**
 * Main program for problem P6.
 *
 * @author Camille Weber <camille.weber@epfl.ch>
 */
class P6Mult {

	public static void main(String[] args) {
		try {

			// READING PARAMETERS

			String dataFileName = "", expFileName = "";

			if (args.length < 2) {
				System.out.println("Usage : P6 dataFileName experimentFileName");
				System.out.println("where : ");
				System.out.println("dataFileName       : tag file name containing learning data and testing data");
				System.out.println("experimentFileName : tag file name containing the experiment parameters");
				System.exit(0);
			} else {
				dataFileName = args[0];
				expFileName = args[1];
			}

			boolean runPCA;
			double explainedVariance;
			double minGrowth;
			double minSigma;
			double confidentRange;
			int nLearningSteps;
			int nKernels;
			int nClasses, nFeatures;

			/*
			 * Reading tag experiment file :
			 * <experiment>
			 * 
			 * Flag saying if a PCA has to be carried out on the learning data
			 * <runPCA> boolean </runPCA>
			 * 
			 * In case of a PCA, the maximum percentage of variance explained
			 * <explainedVariance> double [0.0, 1.0] </explainedVariance>
			 * 
			 * The number of kernels used in classifier :
			 * <nKernels> int </nKernels> (for each class)
			 * 
			 * The minimum value for sigma in multivariate normal distribution
			 * <minSigma> double </minSigma>
			 * 
			 * The growth limit in the EM algorithm. (stopping criteria)
			 * <minGrowth> double </minGrowth>
			 * 
			 * The maximum number of learning steps in the EM algorithm (stopping criteria)
			 * <nLearningSteps> int </nLearningSteps>
			 * 
			 * The confident range value for the classification function
			 * <confidentRange> double [0.0, 1.0 - 1.0/nClasses]</confidentRange>
			 * </experiment>
			 */

			debug("Reading experiment");
			TagReader exp = new TagReader(new FileReader(expFileName));
			exp.readStartTag("experiment");
			runPCA = exp.readBoolean("runPCA");
			explainedVariance = exp.readDouble("explainedVariance");
			nKernels = exp.readInt("nKernels");
			minSigma = exp.readDouble("minSigma");
			minGrowth = exp.readDouble("minGrowth");
			nLearningSteps = exp.readInt("nLearningSteps");
			confidentRange = exp.readDouble("confidentRange");
			exp.readEndTag("experiment");
			exp.close();

			debug("Creating tag reader for data");
			TagReader tr = new TagReader(new FileReader(dataFileName));
			VectorReader[] readers = new VectorReader[2];

			debug("Creating periodic reader for labeled learning");
			readers[0] = new VectorUnknownPeriodReader(new TagVectorReader(tr, "learnL"), false);

			debug("Creating periodic reader for testing");
			readers[1] = new VectorUnknownPeriodReader(new TagVectorReader(tr, "test"), false);

			debug("Computes mean and standard deviation");
			double[] mean = null;
			double[] stdDev = null;
			{
				VectorReader tmp = new P6VectorClassSizeAdapter(readers[0]);
				mean = computeMean(tmp);
				stdDev = computeStdDev(tmp, mean);
			}

			// ________________________________________________________________________

			// Shows how to run a principal component analysis on the learning data
			// before building the classifier.
			// It is not usefull for problem P6.

			double[] eval = null;
			double[][] evectors = null;
			int nComp = 0;
			if (runPCA) {

				debug("-  PCA  -");

				debug("Creating PCA");
				PCA pca = new PCA(new VectorNormalize(new P6VectorClassSizeAdapter(readers[0]), mean, stdDev));
				debug("Running PCA");
				pca.run();

				eval = pca.getEigenvalues();
				evectors = pca.getEigenvectors();
				double sum = 0.0;
				for (double element : eval) {
					sum += element;
				}

				double contribution = 0.0;
				while (contribution < explainedVariance && nComp < eval.length) {

					double localContribution = eval[nComp] / sum;
					contribution += localContribution;
					debug(nComp + 1 + " eval: " + eval[nComp] + " var. explained: " + localContribution + " total: "
							+ contribution);
					nComp++;
				}

				pca = null;
			}

			// ________________________________________________________________________

			debug("-  BUILDING CLASSIFIER  -");

			// Creates learning periodic reader :
			VectorManualPeriodReader learningReader;
			if (runPCA)
				learningReader =
						new VectorManualPeriodReader(new VectorPCATransform(new VectorNormalize(readers[0], mean,
								stdDev), nComp, evectors, eval), false);
			else
				learningReader = new VectorManualPeriodReader(new VectorNormalize(readers[0], mean, stdDev), false);

			// Creates testing periodic reader
			VectorReader testingReader;
			if (runPCA)
				testingReader =
						new VectorPCATransform(new VectorNormalize(readers[1], mean, stdDev), nComp, evectors, eval);
			else
				testingReader = new VectorNormalize(readers[1], mean, stdDev);

			debug("Creating class demultiplexer");
			VectorDemultiplexer classDpx = new P6VectorDemultiplexer(learningReader);
			nClasses = classDpx.nStreams();
			if (runPCA)
				nFeatures = nComp;
			else
				nFeatures = classDpx.getVectorReader(0).size();

			debug("Creating mixture model classifier");
			MultMixtureModelClassifier mmmc =
					new MultMixtureModelClassifier(nClasses, nFeatures, nKernels, null, 0.0, 0.0, 1.0, "P6");

			debug("Creating mixture model learning");
			MultMixtureModelLearning mmml =
					new MultMixtureModelLearning(mmmc, classDpx, minGrowth, minSigma, nLearningSteps, true);

			debug("Learning...");
			mmml.run();
			learningReader.reset();
			testingError(mmmc, confidentRange, learningReader);

			debug("Testing...");
			testingError(mmmc, confidentRange, testingReader);

			biplot(mmmc,
					new P6VectorClassSizeAdapter(new VectorNormalize(new VectorUnionReader(readers), mean, stdDev)),
					"plot");

			System.out.println(mmmc);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// UTILITIES

	private static final int UNKNOWN_CLASS_CODE = -1;

	// Returns the class index which has the maximum probability and a probability
	// greater than 1/nClasses + confidentRange
	private static int classIndex(double[] v, double confidentRange) {
		int maxIndex = 0, n = 0;

		for (int i = 0; i < v.length; i++) {
			if (v[i] > v[maxIndex])
				maxIndex = i;
			if (v[i] > 0.33 + confidentRange)
				n++;
		}
		if (n == 0) {
			return UNKNOWN_CLASS_CODE;
		} else {
			return maxIndex;
		}
	}

	/*
	 * Tests the classifier with data
	 * Prints the following messages on System.out :
	 * <code> <testingDistribution> <classifiedDistribution>
	 * where :
	 * <code> is ? if the classifier can not decide on the result class
	 * X if the classifier has missclassified a known class
	 * else the classification was correct
	 * <testingDistribution> is (0.0, 1.0) or (1.0, 0.0) if the class is known
	 * (?) if the class is unknown
	 * <classifiedDistribution> is (p, 1-p) with p in [0.0, 1.0]
	 * 
	 * Returns an error array :
	 * error[0] = the number of tests
	 * error[1] = the number of missclassified vectors (hard error)
	 * error[2] = the number of known vectors impossible to classify (unknow error)
	 * error[3] = the number of unknown vectors impossible to classify (unknown)
	 */
	private static int[] testingError(Classifier c, double confidentRange, VectorReader testingReader)
			throws IOException {

		VectorSniffer snif = new VectorSniffer(testingReader);
		VectorClassSizeAdapter testingData = new P6VectorClassSizeAdapter(snif);
		double[] result, model;
		int resultIndex, modelIndex;
		int hardError = 0, unknownError = 0, unknown = 0;

		for (int i = 0; i < testingReader.period(); i++) {
			result = c.classify(testingData.read());
			modelIndex = testingData.currentClassIndex();
			resultIndex = classIndex(result, confidentRange);

			if (modelIndex == UNKNOWN_CLASS_CODE) {
				if (resultIndex == UNKNOWN_CLASS_CODE) {

					System.out.print("? (?) ");
					printVector(result);
					System.out.println();

					unknown++;
				} else {

					System.out.print("  (?) ");
					printVector(result);
					System.out.println();

					// Updates the class code in reader
					snif.lastVector()[snif.lastVector().length - 1] = resultIndex;
				}
			} else {
				model = testingData.currentClassRepresentation();
				if (resultIndex == UNKNOWN_CLASS_CODE) {

					System.out.print("? ");
					printVector(model);
					System.out.print(' ');
					printVector(result);
					System.out.println();

					unknownError++;
				} else if (resultIndex != modelIndex) {

					System.out.print("X ");
					printVector(model);
					System.out.print(' ');
					printVector(result);
					System.out.println();

					hardError++;
				}
			}
		}
		System.out.println("Unknown : " + unknown + " Unknown error : " + unknownError + " Hard error : " + hardError);
		int[] error = new int[4];
		error[0] = testingReader.period();
		error[1] = hardError;
		error[2] = unknownError;
		error[3] = unknown;
		return error;
	}

	private static void printVector(double[] v) {
		System.out.print("(");
		for (int i = 0; i < v.length - 1; i++) {
			System.out.print(v[i]);
			System.out.print(", ");
		}
		System.out.print(v[v.length - 1]);
		System.out.print(")");
	}

	/*
	 * private static void printVector(PrintWriter out, double[] v) {
	 * for(int i = 0; i < v.length; i++) {
	 * out.print(v[i]);
	 * out.print(" ");
	 * }
	 * }
	 */

	private static double[] computeMean(VectorReader data) throws IOException {
		int n = data.size();
		int nData = data.period();
		double[] mean = new double[n];

		for (int j = 0; j < n; j++) {
			mean[j] = 0.0;
		}

		for (int i = 0; i < nData; i++) {
			double[] v = data.read();
			for (int j = 0; j < n; j++) {
				mean[j] += v[j];
			}
		}

		for (int j = 0; j < n; j++) {
			mean[j] /= nData;
		}

		return mean;
	}

	private static double[] computeStdDev(VectorReader data, double[] mean) throws IOException {
		int n = data.size();
		int nData = data.period();
		double[] stdDev = new double[n];

		for (int j = 0; j < n; j++) {
			stdDev[j] = 0.0;
		}

		for (int i = 0; i < nData; i++) {
			double[] v = data.read();
			for (int j = 0; j < n; j++) {
				stdDev[j] += (v[j] - mean[j]) * (v[j] - mean[j]);
			}
		}

		for (int j = 0; j < n; j++) {
			stdDev[j] = Math.sqrt(stdDev[j] / (nData - 1));
		}

		return stdDev;
	}

	private static void debug(String msg) {
		System.out.println(msg);
	}

	private static void biplot(MultMixtureModelClassifier mmmc, VectorClassSizeAdapter data, String fileName) {
		try {
			int nClasses = data.nClasses();
			VectorWriter[] out = new VectorWriter[nClasses + 1];
			for (int i = 0; i < nClasses; i++) {
				out[i] = new VectorPrinter(new PrintWriter(new FileWriter(fileName + "." + i)), 2);
			}
			out[nClasses] = new VectorPrinter(new PrintWriter(new FileWriter(fileName + ".unknown")), 2);

			double[] eval;
			double[][] evectors;

			PCA pca = new PCA(data);
			pca.run();

			eval = pca.getEigenvalues();
			evectors = pca.getEigenvectors();
			VectorReader biplot = new VectorPCATransform(data, 2, evectors, eval);

			for (int i = 0; i < biplot.period(); i++) {
				double[] v = biplot.read();
				int classIndex = data.currentClassIndex();
				if (classIndex != UNKNOWN_CLASS_CODE) {
					out[classIndex].write(v);
				} else {
					out[nClasses].write(v);
				}
			}

			for (int i = 0; i < nClasses; i++) {
				MultNormalKernelPdf nK = mmmc.getClassModel(i).getFeaturesPdf();
				int nKernels = nK.nKernels();

				double[] mu;
				double sigma;
				double[] p = new double[2];
				int nDots = 100;

				for (int j = 0; j < nKernels; j++) {
					mu = pcaTransform(nK.getKernel(j).mu, 2, evectors, eval);
					sigma = nK.getKernel(j).sigma;

					out[i].write(mu);
					for (int k = 0; k < nDots; k++) {
						double angle = Math.PI * 2.0 * k / nDots;
						p[0] = Math.cos(angle) * sigma + mu[0];
						p[1] = Math.sin(angle) * sigma + mu[1];
						out[i].write(p);
					}
				}
			}
			for (int i = 0; i < nClasses; i++) {
				out[i].close();
			}
			out[nClasses].close();
		} catch (Exception e) {
			System.err.println(e);
		}
	}

	private static double[] pcaTransform(double[] v, int nComp, double[][] evectors, double[] evalues) {
		double[] v2 = new double[nComp];

		for (int i = 0; i < nComp; i++) {
			v2[i] = 0.0;
			double length = 0.0;
			for (int j = 0; j < evectors[i].length; j++) {
				v2[i] += evectors[i][j] * v[j];
				length += evectors[i][j] * evectors[i][j];
			}
			v2[i] /= length;
			v2[i] *= Math.sqrt(evalues[i]);
		}
		return v2;
	}

}
