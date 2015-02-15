package processing_utilities.stepByStepProjection;

public class AverageProjector implements IProjector {

	double sum = 0;
	private int nEntries = 0;

	@Override
	public final void add(double f) {
		nEntries++;
		sum += f;
	}

	@Override
	public double project() {
		return (float) (sum / nEntries);
	}

	@Override
	public int getNPoints() {
		return nEntries;
	}

}
