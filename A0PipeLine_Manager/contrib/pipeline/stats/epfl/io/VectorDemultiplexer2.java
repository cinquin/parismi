package pipeline.stats.epfl.io;

import java.io.IOException;

/**
 * A double vector demultiplexer. A vector read in input stream is redirected
 * in the streams indexed by the multiple classify method.
 * Subclasses must override the multiple classify method.
 *
 * @author Camille Weber <camille.weber@epfl.ch>
 */
public abstract class VectorDemultiplexer2 extends VectorDemultiplexer {

	/**
	 * Creates a new demultiplexer on the underlying vector reader
	 *
	 * @param in
	 *            the underlying vector reader
	 * @param nStreams
	 *            the number of streams getting out
	 */
	protected VectorDemultiplexer2(VectorReader in, int nStreams) {
		super(in, nStreams);
	}

	// Utilities

	/**
	 * Classifies a vector
	 *
	 * @param v
	 *            the vector to classify
	 * @return an array of stream indexes in range 0..nStreams-1 to redirect the vector.
	 */
	protected abstract int[] multClassify(double[] v);

	@Override
	protected int classify(double[] v) {
		return multClassify(v)[0];
	}

	@Override
	protected synchronized void classifyNext() throws IOException {
		double[] d;
		int[] index;

		d = in.read();
		index = multClassify(d);
		for (int element : index) {
			if (out[element] != null)
				out[element].stream.addLast(d);
		}
	}
}
