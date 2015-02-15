package processing_utilities.pcurves.Optimize;

public interface Optimizable {
	public void OptimizingStep(double step);

	public double GetCriterion();
}
