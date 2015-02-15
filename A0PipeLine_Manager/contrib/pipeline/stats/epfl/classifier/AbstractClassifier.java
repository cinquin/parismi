package pipeline.stats.epfl.classifier;

import java.io.IOException;
import java.io.StringWriter;

import pipeline.stats.epfl.io.TagWriter;
import pipeline.stats.epfl.io.VectorReader;

/**
 * The root class for all classifiers.
 * Subclasses must override the classification function.
 *
 * @author Camille Weber <camille.weber@epfl.ch>
 */
public abstract class AbstractClassifier implements Classifier {

	private VectorReader in;

	/**
	 * Creates a new classifier
	 *
	 * @param in
	 *            the underlying vector reader
	 */
	AbstractClassifier(VectorReader in) {
		this.in = in;
	}

	/** Creates a new classifier without stream connection */
	AbstractClassifier() {
		this(null);
	}

	/** Classification function */
	@Override
	public abstract double[] classify(double[] features);

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
		return classify(v);
	}

	/** Returns the stream size (vector size), same as nClasses */
	@Override
	public int size() {
		return nClasses();
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

	/** Returns the number of classes */
	@Override
	public abstract int nClasses();

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

	/** Returns a string representation for this classifier */
	@Override
	public String toString() {
		StringWriter sw = new StringWriter();
		TagWriter tw = new TagWriter(sw);
		tagWrite(tw);
		tw.close();
		return sw.toString();
	}

	/** Writes a tag representation for this classifier in a tag writer */
	protected abstract void tagWrite(TagWriter out, TagWriter.Indent _indent);

	/** Writes a tag representation for this classifier in a tag writer */
	void tagWrite(TagWriter out) {
		tagWrite(out, null);
	}
}
