package processing_utilities.projection;

/** Compute standard deviation projection. */
public class StandardDeviation extends RayFunction {
	private float[] result;
	private double[] sum, sum2;
	private int num, len;

	@Override
	public final float[] projectProjection(int stride) {
		int nSteps = result.length / stride;
		float[] finalResult = new float[stride];

		if (nSteps * stride != result.length)
			throw new RuntimeException("in standard division projectProjection: " + nSteps + "*" + stride + "!="
					+ result.length);
		double doubleNum = (num * nSteps);
		for (int i = 0; i < stride; i++) {
			double finalSum = 0d;
			double finalSum2 = 0d;
			for (int j = 0; j < nSteps; j++) {
				finalSum += sum[i + stride * j];
				finalSum2 += sum2[i + stride * j];
			}
			double stdDev = (doubleNum * finalSum2 - finalSum * finalSum) / doubleNum;
			if (stdDev > 0.0)
				finalResult[i] = (float) Math.sqrt(stdDev / (doubleNum - 1.0));
			else
				finalResult[i] = 0f;
		}
		return finalResult;
	}

	public StandardDeviation(float[] fp, int num, float minPixelValueToConsider, float maxPixelValueToConsider) {// num
																													// is
																													// the
																													// number
																													// of
																													// slices
		result = fp;
		len = result.length;
		this.num = num;
		sum = new double[len];
		sum2 = new double[len];
		this.minPixelValueToConsider = minPixelValueToConsider;
		this.maxPixelValueToConsider = maxPixelValueToConsider;
	}

	@Override
	public final void projectSlice(byte[] pixels) {
		int v;
		for (int i = 0; i < len; i++) {
			v = pixels[i] & 0xff;
			if ((v >= minPixelValueToConsider) && (v <= maxPixelValueToConsider)) {
				sum[i] += v;
				sum2[i] += v * v;
			}
		}
	}

	@Override
	public final void projectSlice(short[] pixels) {
		double v;
		for (int i = 0; i < len; i++) {
			v = pixels[i] & 0xffff;
			if ((v >= minPixelValueToConsider) && (v <= maxPixelValueToConsider)) {
				sum[i] += v;
				sum2[i] += v * v;
			}
		}
	}

	@Override
	public final void projectSlice(float[] pixels) {
		double v;
		for (int i = 0; i < len; i++) {
			v = pixels[i];
			if ((v >= minPixelValueToConsider) && (v <= maxPixelValueToConsider)) {
				sum[i] += v;
				sum2[i] += v * v;
			}
		}
	}

	@Override
	public final void projectSlice(double[] pixels) {
		double v;
		for (int i = 0; i < len; i++) {
			v = pixels[i];
			if ((v >= minPixelValueToConsider) && (v <= maxPixelValueToConsider)) {
				sum[i] += v;
				sum2[i] += v * v;
			}
		}
	}

	@Override
	public final void postProcess() {
		double stdDev;
		double n = num;
		for (int i = 0; i < len; i++) {
			if (num > 1) {
				stdDev = (n * sum2[i] - sum[i] * sum[i]) / n;
				if (stdDev > 0.0)
					result[i] = (float) Math.sqrt(stdDev / (n - 1.0));
				else
					result[i] = 0f;
			} else
				result[i] = 0f;
		}
	}

	@Override
	public final void preProcess() {

	}

} // end StandardDeviation