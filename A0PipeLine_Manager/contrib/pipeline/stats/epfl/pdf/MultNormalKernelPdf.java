package pipeline.stats.epfl.pdf;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Vector;

import pipeline.stats.epfl.io.TagReader;
import pipeline.stats.epfl.io.TagWriter;

/**
 * A probability density function based on multivariate normal kernels
 *
 * @author Camille Weber <camille.weber@epfl.ch>
 */
public class MultNormalKernelPdf {

	/** A normal kernel class */
	static public class MultNormalKernel extends MultNormalPdf {
		/** Kernel weight */
		public double weight;

		public MultNormalKernel(double weight, double[] mu, double sigma) {
			super(mu, sigma);
			this.weight = weight;
		}
	}

	private Vector<MultNormalKernel> kernels;
	private int dimension;
	private String name;

	/**
	 * Creates a new pdf with multivariate normal kernels
	 *
	 * @param dimension
	 *            the vector space dimension
	 * @param nKernels
	 *            the initial number of kernels <br>
	 *            The kernel mu parameter is set to a random value between minMu and maxMu;
	 *            the sigma parameters are equal to sigma and the weight value is equal to 1.0/nKernels
	 * @param name
	 *            the pdf name
	 */
	public MultNormalKernelPdf(int dimension, int nKernels, double minMu, double maxMu, double sigma, String name) {

		double weight = (nKernels > 0 ? 1.0 / nKernels : 0.0);
		double[] mu = new double[dimension];

		kernels = new Vector<>();
		for (int i = 0; i < nKernels; i++) {
			for (int j = 0; j < dimension; j++) {
				mu[j] = minMu + (maxMu - minMu) * Math.random();
			}
			kernels.add(new MultNormalKernel(weight, mu, sigma));
		}
		if (name == null) {
			this.name = "NormalKernelPdf";
		} else {
			this.name = name;
		}

		this.dimension = dimension;
	}

	/**
	 * Creates a new pdf with normal kernels read in a tag reader
	 *
	 * @param config
	 *            the tag reader to configure the kernels
	 *            //*@param tag the tag name enclosing the kernels parameters <br>
	 *            The file format must be : <br>
	 *            &lt;tag&gt; <br>
	 *            &nbsp;&nbsp;&lt;dimension&gt; int &lt;/dimension&gt; <br>
	 *            &nbsp;&nbsp;&lt;nKernels&gt; int &lt;/nKernels&gt; <br>
	 *            &nbsp;&nbsp;&lt;kernels&gt; <br>
	 *            &nbsp;&nbsp;&nbsp;{double (weight) {double (mu)}dimension double (sigma) } nKernels <br>
	 *            &nbsp;&nbsp;&lt;/kernels&gt; <br>
	 *            &lt;/tag&gt;
	 * @exception IOException
	 *                if there is an error in file format
	 */
	public MultNormalKernelPdf(TagReader config, String name) throws IOException {
		int nKernels;
		double weight, sigma;
		double[] mu;

		this.name = name;
		kernels = new Vector<>();

		config.readStartTag(name);
		dimension = config.readInt("dimension");
		mu = new double[dimension];
		nKernels = config.readInt("nKernels");
		config.readStartTag("kernels");
		for (int i = 0; i < nKernels; i++) {
			weight = config.readDouble();
			for (int j = 0; j < dimension; j++) {
				mu[j] = config.readDouble();
			}
			sigma = config.readDouble();
			kernels.add(new MultNormalKernel(weight, mu, sigma));
		}
		config.readEndTag("kernels");
		config.readEndTag(name);
	}

	/** Probability density function */
	public double pdf(double[] x) {
		double y = 0.0;
		MultNormalKernel k;

		for (MultNormalKernel kernel : kernels) {
			k = kernel;
			y = y + k.weight * k.pdf(x);
		}
		return y;
	}

	/** Returns the vector space dimension */
	public int dimension() {
		return dimension;
	}

	/** Returns the kernel at the given index */
	public MultNormalKernel getKernel(int index) {
		return kernels.elementAt(index);
	}

	/**
	 * Adds a new normal kernel
	 *
	 * @param weight
	 *            the kernel weight
	 */
	public void addKernel(double weight, double[] mu, double sigma) {
		if (mu.length != dimension)
			throw new IllegalArgumentException("mu dimension must equal the vector space dimension");
		kernels.add(new MultNormalKernel(weight, mu, sigma));
	}

	/** Returns the number of kernels */
	public int nKernels() {
		return kernels.size();
	}

	/** Checks if the kernel weights are positive and sum to 1.0 */
	public boolean areKernelWeightsValid() {
		double y = 0.0;
		MultNormalKernel k;

		for (MultNormalKernel kernel : kernels) {
			k = kernel;
			if (k.weight < 0.0)
				return false;
			y = y + k.weight;
		}
		return (y == 1.0);
	}

	/** Returns a string representation for this pdf */
	@Override
	public String toString() {
		StringWriter sw = new StringWriter();
		TagWriter tw = new TagWriter(sw);
		tagWrite(tw);
		tw.close();
		return sw.toString();
	}

	/** Writes a tag representation for this pdf in a tag writer */
	public void tagWrite(TagWriter out, TagWriter.Indent _indent) {
		TagWriter.Indent indent = (_indent == null ? new TagWriter.Indent() : _indent);
		Iterator<MultNormalKernel> it = kernels.iterator();
		MultNormalKernel k;

		out.printIndent(indent);
		out.printStartTagln(name);
		indent.inc();
		out.printIndent(indent);
		out.printTagln("dimension", dimension);
		out.printIndent(indent);
		out.printTagln("nKernels", kernels.size());
		out.printIndent(indent);
		out.printStartTagln("kernels");
		indent.inc();
		while (it.hasNext()) {
			k = it.next();
			out.printIndent(indent);
			out.print(k.weight);
			out.print(' ');
			for (int i = 0; i < dimension; i++) {
				out.print(k.mu[i]);
				out.print(' ');
			}
			out.print(k.sigma);
			out.println();
		}
		indent.dec();
		out.printIndent(indent);
		out.printEndTagln("kernels");
		indent.dec();
		out.printIndent(indent);
		out.printEndTagln(name);
	}

	/** Writes a tag representation for this pdf in a tag writer */
	void tagWrite(TagWriter out) {
		tagWrite(out, null);
	}
}
