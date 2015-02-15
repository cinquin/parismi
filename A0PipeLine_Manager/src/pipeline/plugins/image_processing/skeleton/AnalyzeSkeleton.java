// Adapted from ImageJ/Fiji
package pipeline.plugins.image_processing.skeleton;

import ij.ImagePlus;
import ij.measure.Calibration;
import ij.measure.ResultsTable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import pipeline.PreviewType;
import pipeline.data.IPluginIOListOfQ;
import pipeline.data.IPluginIOStack;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.data.PluginIOListOfQ;
import pipeline.data.PluginIOStack;
import pipeline.misc_util.PluginRuntimeException;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.plugins.AuxiliaryInputOutputPlugin;
import pipeline.plugins.ThreeDPlugin;
import processing_utilities.skeleton.Edge;
import processing_utilities.skeleton.SkeletonGraph;
import processing_utilities.skeleton.SkeletonPoint;
import processing_utilities.skeleton.SkeletonResult;
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
 * @author Ignacio Arganda-Carreras <iarganda@mit.edu>
 *
 */
public class AnalyzeSkeleton extends ThreeDPlugin implements AuxiliaryInputOutputPlugin {

	@Override
	public String getToolTip() {
		return "Create a list of skeleton objects from a skeleton image";
	}

	private boolean cancelled;

	@Override
	public void runChannel(final IPluginIOStack input, final IPluginIOStack output, final ProgressReporter progress,
			PreviewType previewType, boolean inputHasChanged) throws InterruptedException {
		cancelled = false;
		input.computePixelArray();
		output.computePixelArray();
		run(NONE, input, input, output, true, false, progress);
		// TODO the second "input" argument should be the original image before skeletonization; this should probably be
		// a parameter

		// FIXME When we have PluginIOCollections, return a collection of graphs instead of just the first one
		initializeOutputs();
		// IPluginIOList<SkeletonGraph> outputSkeletons=PluginIOListProxy.newInstance(SkeletonGraph.class);
		IPluginIOListOfQ<SkeletonGraph> outputSkeletons = new PluginIOListOfQ<>();
		outputSkeletons.clear();
		showResults();// fill in skeleton info
		outputSkeletons.addAllAndLink(Arrays.asList(skeletonGraph));
		// pluginOutputs.put("Cells", skeletonGraph[0]);
		pluginOutputs.put("Cells", outputSkeletons);

		if (cancelled)
			throw new InterruptedException();
	}

	@Override
	public int getFlags() {
		return SAME_AS_FLOAT + PARALLELIZE_WITH_NEW_INSTANCES + NEED_INPUT_LOCKED;
	}

