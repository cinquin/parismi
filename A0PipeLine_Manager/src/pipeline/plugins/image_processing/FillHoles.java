
package pipeline.plugins.image_processing;

/* Copyright 2006, 2007 Mark Longair */
// Plugin by Mark Longair, downloaded from http://homepages.inf.ed.ac.uk/s9808248/imagej/find-connected-regions/
// Minor modifications by Olivier Cinquin
// Covered by the GPL

import ij.measure.ResultsTable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import pipeline.PreviewType;
import pipeline.data.IPluginIOStack;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.misc_util.PluginRuntimeException;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.plugins.ThreeDPlugin;

public class FillHoles extends ThreeDPlugin {

	/* An inner class to make the results list sortable. */
	private static final class Region implements Comparable<Object> {

		Region(int value, String materialName, int points, boolean sameValue) {
			byteImage = true;
			this.value = value;
			this.materialName = materialName;
			this.points = points;
			this.sameValue = sameValue;
		}

		Region(int points, boolean sameValue) {
			byteImage = false;
			this.points = points;
			this.sameValue = sameValue;
		}

		boolean byteImage;
		int points;
		String materialName;
		int value;
		boolean sameValue;

		@Override
		public int compareTo(Object otherRegion) {
			Region o = (Region) otherRegion;
			return (points < o.points) ? -1 : ((points > o.points) ? 1 : 0);
		}

		@Override
		public String toString() {
			if (byteImage) {
				String materialBit = "";
				if (materialName != null) {
					materialBit = " (" + materialName + ")";
				}
				return "Region of value " + value + materialBit + " containing " + points + " points";
			} else {
				return "Region containing " + points + " points";
			}
		}

		public void addRow(ResultsTable rt) {
			rt.incrementCounter();
			if (byteImage) {
				if (sameValue)
					rt.addValue("Value in Region", value);
				rt.addValue("Points In Region", points);
				if (materialName != null)
					rt.addLabel("Material Name", materialName);
			} else {
				rt.addValue("Points in Region", points);
			}
		}

	}

	private static final int IN_QUEUE = -1;

	// private static final int ADDED = 2; REPLACED BY ID OF REGION

	@Override
	public void runChannel(final IPluginIOStack input, final IPluginIOStack output, ProgressReporter p,
			PreviewType previewType, boolean inputHasChanged) throws InterruptedException {
		input.computePixelArray();
		output.computePixelArray();
		runChannel(input, output, p, previewType, inputHasChanged, 0, input.getWidth() - 1, 0, input.getHeight() - 1);
	}

	private boolean makeEdgesBlack = true;

	/**
	 * If doing only 2D hole filling, slice index to work on
	 */
	public int fixSlice = -1;

