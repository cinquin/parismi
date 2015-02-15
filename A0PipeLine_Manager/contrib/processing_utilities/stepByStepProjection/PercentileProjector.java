package processing_utilities.stepByStepProjection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PercentileProjector implements IProjector {

	private float percentile;

	public PercentileProjector(float percentile) {
		this.percentile = percentile;
	}

	private List<Double> values = new ArrayList<>();
	private int nEntries = 0;

	@Override
	public void add(double f) {
		nEntries++;
		values.add(f);
	}

	@Override
	public double project() {
		Collections.sort(values);
		if (values.size() == 0)
			return Float.NaN;
		return (values.get((int) ((values.size() - 1) * (percentile / 100))));
	}

	@Override
	public int getNPoints() {
		return nEntries;
	}

}
