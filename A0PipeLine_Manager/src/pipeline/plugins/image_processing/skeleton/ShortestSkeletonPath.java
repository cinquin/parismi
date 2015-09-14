//Adapted from ImageJ/Fiji
package pipeline.plugins.image_processing.skeleton;

import ij.IJ;
import ij.measure.Calibration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import pipeline.PreviewType;
import pipeline.GUI_utils.ListOfPointsView;
import pipeline.GUI_utils.PluginIOHyperstackViewWithImagePlus;
import pipeline.GUI_utils.PluginIOView;
import pipeline.data.ClickedPoint;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOList;
import pipeline.data.IPluginIOStack;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOCells;
import pipeline.data.PluginIOHyperstack;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.misc_util.Point3D;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.MultiListParameter;
import pipeline.parameters.TableParameter;
import pipeline.plugins.AuxiliaryInputOutputPlugin;
import pipeline.plugins.FourDPlugin;
import processing_utilities.floyd_warshall.FWEdge;
import processing_utilities.floyd_warshall.FloydWarshall;
import processing_utilities.skeleton.Edge;
import processing_utilities.skeleton.SkeletonGraph;
import processing_utilities.skeleton.SkeletonPoint;
import processing_utilities.skeleton.Vertex;

/**
 * Main class of the ImageJ/Fiji plugin for skeleton analysis.
 * This class is a plugin for the ImageJ and Fiji interfaces for analyzing
 * 2D/3D skeleton images.
 * <p>
 * For more detailed information, visit the AnalyzeSkeleton home page: <A target="_blank"
 * href="http://pacific.mpi-cbg.de/wiki/index.php/AnalyzeSkeleton"
 * >http://pacific.mpi-cbg.de/wiki/index.php/AnalyzeSkeleton</A>
 *
 *
 * @version 01/12/2010
 * @author seems to be Huub Hovens; see
 *         http://imagejconf.tudor.lu/_media/archive/imagej-user-and-developer-conference-2010
 *         /presentations/polder/imagej_conference_2010.pdf
 * @author Olivier Cinquin (adapted code to run in pipeline)
 */
public class ShortestSkeletonPath extends FourDPlugin implements AuxiliaryInputOutputPlugin {

	@Override
	public String getToolTip() {
		return "Identify subset of skeleton that corresponds to shortest path between extremes" + "";
	}

	@Override
	public List<PluginIOView> createOutput(String outputName, PluginIOHyperstackViewWithImagePlus impForDisplay,
			Map<String, IPluginIO> linkedOutputs) throws InterruptedException {
		Utils.log("Creating image", LogLevel.DEBUG);
		initializeOutputs();
		@SuppressWarnings("unchecked")
		IPluginIOList<SkeletonGraph> inputs = (IPluginIOList<SkeletonGraph>) pluginInputs.get("Cells");
		if (inputs.size() == 0)
			throw new IllegalStateException("Input has 0 cells");
		PluginIOHyperstack output =
				new PluginIOHyperstack("Shortest skeleton path", inputs.get(0).getImageWidth(), inputs.get(0)
						.getImageHeight(), inputs.get(0).getImageDepth(), 1, 1, PixelType.BYTE_TYPE, false);
		output.setCalibration((Calibration) inputs.get(0).getCalibration().clone());
		PluginIOHyperstackViewWithImagePlus view = new PluginIOHyperstackViewWithImagePlus("Shortest skeleton path");
		output.setImp(view);
		view.addImage(output);
		pluginOutputs.put("Shortest skeleton path", output);

		PluginIOCells cells = new PluginIOCells();
		ListOfPointsView<ClickedPoint> view2 = new ListOfPointsView<>(cells);
		cells.setCalibration((Calibration) inputs.get(0).getCalibration().clone());
		view2.setData(cells);
		pluginOutputs.put("Cells", cells);
		cells.addQuantifiedPropertyName("geodesicDistance");
		ArrayList<PluginIOView> views = new ArrayList<>();
		views.add(view);
		views.add(view2);

		return views;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void run(ProgressReporter r, MultiListParameter inChannels, TableParameter outChannels,
			PreviewType previewType, boolean inputHasChanged, AbstractParameter parameterWhoseValueChanged,
			boolean stayInCoreLoop) {

		IPluginIOStack stack = ((PluginIOHyperstack) getOutput()).getChannels().entrySet().iterator().next().getValue();

		stack.computePixelArray();
		stack.clearPixels();

		for (SkeletonGraph s : (IPluginIOList<SkeletonGraph>) pluginInputs.get("Cells")) {
			warshallAlgorithm(s, stack, (PluginIOCells) pluginOutputs.get("Cells"));
		}

	}

	@Override
	public int getFlags() {
		return PARALLELIZE_WITH_NEW_INSTANCES + NEED_INPUT_LOCKED;
	}

	@Override
	public String operationName() {
		return "Shortest skeleton path";
	}

	@Override
	public String version() {
		return "1.0";
	}

	/** end point flag */
	public static final byte END_POINT = 30;
	/** junction flag */
	public static final byte JUNCTION = 70;
	/** slab flag */
	public static final byte SLAB = 127;
	/** shortest path flag */
	private static final byte SHORTEST_PATH = 96;

	private static final byte VISITED_SHORTEST_PATH = 97;

	@Override
	public Map<String, InputOutputDescription> getInputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		result.put("Default source", new InputOutputDescription(null, null, new PixelType[] { PixelType.FLOAT_TYPE,
				PixelType.BYTE_TYPE }, InputOutputDescription.NOT_SPECIFIED, InputOutputDescription.NOT_SPECIFIED,
				false, false));
		return result;
	}

