package processing_utilities.stepByStepProjection;

public interface IProjector {
	public void add(double f);

	public double project();

	public int getNPoints();
}
