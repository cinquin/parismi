package pipeline.stats.epfl.classifier;

import java.io.IOException;
import java.io.StringWriter;

import pipeline.stats.epfl.io.TagReader;
import pipeline.stats.epfl.io.TagWriter;
import pipeline.stats.epfl.io.VectorReader;
import pipeline.stats.epfl.pdf.AbstractPdf;
import pipeline.stats.epfl.pdf.NormalKernelPdf;
import pipeline.stats.epfl.pdf.Pdf;

/**
 * A classifier based on mixture models
 *
 * @author Camille Weber <camille.weber@epfl.ch>
 */
public class MixtureModelClassifier extends AbstractClassifier {

	public static class ClassModel {

		/** The class prior probability (weight) */
		public double priorProb;

		/**
		 * The recognition precision [0.0, 1.0]
		 * When evaluating the class pdf, takes only the first n most important
		 * features. Where n = recognition*nFeatures.
		 */
		public double recognition;

		NormalKernelPdf[] featuresPdf;
		String name;

		/**
		 * Creates a new class model
		 *
		 * @param priorProb
		 *            the prior class probability
		 * @param nFeatures
		 *            the number of feature for the class (input vector size)
		 * @param nKernels
		 *            the initial number of kernels for each feature
		 * @param mu
		 *            mu[f][0] = min Mu; mu[f][1] = max Mu for the feature f kernels <br>
		 *            f in range 0..nFeatures-1
		 * @param sigma
		 *            sigma[f] the sigma value for the feature f kernels <br>
		 *            f in range 0..nFeatures-1
		 * @param missingVal
		 *            missingValue[f][0] = missing value code;
		 *            missingValue[f][1] = missing value substitute; <br>
		 *            f in range 0..nFeatures;
		 * @param recognition
		 *            the recognition precision in range 0..1
		 * @param name
		 *            the class model's name
		 */
		public ClassModel(double priorProb, int nFeatures, int nKernels, double[][] mu, double[] sigma,
				double[][] missingVal, double recognition, String name) {
			if (nFeatures <= 0)
				throw new IllegalArgumentException("nFeatures must be positive");
			if (nKernels < 0)
				throw new IllegalArgumentException("nKernels must be positive or null");
			if (mu.length != nFeatures)
				throw new IllegalArgumentException("number of mu initial values must equal nFeatures");
			if (sigma.length != nFeatures)
				throw new IllegalArgumentException("number of sigma initial values must equal nFeatures");
			if (missingVal.length != nFeatures)
				throw new IllegalArgumentException("number of missing values code and substitute must equal nFeatures");
			if (recognition < 0.0 || recognition > 1.0)
				throw new IllegalArgumentException("recognition precision must be in range 0..1");

			this.priorProb = priorProb;
			this.recognition = recognition;
			featuresPdf = new NormalKernelPdf[nFeatures];
			for (int i = 0; i < nFeatures; i++) {
				featuresPdf[i] =
						new NormalKernelPdf(nKernels, mu[i][0], mu[i][1], sigma[i], "feature" + i, missingVal[i][0],
								missingVal[i][1]);
			}
			if (name == null) {
				name = "ClassModel";
			} else {
				this.name = name;
			}
		}

