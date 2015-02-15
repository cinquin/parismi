package pipeline.stats.epfl.pdf;

import java.io.IOException;

import pipeline.stats.epfl.io.VectorReader;
import pipeline.stats.epfl.io.VectorWriter;

/**
 * The root class for all probability density functions
 *
 * @author Camille Weber <camille.weber@epfl.ch>
 */
public abstract class AbstractPdf implements Pdf {

	VectorReader in;

	/**
	 * Creates a new pdf
	 *
	 * @param in
	 *            the underlying vector reader
	 */
	AbstractPdf(VectorReader in) {
		this.in = in;
	}

	/** Creates a new pdf without stream connection */
	public AbstractPdf() {
		this(null);
	}

	/** Probability density function */
	@Override
	public abstract double pdf(double x);

	/**
	 * Plots the pdf in a vector writer
	 *
	 * @param out
	 *            the vector writer size must be 2, out[0] = x and out[1] = pdf(x)
	 * @exception IOException
	 *                if an IO error occurs
	 */
	@Override
	public void plot(double xMin, double xMax, int nPoints, VectorWriter out) throws IOException {
		if (nPoints <= 1)
			throw new IllegalArgumentException("nPoints must be greater than 1");

		double dx = (xMax - xMin) / (nPoints - 1);
		double[] v = new double[2];

		for (int i = 0; i < nPoints; i++) {
			v[0] = xMin + i * dx;
			v[1] = pdf(v[0]);
			out.write(v);
		}
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

		if (in == null)
			throw new IOException("stream is closed");
		v = in.read();
		for (int i = 0; i < in.size(); i++) {
			v[i] = pdf(v[i]);
		}
		return v;
	}

	/** Returns the stream size (vector size) */
	@Override
	public int size() {
		if (in != null) {
			return in.size();
		} else {
			return 1;
		}
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
	 *                if an IO error occurs
	 */
	@Override
	public void close() throws IOException {
		if (in != null) {
			in.close();
			in = null;
		}
	}

}
