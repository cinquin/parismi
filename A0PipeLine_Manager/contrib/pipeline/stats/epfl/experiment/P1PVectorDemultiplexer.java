package pipeline.stats.epfl.experiment;

import java.io.IOException;

import pipeline.stats.epfl.io.VectorDemultiplexer2;
import pipeline.stats.epfl.io.VectorReader;

/**
 * A double vector demultiplexer for problem P1 with prior probabilities
 *
 * @author Camille Weber <camille.weber@epfl.ch>
 */
public class P1PVectorDemultiplexer extends VectorDemultiplexer2 {

	// Class which implements the vector readers getting out of the demultiplexer.
	protected class VectorReaderPImpl implements VectorReader {
		VectorReader in1;
		final int nClasses;
		final int index;

		public VectorReaderPImpl(VectorReader in, int nClasses, int index) {
			this.in1 = in;
			this.nClasses = nClasses;
			this.index = index;
		}

		@Override
		public double[] read() throws IOException {
			double[] v1, v2;
			if (in1 == null)
				throw new IOException("stream is closed");

			int n = size();
			v2 = new double[n];
			v1 = in1.read();

			for (int i = 0; i < n - 1; i++) {
				v2[i] = v1[i];
			}
			v2[n - 1] = v1[n - 1 + index];
			return v2;
		}

		@Override
		public int size() {
			if (in1 != null) {
				return in1.size() - nClasses + 1;
			} else {
				return 0;
			}
		}

		@Override
		public int period() {
			if (in1 != null) {
				return in1.period();
			} else {
				return 0;
			}
		}

		@Override
		public void close() throws IOException {
			if (in1 != null) {
				in1.close();
				in1 = null;
			}
		}
	}

	private VectorReader[] outResized;
	private final int nFeatures;

	/**
	 * Creates a new demultiplexer on the underlying vector reader
	 *
	 * @param in
	 *            the underlying vector reader
	 */
	public P1PVectorDemultiplexer(VectorReader in) {
		super(in, 2);
		this.nFeatures = in.size() - 2;
		outResized = new VectorReaderPImpl[2];
		outResized[0] = new VectorReaderPImpl(super.getVectorReader(0), 2, 0);
		outResized[1] = new VectorReaderPImpl(super.getVectorReader(1), 2, 1);
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
	 * @return an array of indexes in range 0..nStreams-1 to redirect the vector.
	 */
	@Override
	protected int[] multClassify(double[] v) {
		int[] index = new int[v.length - nFeatures];
		int[] out;
		int n = 0;

		// Redirects the vector in each class which has a probability > 0.0
		for (int i = 0; i < v.length - nFeatures; i++) {
			if (v[nFeatures + i] > 0.0) {
				index[n] = i;
				n++;
			}
		}
		out = new int[n];
		for (int i = 0; i < n; i++) {
			out[i] = index[i];
		}
		return out;
	}

	@Override
	protected void closeStream(int index) throws IOException {
		outResized[index] = null;
		super.closeStream(index);
	}
}
