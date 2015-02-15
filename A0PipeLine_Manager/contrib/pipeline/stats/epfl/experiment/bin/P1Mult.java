package pipeline.stats.epfl.experiment.bin;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import pipeline.stats.epfl.classifier.Classifier;
import pipeline.stats.epfl.classifier.MixtureModelClassifier;
import pipeline.stats.epfl.classifier.MixtureModelLearning;
import pipeline.stats.epfl.classifier.MultMixtureModelClassifier;
import pipeline.stats.epfl.classifier.MultMixtureModelLearning;
import pipeline.stats.epfl.classifier.VectorClassSizeAdapter;
import pipeline.stats.epfl.experiment.P1VectorClassSizeAdapter;
import pipeline.stats.epfl.experiment.P1VectorDemultiplexer;
import pipeline.stats.epfl.io.TagReader;
import pipeline.stats.epfl.io.TagVectorReader;
import pipeline.stats.epfl.io.VectorDemultiplexer;
import pipeline.stats.epfl.io.VectorManualPeriodReader;
import pipeline.stats.epfl.io.VectorPrinter;
import pipeline.stats.epfl.io.VectorReader;
import pipeline.stats.epfl.io.VectorReplace;
import pipeline.stats.epfl.io.VectorSimplePeriodReader;
import pipeline.stats.epfl.io.VectorSniffer;
import pipeline.stats.epfl.io.VectorUnionReader;
import pipeline.stats.epfl.io.VectorUnknownPeriodReader;
import pipeline.stats.epfl.io.VectorWriter;
import pipeline.stats.epfl.pdf.MultNormalKernelPdf;
import pipeline.stats.epfl.pdf.NormalKernelPdf;
import pipeline.stats.epfl.pdf.PCA;
import pipeline.stats.epfl.pdf.VectorNormalize;
import pipeline.stats.epfl.pdf.VectorPCATransform;

/**
 * Main program for problem P1.
 *
 * @author Camille Weber <camille.weber@epfl.ch>
 */
public class P1Mult {

