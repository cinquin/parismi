/**
 * Code adapted from ImageJ.
 */

/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.image_processing;

import java.awt.Rectangle;

import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import pipeline.PreviewType;
import pipeline.misc_util.ParameterListenerWeakRef;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.ComboBoxParameter;
import pipeline.parameters.IntParameter;
import pipeline.parameters.ParameterListener;
import pipeline.parameters.ParameterListenerAdapter;
import pipeline.plugins.TwoDPlugin;

public class RankFilters extends TwoDPlugin {

	private static String[] choices = { "", "MEAN", "MIN", "MAX", "VARIANCE", "MEDIAN", "OUTLIERS", "DESPECKLE",
			"MASKS" };
	private static final int MEAN = 1;
	private static final int MIN = 2;
	private static final int MAX = 3;
	private static final int VARIANCE = 4;
	private static final int MEDIAN = 5;
	private static final int OUTLIERS = 6;
	protected static final int DESPECKLE = 7;
	private static final int BRIGHT_OUTLIERS = 0, DARK_OUTLIERS = 1;

	private class MethodListener extends ParameterListenerAdapter {

		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			if (!(((ComboBoxParameter) boxParameter).getSelectionIndex() == filterType)) {
				filterType = ((ComboBoxParameter) boxParameter).getSelectionIndex();
				Utils.log("Filter type set to " + filterType, LogLevel.DEBUG);
				if (pipelineCallback != null) {
					pipelineCallback.parameterValueChanged(ourRow, null, false);
				}
			}
		}
	}

	private ParameterListener methodListener0 = new MethodListener();
	private ParameterListener methodListener1 = new ParameterListenerWeakRef(methodListener0);

	private class RadiusListener extends ParameterListenerAdapter {

		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			if (((int[]) radius_param.getValue())[0] != radius) {
				radius = ((int[]) radius_param.getValue())[0];
				if (pipelineCallback != null) {
					pipelineCallback.parameterValueChanged(ourRow, null, false);
				}

			}
		}
	}

	private ParameterListener radiusListener0 = new RadiusListener();
	private ParameterListener radiusListener1 = new ParameterListenerWeakRef(radiusListener0);

	private AbstractParameter boxParameter = new ComboBoxParameter("Rank filter", "", choices, "MIN", false,
			methodListener1);
	private AbstractParameter radius_param = new IntParameter("Radius", "", 5, 1, 20, true, true, radiusListener1);

	@Override
	public ParameterListener[] getParameterListeners() {
		return new ParameterListener[] { methodListener1, radiusListener1 };
	}

	@Override
	public AbstractParameter[] getParameters() {
		AbstractParameter[] paramArray = { boxParameter, radius_param };
		return paramArray;
	}

	@Override
	public void setParameters(AbstractParameter[] param) {
		if (param[0] != null) {
			boxParameter = param[0];
			boxParameter.addPluginListener(methodListener1);
		}
		else
			Utils.log("Null param0 in RankFilter", LogLevel.WARNING);
		if (param[1] != null) {
			radius_param = param[1];
			radius_param.addPluginListener(radiusListener1);
		}
		else
			Utils.log("Null param0 in RankFilter", LogLevel.WARNING);
		radius = ((int[]) radius_param.getValue())[0];
		filterType = ((ComboBoxParameter) boxParameter).getSelectionIndex();
		makeKernel(radius);
	}

	@Override
	public String operationName() {
		return "Rank filter";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public int getFlags() {
		return SAME_AS_FLOAT + PARALLELIZE_WITH_NEW_INSTANCES + ONLY_FLOAT_INPUT;
	}

	@Override
	public void runSlice(ImageProcessor ip, ImageProcessor dest, PreviewType previewType) throws InterruptedException {

		int[] lineRadius;
		int kRadius;

		lineRadius = (this.lineRadius.clone()); // cloning also required by doFiltering method
		kRadius = this.kRadius;
		// kNPoints = this.kNPoints;
		pass++;
		doFiltering((FloatProcessor) ip, kRadius, lineRadius, filterType, whichOutliers, (float) threshold,
				(FloatProcessor) dest);
		// if (imp!=null && imp.getBitDepth()!=24 && imp.getRoi()==null && filterType==VARIANCE) {
		// new ContrastEnhancer().stretchHistogram(this.imp.getProcessor(), 0.5);
		// }

	}

	// private static final String[] outlierStrings = {"Bright","Dark"};
	// Filter parameters
	private double radius = -1.0;
	private double threshold = 50.;
	private int whichOutliers = BRIGHT_OUTLIERS;
	private int filterType = MIN;
	// F u r t h e r c l a s s v a r i a b l e s
	// private int nPasses = 1; // The number of passes (color channels * stack slices)
	@SuppressWarnings("unused")
	private int pass; // Current pass
	private int kRadius; // kernel radius. Size is (2*kRadius+1)^2
	private int kNPoints; // number of points in the kernel
	private int[] lineRadius; // the length of each kernel line is 2*lineRadius+1

	/**
	 * Filter a FloatProcessor according to filterType
	 *
	 * @param ip
	 *            The image subject to filtering
	 * @param kRadius
	 *            The kernel radius. The kernel has a side length of 2*kRadius+1
	 * @param lineRadius
	 *            The radius of the lines in the kernel. Line length of line i is 2*lineRadius[i]+1.
	 *            Note that the array <code>lineRadius</code> will be modified, thus call this method
	 *            with a clone of the original lineRadius array if the array should be used again.
	 * @param filterType
	 *            as defined above; DESPECKLE is not a valid type here.
	 * @param threshold
	 *            Threshold for 'outliers' filter
	 * @throws InterruptedException
	 */
	//
	// Data handling: The area needed for processing a line, i.e. a stripe of width (2*kRadius+1)
	// is written into the array 'cache'. This array is padded at the edges of the image so that
	// a surrounding with radius kRadius for each pixel processed is within 'cache'. Out-of-image
	// pixels are set to the value of the neares edge pixel. When adding a new line, the lines in
	// 'cache' are not shifted but rather the smaller array with the line lengths of the kernel is
	// shifted.
	//
	// Algorithm: For mean and variance, except for small radius, usually do not calculate the
	// sum over all pixels. This sum is calculated for the first pixel of every line. For the
	// following pixels, add the new values and subtract those that are not in the sum any more.
	// For min/max, also first look at the new values, use their maximum if larger than the old
	// one. The look at the values not in the area any more; if it does not contain the old
	// maximum, leave the maximum unchanged. Otherwise, determine the maximum inside the area.
	// For outliers, calculate the median only if the pixel deviates by more than the threshold
	// from any pixel in the area. Therefore min or max is calculated; this is a much faster
	// operation than the median.
	private void doFiltering(FloatProcessor ip, int kRadius, int[] lineRadius, int filterType, int whichOutliers,
			float threshold, FloatProcessor dest) throws InterruptedException {

		boolean minOrMax = filterType == MIN || filterType == MAX;
		boolean minOrMaxOrOutliers = minOrMax || filterType == OUTLIERS;
		boolean sumFilter = filterType == MEAN || filterType == VARIANCE;
		boolean medianFilter = filterType == MEDIAN || filterType == OUTLIERS;
		double[] sums = sumFilter ? new double[2] : null;
		float[] medianBuf1 = medianFilter ? new float[kNPoints] : null;
		float[] medianBuf2 = medianFilter ? new float[kNPoints] : null;
		float sign = filterType == MIN ? -1f : 1f;
		if (filterType == OUTLIERS) // sign is -1 for high outliers: compare number with minimum
			sign = (ip.isInvertedLut() == (whichOutliers == DARK_OUTLIERS)) ? -1f : 1f;
		float[] pixels = (float[]) ip.getPixels(); // array of the pixel values of the input image
		float[] destPixels = (float[]) dest.getPixels();
		int width = ip.getWidth();
		int height = ip.getHeight();
		Rectangle roi = ip.getRoi();
		int xmin = roi.x - kRadius;
		int xEnd = roi.x + roi.width;
		int xmax = xEnd + kRadius;
		int kSize = 2 * kRadius + 1;
		int cacheWidth = xmax - xmin;
		int xminInside = xmin > 0 ? xmin : 0;
		int xmaxInside = xmax < width ? xmax : width;
		int widthInside = xmaxInside - xminInside;
		boolean smallKernel = kRadius < 2;

		float[] cache = new float[cacheWidth * kSize]; // a stripe of the image with height=2*kRadius+1
		for (int y = roi.y - kRadius, iCache = 0; y < roi.y + kRadius; y++)
			for (int x = xmin; x < xmax; x++, iCache++)
				// fill the cache for filtering the first line
				cache[iCache] =
						pixels[(x < 0 ? 0 : x >= width ? width - 1 : x) + width
								* (y < 0 ? 0 : y >= height ? height - 1 : y)];
		int nextLineInCache = 2 * kRadius; // where the next line should be written to
		float median = cache[0]; // just any value as a first guess
		for (int y = roi.y; y < roi.y + roi.height; y++) {
			if (Thread.interrupted())
				throw new InterruptedException();
			int ynext = y + kRadius; // C O P Y N E W L I N E into cache
			if (ynext >= height)
				ynext = height - 1;
			float leftpxl = pixels[width * ynext]; // edge pixels of the line replace out-of-image pixels
			float rightpxl = pixels[width - 1 + width * ynext];
			int iCache = cacheWidth * nextLineInCache;// where in the cach we have to copy to
			for (int x = xmin; x < 0; x++, iCache++)
				cache[iCache] = leftpxl;
			System.arraycopy(pixels, xminInside + width * ynext, cache, iCache, widthInside);
			iCache += widthInside;
			for (int x = width; x < xmax; x++, iCache++)
				cache[iCache] = rightpxl;
			nextLineInCache = (nextLineInCache + 1) % kSize;
			float max = 0f; // F I L T E R the line
			boolean fullCalculation = true;
			for (int x = roi.x, p = x + y * width, xCache0 = kRadius; x < xEnd; x++, p++, xCache0++) {
				if (fullCalculation) {
					fullCalculation = smallKernel; // for small kernel, always use the full area, not incremental
													// algorithm
					if (minOrMaxOrOutliers)
						max = getAreaMax(cache, cacheWidth, xCache0, lineRadius, kSize, 0, -Float.MAX_VALUE, sign);
					if (minOrMax)
						destPixels[p] = max * sign;
					else if (sumFilter)
						getAreaSums(cache, cacheWidth, xCache0, lineRadius, kSize, sums);
				} else {
					if (minOrMaxOrOutliers) {
						float newPointsMax = getSideMax(cache, cacheWidth, xCache0, lineRadius, kSize, true, sign);
						if (newPointsMax >= max) { // compare with previous maximum 'max'
							max = newPointsMax;
						} else {
							float removedPointsMax =
									getSideMax(cache, cacheWidth, xCache0, lineRadius, kSize, false, sign);
							if (removedPointsMax >= max)
								max = getAreaMax(cache, cacheWidth, xCache0, lineRadius, kSize, 1, newPointsMax, sign);
						}
						if (minOrMax)
							destPixels[p] = max * sign;
					} else if (sumFilter)
						addSideSums(cache, cacheWidth, xCache0, lineRadius, kSize, sums);
				}
				if (medianFilter) {
					if (filterType == MEDIAN || pixels[p] * sign + threshold < max) {
						median =
								getMedian(cache, cacheWidth, xCache0, lineRadius, kSize, medianBuf1, medianBuf2, median);
						if (filterType == MEDIAN || pixels[p] * sign + threshold < median * sign)
							destPixels[p] = median;
					}
				} else if (sumFilter) {
					if (filterType == MEAN)
						destPixels[p] = (float) (sums[0] / kNPoints);
					else
						// Variance: sum of squares - square of sums
						destPixels[p] = (float) ((sums[1] - sums[0] * sums[0] / kNPoints) / kNPoints);
				}
			} // for x
			int newLineRadius0 = lineRadius[kSize - 1]; // shift kernel lineRadii one line
			System.arraycopy(lineRadius, 0, lineRadius, 1, kSize - 1);
			lineRadius[0] = newLineRadius0;
		} // for y
	}

	/**
	 * Filters an image
	 *
	 * @param ip
	 *            The ImageProcessor that should be filtered (all 4 types supported)
	 * @param radius
	 *            Determines the kernel size, see Process>Filters>Show Circular Masks.
	 *            Must not be negative. No checking is done for large values that would
	 *            lead to excessive computing times.
	 * @param rankType
	 *            May be MEAN, MIN, MAX, VARIANCE, or MEDIAN.
	 */
	/*
	 * public void rank(ImageProcessor ip, double radius, int rankType) {
	 * FloatProcessor fp = null;
	 * for (int i=0; i<ip.getNChannels(); i++) {
	 * makeKernel(radius);
	 * fp = ip.toFloat(i, fp);
	 * doFiltering(fp, kRadius, lineRadius, rankType, BRIGHT_OUTLIERS, 50f);
	 * ip.setPixels(i, fp);
	 * }
	 * }
	 */

	/**
	 * Get max (or -min if sign=-1) within the kernel area.
	 *
	 * @param xCache0
	 *            points to cache element equivalent to x
	 * @param ignoreRight
	 *            should be 0 for analyzing all data or 1 for leaving out the row at the right
	 * @param max
	 *            should be -Float.MAX_VALUE or the smallest value the maximum can be
	 */
	private static float getAreaMax(float[] cache, int cacheWidth, int xCache0, int[] lineRadius, int kSize,
			int ignoreRight, float max, float sign) {
		for (int y = 0; y < kSize; y++) { // y within the cache stripe
			for (int x = xCache0 - lineRadius[y], iCache = y * cacheWidth + x; x <= xCache0 + lineRadius[y]
					- ignoreRight; x++, iCache++) {
				float v = cache[iCache] * sign;
				if (max < v)
					max = v;
			}
		}
		return max;
	}

	/**
	 * Get max (or -min if sign=-1) at the right border inside or left border outside the kernel area.
	 * cache0 points to cache element equivalent to x
	 */
	private static float getSideMax(float[] cache, int cacheWidth, int xCache0, int[] lineRadius, int kSize,
			boolean isRight, float sign) {
		float max = -Float.MAX_VALUE;
		for (int y = 0; y < kSize; y++) { // y within the cache stripe
			int x = isRight ? xCache0 + lineRadius[y] : xCache0 - lineRadius[y] - 1;
			int iCache = y * cacheWidth + x;
			float v = cache[iCache] * sign;
			if (max < v)
				max = v;
		}
		return max;
	}

	/**
	 * Get sum of values and values squared within the kernel area.
	 * xCache0 points to cache element equivalent to current x coordinate.
	 * Output is written to array sums[0] = sum; sums[1] = sum of squares
	 */
	private static void getAreaSums(float[] cache, int cacheWidth, int xCache0, int[] lineRadius, int kSize,
			double[] sums) {
		double sum = 0, sum2 = 0;
		for (int y = 0; y < kSize; y++) { // y within the cache stripe
			for (int x = xCache0 - lineRadius[y], iCache = y * cacheWidth + x; x <= xCache0 + lineRadius[y]; x++, iCache++) {
				float v = cache[iCache];
				sum += v;
				sum2 += v * v;
			}
		}
		sums[0] = sum;
		sums[1] = sum2;
		return;
	}

	/**
	 * Add all values and values squared at the right border inside minus at the left border outside the kernal area.
	 * Output is added or subtracted to/from array sums[0] += sum; sums[1] += sum of squares when at
	 * the right border, minus when at the left border
	 */
	private static void addSideSums(float[] cache, int cacheWidth, int xCache0, int[] lineRadius, int kSize,
			double[] sums) {
		double sum = 0, sum2 = 0;
		for (int y = 0; y < kSize; y++) { // y within the cache stripe
			int iCache0 = y * cacheWidth + xCache0;
			float v = cache[iCache0 + lineRadius[y]];
			sum += v;
			sum2 += v * v;
			v = cache[iCache0 - lineRadius[y] - 1];
			sum -= v;
			sum2 -= v * v;
		}
		sums[0] += sum;
		sums[1] += sum2;
		return;
	}

	/** Get median of values and values squared within area. Kernel size kNPoints should be odd. */
	private float getMedian(float[] cache, int cacheWidth, int xCache0, int[] lineRadius, int kSize, float[] aboveBuf,
			float[] belowBuf, float guess) {
		int half = kNPoints / 2;
		int nAbove = 0, nBelow = 0;
		for (int y = 0; y < kSize; y++) { // y within the cache stripe
			for (int x = xCache0 - lineRadius[y], iCache = y * cacheWidth + x; x <= xCache0 + lineRadius[y]; x++, iCache++) {
				float v = cache[iCache];
				if (v > guess) {
					aboveBuf[nAbove] = v;
					nAbove++;
				} else if (v < guess) {
					belowBuf[nBelow] = v;
					nBelow++;
				}
			}
		}
		if (nAbove > half)
			return findNthLowestNumber(aboveBuf, nAbove, nAbove - half - 1);
		else if (nBelow > half)
			return findNthLowestNumber(belowBuf, nBelow, half);
		else
			return guess;
	}

	/**
	 * Find the n-th lowest number in part of an array
	 *
	 * @param buf
	 *            The input array. Only values 0 ... bufLength are read. <code>buf</code> will be modified.
	 * @param bufLength
	 *            Number of values in <code>buf</code> that should be read
	 * @param n
	 *            which value should be found; n=0 for the lowest, n=bufLength-1 for the highest
	 * @return the value
	 */
	private static float findNthLowestNumber(float[] buf, int bufLength, int n) {
		// Modified algorithm according to http://www.geocities.com/zabrodskyvlada/3alg.html
		// Contributed by Heinz Klar
		int i, j;
		int l = 0;
		int m = bufLength - 1;
		float med = buf[n];
		float dum;

		while (l < m) {
			i = l;
			j = m;
			do {
				while (buf[i] < med)
					i++;
				while (med < buf[j])
					j--;
				dum = buf[j];
				buf[j] = buf[i];
				buf[i] = dum;
				i++;
				j--;
			} while ((j >= n) && (i <= n));
			if (j < n)
				l = i;
			if (n < i)
				m = j;
			med = buf[n];
		}
		return med;
	}

	/**
	 * Create a circular kernel of a given radius. Radius = 0.5 includes the 4 neighbors of the
	 * pixel in the center, radius = 1 corresponds to a 3x3 kernel size.
	 * The output is written to class variables kNPoints (number of points inside the kernel) and
	 * lineRadius, which is an array giving the radius of each line. Line length is 2*lineRadius+1.
	 */
	private void makeKernel(double radius) {
		if (radius >= 1.5 && radius < 1.75) // this code creates the same sizes as the previous RankFilters
			radius = 1.75;
		else if (radius >= 2.5 && radius < 2.85)
			radius = 2.85;
		int r2 = (int) (radius * radius) + 1;
		kRadius = (int) (Math.sqrt(r2 + 1e-10));
		lineRadius = new int[2 * kRadius + 1];
		lineRadius[kRadius] = kRadius;
		kNPoints = 2 * kRadius + 1;
		for (int y = 1; y <= kRadius; y++) {
			int dx = (int) (Math.sqrt(r2 - y * y + 1e-10));
			lineRadius[kRadius + y] = dx;
			lineRadius[kRadius - y] = dx;
			kNPoints += 4 * dx + 2;
		}
	}

}
