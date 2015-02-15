package pipeline.stats.epfl.io;

import java.io.IOException;
import java.util.LinkedList;

/**
 * A double vector demultiplexer. The input stream is redirected to
 * a specific reader according to the classify method.
 * Subclasses must override the classify method.
 *
 * @author Camille Weber <camille.weber@epfl.ch>
 */
public abstract class VectorDemultiplexer {

	VectorReader in;
	protected VectorReaderImpl[] out;
	private int openedStreams;

	protected class VectorReaderImpl implements VectorReader {
		LinkedList<double[]> stream;
		final int size;
		final int index;

		VectorReaderImpl(int size, int index) {
			this.size = size;
			this.index = index;
			stream = new LinkedList<>();
		}

		@Override
		public double[] read() throws IOException {
			if (stream == null)
				throw new IOException("stream is closed");
			while (stream.size() == 0) {
				classifyNext();
			}
			return stream.removeFirst();
		}

		@Override
		public int size() {
			return size;
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
	 * Creates a new demultiplexer on the underlying vector reader
	 *
	 * @param in
	 *            the underlying vector reader
	 * @param nStreams
	 *            the number of streams getting out
	 */
	protected VectorDemultiplexer(VectorReader in, int nStreams) {
		int s = in.size();

		if (nStreams <= 0)
			throw new IllegalArgumentException("nStreams must be positive");
		this.in = in;
		out = new VectorReaderImpl[nStreams];
		for (int i = 0; i < nStreams; i++) {
			out[i] = new VectorReaderImpl(s, i);
		}
		openedStreams = nStreams;
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
			throw new IllegalArgumentException("index must be in range 0..1");
		return out[index];
	}

	/** Returns the number of streams getting out */
	public int nStreams() {
		return out.length;
	}

	// Utilities

	/**
	 * Classifies a vector
	 *
	 * @param v
	 *            the vector to classify
	 * @return a number in range 0..nStreams - 1
	 */
	protected abstract int classify(double[] v);

	synchronized void classifyNext() throws IOException {
		double[] d;
		int index;

		d = in.read();
		index = classify(d);
		if (out[index] != null)
			out[index].stream.addLast(d);
	}

	protected void closeStream(int index) throws IOException {
		out[index] = null;
		openedStreams--;
		if (openedStreams == 0) {
			in.close();
			in = null;
		}
	}
}
