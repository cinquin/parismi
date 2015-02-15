package processing_utilities.stepByStepProjection;

public class SumProjector extends AverageProjector {

	@Override
	public double project() {
		return sum;
	}

}
