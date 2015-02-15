package pipeline.stats.epfl.io;

import java.io.IOException;

/**
 * A vector reader plugged on a tag reader.
 *
 * @author Camille Weber <camille.weber@epfl.ch>
 */
public class TagVectorReader implements VectorReader {

	private TagReader in;
	private final String streamName;
	private final int size;
	private final int capacity;
	private int nread;

	/**
	 * Creates a new VectorReader from the underlying TagReader
	 *
	 * @param in
	 *            the underlying TagReader
	 * @param streamName
	 *            the tag name enclosing the data to read
	 * @exception IOException
	 *                if the data format is not : <br>
	 *                &lt;streamName&gt; <br>
	 *                &nbsp;&nbsp;&lt;capacity> int &lt;/capacity&gt; <br>
	 *                &nbsp;&nbsp;&lt;size&gt; int &lt;/size&gt; <br>
	 *                &nbsp;&nbsp;&lt;data&gt; {double} size * capacity &lt;/data&gt; <br>
	 *                &lt;/streamName&gt;
	 */
	public TagVectorReader(TagReader in, String streamName) throws IOException {
		this.streamName = streamName;
		this.in = in;
		in.readStartTag(streamName);
		capacity = in.readInt("capacity");
		size = in.readInt("size");
		in.readStartTag("data");
		nread = 0;
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
		double[] buf = new double[size];

		if (in == null)
			throw new IOException("stream is closed");
		if (nread >= capacity)
			throw new IOException("capacity is overloaded");

		for (int i = 0; i < size; i++) {
			buf[i] = in.readDouble();
		}
		nread++;
		return buf;
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
		return 0;
	}

	/** Returns the stream capacity (number of vectors) */
	public int capacity() {
		return capacity;
	}

	/** Returns the number of vectors actually read */
	public int nRead() {
		return nread;
	}

	/**
	 * Closes the stream
	 *
	 * @warning the underlying TagReader is not closed
	 * @exception IOException
	 *                if the end tag streamName doesn't exist
	 */
	@Override
	public void close() throws IOException {
		if (in != null) {
			in.readEndTag("data");
			in.readEndTag(streamName);
			in = null;
		}
	}
}
