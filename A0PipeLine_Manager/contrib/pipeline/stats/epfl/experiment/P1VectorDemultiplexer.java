package pipeline.stats.epfl.experiment;

import java.io.IOException;

import pipeline.stats.epfl.io.VectorDemultiplexer;
import pipeline.stats.epfl.io.VectorReader;
import pipeline.stats.epfl.io.VectorSizeAdapter;

/**
 * A double vector demultiplexer for problem P1
 *
 * @author Camille Weber <camille.weber@epfl.ch>
 */
public class P1VectorDemultiplexer extends VectorDemultiplexer {

	private VectorReader[] outResized;
	private final int nFeatures;

	/**
	 * Creates a new demultiplexer on the underlying vector reader
	 *
	 * @param in
	 *            the underlying vector reader
	 */
	public P1VectorDemultiplexer(VectorReader in) {
		// Builds a new vector demultiplexer for 2 classes
		super(in, 2);
		this.nFeatures = in.size() - 1;
		outResized = new VectorSizeAdapter[2];
		outResized[0] = new VectorSizeAdapter(super.getVectorReader(0), nFeatures);
		outResized[1] = new VectorSizeAdapter(super.getVectorReader(1), nFeatures);
	}

	/**
	 * Returns a reference on the given vector reader
	 *
	 * @param index
	 *            0..nStreams-1
	 * @return null if the stream is closed
	 */
	@Override
	public VectorReader getVectorReader(int index) {
		if (index < 0 || index >= out.length)
			throw new IllegalArgumentException("index must be in range 0..1");
		return outResized[index];
	}

	// Utilities

	/**
	 * Classifies a vector
	 *
	 * @param v
	 *            the vector to classify
	 * @return a number in range 0..nStreams - 1
	 */
	@Override
	protected int classify(double[] v) {
		// The last vector component is the class index
		return (int) v[nFeatures];
	}

	@Override
	protected void closeStream(int index) throws IOException {
		outResized[index] = null;
		super.closeStream(index);
	}
}