	@Override
	public Map<String, InputOutputDescription> getOutputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		result.put("Default destination", new InputOutputDescription(null, null,
				new PixelType[] { PixelType.BYTE_TYPE }, InputOutputDescription.KEEP_IN_RAM,
				InputOutputDescription.SAME_XY_MATCH_SELECTED_CHANNELS, true, false));

		InputOutputDescription desc0 = new InputOutputDescription(null, null, null, 0, 0, true, false);
		desc0.name = "Cells";
		desc0.pluginWillAllocateOutputItself = true;
		result.put("Cells", desc0);

		return result;
	}

	/**
	 * Determine the longest shortest path using the APSP (all pairs shortest path)
	 * warshall algorithm
	 * 
	 * @param graph
	 *            the graph of a tree
	 * @return longest shortest path length
	 * @author Huub Hovens
	 */
	private static double
			warshallAlgorithm(SkeletonGraph graph, IPluginIOStack outputImage, PluginIOCells outputPoints) {
		// local fields
		/** vertex 1 of an edge */
		Vertex v1 = null;
		/** vertex 2 of an edge */
		Vertex v2 = null;
		/** the equivalent of row in a matrix */
		int row = 0;
		/** the equivalent of column in a matrix */
		int column = 0;
		/** the value of the longest shortest path */
		double maxPath = 0;
		/** row that contains the longest shortest path value */
		int a = 0;
		/** column that contains the longest shortest path value */
		int b = 0;

		ArrayList<Edge> edgeList = graph.getEdges();
		ArrayList<Vertex> vertexList = graph.getVertices();

		// create empty adjacency and predecessor matrix

		/** the matrix that contains the length of the shortest path from vertex a to vertex b */
		double[][] adjacencyMatrix = new double[vertexList.size()][vertexList.size()];
		/** the matrix that contains the predecessor vertex of vertex b in the shortest path from vertex a to b */
		int[][] predecessorMatrix = new int[vertexList.size()][vertexList.size()];

		// initial conditions for both matrices
		/*
		 * create 2D-adjacency array with the distance between the nodes.
		 * distance i --> i = 0
		 * distance i -/> j = infinite (edge does not exist)
		 * distance i --> j = length of branch
		 */

		/*
		 * create 2D-predecessor array using interconnected vertices.
		 * the predecessor matrix contains the predecessor of j in a path from node i to j.
		 * distance i --> i = NIL (there is no edge between a single vertex)
		 * distance i -/> j = NIL (there is no edge between the vertices)
		 * distance i --> j = i (the initial matrix only contains paths with a single edge)
		 * 
		 * I'm using -1 as NIL since it cannot refer to an index (and thus a vertex)
		 */

		// applying initial conditions
		for (int i = 0; i < vertexList.size(); i++) {
			for (int j = 0; j < vertexList.size(); j++) {
				adjacencyMatrix[i][j] = Double.POSITIVE_INFINITY;
				predecessorMatrix[i][j] = -1;
			}
		}

		for (Edge edge : edgeList) {
			if (Thread.interrupted())
				throw new RuntimeException("Interrupted");

			v1 = edge.getV1();
			v2 = edge.getV2();
			// use the index of the vertices as the index in the matrix
			row = vertexList.indexOf(v1);
			if (row == -1) {
				IJ.log("Vertex " + v1.getPoints().get(0) + " not found in the list of vertices!");
				continue;
			}

			column = vertexList.indexOf(v2);
			if (column == -1) {
				IJ.log("Vertex " + v2.getPoints().get(0) + " not found in the list of vertices!");
				continue;
			}

			/*
			 * the diagonal is 0.
			 * 
			 * Because not every vertex is a 'v1 vertex'
			 * the [column][column] statement is needed as well.
			 * 
			 * in an undirected graph the adjacencyMatrix is symmetric
			 * thus A = Transpose(A)
			 */
			adjacencyMatrix[row][row] = 0;
			adjacencyMatrix[column][column] = 0;
			adjacencyMatrix[row][column] = edge.getLength();
			adjacencyMatrix[column][row] = edge.getLength();

			/*
			 * the diagonal remains -1.
			 * for the rest I use the index of the vertex so I can later refer to the vertexList
			 * for the correct information
			 * 
			 * Determining what belongs where requires careful consideration of the definition
			 * 
			 * the array contains the predecessor of "column" in a path from "row" to "column"
			 * therefore in the other statement it is the other way around.
			 */
			predecessorMatrix[row][row] = -1;
			predecessorMatrix[column][column] = -1;
			predecessorMatrix[row][column] = row;
			predecessorMatrix[column][row] = column;
		}
		// matrices now have their initial conditions

		// the warshall algorithm with k as candidate vertex and i and j walk through the adjacencyMatrix
		// the predecessor matrix is updated at the same time.

		int size = vertexList.size();
		for (int k = 0; k < size; k++) {
			if (Thread.interrupted())
				throw new RuntimeException("Interrupted");
			for (int i = 0; i < size; i++) {
				for (int j = 0; j < size; j++) {
					if (adjacencyMatrix[i][k] + adjacencyMatrix[k][j] < adjacencyMatrix[i][j]) {
						adjacencyMatrix[i][j] = adjacencyMatrix[i][k] + adjacencyMatrix[k][j];
						predecessorMatrix[i][j] = predecessorMatrix[k][j];

					}
				}
			}
		}

		// find the maximum of all shortest paths
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				// sometimes infinities still remain
				if (adjacencyMatrix[i][j] > maxPath && adjacencyMatrix[i][j] != Double.POSITIVE_INFINITY) {
					maxPath = adjacencyMatrix[i][j];
					a = i;
					b = j;

				}
			}
		}

		// !important return maxPath;
		reconstructPath(predecessorMatrix, a, b, edgeList, vertexList, outputPoints, outputImage);
		return maxPath;

	}

	// end method warshallAlgorithm

	/**
	 * Reconstruction and visualisation of the longest shortest path found by the APSP warshall algorithm
	 * 
	 * @param predecessorMatrix
	 *            the Matrix which contains the predecessor of vertex b in the shortest path from a to b
	 * @param startIndex
	 *            the index of the row which contains the longest shortest path
	 * @param endIndex
	 *            the index of the column which contains the longest shortest path
	 * @param edgeList
	 *            the list of edges
	 * @param vertexList
	 *            the list of vertices
	 * 
	 * @author Huub Hovens
	 */
	private static void reconstructPath(int[][] predecessorMatrix, int startIndex, int endIndex,
			ArrayList<Edge> edgeList, ArrayList<Vertex> vertexList, PluginIOCells outputPoints,
			IPluginIOStack outputImage) {
		/** contains points of the longest shortest path for each graph */
		if (outputPoints != null)
			outputPoints.clear();

		List<SkeletonPoint> allPoints = new ArrayList<>(1000);

		// We know the first and last vertex of the longest shortest path, namely a and b
		// using the predecessor matrix we can now determine the path that is taken from a to b
		// remember a and b are indices and not the actual vertices.

		int b = endIndex;
		int a = startIndex;

		Edge previousShortestEdge = null;

		while (b != a) {
			Utils.log("Shortest path identification: " + a + ", " + b, LogLevel.DEBUG);
			if (Thread.interrupted())
				throw new RuntimeException("Interrupted");
			Vertex predecessor = vertexList.get(predecessorMatrix[a][b]);
			Vertex endvertex = vertexList.get(b);
			ArrayList<Edge> sp_edgeslist = new ArrayList<>();
			Double lengthtest = Double.POSITIVE_INFINITY;
			Edge shortestEdge = null;

			// search all edges for a combination of the two vertices
			// sometimes there are multiple edges between two vertices so add them to a list
			// for a second test
			sp_edgeslist
					.addAll(edgeList.stream().filter(
							edge -> (edge.getV1() == predecessor && edge.getV2() == endvertex)
									|| (edge.getV1() == endvertex && edge.getV2() == predecessor)).collect(
							Collectors.toList()));
			Utils.log("Second test", LogLevel.DEBUG);

			// the second test
			// this test looks which edge has the shortest length in sp_edgeslist
			for (Edge edge : sp_edgeslist) {
				if (edge.getLength() < lengthtest) {
					shortestEdge = edge;
					lengthtest = edge.getLength();
				}

			}

			if (previousShortestEdge != null) {
				List<SkeletonPoint> shortestPathWithinVertex =
						findShortestPath(shortestEdge.getSlabs(), previousShortestEdge.getSlabs(), endvertex);
				addPoints(shortestPathWithinVertex, outputImage);
				allPoints.addAll(shortestPathWithinVertex);
			}

			addPoints(shortestEdge.getSlabs(), outputImage);
			allPoints.addAll(shortestEdge.getSlabs());

			// if (shortestedge.getV1().getPoints().size()<20)
			// addPoints(shortestedge.getV1().getPoints(),outputListOfPoints, outputImage);
			// if (shortestedge.getV2().getPoints().size()<20)
			// addPoints(shortestedge.getV2().getPoints(),outputListOfPoints, outputImage);

			// now make the index of the endvertex the index of the predecessor so that the path now goes from
			// a to predecessor and repeat cycle
			b = predecessorMatrix[a][b];
			previousShortestEdge = shortestEdge;
		}

		// Now create pipeline-style points and compute geodesic distances
		// First, find an extremity

		SkeletonPoint edgePoint1 = null, edgePoint2 = null;
		List<SkeletonPoint> edgePoints = new ArrayList<>();
		int nEdges = 0;
		Utils.log("Showing all points in image", LogLevel.DEBUG);
		for (SkeletonPoint p : allPoints) {
			if (AnalyzeSkeleton.getNumberOfNeighbors(outputImage, p.x, p.y, p.z) == 1) {
				if (nEdges == 2) {
					// throw new IllegalStateException("Found more than 2 edges in shortest skeleton path");
					Utils.displayMessage("Found more than 2 edges in shortest skeleton path; will try to "
							+ "find most sensible pair", true, LogLevel.WARNING);
				}
				edgePoints.add(p);
				if (edgePoint1 != null)
					edgePoint2 = p;
				else
					edgePoint1 = p;
				nEdges++;
				// don't break so we label all point indices
			}
		}

		if (nEdges < 2) {
			throw new IllegalStateException("Found " + nEdges + " instead of 2 extremities in shortest skeleton path");
		}

		if (edgePoints.size() > 2) {
			double maxDistance = 0;
			for (SkeletonPoint p : edgePoints) {
				for (SkeletonPoint q : edgePoints) {
					double distance = p.distanceTo(q);
					if (distance > maxDistance) {
						maxDistance = distance;
						edgePoint1 = p;
						edgePoint2 = q;
					}
				}
			}
		}

		Point3D edgePoint = (edgePoint2.x < edgePoint1.x) ? new Point3D(edgePoint2) : new Point3D(edgePoint1);
		// take the edge with smallest x

		boolean done = false;
		double geodesicDistance = 0;
		Point3D previousPoint = null;
		outputImage.setPixelValue(edgePoint.x, edgePoint.y, edgePoint.z, VISITED_SHORTEST_PATH);
		int counter = 1;

		Utils.log("Writing geodesic distances", LogLevel.DEBUG);
		while (!done) {
			if (previousPoint != null) {
				geodesicDistance += previousPoint.distanceTo(edgePoint);
			}
			// add edgepoint
			ClickedPoint newPoint = new ClickedPoint(edgePoint.x, edgePoint.y, edgePoint.z, 0, 0, 0);
			outputPoints.addAndLink(newPoint);
			newPoint.setQuantifiedProperty("geodesicDistance", (float) geodesicDistance);
			// find a non-visited neighbor
			previousPoint = edgePoint;
			boolean notFound = true;
			for (int xOffset = -1; (xOffset <= 1) && notFound; xOffset++) {
				for (int yOffset = -1; (yOffset <= 1) && notFound; yOffset++) {
					for (int zOffset = -1; (zOffset <= 1) && notFound; zOffset++) {
						int nx = edgePoint.x + xOffset;
						int ny = edgePoint.y + yOffset;
						int nz = edgePoint.z + zOffset;
						float pixelValue = outputImage.getPixelValue(nx, ny, nz);
						if (pixelValue == 0)
							continue;
						if (((byte) pixelValue) == VISITED_SHORTEST_PATH)
							continue;
						notFound = false;
						outputImage.setPixelValue(nx, ny, nz, VISITED_SHORTEST_PATH);
						edgePoint = new Point3D(nx, ny, nz);
					}
				}
			}

			if (notFound)
				done = true;
			counter++;
		}

		Utils.log("Found " + counter + " points", LogLevel.DEBUG);
		outputPoints.fireValueChanged(false, false);

	}

	// end method reconstructPath

	private static List<SkeletonPoint> findShortestPath(ArrayList<SkeletonPoint> slabs1,
			ArrayList<SkeletonPoint> slabs2, Vertex endvertex) {
		List<SkeletonPoint> connectingPoints = new ArrayList<>(100);

		// Need to set index value for Vertices, and create edges

		Map<Point3D, SkeletonPoint> points = new HashMap<>();
		final int VERTEX_POINT = 1;

		for (SkeletonPoint p : endvertex.getPoints()) {
			points.put(new Point3D(p), p);
			p.owner = VERTEX_POINT;
		}

		for (SkeletonPoint p : slabs1) {
			points.put(new Point3D(p), p);
			p.owner = 0;
		}
		for (SkeletonPoint p : slabs2) {
			points.put(new Point3D(p), p);
			p.owner = 0;
		}

		List<FWEdge> edges = new ArrayList<>(200);

		Utils.log("Creating edges in findShortestPath", LogLevel.DEBUG);

		// Create edges
		for (Entry<Point3D, SkeletonPoint> p : points.entrySet()) {
			int x = p.getKey().x;
			int y = p.getKey().y;
			int z = p.getKey().z;

			checkAddEdge(points, edges, p, new Point3D(x - 1, y - 1, z - 1));
			checkAddEdge(points, edges, p, new Point3D(x, y - 1, z - 1));
			checkAddEdge(points, edges, p, new Point3D(x + 1, y - 1, z - 1));

			checkAddEdge(points, edges, p, new Point3D(x - 1, y, z - 1));
			checkAddEdge(points, edges, p, new Point3D(x, y, z - 1));
			checkAddEdge(points, edges, p, new Point3D(x + 1, y, z - 1));

			checkAddEdge(points, edges, p, new Point3D(x - 1, y + 1, z - 1));
			checkAddEdge(points, edges, p, new Point3D(x, y + 1, z - 1));
			checkAddEdge(points, edges, p, new Point3D(x + 1, y + 1, z - 1));

			checkAddEdge(points, edges, p, new Point3D(x - 1, y - 1, z));
			checkAddEdge(points, edges, p, new Point3D(x, y - 1, z));
			checkAddEdge(points, edges, p, new Point3D(x + 1, y - 1, z));

			checkAddEdge(points, edges, p, new Point3D(x - 1, y, z));
			checkAddEdge(points, edges, p, new Point3D(x, y, z));
			checkAddEdge(points, edges, p, new Point3D(x + 1, y, z));

			checkAddEdge(points, edges, p, new Point3D(x - 1, y + 1, z));
			checkAddEdge(points, edges, p, new Point3D(x, y + 1, z));
			checkAddEdge(points, edges, p, new Point3D(x + 1, y + 1, z));

			checkAddEdge(points, edges, p, new Point3D(x - 1, y - 1, z + 1));
			checkAddEdge(points, edges, p, new Point3D(x, y - 1, z + 1));
			checkAddEdge(points, edges, p, new Point3D(x + 1, y - 1, z + 1));

			checkAddEdge(points, edges, p, new Point3D(x - 1, y, z + 1));
			checkAddEdge(points, edges, p, new Point3D(x, y, z + 1));
			checkAddEdge(points, edges, p, new Point3D(x + 1, y, z + 1));

			checkAddEdge(points, edges, p, new Point3D(x - 1, y + 1, z + 1));
			checkAddEdge(points, edges, p, new Point3D(x, y + 1, z + 1));
			checkAddEdge(points, edges, p, new Point3D(x + 1, y + 1, z + 1));

		}

		SkeletonPoint[] pointArray = new SkeletonPoint[points.size()];
		int i = 0;
		for (SkeletonPoint p : points.values()) {
			pointArray[i] = p;
			p.index = i;
			i++;
		}

		FloydWarshall fw = new FloydWarshall();

		Utils.log("Calculating shortest path", LogLevel.DEBUG);

		fw.calcShortestPaths(pointArray, edges.toArray(new FWEdge[] {}));

		Utils.log("Done with shortest path", LogLevel.DEBUG);
		connectingPoints.addAll(fw.getShortestPath(slabs1.get(0), slabs2.get(0)).stream().filter(
				pointIndex -> pointArray[pointIndex.getIndex()].owner == VERTEX_POINT).map(
				pointIndex -> pointArray[pointIndex.getIndex()]).collect(Collectors.toList()));
		Utils.log("Returning shortest path", LogLevel.DEBUG);
		return connectingPoints;
	}

	private static void checkAddEdge(Map<Point3D, SkeletonPoint> points, List<FWEdge> edges,
			Entry<Point3D, SkeletonPoint> ps, Point3D point3D) {
		SkeletonPoint q = points.get(point3D);
		if (q == null)
			return;
		double distance = ps.getKey().distanceTo(point3D);
		if (distance == 0)
			return;
		SkeletonPoint p = ps.getValue();
		edges.add(new FWEdge(p, q, distance));
	}

	private static void addPoints(List<SkeletonPoint> skeletonPoints, IPluginIOStack outputImage) {
		/*
		 * if (outputPoints!=null){
		 * ClickedPoint newPoint=new ClickedPoint(p.x, p.y, p.z, 0, 0, 0);
		 * outputPoints.addAndLink(newPoint);
		 * if (previousPoint!=null)
		 * geodesicDistance+=previousPoint.distanceTo(p);
		 * newPoint.setQuantifiedProperty("geodesicDistance", (float) geodesicDistance);
		 * previousPoint=p;
		 * }
		 */
		if (outputImage != null)
			skeletonPoints.stream().forEach(p -> AnalyzeSkeleton.setPixel(outputImage, p.x, p.y, p.z, SHORTEST_PATH));
	}

	@Override
	public String[] getInputLabels() {
		return new String[] { "Cells" };
	}

	@Override
	public String[] getOutputLabels() {
		return new String[] { "Cells" };
	}

	/*
	 * @Override
	 * public int getOutputDepth(PluginIO input) {
	 * return ((SkeletonGraph) input).getImageDepth();
	 * }
	 * 
	 * @Override
	 * public int getOutputHeight(PluginIO input) {
	 * return ((SkeletonGraph) input).getImageHeight();
	 * }
	 * 
	 * @Override
	 * public int getOutputNChannels(PluginIO input) {
	 * return 1;
	 * }
	 * 
	 * @Override
	 * public int getOutputWidth(PluginIO input) {
	 * return ((SkeletonGraph) input).getImageWidth();
	 * }
	 */

}// end class AnalyzeSkeleton_
