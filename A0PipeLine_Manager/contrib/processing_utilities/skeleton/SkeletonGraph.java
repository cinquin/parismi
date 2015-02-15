package processing_utilities.skeleton;

import ij.measure.Calibration;

import java.util.ArrayList;
import java.util.Stack;

import pipeline.data.IPluginIOList;
import pipeline.data.IPluginIOListMemberQ;
import pipeline.data.Quantifiable;

/**
 * AnalyzeSkeleton_ plugin for ImageJ(C).
 * Copyright (C) 2008,2009 Ignacio Arganda-Carreras
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt )
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * 
 */

/**
 * This class represents an undirected graph to allow
 * visiting the skeleton in an efficient way
 */
public class SkeletonGraph extends Quantifiable implements IPluginIOListMemberQ<SkeletonGraph> {
	@Override
	// FIXME For now this only performs a shallow copy; this is contrary to usual pipeline semantics
			public
			Object clone() {
		SkeletonGraph newGraph = new SkeletonGraph(imageWidth, imageHeight, imageDepth, calibration);
		super.copyInto(newGraph);
		newGraph.edges = edges;
		newGraph.vertices = vertices;
		newGraph.root = root;
		return newGraph;
	}

	/** list of edges */
	private ArrayList<Edge> edges = null;
	/** list of vertices */
	private ArrayList<Vertex> vertices = null;

	/** root vertex */
	private Vertex root = null;

	private int imageWidth, imageHeight, imageDepth;

	public int getImageWidth() {
		return imageWidth;
	}

	public void setImageWidth(int imageWidth) {
		this.imageWidth = imageWidth;
	}

	public int getImageHeight() {
		return imageHeight;
	}

	public void setImageHeight(int imageHeight) {
		this.imageHeight = imageHeight;
	}

	public int getImageDepth() {
		return imageDepth;
	}

	public void setImageDepth(int imageDepth) {
		this.imageDepth = imageDepth;
	}

	// --------------------------------------------------------------------------
	/**
	 * Empty constructor.
	 * 
	 * @param calibration
	 */
	public SkeletonGraph(int imageWidth, int imageHeight, int imageDepth, Calibration calibration) {
		this.imageDepth = imageDepth;
		this.imageHeight = imageHeight;
		this.imageWidth = imageWidth;
		this.calibration = calibration;
		this.edges = new ArrayList<>();
		this.vertices = new ArrayList<>();

	}

	// --------------------------------------------------------------------------
	/**
	 * Add edge to the graph.
	 * 
	 * @param e
	 *            edge to be added
	 * @return false if the edge could not be added, true otherwise
	 */
	public boolean addEdge(Edge e) {
		if (this.edges.contains(e))
			return false;
		else {
			// Set vertices from e as neighbors (undirected graph)
			e.getV1().setBranch(e);
			if (!e.getV1().equals(e.getV2()))
				e.getV2().setBranch(e);
			// Add edge to the list of edges in the graph
			this.edges.add(e);
			return true;
		}
	}// end method addEdge

	// --------------------------------------------------------------------------
	/**
	 * Add vertex to the graph.
	 * 
	 * @param v
	 *            vertex to be added
	 * @return false if the vertex could not be added, true otherwise
	 */
	public boolean addVertex(Vertex v) {
		if (this.vertices.contains(v))
			return false;
		else {
			this.vertices.add(v);
			return true;
		}
	}// end method addVertex
		// --------------------------------------------------------------------------

	/**
	 * Get list of vertices in the graph.
	 * 
	 * @return list of vertices in the graph
	 */
	public ArrayList<Vertex> getVertices() {
		return this.vertices;
	}

	// --------------------------------------------------------------------------
	/**
	 * Get list of edges in the graph.
	 * 
	 * @return list of edges in the graph
	 */
	public ArrayList<Edge> getEdges() {
		return this.edges;
	}

	// --------------------------------------------------------------------------
	/**
	 * Set root vertex.
	 */
	public void setRoot(Vertex v) {
		this.root = v;
	}

	// --------------------------------------------------------------------------
	/**
	 * Depth first search method to detect cycles in the graph.
	 * 
	 * @return list of BACK edges
	 */
	public ArrayList<Edge> depthFirstSearch() {
		ArrayList<Edge> backEdges = new ArrayList<>();

		// Create empty stack
		Stack<Vertex> stack = new Stack<>();

		// Mark all vertices as non-visited
		for (final Vertex v : this.vertices)
			v.setVisited(false);

		// Push the root into the stack
		stack.push(this.root);

		int visitOrder = 0;

		while (!stack.empty()) {
			Vertex u = stack.pop();

			if (!u.isVisited()) {
				// IJ.log(" Visiting vertex " + u.getPoints().get(0));

				// If the vertex has not been visited yet, then
				// the edge from the predecessor to this vertex
				// is mark as TREE
				if (u.getPredecessor() != null)
					u.getPredecessor().setType(Edge.TREE);

				// mark as visited
				u.setVisited(true, visitOrder++);

				// For the undefined branches:
				// We push the unvisited vertices in the stack,
				// and mark the edge to the others as BACK
				u.getBranches().stream().filter(e -> e.getType() == Edge.UNDEFINED).forEach(e -> {
					final Vertex ov = e.getOppositeVertex(u);
					if (!ov.isVisited()) {
						stack.push(ov);
						ov.setPredecessor(e);
					} else {
						e.setType(Edge.BACK);
						backEdges.add(e);
					}

				});
			}
		}

		return backEdges;

	} // end method depthFirstSearch

	@Override
	public void linkToList(IPluginIOList<?> list) {
		// Nothing to do since skeletons in a same group don't share data
	}

	private Calibration calibration;

	public Calibration getCalibration() {
		return calibration;
	}

	public void setCalibration(Calibration calibration) {
		this.calibration = calibration;
	}

}// end class SkeletonGraph
