/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.input_output;

import ij.measure.Calibration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;

import pipeline.PreviewType;
import pipeline.GUI_utils.PluginIOHyperstackViewWithImagePlus;
import pipeline.GUI_utils.PluginIOView;
import pipeline.GUI_utils.image_with_toolbar.PluginIOHyperstackWithToolbar;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOHyperstack;
import pipeline.data.IPluginIOImage;
import pipeline.data.IPluginIOStack;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOHyperstack;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.data.PluginIOString;
import pipeline.misc_util.FileNameUtils;
import pipeline.misc_util.IntrospectionParameters.ParameterInfo;
import pipeline.misc_util.IntrospectionParameters.ParameterType;
import pipeline.misc_util.PluginRuntimeException;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.MultiListParameter;
import pipeline.parameters.TableParameter;
import pipeline.plugins.AuxiliaryInputOutputPlugin;
import pipeline.plugins.FourDPlugin;
import pipeline.plugins.SpecialDimPlugin;

/**
 * Performs a "shallow copy" of the source dataset, i.e. the pixel arrays are not duplicated. This is a very quick copy,
 * but means that any modification of the output of this plugin will affect the source image.
 *
 */
public class LazyCopy extends FourDPlugin implements SpecialDimPlugin, AuxiliaryInputOutputPlugin {

	@ParameterInfo(userDisplayName = "Convert to ...", stringValue = "No conversion", stringChoices = {
			"No conversion", "8 bit", "16 bit", "32 bit" }, noErrorIfMissingOnReload = true)
	@ParameterType(parameterType = "ComboBox", printValueAsString = true)
	private String pixelDepth;

	@Override
	public String[] getInputLabels() {
		return new String[] { "Aux 1", "Aux 2", "Aux 3", "Aux 4", "Aux 5" };
	}

	@Override
	public String[] getOutputLabels() {
		return new String[] { "File name" };
	}

	@Override
	public String operationName() {
		return "LazyCopy";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public int getFlags() {
		return DONT_ALLOCATE_OUTPUT_PIXELS;
	}

	@Override
	public Map<String, InputOutputDescription> getInputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		switch (pixelDepth) {
			case "No conversion":
				result.put("Default source", new InputOutputDescription(null, null, new PixelType[] {
						PixelType.FLOAT_TYPE, PixelType.BYTE_TYPE, PixelType.SHORT_TYPE },
						InputOutputDescription.NOT_SPECIFIED, InputOutputDescription.NOT_SPECIFIED, false, false));
				break;
			case "8 bit":
				result.put("Default source", new InputOutputDescription(null, null,
						new PixelType[] { PixelType.BYTE_TYPE }, InputOutputDescription.NOT_SPECIFIED,
						InputOutputDescription.NOT_SPECIFIED, false, false));
				break;
			case "16 bit":
				result.put("Default source", new InputOutputDescription(null, null,
						new PixelType[] { PixelType.SHORT_TYPE }, InputOutputDescription.NOT_SPECIFIED,
						InputOutputDescription.NOT_SPECIFIED, false, false));
				break;
			case "32 bit":
				result.put("Default source", new InputOutputDescription(null, null,
						new PixelType[] { PixelType.FLOAT_TYPE }, InputOutputDescription.NOT_SPECIFIED,
						InputOutputDescription.NOT_SPECIFIED, false, false));
				break;
			default:
				throw new IllegalStateException("Unknown conversion choice " + pixelDepth);
		}
		return result;
	}

	@Override
	public int getOutputDepth(IPluginIO input) {
		int maxDepth = -1;
		for (IPluginIO io : pluginInputs.values()) {
			int depth = ((IPluginIOImage) io).getDimensions().depth;
			if (depth > maxDepth) {
				maxDepth = depth;
			}
		}
		return maxDepth;
	}

	@Override
	public int getOutputNTimePoints(IPluginIO input) {
		int maxNTP = -1;
		for (IPluginIO io : pluginInputs.values()) {
			int nTimePoints = ((IPluginIOImage) io).getDimensions().nTimePoints;
			if (nTimePoints > maxNTP) {
				maxNTP = nTimePoints;
			}
		}
		return maxNTP;
	}

	@Override
	public int getOutputHeight(IPluginIO input) {
		int maxHeight = -1;
		for (IPluginIO io : pluginInputs.values()) {
			int height = ((IPluginIOImage) io).getDimensions().height;
			if (height > maxHeight) {
				maxHeight = height;
			}
		}
		return maxHeight;
	}

	@Override
	public int getOutputNChannels(IPluginIO input) {
		int sumChannels = 0;
		for (IPluginIO io : pluginInputs.values()) {
			if (io instanceof IPluginIOHyperstack) {
				sumChannels += ((IPluginIOHyperstack) io).getnChannels();
			}
		}
		return sumChannels;
	}

	@Override
	public int getOutputWidth(IPluginIO input) {
		int maxWidth = -1;
		for (IPluginIO io : pluginInputs.values()) {
			int width = ((IPluginIOImage) io).getDimensions().width;
			if (width > maxWidth) {
				maxWidth = width;
			}
		}
		return maxWidth;
	}

	@Override
	public boolean shouldClearOutputs() {
		IPluginIOHyperstack input = (IPluginIOHyperstack) getInput();

		IPluginIOHyperstack destination = (IPluginIOHyperstack) getOutput();

		if (destination != null
				&& (input.getDepth() != destination.getDepth() || input.getWidth() != destination.getWidth() || input
						.getHeight() != destination.getHeight())) {
			clearOutputs();
			return true;
		}
		return false;
	}

