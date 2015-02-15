package processing_utilities.projection;

/** Compute max intensity projection. */
public class MaxIntensity extends RayFunction {
	private float[] fpixels;
	private int len;

	@Override
	public final float[] projectProjection(int stride) {
		int nSteps = fpixels.length / stride;
		float[] finalResult = new float[stride];
		if (nSteps * stride != fpixels.length)
			throw new RuntimeException("in standard division AverageIntensity: " + nSteps + "*" + stride + "!="
					+ fpixels.length);
		for (int i = 0; i < stride; i++) {
			finalResult[i] = Float.MIN_VALUE;
			for (int j = 0; j < nSteps; j++) {
				if (fpixels[i + stride * j] > finalResult[i])
					finalResult[i] = fpixels[i + stride * j];
			}
		}
		return finalResult;
	}

	/** Simple constructor since no preprocessing is necessary. */
	public MaxIntensity(float[] fp, float minPixelValueToConsider, float maxPixelValueToConsider) {
		fpixels = fp;
		len = fpixels.length;
		for (int i = 0; i < len; i++)
			fpixels[i] = -Float.MAX_VALUE;
		this.minPixelValueToConsider = minPixelValueToConsider;
		this.maxPixelValueToConsider = maxPixelValueToConsider;

	}

	@Override
	public final void projectSlice(byte[] pixels) {
		for (int i = 0; i < len; i++) {
			if ((pixels[i] & 0xff) > fpixels[i])
				if (((pixels[i] & 0xff) >= minPixelValueToConsider) && ((pixels[i] & 0xff) <= maxPixelValueToConsider))
					fpixels[i] = (pixels[i] & 0xff);
		}
	}

	@Override
	public final void projectSlice(short[] pixels) {
		for (int i = 0; i < len; i++) {
			if ((pixels[i] & 0xffff) > fpixels[i])
				if (((pixels[i] & 0xffff) >= minPixelValueToConsider)
						&& ((pixels[i] & 0xffff) <= maxPixelValueToConsider))
					fpixels[i] = pixels[i] & 0xffff;
		}
	}

	@Override
	public final void projectSlice(float[] pixels) {
		for (int i = 0; i < len; i++) {
			if (pixels[i] > fpixels[i])
				if ((pixels[i] >= minPixelValueToConsider) && (pixels[i] <= maxPixelValueToConsider))
					fpixels[i] = pixels[i];
		}
	}

	@Override
	public final void projectSlice(double[] pixels) {
		for (int i = 0; i < len; i++) {
			if (pixels[i] > fpixels[i])
				if ((pixels[i] >= minPixelValueToConsider) && (pixels[i] <= maxPixelValueToConsider))
					fpixels[i] = (float) pixels[i];
		}
	}

	@Override
	public final void preProcess() {
	}

} // end MaxIntensity