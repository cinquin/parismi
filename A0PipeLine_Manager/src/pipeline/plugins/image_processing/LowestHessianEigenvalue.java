/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
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

package pipeline.plugins.image_processing;

import java.util.concurrent.atomic.AtomicInteger;

import pipeline.PreviewType;
import pipeline.data.IPluginIOStack;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.AbstractParameter;
import pipeline.plugins.ThreeDPlugin;
import processing_utilities.curvature.math3d.JacobiFloat;

// For some reason this plugin does not appear to be thread-safe; is that because of JacobiFloat?
public class LowestHessianEigenvalue extends ThreeDPlugin {
	@Override
	public String operationName() {
		return "Lowest Hessian Eigenvalue";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public int getFlags() {
		return SAME_AS_FLOAT + ONLY_FLOAT_INPUT + ONE_OUTPUT_CHANNEL_PER_INPUT_CHANNEL + DONT_PARALLELIZE;
	}

	private float sigma = 15.0f;

	private AtomicInteger thread_index = new AtomicInteger(0);

	@Override
	public void runChannel(final IPluginIOStack input, final IPluginIOStack output, final ProgressReporter p,
			final PreviewType previewType, boolean inputHasChanged) throws InterruptedException {

		Utils.log("blurrinxxg " + sigma, LogLevel.DEBUG);

		final float sepX = 1, sepY = 1, sepZ = 1;
		/*
		 * if( useCalibration && (calibration!=null) ) {
		 * sepX = (float)calibration.pixelWidth;
		 * sepY = (float)calibration.pixelHeight;
		 * sepZ = (float)calibration.pixelDepth;
		 * }
		 */
		nCpus = Runtime.getRuntime().availableProcessors();
		if (input.getDepth() < nCpus)
			nCpus = input.getDepth();
		threads = new Thread[nCpus];

		input.computePixelArray();
		output.computePixelArray();

		progressSetValueThreadSafe(p, 0);
		progressSetIndeterminateThreadSafe(p, false);

		for (int z = 0; z < input.getDepth(); z++)
			for (int y = 0; y < input.getHeight(); y++)
				for (int x = 0; x < input.getWidth(); x++)
					((float[]) output.getStackPixelArray()[z])[x + y * output.getWidth()] = 0;

		// OneDKernel=createGaussianKernel1D(sigma, true);

		for (int ithread = 0; ithread < nCpus; ithread++) {
			threads[ithread] = new Thread("Directional blurring 1") {
				@Override
				public void run() {
					// final int localThreadIndex=thread_index.getAndIncrement();
					for (int z = slice_registry.getAndIncrement(); z < input.getDepth() - 1; z =
							slice_registry.getAndIncrement()) {
						for (int y = 1; y < input.getHeight() - 1; y++) {
							for (int x = 1; x < input.getWidth() - 1; x++) {
								JacobiFloat jacobi =
										new JacobiFloat(computeHessianMatrix3DFloat(input, x, y, z, sigma, sepX, sepY,
												sepZ));
								// float[][] eigenVectors=jacobi.getEigenVectors();
								float[] eigenValues = jacobi.getEigenValues();

								// int minIndex=Utils.indexOfMin(Utils.arrayAbs(eigenValues));
								int minIndex = Utils.indexOfMin(eigenValues);


								((float[]) output.getStackPixelArray()[z])[x + y * output.getWidth()] =
										(eigenValues[minIndex]);

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
	 *
	 * author Stephan Saalfeld
	 */
	/*
	 * private static final float[] createGaussianKernel1D(float sigma, boolean normalize)
	 * {
	 * float[] gaussianKernel;
	 * 
	 * if (sigma <= 0) {
	 * 
	 * gaussianKernel = new float[3];
	 * gaussianKernel[1] = 1;
	 * 
	 * } else {
	 * 
	 * int size = Math.max(3, (int)(2*(int)(3*sigma + 0.5)+1));
	 * 
	 * float two_sq_sigma = 2*sigma*sigma;
	 * gaussianKernel = new float[size];
	 * 
	 * for (int x = size/2; x >= 0; --x) {
	 * 
	 * float val = (float)Math.exp(-(float)(x*x)/two_sq_sigma);
	 * 
	 * gaussianKernel[size/2-x] = val;
	 * gaussianKernel[size/2+x] = val;
	 * }
	 * }
	 * 
	 * if (normalize)
	 * {
	 * float sum = 0;
	 * 
	 * for (int i = 0; i < gaussianKernel.length; i++)
	 * sum += gaussianKernel[i];
	 * 
	 * //for (float value : gaussianKernel)
	 * //sum += value;
	 * 
	 * for (int i = 0; i < gaussianKernel.length; i++)
	 * gaussianKernel[i] /= sum;
	 * }
	 * 
	 * 
	 * return gaussianKernel;
	 * }
	 */

	@Override
	public void setParameters(AbstractParameter[] param) {

	}

}