		/**
		 * Creates a new Class model with parameters read in a file
		 *
		 * @param config
		 *            the configuration file
		 * @param name
		 *            the tag name enclosing the parameters <br>
		 *            The file format must be : <br>
		 *            &lt;name&gt; <br>
		 *            &nbsp;&nbsp;&lt;priorProb&gt; double &lt;/priorProb&gt; <br>
		 *            &nbsp;&nbsp;&lt;recognition&gt; double &lt;/recognition&gt; <br>
		 *            &nbsp;&nbsp;&lt;nFeatures&gt;int &lt;/nFeatures&gt; <br>
		 *            &nbsp;&nbsp;{ &lt;featurei&gt; <br>
		 *            &nbsp;&nbsp;&nbsp;&nbsp;NormalKernelPdf <br>
		 *            &nbsp;&nbsp;&lt;/featurei&gt; } i in range 0..nFeatures-1 <br>
		 *            &lt;/name&gt;
		 *
		 * @exception IOException
		 *                if there is an error in file format
		 */
		public ClassModel(TagReader config, String name) throws IOException {
			int nFeatures;

			this.name = name;

			config.readStartTag(name);
			priorProb = config.readDouble("priorProb");
			recognition = config.readDouble("recognition");
			nFeatures = config.readInt("nFeatures");
			if (nFeatures <= 0)
				throw new IOException("nFeatures must be positive");
			featuresPdf = new NormalKernelPdf[nFeatures];
			for (int i = 0; i < nFeatures; i++) {
				featuresPdf[i] = new NormalKernelPdf(config, "feature" + i);
			}
			config.readEndTag(name);
		}

		/** Returns the pdf associated to the given feature index 0..nFeatures-1 */
		public NormalKernelPdf getFeaturePdf(int index) {
			return featuresPdf[index];
		}

		/** Returns the number of features (input vector size) */
		public int nFeatures() {
			return featuresPdf.length;
		}

		/** The class pdf */
		public double pdf(double[] features) {
			int nF = featuresPdf.length;
			double[] pF = new double[nF];

			if (features.length != nF)
				throw new IllegalArgumentException("the number of features must equal the input vector size");
			for (int i = 0; i < nF; i++) {
				pF[i] = featuresPdf[i].pdf(features[i]);
				if (pF[i] < 0.0001)
					pF[i] = 0.0;
			}
			java.util.Arrays.sort(pF);

			double p = 1.0;
			int ignore = (int) (nF * (1.0 - recognition));
			for (int i = nF - 1; i >= ignore; i--) {
				p *= pF[i];
			}
			return p;
		}

		/** Returns a string representation for this class model */
		@Override
		public String toString() {
			StringWriter sw = new StringWriter();
			TagWriter tw = new TagWriter(sw);
			tagWrite(tw);
			tw.close();
			return sw.toString();
		}

		/** Writes a tag representation for this class model in a tag writer */
		public void tagWrite(TagWriter out, TagWriter.Indent _indent) {
			TagWriter.Indent indent = (_indent == null ? new TagWriter.Indent() : _indent);
			int nF = featuresPdf.length;

			out.printIndent(indent);
			out.printStartTagln(name);
			indent.inc();
			out.printIndent(indent);
			out.printTagln("priorProb", priorProb);
			out.printIndent(indent);
			out.printTagln("recognition", recognition);
			out.printIndent(indent);
			out.printTagln("nFeatures", nF);
			for (NormalKernelPdf aFeaturesPdf : featuresPdf) {
				aFeaturesPdf.tagWrite(out, indent);
			}
			indent.dec();
			out.printIndent(indent);
			out.printEndTagln(name);
		}

		public void tagWrite(TagWriter out) {
			tagWrite(out, null);
		}
	}

	/**
	 * Object which represents p(class k | feature i),
	 * it extends the AbstractPdf class to export the plot function.
	 */
	protected class FeatureClassifier extends AbstractPdf {
		final int classIndex;
		final int featureIndex;

		FeatureClassifier(int classIndex, int featureIndex) {
			super();
			if (classIndex < 0 || classIndex >= nClasses())
				throw new IndexOutOfBoundsException("invalid classIndex");
			if (featureIndex < 0 || featureIndex >= nFeatures)
				throw new IndexOutOfBoundsException("invalid featureIndex");
			this.classIndex = classIndex;
			this.featureIndex = featureIndex;
		}

