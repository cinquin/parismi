package processing_utilities.floyd_warshall;

import java.util.ArrayList;
import java.util.Arrays;

import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;

// From http://algowiki.net/wiki/index.php?title=Floyd-Warshall's_algorithm

public class FloydWarshall {

	private double[][] D;
	private IFWNode[][] P;

	public void calcShortestPaths(IFWNode[] FWNodes, IFWEdge[] FWEdges) {
		D = initializeWeight(FWNodes, FWEdges);
		P = new IFWNode[FWNodes.length][FWNodes.length];
		Utils.log("calcShortestPaths with " + FWNodes.length + " nodes", LogLevel.VERBOSE_VERBOSE_DEBUG);
		for (int k = 0; k < FWNodes.length; k++) {
			for (int i = 0; i < FWNodes.length; i++) {
				for (int j = 0; j < FWNodes.length; j++) {
					if (D[i][k] != Double.MAX_VALUE && D[k][j] != Double.MAX_VALUE && D[i][k] + D[k][j] < D[i][j]) {
						D[i][j] = D[i][k] + D[k][j];
						P[i][j] = FWNodes[k];
					}
				}
			}
		}
		Utils.log("Returning shortest path computed with " + FWNodes.length + " nodes", LogLevel.VERBOSE_VERBOSE_DEBUG);

	}

	public double getShortestDistance(IFWNode source, IFWNode target) {
		return D[source.getIndex()][target.getIndex()];
	}

	public ArrayList<IFWNode> getShortestPath(IFWNode source, IFWNode target) {
		if (D[source.getIndex()][target.getIndex()] == Double.MAX_VALUE) {
			return new ArrayList<>();
		}
		ArrayList<IFWNode> path = getIntermediatePath(source, target);
		path.add(0, source);
		path.add(target);
		return path;
	}

	private ArrayList<IFWNode> getIntermediatePath(IFWNode source, IFWNode target) {
		if (D == null) {
			throw new IllegalStateException("Must call calcShortestPaths(...) before attempting to obtain a path.");
		}
		if (P[source.getIndex()][target.getIndex()] == null) {
			return new ArrayList<>();
		}
		ArrayList<IFWNode> path = new ArrayList<>();
		path.addAll(getIntermediatePath(source, P[source.getIndex()][target.getIndex()]));
		path.add(P[source.getIndex()][target.getIndex()]);
		path.addAll(getIntermediatePath(P[source.getIndex()][target.getIndex()], target));
		return path;
	}

	private static double[][] initializeWeight(IFWNode[] FWNodes, IFWEdge[] FWEdges) {
		double[][] weights = new double[FWNodes.length][FWNodes.length];
		for (int i = 0; i < FWNodes.length; i++) {
			Arrays.fill(weights[i], Double.MAX_VALUE);
		}
		for (IFWEdge e : FWEdges) {
			weights[e.getFrom().getIndex()][e.getTo().getIndex()] = e.getWeight();
		}
		return weights;
	}
}