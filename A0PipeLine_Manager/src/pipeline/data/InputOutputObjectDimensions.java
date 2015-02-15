/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.data;

public class InputOutputObjectDimensions {

	public InputOutputObjectDimensions(int width, int height, int depth, int nChannels, int nTimePoints) {
		this.width = width;
		this.height = height;
		this.depth = depth;
		this.nChannels = nChannels;
		this.nTimePoints = nTimePoints;
	}

	public enum dimensionType {
		x, y, z, t
	}

	public int width, height, depth, nChannels, nTimePoints;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + depth;
		result = prime * result + height;
		result = prime * result + nChannels;
		result = prime * result + nTimePoints;
		result = prime * result + width;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		InputOutputObjectDimensions other = (InputOutputObjectDimensions) obj;
		if (depth != other.depth)
			return false;
		if (height != other.height)
			return false;
		if (nChannels != other.nChannels)
			return false;
		if (nTimePoints != other.nTimePoints)
			return false;
		if (width != other.width)
			return false;
		return true;
	}
}
