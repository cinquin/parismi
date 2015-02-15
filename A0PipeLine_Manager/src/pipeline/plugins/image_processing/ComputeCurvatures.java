
/**
 * <p>
 * Title: Principle Curvature Plugin for ImageJ
 * </p>
 *
 * <p>
 * Description: Computes the Principle Curvatures of for 2D and 3D images except the pixels/voxels directly at the
 * borders of the image
 * </p>
 *
 * <p>
 * Copyright: Copyright (c) 2007
 * </p>
 *
 * <p>
 * Company: MPI-CBG
 * </p>
 *
 * <p>
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License 2 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * @author Stephan Preibisch
 * @version 1.0
 *
 *          Change in this version (Mark Longair):
 *
 *          - Made the top level plugin a wrapper for this class so that
 *          "features" package so that it can be used by classes in other
 *          packages.
 *
 *          - Now implements Runnable, with the void run() method creating
 *          the Gaussian and reporting progress via an optional callback.
 *          (If used in this way you need to use the constructor where you
 *          supply an ImagePlus, sigma and an optional callback.
 *
 *          - Switched to using Johannes's JacobiDouble / JacobiFloat classes
 *          for finding the eigenvalues instead of the Jama classes, so we
 *          don't introduce an additional dependency. It's about 15% faster
 *          with JacobiDouble, but I haven't yet measured any speed gain due
 *          to using floats rather than doubles.
 *
 *          - Added options to only generate particular eigenvalue images, and
 *          some other slight changes to the initial dialog.
 *
 *          - Added additional methods using floats for calculation (and the
 *          interface) in case clients only need floats.
 *
 *          - Added ordering of the eigenvalues (optionally on absolute
 *          values).
 *
 *          - Added normalisation of the eigenvalues to that the largest has
 *          size 1 (some papers use methods that require this)
 *
 *          - Now we take notice of the calibration information (or not, if
 *          that option is deselected).
 *
 *          - Use some faster eigenvalue calculation code for the 3x3 case.
 */
