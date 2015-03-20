
package pipeline.plugins.image_processing;

/* Copyright 2006, 2007 Mark Longair */
// Plugin by Mark Longair, downloaded from http://homepages.inf.ed.ac.uk/s9808248/imagej/find-connected-regions/
// Minor modifications by Olivier Cinquin
// Covered by the GPL

import ij.IJ;
import ij.gui.Roi;
import ij.measure.ResultsTable;

import java.awt.Polygon;
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
import pipeline.plugins.ThreeDPlugin;

public class FindConnectedRegion extends ThreeDPlugin {

	// public static final String PLUGIN_VERSION = "1.2";

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

	private static final byte IN_QUEUE = 1;
	private static final byte ADDED = 2;

	@Override
	public void runChannel(final IPluginIOStack input, final IPluginIOStack output, ProgressReporter p,
			PreviewType previewType, boolean inputHasChanged) throws InterruptedException {
		// final boolean diagonal = true;
		final boolean display = true;
		final boolean showResults = true;
		final boolean mustHaveSameValue = false;
		boolean startFromPointROI = true;
		boolean roiPresent = true;
		// boolean autoSubtract = false;
		double valuesOverDouble = 0.00000001;// gd.getNextNumber();
		double minimumPointsInRegionDouble = 1d;// gd.getNextNumber();
		int stopAfterNumberOfRegions = 1;// gd.getNextNumber();

		boolean byteImage = input.getPixelType() == PixelType.BYTE_TYPE;

		boolean startAtMaxValue = true;

		int point_roi_x = -1;
		int point_roi_y = -1;
		int point_roi_z = -1;

		if (startFromPointROI) {

			Roi roi = null;
			if (input.getParentHyperstack() != null)
				roi = input.getParentHyperstack().getImp().imp.getRoi();
			if (roi == null) {
				IJ.log("There's no point selected in the image.");
				roiPresent = false;
				startFromPointROI = false;
			} else {
				if (roi.getType() != Roi.POINT) {
					IJ.log("There's a selection in the image, but it's not a point selection.");
					roiPresent = false;
					startFromPointROI = false;
				} else {
					Polygon pol = roi.getPolygon();
					if (pol.npoints > 1) {
						IJ.log("You can only have one point selected.");
						roiPresent = false;
					}

					if (!roiPresent) { // we could find a non-0 point to use as a starting point
						// take a middle slice in the stack, and sweep with a vertical line from the left
						// to the right
						// but for now, just pick the region with the highest number of points in it

					}

					point_roi_x = pol.xpoints[0];
					point_roi_y = pol.ypoints[0];
					point_roi_z = input.getImagePlusDisplay().getCurrentSlice() - 1;
					if (input.getImagePlusDisplay().isHyperStack())
						point_roi_z = point_roi_z / input.getImagePlusDisplay().getNChannels();
					IJ.log("current slice: " + point_roi_z);
					IJ.log("Pixels value is" + input.getPixelValue(point_roi_x, point_roi_y, point_roi_z));
					// System.out.println("Fetched ROI with co-ordinates: "+pol.xpoints[0]+", "+pol.ypoints[0]);
				}
			}
		}

		if (!startFromPointROI)
			stopAfterNumberOfRegions = Integer.MAX_VALUE;

		int width = input.getWidth();
		int height = input.getHeight();
		int depth = input.getDepth();
		input.computePixelArray();
		output.computePixelArray();

		if (((long) width) * height * depth > Integer.MAX_VALUE) {
			throw new PluginRuntimeException("This stack is too large for this plugin (must have less than "
					+ Integer.MAX_VALUE + " points.", true);
		}

		ArrayList<Region> results = new ArrayList<>();

		// ImageStack stack = imagePlus.getStack();

		byte[][] sliceDataBytes = null;
		float[][] sliceDataFloats = null;

		if (byteImage) {
			sliceDataBytes = new byte[depth][];
			for (int z = 0; z < depth; ++z) {
				sliceDataBytes[z] = (byte[]) input.getPixelsCopy(z);
			}
		} else {
			sliceDataFloats = new float[depth][];
			for (int z = 0; z < depth; ++z) {
				sliceDataFloats[z] = (float[]) input.getPixelsCopy(z);
			}
		}

		ResultsTable rt = ResultsTable.getResultsTable();
		rt.reset();

		boolean firstTime = true;
		byte[] largestRegion = null;
		int maxRegionPoints = 0;

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

			int foundValueInt = -1;
			float foundValueFloat = Float.MIN_VALUE;
			int maxValueInt = -1;
			float maxValueFloat = Float.MIN_VALUE;

			if (firstTime && (startFromPointROI && roiPresent)) {

				initial_x = point_roi_x;
				initial_y = point_roi_y;
				initial_z = point_roi_z;

				if (byteImage)
					foundValueInt = sliceDataBytes[initial_z][initial_y * width + initial_x] & 0xFF;
				else
					foundValueFloat = sliceDataFloats[initial_z][initial_y * width + initial_x];

				IJ.log("found value " + foundValueFloat);

			} else if (byteImage && startAtMaxValue) {

				for (int z = 0; z < depth; ++z) {
					for (int y = 0; y < height; ++y) {
						for (int x = 0; x < width; ++x) {
							int value = sliceDataBytes[z][y * width + x] & 0xFF;
							if (value > maxValueInt) {
								initial_x = x;
								initial_y = y;
								initial_z = z;
								maxValueInt = value;
							}
						}
					}
				}

				foundValueInt = maxValueInt;

				/*
				 * If the maximum value is below the
				 * level we care about, we're done.
				 */

				if (foundValueInt < valuesOverDouble) {
					break;
				}

			} else if (byteImage && !startAtMaxValue) {

				// Just finding some point in the a region...
				for (int z = 0; z < depth && foundValueInt == -1; ++z) {
					for (int y = 0; y < height && foundValueInt == -1; ++y) {
						for (int x = 0; x < width; ++x) {
							float value;
							if (byteImage)
								value = sliceDataBytes[z][y * width + x] & 0xFF;
							else
								value = sliceDataFloats[z][y * width + x];
							if (value > valuesOverDouble) {
								initial_x = x;
								initial_y = y;
								initial_z = z;
								foundValueInt = (int) value;
								break;
							}
						}
					}
				}

				if (foundValueInt == -1) {
					break;
				}

			} else {

				// This must be a 32 bit image and we're starting at the maximum
				assert (!byteImage && startAtMaxValue);

				for (int z = 0; z < depth; ++z) {
					for (int y = 0; y < height; ++y) {
						for (int x = 0; x < width; ++x) {
							float value = sliceDataFloats[z][y * width + x];
							if (value > valuesOverDouble) {
								if (value > maxValueFloat) {
									initial_x = x;
									initial_y = y;
									initial_z = z;
									maxValueFloat = value;
								}
							}
						}
					}
				}

				foundValueFloat = maxValueFloat;

				if (foundValueFloat == Float.MIN_VALUE) {
					break;

					// If the maximum value is below the level we
					// care about, we're done.
				}
				if (foundValueFloat < valuesOverDouble) {
					break;
				}

			}

			firstTime = false;

			int vint = foundValueInt;

			int pointsInQueue = 0;
			int queueArrayLength = 1024;
			int[] queue = new int[queueArrayLength];

			byte[] pointState = new byte[depth * width * height];
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

				pointState[currentPointStateIndex] = ADDED;

				if (byteImage) {
					sliceDataBytes[pz][currentSliceIndex] = 0;
				} else {
					sliceDataFloats[pz][currentSliceIndex] = Float.MIN_VALUE;
				}
				++pointsInThisRegion;

				int x_unchecked_min = px - 1;
				int y_unchecked_min = py - 1;
				int z_unchecked_min = pz - 1;

				int x_unchecked_max = px + 1;
				int y_unchecked_max = py + 1;
				int z_unchecked_max = pz + 1;

				int x_min = (x_unchecked_min < 0) ? 0 : x_unchecked_min;
				int y_min = (y_unchecked_min < 0) ? 0 : y_unchecked_min;
				int z_min = (z_unchecked_min < 0) ? 0 : z_unchecked_min;

				int x_max = (x_unchecked_max >= width) ? width - 1 : x_unchecked_max;
				int y_max = (y_unchecked_max >= height) ? height - 1 : y_unchecked_max;
				int z_max = (z_unchecked_max >= depth) ? depth - 1 : z_unchecked_max;

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

							if (byteImage) {

								int neighbourValue = sliceDataBytes[z][newSliceIndex] & 0xFF;

								if (mustHaveSameValue) {
									if (neighbourValue != vint) {
										continue;
									}
								} else {
									if (neighbourValue <= valuesOverDouble) {
										continue;
									}
								}
							} else {

								float neighbourValue = sliceDataFloats[z][newSliceIndex];

								if (neighbourValue <= valuesOverDouble) {
									continue;
								}
							}

							if (0 == pointState[newPointStateIndex]) {
								pointState[newPointStateIndex] = IN_QUEUE;
								if (pointsInQueue == queueArrayLength) {
									int newArrayLength = (int) (queueArrayLength * 1.2);
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

			if (!startFromPointROI) { // if we're not starting from a ROI, assume we want to output the largest
										// connected region
				// check if this region has more points that the previous largest region
				// if it does, save its points so we can output them later
				if (pointsInThisRegion > maxRegionPoints) {
					largestRegion = pointState;
					maxRegionPoints = pointsInThisRegion;
				}
			} else if (display) {
				// ImageStack newStack = new ImageStack(width, height);
				copyRegionToOutputWithOriginalImageValues(input, output, pointState, byteImage);
			}

			if ((stopAfterNumberOfRegions > 0) && (results.size() >= stopAfterNumberOfRegions)) {
				break;
			}
		}

		if ((!startFromPointROI) && (largestRegion != null))
			copyRegionToOutputWithOriginalImageValues(input, output, largestRegion, byteImage);

		Collections.sort(results, Collections.reverseOrder());

		if (showResults) {
			results.stream().forEach(r -> {
				r.addRow(rt);
			});
			rt.show("Results");
		}
	}

	static void copyRegionToOutputWithOriginalImageValues(IPluginIOStack input, IPluginIOStack output,
			byte[] pointState, boolean byteImage) {
		int width = input.getWidth();
		int height = input.getHeight();
		int depth = input.getDepth();

		if (output.getPixelType() == PixelType.BYTE_TYPE) {
			for (int z = 0; z < depth; ++z) {
				byte[] outputBytes = (byte[]) output.getStackPixelArray()[z];

				for (int y = 0; y < height; ++y) {
					for (int x = 0; x < width; ++x) {

						byte status = pointState[width * (z * height + y) + x];

						if (status == IN_QUEUE) {
							IJ.log("BUG: point " + x + "," + y + "," + z + " is still marked as IN_QUEUE");
						}

						if (status == ADDED) {
							if (byteImage)
								outputBytes[y * width + x] = ((byte[]) input.getStackPixelArray()[z])[y * width + x];// was
																														// replacementValue
							else {
								outputBytes[y * width + x] =
										(byte) ((float[]) input.getStackPixelArray()[z])[y * width + x];
								// IJ.log(""+sliceDataFloats[z][y*width + x]);
							}
						} else
							outputBytes[y * width + x] = 0;
					}
				}
			}
		} else if (output.getPixelType() == PixelType.FLOAT_TYPE) {
			for (int z = 0; z < depth; ++z) {
				float[] outputFloats = (float[]) output.getStackPixelArray()[z];

				for (int y = 0; y < height; ++y) {
					for (int x = 0; x < width; ++x) {

						byte status = pointState[width * (z * height + y) + x];

						if (status == IN_QUEUE) {
							IJ.log("BUG: point " + x + "," + y + "," + z + " is still marked as IN_QUEUE");
						}

						if (status == ADDED) {
							if (byteImage)
								outputFloats[y * width + x] = ((byte[]) input.getStackPixelArray()[z])[y * width + x];// was
																														// replacementValue
							else
								outputFloats[y * width + x] = ((float[]) input.getStackPixelArray()[z])[y * width + x];
						} else
							outputFloats[y * width + x] = 0;
					}
				}
			}
		}

	}

	@Override
	public int getFlags() {
		return SAME_AS_BINARY + PARALLELIZE_WITH_NEW_INSTANCES + ONLY_FLOAT_INPUT;
	}

	@Override
	public String operationName() {
		return "Find Connected Region";
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
