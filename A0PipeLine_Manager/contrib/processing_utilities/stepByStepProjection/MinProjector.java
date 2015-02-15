package processing_utilities.stepByStepProjection;

public class MinProjector implements IProjector {

	private double min = Double.MAX_VALUE;

	private int nEntries = 0;

	@Override
	public final void add(double f) {
		nEntries++;
		if (f < min)
			min = f;
	}

	@Override
	public final double project() {
		return min;
	}

	@Override
	public int getNPoints() {
		return nEntries;
	}

}
