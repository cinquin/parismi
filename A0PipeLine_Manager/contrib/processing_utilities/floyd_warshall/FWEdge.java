package processing_utilities.floyd_warshall;

public class FWEdge implements IFWEdge {

	private IFWNode from, to;
	private double weight;

	public FWEdge(IFWNode from, IFWNode to, double d) {
		this.from = from;
		this.to = to;
		this.weight = d;
	}

	@Override
	public final IFWNode getFrom() {
		return from;
	}

	@Override
	public final IFWNode getTo() {
		return to;
	}

	@Override
	public final double getWeight() {
		return weight;
	}

}
