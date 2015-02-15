package processing_utilities.convolution;

import pipeline.data.IPluginIOStack;

public interface convolver3D {

	public void convolveX(IPluginIOStack input, Object destination, int slice, float[] H_x);

	public void convolveY(IPluginIOStack image, Object destination, int slice, float[] H_x);

	public void convolveZ(IPluginIOStack image, Object destination, int slice, float[] H_x);
}
