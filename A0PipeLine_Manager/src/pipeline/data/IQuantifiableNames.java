/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.data;

import java.util.List;

public interface IQuantifiableNames {
	List<String> getQuantifiedPropertyNames();

	boolean hasQuantifiedProperty(String name);

	boolean addQuantifiedPropertyName(String name);

	void setQuantifiedPropertyNames(List<String> desc);
}
