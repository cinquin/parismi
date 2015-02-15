package pipeline.stats.epfl.io;

import java.io.IOException;

/**
 * A vector writer plugged on a tag writer
 *
 * @author Camille Weber <camille.weber@epfl.ch>
 */
public class TagVectorWriter implements VectorWriter {

	private TagWriter out;
	private TagWriter.Indent indent;
	private final String streamName;
	private final int size;
	private final int capacity;
	private int nwritten;

	/**
	 * Creates a new VectorWriter on the underlying TagWriter
	 *
	 * @param out
	 *            the underlying TagWriter
	 * @param indent
	 *            indentation in stream
	 * @param streamName
	 *            the tag name enclosing the data to write
	 * @param size
	 *            the stream size (vector size)
	 * @param capacity
	 *            the stream capacity (number of vectors)
	 * @exception IOException
	 *                if an IO error occurs
	 */
	private TagVectorWriter(TagWriter out, String streamName, int size, int capacity, TagWriter.Indent indent)
			throws IOException {
		if (size < 0)
			throw new IllegalArgumentException("size must be positive");
		if (capacity < 0)
			throw new IllegalArgumentException("capacity must be positive");
		this.out = out;
		this.size = size;
		this.capacity = capacity;
		this.indent = indent;
		this.streamName = streamName;
		nwritten = 0;
		out.printIndent(indent);
		out.printStartTagln(streamName);
		indent.inc();
		out.printIndent(indent);
		out.printTagln("capacity", capacity);
		out.printIndent(indent);
		out.printTagln("size", size);
		out.printIndent(indent);
		out.printStartTagln("data");
		indent.inc();
		if (out.checkError())
			throw new IOException("error in underlying tag writer");
	}

	/**
	 * Creates a new VectorWriter on the underlying TagWriter
	 *
	 * @param out
	 *            the underlying TagWriter
	 * @param streamName
	 *            the tag name enclosing the data to write
	 * @param size
	 *            the stream size (vector size)
	 * @param capacity
	 *            the stream capacity (number of vectors)
	 * @exception IOException
	 *                if an IO error occurs
	 */
	public TagVectorWriter(TagWriter out, String streamName, int size, int capacity) throws IOException {
		this(out, streamName, size, capacity, new TagWriter.Indent());
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

		if (nwritten >= capacity)
			throw new IOException("capacity is overloaded");
		out.printIndent(indent);
		for (int i = 0; i < n; i++) {
			out.print(v[i]);
			out.print(' ');
		}
		for (int i = 0; i < size - n; i++) {
			out.print(0.0);
			out.print(' ');
		}
		out.println();
		nwritten++;
		if (out.checkError())
			throw new IOException("error in underlying tag writer");
	}

	/**
	 * Returns the stream size (vector size)
	 */
	@Override
	public int size() {
		return size;
	}

	/** Returns the stream capacity (number of vectors) */
	public int capacity() {
		return capacity;
	}

	/** Returns the number of vectors actually written */
	public int nWritten() {
		return nwritten;
	}

	/**
	 * Flushes the stream
	 *
	 * @exception IOException
	 *                if an IO error occurs
	 */
	@Override
	public void flush() throws IOException {
		if (out != null)
			out.flush();
		if (out.checkError())
			throw new IOException("error in underlying tag writer");
	}

	/**
	 * Closes the stream
	 *
	 * @warning the underlying TagWriter is not closed
	 * @exception IOException
	 *                if an IO error occurs
	 */
	@Override
	public void close() throws IOException {
		if (out != null) {
			indent.dec();
			out.printIndent(indent);
			out.printEndTagln("data");
			indent.dec();
			out.printIndent(indent);
			out.printEndTagln(streamName);
			out.flush();
			if (out.checkError())
				throw new IOException("error in underlying tag writer");
			out = null;
			indent = null;
		}
	}
}
