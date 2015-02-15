package processing_utilities.stepByStepProjection;

public class MaxProjector implements IProjector {

	private double max = -Double.MAX_VALUE;
	private int nEntries = 0;

	@Override
	public final void add(double f) {
		nEntries++;
		if (f > max)
			max = f;
	}

	@Override
	public final double project() {
		return max;
	}

	@Override
	public int getNPoints() {
		return nEntries;
	}

}
