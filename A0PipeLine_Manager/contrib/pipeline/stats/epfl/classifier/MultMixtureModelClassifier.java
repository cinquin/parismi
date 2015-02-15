package pipeline.stats.epfl.classifier;

import java.io.IOException;
import java.io.StringWriter;

import pipeline.stats.epfl.io.TagReader;
import pipeline.stats.epfl.io.TagWriter;
import pipeline.stats.epfl.io.VectorReader;
import pipeline.stats.epfl.pdf.MultNormalKernelPdf;

/**
 * A classifier based on multivariate mixture models
 *
 * @author Camille Weber <camille.weber@epfl.ch>
 */
public class MultMixtureModelClassifier extends AbstractClassifier {

	public static class MultClassModel {

		/** The class prior probability (weight) */
		public double priorProb;

		MultNormalKernelPdf featuresPdf;
		String name;

		/**
		 * Creates a new class model
		 *
		 * @param priorProb
		 *            the prior class probability
		 * @param dimension
		 *            the vector space dimension
		 * @param nKernels
		 *            the initial number of kernels <br>
		 *            The kernel mu parameter is set to a random value between minMu and maxMu;
		 *            the sigma parameters are equal to sigma and the weight value is equal to 1.0/nKernels
		 * @param name
		 *            the class model's name
		 */
		public MultClassModel(double priorProb, int dimension, int nKernels, double minMu, double maxMu, double sigma,
				String name) {
			if (dimension <= 0)
				throw new IllegalArgumentException("dimension must be positive");
			if (nKernels < 0)
				throw new IllegalArgumentException("nKernels must be positive or null");

			this.priorProb = priorProb;
			featuresPdf = new MultNormalKernelPdf(dimension, nKernels, minMu, maxMu, sigma, "featuresPdf");
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
		 *            &nbsp;&nbsp;&lt;featuresPdf&gt; <br>
		 *            &nbsp;&nbsp;&nbsp;&nbsp;NormalKernelPdf <br>
		 *            &nbsp;&nbsp;&lt;/featuresPdf&gt; <br>
		 *            &lt;/name&gt;
		 *
		 * @exception IOException
		 *                if there is an error in file format
		 */
		public MultClassModel(TagReader config, String name) throws IOException {
			// int nFeatures;

			this.name = name;

			config.readStartTag(name);
			priorProb = config.readDouble("priorProb");
			featuresPdf = new MultNormalKernelPdf(config, "featuresPdf");
			config.readEndTag(name);
		}

		/** Returns the associated feature pdf */
		public MultNormalKernelPdf getFeaturesPdf() {
			return featuresPdf;
		}

		/** Returns vector space dimension */
		public int dimension() {
			return featuresPdf.dimension();
		}

		/** The class pdf */
		public double pdf(double[] features) {
			return featuresPdf.pdf(features);
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

			out.printIndent(indent);
			out.printStartTagln(name);
			indent.inc();
			out.printIndent(indent);
			out.printTagln("priorProb", priorProb);
			featuresPdf.tagWrite(out, indent);
			indent.dec();
			out.printIndent(indent);
			out.printEndTagln(name);
		}

		public void tagWrite(TagWriter out) {
			tagWrite(out, null);
		}
	}

	private MultClassModel[] classModels;
	private String name;
	private final int dimension;

