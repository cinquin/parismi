package processing_utilities.projection;

/** Compute average intensity projection. */
public class AverageIntensity_NO_0 extends RayFunction {
	private float[] fpixels;
	private int len;
	private int[] num_non_0;
	private float minPixelValueToConsider1;
	private float maxPixelValueToConsider1;

	@Override
	public final float[] projectProjection(int stride) {
		float[] finalResult = new float[stride];
		double[] final_num_non_0 = new double[stride];
		int nSteps = fpixels.length / stride;
		if (nSteps * stride != fpixels.length)
			throw new RuntimeException("in standard division AverageIntensity_NO_0: " + nSteps + "*" + stride + "!="
					+ fpixels.length);
		for (int i = 0; i < stride; i++) {
			for (int j = 0; j < nSteps; j++) {
				final_num_non_0[i] += num_non_0[i + stride * j];
				finalResult[i] += fpixels[i + stride * j];
			}
			if (final_num_non_0[i] > 0.0)
				finalResult[i] = (float) (finalResult[i] / final_num_non_0[i]);
			else
				finalResult[i] = 0f;
		}
		return finalResult;
	}

	/**
	 * Constructor requires number of slices to be
	 * projected. This is used to determine average at each
	 * pixel.
	 */
	public AverageIntensity_NO_0(float[] fp, int num, float minPixelValueToConsider, float maxPixelValueToConsider) { // num
																														// is
																														// the
																														// number
																														// of
																														// slices
		fpixels = fp;
		len = fpixels.length;
		num_non_0 = new int[len];
		this.minPixelValueToConsider1 = minPixelValueToConsider;
		this.maxPixelValueToConsider1 = maxPixelValueToConsider;
	}

	@Override
	public final void projectSlice(byte[] pixels) {
		for (int i = 0; i < len; i++) {
			if (((pixels[i] & 0xff) > 0.0) && ((pixels[i] & 0xff) >= minPixelValueToConsider1)
					&& ((pixels[i] & 0xff) <= maxPixelValueToConsider1)) {
				fpixels[i] += (pixels[i] & 0xff);
				num_non_0[i]++;
			}
		}
	}

	@Override
	public final void projectSlice(short[] pixels) {
		for (int i = 0; i < len; i++)
			if (((pixels[i] & 0xffff) > 0.0) && ((pixels[i] & 0xffff) >= minPixelValueToConsider1)
					&& ((pixels[i] & 0xffff) <= maxPixelValueToConsider1)) {
				fpixels[i] += pixels[i] & 0xffff;
				num_non_0[i]++;
			}
	}

	@Override
	public final void projectSlice(float[] pixels) {
		for (int i = 0; i < len; i++)
			if ((pixels[i] > 0.0) && (pixels[i] >= minPixelValueToConsider1) && (pixels[i] <= maxPixelValueToConsider1)) {
				fpixels[i] += pixels[i];
				num_non_0[i]++;
			}
	}

	@Override
	public final void projectSlice(double[] pixels) {
		for (int i = 0; i < len; i++)
			if ((pixels[i] > 0.0) && (pixels[i] >= minPixelValueToConsider1) && (pixels[i] <= maxPixelValueToConsider1)) {
				fpixels[i] += pixels[i];
				num_non_0[i]++;
			}
	}

	@Override
	public final void postProcess() {
		for (int i = 0; i < len; i++)
			if (num_non_0[i] > 0) {
				fpixels[i] /= num_non_0[i];
			} else
				fpixels[i] = 0.0f; // should be 0 already anyway
	}

	@Override
	public final void preProcess() {
	}

} // end AverageIntensity_NO_0