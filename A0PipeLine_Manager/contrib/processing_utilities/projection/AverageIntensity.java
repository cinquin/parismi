package processing_utilities.projection;

/** Compute average intensity projection. */
public class AverageIntensity extends RayFunction {
	private float[] fpixels;
	private int num, len;
	private float minPixelValueToConsider1;
	private float maxPixelValueToConsider1;

	@Override
	public final float[] projectProjection(int stride) {
		int nSteps = fpixels.length / stride;
		float[] finalResult = new float[stride];
		if (nSteps * stride != fpixels.length)
			throw new RuntimeException("in standard division AverageIntensity: " + nSteps + "*" + stride + "!="
					+ fpixels.length);
		double doubleNum = (double) num * stride;
		for (int i = 0; i < stride; i++) {
			for (int j = 0; j < nSteps; j++) {
				finalResult[i] += fpixels[i + stride * j];
			}
			// need to test method because we could have been called to compute a sum and not an average
			if (method == AVG_METHOD)
				finalResult[i] = (float) (finalResult[i] / doubleNum);
		}
		return finalResult;
	}

	private int method = -1;

	/**
	 * Constructor requires number of slices to be
	 * projected. This is used to determine average at each
	 * pixel.
	 * 
	 * @param method
	 */
	public AverageIntensity(int method, float[] fp, int num, float minPixelValueToConsider,
			float maxPixelValueToConsider) {
		this.method = method;
		fpixels = fp;
		len = fpixels.length;
		this.num = num;
		this.minPixelValueToConsider1 = minPixelValueToConsider;
		this.maxPixelValueToConsider1 = maxPixelValueToConsider;
	}

	@Override
	public final void projectSlice(byte[] pixels) {
		for (int i = 0; i < len; i++)
			if (((pixels[i] & 0xff) >= minPixelValueToConsider1) && ((pixels[i] & 0xff) <= maxPixelValueToConsider1))
				fpixels[i] += (pixels[i] & 0xff);
	}

	@Override
	public final void projectSlice(short[] pixels) {
		for (int i = 0; i < len; i++)
			if (((pixels[i] & 0xffff) >= minPixelValueToConsider1)
					&& ((pixels[i] & 0xffff) <= maxPixelValueToConsider1))
				fpixels[i] += pixels[i] & 0xffff;
	}

	@Override
	public final void projectSlice(float[] pixels) {
		for (int i = 0; i < len; i++)
			if ((pixels[i] >= minPixelValueToConsider1) && (pixels[i] <= maxPixelValueToConsider1))
				fpixels[i] += pixels[i];
	}

	@Override
	public final void projectSlice(double[] pixels) {
		for (int i = 0; i < len; i++)
			if ((pixels[i] >= minPixelValueToConsider1) && (pixels[i] <= maxPixelValueToConsider1))
				fpixels[i] += pixels[i];
	}

	@Override
	public void postProcess() {
		float fnum = num;
		if (method == AVG_METHOD)
			for (int i = 0; i < len; i++)
				fpixels[i] /= fnum;
	}

	@Override
	public final void preProcess() {
	}

} // end AverageIntensity