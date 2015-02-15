package pipeline.stats.epfl.io;

import java.io.IOException;

/**
 * A stream which is the union from a set of streams
 *
 * @author Camille Weber <camille.weber@epfl.ch>
 */
public class VectorUnionReader implements VectorReader {

	private VectorReader[] readers;
	private int currentReader;
	private int nRead;

	/** Creates a new union vector reader on the underlying set of streams */
	public VectorUnionReader(VectorReader[] readers) {
		this.readers = readers;
		currentReader = 0;
		nRead = 0;
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
		int n = getNextReader();
		if (nRead >= readers[currentReader].period()) {
			currentReader = n;
			nRead = 0;
		}

		double[] v;
		v = readers[currentReader].read();
		nRead++;
		return v;
	}

	/** Returns the stream size (vector size) */
	@Override
	public int size() {
		if (readers[currentReader] != null) {
			return readers[currentReader].size();
		} else {
			return 0;
		}
	}

	/**
	 * Closes the stream
	 *
	 * @exception IOException
	 *                if an IO error occurs
	 */
	@Override
	public void close() throws IOException {
		for (int i = 0; i < readers.length; i++) {
			if (readers[i] != null) {
				readers[i].close();
				readers[i] = null;
			}
		}
		currentReader = 0;
		nRead = 0;
	}

	/** Returns the stream period */
	@Override
	public int period() {
		int p = 0;

		for (VectorReader reader : readers) {
			if (reader != null) {
				p += reader.period();
			}
		}
		return p;
	}

	// Utilities

	private int getNextReader() throws IOException {
		int n = 0;
		int i = (currentReader + 1) % readers.length;

		while (readers[i] == null && n < readers.length) {
			i = (i + 1) % readers.length;
			n++;
		}
		if (n == readers.length) {
			throw new IOException("stream is closed");
		}
		return i;
	}
}
