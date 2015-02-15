package pipeline.stats.epfl.io;

import java.io.IOException;
import java.util.LinkedList;

/**
 * A double vector feature (component) separator.
 * There is a stream for each vector component.
 *
 * @author Camille Weber <camille.weber@epfl.ch>
 */
public class FeatureSeparator {

	private VectorReader in;
	private VectorReaderImpl[] out;
	private int openedStreams = 0;

	protected class VectorReaderImpl implements VectorReader {
		LinkedList<double[]> stream;
		final int index;

		VectorReaderImpl(int index) {
			this.index = index;
			stream = new LinkedList<>();
		}

		@Override
		public double[] read() throws IOException {
			if (stream == null)
				throw new IOException("stream is closed");
			if (stream.size() == 0) {
				splitNext();
			}
			return stream.removeFirst();
		}

		@Override
		public int size() {
			return 1;
		}

		@Override
		public int period() {
			return 0;
		}

		@Override
		public void close() throws IOException {
			if (stream != null) {
				stream.clear();
				stream = null;
				closeStream(index);
			}
		}

	}

	/**
	 * Creates a feature separator on the underlying vector reader
	 *
	 * @param in
	 *            the underlying vector reader
	 */
	public FeatureSeparator(VectorReader in) {
		int nFeatures = in.size();
		this.in = in;
		out = new VectorReaderImpl[nFeatures];
		for (int i = 0; i < nFeatures; i++) {
			out[i] = new VectorReaderImpl(i);
		}
		openedStreams = nFeatures;
	}

	/**
	 * Returns a reference on the given vector reader
	 *
	 * @param index
	 *            0..nStreams-1
	 * @return null if the stream is closed
	 */
	public VectorReader getVectorReader(int index) {
		if (index < 0 || index >= out.length)
			throw new IllegalArgumentException("index must be in range 0..nStreams");
		return out[index];
	}

	/** Returns the number of streams getting out. It equals the vector size. */
	public int nStreams() {
		return out.length;
	}

	// Utilities

	synchronized void splitNext() throws IOException {
		double[] d1;
		double[] d2;

		d1 = in.read();
		for (int i = 0; i < out.length; i++) {
			if (out[i] != null) {
				d2 = new double[1];
				d2[0] = d1[i];
				out[i].stream.addLast(d2);
			}
		}
	}

	void closeStream(int index) throws IOException {
		out[index] = null;
		openedStreams--;
		if (openedStreams == 0) {
			in.close();
			in = null;
		}
	}
}
