package pipeline.stats.epfl.io;

/**
 * A double vector manual periodic reader.
 * Period vectors can be read and then the reader must be reset.
 * The vectors can be shuffled at any time.
 *
 * @author Camille Weber <camille.weber@epfl.ch>
 */
public class VectorShufflePeriodReader extends VectorManualPeriodReader {

	/**
	 * Creates a new periodic reader
	 *
	 * @param in
	 *            the underlying vector reader
	 * @param makePeriodicCopy
	 *            flag saying if on each period a new vector is built
	 */
	public VectorShufflePeriodReader(VectorReader in, boolean makePeriodicCopy) {
		super(in, makePeriodicCopy);
	}

	/**
	 * Creates a new periodic reader. Each period a new vector is built
	 *
	 * @param in
	 *            the underlying vector reader
	 */
	public VectorShufflePeriodReader(VectorReader in) {
		super(in);
	}

	/** Shuffles the vectors */
	public void shuffle() {
		int period = period();
		int j;
		double[] tmp;

		for (int i = 0; i < period; i++) {
			j = (int) (Math.random() * (period - i - 1));
			tmp = vectors.get(i);
			vectors.set(i, vectors.get(j));
			vectors.set(j, tmp);
		}
	}
}
