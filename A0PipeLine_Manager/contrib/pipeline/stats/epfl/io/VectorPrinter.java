package pipeline.stats.epfl.io;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * A vector writer plugged on a PrintWriter. One vector per line is written.
 *
 * @author Camille Weber <camille.weber@epfl.ch>
 */
public class VectorPrinter implements VectorWriter {

	private PrintWriter out;
	private final int size;

	/**
	 * Creates a new vector printer
	 *
	 * @param out
	 *            the underlying print writer
	 * @param size
	 *            the output stream size (vector size)
	 */
	public VectorPrinter(PrintWriter out, int size) {
		if (size <= 0)
			throw new IllegalArgumentException("size must be positive");
		this.out = out;
		this.size = size;
	}

	/**
	 * Writes a vector in stream
	 *
	 * @param v
	 *            vector to write; the vector is truncated if necessary,
	 *            or completed with 0.0
	 * @exception IOException
	 *                if an IO error occurs
	 */
	@Override
	public void write(double[] v) throws IOException {
		int n = Math.min(v.length, size);

		if (out == null)
			throw new IOException("stream is closed");
		for (int i = 0; i < n; i++) {
			out.print(v[i]);
			out.print(' ');
		}
		for (int i = n; i < size; i++) {
			out.print(0.0);
			out.print(' ');
		}
		out.println();
		if (out.checkError())
			throw new IOException("error in underlying writer");
	}

	/** Returns the stream size (vector size) */
	@Override
	public int size() {
		return size;
	}

	/**
	 * Flushes the stream
	 *
	 * @exception IOException
	 *                if an IO error occurs
	 */
	@Override
	public void flush() throws IOException {
		if (out == null)
			throw new IOException("stream is closed");
		out.flush();
		if (out.checkError())
			throw new IOException("error in underlying writer");
	}

	/**
	 * Closes the stream
	 *
	 * @exception IOException
	 *                if an IO error occurs
	 */
	@Override
	public void close() throws IOException {
		if (out != null) {
			out.close();
			if (out.checkError())
				throw new IOException("error in underlying writer");
			out = null;
		}
	}
}
