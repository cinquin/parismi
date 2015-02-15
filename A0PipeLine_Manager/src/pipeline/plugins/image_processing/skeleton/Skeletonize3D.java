//Adapted from ImageJ/Fiji
package pipeline.plugins.image_processing.skeleton;

/**
 * Skeletonize3D plugin for ImageJ(C).
 * Copyright (C) 2008 Ignacio Arganda-Carreras
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import pipeline.PreviewType;
import pipeline.data.IPluginIOStack;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.plugins.ThreeDPlugin;

/**
 * Main class.
 * This class is a plugin for the ImageJ interface for 2D and 3D thinning
 * (skeletonization) of binary images (2D/3D).
 *
 * <p>
 * This work is an implementation by Ignacio Arganda-Carreras of the 3D thinning algorithm from Lee et al. "Building
 * skeleton models via 3-D medial surface/axis thinning algorithms. Computer Vision, Graphics, and Image Processing,
 * 56(6):462???478, 1994." Based on the ITK version from Hanno Homann <a href="http://hdl.handle.net/1926/1292">
 * http://hdl.handle.net/1926/1292</a>
 * <p>
 * More information at Skeletonize3D homepage:
 * http://imagejdocu.tudor.lu/doku.php?id=plugin:morphology:skeletonize3d:start
 *
 * @version 1.0 11/19/2008
 * @author Ignacio Arganda-Carreras <ignacio.arganda@uam.es>
 *
 */
public class Skeletonize3D extends ThreeDPlugin {
	@Override
	public int getFlags() {
		return SAME_AS_BINARY + DONT_PARALLELIZE + ONLY_BINARY_INPUT;
	}

	@Override
	public String operationName() {
		return "3D Skeletonize";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public Map<String, InputOutputDescription> getInputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		result.put("Default source", new InputOutputDescription(null, null, new PixelType[] { PixelType.BYTE_TYPE,
				PixelType.SHORT_TYPE, PixelType.FLOAT_TYPE }, InputOutputDescription.NOT_SPECIFIED,
				InputOutputDescription.NOT_SPECIFIED, false, false));
		return result;
	}

