//Adapted from ImageJ??
package pipeline.plugins.image_processing;

import ij.plugin.filter.GaussianBlur;
import ij.process.Blitter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.awt.Rectangle;

import pipeline.PreviewType;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOImage;
import pipeline.misc_util.ParameterListenerWeakRef;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.FloatParameter;
import pipeline.parameters.ParameterListener;
import pipeline.parameters.ParameterListenerAdapter;
import pipeline.plugins.TwoDPlugin;

/**
 * This plugin-filter implements ImageJ's Unsharp Mask command.
 * Unsharp masking subtracts a blurred copy of the image and rescales the image
 * to obtain the same contrast of large (low-frequency) structures as in the
 * input image. This is equivalent to adding a high-pass filtered image and
 * thus sharpens the image.
 * "Radius (Sigma)" is the standard deviation (blur radius) of the Gaussian blur that
 * is subtracted. "Mask Weight" determines the strength of filtering, where "Mask Weight"=1
 * would be an infinite weight of the high-pass filtered image that is added.
 */
public class UnsharpMask extends TwoDPlugin {

	@Override
	public String operationName() {
		return "Unsharp";
	}

	@Override
	public String version() {
		return "1.0";
	}

	private class SigmaListener extends ParameterListenerAdapter {

		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			if (((float[]) sigma_param.getValue())[0] != sigma) {
				sigma = ((float[]) sigma_param.getValue())[0];
				pipelineCallback.parameterValueChanged(ourRow, null, false);
			}
		}
	}

	private ParameterListener sigmaListener0 = new SigmaListener();
	private ParameterListener sigmaListener1 = new ParameterListenerWeakRef(sigmaListener0);

	private AbstractParameter sigma_param = new FloatParameter("Sigma",
			"Sets the spread of the distribution used for convolution.\nHigher values lead to more blurring.", 1.0f,
			0.0f, 20.0f, true, true, true, sigmaListener1);

	private class WeightListener extends ParameterListenerAdapter {

		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			if (((float[]) weight_param.getValue())[0] != weight) {
				weight = ((float[]) weight_param.getValue())[0];
				pipelineCallback.parameterValueChanged(ourRow, null, false);
			}
		}
	}

	private ParameterListener weightListener0 = new WeightListener();
	private ParameterListener weightListener1 = new ParameterListenerWeakRef(weightListener0);

	private AbstractParameter weight_param = new FloatParameter("Weight", "Sets the strength of the effect.", 0.9f,
			0.0f, 1.0f, true, true, true, weightListener1);

	@Override
	public ParameterListener[] getParameterListeners() {
		return new ParameterListener[] { sigmaListener1, weightListener1 };
	}

	@Override
	public AbstractParameter[] getParameters() {
		AbstractParameter[] paramArray = { sigma_param, weight_param };
		return paramArray;
	}

	@Override
	public void setParameters(AbstractParameter[] param) {
		sigma_param = param[0];
		weight_param = param[1];
		sigma = ((float[]) sigma_param.getValue())[0];
		weight = ((float[]) weight_param.getValue())[0];
	}

	@Override
	public int getFlags() {
		return SAME_AS_FLOAT + PARALLELIZE_WITH_NEW_INSTANCES + ONLY_FLOAT_INPUT;
	}

	@Override
	public void setInput(IPluginIO source) {
		super.setInput(source);
		if (tempFloatProcessor == null) {
			tempFloatProcessor =
					new FloatProcessor(((IPluginIOImage) source).getDimensions().width, ((IPluginIOImage) source)
							.getDimensions().height);
		} else {
			if ((tempFloatProcessor.getWidth() != ((IPluginIOImage) source).getDimensions().width)
					|| (tempFloatProcessor.getHeight() != ((IPluginIOImage) source).getDimensions().height))
				tempFloatProcessor =
						new FloatProcessor(((IPluginIOImage) source).getDimensions().width, ((IPluginIOImage) source)
								.getDimensions().height);

		}
	}

	private ImageProcessor destination_processor;

	private double sigma = 1.0; // standard deviation of the Gaussian
	private double weight = 0.9; // weight of the mask
	private GaussianBlur gb;
	private FloatProcessor tempFloatProcessor = null;

	private int lastWidth = 0;
	private int lastHeight = 0;

	@Override
	public void runSlice(ImageProcessor ip, ImageProcessor dest, PreviewType previewType) {
		if (dest != null) {
			destination_processor = dest;
		}
		if (tempFloatProcessor == null) {
			tempFloatProcessor = new FloatProcessor(ip.getWidth(), ip.getHeight());
			lastWidth = ip.getWidth();
			lastHeight = ip.getHeight();
		} else {
			if ((lastWidth != ip.getWidth()) || (lastHeight != ip.getHeight())) {
				tempFloatProcessor = new FloatProcessor(ip.getWidth(), ip.getHeight());
				lastWidth = ip.getWidth();
				lastHeight = ip.getHeight();
			}
		}
		sharpenFloat(ip, sigma, (float) weight);
	}

	/** Unsharp Mask filtering of a float image. */
	public void sharpenFloat(ImageProcessor fp, double sigma, float weight) {
		if (gb == null)
			gb = new GaussianBlur();
		int width = fp.getWidth();
		Rectangle roi = fp.getRoi();
		if (fp instanceof FloatProcessor)
			tempFloatProcessor.copyBits(fp, 0, 0, Blitter.COPY);
		else {
			Utils.log("Our unsharp plugin is lazy and only takes float as input", LogLevel.ERROR);
			throw new IllegalArgumentException();
		}
		gb.blurGaussian(tempFloatProcessor, sigma, sigma, 0.01);
		float[] blurred_pixels = (float[]) tempFloatProcessor.getPixels();
		float[] output_pixels = (float[]) destination_processor.getPixels();
		float[] fp_pixels = (float[]) fp.getPixels();
		for (int y = roi.y; y < roi.y + roi.height; y++)
			for (int x = roi.x, p = width * y + x; x < roi.x + roi.width; x++, p++)
				output_pixels[p] = (fp_pixels[p] - weight * blurred_pixels[p]) / (1f - weight);
	}

}
