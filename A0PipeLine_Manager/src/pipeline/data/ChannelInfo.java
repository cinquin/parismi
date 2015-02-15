/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.data;

import java.util.ArrayList;
import java.util.List;

public class ChannelInfo {
	private String originalFilePath;
	private List<Integer> excitationWavelengths = new ArrayList<>();
	private List<Integer> detectionRanges = new ArrayList<>();

	public String getOriginalFilePath() {
		return originalFilePath;
	}

	public void setOriginalFilePath(String originalFilePath) {
		this.originalFilePath = originalFilePath;
	}

	public List<Integer> getExcitationWavelengths() {
		return excitationWavelengths;
	}

	public void setExcitationWavelengths(List<Integer> excitationWavelengths) {
		this.excitationWavelengths = excitationWavelengths;
	}

	public List<Integer> getDetectionRanges() {
		return detectionRanges;
	}

	public void setDetectionRanges(List<Integer> detectionRanges) {
		this.detectionRanges = detectionRanges;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("originalFile=").append(originalFilePath).append("\n");
		int index = 0;
		for (int i : getDetectionRanges()) {
			result.append("detection").append(index).append("=").append(i).append("\n");
			index++;
		}
		return result.toString();
	}
}