	/**
	 * Creates a mixture model classifier with stream connection
	 *
	 * @param nClasses
	 *            the number of classes
	 * @param dimension
	 *            the vector space dimension
	 * @param priorProb
	 *            the prior class distribution (if null then p = 1/nClasses)
	 * @param nKernels
	 *            the initial number of kernels <br>
	 *            The kernel mu parameter is set to a random value between minMu and maxMu;
	 *            the sigma parameters are equal to sigma and the weight value is equal to 1.0/nKernels
	 * @param name
	 *            the classifier's name
	 * @param in
	 *            the underlying vector reader
	 */
	private MultMixtureModelClassifier(int nClasses, int dimension, int nKernels, double[] priorProb, double minMu,
			double maxMu, double sigma, String name, VectorReader in) {
		super(in);
		if (nClasses <= 0)
			throw new IllegalArgumentException("nClasses must be positive");
		if (dimension <= 0)
			throw new IllegalArgumentException("dimension must be positive");
		if (nKernels < 0)
			throw new IllegalArgumentException("nKernels must be positive or null");
		if (priorProb != null) {
			if (priorProb.length != nClasses)
				throw new IllegalArgumentException("number of prior prob must equal nClasses or be null");
		}
		if (in != null) {
			if (dimension != in.size())
				throw new IllegalArgumentException("underlying stream size and dimension must be equal");
		}

		double p;

		this.dimension = dimension;
		classModels = new MultClassModel[nClasses];
		for (int k = 0; k < nClasses; k++) {
			p = (priorProb == null ? 1.0 / nClasses : priorProb[k]);
			classModels[k] = new MultClassModel(p, dimension, nKernels, minMu, maxMu, sigma, "class" + k);
		}
		if (name == null) {
			this.name = "MultMixtureModelClassifier";
		} else {
			this.name = name;
		}
	}

	/** Creates a new mixture model classifier without stream connection */
	public MultMixtureModelClassifier(int nClasses, int dimension, int nKernels, double[] priorProb, double minMu,
			double maxMu, double sigma, String name) {
		this(nClasses, dimension, nKernels, priorProb, minMu, maxMu, sigma, name, null);
	}

	/**
	 * Creates a new mixture model classifier with parameters read in a file
	 *
	 * @param config
	 *            the configuration file
	 *            //*@param tag the tag name enclosing the parameters <br>
	 *            The file format must be : <br>
	 *            &lt;name> <br>
	 *            &nbsp;&nbsp;&lt;nClasses&gt; int &lt;/nClasses&gt; <br>
	 *            &nbsp;&nbsp;{ &lt;classi&gt; <br>
	 *            &nbsp;&nbsp;&nbsp;&nbsp;ClassModel <br>
	 *            &nbsp;&nbsp;&lt;/classi&gt;} i in range 0..nClasses-1 <br>
	 *            &lt;name&gt;
	 * @param in
	 *            the underlying vector reader
	 * @exception IOException
	 *                if there is an error in file format
	 */
	private MultMixtureModelClassifier(TagReader config, String name, VectorReader in) throws IOException {
		super(in);

		int nClasses;

		this.name = name;
		config.readStartTag(name);
		nClasses = config.readInt("nClasses");
		if (nClasses <= 0)
			throw new IOException("nClasses must be positive");
		classModels = new MultClassModel[nClasses];
		for (int k = 0; k < nClasses; k++) {
			classModels[k] = new MultClassModel(config, "class" + k);
		}
		config.readEndTag(name);
		dimension = classModels[0].dimension();
		if (in != null) {
			if (dimension != in.size())
				throw new IOException("underlying stream size and nFeatures must be equal");
		}
	}

	/** Creates a new mixture model classifier without stream connection */
	public MultMixtureModelClassifier(TagReader config, String name) throws IOException {
		this(config, name, null);
	}

	/**
	 * Returns the model of the given class
	 *
	 * @param index
	 *            in range 0..nClasses-1
	 */
	public MultClassModel getClassModel(int index) {
		return classModels[index];
	}

	/** Returns the number of classes */
	@Override
	public int nClasses() {
		return classModels.length;
	}

	/** Checks if the class models prior probability are positive and sum to 1.0 */
	public boolean areClassModelWeightsValid() {
		double y = 0.0;

		for (MultClassModel classModel : classModels) {
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

		if (features.length != dimension)
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

	/** Returns the vector space dimension */
	public int dimension() {
		return dimension;
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
		for (MultClassModel classModel : classModels) {
			classModel.tagWrite(out, indent);
		}
		indent.dec();
		out.printIndent(indent);
		out.printEndTagln(name);
	}
}