		@Override
		public double pdf(double x) {
			double pCF; // p(class k | feature)
			double[] pFC; // p(feature | class k) * p(class k)
			double sumPFC; // sum on all classes (pFc)
			int nC = nClasses();

			pFC = new double[nC];
			sumPFC = 0.0;
			for (int k = 0; k < nC; k++) {
				// Warning : the pdf is used and not the real p(feature | class k)
				pFC[k] = classModels[k].getFeaturePdf(featureIndex).pdf(x) * classModels[k].priorProb;
				sumPFC += pFC[k];
			}

			pCF = pFC[classIndex] / sumPFC; // Bayes rule
			if (Double.isNaN(pCF)) {
				pCF = 1.0 / nC;
			}

			return pCF;
		}
	}

	private ClassModel[] classModels;
	private String name;
	private final int nFeatures;

	/**
	 * Creates a mixture model classifier with stream connection
	 *
	 * @param nClasses
	 *            the number of classes
	 * @param nFeatures
	 *            the number of feature for a class (input vector size)
	 * @param nKernels
	 *            the initial number of kernels for each feature
	 * @param priorProb
	 *            the prior class distribution (if null then p = 1/nClasses)
	 * @param mu
	 *            mu[f][0] = min Mu; mu[f][1] = max Mu for the feature f kernels <br>
	 *            f in range 0..nFeatures-1
	 * @param sigma
	 *            sigma[f] the sigma value for the feature f kernels <br>
	 *            f in range 0..nFeatures-1
	 * @param missingVal
	 *            missingValue[f][0] = missing value code;
	 *            missingValue[f][1] = missing value substitute; <br>
	 *            f in range 0..nFeatures;
	 * @param recognition
	 *            the recognition precision in range 0..1
	 * @param name
	 *            the classifier's name
	 * @param in
	 *            the underlying vector reader
	 */
	private MixtureModelClassifier(int nClasses, int nFeatures, int nKernels, double[] priorProb, double[][] mu,
			double[] sigma, double[][] missingVal, double recognition, String name, VectorReader in) {
		super(in);
		if (nClasses <= 0)
			throw new IllegalArgumentException("nClasses must be positive");
		if (nFeatures <= 0)
			throw new IllegalArgumentException("nFeatures must be positive");
		if (nKernels < 0)
			throw new IllegalArgumentException("nKernels must be positive or null");
		if (mu.length != nFeatures)
			throw new IllegalArgumentException("number of mu initial values must equal nFeatures");
		if (sigma.length != nFeatures)
			throw new IllegalArgumentException("number of sigma initial values must equal nFeatures");
		if (missingVal.length != nFeatures)
			throw new IllegalArgumentException("number of missing values code and substitute must equal nFeatures");
		if (priorProb != null) {
			if (priorProb.length != nClasses)
				throw new IllegalArgumentException("number of prior prob must equal nClasses or be null");
		}
		if (in != null) {
			if (nFeatures != in.size())
				throw new IllegalArgumentException("underlying stream size and nFeatures must be equal");
		}

		double p;

		this.nFeatures = nFeatures;
		classModels = new ClassModel[nClasses];
		for (int k = 0; k < nClasses; k++) {
			p = (priorProb == null ? 1.0 / nClasses : priorProb[k]);
			classModels[k] = new ClassModel(p, nFeatures, nKernels, mu, sigma, missingVal, recognition, "class" + k);
		}
		if (name == null) {
			this.name = "MixtureModelClassifier";
		} else {
			this.name = name;
		}
	}

	/** Creates a new mixture model classifier without stream connection */
	public MixtureModelClassifier(int nClasses, int nFeatures, int nKernels, double[] priorProb, double[][] mu,
			double[] sigma, double[][] missingVal, double recognition, String name) {
		this(nClasses, nFeatures, nKernels, priorProb, mu, sigma, missingVal, recognition, name, null);
	}