	public static final class DimensionMismatchException extends PluginRuntimeException {

		private static final long serialVersionUID = -4594939512721080584L;

		public DimensionMismatchException(String message, boolean displayUserDialog) {
			super(message, displayUserDialog);
		}
	}

	@SuppressWarnings("null")
	@Override
	public void run(ProgressReporter r, MultiListParameter inChannels, TableParameter outChannels,
			PreviewType previewType, boolean inputHasChanged, AbstractParameter parameterWhoseValueChanged,
			boolean stayInCoreLoop) {

		IPluginIOHyperstack destination = (IPluginIOHyperstack) getOutput();

		Iterator<IPluginIOStack> destinationIterator = destination.getChannels().values().iterator();

		for (IPluginIO io : pluginInputs.values()) {
			if (io instanceof IPluginIOHyperstack) {
				if (io.getDiskLocation() != null) {
					String filePath = io.getDiskLocation();
					@NonNull String fileName = new File(filePath).getName();
					// Be careful when stripping the extension that the file might not have an extension,
					// and a "." character could occur in a parent directory name
					if (fileName.contains(".")) {
						fileName = filePath.substring(0, filePath.lastIndexOf('.'));
					}
					fileName = FileNameUtils.compactPath(fileName);
					pluginOutputs.put("File name", new PluginIOString(fileName));
				}
				IPluginIOHyperstack hst = (IPluginIOHyperstack) io;
				
				boolean xyDimensionMismatch = 
						hst.getWidth() != destination.getDimensions().width
						|| hst.getHeight() != destination.getDimensions().height;
				
				/*if (hst.getWidth() != getImageInput().getDimensions().width
						|| hst.getHeight() != getImageInput().getDimensions().height
						|| hst.getDepth() != getImageInput().getDimensions().depth)
					throw new DimensionMismatchException("Dimension mismatch between inputs of LazyCopy plugin", true);
				*/
				
				// FIXME Should concatenate (or rather structure properly) metadata from different pluginInputs rather
				// than just keeping the metadata from the last source we iterate over
				destination.setImageAcquisitionMetadata(hst.getImageAcquisitionMetadata());
				for (IPluginIOStack channel : hst.getChannels().values()) {
					IPluginIOStack destinationChannel;
					if (!destinationIterator.hasNext()) {
						destinationChannel = destination.addChannel(null);
					} else
						destinationChannel = destinationIterator.next();
					destinationChannel.setImageAcquisitionMetadata(channel.getImageAcquisitionMetadata());
					//TODO Loop below will need adjusting if more than 1 time point
					for (int z = 0; z < channel.getDepth() * channel.getnTimePoints(); z++) {
						if (xyDimensionMismatch) {
							final Object pixels;
							final int length = destinationChannel.getWidth() * destinationChannel.getHeight();
							switch(destinationChannel.getPixelType()) {
								case BYTE_TYPE:
									pixels = new byte[length];
									break;
								case FLOAT_TYPE:
									pixels = new float[length];
									break;
								case SHORT_TYPE:
									pixels = new short[length];
									break;
								default:
									throw new RuntimeException("Unknown pixel type " + 
											destinationChannel.getPixelType());
							}
							destinationChannel.setPixels(pixels, z);
							for (int x = 0; x < hst.getWidth(); x++) {
								for (int y = 0; y < hst.getHeight(); y++) {
									destinationChannel.setPixelValue(x, y, z, 
											channel.getPixelValue(x, y, z));
								}
							}
						} else {
							if (channel.getPixels(z) == null) {
								Utils.log("Null input slice", LogLevel.WARNING);
							}
							destinationChannel.setPixels(channel.getPixels(z), z);
						}
					}

				}
			}
		}
	}

	@Override
	public List<PluginIOView> createOutput(@NonNull String outputName, PluginIOHyperstackViewWithImagePlus impForDisplay,
			Map<String, IPluginIO> linkedOutputs) throws InterruptedException {
		PluginIOHyperstack createdOutput =
				new PluginIOHyperstack(outputName, getOutputWidth(getInput()), getOutputHeight(getInput()),
						getOutputDepth(getInput()), ((IPluginIOImage) getInput()).getDimensions().nChannels,
						getOutputNTimePoints(getInput()), ((IPluginIOImage) getInput()).getPixelType(), true);

		int nChannelsToAdd = getOutputNChannels(null) - ((IPluginIOImage) getInput()).getDimensions().nChannels;

		for (int i = 0; i < nChannelsToAdd; i++) {
			createdOutput.addChannel("Aux channel " + i);
		}

		setOutput("Default destination", createdOutput, true);

		PluginIOHyperstackViewWithImagePlus display = null;
		if (impForDisplay != null) {
			createdOutput.setImp(impForDisplay);
			display = impForDisplay;
		} else {
			display = new PluginIOHyperstackWithToolbar(createdOutput.getName());// PluginIOHyperstackViewWithImagePlus
			createdOutput.setImp(display);
		}

		ArrayList<PluginIOView> imagesToShow = new ArrayList<>();

		display.addImage(createdOutput);
		display.shouldUpdateRange = true;
		imagesToShow.add(display);

		Calibration inputCal = ((IPluginIOImage) getInput()).getCalibration();
		if (inputCal != null)
			createdOutput.setCalibration((Calibration) inputCal.clone());
		else
			Utils.displayMessage("LazyCopy: no calibration in input image; continuing anyway", true, LogLevel.WARNING);

		return imagesToShow;
	}

	@Override
	public PixelType getOutputPixelType(IPluginIOStack input) {
		return ((IPluginIOImage) getInput()).getPixelType();
	}

}