	@SuppressWarnings("unused")
	public int runChannel(final IPluginIOStack input, final IPluginIOStack output, ProgressReporter p,
			PreviewType previewType, boolean inputHasChanged, int startX, int stopX, int startY, int stopY)
			throws InterruptedException {
		// Find all regions of value 0
		// For all the regions except the one that has most points, change them to 255
		// This should fill all the holes
		// final boolean diagonal = true;
		final boolean display = true;
		final boolean showResults = false;
		final boolean mustHaveSameValue = false;
		final boolean startFromPointROI = false;
		boolean autoSubtract = false;
		double minimumPointsInRegionDouble = 1d;// gd.getNextNumber();
		int stopAfterNumberOfRegions = 1;// gd.getNextNumber();

		final boolean twoDFilling = fixSlice > -1;

		progressSetIndeterminateThreadSafe(p, true);

		/*
		 * int type = input.getType();
		 * 
		 * if (!(ImagePlus.GRAY8 == type || ImagePlus.COLOR_256 == type || ImagePlus.GRAY32 == type)) {
		 * Utils.displayError("The image must be either 8 bit or 32 bit for this plugin.");
		 * return 1;
		 * }
		 * 
		 * 
		 * if (ImagePlus.GRAY8 == type || ImagePlus.COLOR_256 == type) {
		 * byteImage = true;
		 * }
		 */

		boolean byteImage = input.getPixelType() == PixelType.BYTE_TYPE;

		final boolean startAtMaxValue = false;

		if (true)
			stopAfterNumberOfRegions = Integer.MAX_VALUE;

		int width = input.getWidth();
		int height = input.getHeight();

		int depth = input.getDepth();

		if ((((long) width) * height * depth) > Integer.MAX_VALUE) {
			throw new PluginRuntimeException("This stack is too large for this plugin (must have less than "
					+ Integer.MAX_VALUE + " points.", true);
		}

		ArrayList<Region> results = new ArrayList<>();


		// Preserve the calibration and colour lookup tables
		// for generating new images of each individual
		// region.
		// Calibration calibration = input.imp.getCalibration();

		/*
		 * ColorModel cm = null;
		 * if (ImagePlus.COLOR_256 == type) {
		 * cm = input.imp.getStack().getColorModel();
		 * }
		 */

		ResultsTable rt = ResultsTable.getResultsTable();
		rt.reset();

		int maxRegionPoints = 0;

		int[] pointState = new int[depth * width * height]; // TODO This uses more memory than necessary as we could
		// do with just depth * subRegionWidth * subRegionHeight; but we would need to compute indeces to access points
		// differently than the way the current code does it.
		int currentRegion = 1;
		int idOfRegionWithMostPoints = -1;

		final int minZ = fixSlice > -1 ? fixSlice : 0;
		final int maxZ = fixSlice > -1 ? fixSlice : (depth - 1);

		while (true) {

			if (Thread.interrupted())
				throw new InterruptedException();

			/*
			 * Find one pixel that's above the minimum, or
			 * find the maximum in the case where we're
			 * not insisting that all regions are made up
			 * of the same color. These are set in all
			 * cases...
			 */

			int initial_x = -1;
			int initial_y = -1;
			int initial_z = -1;

			int foundValue = -1;

			if (byteImage) {

				// Just finding some point in the a region...
				for (int z = minZ; z <= maxZ && foundValue == -1; ++z) {
					for (int y = startY; y <= stopY && foundValue == -1; ++y) {
						for (int x = startX; x <= stopX; ++x) {
							float value;
							if (byteImage)
								value = ((byte[]) input.getStackPixelArray()[z])[y * width + x] & 0xFF;
							else
								value = ((float[]) input.getStackPixelArray()[z])[y * width + x];
							if ((value == 0) && (pointState[width * (z * height + y) + x] == 0)) {
								initial_x = x;
								initial_y = y;
								initial_z = z;
								foundValue = (int) value;
								break;
							}
						}
					}
				}

				if (foundValue == -1) {
					break;
				}

			} else {

				// This must be a 32 bit image
				assert (false);

				for (int z = minZ; z <= maxZ && foundValue == -1; ++z) {
					for (int y = startY; y <= stopY && foundValue == -1; ++y) {
						for (int x = startX; x <= stopX; ++x) {
							float value = ((float[]) input.getStackPixelArray()[z])[y * width + x];
							if ((value == 0) && (pointState[width * (z * height + y) + x] == 0)) {
								initial_x = x;
								initial_y = y;
								initial_z = z;
								foundValue = 1;
								break;
							}
						}
					}
				}

				if (foundValue == -1) {
					break;
				}

			}

			int vint = foundValue;

			int pointsInQueue = 0;
			int queueArrayLength = 1024;
			int[] queue = new int[queueArrayLength];

			int i = width * (initial_z * height + initial_y) + initial_x;
			pointState[i] = IN_QUEUE;
			queue[pointsInQueue++] = i;

			int pointsInThisRegion = 0;

			while (pointsInQueue > 0) {

				if (Thread.interrupted())
					throw new InterruptedException();

				int nextIndex = queue[--pointsInQueue];

				int currentPointStateIndex = nextIndex;
				int pz = nextIndex / (width * height);
				int currentSliceIndex = nextIndex % (width * height);
				int py = currentSliceIndex / width;
				int px = currentSliceIndex % width;

				pointState[currentPointStateIndex] = currentRegion;

				// if (byteImage) {
				// sliceDataBytes[pz][currentSliceIndex] = -1;
				// } else {
				// sliceDataFloats[pz][currentSliceIndex] = -1;
				// }
				++pointsInThisRegion;

				int x_unchecked_min = px - 1;
				int y_unchecked_min = py - 1;
				int z_unchecked_min = pz - 1;

				int x_unchecked_max = px + 1;
				int y_unchecked_max = py + 1;
				int z_unchecked_max = pz + 1;

				int x_min = (x_unchecked_min < startX) ? startX : x_unchecked_min;
				int y_min = (y_unchecked_min < startY) ? startY : y_unchecked_min;
				int z_min = (z_unchecked_min < 0) ? 0 : z_unchecked_min;

				int x_max = (x_unchecked_max > stopX) ? stopX : x_unchecked_max;
				int y_max = (y_unchecked_max > stopY) ? stopY : y_unchecked_max;
				int z_max = (z_unchecked_max >= depth) ? depth - 1 : z_unchecked_max;

				if (twoDFilling)
					z_min = z_max = pz;

				for (int z = z_min; z <= z_max; ++z) {
					for (int y = y_min; y <= y_max; ++y) {
						for (int x = x_min; x <= x_max; ++x) {

							// If we're not including diagonals,
							// skip those points.
							// if ((!diagonal) && (x == x_unchecked_min || x == x_unchecked_max) && (y ==
							// y_unchecked_min || y == y_unchecked_max) && (z == z_unchecked_min || z ==
							// z_unchecked_max)) {
							// continue;
							// }
							int newSliceIndex = y * width + x;
							int newPointStateIndex = width * (z * height + y) + x;

							if ((!makeEdgesBlack) || ((x > startX) && (x < stopX) && (y > startY) && (y < stopY))) {

								if (byteImage) {

									int neighbourValue = ((byte[]) input.getStackPixelArray()[z])[newSliceIndex] & 0xFF;

									if (neighbourValue != 0) {
										continue;
									}
								} else {

									float neighbourValue = ((float[]) input.getStackPixelArray()[z])[newSliceIndex];

									if (neighbourValue != 0) {
										continue;
									}
								}
							}

							if (0 == pointState[newPointStateIndex]) {
								pointState[newPointStateIndex] = IN_QUEUE;
								if (pointsInQueue == queueArrayLength) {
									int newArrayLength = queueArrayLength * 2;
									int[] newArray = new int[newArrayLength];
									System.arraycopy(queue, 0, newArray, 0, pointsInQueue);
									queue = newArray;
									queueArrayLength = newArrayLength;
								}
								queue[pointsInQueue++] = newPointStateIndex;
							}
						}
					}
				}
			}

			if (Thread.interrupted())
				throw new InterruptedException();

			// So now pointState should have no IN_QUEUE
			// status points...
			Region region;
			if (byteImage) {
				region = new Region(vint, "", pointsInThisRegion, mustHaveSameValue);
			} else {
				region = new Region(pointsInThisRegion, mustHaveSameValue);
			}
			if (pointsInThisRegion < minimumPointsInRegionDouble) {
				// System.out.println("Too few points - only " + pointsInThisRegion);
				continue;
			}

			results.add(region);

			/*
			 * byte replacementValue;
			 * if (byteImage) {
			 * replacementValue = (byte) ( (cm == null) ? 255 : vint );
			 * } else {
			 * replacementValue = (byte) 255;
			 * }
			 */

			if (true) { // if we're not starting from a ROI, assume we want to output the largest connected region
				// check if this region has more points that the previous largest region
				// if it does, save its points so we can output them later
				if (pointsInThisRegion > maxRegionPoints) {
					idOfRegionWithMostPoints = currentRegion;
					maxRegionPoints = pointsInThisRegion;
				}
			} else if (true) {
				// ImageStack newStack = new ImageStack(width, height);
				// THIS SHOULD BE DEADCODE
				copyRegionToOutputWithOriginalImageValues(input, output, pointState, idOfRegionWithMostPoints, startX,
						stopX, startY, stopY, byteImage);
			}

			if ((stopAfterNumberOfRegions > 0) && (results.size() >= stopAfterNumberOfRegions)) {
				break;
			}

			currentRegion++;
			if (pipelineCallback != null)
				pipelineCallback.redrawLine(ourRow); // to update the progress indicator

		}

		if ((true))
			copyRegionToOutputWithOriginalImageValues(input, output, pointState, idOfRegionWithMostPoints, startX,
					stopX, startY, stopY, byteImage);

		Collections.sort(results, Collections.reverseOrder());

		// System.out.println(r.toString());
		if (showResults) {
			results.stream().forEach(r -> {
				r.addRow(rt);
			});
			rt.show("Results");
		}

		progressSetIndeterminateThreadSafe(p, false);
		return 0;
	}