	@Override
	public String operationName() {
		return "Analyze skeleton";
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
	private static final byte SLAB = 127;
	/** shortest path flag */
	public static byte SHORTEST_PATH = 96;

	/** working image plus */
	private ImagePlus imRef;

	/** working image width */
	private int width = 0;
	/** working image height */
	private int height = 0;
	/** working image depth */
	private int depth = 0;
	/** working image stack */
	private IPluginIOStack inputImage = null;

	/** visit flags */
	private boolean[][][] visited = null;

	// Measures
	/** total number of end points voxels */
	private int totalNumberOfEndPoints = 0;
	/** total number of junctions voxels */
	private int totalNumberOfJunctionVoxels = 0;
	/** total number of slab voxels */
	@SuppressWarnings("unused")
	private int totalNumberOfSlabs = 0;

	// Tree fields
	/** number of branches for every specific tree */
	private int[] numberOfBranches = null;
	/** number of end points voxels of every tree */
	private int[] numberOfEndPoints = null;
	/** number of junctions voxels of every tree */
	private int[] numberOfJunctionVoxels = null;
	/** number of slab voxels of every specific tree */
	private int[] numberOfSlabs = null;
	/** number of junctions of every specific tree */
	private int[] numberOfJunctions = null;
	/** number of triple points in every tree */
	private int[] numberOfTriplePoints = null;
	/** number of quadruple points in every tree */
	private int[] numberOfQuadruplePoints = null;
	/** list of end points in every tree */
	private ArrayList<SkeletonPoint> endPointsTree[] = null;
	/** list of junction voxels in every tree */
	private ArrayList<SkeletonPoint> junctionVoxelTree[] = null;
	/** list of special slab coordinates where circular tree starts */
	private ArrayList<SkeletonPoint> startingSlabTree[] = null;

	/** average branch length */
	private double[] averageBranchLength = null;

	/** maximum branch length */
	private double[] maximumBranchLength = null;

	/** list of end point coordinates in the entire image */
	private ArrayList<SkeletonPoint> listOfEndPoints = null;
	/** list of junction coordinates in the entire image */
	private ArrayList<SkeletonPoint> listOfJunctionVoxels = null;
	/** list of slab coordinates in the entire image */
	private ArrayList<SkeletonPoint> listOfSlabVoxels = null;
	/** list of slab coordinates in the entire image */
	private ArrayList<SkeletonPoint> listOfStartingSlabVoxels = null;

	/** list of groups of junction voxels that belong to the same tree junction (in every tree) */
	private ArrayList<ArrayList<SkeletonPoint>> listOfSingleJunctions[] = null;
	/** array of junction vertex per tree */
	private Vertex[][] junctionVertex = null;

	/** stack image containing the corresponding skeleton tags (end point, junction or slab) */
	private IPluginIOStack taggedImage = null;

	/** auxiliary temporary point */
	private SkeletonPoint auxPoint = null;

	/** number of trees (skeletons) in the image */
	private int numOfTrees = 0;

	/** pruning option */
	private boolean bPruneCycles = true;

	/** array of graphs (one per tree) */
	private SkeletonGraph[] skeletonGraph = null;

	/** auxiliary list of slabs */
	private ArrayList<SkeletonPoint> slabList = null;
	/** auxiliary final vertex */
	private Vertex auxFinalVertex = null;

	/** prune cycle options */
	public static final String[] pruneCyclesModes = { "none", "shortest branch", "lowest intensity voxel",
			"lowest intensity branch" };
	/** no pruning mode index */
	private static final int NONE = 0;
	/** shortest branch pruning mode index */
	private static final int SHORTEST_BRANCH = 1;
	/** lowest pixel intensity pruning mode index */
	private static final int LOWEST_INTENSITY_VOXEL = 2;
	/** lowest intensity branch pruning mode index */
	private static final int LOWEST_INTENSITY_BRANCH = 3;

	/** original grayscale image (for lowest pixel intensity pruning mode) */
	private IPluginIOStack originalImage = null;

	/** prune cycle options index */
	private int pruneIndex = AnalyzeSkeleton.NONE;

	/** x- neighborhood offset */
	private int x_offset = 1;
	/** y- neighborhood offset */
	private int y_offset = 1;
	/** z- neighborhood offset */
	private int z_offset = 1;

	/** boolean flag to display extra information in result tables */
	private boolean verbose = false;

	/** silent run flag, to distinguish between GUI and plugin calls */
	private boolean silent = false;

	/** debugging flag */
	private static final boolean debug = false;

	/**
	 * This method is intended for non-interactively using this plugin.
	 * <p>
	 * 
	 * @param pruneIndex
	 *            The pruneIndex, as asked by the initial gui dialog.
	 * @throws InterruptedException
	 */
	SkeletonResult run(int pruneIndex, IPluginIOStack input, IPluginIOStack input2, IPluginIOStack output,
			boolean silent, boolean verbose, ProgressReporter progress) throws InterruptedException {
		this.pruneIndex = pruneIndex;
		this.silent = silent;
		this.verbose = verbose;

		switch (pruneIndex) {
		// No pruning
			case AnalyzeSkeleton.NONE:
				this.bPruneCycles = false;
				break;
			// Pruning cycles by shortest branch
			case AnalyzeSkeleton.SHORTEST_BRANCH:
				this.bPruneCycles = true;
				break;
			// Pruning cycles by lowest pixel intensity
			case AnalyzeSkeleton.LOWEST_INTENSITY_VOXEL:
			case AnalyzeSkeleton.LOWEST_INTENSITY_BRANCH:
				// calculate neighborhood size given the calibration
				calculateNeighborhoodOffsets(input.getCalibration());
				this.originalImage = input;
				this.bPruneCycles = true;
				break;
			default:
		}

		this.width = input.getWidth();
		this.height = input.getHeight();
		this.depth = input.getDepth();
		// this.inputImage = originalImageNotInBinary;
		this.inputImage = input;

		// initialize visit flags
		resetVisited();

		// Tag skeleton, differentiate trees and visit them
		processSkeleton(input, output, progress);// was this.inputImage
		if (endPointsTree.length == 0)
			return null;

		// Prune cycles if necessary
		if (bPruneCycles) {
			if (pruneCycles(this.inputImage, this.originalImage, this.pruneIndex)) {
				// initialize visit flags
				resetVisited();
				// Recalculate analysis over the new image
				bPruneCycles = false;
				processSkeleton(this.inputImage, output, progress);
				if (cancelled)
					return null;
			}
		}

		// Calculate triple points (junctions with exactly 3 branches)
		calculateTripleAndQuadruplePoints();

		// Return the analysis results
		return assembleResults();
	}

	/**
	 * A simpler standalone running method, for analyzation without pruning
	 * or showing images.
	 * <p>
	 * This one just calls run(AnalyzeSkeleton_.NONE, null, true, false)
	 */
	// public SkeletonResult run()
	// {
	// return run(NONE, null, true, false);
	// }

	// ---------------------------------------------------------------------------
	/**
	 * Calculate the neighborhood size based on the calibration of the image.
	 * 
	 * @param calibration
	 *            image calibration
	 */
	private void calculateNeighborhoodOffsets(Calibration calibration) {
		double max = calibration.pixelDepth;
		if (calibration.pixelHeight > max)
			max = calibration.pixelHeight;
		if (calibration.pixelWidth > max)
			max = calibration.pixelWidth;

		this.x_offset =
				((int) Math.round(max / calibration.pixelWidth) > 1) ? (int) Math.round(max / calibration.pixelWidth)
						: 1;
		this.y_offset =
				((int) Math.round(max / calibration.pixelHeight) > 1) ? (int) Math.round(max / calibration.pixelHeight)
						: 1;
		this.z_offset =
				((int) Math.round(max / calibration.pixelDepth) > 1) ? (int) Math.round(max / calibration.pixelDepth)
						: 1;

		if (debug) {
			Utils.log("x_offset = " + this.x_offset, LogLevel.VERBOSE_DEBUG);
			Utils.log("y_offset = " + this.y_offset, LogLevel.VERBOSE_DEBUG);
			Utils.log("z_offset = " + this.z_offset, LogLevel.VERBOSE_DEBUG);
		}

	}// end method calculateNeighborhoodOffsets

	// ---------------------------------------------------------------------------
	/**
	 * Process skeleton: tag image, mark trees and visit.
	 * 
	 * @param inputImage2
	 *            input skeleton image to process
	 * @throws InterruptedException
	 */
	void processSkeleton(IPluginIOStack inputImage2, IPluginIOStack outputImage, ProgressReporter progress)
			throws InterruptedException {
		// Initialize global lists of points
		this.listOfEndPoints = new ArrayList<>();
		this.listOfJunctionVoxels = new ArrayList<>();
		this.listOfSlabVoxels = new ArrayList<>();
		this.listOfStartingSlabVoxels = new ArrayList<>();
		this.totalNumberOfEndPoints = 0;
		this.totalNumberOfJunctionVoxels = 0;
		this.totalNumberOfSlabs = 0;

		// Prepare data: classify voxels and tag them.
		tagImage(inputImage2, outputImage, progress);
		if (cancelled)
			return;

		this.taggedImage = outputImage;

		// Show tags image.
		if (!bPruneCycles && !silent) {
			// displayTagImage(taggedImage);
		}

		// Mark trees
		IPluginIOStack treeIS = markTrees(taggedImage);
		// markTrees creates an output image by itself, and sets the number of trees (instance variable numOfTrees),
		// based on totalnumberof different types of voxels
		// computed by tagimage
		if (cancelled)
			return;

		// Ask memory for every tree
		initializeTrees();

		// Divide groups of end-points and junction voxels

		if (endPointsTree.length == 0) {
			return;
		}

		if (this.numOfTrees > 1)
			divideVoxelsByTrees(treeIS);
		else {
			if (debug)
				Utils.log("list of end points size = " + this.listOfEndPoints.size(), LogLevel.VERBOSE_DEBUG);
			this.endPointsTree[0] = this.listOfEndPoints;
			this.numberOfEndPoints[0] = this.listOfEndPoints.size();
			this.junctionVoxelTree[0] = this.listOfJunctionVoxels;
			this.numberOfJunctionVoxels[0] = this.listOfJunctionVoxels.size();
			this.startingSlabTree[0] = this.listOfStartingSlabVoxels;
		}

		// Calculate number of junctions (skipping neighbor junction voxels)
		groupJunctions(treeIS);

		// Visit skeleton and measure distances.
		for (int i = 0; i < this.numOfTrees; i++)
			visitSkeleton(taggedImage, treeIS, i + 1);

	} // end method processSkeleton

	// -----------------------------------------------------------------------
	/**
	 * Prune cycles from tagged image and update it.
	 * 
	 * @param inputImage
	 *            input skeleton image
	 * @param originalImage
	 *            original gray-scale image
	 * @param pruningMode
	 *            (SHORTEST_BRANCH, LOWEST_INTENSITY_VOXEL, LOWEST_INTENSITY_BRANCH)
	 * @return true if the input image was pruned or false if there were no cycles
	 */
	private boolean pruneCycles(IPluginIOStack inputImage, final IPluginIOStack originalImage, final int pruningMode) {
		boolean pruned = false;

		for (int iTree = 0; iTree < this.numOfTrees; iTree++) {
			// For circular trees we just remove one slab
			if (this.startingSlabTree[iTree].size() == 1) {
				setPixel(inputImage, this.startingSlabTree[iTree].get(0), (byte) 0);
				pruned = true;
			} else // For the rest, we do depth-first search to detect the cycles
			{
				// DFS
				ArrayList<Edge> backEdges = this.skeletonGraph[iTree].depthFirstSearch();

				if (debug) {
					Utils.log(" --------------------------- ", LogLevel.VERBOSE_DEBUG);
					final String[] s = new String[] { "UNDEFINED", "TREE", "BACK" };
					for (final Edge e : this.skeletonGraph[iTree].getEdges()) {
						Utils.log(" edge " + e.getV1().getPoints().get(0) + " - " + e.getV2().getPoints().get(0)
								+ " : " + s[e.getType() + 1], LogLevel.VERBOSE_DEBUG);
					}
				}

				// If DFS returned backEdges, we need to delete the loops
				if (backEdges.size() > 0) {
					// Find all edges of each loop (backtracking the predecessors)
					for (final Edge e : backEdges) {
						ArrayList<Edge> loopEdges = new ArrayList<>();
						loopEdges.add(e);

						Edge minEdge = e;

						// backtracking (starting at the vertex with higher order index
						final Vertex finalLoopVertex =
								e.getV1().getVisitOrder() < e.getV2().getVisitOrder() ? e.getV1() : e.getV2();

						Vertex backtrackVertex =
								e.getV1().getVisitOrder() < e.getV2().getVisitOrder() ? e.getV2() : e.getV1();

						// backtrack until reaching final loop vertex
						while (!finalLoopVertex.equals(backtrackVertex)) {
							// Extract predecessor
							final Edge pre = backtrackVertex.getPredecessor();
							// Update shortest loop edge if necessary
							if (pruningMode == AnalyzeSkeleton.SHORTEST_BRANCH
									&& pre.getSlabs().size() < minEdge.getSlabs().size())
								minEdge = pre;
							// Add to loop edge list
							loopEdges.add(pre);
							// Extract predecessor
							backtrackVertex = pre.getV1().equals(backtrackVertex) ? pre.getV2() : pre.getV1();
						}

						// Prune cycle
						if (pruningMode == AnalyzeSkeleton.SHORTEST_BRANCH) {
							// Remove middle slab from the shortest loop edge
							SkeletonPoint removeCoords = null;
							if (minEdge.getSlabs().size() > 0)
								removeCoords = minEdge.getSlabs().get(minEdge.getSlabs().size() / 2);
							else
								removeCoords = minEdge.getV1().getPoints().get(0);
							setPixel(inputImage, removeCoords, (byte) 0);
						} else if (pruningMode == AnalyzeSkeleton.LOWEST_INTENSITY_VOXEL) {
							removeLowestIntensityVoxel(loopEdges, inputImage, originalImage);
						} else if (pruningMode == AnalyzeSkeleton.LOWEST_INTENSITY_BRANCH) {
							cutLowestIntensityBranch(loopEdges, inputImage, originalImage);
						}
					}// endfor backEdges

					pruned = true;
				}
			}
		}

		return pruned;
	}// end method pruneCycles

	// -----------------------------------------------------------------------
	/**
	 * Cut the a list of edges in the lowest pixel intensity voxel (calculated
	 * from the original -grayscale- image).
	 * 
	 * @param loopEdges
	 *            list of edges to be analyzed
	 * @param inputImage2
	 *            input skeleton image
	 * @param originalGrayImage
	 *            original gray image
	 */
	private void removeLowestIntensityVoxel(final ArrayList<Edge> loopEdges, IPluginIOStack inputImage2,
			IPluginIOStack originalGrayImage) {
		SkeletonPoint lowestIntensityVoxel = null;

		double lowestIntensityValue = Double.MAX_VALUE;

		for (final Edge e : loopEdges) {
			// Check slab points
			for (final SkeletonPoint p : e.getSlabs()) {
				final double avg =
						getAverageNeighborhoodValue(originalGrayImage, p, this.x_offset, this.y_offset, this.z_offset);
				if (avg < lowestIntensityValue) {
					lowestIntensityValue = avg;
					lowestIntensityVoxel = p;
				}
			}
			// Check vertices
			/*
			 * for(final SkeletonPoint p : e.getV1().getPoints())
			 * {
			 * final double avg = getAverageNeighborhoodValue(originalGrayImage, p,
			 * this.x_offset, this.y_offset, this.z_offset);
			 * if(avg < lowestIntensityValue)
			 * {
			 * lowestIntensityValue = avg;
			 * lowestIntensityVoxel = p;
			 * }
			 * }
			 * for(final SkeletonPoint p : e.getV2().getPoints())
			 * {
			 * final double avg = getAverageNeighborhoodValue(originalGrayImage, p,
			 * this.x_offset, this.y_offset, this.z_offset);
			 * if(avg < lowestIntensityValue)
			 * {
			 * lowestIntensityValue = avg;
			 * lowestIntensityVoxel = p;
			 * }
			 * }
			 */
		}

		// Cut loop in the lowest intensity pixel value position
		if (debug)
			Utils.log("Cut loop at coordinates: " + lowestIntensityVoxel, LogLevel.VERBOSE_DEBUG);
		setPixel(inputImage2, lowestIntensityVoxel, (byte) 0);
	}// end method removeLowestIntensityVoxel

	// -----------------------------------------------------------------------
	/**
	 * Cut the a list of edges in the lowest pixel intensity branch.
	 * 
	 * @param loopEdges
	 *            list of edges to be analyzed
	 * @param inputImage2
	 *            input skeleton image
	 */
	private void cutLowestIntensityBranch(final ArrayList<Edge> loopEdges, IPluginIOStack inputImage2,
			IPluginIOStack originalGrayImage) {
		Edge lowestIntensityEdge = null;

		double lowestIntensityValue = Double.MAX_VALUE;

		SkeletonPoint cutPoint = null;

		for (final Edge e : loopEdges) {
			// Calculate average intensity of the edge neighborhood
			double min_val = Double.MAX_VALUE;
			SkeletonPoint darkestPoint = null;

			double edgeIntensity = 0;
			double n_vox = 0;

			// Check slab points
			for (final SkeletonPoint p : e.getSlabs()) {
				final double avg =
						getAverageNeighborhoodValue(originalGrayImage, p, this.x_offset, this.y_offset, this.z_offset);
				// Keep track of the darkest slab point of the edge
				if (avg < min_val) {
					min_val = avg;
					darkestPoint = p;
				}
				edgeIntensity += avg;
				n_vox++;
			}
			// Check vertices
			for (final SkeletonPoint p : e.getV1().getPoints()) {
				edgeIntensity +=
						getAverageNeighborhoodValue(originalGrayImage, p, this.x_offset, this.y_offset, this.z_offset);
				n_vox++;
			}
			for (final SkeletonPoint p : e.getV2().getPoints()) {
				edgeIntensity +=
						getAverageNeighborhoodValue(originalGrayImage, p, this.x_offset, this.y_offset, this.z_offset);
				n_vox++;
			}

			if (n_vox != 0)
				edgeIntensity /= n_vox;
			if (debug) {
				Utils.log("Loop edge between " + e.getV1().getPoints().get(0) + " and " + e.getV2().getPoints().get(0)
						+ ":", LogLevel.VERBOSE_DEBUG);
				Utils.log("avg edge intensity = " + edgeIntensity + " darkest slab point = " + darkestPoint.toString(),
						LogLevel.VERBOSE_DEBUG);
			}
			// Keep track of the lowest intensity edge
			if (edgeIntensity < lowestIntensityValue) {
				lowestIntensityEdge = e;
				lowestIntensityValue = edgeIntensity;
				cutPoint = darkestPoint;
			}
		}

		// Cut loop in the lowest intensity branch medium position
		SkeletonPoint removeCoords = null;
		if (lowestIntensityEdge.getSlabs().size() > 0)
			removeCoords = cutPoint;
		else {
			Utils.displayMessage("Lowest intensity branch without slabs?!: vertex "
					+ lowestIntensityEdge.getV1().getPoints().get(0), true, LogLevel.WARNING);
			removeCoords = lowestIntensityEdge.getV1().getPoints().get(0);
		}

		if (debug)
			Utils.log("Cut loop at coordinates: " + removeCoords, LogLevel.VERBOSE_DEBUG);
		setPixel(inputImage2, removeCoords, (byte) 0);

	}// end method cutLowestIntensityBranch

	// -----------------------------------------------------------------------
	/**
	 * Display tag image on a new window.
	 * 
	 * @param taggedImage
	 *            tag image to be diplayed
	 */
	/*
	 * void displayTagImage(PluginIOStackInterface taggedImage)
	 * {
	 * ImagePlus tagIP = new ImagePlus("Tagged skeleton", taggedImage);
	 * tagIP.show();
	 * 
	 * // Set same calibration as the input image
	 * tagIP.setCalibration(this.imRef.getCalibration());
	 * 
	 * // We apply the Fire LUT and reset the min and max to be between 0-255.
	 * IJ.run(tagIP, "Fire", null);
	 * 
	 * //IJ.resetMinAndMax();
	 * tagIP.resetDisplayRange();
	 * tagIP.updateAndDraw();
	 * } // end method displayTagImage
	 */
	// -----------------------------------------------------------------------
	/**
	 * Divide the end point, junction and special (starting) slab voxels in the
	 * corresponding tree lists.
	 * 
	 * @param treeIS
	 *            tree image
	 */
	private void divideVoxelsByTrees(IPluginIOStack treeIS) {
		Utils.log("numofTrees=" + numOfTrees, LogLevel.DEBUG);
		// Add end points to the corresponding tree
		for (int i = 0; i < this.totalNumberOfEndPoints; i++) {
			final SkeletonPoint p = this.listOfEndPoints.get(i);
			this.endPointsTree[getPixel(treeIS, p) - 1].add(p);
		}

		// Add junction voxels to the corresponding tree
		for (int i = 0; i < this.totalNumberOfJunctionVoxels; i++) {
			final SkeletonPoint p = this.listOfJunctionVoxels.get(i);
			this.junctionVoxelTree[getPixel(treeIS, p) - 1].add(p);
		}

		// Add special slab voxels to the corresponding tree
		for (final SkeletonPoint p : this.listOfStartingSlabVoxels) {
			this.startingSlabTree[getPixel(treeIS, p) - 1].add(p);
		}

		// Assign number of end points and junction voxels per tree
		for (int iTree = 0; iTree < this.numOfTrees; iTree++) {
			this.numberOfEndPoints[iTree] = this.endPointsTree[iTree].size();
			this.numberOfJunctionVoxels[iTree] = this.junctionVoxelTree[iTree].size();
		}

	} // end divideVoxelsByTrees

	// -----------------------------------------------------------------------
	/**
	 * Ask memory for trees.
	 */
	@SuppressWarnings("unchecked")
	private void initializeTrees() {
		this.numberOfBranches = new int[this.numOfTrees];
		this.numberOfEndPoints = new int[this.numOfTrees];
		this.numberOfJunctionVoxels = new int[this.numOfTrees];
		this.numberOfJunctions = new int[this.numOfTrees];
		this.numberOfSlabs = new int[this.numOfTrees];
		this.numberOfTriplePoints = new int[this.numOfTrees];
		this.numberOfQuadruplePoints = new int[this.numOfTrees];
		this.averageBranchLength = new double[this.numOfTrees];
		this.maximumBranchLength = new double[this.numOfTrees];
		this.endPointsTree = new ArrayList[this.numOfTrees];
		this.junctionVoxelTree = new ArrayList[this.numOfTrees];
		this.startingSlabTree = new ArrayList[this.numOfTrees];
		this.listOfSingleJunctions = new ArrayList[this.numOfTrees];

		this.skeletonGraph = new SkeletonGraph[this.numOfTrees];

		for (int i = 0; i < this.numOfTrees; i++) {
			this.endPointsTree[i] = new ArrayList<>();
			this.junctionVoxelTree[i] = new ArrayList<>();
			this.startingSlabTree[i] = new ArrayList<>();
			this.listOfSingleJunctions[i] = new ArrayList<>();
		}
		this.junctionVertex = new Vertex[this.numOfTrees][];
	}// end method initializeTrees

	// -----------------------------------------------------------------------
	/**
	 * Show results table.
	 */
	@SuppressWarnings({ "deprecation", "unused" })
	private void showResults() {
		final ResultsTable rt = new ResultsTable();

		final String[] head =
				{ "Skeleton", "# Branches", "# Junctions", "# End-point voxels", "# Junction voxels", "# Slab voxels",
						"Average Branch Length", "# Triple points", "# Quadruple points", "Maximum Branch Length" };

		for (int i = 1; i < head.length; i++)
			rt.setHeading(i, head[i]);

		for (int i = 0; i < this.numOfTrees; i++) {
			rt.incrementCounter();

			rt.addValue(1, this.numberOfBranches[i]);
			skeletonGraph[i].setQuantifiedProperty("n_branches", this.numberOfBranches[i]);
			rt.addValue(2, this.numberOfJunctions[i]);
			skeletonGraph[i].setQuantifiedProperty("n_junctions", this.numberOfJunctions[i]);
			rt.addValue(3, this.numberOfEndPoints[i]);
			skeletonGraph[i].setQuantifiedProperty("n_end_points", this.numberOfEndPoints[i]);
			rt.addValue(4, this.numberOfJunctionVoxels[i]);
			skeletonGraph[i].setQuantifiedProperty("n_junction_voxels", this.numberOfJunctionVoxels[i]);
			rt.addValue(5, this.numberOfSlabs[i]);
			skeletonGraph[i].setQuantifiedProperty("n_slabs", this.numberOfSlabs[i]);
			rt.addValue(6, this.averageBranchLength[i]);
			skeletonGraph[i].setQuantifiedProperty("av_branch_length", (float) this.averageBranchLength[i]);
			skeletonGraph[i].setQuantifiedProperty("sum_branch_lengths", (float) this.averageBranchLength[i]
					* this.numberOfBranches[i]);
			rt.addValue(7, this.numberOfTriplePoints[i]);
			skeletonGraph[i].setQuantifiedProperty("n_triple_points", this.numberOfTriplePoints[i]);
			rt.addValue(8, this.numberOfQuadruplePoints[i]);
			skeletonGraph[i].setQuantifiedProperty("n_quad_points", this.numberOfQuadruplePoints[i]);
			rt.addValue(9, this.maximumBranchLength[i]);
			skeletonGraph[i].setQuantifiedProperty("max_branch_length", (float) this.maximumBranchLength[i]);

			// if (0 == i % 100)
			// rt.show("Results");
		}

		if (true)
			return;

		rt.show("Results");

		// Extra information
		if (this.verbose) {
			// New results table
			final ResultsTable extra_rt = new ResultsTable();

			final String[] extra_head =
					{ "Branch", "Skeleton ID", "Branch length", "V1 x", "V1 y", "V1 z", "V2 x", "V2 y", "V2 z",
							"Euclidean distance" };

			for (int i = 1; i < extra_head.length; i++)
				extra_rt.setHeading(i, extra_head[i]);
			// Edge comparator (by branch length)
			Comparator<Edge> comp = new Comparator<Edge>() {
				@Override
				public int compare(Edge o1, Edge o2) {
					final double diff = o1.getLength() - o2.getLength();
					if (diff < 0)
						return 1;
					else if (diff == 0)
						return 0;
					else
						return -1;
				}

				@Override
				public boolean equals(Object o) {
					return false;
				}

				@Override
				public int hashCode() {
					throw new RuntimeException("Unimplemented hashcode");
				}
			};
			// Display branch information for each tree
			for (int i = 0; i < this.numOfTrees; i++) {
				final ArrayList<Edge> listEdges = this.skeletonGraph[i].getEdges();
				// Sort branches by length
				Collections.sort(listEdges, comp);
				for (final Edge e : listEdges) {
					extra_rt.incrementCounter();
					extra_rt.addValue(1, i + 1);
					extra_rt.addValue(2, e.getLength());
					extra_rt.addValue(3, e.getV1().getPoints().get(0).x * this.imRef.getCalibration().pixelWidth);
					extra_rt.addValue(4, e.getV1().getPoints().get(0).y * this.imRef.getCalibration().pixelHeight);
					extra_rt.addValue(5, e.getV1().getPoints().get(0).z * this.imRef.getCalibration().pixelDepth);
					extra_rt.addValue(6, e.getV2().getPoints().get(0).x * this.imRef.getCalibration().pixelWidth);
					extra_rt.addValue(7, e.getV2().getPoints().get(0).y * this.imRef.getCalibration().pixelHeight);
					extra_rt.addValue(8, e.getV2().getPoints().get(0).z * this.imRef.getCalibration().pixelDepth);
					extra_rt.addValue(9, this.calculateDistance(e.getV1().getPoints().get(0), e.getV2().getPoints()
							.get(0)));
				}
			}
			extra_rt.show("Branch information");
		}

	}// end method showResults

	/**
	 * Returns the analysis results in a SkeletonResult object.
	 * <p>
	 *
	 * @return The results of the skeleton analysis.
	 */
	SkeletonResult assembleResults() {
		SkeletonResult result = new SkeletonResult(numOfTrees);
		result.setBranches(numberOfBranches);
		result.setJunctions(numberOfJunctions);
		result.setEndPoints(numberOfEndPoints);
		result.setJunctionVoxels(numberOfJunctionVoxels);
		result.setSlabs(numberOfSlabs);
		result.setAverageBranchLength(averageBranchLength);
		result.setTriples(numberOfTriplePoints);
		result.setQuadruples(numberOfQuadruplePoints);
		result.setMaximumBranchLength(maximumBranchLength);

		result.setListOfEndPoints(listOfEndPoints);
		result.setListOfJunctionVoxels(listOfJunctionVoxels);
		result.setListOfSlabVoxels(listOfSlabVoxels);
		result.setListOfStartingSlabVoxels(listOfStartingSlabVoxels);

		result.setGraph(skeletonGraph);

		result.calculateNumberOfVoxels();

		return result;
	}

	// -----------------------------------------------------------------------
	/**
	 * Visit skeleton from end points and register measures.
	 * 
	 * @param taggedImage
	 * 
	 * @deprecated
	 */
	@SuppressWarnings("unused")
	private void visitSkeleton(IPluginIOStack taggedImage) {

		// length of branches
		double branchLength = 0;
		int numberOfBranches = 0;
		double maximumBranchLength = 0;
		double averageBranchLength = 0;
		SkeletonPoint initialPoint = null;
		SkeletonPoint finalPoint = null;

		// Visit branches starting at end points
		for (int i = 0; i < this.totalNumberOfEndPoints; i++) {
			SkeletonPoint endPointCoord = this.listOfEndPoints.get(i);

			// visit branch until next junction or end point.
			double length = visitBranch(endPointCoord);

			if (length == 0)
				continue;

			// increase number of branches
			numberOfBranches++;
			branchLength += length;

			// update maximum branch length
			if (length > maximumBranchLength) {
				maximumBranchLength = length;
				initialPoint = endPointCoord;
				finalPoint = this.auxPoint;
			}
		}

		// Now visit branches starting at junctions
		for (int i = 0; i < this.totalNumberOfJunctionVoxels; i++) {
			SkeletonPoint junctionCoord = this.listOfJunctionVoxels.get(i);

			// Mark junction as visited
			setVisited(junctionCoord, true);

			SkeletonPoint nextPoint = getNextUnvisitedVoxel(junctionCoord);

			while (nextPoint != null) {
				branchLength += calculateDistance(junctionCoord, nextPoint);

				double length = visitBranch(nextPoint);

				branchLength += length;

				// Increase number of branches
				if (length != 0) {
					numberOfBranches++;
					// update maximum branch length
					if (length > maximumBranchLength) {
						maximumBranchLength = length;
						initialPoint = junctionCoord;
						finalPoint = this.auxPoint;
					}
				}

				nextPoint = getNextUnvisitedVoxel(junctionCoord);
			}
		}

		// Average length
		averageBranchLength = branchLength / numberOfBranches;

	} // end visitSkeleton

	/* ----------------------------------------------------------------------- */
	/**
	 * Visit skeleton starting at end-points, junctions and slab of circular
	 * skeletons, and record measurements.
	 * 
	 * @param taggedImage
	 *            tag skeleton image
	 * @param treeImage
	 *            skeleton image with tree classification
	 * @param currentTree
	 *            number of the tree to be visited
	 */
	private void visitSkeleton(IPluginIOStack taggedImage, IPluginIOStack treeImage, int currentTree) {
		// tree index
		final int iTree = currentTree - 1;

		if (debug) {
			// Junction vertices in the tree
			Utils.log("this.junctionVertex[" + (iTree) + "].length = " + this.junctionVertex[iTree].length,
					LogLevel.VERBOSE_DEBUG);
			for (int i = 0; i < this.junctionVertex[iTree].length; i++) {
				Utils.log(" vertices points: " + this.junctionVertex[iTree][i], LogLevel.VERBOSE_DEBUG);
			}
		}

		// Create new skeletonGraph
		this.skeletonGraph[iTree] =
				new SkeletonGraph(taggedImage.getDimensions().width, taggedImage.getDimensions().height, taggedImage
						.getDimensions().depth, taggedImage.getCalibration());
		// Add all junction vertices
		for (int i = 0; i < this.junctionVertex[iTree].length; i++)
			this.skeletonGraph[iTree].addVertex(this.junctionVertex[iTree][i]);

		if (debug)
			Utils.log(" Analyzing tree number " + currentTree, LogLevel.VERBOSE_DEBUG);
		// length of branches
		double branchLength = 0;

		this.maximumBranchLength[iTree] = 0;
		this.numberOfSlabs[iTree] = 0;

		// Visit branches starting at end points
		for (int i = 0; i < this.numberOfEndPoints[iTree]; i++) {
			final SkeletonPoint endPointCoord = this.endPointsTree[iTree].get(i);

			if (debug)
				Utils.log("\n*** visit from end point: " + endPointCoord + " *** ", LogLevel.VERBOSE_DEBUG);

			// Skip when visited
			if (isVisited(endPointCoord)) {
				// if(this.initialPoint[iTree] == null)
				// Utils.displayError("WEIRD:" + " (" + endPointCoord.x + ", " + endPointCoord.y + ", " +
				// endPointCoord.z + ")");
				if (debug)
					Utils.log("visited = (" + endPointCoord.x + ", " + endPointCoord.y + ", " + endPointCoord.z + ")",
							LogLevel.VERBOSE_DEBUG);
				continue;
			}

			// Initial vertex
			Vertex v1 = new Vertex();
			v1.addPoint(endPointCoord);
			this.skeletonGraph[iTree].addVertex(v1);
			if (i == 0)
				this.skeletonGraph[iTree].setRoot(v1);

			// slab list for the edge
			this.slabList = new ArrayList<>();

			// Otherwise, visit branch until next junction or end point.
			final double length = visitBranch(endPointCoord, iTree);

			// If length is 0, it means the tree is formed by only one voxel.
			if (length == 0) {
				if (debug)
					Utils.log("set initial point to final point", LogLevel.VERBOSE_DEBUG);
				continue;
			}

			// Add branch to skeletonGraph
			if (debug)
				Utils.log("adding branch from " + v1.getPoints().get(0) + " to "
						+ this.auxFinalVertex.getPoints().get(0), LogLevel.VERBOSE_DEBUG);
			this.skeletonGraph[iTree].addVertex(this.auxFinalVertex);
			this.skeletonGraph[iTree].addEdge(new Edge(v1, this.auxFinalVertex, this.slabList, length));

			// increase number of branches
			this.numberOfBranches[iTree]++;

			if (debug)
				Utils.log("increased number of branches, length = " + length, LogLevel.VERBOSE_DEBUG);

			branchLength += length;

			// update maximum branch length
			if (length > this.maximumBranchLength[iTree]) {
				this.maximumBranchLength[iTree] = length;
			}
		}

		// If there is no end points, set the first junction as root.
		if (this.numberOfEndPoints[iTree] == 0 && this.junctionVoxelTree[iTree].size() > 0)
			this.skeletonGraph[iTree].setRoot(this.junctionVertex[iTree][0]);

		if (debug)
			Utils.log(" --------------------------- ", LogLevel.VERBOSE_DEBUG);

		// Now visit branches starting at junctions
		// 08/26/2009 Changed the loop to visit first the junction voxel that are
		// forming a single junction.
		for (int i = 0; i < this.junctionVertex[iTree].length; i++) {
			for (int j = 0; j < this.junctionVertex[iTree][i].getPoints().size(); j++) {
				final SkeletonPoint junctionCoord = this.junctionVertex[iTree][i].getPoints().get(j);

				if (debug)
					Utils.log("\n*** visit from junction " + junctionCoord + " *** ", LogLevel.VERBOSE_DEBUG);

				// Mark junction as visited
				setVisited(junctionCoord, true);

				SkeletonPoint nextPoint = getNextUnvisitedVoxel(junctionCoord);

				while (nextPoint != null) {
					// Do not count adjacent junctions
					if (!isJunction(nextPoint)) {
						// Create skeletonGraph edge
						this.slabList = new ArrayList<>();
						this.slabList.add(nextPoint);

						// Calculate distance from junction to that point
						double length = calculateDistance(junctionCoord, nextPoint);

						// Visit branch
						this.auxPoint = null;
						length += visitBranch(nextPoint, iTree);

						// Increase total length of branches
						branchLength += length;

						// Increase number of branches
						if (length != 0) {
							if (this.auxPoint == null)
								this.auxPoint = nextPoint;

							this.numberOfBranches[iTree]++;

							// Initial vertex
							Vertex initialVertex = null;
							for (int k = 0; k < this.junctionVertex[iTree].length; k++)
								if (this.junctionVertex[iTree][k].isVertexPoint(junctionCoord)) {
									initialVertex = this.junctionVertex[iTree][k];
									break;
								}

							// If the final point is a slab, then we add the path to the
							// neighbor junction voxel not belonging to the initial vertex
							// (unless it is a self loop)
							if (isSlab(this.auxPoint)) {
								final SkeletonPoint aux = this.auxPoint;
								// Utils.log("Looking for " + this.auxPoint + " in the list of vertices...");
								this.auxPoint = getVisitedJunctionNeighbor(this.auxPoint, initialVertex);
								this.auxFinalVertex = findPointVertex(this.junctionVertex[iTree], this.auxPoint);
								if (this.auxPoint == null) {
									// Utils.displayError("SkeletonPoint "+ aux + " has not neighbor end junction!");
									// Inner loop
									this.auxFinalVertex = initialVertex;
									this.auxPoint = aux;
								}
								length += calculateDistance(this.auxPoint, aux);
							}

							if (debug)
								Utils.log("increased number of branches, length = " + length + " (last point = "
										+ this.auxPoint + ")", LogLevel.VERBOSE_DEBUG);
							// update maximum branch length
							if (length > this.maximumBranchLength[iTree]) {
								this.maximumBranchLength[iTree] = length;
							}

							// Create skeletonGraph branch

							// Add branch to skeletonGraph
							if (debug)
								Utils.log("adding branch from " + initialVertex.getPoints().get(0) + " to "
										+ this.auxFinalVertex.getPoints().get(0), LogLevel.VERBOSE_DEBUG);
							this.skeletonGraph[iTree].addEdge(new Edge(initialVertex, this.auxFinalVertex,
									this.slabList, length));
						}
					} else
						setVisited(nextPoint, true);

					nextPoint = getNextUnvisitedVoxel(junctionCoord);
				}
			}
		}

		if (debug)
			Utils.log(" --------------------------- ", LogLevel.VERBOSE_DEBUG);

		// Finally visit branches starting at slabs (special case for circular trees)
		if (this.startingSlabTree[iTree].size() == 1) {
			if (debug)
				Utils.log("visit from slabs", LogLevel.VERBOSE_DEBUG);

			final SkeletonPoint startCoord = this.startingSlabTree[iTree].get(0);

			// Create circular skeletonGraph (only one vertex)
			final Vertex v1 = new Vertex();
			v1.addPoint(startCoord);
			this.skeletonGraph[iTree].addVertex(v1);

			this.slabList = new ArrayList<>();
			this.slabList.add(startCoord);

			this.numberOfSlabs[iTree]++;

			// visit branch until finding visited voxel.
			final double length = visitBranch(startCoord, iTree);

			if (length != 0) {
				// increase number of branches
				this.numberOfBranches[iTree]++;
				branchLength += length;

				// update maximum branch length
				if (length > this.maximumBranchLength[iTree]) {
					this.maximumBranchLength[iTree] = length;
				}
			}

			// Create circular edge
			this.skeletonGraph[iTree].addEdge(new Edge(v1, v1, this.slabList, length));
		}

		if (debug)
			Utils.log(" --------------------------- ", LogLevel.VERBOSE_DEBUG);

		if (this.numberOfBranches[iTree] == 0)
			return;
		// Average length
		this.averageBranchLength[iTree] = branchLength / this.numberOfBranches[iTree];

		if (debug) {
			Utils.log("Num of vertices = " + this.skeletonGraph[iTree].getVertices().size() + " num of edges = "
					+ this.skeletonGraph[iTree].getEdges().size(), LogLevel.VERBOSE_DEBUG);
			for (int i = 0; i < this.skeletonGraph[iTree].getVertices().size(); i++) {
				Vertex v = this.skeletonGraph[iTree].getVertices().get(i);
				Utils.log(" vertex " + v.getPoints().get(0) + " has neighbors: ", LogLevel.VERBOSE_DEBUG);
				for (int j = 0; j < v.getBranches().size(); j++) {
					final Vertex v1 = v.getBranches().get(j).getV1();
					final Vertex oppositeVertex = v1.equals(v) ? v.getBranches().get(j).getV2() : v1;
					Utils.log(j + ": " + oppositeVertex.getPoints().get(0), LogLevel.VERBOSE_DEBUG);
				}

			}

			Utils.log(" --------------------------- ", LogLevel.VERBOSE_DEBUG);
			for (int i = 0; i < this.junctionVertex[iTree].length; i++) {
				Utils.log("Junction #" + i + " is formed by: ", LogLevel.VERBOSE_DEBUG);
				for (int j = 0; j < this.junctionVertex[iTree][i].getPoints().size(); j++)
					Utils.log(j + ": " + this.junctionVertex[iTree][i].getPoints().get(j), LogLevel.VERBOSE_DEBUG);
			}
		}

	} // end visitSkeleton

	/* ----------------------------------------------------------------------- */
	/**
	 * Color the different trees in the skeleton.
	 * 
	 * @param taggedImage
	 * 
	 * @return image with every tree tagged with a different number
	 */
	private IPluginIOStack markTrees(IPluginIOStack taggedImage) {
		if (debug)
			Utils.log("=== Mark Trees ===", LogLevel.VERBOSE_DEBUG);

		IPluginIOStack outputImage = (PluginIOStack) taggedImage.duplicateStructure(null, -1, -1, false);
		outputImage.computePixelArray();

		this.numOfTrees = 0;

		short color = 0;

		float maxValue = 0;
		Object pixels = outputImage.getStackPixelArray()[0];
		if (pixels instanceof float[])
			maxValue = Float.MAX_VALUE;
		else if (pixels instanceof byte[])
			maxValue = Byte.MAX_VALUE;
		else if (pixels instanceof short[])
			maxValue = Short.MAX_VALUE;
		else if (pixels instanceof int[])
			maxValue = Integer.MAX_VALUE;

		// Visit trees starting at end points
		for (int i = 0; i < this.totalNumberOfEndPoints; i++) {
			SkeletonPoint endPointCoord = this.listOfEndPoints.get(i);

			if (isVisited(endPointCoord))
				continue;

			color++;

			if (color >= maxValue) {
				throw new PluginRuntimeException("More than " + (color)
						+ " skeletons in the image. AnalyzeSkeleton can only process up to " + maxValue, true);
				// return outputImage;
			}

			if (debug)
				Utils.log("-- Visit tree from end-point: " + endPointCoord, LogLevel.VERBOSE_DEBUG);
			// Visit the entire tree.

			visitTree(endPointCoord, outputImage, color);

			// increase number of trees
			this.numOfTrees++;
		}

		// Visit trees starting at junction points
		// (some circular trees do not have end points)
		for (int i = 0; i < this.totalNumberOfJunctionVoxels; i++) {
			SkeletonPoint junctionCoord = this.listOfJunctionVoxels.get(i);
			if (isVisited(junctionCoord))
				continue;

			color++;

			if (color >= maxValue) {
				Utils.displayMessage("More than " + (color)
						+ " skeletons in the image. AnalyzeSkeleton can only process up to " + maxValue, true,
						LogLevel.ERROR);
				throw new RuntimeException("Too many skeletons to tag");
				// return outputImage;
			}

			if (debug)
				Utils.log("-- Visit tree from junction: " + junctionCoord, LogLevel.VERBOSE_DEBUG);

			// else, visit branch until next junction or end point.
			int length = visitTree(junctionCoord, outputImage, color);

			if (length == 0) {
				color--; // the color was not used
				continue;
			}

			// increase number of trees
			this.numOfTrees++;
		}

		// Check for unvisited slab voxels
		// (just in case there are circular trees without junctions)
		for (SkeletonPoint p : this.listOfSlabVoxels) {
			if (!isVisited(p)) {
				// Mark that voxel as the start point of the circular skeleton
				this.listOfStartingSlabVoxels.add(p);

				color++;

				if (color >= maxValue) {
					Utils.displayMessage("More than " + (color)
							+ " skeletons in the image. AnalyzeSkeleton can only process up to " + maxValue, true,
							LogLevel.ERROR);
					throw new RuntimeException("Too many skeletons to tag");
					// return outputImage;
				}

				if (debug)
					Utils.log("-- Visit tree from slab:", LogLevel.VERBOSE_DEBUG);

				// else, visit branch until next junction or end point.
				int length = visitTree(p, outputImage, color);

				if (length == 0) {
					color--; // the color was not used
					continue;
				}

				// increase number of trees
				this.numOfTrees++;
			}
		}

		Utils.log("Number of trees =" + this.numOfTrees + " number of colors=" + color, LogLevel.VERBOSE_DEBUG);

		// Show tree image.
		/*
		 * if(debug)
		 * {
		 * ImagePlus treesIP = new ImagePlus("Trees skeleton", outputImage);
		 * treesIP.show();
		 * 
		 * // Set same calibration as the input image
		 * treesIP.setCalibration(this.imRef.getCalibration());
		 * 
		 * // We apply the Fire LUT and reset the min and max to be between 0-255.
		 * IJ.run("Fire");
		 * 
		 * //IJ.resetMinAndMax();
		 * treesIP.resetDisplayRange();
		 * treesIP.updateAndDraw();
		 * }
		 */

		// Reset visited variable
		resetVisited();

		// Utils.log("Number of trees: " + this.numOfTrees + ", # colors = " + color);

		return outputImage;

	} /* end markTrees */

	// --------------------------------------------------------------
	/**
	 * Visit tree marking the voxels with a reference tree color.
	 * 
	 * @param startingPoint
	 *            starting tree point
	 * @param outputImage
	 *            3D image to visit
	 * @param color
	 *            reference tree color
	 * @return number of voxels in the tree
	 */
	private int visitTree(SkeletonPoint startingPoint, IPluginIOStack outputImage, short color) {
		int numOfVoxels = 0;

		if (debug)
			Utils.log("                      visiting " + startingPoint + " color = " + color, LogLevel.VERBOSE_DEBUG);

		if (isVisited(startingPoint)) {

			Utils.log("Starting point already visited", LogLevel.VERBOSE_DEBUG);
			return 0;

		}

		// Set pixel color
		setPixel(outputImage, startingPoint.x, startingPoint.y, startingPoint.z, color);
		setVisited(startingPoint, true);

		ArrayList<SkeletonPoint> toRevisit = new ArrayList<>();

		// Add starting point to revisit list if it is a junction
		if (isJunction(startingPoint))
			toRevisit.add(startingPoint);

		SkeletonPoint nextPoint = getNextUnvisitedVoxel(startingPoint);

		while (nextPoint != null || toRevisit.size() != 0) {
			if (nextPoint != null) {
				if (!isVisited(nextPoint)) {
					numOfVoxels++;
					if (debug)
						Utils.log("visiting " + nextPoint + " color = " + color, LogLevel.VERBOSE_DEBUG);

					// Set color and visit flat
					// Utils.log("Set color of "+nextPoint+" to "+color,LogLevel.VERBOSE_DEBUG);
					setPixel(outputImage, nextPoint.x, nextPoint.y, nextPoint.z, color);
					setVisited(nextPoint, true);

					// If it is a junction, add it to the revisit list
					if (isJunction(nextPoint))
						toRevisit.add(nextPoint);

					// Calculate next point to visit
					nextPoint = getNextUnvisitedVoxel(nextPoint);
				}
			} else // revisit list
			{
				nextPoint = toRevisit.get(0);
				if (debug)
					Utils.log("visiting " + nextPoint + " color = " + color, LogLevel.VERBOSE_DEBUG);

				// Calculate next point to visit
				nextPoint = getNextUnvisitedVoxel(nextPoint);
				// Maintain junction in the list until there is no more branches
				if (nextPoint == null)
					toRevisit.remove(0);
			}
		}

		return numOfVoxels;
	} // end method visitTree

	// -----------------------------------------------------------------------
	/**
	 * Visit a branch and calculate length.
	 * 
	 * @param startingPoint
	 *            starting coordinates
	 * @return branch length
	 * 
	 * @deprecated
	 */
	private double visitBranch(SkeletonPoint startingPoint) {
		double length = 0;

		// mark starting point as visited
		setVisited(startingPoint, true);

		// Get next unvisited voxel
		SkeletonPoint nextPoint = getNextUnvisitedVoxel(startingPoint);

		if (nextPoint == null)
			return 0;

		SkeletonPoint previousPoint = startingPoint;

		// We visit the branch until we find an end point or a junction
		while (nextPoint != null && isSlab(nextPoint)) {
			// Add length
			length += calculateDistance(previousPoint, nextPoint);

			// Mark as visited
			setVisited(nextPoint, true);

			// Move in the skeletonGraph
			previousPoint = nextPoint;
			nextPoint = getNextUnvisitedVoxel(previousPoint);
		}

		if (nextPoint != null) {
			// Add distance to last point
			length += calculateDistance(previousPoint, nextPoint);

			// Mark last point as visited
			setVisited(nextPoint, true);
		}

		this.auxPoint = previousPoint;

		return length;
	}// end visitBranch

	// -----------------------------------------------------------------------
	/**
	 * Visit a branch and calculate length in a specific tree
	 * 
	 * @param startingPoint
	 *            starting coordinates
	 * @param iTree
	 *            tree index
	 * @return branch length
	 */
	private double visitBranch(SkeletonPoint startingPoint, int iTree) {
		// Utils.log("startingPoint = (" + startingPoint.x + ", " + startingPoint.y + ", " + startingPoint.z + ")");
		double length = 0;

		// mark starting point as visited
		setVisited(startingPoint, true);

		// Get next unvisited voxel
		SkeletonPoint nextPoint = getNextUnvisitedVoxel(startingPoint);

		if (nextPoint == null)
			return 0;

		SkeletonPoint previousPoint = startingPoint;

		// We visit the branch until we find an end point or a junction
		while (nextPoint != null && isSlab(nextPoint)) {
			this.numberOfSlabs[iTree]++;

			// Add slab voxel to the edge
			this.slabList.add(nextPoint);

			// Add length
			length += calculateDistance(previousPoint, nextPoint);

			// Mark as visited
			setVisited(nextPoint, true);

			// Move in the skeletonGraph
			previousPoint = nextPoint;
			nextPoint = getNextUnvisitedVoxel(previousPoint);
		}

		if (nextPoint != null) {
			// Add distance to last point
			length += calculateDistance(previousPoint, nextPoint);

			// Mark last point as visited
			setVisited(nextPoint, true);

			// Mark final vertex
			if (isEndPoint(nextPoint)) {
				this.auxFinalVertex = new Vertex();
				this.auxFinalVertex.addPoint(nextPoint);
			} else if (isJunction(nextPoint)) {
				this.auxFinalVertex = findPointVertex(this.junctionVertex[iTree], nextPoint);
				/*
				 * int j = 0;
				 * for(j = 0; j < this.junctionVertex[iTree].length; j++)
				 * if(this.junctionVertex[iTree][j].isVertexPoint(nextPoint))
				 * {
				 * this.auxFinalVertex = this.junctionVertex[iTree][j];
				 * Utils.log(" " + nextPoint + " belongs to junction " + this.auxFinalVertex.getPoints().get(0));
				 * break;
				 * }
				 * if(j == this.junctionVertex[iTree].length)
				 * Utils.log("point " + nextPoint + " was not found in vertex list!");
				 */
			}

			this.auxPoint = nextPoint;
		} else
			this.auxPoint = previousPoint;

		// Utils.log("finalPoint = (" + nextPoint.x + ", " + nextPoint.y + ", " + nextPoint.z + ")");
		return length;
	} // end visitBranch

	// -----------------------------------------------------------------------
	/**
	 * Find vertex in an array given a specific vertex point.
	 * 
	 * @param vertex
	 *            array of search
	 * @param p
	 *            vertex point
	 * @return vertex containing that point
	 */
	public static Vertex findPointVertex(Vertex[] vertex, SkeletonPoint p) {
		int j = 0;
		for (j = 0; j < vertex.length; j++)
			if (vertex[j].isVertexPoint(p)) {
				if (debug)
					Utils.log(" " + p + " belongs to junction " + vertex[j].getPoints().get(0), LogLevel.VERBOSE_DEBUG);
				return vertex[j];
			}
		if (debug)
			Utils.log("point " + p + " was not found in vertex list! (vertex.length= " + vertex.length + ")",
					LogLevel.VERBOSE_DEBUG);
		return null;
	}

	// -----------------------------------------------------------------------
	/**
	 * Calculate distance between two points in 3D.
	 * 
	 * @param point1
	 *            first point coordinates
	 * @param point2
	 *            second point coordinates
	 * @return distance (in the corresponding units)
	 */
	private double calculateDistance(SkeletonPoint point1, SkeletonPoint point2) {
		if (inputImage.getCalibration() == null)
			return Math.sqrt(Math.pow((point1.x - point2.x), 2) + Math.pow((point1.y - point2.y), 2)
					+ Math.pow((point1.z - point2.z), 2));
		else
			return Math.sqrt(Math.pow((point1.x - point2.x) * inputImage.getCalibration().pixelWidth, 2)
					+ Math.pow((point1.y - point2.y) * inputImage.getCalibration().pixelHeight, 2)
					+ Math.pow((point1.z - point2.z) * inputImage.getCalibration().pixelDepth, 2));
	}

	// -----------------------------------------------------------------------
	/**
	 * Calculate number of junction skipping neighbor junction voxels
	 * 
	 * @param treeIS
	 *            tree stack
	 */
	private void groupJunctions(IPluginIOStack treeIS) {
		// Mark all unvisited
		resetVisited();

		for (int iTree = 0; iTree < this.numOfTrees; iTree++) {
			// Visit list of junction voxels
			for (int i = 0; i < this.numberOfJunctionVoxels[iTree]; i++) {
				SkeletonPoint pi = this.junctionVoxelTree[iTree].get(i);

				if (!isVisited(pi))
					fusionNeighborJunction(pi, this.listOfSingleJunctions[iTree]);
			}
		}

		// Count number of single junctions for every tree in the image
		for (int iTree = 0; iTree < this.numOfTrees; iTree++) {
			if (debug)
				Utils.log("this.listOfSingleJunctions[" + iTree + "].size() = "
						+ this.listOfSingleJunctions[iTree].size(), LogLevel.VERBOSE_DEBUG);

			this.numberOfJunctions[iTree] = this.listOfSingleJunctions[iTree].size();

			// Create array of junction vertices for the skeletonGraph
			this.junctionVertex[iTree] = new Vertex[this.listOfSingleJunctions[iTree].size()];

			for (int j = 0; j < this.listOfSingleJunctions[iTree].size(); j++) {
				final ArrayList<SkeletonPoint> list = this.listOfSingleJunctions[iTree].get(j);
				this.junctionVertex[iTree][j] = new Vertex();
				for (final SkeletonPoint p : list)
					this.junctionVertex[iTree][j].addPoint(p);

			}
		}

		// Mark all unvisited
		resetVisited();
	}

	// -----------------------------------------------------------------------
	/**
	 * Reset visit variable and set it to false.
	 */
	private void resetVisited() {
		// Reset visited variable
		this.visited = new boolean[this.width][this.height][inputImage.getDepth() + 1];

	}

	// -----------------------------------------------------------------------
	/**
	 * Fusion neighbor junctions voxels into the same list.
	 * 
	 * @param startingPoint
	 *            starting junction voxel
	 * @param singleJunctionsList
	 *            list of single junctions
	 */
	private void fusionNeighborJunction(SkeletonPoint startingPoint,
			ArrayList<ArrayList<SkeletonPoint>> singleJunctionsList) {
		// Create new group of junctions
		ArrayList<SkeletonPoint> newGroup = new ArrayList<>();
		newGroup.add(startingPoint);

		// Mark the starting junction as visited
		setVisited(startingPoint, true);

		// Look for neighbor junctions and add them to the new group
		ArrayList<SkeletonPoint> toRevisit = new ArrayList<>();
		toRevisit.add(startingPoint);

		SkeletonPoint nextPoint = getNextUnvisitedJunctionVoxel(startingPoint);

		while (nextPoint != null || toRevisit.size() != 0) {
			if (nextPoint != null && !isVisited(nextPoint)) {
				// Add to the group
				newGroup.add(nextPoint);
				// Mark as visited
				setVisited(nextPoint, true);

				// add it to the revisit list
				toRevisit.add(nextPoint);

				// Calculate next junction point to visit
				nextPoint = getNextUnvisitedJunctionVoxel(nextPoint);
			} else // revisit list
			{
				nextPoint = toRevisit.get(0);
				// Utils.log("visiting " + nextPoint + " color = " + color);

				// Calculate next point to visit
				nextPoint = getNextUnvisitedJunctionVoxel(nextPoint);
				// Maintain junction in the list until there is no more branches
				if (nextPoint == null)
					toRevisit.remove(0);
			}
		}

		// Add group to the single junction list
		singleJunctionsList.add(newGroup);

	}// end method fusionNeighborJunction

	// -----------------------------------------------------------------------
	/**
	 * Check if two groups of voxels are neighbors.
	 * 
	 * @param g1
	 *            first group
	 * @param g2
	 *            second group
	 * 
	 * @return true if the groups have any neighbor voxel
	 */
	static boolean checkNeighborGroups(ArrayList<SkeletonPoint> g1, ArrayList<SkeletonPoint> g2) {
		for (SkeletonPoint pi : g1) {
			for (SkeletonPoint pj : g2) {
				if (isNeighbor(pi, pj))
					return true;
			}
		}
		return false;
	}

	// -----------------------------------------------------------------------
	/**
	 * Calculate number of triple and quadruple points in the skeleton. Triple and
	 * quadruple points are junctions with exactly 3 and 4 branches respectively.
	 */
	private void calculateTripleAndQuadruplePoints() {
		for (int iTree = 0; iTree < this.numOfTrees; iTree++) {
			// Visit the groups of junction voxels
			for (int i = 0; i < this.numberOfJunctions[iTree]; i++) {

				ArrayList<SkeletonPoint> groupOfJunctions = this.listOfSingleJunctions[iTree].get(i);

				// Count the number of slab and end-points neighbors of every voxel in the group
				int nBranch = 0;
				for (SkeletonPoint pj : groupOfJunctions) {
					// Get neighbors and check the slabs or end-points
					byte[] neighborhood = getNeighborhood(this.taggedImage, pj.x, pj.y, pj.z);
					for (int k = 0; k < 27; k++)
						if (neighborhood[k] == AnalyzeSkeleton.SLAB || neighborhood[k] == AnalyzeSkeleton.END_POINT)
							nBranch++;
				}
				// If the junction has only 3 slab/end-point neighbors, then it is a triple point
				if (nBranch == 3)
					this.numberOfTriplePoints[iTree]++;
				else if (nBranch == 4) // quadruple point if 4
					this.numberOfQuadruplePoints[iTree]++;
			}

		}

	}// end calculateTripleAndQuadruplePoints

	/* ----------------------------------------------------------------------- */
	/**
	 * Calculate if two points are neighbors.
	 * 
	 * @param point1
	 *            first point
	 * @param point2
	 *            second point
	 * @return true if the points are neighbors (26-pixel neighborhood)
	 */
	private static boolean isNeighbor(SkeletonPoint point1, SkeletonPoint point2) {
		return Math.sqrt(Math.pow((point1.x - point2.x), 2) + Math.pow((point1.y - point2.y), 2)
				+ Math.pow((point1.z - point2.z), 2)) <= Math.sqrt(3);
	}

	/* ----------------------------------------------------------------------- */
	/**
	 * Check if the point is slab.
	 * 
	 * @param skeletonPoint
	 *            actual point
	 * @return true if the point has slab status
	 */
	private boolean isSlab(SkeletonPoint skeletonPoint) {
		return getPixel(this.taggedImage, skeletonPoint.x, skeletonPoint.y, skeletonPoint.z) == AnalyzeSkeleton.SLAB;
	}

	/* ----------------------------------------------------------------------- */
	/**
	 * Check if the point is a junction.
	 * 
	 * @param skeletonPoint
	 *            actual point
	 * @return true if the point has slab status
	 */
	private boolean isJunction(SkeletonPoint skeletonPoint) {
		return getPixel(this.taggedImage, skeletonPoint.x, skeletonPoint.y, skeletonPoint.z) == AnalyzeSkeleton.JUNCTION;
	}

	/* ----------------------------------------------------------------------- */
	/**
	 * Check if the point is an end point.
	 * 
	 * @param skeletonPoint
	 *            actual point
	 * @return true if the point has slab status
	 */
	private boolean isEndPoint(SkeletonPoint skeletonPoint) {
		return getPixel(this.taggedImage, skeletonPoint.x, skeletonPoint.y, skeletonPoint.z) == AnalyzeSkeleton.END_POINT;
	}

	/* ----------------------------------------------------------------------- */
	/**
	 * Check if the point is a junction.
	 * 
	 * @param x
	 *            x- voxel coordinate
	 * @param y
	 *            y- voxel coordinate
	 * @param z
	 *            z- voxel coordinate
	 * @return true if the point has slab status
	 */
	private boolean isJunction(int x, int y, int z) {
		return getPixel(this.taggedImage, x, y, z) == AnalyzeSkeleton.JUNCTION;
	}

	/* ----------------------------------------------------------------------- */
	/**
	 * Get next unvisited neighbor voxel.
	 * 
	 * @param skeletonPoint
	 *            starting point
	 * @return unvisited neighbor or null if all neighbors are visited
	 */
	private SkeletonPoint getNextUnvisitedVoxel(SkeletonPoint skeletonPoint) {
		SkeletonPoint unvisitedNeighbor = null;

		// Check neighbors status
		for (int x = -1; x < 2; x++)
			for (int y = -1; y < 2; y++)
				for (int z = -1; z < 2; z++) {
					if (x == 0 && y == 0 && z == 0)
						continue;

					if (getPixel(this.inputImage, skeletonPoint.x + x, skeletonPoint.y + y, skeletonPoint.z + z) != 0
							&& !isVisited(skeletonPoint.x + x, skeletonPoint.y + y, skeletonPoint.z + z)) {
						unvisitedNeighbor =
								new SkeletonPoint(skeletonPoint.x + x, skeletonPoint.y + y, skeletonPoint.z + z);
						break;
					}

				}

		return unvisitedNeighbor;
	}// end getNextUnvisitedVoxel

	/* ----------------------------------------------------------------------- */
	/**
	 * Get next unvisited junction neighbor voxel.
	 * 
	 * @param skeletonPoint
	 *            starting point
	 * @return unvisited neighbor or null if all neighbors are visited
	 */
	private SkeletonPoint getNextUnvisitedJunctionVoxel(SkeletonPoint skeletonPoint) {
		SkeletonPoint unvisitedNeighbor = null;

		// Check neighbors status
		for (int x = -1; x < 2; x++)
			for (int y = -1; y < 2; y++)
				for (int z = -1; z < 2; z++) {
					if (x == 0 && y == 0 && z == 0)
						continue;

					if (getPixel(this.inputImage, skeletonPoint.x + x, skeletonPoint.y + y, skeletonPoint.z + z) != 0
							&& !isVisited(skeletonPoint.x + x, skeletonPoint.y + y, skeletonPoint.z + z)
							&& isJunction(skeletonPoint.x + x, skeletonPoint.y + y, skeletonPoint.z + z)) {
						unvisitedNeighbor =
								new SkeletonPoint(skeletonPoint.x + x, skeletonPoint.y + y, skeletonPoint.z + z);
						break;
					}

				}

		return unvisitedNeighbor;
	}// end getNextUnvisitedJunctionVoxel

	// -----------------------------------------------------------------------
	/**
	 * Get next visited junction neighbor voxel excluding the ones belonging
	 * to a give vertex
	 * 
	 * @param skeletonPoint
	 *            starting point
	 * @param exclude
	 *            exclusion vertex
	 * @return unvisited neighbor or null if all neighbors are visited
	 */
	private SkeletonPoint getVisitedJunctionNeighbor(SkeletonPoint skeletonPoint, Vertex exclude) {
		SkeletonPoint finalNeighbor = null;

		// Check neighbors status
		for (int x = -1; x < 2; x++)
			for (int y = -1; y < 2; y++)
				for (int z = -1; z < 2; z++) {
					if (x == 0 && y == 0 && z == 0)
						continue;

					final SkeletonPoint neighbor =
							new SkeletonPoint(skeletonPoint.x + x, skeletonPoint.y + y, skeletonPoint.z + z);

					if (getPixel(this.inputImage, neighbor) != 0 && isVisited(neighbor) && isJunction(neighbor)
							&& !exclude.getPoints().contains(neighbor)) {
						finalNeighbor = neighbor;
						break;
					}

				}

		return finalNeighbor;
	}// end getNextUnvisitedJunctionVoxel

	// -----------------------------------------------------------------------
	/**
	 * Check if a voxel is visited taking into account the borders.
	 * Out of range voxels are considered as visited.
	 * 
	 * @param skeletonPoint
	 * @return true if the voxel is visited
	 */
	private boolean isVisited(SkeletonPoint skeletonPoint) {
		return isVisited(skeletonPoint.x, skeletonPoint.y, skeletonPoint.z);
	}

	/* ----------------------------------------------------------------------- */
	/**
	 * Check if a voxel is visited taking into account the borders.
	 * Out of range voxels are considered as visited.
	 * 
	 * @param x
	 *            x- voxel coordinate
	 * @param y
	 *            y- voxel coordinate
	 * @param z
	 *            z- voxel coordinate
	 * @return true if the voxel is visited
	 */
	private boolean isVisited(int x, int y, int z) {
		if (x >= 0 && x < this.width && y >= 0 && y < this.height && z >= 1 && z <= this.depth)
			return this.visited[x][y][z];
		return true;
	}

	/* ----------------------------------------------------------------------- */
	/**
	 * Set value in the visited flags matrix.
	 * 
	 * @param x
	 *            x- voxel coordinate
	 * @param y
	 *            y- voxel coordinate
	 * @param z
	 *            z- voxel coordinate
	 * @param b
	 */
	private void setVisited(int x, int y, int z, boolean b) {
		if (x >= 0 && x < this.width && y >= 0 && y < this.height && z >= 1 && z <= this.depth)
			this.visited[x][y][z] = b;
	}

	/* ----------------------------------------------------------------------- */
	/**
	 * Set value in the visited flags matrix.
	 * 
	 * @param skeletonPoint
	 *            voxel coordinates
	 * @param b
	 *            visited flag value
	 */
	private void setVisited(SkeletonPoint skeletonPoint, boolean b) {
		int x = skeletonPoint.x;
		int y = skeletonPoint.y;
		int z = skeletonPoint.z;

		setVisited(x, y, z, b);
	}

	int[] totalNumberOfEndPointsArray;
	int[] totalNumberOfJunctionVoxelsArray;
	int[] totalNumberOfSlabsArray;

	ArrayList<SkeletonPoint>[] listOfEndPointsArray;
	ArrayList<SkeletonPoint>[] listOfJunctionVoxelsArray;
	ArrayList<SkeletonPoint>[] listOfSlabVoxelsArray;

	final AtomicInteger thread_registry = new AtomicInteger(0);

	@SuppressWarnings("unchecked")
	private void tagImage(final IPluginIOStack input, final IPluginIOStack output, final ProgressReporter progress)
			throws InterruptedException {

		// Tag voxels

		nCpus = Runtime.getRuntime().availableProcessors();
		if (input.getDepth() < nCpus)
			nCpus = input.getDepth();
		threads = newThreadArray();
		cancelled = false;

		listOfEndPointsArray = new ArrayList[nCpus];
		listOfJunctionVoxelsArray = new ArrayList[nCpus];
		listOfSlabVoxelsArray = new ArrayList[nCpus];

		for (int ithread = 0; ithread < threads.length; ithread++) {
			threads[ithread] = new Thread("AnalyzeSkeleton worker thread") {
				@Override
				public void run() {
					int threadid = thread_registry.getAndIncrement();
					listOfEndPointsArray[threadid] = new ArrayList<>(100);
					listOfJunctionVoxelsArray[threadid] = new ArrayList<>(100);
					listOfSlabVoxelsArray[threadid] = new ArrayList<>(100);

					for (int z = slice_registry.getAndIncrement(); z <= input.getDepth(); z =
							slice_registry.getAndIncrement()) {
						if (cancelled)
							return;

						for (int x = 0; x < input.getWidth(); x++)
							for (int y = 0; y < input.getHeight(); y++) {
								if (getPixel(input, x, y, z) != 0) {
									int numOfNeighbors = getNumberOfNeighbors(input, x, y, z);
									if (numOfNeighbors < 2) {
										setPixel(output, x, y, z, AnalyzeSkeleton.END_POINT);
										SkeletonPoint endPoint = new SkeletonPoint(x, y, z);
										Utils.log("End point at " + endPoint, LogLevel.VERBOSE_DEBUG);
										listOfEndPointsArray[threadid].add(endPoint);
									} else if (numOfNeighbors > 2) {
										setPixel(output, x, y, z, AnalyzeSkeleton.JUNCTION);
										SkeletonPoint junction = new SkeletonPoint(x, y, z);
										listOfJunctionVoxelsArray[threadid].add(junction);
									} else {
										setPixel(output, x, y, z, AnalyzeSkeleton.SLAB);
										SkeletonPoint slab = new SkeletonPoint(x, y, z);
										listOfSlabVoxelsArray[threadid].add(slab);
									}
								} else
									setPixel(output, x, y, z, (byte) 0);
							}

						int our_progress = ((int) (100.0 * (z) / input.getDepth()));
						if (our_progress > progress.getValue())
							progressSetValueThreadSafe(progress, our_progress); // not perfect but at least does not
																				// require synchronization
						if (indeterminateProgress) {
							progressSetIndeterminateThreadSafe(progress, false);
							indeterminateProgress = false;
						}
						if (Thread.interrupted()) {
							cancelled = true;
						}

					}
				}
			};
		}
		thread_registry.set(0);
		slice_registry.set(1);
		startAndJoin(threads);
		if (cancelled)
			throw new InterruptedException();

		totalNumberOfEndPoints = 0;
		for (int i = 0; i < threads.length; i++) {
			totalNumberOfEndPoints += listOfEndPointsArray[i].size();
			for (int j = 0; j < listOfEndPointsArray[i].size(); j++) {
				listOfEndPoints.add(listOfEndPointsArray[i].get(j));
			}
		}

		totalNumberOfSlabs = 0;
		for (int i = 0; i < nCpus; i++) {
			totalNumberOfSlabs += listOfSlabVoxelsArray[i].size();
			for (int j = 0; j < listOfSlabVoxelsArray[i].size(); j++) {
				listOfSlabVoxels.add(listOfSlabVoxelsArray[i].get(j));
			}
		}

		totalNumberOfJunctionVoxels = 0;
		for (int i = 0; i < nCpus; i++) {
			totalNumberOfJunctionVoxels += listOfJunctionVoxelsArray[i].size();
			for (int j = 0; j < listOfJunctionVoxelsArray[i].size(); j++) {
				listOfJunctionVoxels.add(listOfJunctionVoxelsArray[i].get(j));
			}
		}
		return;
	}// end method tagImage

	/* ----------------------------------------------------------------------- */
	/**
	 * Get number of neighbors of a voxel in a 3D image (0 border conditions).
	 * 
	 * @param channel
	 *            3D image (as a PluginIOStack)
	 * @param x
	 *            x- coordinate
	 * @param y
	 *            y- coordinate
	 * @param z
	 *            z- coordinate (in image stacks the indexes start at 1)
	 * @return corresponding 27-pixels neighborhood (0 if out of image)
	 */
	static int getNumberOfNeighbors(IPluginIOStack channel, int x, int y, int z) {
		int n = 0;
		byte[] neighborhood = getNeighborhood(channel, x, y, z);

		for (int i = 0; i < 27; i++)
			if (neighborhood[i] != 0)
				n++;
		// We return n-1 because neighborhood includes the actual voxel.
		return (n - 1);
	}// end method getNumberOfNeighbors

	// -----------------------------------------------------------------------
	/**
	 * Get average 3x3x3 neighborhood pixel value of a given point
	 * 
	 * 
	 * @param p
	 *            image coordinates
	 */
	@SuppressWarnings("unused")
	private static double getAverageNeighborhoodValue(IPluginIOStack channel, SkeletonPoint p) {
		byte[] neighborhood = getNeighborhood(channel, p);

		double avg = 0;
		for (byte element : neighborhood)
			avg += (element & 0xFF);
		if (neighborhood.length > 0)
			return avg / neighborhood.length;
		else
			return 0;
	}// end method getAverageNeighborhoodValue

	// -----------------------------------------------------------------------
	/**
	 * Get average neighborhood pixel value of a given point.
	 * 
	 * 
	 * @param p
	 *            image coordinates
	 */
	public static double getAverageNeighborhoodValue(final IPluginIOStack channel, final SkeletonPoint p,
			final int x_offset, final int y_offset, final int z_offset) {
		byte[] neighborhood = getNeighborhood(channel, p, x_offset, y_offset, z_offset);

		double avg = 0;
		for (byte element : neighborhood)
			avg += (element & 0xFF);
		if (neighborhood.length > 0)
			return avg / neighborhood.length;
		else
			return 0;
	}// end method getAverageNeighborhoodValue

	// -----------------------------------------------------------------------
	/**
	 * Get neighborhood of a pixel in a 3D image (0 border conditions).
	 * 
	 * @param channel
	 *            3D image (as a ChannelView)
	 * @param p
	 *            point coordinates
	 * @param x_offset
	 *            x- neighborhood offset
	 * @param y_offset
	 *            y- neighborhood offset
	 * @param z_offset
	 *            z- neighborhood offset
	 * @return corresponding neighborhood (0 if out of image)
	 */
	public static byte[] getNeighborhood(final IPluginIOStack channel, final SkeletonPoint p, final int x_offset,
			final int y_offset, final int z_offset) {
		final byte[] neighborhood = new byte[(2 * x_offset + 1) * (2 * y_offset + 1) * (2 * z_offset + 1)];

		for (int l = 0, k = p.z - z_offset; k <= p.z + z_offset; k++)
			for (int j = p.y - y_offset; j <= p.y + y_offset; j++)
				for (int i = p.x - x_offset; i <= p.x + x_offset; i++, l++)
					neighborhood[l] = getBytePixel(channel, i, j, k);
		return neighborhood;
	} // end getNeighborhood

	// -----------------------------------------------------------------------
	/**
	 * Get neighborhood of a pixel in a 3D image (0 border conditions).
	 * 
	 * @param channel
	 *            3D image (as a ChannelView)
	 * @param p
	 *            3D point coordinates
	 * @return corresponding 27-pixels neighborhood (0 if out of image)
	 */
	private static byte[] getNeighborhood(IPluginIOStack channel, SkeletonPoint p) {
		return getNeighborhood(channel, p.x, p.y, p.z);
	}

	// -----------------------------------------------------------------------
	/**
	 * Get neighborhood of a pixel in a 3D image (0 border conditions).
	 * 
	 * @param channel
	 *            3D image (as a ChannelView)
	 * @param x
	 *            x- coordinate
	 * @param y
	 *            y- coordinate
	 * @param z
	 *            z- coordinate (in image stacks the indexes start at 1)
	 * @return corresponding 27-pixels neighborhood (0 if out of image)
	 */
	private static byte[] getNeighborhood(IPluginIOStack channel, int x, int y, int z) {
		byte[] neighborhood = new byte[27];

		neighborhood[0] = getBytePixel(channel, x - 1, y - 1, z - 1);
		neighborhood[1] = getBytePixel(channel, x, y - 1, z - 1);
		neighborhood[2] = getBytePixel(channel, x + 1, y - 1, z - 1);

		neighborhood[3] = getBytePixel(channel, x - 1, y, z - 1);
		neighborhood[4] = getBytePixel(channel, x, y, z - 1);
		neighborhood[5] = getBytePixel(channel, x + 1, y, z - 1);

		neighborhood[6] = getBytePixel(channel, x - 1, y + 1, z - 1);
		neighborhood[7] = getBytePixel(channel, x, y + 1, z - 1);
		neighborhood[8] = getBytePixel(channel, x + 1, y + 1, z - 1);

		neighborhood[9] = getBytePixel(channel, x - 1, y - 1, z);
		neighborhood[10] = getBytePixel(channel, x, y - 1, z);
		neighborhood[11] = getBytePixel(channel, x + 1, y - 1, z);

		neighborhood[12] = getBytePixel(channel, x - 1, y, z);
		neighborhood[13] = getBytePixel(channel, x, y, z);
		neighborhood[14] = getBytePixel(channel, x + 1, y, z);

		neighborhood[15] = getBytePixel(channel, x - 1, y + 1, z);
		neighborhood[16] = getBytePixel(channel, x, y + 1, z);
		neighborhood[17] = getBytePixel(channel, x + 1, y + 1, z);

		neighborhood[18] = getBytePixel(channel, x - 1, y - 1, z + 1);
		neighborhood[19] = getBytePixel(channel, x, y - 1, z + 1);
		neighborhood[20] = getBytePixel(channel, x + 1, y - 1, z + 1);

		neighborhood[21] = getBytePixel(channel, x - 1, y, z + 1);
		neighborhood[22] = getBytePixel(channel, x, y, z + 1);
		neighborhood[23] = getBytePixel(channel, x + 1, y, z + 1);

		neighborhood[24] = getBytePixel(channel, x - 1, y + 1, z + 1);
		neighborhood[25] = getBytePixel(channel, x, y + 1, z + 1);
		neighborhood[26] = getBytePixel(channel, x + 1, y + 1, z + 1);

		return neighborhood;
	} // end getNeighborhood

	// -----------------------------------------------------------------------
	/**
	 * Get pixel in 3D image (0 border conditions)
	 * 
	 * @param channel
	 *            3D image (as a ChannelView)
	 * @param x
	 *            x- coordinate
	 * @param y
	 *            y- coordinate
	 * @param z
	 *            z- coordinate (in image stacks the indexes start at 1)
	 * @return corresponding pixel (0 if out of image)
	 */
	public static short getPixel(final IPluginIOStack channel, final int x, final int y, final int z) {
		final int width = channel.getWidth();
		final int height = channel.getHeight();
		final int depth = channel.getDepth();
		if (x >= 0 && x < width && y >= 0 && y < height && z >= 1 && z <= depth) {
			Object pixels = channel.getStackPixelArray()[z - 1];
			if (pixels instanceof byte[])
				return ((byte[]) pixels)[x + y * width];
			else if (pixels instanceof short[])
				return ((short[]) pixels)[x + y * width];
			else if (pixels instanceof float[]) {
				float result = ((float[]) pixels)[x + y * width];
				return (short) result;
			} else
				throw new RuntimeException("Unsupported processor type in getPixel in AnalyzeSkeleton");
		} else
			return 0;
	} // end getPixel

	public static byte getBytePixel(final IPluginIOStack channel, final int x, final int y, final int z) {
		final int width = channel.getWidth();
		final int height = channel.getHeight();
		final int depth = channel.getDepth();
		if (x >= 0 && x < width && y >= 0 && y < height && z >= 1 && z <= depth) {
			Object pixels = channel.getStackPixelArray()[z - 1];
			if (pixels instanceof byte[])
				return ((byte[]) pixels)[x + y * width];
			else if (pixels instanceof short[])
				return (byte) ((short[]) pixels)[x + y * width];
			else if (pixels instanceof float[])
				return (byte) ((float[]) pixels)[x + y * width];
			else
				throw new RuntimeException("Unsupported processor type in getPixel in AnalyzeSkeleton");
		} else
			return 0;
	} // end getPixel

	/* ----------------------------------------------------------------------- */
	/**
	 * Get pixel in 3D image (0 border conditions).
	 * 
	 * @param channel
	 *            3D image (as a ChannelView)
	 * @param x
	 *            x- coordinate
	 * @param y
	 *            y- coordinate
	 * @param z
	 *            z- coordinate (in image stacks the indexes start at 1)
	 * @return corresponding pixel (0 if out of image)
	 */
	/*
	 * private short getShortPixel(ImageStack image, int x, int y, int z)
	 * {
	 * if(x >= 0 && x < this.width && y >= 0 && y < this.height && z >= 0 && z < this.depth)
	 * return ((short[]) image.getPixels(z + 1))[x + y * this.width];
	 * else return 0;
	 * } /* end getShortPixel
	 */

	/* ----------------------------------------------------------------------- */
	/**
	 * Get pixel in 3D image (0 border conditions).
	 * 
	 * @param channel
	 *            3D image (as a ChannelView)
	 * @param point
	 *            point to be evaluated
	 * @return corresponding pixel (0 if out of image)
	 */
	/*
	 * private short getShortPixel(ImageStack image, SkeletonPoint point)
	 * {
	 * return getShortPixel(image, point.x, point.y, point.z);
	 * } // end getPixel
	 */

	/* ----------------------------------------------------------------------- */
	/**
	 * Get pixel in 3D image (0 border conditions).
	 * 
	 * @param inputImage2
	 *            3D image (as a ChannelView)
	 * @param skeletonPoint
	 *            point to be evaluated
	 * @return corresponding pixel (0 if out of image)
	 */
	private static int getPixel(IPluginIOStack inputImage2, SkeletonPoint skeletonPoint) // *** was byte
	{
		return getPixel(inputImage2, skeletonPoint.x, skeletonPoint.y, skeletonPoint.z);
	} // end getPixel

	/* ----------------------------------------------------------------------- */
	/**
	 * Set pixel in 3D image.
	 * 
	 * @param channel
	 *            3D image (as a ChannelView)
	 * @param p
	 *            point coordinates
	 * @param value
	 *            pixel value
	 */
	public static void setPixel(IPluginIOStack channel, SkeletonPoint p, short value) // *** was byte
	{
		if (p.x >= 0 && p.x < channel.getWidth() && p.y >= 0 && p.y < channel.getHeight() && p.z >= 1
				&& p.z <= channel.getDepth()) {
			Object pixels = channel.getStackPixelArray()[p.z - 1];
			if (pixels instanceof byte[]) {
				if (value > 127)
					throw new RuntimeException("Byte overflow");
				((byte[]) pixels)[p.x + p.y * channel.getWidth()] = (byte) value;
			} else if (pixels instanceof short[])
				((short[]) pixels)[p.x + p.y * channel.getWidth()] = value;
			else if (pixels instanceof float[])
				((float[]) pixels)[p.x + p.y * channel.getWidth()] = value;
			else
				throw new RuntimeException("Unsupported pixel type in setPixel in AnalyzeSkeleton");

		}
	} // end setPixel

	/* ----------------------------------------------------------------------- */
	/**
	 * Set pixel in 3D image.
	 * 
	 * @param entry
	 *            3D image (as a ChannelView)
	 * @param x
	 *            x- coordinate
	 * @param y
	 *            y- coordinate
	 * @param z
	 *            z- coordinate (in image stacks the indexes start at 1)
	 * @param value
	 *            pixel value
	 */
	public static void setPixel(IPluginIOStack entry, int x, int y, int z, byte value) {
		if (value < 0) {
			throw new RuntimeException("Setting negative pixel");
		}
		if (x >= 0 && x < entry.getWidth() && y >= 0 && y < entry.getHeight() && z >= 1 && z <= entry.getDepth()) {
			Object pixels = entry.getStackPixelArray()[z - 1];
			if (pixels instanceof byte[])
				((byte[]) pixels)[x + y * entry.getWidth()] = value;
			else if (pixels instanceof short[])
				((short[]) pixels)[x + y * entry.getWidth()] = value;
			else if (pixels instanceof float[])
				((float[]) pixels)[x + y * entry.getWidth()] = value;
			else
				throw new RuntimeException("Unsupported pixel type in setPixel in AnalyzeSkeleton");

		}

	} // end setPixel

	/* ----------------------------------------------------------------------- */
	/**
	 * Set pixel in 3D (short) image.
	 * 
	 * @param channel
	 *            3D image (as a ChannelView)
	 * @param x
	 *            x- coordinate
	 * @param y
	 *            y- coordinate
	 * @param z
	 *            z- coordinate (in image stacks the indexes start at 1)
	 * @param value
	 *            pixel value
	 */
	public static void setPixel(IPluginIOStack channel, int x, int y, int z, short value) {
		if (x >= 0 && x < channel.getWidth() && y >= 0 && y < channel.getHeight() && z >= 1 && z <= channel.getDepth()) {
			Object pixels = channel.getStackPixelArray()[z - 1];
			if (pixels instanceof byte[])
				((byte[]) pixels)[x + y * channel.getWidth()] = (byte) value;
			else if (pixels instanceof short[])
				((short[]) pixels)[x + y * channel.getWidth()] = value;
			else if (pixels instanceof float[])
				((float[]) pixels)[x + y * channel.getWidth()] = value;
			else
				throw new RuntimeException("Unsupported pixel type in setPixel in AnalyzeSkeleton");

		}
	} // end setPixel

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
				new PixelType[] { PixelType.FLOAT_TYPE }, InputOutputDescription.KEEP_IN_RAM,
				InputOutputDescription.SAME_XY_MATCH_SELECTED_CHANNELS, true, false));
		return result;
	}

	@Override
	public String[] getInputLabels() {
		return new String[] {};
	}

	@Override
	public String[] getOutputLabels() {
		return new String[] { "Cells" };
	}

}// end class AnalyzeSkeleton_