	/**
	 * Creates a new mixture model classifier with parameters read in a file
	 *
	 * @param config
	 *            the configuration file
	 *            // *@param tag the tag name enclosing the parameters <br>
	 *            The file format must be : <br>
	 *            &lt;name&gt; <br>
	 *            &nbsp;&nbsp;&lt;nClasses&gt; int &lt;/nClasses&gt; <br>
	 *            &nbsp;&nbsp;{ &lt;classi&gt; <br>
	 *            &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;ClassModel <br>
	 *            &nbsp;&nbsp;&lt;/classi&gt; } i in range 0..nClasses-1 <br>
	 *            &lt;/name&gt;
	 * @param in
	 *            the underlying vector reader
	 * @exception IOException
	 *                if there is an error in file format
	 */
	private MixtureModelClassifier(TagReader config, String name, VectorReader in) throws IOException {
		super(in);

		int nClasses;

		this.name = name;
		config.readStartTag(name);
		nClasses = config.readInt("nClasses");
		if (nClasses <= 0)
			throw new IOException("nClasses must be positive");
		classModels = new ClassModel[nClasses];
		for (int k = 0; k < nClasses; k++) {
			classModels[k] = new ClassModel(config, "class" + k);
		}
		config.readEndTag(name);
		nFeatures = classModels[0].nFeatures();
		if (in != null) {
			if (nFeatures != in.size())
				throw new IOException("underlying stream size and nFeatures must be equal");
		}
	}

	/** Creates a new mixture model classifier without stream connection */
	public MixtureModelClassifier(TagReader config, String name) throws IOException {
		this(config, name, null);
	}

	/**
	 * Returns the model of the given class
	 *
	 * @param index
	 *            in range 0..nClasses-1
	 */
	public ClassModel getClassModel(int index) {
		return classModels[index];
	}

	/** Returns a pdf which represents p(class k | feature i) */
	public Pdf getFeatureClassifier(int classIndex, int featureIndex) {
		return new FeatureClassifier(classIndex, featureIndex);
	}

	/** Returns the number of classes */
	@Override
	public int nClasses() {
		return classModels.length;
	}

	/** Returns the number of features */
	public int nFeatures() {
		return nFeatures;
	}

	/** Checks if the class models prior probability are positive and sum to 1.0 */
	public boolean areClassModelWeightsValid() {
		double y = 0.0;

		for (ClassModel classModel : classModels) {
			if (classModel.priorProb < 0.0)
				return false;
			y = y + classModel.priorProb;
		}
		return (y == 1.0);
	}

	/** Classification function */
	@Override
	public double[] classify(double[] features) {
		double[] pCF; // p(class k | features)
		double[] pFC; // p(features | class k) * p(class k)
		double sumPFC; // sum on all classes (pFc)
		int nC = nClasses();

		if (features.length != nFeatures)
			throw new IllegalArgumentException("the number of features must equal the input vector size");

		pFC = new double[nC];
		sumPFC = 0.0;
		for (int k = 0; k < nC; k++) {
			// Warning : the pdf is used and not the real p(features | class k)
			pFC[k] = classModels[k].pdf(features) * classModels[k].priorProb;
			sumPFC += pFC[k];
		}

		pCF = new double[nC];
		for (int k = 0; k < nC; k++) {
			pCF[k] = pFC[k] / sumPFC; // Bayes rule
			if (Double.isNaN(pCF[k])) {
				pCF[k] = 1.0 / nC;
			}
		}

		return pCF;
	}

	/** Writes a tag representation for this classifier in a tag writer */
	@Override
	public void tagWrite(TagWriter out, TagWriter.Indent _indent) {
		TagWriter.Indent indent = (_indent == null ? new TagWriter.Indent() : _indent);
		int nC = classModels.length;

		out.printIndent(indent);
		out.printStartTagln(name);
		indent.inc();
		out.printIndent(indent);
		out.printTagln("nClasses", nC);
		for (ClassModel classModel : classModels) {
			classModel.tagWrite(out, indent);
		}
		indent.dec();
		out.printIndent(indent);
		out.printEndTagln(name);
	}
}
