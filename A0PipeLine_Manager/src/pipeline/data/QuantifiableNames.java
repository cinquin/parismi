/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.data;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;

public class QuantifiableNames implements IQuantifiableNames {
	private @NonNull List<String> namesOfQuantifiedProperties = new ArrayList<>();

	@Override
	public boolean hasQuantifiedProperty(String name) {
		int index = namesOfQuantifiedProperties.indexOf(name);
		return (index > -1);
	}

	@Override
	public List<String> getQuantifiedPropertyNames() {
		return namesOfQuantifiedProperties;
	}

	@Override
	public void setQuantifiedPropertyNames(List<String> desc) {
		namesOfQuantifiedProperties = desc;
	}

	@Override
	public boolean addQuantifiedPropertyName(String name) {
		throw new RuntimeException("Unimplemented");
	}

}
