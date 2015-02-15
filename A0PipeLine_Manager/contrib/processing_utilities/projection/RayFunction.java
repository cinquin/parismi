package processing_utilities.projection;

import pipeline.data.PluginIOImage.PixelType;
import pipeline.misc_util.PluginRuntimeException;

public abstract class RayFunction {
	/**
	 * Abstract class that specifies structure of ray
	 * function. Preprocessing should be done in derived class
	 * constructors.
	 */
	/** Do actual slice projection for specific data types. */
	protected abstract void projectSlice(byte[] pixels);

	protected abstract void projectSlice(short[] pixels);

	protected abstract void projectSlice(float[] pixels);

	protected abstract void projectSlice(double[] pixels);

	/**
	 * Perform any necessary post processing operations, e.g.
	 * averaging values.
	 */
	public void postProcess() {
	}

	public void preProcess() {
	}

	public static final int AVG_METHOD = 0;
	public static final int MAX_METHOD = 1;
	public static final int MIN_METHOD = 2;
	public static final int SUM_METHOD = 3;
	public static final int SD_METHOD = 4;
	public static final int MEDIAN_METHOD = 5;
	public static final int MEDIAN_METHOD_NON_0 = 6;
	public static final int AVG_METHOD_NON_0 = 7;
	public static final String[] METHODS = { "", "Average Intensity", "Max Intensity", "Min Intensity", "Sum Slices",
			"Standard Deviation", "Median", "Median non-0", "Average non-0" };

	public static final int BYTE_TYPE = 0;
	public static final int SHORT_TYPE = 1;
	public static final int FLOAT_TYPE = 2;
	public static final int DOUBLE_TYPE = 3;

	protected float minPixelValueToConsider, maxPixelValueToConsider;

	public static RayFunction getRayFunction(int method, float[] fp, int sliceCount, float minPixelValueToConsider,
			float maxPixelValueToConsider) {
		if (method < 0)
			method = MAX_METHOD;
		switch (method) {
			case AVG_METHOD:
			case SUM_METHOD:
				return new AverageIntensity(method, fp, sliceCount, minPixelValueToConsider, maxPixelValueToConsider);
			case MAX_METHOD:
				return new MaxIntensity(fp, minPixelValueToConsider, maxPixelValueToConsider);
			case MIN_METHOD:
				return new MinIntensity(fp, minPixelValueToConsider, maxPixelValueToConsider);
			case SD_METHOD:
				return new StandardDeviation(fp, sliceCount, minPixelValueToConsider, maxPixelValueToConsider);
			case AVG_METHOD_NON_0:
				return new AverageIntensity_NO_0(fp, sliceCount, minPixelValueToConsider, maxPixelValueToConsider);
			default:
				throw new PluginRuntimeException("ZProjection - unknown method.", true);
		}
	}

	/**
	 * Handles mechanics of projection by selecting appropriate pixel
	 * array type. We do this rather than using more general
	 * ImageProcessor getPixelValue() and putPixel() methods because
	 * direct manipulation of pixel arrays is much more efficient.
	 */
	public static void projectSlice(Object pixelArray, RayFunction rayFunc, PixelType ptype) {
		switch (ptype) {
			case BYTE_TYPE:
				rayFunc.projectSlice((byte[]) pixelArray);
				break;
			case SHORT_TYPE:
				rayFunc.projectSlice((short[]) pixelArray);
				break;
			case FLOAT_TYPE:
				rayFunc.projectSlice((float[]) pixelArray);
				break;
			case DOUBLE_TYPE:
				rayFunc.projectSlice((double[]) pixelArray);
				break;
			default:
				throw new RuntimeException("Unknown pixel type " + ptype);
		}
	}

	public abstract float[] projectProjection(int stride);

} // end RayFunction