	public static void main(String[] args) {
		try {

			// READING PARAMETERS

			String dataFileName = "", expFileName = "", validationFileName = null;

			if (args.length < 2) {
				System.out.println("Usage : P1Mult dataFileName experimentFileName [validationFileName]");
				System.out.println("where : ");
				System.out.println("dataFileName       : tag file name containing learning data and testing data");
				System.out.println("experimentFileName : tag file name containing the experiment parameters");
				System.out.println("validationFileName : tag file name containing the validation data");
				System.exit(0);
			} else {
				dataFileName = args[0];
				expFileName = args[1];
				if (args.length >= 3) {
					validationFileName = args[2];
				}
			}

			boolean runPCA;
			double explainedVariance = 1.0;
			double minGrowth;
			double minSigma;
			double confidentRange;
			int nLearningSteps, nLearningCycles;
			int nKernelsMV, nKernels;
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
			 * The number of kernels used for missing value estimation
			 * <nKernelsMV> int </nKernelsMV> (for each class)
			 * 
			 * The number of kernels used in classifier
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
			 * The maximum number of learning cycles in high level loop
			 * <nLearningCycles> int </nLearningCycles>
			 * 
			 * The confident range value for the classification function
			 * <confidentRange> double [0.0, 1.0 - 1.0/nClasses]</confidentRange>
			 * </experiment>
			 */

			debug("Reading experiment");
			TagReader exp = new TagReader(new FileReader(expFileName));
			exp.readStartTag("experiment");
			runPCA = exp.readBoolean("runPCA");
			if (runPCA)
				explainedVariance = exp.readDouble("explainedVariance");
			nKernelsMV = exp.readInt("nKernelsMV");
			nKernels = exp.readInt("nKernels");
			minSigma = exp.readDouble("minSigma");
			minGrowth = exp.readDouble("minGrowth");
			nLearningSteps = exp.readInt("nLearningSteps");
			nLearningCycles = exp.readInt("nLearningCycles");
			confidentRange = exp.readDouble("confidentRange");
			exp.readEndTag("experiment");
			exp.close();

			debug("Creating tag reader for data");
			TagReader tr = new TagReader(new FileReader(dataFileName));
			VectorReader[] readers = new VectorReader[4];

			debug("Creating periodic reader for labeled learning");
			readers[0] = new VectorUnknownPeriodReader(new TagVectorReader(tr, "learnL"), false);

			debug("Creating periodic reader for unlabeled learning");
			readers[1] = new VectorUnknownPeriodReader(new TagVectorReader(tr, "learnU"), false);

			debug("Creating periodic reader for testing");
			readers[2] = new VectorUnknownPeriodReader(new TagVectorReader(tr, "test"), false);

			// Validation data
			readers[3] = null;
			PrintWriter validationOutput = null;
			if (validationFileName != null) {
				debug("Creating periodic reader for validation");
				readers[3] =
						new VectorUnknownPeriodReader(new TagVectorReader(new TagReader(new FileReader(
								validationFileName)), "p1Validation"), false);
				debug("Creating print writer for validation output");
				validationOutput = new PrintWriter(new FileWriter(validationFileName + ".out"));
			}

			// _______________________________________________________________________

			debug("-  MISSING VALUE ESTIMATION  -");

			debug("Creating class demultiplexer");
			/*
			 * Creates a demultiplexer on the learning data for dispatching vectors
			 * according to their class
			 */
			VectorDemultiplexer classDpx = new P1VectorDemultiplexer(new VectorManualPeriodReader(readers[0], false));

			nClasses = classDpx.nStreams();
			nFeatures = classDpx.getVectorReader(0).size();
			double[][] mu = new double[nFeatures][2];
			double[] sigma = new double[nFeatures];
			fillMuSigma(mu, sigma); // Initializes mu and sigma

			/*
			 * Array for missing value substitution
			 * missingVal[i][0] = missing value code of feature i
			 * missingVal[i][1] = missing value subsitute
			 */
			double[][] missingVal = new double[nFeatures][2];
			fillMissingVal(missingVal); // Initializes the array

			/*
			 * Creates a univariate kernel distribution for each feature
			 * and then runs the EM algorithm
			 */
			debug("Creating mixture model classifier");
			MixtureModelClassifier mmc =
					new MixtureModelClassifier(nClasses, nFeatures, nKernelsMV, null, mu, sigma, missingVal, 1.0, "P1");
			debug("Creating mixture model learning");
			MixtureModelLearning mml =
					new MixtureModelLearning(mmc, classDpx, minGrowth, minSigma, true, nLearningSteps, false);

			debug("Estimating missing values...");
			mml.run();
			missingVal = getMissingVal(mmc);
			// System.out.println(mmc);

			double[] mean = null;
			double[] stdDev = null;
			if (!runPCA) {
				debug("Computes mean and standard deviation");
				VectorReader[] tmp = new VectorReader[3];
				tmp[0] = new VectorReplace(new P1VectorClassSizeAdapter(readers[0]), missingVal);
				tmp[1] = new VectorReplace(readers[1], missingVal);
				tmp[2] = (readers[3] != null ? new VectorReplace(readers[3], missingVal) : null);
				VectorReader tmp1 = new VectorUnionReader(tmp);
				mean = computeMean(tmp1);
				stdDev = computeStdDev(tmp1, mean);

			}

			// ________________________________________________________________________

			// Shows how to run a principal component analysis on the learning data
			// before building the classifier.
			// It is not usefull for problem P1.

			double[] eval = null;
			double[][] evectors = null;
			int nComp = 0;
			if (runPCA) {

				debug("-  PCA  -");

				debug("Creating reader for PCA");
				VectorReader pcaReader;
				{
					VectorReader[] tmp = new VectorReader[3];
					tmp[0] = new VectorReplace(new P1VectorClassSizeAdapter(readers[0]), missingVal);
					tmp[1] = new VectorReplace(readers[1], missingVal);
					tmp[2] = (readers[3] != null ? new VectorReplace(readers[3], missingVal) : null);
					pcaReader = new VectorUnionReader(tmp);
				}

				debug("Computes mean and standard deviation");
				mean = computeMean(pcaReader);
				stdDev = computeStdDev(pcaReader, mean);

				debug("Creating PCA");
				PCA pca = new PCA(new VectorNormalize(pcaReader, mean, stdDev));
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

				pcaReader = null;
				pca = null;

				nFeatures = nComp;
			}

			// ________________________________________________________________________

			debug("-  BUILDING CLASSIFIER  -");

			// Creates learning periodic reader
			VectorManualPeriodReader learningReader;
			{
				VectorReader[] tmp = new VectorReader[2];
				tmp[0] = readers[0];
				tmp[1] = new UnlabeledSource(readers[1], UNKNOWN_CLASS_CODE);
				if (runPCA)
					learningReader =
							new VectorManualPeriodReader(new VectorPCATransform(new VectorNormalize(new VectorReplace(
									new VectorUnionReader(tmp), missingVal), mean, stdDev), nComp, evectors, eval),
									false);
				else
					learningReader =
							new VectorManualPeriodReader(new VectorNormalize(new VectorReplace(new VectorUnionReader(
									tmp), missingVal), mean, stdDev), false);
			}

			// Creates testing periodic reader
			VectorReader testingReader;
			if (runPCA)
				testingReader =
						new VectorPCATransform(new VectorNormalize(new VectorReplace(readers[2], missingVal), mean,
								stdDev), nComp, evectors, eval);
			else
				testingReader = new VectorNormalize(new VectorReplace(readers[2], missingVal), mean, stdDev);

			// Creates validation reader
			VectorSniffer sniffer = null;
			VectorReader validationReader = null;
			if (readers[3] != null) {
				sniffer = new VectorSniffer(readers[3]);
				if (runPCA)
					validationReader =
							new VectorPCATransform(new VectorNormalize(new VectorReplace(sniffer, missingVal), mean,
									stdDev), nComp, evectors, eval);
				else
					validationReader = new VectorNormalize(new VectorReplace(sniffer, missingVal), mean, stdDev);
			}

			debug("Creating mixture model classifier");
			MultMixtureModelClassifier mmmc =
					new MultMixtureModelClassifier(nClasses, nFeatures, nKernels, null, 0.0, 0.0, 1.0, "P1");

			// Graphs output
			VectorWriter herOut = new VectorPrinter(new PrintWriter(new FileWriter("hardErrorRate.data")), 2);
			VectorWriter uerOut = new VectorPrinter(new PrintWriter(new FileWriter("unknownErrorRate.data")), 2);
			VectorWriter urOut = new VectorPrinter(new PrintWriter(new FileWriter("unknownRate.data")), 2);
			VectorWriter frOut = new VectorPrinter(new PrintWriter(new FileWriter("flipRate.data")), 2);

			// Biplots the data to be learnt
			{
				VectorReader[] tmp = new VectorReader[2];
				tmp[0] = new VectorSimplePeriodReader(learningReader, learningReader.period(), false);
				tmp[1] = testingReader;

				biplot(new P1VectorClassSizeAdapter(new VectorUnionReader(tmp)), "firstPlot");
				learningReader.reset();
			}

			// Learning loop
			int[] error;
			int known = readers[0].period();
			for (int i = 0; i < nLearningCycles; i++) {

				debug("Creating class demultiplexer");
				classDpx = new P1VectorDemultiplexer(new UnknownClassFilter(learningReader, UNKNOWN_CLASS_CODE));

				debug("Creating mixture model learning process");
				MultMixtureModelLearning mmml =
						new MultMixtureModelLearning(mmmc, classDpx, minGrowth, minSigma, nLearningSteps, true);

				debug("Learning...");
				mmml.run();

				learningReader.reset();

				// Computes the learning error
				error = testingError(mmmc, confidentRange, learningReader);
				double hardErrorRate = (double) error[1] / (double) known;
				double unknownErrorRate = (double) error[2] / (double) known;
				double unknownRate = (double) error[3] / (double) error[0];
				{
					double[] tmp = new double[2];
					tmp[0] = i;
					tmp[1] = hardErrorRate;
					herOut.write(tmp);
					tmp[1] = unknownErrorRate;
					uerOut.write(tmp);
					tmp[1] = unknownRate;
					urOut.write(tmp);
				}

				learningReader.reset();
				known = error[0] - error[3];
			} // end of learning loop
			herOut.close();
			uerOut.close();
			urOut.close();
			frOut.close(); // Closes the graphs

			debug("Testing...");
			testingError(mmmc, confidentRange, testingReader);

			if (validationReader != null) {
				debug("Classifier validation");
				validation(mmmc, confidentRange, validationReader, sniffer, validationOutput);
				validationReader.close();
				validationOutput.close();
			}

			// Final biplot
			{
				VectorReader[] tmp = new VectorReader[2];
				tmp[0] = new VectorSimplePeriodReader(learningReader, learningReader.period(), false);
				tmp[1] = testingReader;

				biplot(mmmc, new P1VectorClassSizeAdapter(new VectorUnionReader(tmp)), "plot");
			}

			// Outputs the classifier
			debug("Classifier :");
			System.out.println(mmmc);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// UTILITIES

	private static final int UNKNOWN_CLASS_CODE = -1;

	// This vector reader takes a none labeled vector and appends
	// an UNKNOWN_CLASS_CODE at the end.
	private static class UnlabeledSource implements VectorReader {
		VectorReader in;
		final double unknownClassCode;

		public UnlabeledSource(VectorReader in, double unknownClassCode) {
			this.in = in;
			this.unknownClassCode = unknownClassCode;
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
			double[] v1, v2;
			v1 = in.read();
			v2 = new double[size()];

			for (int i = 0; i < v1.length; i++) {
				v2[i] = v1[i];
			}
			v2[v1.length] = unknownClassCode;
			return v2;
		}

		/** Returns the stream size (vector size) */
		@Override
		public int size() {
			return in.size() + 1;
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
	}

	// This vector reader filters all none labeled vectors
	protected static class UnknownClassFilter implements VectorReader {

		VectorReader in;
		final double unknownClassCode;

		/** Builds a new unknown class filter on the underlying vector reader */
		public UnknownClassFilter(VectorReader in, double unknownClassCode) {
			this.in = in;
			this.unknownClassCode = unknownClassCode;
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
			while (isClassUnknown(v)) {
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

		boolean isClassUnknown(double[] v) {
			return (v[v.length - 1] == unknownClassCode);
		}
	}

	// Returns the class index which has the maximum probability and a probability
	// greater than 1/nClasses + confidentRange
	private static int classIndex(double[] v, double confidentRange) {
		int maxIndex = 0, n = 0;

		for (int i = 0; i < v.length; i++) {
			if (v[i] > v[maxIndex])
				maxIndex = i;
			if (v[i] > 0.5 + confidentRange)
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
		VectorClassSizeAdapter testingData = new P1VectorClassSizeAdapter(snif);
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

	// Validates the classifier
	// Classifies the validation vectors and prints the output in a file
	// in the NIPS format : http://q.cis.uoguelph.ca/~skremer/NIPS2000
	private static void validation(Classifier c, double confidentRange, VectorReader validationData,
			VectorSniffer sniffer, PrintWriter validationOutput) throws IOException {

		double[] v1, v2;
		int index, unknown = 0;
		for (int i = 0; i < validationData.period(); i++) {
			v1 = validationData.read();
			printVector(validationOutput, sniffer.lastVector());
			v2 = c.classify(v1);
			index = classIndex(v2, confidentRange);
			if (classIndex(v2, confidentRange) == UNKNOWN_CLASS_CODE) {
				unknown++;
			}
			validationOutput.println(index);
		}
		System.err.println("Unknown: " + unknown);
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

	private static void printVector(PrintWriter out, double[] v) {
		for (double element : v) {
			out.print(element);
			out.print(" ");
		}
	}

	private static void fillMuSigma(double[][] mu, double sigma[]) {
		for (int i = 0; i < mu.length; i++) {
			mu[i][0] = 0.0;
			mu[i][1] = 0.0;
			sigma[i] = 1.0;
		}
	}

	private static void fillMissingVal(double[][] missingVal) {
		missingVal[0][0] = -1.0;
		missingVal[0][1] = -1.0;
		for (int i = 1; i < missingVal.length; i++) {
			missingVal[i][0] = 0.0; // Missing value code
			missingVal[i][1] = 0.0; // Missing value substitute
		}
	}

	private static double[][] getMissingVal(MixtureModelClassifier mmc) {
		int nClasses = mmc.nClasses();
		int nFeatures = mmc.nFeatures();
		double[][] missingVal = new double[nFeatures][2];

		for (int i = 0; i < nFeatures; i++) {
			missingVal[i][1] = 0.0;
		}

		for (int i = 0; i < nClasses; i++) {
			MixtureModelClassifier.ClassModel cm = mmc.getClassModel(i);
			for (int j = 0; j < nFeatures; j++) {
				NormalKernelPdf pdf = cm.getFeaturePdf(j);
				missingVal[j][0] = pdf.missingValCode;
				missingVal[j][1] += pdf.missingValSubst;
			}
		}

		for (int i = 0; i < nFeatures; i++) {
			missingVal[i][1] /= nClasses;
		}
		return missingVal;
	}

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

	private static void biplot(VectorClassSizeAdapter data, String fileName) {
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
				out[i].close();
			}
			out[nClasses].close();
		} catch (Exception e) {
			e.printStackTrace();
		}
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
			e.printStackTrace();
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