/*******************************************************************************
 * Code modified for Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/

package pipeline.plugins.image_processing;

import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicInteger;

import pipeline.PreviewType;
import pipeline.data.IPluginIOStack;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.data.PluginIOStack;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.FloatParameter;
import pipeline.parameters.FloatRangeParameter;
import pipeline.parameters.ParameterListener;
import pipeline.parameters.SplitParameter;
import pipeline.parameters.SplitParameterListener;
import pipeline.plugins.ThreeDPlugin;
import processing_utilities.curvature.math3d.JacobiFloat;

public class ComputeCurvatures extends ThreeDPlugin {

	@Override
	public String operationName() {
		return "3D curvature filter";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public int getFlags() {
		return SAME_AS_FLOAT + ONLY_FLOAT_INPUT + ONE_OUTPUT_CHANNEL_PER_INPUT_CHANNEL;
	}

	private float sigma = 5.0f;

	private class sigmoidListener implements ParameterListener {
		private String parameterName;

		@Override
		public String getParameterName() {
			return parameterName;
		}

		@Override
		public void setParameterName(String name) {
			parameterName = name;
		}

		@Override
		public void buttonPressed(String commandName, AbstractParameter parameter, ActionEvent event) {
		}

		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			boolean reRun = false;
			if (((float[]) max_and_EC50.getValue())[0] != EC50) {
				EC50 = ((float[]) max_and_EC50.getValue())[0];
				reRun = true;
			}
			if (((float[]) max_and_EC50.getValue())[1] != maxSigmoid) {
				maxSigmoid = ((float[]) max_and_EC50.getValue())[1];
				reRun = true;
			}
			if (reRun)
				pipelineCallback.parameterValueChanged(ourRow, null, false);
		}

		@Override
		public void parameterPropertiesChanged(AbstractParameter parameterWhosePropertiesChanged) {
		}

		@Override
		public boolean alwaysNotify() {
			return false;
		}
	}

	private class scaleListener implements ParameterListener {
		private String parameterName;

		@Override
		public String getParameterName() {
			return parameterName;
		}

		@Override
		public void setParameterName(String name) {
			parameterName = name;
		}

		@Override
		public void buttonPressed(String commandName, AbstractParameter parameter, ActionEvent event) {
		}

		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			if (((float[]) scale_param.getValue())[0] != scale) {
				scale = ((float[]) scale_param.getValue())[0];
				pipelineCallback.parameterValueChanged(ourRow, null, false);
			}
		}

		@Override
		public void parameterPropertiesChanged(AbstractParameter parameterWhosePropertiesChanged) {
		}

		@Override
		public boolean alwaysNotify() {
			return false;
		}
	}

	private class scale1Listener implements ParameterListener {
		private String parameterName;

		@Override
		public String getParameterName() {
			return parameterName;
		}

		@Override
		public void setParameterName(String name) {
			parameterName = name;
		}

		@Override
		public void buttonPressed(String commandName, AbstractParameter parameter, ActionEvent event) {
		}

		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			if (((float[]) scale1_param.getValue())[0] != scale1) {
				scale1 = ((float[]) scale1_param.getValue())[0];
				pipelineCallback.parameterValueChanged(ourRow, null, false);
			}
		}

		@Override
		public void parameterPropertiesChanged(AbstractParameter parameterWhosePropertiesChanged) {
		}

		@Override
		public boolean alwaysNotify() {
			return false;
		}
	}

	private class sigmaListener implements ParameterListener {
		private String parameterName;

		@Override
		public String getParameterName() {
			return parameterName;
		}

		@Override
		public void setParameterName(String name) {
			parameterName = name;
		}

		@Override
		public void buttonPressed(String commandName, AbstractParameter parameter, ActionEvent event) {
		}

		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			if (((float[]) sigma_param.getValue())[0] != sigma) {
				sigma = ((float[]) sigma_param.getValue())[0];
				pipelineCallback.parameterValueChanged(ourRow, null, false);
			}
		}

		@Override
		public void parameterPropertiesChanged(AbstractParameter parameterWhosePropertiesChanged) {
		}

		@Override
		public boolean alwaysNotify() {
			return false;
		}
	}

	private float scale = 3.0f;
	private float scale1 = 1.0e-5f;
	private float[] OneDKernel;
	private float EC50 = 100f;
	private float maxSigmoid = 100f;

	private AbstractParameter max_and_EC50 = new FloatRangeParameter("EC50 and Max",
			"Sets parameters of the sigmoid that weighs pixels values before multiplication with local tubeness value",
			100f, 100f, 0f, 200f, true, true, new sigmoidListener(), null);
	private AbstractParameter scale_param = new FloatParameter("Scale", "Multiplier in exponential for tubeness.",
			3.0f, 0.0f, 20.0f, true, true, true, new scaleListener());
	private AbstractParameter scale1_param = new FloatParameter("Scale1",
			"Multiplication factor for length of lines to blur along.", 1.0e-5f, 1.0e-7f, 1.0f, true, true, true,
			new scale1Listener());
	private AbstractParameter sigma_param = new FloatParameter("Sigma", "", 5.0f, 0.0f, 20.0f, true, true, true,
			new sigmaListener());

	private AbstractParameter splitScaleParams, splitSigmoidSigma;

	@Override
	public AbstractParameter[] getParameters() {
		if (splitScaleParams == null) {
			splitScaleParams = new SplitParameter(new Object[] { scale_param, scale1_param });
			splitSigmoidSigma = new SplitParameter(new Object[] { max_and_EC50, sigma_param });
			((FloatParameter) scale1_param).useExponentialFormat = true;
		}
		return new AbstractParameter[] { splitScaleParams, splitSigmoidSigma };
	}

	@Override
	public ParameterListener[] getParameterListeners() {
		return new ParameterListener[] {
				new SplitParameterListener(new ParameterListener[] { new scaleListener(), new scale1Listener() }),
				new SplitParameterListener(new ParameterListener[] { new sigmoidListener(), new sigmaListener() }) };
	}

	@Override
	public void setParameters(AbstractParameter[] param) {

		Object[] splitParameters = (Object[]) param[0].getValue();
		scale_param = (AbstractParameter) splitParameters[0];
		scale1_param = (AbstractParameter) splitParameters[1];
		Object[] splitParameters2 = (Object[]) param[1].getValue();
		max_and_EC50 = (AbstractParameter) splitParameters2[0];
		sigma_param = (AbstractParameter) splitParameters2[1];

		EC50 = ((float[]) max_and_EC50.getValue())[0];
		maxSigmoid = ((float[]) max_and_EC50.getValue())[1];
		scale = ((float[]) scale_param.getValue())[0];
		scale1 = ((float[]) scale1_param.getValue())[0];
		sigma = ((float[]) sigma_param.getValue())[0];
	}

	private static float norm(float[] vector) {
		float sum = 0;
		for (float element : vector) {
			sum += element * element;
		}
		return (float) Math.sqrt(sum);
	}

	void blurAlongLine(final PluginIOStack output, float pixelValue, float[] centerPoint, float[] vector,
			float distanceToTravel) {
		float lineResolution = 0.1f;
		int nbreaks = (int) (distanceToTravel / lineResolution);
		float[] increment = new float[centerPoint.length];
		float[] currentPoint = new float[centerPoint.length];
		for (int i = 0; i < increment.length; i++) {
			increment[i] = (distanceToTravel * vector[i]) / (norm(vector) * (nbreaks));
			currentPoint[i] = centerPoint[i] - (nbreaks) * 0.5f * increment[i];
		}

		for (int i = -nbreaks / 2; i < nbreaks / 2; i++) {
			Utils.add(currentPoint, increment);
			int roundedX, roundedY, roundedZ;
			roundedX = (int) currentPoint[0];
			roundedY = (int) currentPoint[1];
			roundedZ = (int) currentPoint[2];

			if ((roundedX < 0) || (roundedX >= output.getWidth() - 1))
				continue;
			if ((roundedY < 0) || (roundedY >= output.getHeight() - 1))
				continue;
			if ((roundedZ < 0) || (roundedZ > output.getDepth() - 1))
				continue;

			float gaussianFactor;
			int absI = Math.abs(i);
			gaussianFactor = OneDKernel[((OneDKernel.length - 1) * absI * 2) / nbreaks];
			// TODO CHECK HOW LONG THE KERNEL IS CREATED

			((float[]) output.getStackPixelArray()[roundedZ])[roundedX + roundedY * output.getWidth()] +=
					pixelValue * gaussianFactor;
		}
	}

	float sigmoid(float x) {
		float result = maxSigmoid * (x * x) / (EC50 * EC50 + x * x);
		return result;
	}

	private AtomicInteger thread_index = new AtomicInteger(0);

	@Override
	public void runChannel(final IPluginIOStack input, final IPluginIOStack output, final ProgressReporter p,
			PreviewType previewType, boolean inputHasChanged) throws InterruptedException {

		final float sepX = 1, sepY = 1, sepZ = 1;
		nCpus = Runtime.getRuntime().availableProcessors();
		if (input.getDepth() < nCpus)
			nCpus = input.getDepth();
		threads = new Thread[nCpus];
		final PluginIOStack[] tempStorage = new PluginIOStack[nCpus];

		for (int i = 0; i < nCpus; i++) {
			tempStorage[i] =
					new PluginIOStack("Temp storage", input.getWidth(), input.getHeight(), input.getDepth(), 1,
							PixelType.FLOAT_TYPE);
		}

		input.computePixelArray();
		output.computePixelArray();

		progressSetValueThreadSafe(p, 0);
		progressSetIndeterminateThreadSafe(p, false);

		for (int z = 0; z < input.getDepth(); z++)
			for (int y = 0; y < input.getHeight(); y++)
				for (int x = 0; x < input.getWidth(); x++)
					((float[]) output.getStackPixelArray()[z])[x + y * output.getWidth()] = 0;

		OneDKernel = createGaussianKernel1D(sigma, true);

		for (int ithread = 0; ithread < nCpus; ithread++) {
			threads[ithread] = new Thread("Directional blurring 1") {
				@Override
				public void run() {
					final int localThreadIndex = thread_index.getAndIncrement();
					for (int z = slice_registry.getAndIncrement(); z < input.getDepth() - 1; z =
							slice_registry.getAndIncrement()) {
						for (int y = 1; y < input.getHeight() - 1; y++) {
							for (int x = 1; x < input.getWidth() - 1; x++) {
								JacobiFloat jacobi =
										new JacobiFloat(computeHessianMatrix3DFloat(input, x, y, z, sigma, sepX, sepY,
												sepZ));
								float[][] eigenVectors = jacobi.getEigenVectors();
								float[] eigenValues = jacobi.getEigenValues();

								// blur along the direction of the eigenvector with smallest eigenvalue
								int minIndex = Utils.indexOfMin(Utils.arrayAbs(eigenValues));
								float[] centerPoint = new float[] { x, y, z };

								float lowEigenVal = eigenValues[minIndex];
								float[] otherEigenVal = new float[2];
								int i = 0;
								for (int j = 0; j < 3; j++) {
									if (j != minIndex) {
										otherEigenVal[i] = eigenValues[j];
										i++;
									}
								}

								if ((otherEigenVal[0] > 0) || (otherEigenVal[1] > 0)) {
									((float[]) output.getStackPixelArray()[z])[x + y * output.getWidth()] = 0;// ((float
																												// [])
																												// input.pixelArray[z])[x+y*output.width];
									continue;
								}
								float pixelValue = ((float[]) input.getStackPixelArray()[z])[x + y * output.getWidth()];
								float distanceToTravel =
										(otherEigenVal[0] * otherEigenVal[1])
												* ((float) Math
														.exp(-scale
																* ((lowEigenVal / otherEigenVal[0])
																		* (lowEigenVal / otherEigenVal[0]) + (lowEigenVal / otherEigenVal[1])
																		* (lowEigenVal / otherEigenVal[1]))));
								// if (distanceToTravel<0) distanceToTravel*=-1f;

								//
								blurAlongLine(tempStorage[localThreadIndex], sigmoid(distanceToTravel / (pixelValue)
										+ pixelValue), centerPoint, eigenVectors[minIndex], scale1 * distanceToTravel
										/ pixelValue);

							}
						}
						progressSetValueThreadSafe(p, (int) (100f * z / input.getDepth()));
					}
				}
			};
		}

		slice_registry.set(1);
		thread_index.set(0);
		startAndJoin(threads);

		// Now sum up all the tempStorage arrays

		for (int ithread = 0; ithread < nCpus; ithread++) {
			threads[ithread] = new Thread("Direcitonal blurring 2") {
				@Override
				public void run() {
					for (int z = slice_registry.getAndIncrement(); z < input.getDepth() - 1; z =
							slice_registry.getAndIncrement()) {
						for (int y = 1; y < input.getHeight() - 1; y++) {
							for (int x = 1; x < input.getWidth() - 1; x++) {
								for (int ts = 0; ts < nCpus; ts++) {
									((float[]) output.getStackPixelArray()[z])[x + y * output.getWidth()] +=
											((float[]) tempStorage[ts].getStackPixelArray()[z])[x + y
													* output.getWidth()];
								}
							}
						}
					}
				}
			};
		}

		slice_registry.set(1);
		startAndJoin(threads);
	}

	// ------------------------------------------------------------------------

	/*
	 * There are four versions of the this function, for calculating
	 * the eigenvalues of the Hessian matrix at a particular point in
	 * the image. There are versions for 2D or 3D and using floats of
	 * doubles.
	 */

	public static double[][] computeHessianMatrix3DDouble(IPluginIOStack img, int x, int y, int z, double sigma,
			float sepX, float sepY, float sepZ) {

		double[][] hessianMatrix = new double[3][3]; // zeile, spalte

		double temp = 2 * img.getDouble(x, y, z);

		// xx
		hessianMatrix[0][0] = img.getDouble(x + 1, y, z) - temp + img.getDouble(x - 1, y, z);

		// yy
		hessianMatrix[1][1] = img.getDouble(x, y + 1, z) - temp + img.getDouble(x, y - 1, z);

		// zz
		hessianMatrix[2][2] = img.getDouble(x, y, z + 1) - temp + img.getDouble(x, y, z - 1);

		// xy
		hessianMatrix[0][1] =
				hessianMatrix[1][0] =
						((img.getDouble(x + 1, y + 1, z) - img.getDouble(x - 1, y + 1, z)) / 2 - (img.getDouble(x + 1,
								y - 1, z) - img.getDouble(x - 1, y - 1, z)) / 2) / 2;

		// xz
		hessianMatrix[0][2] =
				hessianMatrix[2][0] =
						((img.getDouble(x + 1, y, z + 1) - img.getDouble(x - 1, y, z + 1)) / 2 - (img.getDouble(x + 1,
								y, z - 1) - img.getDouble(x - 1, y, z - 1)) / 2) / 2;

		// yz
		hessianMatrix[1][2] =
				hessianMatrix[2][1] =
						((img.getDouble(x, y + 1, z + 1) - img.getDouble(x, y - 1, z + 1)) / 2 - (img.getDouble(x,
								y + 1, z - 1) - img.getDouble(x, y - 1, z - 1)) / 2) / 2;

		// FIXME: get Stephan to remind me why this is needed...
		for (int i = 0; i < 3; i++)
			for (int j = 0; j < 3; j++)
				hessianMatrix[i][j] *= (sigma * sigma);

		return hessianMatrix;
	}

	/**
	 * This method computes the Hessian Matrix for the 3x3x3 environment of a certain voxel <br>
	 * <br>
	 *
	 * The 3D Hessian Matrix:<br>
	 * xx xy xz <br>
	 * yx yy yz <br>
	 * zx zy zz <br>
	 *
	 * @param img
	 *            The image as PluginIOStack
	 * @param x
	 *            The x-position of the voxel
	 * @param y
	 *            The y-position of the voxel
	 * @param z
	 *            The z-position of the voxel
	 * @return float[][] The 3D - Hessian Matrix
	 *
	 *         author Stephan Preibisch
	 */
	private static float[][] computeHessianMatrix3DFloat(IPluginIOStack img, int x, int y, int z, double sigma,
			float sepX, float sepY, float sepZ) {

		float[][] hessianMatrix = new float[3][3]; // zeile, spalte

		float temp = 2 * img.getFloat(x, y, z);

		// xx
		hessianMatrix[0][0] = img.getFloat(x + 1, y, z) - temp + img.getFloat(x - 1, y, z);

		// yy
		hessianMatrix[1][1] = img.getFloat(x, y + 1, z) - temp + img.getFloat(x, y - 1, z);

		// zz
		hessianMatrix[2][2] = img.getFloat(x, y, z + 1) - temp + img.getFloat(x, y, z - 1);

		// xy
		hessianMatrix[0][1] =
				hessianMatrix[1][0] =
						((img.getFloat(x + 1, y + 1, z) - img.getFloat(x - 1, y + 1, z)) / 2 - (img.getFloat(x + 1,
								y - 1, z) - img.getFloat(x - 1, y - 1, z)) / 2) / 2;

		// xz
		hessianMatrix[0][2] =
				hessianMatrix[2][0] =
						((img.getFloat(x + 1, y, z + 1) - img.getFloat(x - 1, y, z + 1)) / 2 - (img.getFloat(x + 1, y,
								z - 1) - img.getFloat(x - 1, y, z - 1)) / 2) / 2;

		// yz
		hessianMatrix[1][2] =
				hessianMatrix[2][1] =
						((img.getFloat(x, y + 1, z + 1) - img.getFloat(x, y - 1, z + 1)) / 2 - (img.getFloat(x, y + 1,
								z - 1) - img.getFloat(x, y - 1, z - 1)) / 2) / 2;

		// FIXME: get Stephan to remind me why this is needed...
		for (int i = 0; i < 3; i++)
			for (int j = 0; j < 3; j++)
				hessianMatrix[i][j] *= (sigma * sigma);

		return hessianMatrix;
	}

	/**
	 * This method creates a gaussian kernel
	 *
	 * @param sigma
	 *            Standard Derivation of the gaussian function
	 * @param normalize
	 *            Normalize integral of gaussian function to 1 or not...
	 * @return float[] The gaussian kernel
	 *
	 *         author Stephan Saalfeld
	 */
	private static float[] createGaussianKernel1D(float sigma, boolean normalize) {
		float[] gaussianKernel;

		if (sigma <= 0) {

			gaussianKernel = new float[3];
			gaussianKernel[1] = 1;

		} else {

			int size = Math.max(3, (2 * (int) (3 * sigma + 0.5) + 1));

			float two_sq_sigma = 2 * sigma * sigma;
			gaussianKernel = new float[size];

			for (int x = size / 2; x >= 0; --x) {

				float val = (float) Math.exp(-(float) (x * x) / two_sq_sigma);

				gaussianKernel[size / 2 - x] = val;
				gaussianKernel[size / 2 + x] = val;
			}
		}

		if (normalize) {
			float sum = 0;

			for (float element : gaussianKernel)
				sum += element;

			/*
			 * for (float value : gaussianKernel)
			 * sum += value;
			 */

			for (int i = 0; i < gaussianKernel.length; i++)
				gaussianKernel[i] /= sum;
		}

		return gaussianKernel;
	}

}
