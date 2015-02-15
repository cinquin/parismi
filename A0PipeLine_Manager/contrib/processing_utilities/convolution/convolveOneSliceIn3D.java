// Code from fiji, modified by multithreading
/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package processing_utilities.convolution;

import pipeline.data.IPluginIOStack;
import pipeline.misc_util.PluginRuntimeException;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;

public class convolveOneSliceIn3D implements convolver3D {

	private static final boolean do_only_float = true;

	@Override
	public final void convolveY(IPluginIOStack image, Object destination, int slice, float[] H_y) {
		float[][][] H = new float[1][H_y.length][1];
		for (int i = 0; i < H_y.length; i++) {
			H[0][i][0] = H_y[i];
		}
		convolve(image, destination, slice, H);
	}

	@Override
	public final void convolveZ(IPluginIOStack image, Object destination, int slice, float[] H_z) {
		float[][][] H = new float[H_z.length][1][1];
		for (int i = 0; i < H_z.length; i++) {
			H[i][0][0] = H_z[i];
		}
		convolve(image, destination, slice, H);
	}

	@Override
	public final void convolveX(IPluginIOStack image, Object destination, int slice, float[] H_x) {
		float[][][] H = new float[1][1][H_x.length];
		for (int i = 0; i < H_x.length; i++) {
			H[0][0][i] = H_x[i];
		}
		convolve(image, destination, slice, H);
	}

	private int w, h, d;
	private int r_x, r_y, r_z;
	private boolean isByte, isShort, isFloat;
	private float[][][] H;
	private Object[] pixels_in;
	private float[][] float_pixels_in;

	@SuppressWarnings("unused")
	private void convolve(IPluginIOStack source, Object destination, int slice, float[][][] kernel) {
		// numberOfChannels=1;
		H = kernel;
		// Determine dimensions of the filter
		r_z = H.length;
		r_y = H[0].length;
		r_x = H[0][0].length;

		half_r_z = r_z / 2;
		half_r_x = r_x / 2;
		half_r_y = r_y / 2;

		// Determine dimensions of the image
		w = source.getWidth();
		h = source.getHeight();
		d = source.getDepth();

		// Utils.log("width="+w+", height="+h+", depth="+d,LogLevel.VERBOSE_VERBOSE_DEBUG);
		// Utils.log("radii: "+r_x+" "+r_y+" "+r_z,LogLevel.VERBOSE_VERBOSE_DEBUG);

		// determine image type
		if (false) {
			isByte = source.getPixels(0) instanceof byte[];
			isShort = source.getPixels(0) instanceof short[];
			isFloat = source.getPixels(0) instanceof float[];
		} else
			isFloat = true;

		// initialize slices_in and slices_out
		if (isFloat)
			float_pixels_in = new float[d][];
		else
			pixels_in = new Object[d];

		pixels_in = new Object[d];
		for (int i = 0; i < d; i++) {
			if (do_only_float)
				float_pixels_in[i] = (float[]) source.getPixels(i);
			else
				pixels_in[i] = source.getPixels(i);
		}

		if (destination == null) {
			Utils.log("Null destination in 3D convolve", LogLevel.ERROR);
		}
		if (!(destination instanceof float[])) {
			Utils.log("not float array but instead ", LogLevel.DEBUG);
			if (destination == null) {
				Utils.log("null pixels", LogLevel.DEBUG);
			}
			isByte = destination instanceof byte[];
			isShort = destination instanceof short[];
			if (isShort) {
				Utils.log("short processor", LogLevel.DEBUG);
			}
			if (isByte) {
				Utils.log("byte processor", LogLevel.DEBUG);
			}
		}
		float[] destinationPixels = (float[]) destination;
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				destinationPixels[y * w + x] = convolvePoint(y, x, slice - 1);
			}
		}
	}

	int half_r_z;
	int half_r_x;
	int half_r_y;

	private float convolvePoint(int y, int x, int z) {
		float sum = 0f;

		int z0 = Math.min(z, half_r_z);
		int z1 = Math.min(d - 1 - z, half_r_z);

		int y0 = Math.min(y, half_r_y);
		int y1 = Math.min(h - 1 - (y), half_r_y);

		int x0 = Math.min(x, half_r_x);
		int x1 = Math.min(w - 1 - x, half_r_x);

		// if (y1-y0>10) Utils.log(z0+"-"+z1+", "+y0+"-" +y1+", "+x0+"-"+x1,LogLevel.VERBOSE_VERBOSE_DEBUG);

		if (do_only_float) {
			for (int k = -z0; k <= z1; k++) {
				for (int j = -y0; j <= y1; j++) {
					for (int i = -x0; i <= x1; i++) {
						sum +=
								float_pixels_in[k + z][x + i + (y + j) * w]
										* H[k + half_r_z][j + half_r_y][i + half_r_x];
					}
				}
			}
		} else {
			for (int k = -half_r_z; k <= +half_r_z; k++) {
				for (int j = -half_r_y; j <= +half_r_y; j++) {
					for (int i = -half_r_x; i <= +half_r_x; i++) {
						sum += getValue(x + i, y + j, z + k) * H[k + half_r_z][j + half_r_y][i + half_r_x];
					}
				}
			}
		}
		return sum;
	}

	private float getValue(int x, int y, int z) {
		if (x < 0)
			return 0f;
		if (x > w - 1)
			return 0f;
		if (y < 0)
			return 0f;
		if (y > h - 1)
			return 0f;
		if (z < 0)
			return 0f;
		if (z > d - 1)
			return 0f;
		int index = y * w + x;
		if (isByte)
			return ((byte[]) pixels_in[z])[index] & 0xff;
		else if (isShort)
			return ((short[]) pixels_in[z])[index];
		else if (isFloat)
			return ((float[]) pixels_in[z])[index];
		throw new PluginRuntimeException("Neither short nor byte nor float image", true);
	}

}
