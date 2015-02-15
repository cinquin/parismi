package processing_utilities.floyd_warshall;

public interface IFWEdge {

	public IFWNode getFrom();

	public IFWNode getTo();

	public double getWeight();

}
