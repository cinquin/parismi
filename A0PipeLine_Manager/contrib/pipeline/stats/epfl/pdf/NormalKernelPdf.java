package pipeline.stats.epfl.pdf;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Vector;

import pipeline.stats.epfl.io.TagReader;
import pipeline.stats.epfl.io.TagWriter;
import pipeline.stats.epfl.io.VectorReader;

/**
 * A probability density function based on normal kernels
 *
 * @author Camille Weber <camille.weber@epfl.ch>
 */
public class NormalKernelPdf extends AbstractPdf {

	/** A normal kernel class */
	static public class NormalKernel extends NormalPdf {
		/** Kernel weight */
		public double weight;

		public NormalKernel(double weight, double mu, double sigma) {
			super(mu, sigma);
			this.weight = weight;
		}
	}

	private Vector<NormalKernel> kernels;
	private String name;

	/** Missing value code */
	public double missingValCode;
	/** Missing value substitute */
	public double missingValSubst;

	/**
	 * Creates a new pdf with normal kernels
	 *
	 * @param in
	 *            the underlying vector reader
	 * @param nKernels
	 *            the initial number of kernels <br>
	 *            The kernel mu parameter is set to a random value between minMu and maxMu;
	 *            the sigma parameters are equal to sigma and the weight value is equal to 1.0/nKernels
	 * @param name
	 *            the pdf name
	 */
	private NormalKernelPdf(VectorReader in, int nKernels, double minMu, double maxMu, double sigma, String name,
			double missingValCode, double missingValSubst) {

		super(in);
		double weight = (nKernels > 0 ? 1.0 / nKernels : 0.0);

		kernels = new Vector<>();
		for (int i = 0; i < nKernels; i++) {
			kernels.add(new NormalKernel(weight, minMu + (maxMu - minMu) * Math.random(), sigma));
		}
		if (name == null) {
			this.name = "NormalKernelPdf";
		} else {
			this.name = name;
		}

		this.missingValCode = missingValCode;
		this.missingValSubst = missingValSubst;
	}

	/** Creates a new pdf without stream connection */
	public NormalKernelPdf(int nKernels, double minMu, double maxMu, double sigma, String name, double missingValCode,
			double missingValSubst) {
		this(null, nKernels, minMu, maxMu, sigma, name, missingValCode, missingValSubst);
	}

	/**
	 * Creates a new pdf with normal kernels read in a tag reader
	 *
	 * @param config
	 *            the tag reader to configure the kernels
	 *            //*@param tag the tag name enclosing the kernels parameters <br>
	 *            The file format must be : <br>
	 *            &lt;tag&gt; <br>
	 *            &nbsp;&nbsp;&lt;missingValCode&gt; double &lt;/missingValCode&gt; <br>
	 *            &nbsp;&nbsp;&lt;missingValSubst&gt;double &lt;/missingValSubst&gt; <br>
	 *            &nbsp;&nbsp;&lt;nKernels&gt; int &lt;/nKernels&gt; <br>
	 *            &nbsp;&nbsp;&lt;kernels&gt; <br>
	 *            &nbsp;&nbsp;&nbsp;{double (weight) double (mu) double (sigma) } nKernels <br>
	 *            &nbsp;&nbsp;&lt;/kernels&gt; <br>
	 *            &lt;/tag&gt;
	 * @param in
	 *            the underlying vector reader
	 * @exception IOException
	 *                if there is an error in file format
	 */
	private NormalKernelPdf(TagReader config, String name, VectorReader in) throws IOException {
		super(in);

		int nKernels;
		double weight, mu, sigma;

		this.in = in;
		this.name = name;
		kernels = new Vector<>();

		config.readStartTag(name);
		missingValCode = config.readDouble("missingValCode");
		missingValSubst = config.readDouble("missingValSubst");
		nKernels = config.readInt("nKernels");
		config.readStartTag("kernels");
		for (int i = 0; i < nKernels; i++) {
			weight = config.readDouble();
			mu = config.readDouble();
			sigma = config.readDouble();
			kernels.add(new NormalKernel(weight, mu, sigma));
		}
		config.readEndTag("kernels");
		config.readEndTag(name);
	}

	/** Creates a new pdf without stream connection */
	public NormalKernelPdf(TagReader config, String name) throws IOException {
		this(config, name, null);
	}

	/** Probability density function */
	@Override
	public double pdf(double x_) {
		double y = 0.0;
		NormalKernel k;

		double x = (x_ != missingValCode ? x_ : missingValSubst);

		for (NormalKernel kernel : kernels) {
			k = kernel;
			y = y + k.weight * k.pdf(x);
		}
		return y;
	}

	/** Returns the kernel at the given index */
	public NormalKernel getKernel(int index) {
		return kernels.elementAt(index);
	}

	/**
	 * Adds a new normal kernel
	 *
	 * @param weight
	 *            the kernel weight
	 */
	public void addKernel(double weight, double mu, double sigma) {
		kernels.add(new NormalKernel(weight, mu, sigma));
	}

	/** Returns the number of kernels */
	public int nKernels() {
		return kernels.size();
	}

	/** Checks if the kernel weights are positive and sum to 1.0 */
	public boolean areKernelWeightsValid() {
		double y = 0.0;
		NormalKernel k;

		for (NormalKernel kernel : kernels) {
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
		Iterator<NormalKernel> it = kernels.iterator();
		NormalKernel k;

		out.printIndent(indent);
		out.printStartTagln(name);
		indent.inc();
		out.printIndent(indent);
		out.printTagln("missingValCode", missingValCode);
		out.printIndent(indent);
		out.printTagln("missingValSubst", missingValSubst);
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
			out.print(k.mu);
			out.print(' ');
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
