package pipeline.stats.epfl.io;

import java.io.IOException;

/**
 * A size adapter for a vector reader.
 * If the underlying vector reader size is smaller than the output size,
 * the vectors are completed with 0.0 else there are truncated.
 *
 * @author Camille Weber <camille.weber@epfl.ch>
 */
public class VectorSizeAdapter implements VectorReader {

	private VectorReader in;
	private final int size;

	/**
	 * Creates a new size adapter
	 *
	 * @param in
	 *            the underlying vector reader
	 * @param size
	 *            the output size (vector size)
	 */
	public VectorSizeAdapter(VectorReader in, int size) {
		if (size <= 0)
			throw new IllegalArgumentException("size must be positive");
		this.size = size;
		this.in = in;
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
		double[] d1, d2;
		int n;

		if (in == null)
			throw new IOException("stream is closed");
		d1 = in.read();
		d2 = new double[size];
		n = Math.min(in.size(), size);

		for (int i = 0; i < n; i++) {
			d2[i] = d1[i];
		}
		for (int i = n; i < size; i++) {
			d2[i] = 0.0;
		}
		return d2;
	}

	/** Returns the stream size (vector size) */
	@Override
	public int size() {
		return size;
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