	@Override
	public Map<String, InputOutputDescription> getOutputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		result.put("Default destination", new InputOutputDescription(null, null,
				new PixelType[] { PixelType.BYTE_TYPE }, InputOutputDescription.KEEP_IN_RAM,
				InputOutputDescription.SAME_XY_MATCH_SELECTED_CHANNELS, true, false));
		return result;
	}

	/** working image width */
	private int width = 0;
	/** working image height */
	private int height = 0;
	/** working image depth */
	private int depth = 0;

	private IPluginIOStack output;
	private IPluginIOStack input;

	private AtomicInteger sliceRegistry = new AtomicInteger(0);
	private AtomicInteger thread_index = new AtomicInteger(0);

	private byte[][] pixel_array;

	private boolean cancelled;

	@Override
	public void runChannel(final IPluginIOStack input, final IPluginIOStack output, final ProgressReporter p,
			final PreviewType previewType, boolean inputHasChanged) throws InterruptedException {
		cancelled = false;

		if (threads == null) {
			threads = new Thread[nCpus];
		}// newThreadArray()

		width = input.getWidth();
		height = input.getHeight();
		depth = input.getDepth();
		this.output = output;
		this.input = input;

		// Prepare data; copy source into destination in the process
		prepareChannelViewData();

		pixel_array = new byte[output.getDepth() + 2][];
		pixel_array[0] = new byte[width * height];
		for (int i = 1; i <= output.getDepth(); i++) {
			pixel_array[i] = (byte[]) output.getPixels(i - 1);
		}
		pixel_array[output.getDepth() + 1] = new byte[width * height];

		computeThinImage();

		// Convert image to binary 0-255
		for (int i = 1; i <= output.getDepth(); i++) {
			output.multiply(i, 1, 255);
		}

	}

	private void prepareChannelViewData() {

		// Copy the input to the output, changing all foreground pixels to
		// have value 1 in the process.
		for (int z = 0; z < depth; z++) {
			// byte [] inputBytes=(byte []) input.getPixels(z+1);
			byte[] outputBytes = (byte[]) output.getPixels(z);
			for (int x = 0; x < width; x++)
				for (int y = 0; y < height; y++)
					if (input.getPixelValue(x, y, z) != 0)
						// if ( inputBytes[x + y * width] != 0 )
						outputBytes[x + y * width] = 1;
					else
						outputBytes[x + y * width] = 0;
		}

	}

	/* ----------------------------------------------------------------------- */
	/**
	 * Post processing for computing thinning.
	 */
	private byte currentBorder;
	private ArrayList<int[]>[] simpleBorderPoints;

	@SuppressWarnings("unchecked")
	int computeThinImage() throws InterruptedException {
		cancelled = false;

		// if (simpleBorderPoints==null){
		simpleBorderPoints = new ArrayList[nCpus];
		for (int i = 0; i < nCpus; i++) {
			simpleBorderPoints[i] = new ArrayList<>(100);
		}
		// }

		// Prepare Euler LUT [Lee94]
		final int eulerLUT[] = new int[256];
		fillEulerLUT(eulerLUT);

		int iter = 1;

		// Loop through the image several times until there is no change.
		int unchangedBorders = 0;
		while (unchangedBorders < 6) // loop until no change for all the six border types
		{
			unchangedBorders = 0;
			for (currentBorder = 1; currentBorder <= 6; currentBorder++) {
				for (int ithread = 0; ithread < threads.length; ithread++) {
					threads[ithread] = new Thread("Skeletonize3D worker thread") {
						@Override
						public void run() {
							int threadid = thread_index.getAndIncrement();
							for (int z = sliceRegistry.getAndIncrement(); z < depth; z =
									sliceRegistry.getAndIncrement()) {
								if (cancelled)
									return;
								for (int y = 1; y < height - 1; y++) { // *** IGNORE FIRST AND LAST COLUMN
									for (int x = 1; x < width - 1; x++) { // *** IGNORE FIRST AND LAST LINE
										// check if point is foreground
										if (getPixel(x, y, z) != 1) {
											continue; // current point is already background
										}
										// check 6-neighbors if point is a border point of type currentBorder
										boolean isBorderPoint = false;

										// North
										if (currentBorder == 1 && N(x, y, z) <= 0)
											isBorderPoint = true;
										// South
										else if (currentBorder == 2 && S(x, y, z) <= 0)
											isBorderPoint = true;
										// East
										else if (currentBorder == 3 && E(x, y, z) <= 0)
											isBorderPoint = true;
										// West
										else if (currentBorder == 4 && W(x, y, z) <= 0)
											isBorderPoint = true;
										// Up
										else if (currentBorder == 5 && U(x, y, z) <= 0)
											isBorderPoint = true;
										// Bottom
										else if (currentBorder == 6 && B(x, y, z) <= 0)
											isBorderPoint = true;

										if (!isBorderPoint) {
											continue; // current point is not deletable
										}

										// check if point is the end of an arc
										int numberOfNeighbors = -1; // -1 and not 0 because the center pixel will be
																	// counted as well
										byte[] neighbor = getNeighborhood(x, y, z);
										for (int i = 0; i < 27; i++) {
											if (neighbor[i] == 1)
												numberOfNeighbors++;
										}

										if (numberOfNeighbors == 1) {
											continue; // current point is not deletable
										}

										// Check if point is Euler invariant
										if (!isEulerInvariant(getNeighborhood(x, y, z), eulerLUT)) {
											continue; // current point is not deletable
										}
										// Check if point is simple (deletion does not change connectivity in the 3x3x3
										// neighborhood)
										if (!isSimplePoint(getNeighborhood(x, y, z))) {
											continue; // current point is not deletable
										}
										// add all simple border points to a list for sequential re-checking
										simpleBorderPoints[threadid].add(new int[] { x, y, z });
									}
								}
							}
						}
					};
				}

				sliceRegistry.set(0);
				thread_index.set(0); // IGNORE FIRST SLICE
				startAndJoin(threads);
				if (cancelled)
					throw new InterruptedException();
				// sequential re-checking to preserve connectivity when
				// deleting in a parallel way
				boolean noChange = true;
				for (int thread = 0; thread < nCpus; thread++) {
					for (int[] index : simpleBorderPoints[thread]) {
						// 1. Set simple border point to 0
						setPixel(index[0], index[1], index[2], (byte) 0);

						// 2. Check if neighborhood is still connected
						if (!isSimplePoint(getNeighborhood(index[0], index[1], index[2]))) {
							// we cannot delete current point, so reset
							setPixel(index[0], index[1], index[2], (byte) 1);
						} else {
							noChange = false;
						}
					}
					simpleBorderPoints[thread].clear();
				}
				if (noChange)
					unchangedBorders++;

				// IJ.write("# simple border points = " + simpleBorderPoints.size());
				/*
				 * if(index != null)
				 * IJ.write("# last point = [" + index[0] + ", " + index[1] + "," + index[2]+ "]");
				 * else
				 * IJ.write("# last point = [0, 0, 0]");
				 */

				// IJ.write("# simple border points = " + simpleBorderPoints.size() + "\n");

			} // end currentBorder for loop

			// Progress bar iterations
			iter++;
		}

		Utils.log("Did " + iter + " iterations", LogLevel.VERBOSE_DEBUG);

		// IJ.write("Compute Thin Image End");
		return 0;
	} /* end computeThinImage */

	/* ----------------------------------------------------------------------- */
	/**
	 * Get neighborhood of a pixel in a 3D image (0 border conditions)
	 * 
	 * @param x
	 *            x- coordinate
	 * @param y
	 *            y- coordinate
	 * @param z
	 *            z- coordinate (in image stacks the indexes start at 1)
	 * @return corresponding 27-pixels neighborhood (0 if out of image)
	 */
	private byte[] getNeighborhood(int x, int y, int z) {
		byte[] neighborhood = new byte[27];

		neighborhood[0] = getPixel(x - 1, y - 1, z - 1);
		neighborhood[1] = getPixel(x, y - 1, z - 1);
		neighborhood[2] = getPixel(x + 1, y - 1, z - 1);

		neighborhood[3] = getPixel(x - 1, y, z - 1);
		neighborhood[4] = getPixel(x, y, z - 1);
		neighborhood[5] = getPixel(x + 1, y, z - 1);

		neighborhood[6] = getPixel(x - 1, y + 1, z - 1);
		neighborhood[7] = getPixel(x, y + 1, z - 1);
		neighborhood[8] = getPixel(x + 1, y + 1, z - 1);

		neighborhood[9] = getPixel(x - 1, y - 1, z);
		neighborhood[10] = getPixel(x, y - 1, z);
		neighborhood[11] = getPixel(x + 1, y - 1, z);

		neighborhood[12] = getPixel(x - 1, y, z);
		neighborhood[13] = getPixel(x, y, z);
		neighborhood[14] = getPixel(x + 1, y, z);

		neighborhood[15] = getPixel(x - 1, y + 1, z);
		neighborhood[16] = getPixel(x, y + 1, z);
		neighborhood[17] = getPixel(x + 1, y + 1, z);

		neighborhood[18] = getPixel(x - 1, y - 1, z + 1);
		neighborhood[19] = getPixel(x, y - 1, z + 1);
		neighborhood[20] = getPixel(x + 1, y - 1, z + 1);

		neighborhood[21] = getPixel(x - 1, y, z + 1);
		neighborhood[22] = getPixel(x, y, z + 1);
		neighborhood[23] = getPixel(x + 1, y, z + 1);

		neighborhood[24] = getPixel(x - 1, y + 1, z + 1);
		neighborhood[25] = getPixel(x, y + 1, z + 1);
		neighborhood[26] = getPixel(x + 1, y + 1, z + 1);

		return neighborhood;
	} /* end getNeighborhood */

	/* ----------------------------------------------------------------------- */
	/**
	 * Get pixel in 3D image (0 border conditions)
	 * 
	 * 
	 * @param x
	 *            x- coordinate
	 * @param y
	 *            y- coordinate
	 * @param z
	 *            z- coordinate (in image stacks the indexes start at 1)
	 * @return corresponding pixel (0 if out of image)
	 */

	private byte getPixel(int x, int y, int z) {
		// if(x >= 0 && x < width && y >= 0 && y < height && z >= 0 && z < depth)
		// return ((byte[]) image.getPixels(z + 1))[x + y * width];
		return pixel_array[z + 1][x + y * width];
		// else return 0;
	} /* end getPixel */

	/* ----------------------------------------------------------------------- */
	/**
	 * Set pixel in 3D image
	 * 
	 * 
	 * @param x
	 *            x- coordinate
	 * @param y
	 *            y- coordinate
	 * @param z
	 *            z- coordinate (in image stacks the indexes start at 1)
	 * @param value
	 *            pixel value
	 */
	private void setPixel(int x, int y, int z, byte value) {
		// if(x >= 0 && x < width && y >= 0 && y < height && z >= 0 && z < depth)
		// ((byte[]) image.getPixels(z + 1))[x + y * width] = value;
		pixel_array[z + 1][x + y * width] = value;
	} /* end getPixel */

	/* ----------------------------------------------------------------------- */
	/**
	 * North neighborhood (0 border conditions)
	 * 
	 * 
	 * @param x
	 *            x- coordinate
	 * @param y
	 *            y- coordinate
	 * @param z
	 *            z- coordinate (in image stacks the indexes start at 1)
	 * @return corresponding north pixel
	 */
	private byte N(int x, int y, int z) {
		return getPixel(x, y - 1, z);
	} /* end N */

	/* ----------------------------------------------------------------------- */
	/**
	 * South neighborhood (0 border conditions)
	 * 
	 * 
	 * @param x
	 *            x- coordinate
	 * @param y
	 *            y- coordinate
	 * @param z
	 *            z- coordinate (in image stacks the indexes start at 1)
	 * @return corresponding south pixel
	 */
	private byte S(int x, int y, int z) {
		return getPixel(x, y + 1, z);
	} /* end S */

	/* ----------------------------------------------------------------------- */
	/**
	 * East neighborhood (0 border conditions)
	 * 
	 * 
	 * @param x
	 *            x- coordinate
	 * @param y
	 *            y- coordinate
	 * @param z
	 *            z- coordinate (in image stacks the indexes start at 1)
	 * @return corresponding east pixel
	 */
	private byte E(int x, int y, int z) {
		return getPixel(x + 1, y, z);
	} /* end E */

	/* ----------------------------------------------------------------------- */
	/**
	 * West neighborhood (0 border conditions)
	 * 
	 * 
	 * @param x
	 *            x- coordinate
	 * @param y
	 *            y- coordinate
	 * @param z
	 *            z- coordinate (in image stacks the indexes start at 1)
	 * @return corresponding west pixel
	 */
	private byte W(int x, int y, int z) {
		return getPixel(x - 1, y, z);
	} /* end W */

	/* ----------------------------------------------------------------------- */
	/**
	 * Up neighborhood (0 border conditions)
	 * 
	 * 
	 * @param x
	 *            x- coordinate
	 * @param y
	 *            y- coordinate
	 * @param z
	 *            z- coordinate (in image stacks the indexes start at 1)
	 * @return corresponding up pixel
	 */
	private byte U(int x, int y, int z) {
		return getPixel(x, y, z + 1);
	} /* end U */

	/* ----------------------------------------------------------------------- */
	/**
	 * Bottom neighborhood (0 border conditions)
	 * 
	 * 
	 * @param x
	 *            x- coordinate
	 * @param y
	 *            y- coordinate
	 * @param z
	 *            z- coordinate (in image stacks the indexes start at 1)
	 * @return corresponding bottom pixel
	 */
	private byte B(int x, int y, int z) {
		return getPixel(x, y, z - 1);
	} /* end N */

	/* ----------------------------------------------------------------------- */
	/**
	 * Fill Euler LUT
	 * 
	 * @param LUT
	 *            Euler LUT
	 */
	static private void fillEulerLUT(int[] LUT) {
		LUT[1] = 1;
		LUT[3] = -1;
		LUT[5] = -1;
		LUT[7] = 1;
		LUT[9] = -3;
		LUT[11] = -1;
		LUT[13] = -1;
		LUT[15] = 1;
		LUT[17] = -1;
		LUT[19] = 1;
		LUT[21] = 1;
		LUT[23] = -1;
		LUT[25] = 3;
		LUT[27] = 1;
		LUT[29] = 1;
		LUT[31] = -1;
		LUT[33] = -3;
		LUT[35] = -1;
		LUT[37] = 3;
		LUT[39] = 1;
		LUT[41] = 1;
		LUT[43] = -1;
		LUT[45] = 3;
		LUT[47] = 1;
		LUT[49] = -1;
		LUT[51] = 1;

		LUT[53] = 1;
		LUT[55] = -1;
		LUT[57] = 3;
		LUT[59] = 1;
		LUT[61] = 1;
		LUT[63] = -1;
		LUT[65] = -3;
		LUT[67] = 3;
		LUT[69] = -1;
		LUT[71] = 1;
		LUT[73] = 1;
		LUT[75] = 3;
		LUT[77] = -1;
		LUT[79] = 1;
		LUT[81] = -1;
		LUT[83] = 1;
		LUT[85] = 1;
		LUT[87] = -1;
		LUT[89] = 3;
		LUT[91] = 1;
		LUT[93] = 1;
		LUT[95] = -1;
		LUT[97] = 1;
		LUT[99] = 3;
		LUT[101] = 3;
		LUT[103] = 1;

		LUT[105] = 5;
		LUT[107] = 3;
		LUT[109] = 3;
		LUT[111] = 1;
		LUT[113] = -1;
		LUT[115] = 1;
		LUT[117] = 1;
		LUT[119] = -1;
		LUT[121] = 3;
		LUT[123] = 1;
		LUT[125] = 1;
		LUT[127] = -1;
		LUT[129] = -7;
		LUT[131] = -1;
		LUT[133] = -1;
		LUT[135] = 1;
		LUT[137] = -3;
		LUT[139] = -1;
		LUT[141] = -1;
		LUT[143] = 1;
		LUT[145] = -1;
		LUT[147] = 1;
		LUT[149] = 1;
		LUT[151] = -1;
		LUT[153] = 3;
		LUT[155] = 1;

		LUT[157] = 1;
		LUT[159] = -1;
		LUT[161] = -3;
		LUT[163] = -1;
		LUT[165] = 3;
		LUT[167] = 1;
		LUT[169] = 1;
		LUT[171] = -1;
		LUT[173] = 3;
		LUT[175] = 1;
		LUT[177] = -1;
		LUT[179] = 1;
		LUT[181] = 1;
		LUT[183] = -1;
		LUT[185] = 3;
		LUT[187] = 1;
		LUT[189] = 1;
		LUT[191] = -1;
		LUT[193] = -3;
		LUT[195] = 3;
		LUT[197] = -1;
		LUT[199] = 1;
		LUT[201] = 1;
		LUT[203] = 3;
		LUT[205] = -1;
		LUT[207] = 1;

		LUT[209] = -1;
		LUT[211] = 1;
		LUT[213] = 1;
		LUT[215] = -1;
		LUT[217] = 3;
		LUT[219] = 1;
		LUT[221] = 1;
		LUT[223] = -1;
		LUT[225] = 1;
		LUT[227] = 3;
		LUT[229] = 3;
		LUT[231] = 1;
		LUT[233] = 5;
		LUT[235] = 3;
		LUT[237] = 3;
		LUT[239] = 1;
		LUT[241] = -1;
		LUT[243] = 1;
		LUT[245] = 1;
		LUT[247] = -1;
		LUT[249] = 3;
		LUT[251] = 1;
		LUT[253] = 1;
		LUT[255] = -1;
	}

	/**
	 * Check if a point is Euler invariant
	 * 
	 * @param neighbors
	 *            neighbor pixels of the point
	 * @param LUT
	 *            Euler LUT
	 * @return true or false if the point is Euler invariant or not
	 */
	private static boolean isEulerInvariant(byte[] neighbors, int[] LUT) {
		// Calculate Euler characteristic for each octant and sum up
		int eulerChar = 0;
		char n;
		// Octant SWU
		n = 1;
		if (neighbors[24] == 1)
			n |= 128;
		if (neighbors[25] == 1)
			n |= 64;
		if (neighbors[15] == 1)
			n |= 32;
		if (neighbors[16] == 1)
			n |= 16;
		if (neighbors[21] == 1)
			n |= 8;
		if (neighbors[22] == 1)
			n |= 4;
		if (neighbors[12] == 1)
			n |= 2;
		eulerChar += LUT[n];
		// Octant SEU
		n = 1;
		if (neighbors[26] == 1)
			n |= 128;
		if (neighbors[23] == 1)
			n |= 64;
		if (neighbors[17] == 1)
			n |= 32;
		if (neighbors[14] == 1)
			n |= 16;
		if (neighbors[25] == 1)
			n |= 8;
		if (neighbors[22] == 1)
			n |= 4;
		if (neighbors[16] == 1)
			n |= 2;
		eulerChar += LUT[n];
		// Octant NWU
		n = 1;
		if (neighbors[18] == 1)
			n |= 128;
		if (neighbors[21] == 1)
			n |= 64;
		if (neighbors[9] == 1)
			n |= 32;
		if (neighbors[12] == 1)
			n |= 16;
		if (neighbors[19] == 1)
			n |= 8;
		if (neighbors[22] == 1)
			n |= 4;
		if (neighbors[10] == 1)
			n |= 2;
		eulerChar += LUT[n];
		// Octant NEU
		n = 1;
		if (neighbors[20] == 1)
			n |= 128;
		if (neighbors[23] == 1)
			n |= 64;
		if (neighbors[19] == 1)
			n |= 32;
		if (neighbors[22] == 1)
			n |= 16;
		if (neighbors[11] == 1)
			n |= 8;
		if (neighbors[14] == 1)
			n |= 4;
		if (neighbors[10] == 1)
			n |= 2;
		eulerChar += LUT[n];
		// Octant SWB
		n = 1;
		if (neighbors[6] == 1)
			n |= 128;
		if (neighbors[15] == 1)
			n |= 64;
		if (neighbors[7] == 1)
			n |= 32;
		if (neighbors[16] == 1)
			n |= 16;
		if (neighbors[3] == 1)
			n |= 8;
		if (neighbors[12] == 1)
			n |= 4;
		if (neighbors[4] == 1)
			n |= 2;
		eulerChar += LUT[n];
		// Octant SEB
		n = 1;
		if (neighbors[8] == 1)
			n |= 128;
		if (neighbors[7] == 1)
			n |= 64;
		if (neighbors[17] == 1)
			n |= 32;
		if (neighbors[16] == 1)
			n |= 16;
		if (neighbors[5] == 1)
			n |= 8;
		if (neighbors[4] == 1)
			n |= 4;
		if (neighbors[14] == 1)
			n |= 2;
		eulerChar += LUT[n];
		// Octant NWB
		n = 1;
		if (neighbors[0] == 1)
			n |= 128;
		if (neighbors[9] == 1)
			n |= 64;
		if (neighbors[3] == 1)
			n |= 32;
		if (neighbors[12] == 1)
			n |= 16;
		if (neighbors[1] == 1)
			n |= 8;
		if (neighbors[10] == 1)
			n |= 4;
		if (neighbors[4] == 1)
			n |= 2;
		eulerChar += LUT[n];
		// Octant NEB
		n = 1;
		if (neighbors[2] == 1)
			n |= 128;
		if (neighbors[1] == 1)
			n |= 64;
		if (neighbors[11] == 1)
			n |= 32;
		if (neighbors[10] == 1)
			n |= 16;
		if (neighbors[5] == 1)
			n |= 8;
		if (neighbors[4] == 1)
			n |= 4;
		if (neighbors[14] == 1)
			n |= 2;
		eulerChar += LUT[n];
		return eulerChar == 0;
	}

	/* ----------------------------------------------------------------------- */
	/**
	 * Check if current point is a Simple SkeletonPoint.
	 * This method is named 'N(v)_labeling' in [Lee94].
	 * Outputs the number of connected objects in a neighborhood of a point
	 * after this point would have been removed.
	 * 
	 * @param neighbors
	 *            neighbor pixels of the point
	 * @return true or false if the point is simple or not
	 */
	static private boolean isSimplePoint(byte[] neighbors) {
		// copy neighbors for labeling
		int cube[] = new int[26];
		int i = 0;
		for (i = 0; i < 13; i++)
			// i = 0..12 -> cube[0..12]
			cube[i] = neighbors[i];
		// i != 13 : ignore center pixel when counting (see [Lee94])
		for (i = 14; i < 27; i++)
			// i = 14..26 -> cube[13..25]
			cube[i - 1] = neighbors[i];
		// set initial label
		int label = 2;
		// for all points in the neighborhood
		for (i = 0; i < 26; i++) {
			if (cube[i] == 1) // voxel has not been labelled yet
			{
				// start recursion with any octant that contains the point i
				switch (i) {
					case 0:
					case 1:
					case 3:
					case 4:
					case 9:
					case 10:
					case 12:
						octreeLabeling(1, label, cube);
						break;
					case 2:
					case 5:
					case 11:
					case 13:
						octreeLabeling(2, label, cube);
						break;
					case 6:
					case 7:
					case 14:
					case 15:
						octreeLabeling(3, label, cube);
						break;
					case 8:
					case 16:
						octreeLabeling(4, label, cube);
						break;
					case 17:
					case 18:
					case 20:
					case 21:
						octreeLabeling(5, label, cube);
						break;
					case 19:
					case 22:
						octreeLabeling(6, label, cube);
						break;
					case 23:
					case 24:
						octreeLabeling(7, label, cube);
						break;
					case 25:
						octreeLabeling(8, label, cube);
						break;
					default:
						throw new IllegalStateException("Unexpected value of i" + i);
				}
				label++;
				if (label - 2 >= 2) {
					return false;
				}
			}
		}
		// return label-2; in [Lee94] if the number of connected components would be needed
		return true;
	}

	/* ----------------------------------------------------------------------- */
	/**
	 * This is a recursive method that calculates the number of connected
	 * components in the 3D neighborhood after the center pixel would
	 * have been removed.
	 * 
	 * @param octant
	 * @param label
	 * @param cube
	 */
	static private void octreeLabeling(int octant, int label, int[] cube) {
		// check if there are points in the octant with value 1
		if (octant == 1) {
			// set points in this octant to current label
			// and recursive labeling of adjacent octants
			if (cube[0] == 1)
				cube[0] = label;
			if (cube[1] == 1) {
				cube[1] = label;
				octreeLabeling(2, label, cube);
			}
			if (cube[3] == 1) {
				cube[3] = label;
				octreeLabeling(3, label, cube);
			}
			if (cube[4] == 1) {
				cube[4] = label;
				octreeLabeling(2, label, cube);
				octreeLabeling(3, label, cube);
				octreeLabeling(4, label, cube);
			}
			if (cube[9] == 1) {
				cube[9] = label;
				octreeLabeling(5, label, cube);
			}
			if (cube[10] == 1) {
				cube[10] = label;
				octreeLabeling(2, label, cube);
				octreeLabeling(5, label, cube);
				octreeLabeling(6, label, cube);
			}
			if (cube[12] == 1) {
				cube[12] = label;
				octreeLabeling(3, label, cube);
				octreeLabeling(5, label, cube);
				octreeLabeling(7, label, cube);
			}
		}
		if (octant == 2) {
			if (cube[1] == 1) {
				cube[1] = label;
				octreeLabeling(1, label, cube);
			}
			if (cube[4] == 1) {
				cube[4] = label;
				octreeLabeling(1, label, cube);
				octreeLabeling(3, label, cube);
				octreeLabeling(4, label, cube);
			}
			if (cube[10] == 1) {
				cube[10] = label;
				octreeLabeling(1, label, cube);
				octreeLabeling(5, label, cube);
				octreeLabeling(6, label, cube);
			}
			if (cube[2] == 1)
				cube[2] = label;
			if (cube[5] == 1) {
				cube[5] = label;
				octreeLabeling(4, label, cube);
			}
			if (cube[11] == 1) {
				cube[11] = label;
				octreeLabeling(6, label, cube);
			}
			if (cube[13] == 1) {
				cube[13] = label;
				octreeLabeling(4, label, cube);
				octreeLabeling(6, label, cube);
				octreeLabeling(8, label, cube);
			}
		}
		if (octant == 3) {
			if (cube[3] == 1) {
				cube[3] = label;
				octreeLabeling(1, label, cube);
			}
			if (cube[4] == 1) {
				cube[4] = label;
				octreeLabeling(1, label, cube);
				octreeLabeling(2, label, cube);
				octreeLabeling(4, label, cube);
			}
			if (cube[12] == 1) {
				cube[12] = label;
				octreeLabeling(1, label, cube);
				octreeLabeling(5, label, cube);
				octreeLabeling(7, label, cube);
			}
			if (cube[6] == 1)
				cube[6] = label;
			if (cube[7] == 1) {
				cube[7] = label;
				octreeLabeling(4, label, cube);
			}
			if (cube[14] == 1) {
				cube[14] = label;
				octreeLabeling(7, label, cube);
			}
			if (cube[15] == 1) {
				cube[15] = label;
				octreeLabeling(4, label, cube);
				octreeLabeling(7, label, cube);
				octreeLabeling(8, label, cube);
			}
		}
		if (octant == 4) {
			if (cube[4] == 1) {
				cube[4] = label;
				octreeLabeling(1, label, cube);
				octreeLabeling(2, label, cube);
				octreeLabeling(3, label, cube);
			}
			if (cube[5] == 1) {
				cube[5] = label;
				octreeLabeling(2, label, cube);
			}
			if (cube[13] == 1) {
				cube[13] = label;
				octreeLabeling(2, label, cube);
				octreeLabeling(6, label, cube);
				octreeLabeling(8, label, cube);
			}
			if (cube[7] == 1) {
				cube[7] = label;
				octreeLabeling(3, label, cube);
			}
			if (cube[15] == 1) {
				cube[15] = label;
				octreeLabeling(3, label, cube);
				octreeLabeling(7, label, cube);
				octreeLabeling(8, label, cube);
			}
			if (cube[8] == 1)
				cube[8] = label;
			if (cube[16] == 1) {
				cube[16] = label;
				octreeLabeling(8, label, cube);
			}
		}
		if (octant == 5) {
			if (cube[9] == 1) {
				cube[9] = label;
				octreeLabeling(1, label, cube);
			}
			if (cube[10] == 1) {
				cube[10] = label;
				octreeLabeling(1, label, cube);
				octreeLabeling(2, label, cube);
				octreeLabeling(6, label, cube);
			}
			if (cube[12] == 1) {
				cube[12] = label;
				octreeLabeling(1, label, cube);
				octreeLabeling(3, label, cube);
				octreeLabeling(7, label, cube);
			}
			if (cube[17] == 1)
				cube[17] = label;
			if (cube[18] == 1) {
				cube[18] = label;
				octreeLabeling(6, label, cube);
			}
			if (cube[20] == 1) {
				cube[20] = label;
				octreeLabeling(7, label, cube);
			}
			if (cube[21] == 1) {
				cube[21] = label;
				octreeLabeling(6, label, cube);
				octreeLabeling(7, label, cube);
				octreeLabeling(8, label, cube);
			}
		}
		if (octant == 6) {
			if (cube[10] == 1) {
				cube[10] = label;
				octreeLabeling(1, label, cube);
				octreeLabeling(2, label, cube);
				octreeLabeling(5, label, cube);
			}
			if (cube[11] == 1) {
				cube[11] = label;
				octreeLabeling(2, label, cube);
			}
			if (cube[13] == 1) {
				cube[13] = label;
				octreeLabeling(2, label, cube);
				octreeLabeling(4, label, cube);
				octreeLabeling(8, label, cube);
			}
			if (cube[18] == 1) {
				cube[18] = label;
				octreeLabeling(5, label, cube);
			}
			if (cube[21] == 1) {
				cube[21] = label;
				octreeLabeling(5, label, cube);
				octreeLabeling(7, label, cube);
				octreeLabeling(8, label, cube);
			}
			if (cube[19] == 1)
				cube[19] = label;
			if (cube[22] == 1) {
				cube[22] = label;
				octreeLabeling(8, label, cube);
			}
		}
		if (octant == 7) {
			if (cube[12] == 1) {
				cube[12] = label;
				octreeLabeling(1, label, cube);
				octreeLabeling(3, label, cube);
				octreeLabeling(5, label, cube);
			}
			if (cube[14] == 1) {
				cube[14] = label;
				octreeLabeling(3, label, cube);
			}
			if (cube[15] == 1) {
				cube[15] = label;
				octreeLabeling(3, label, cube);
				octreeLabeling(4, label, cube);
				octreeLabeling(8, label, cube);
			}
			if (cube[20] == 1) {
				cube[20] = label;
				octreeLabeling(5, label, cube);
			}
			if (cube[21] == 1) {
				cube[21] = label;
				octreeLabeling(5, label, cube);
				octreeLabeling(6, label, cube);
				octreeLabeling(8, label, cube);
			}
			if (cube[23] == 1)
				cube[23] = label;
			if (cube[24] == 1) {
				cube[24] = label;
				octreeLabeling(8, label, cube);
			}
		}
		if (octant == 8) {
			if (cube[13] == 1) {
				cube[13] = label;
				octreeLabeling(2, label, cube);
				octreeLabeling(4, label, cube);
				octreeLabeling(6, label, cube);
			}
			if (cube[15] == 1) {
				cube[15] = label;
				octreeLabeling(3, label, cube);
				octreeLabeling(4, label, cube);
				octreeLabeling(7, label, cube);
			}
			if (cube[16] == 1) {
				cube[16] = label;
				octreeLabeling(4, label, cube);
			}
			if (cube[21] == 1) {
				cube[21] = label;
				octreeLabeling(5, label, cube);
				octreeLabeling(6, label, cube);
				octreeLabeling(7, label, cube);
			}
			if (cube[22] == 1) {
				cube[22] = label;
				octreeLabeling(6, label, cube);
			}
			if (cube[24] == 1) {
				cube[24] = label;
				octreeLabeling(7, label, cube);
			}
			if (cube[25] == 1)
				cube[25] = label;
		}

	}

} /* end Skeletonize3D_ */
