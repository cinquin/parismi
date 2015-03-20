/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.data;

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;

public interface IQuantifiable extends IQuantifiableNames {
	@NonNull List<Float> getQuantifiedProperties();

	void setQuantifiedProperties(@NonNull List<Float> qp);

	float getQuantifiedProperty(String name);

	boolean setQuantifiedProperty(String name, float value);
}