	void copyRegionToOutputWithOriginalImageValues(IPluginIOStack input, IPluginIOStack output, int[] pointState,
			int idOfRegionToLeaveAlone, int startX, int stopX, int startY, int stopY, boolean byteImage) {
		int width = input.getWidth();
		int height = input.getHeight();
		int depth = input.getDepth();

		final int minZ = fixSlice > -1 ? fixSlice : 0;
		final int maxZ = fixSlice > -1 ? fixSlice : (depth - 1);

		if (output.getPixelType() == PixelType.BYTE_TYPE) {
			for (int z = minZ; z <= maxZ; ++z) {
				byte[] outputBytes = (byte[]) output.getStackPixelArray()[z];

				for (int y = startY; y <= stopY; ++y) {
					for (int x = startX; x <= stopX; ++x) {

						int status = pointState[width * (z * height + y) + x];

						if (status == IN_QUEUE) {
							Utils.log("BUG: point " + x + "," + y + "," + z + " is still marked as IN_QUEUE",
									LogLevel.ERROR);
						}

						if ((status > 0) && (status != idOfRegionToLeaveAlone)) {
							outputBytes[y * width + x] = (byte) 255;
						} else {
							if (byteImage)
								outputBytes[y * width + x] = ((byte[]) input.getStackPixelArray()[z])[y * width + x];
							else {
								outputBytes[y * width + x] =
										(byte) ((float[]) input.getStackPixelArray()[z])[y * width + x];
							}
						}
					}
				}
			}
		} else if (output.getPixelType() == PixelType.FLOAT_TYPE) {
			for (int z = minZ; z <= maxZ; ++z) {
				float[] outputFloats = (float[]) output.getStackPixelArray()[z];

				for (int y = startY; y <= stopY; ++y) {
					for (int x = startX; x <= stopX; ++x) {

						int status = pointState[width * (z * height + y) + x];

						if (status == IN_QUEUE) {
							Utils.log("BUG: point " + x + "," + y + "," + z + " is still marked as IN_QUEUE",
									LogLevel.ERROR);
						}

						if ((status > 0) && (status != idOfRegionToLeaveAlone)) {
							outputFloats[y * width + x] = 255;
						} else {
							if (byteImage)
								outputFloats[y * width + x] =
										(((byte[]) input.getStackPixelArray()[z])[y * width + x]) & 0xFF;
							else {
								outputFloats[y * width + x] = ((float[]) input.getStackPixelArray()[z])[y * width + x];
							}
						}
					}
				}
			}
		}

	}

	@Override
	public int getFlags() {
		return SAME_AS_BINARY;
	}

	@Override
	public String operationName() {
		return "Fill 3D Holes";
	}

	@Override
	public String version() {
		return "1.0";
	}

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
		return result;
	}

}
