/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.data;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;

import pipeline.GUI_utils.bean_table.DoNotShowInTable;
import pipeline.GUI_utils.bean_table.MethodToGetColumnNames;
import pipeline.misc_util.Utils;

public class Quantifiable extends QuantifiableNames implements IQuantifiable {

	protected void copyInto(Quantifiable q2) {
		q2.setQuantifiedPropertyNames(new ArrayList<String>());
		q2.quantifiedProperties = new ArrayList<>();
		q2.getQuantifiedPropertyNames().addAll(getQuantifiedPropertyNames());
		q2.quantifiedProperties.addAll(quantifiedProperties.stream().map(Float::new).collect(Collectors.toList()));
	}

	private @NonNull List<Float> quantifiedProperties = new ArrayList<>();

	/**
	 * 
	 * @param name
	 * @param value
	 * @return true if property already existed
	 */
	@Override
	public boolean setQuantifiedProperty(String name, float value) {
		int index = getQuantifiedPropertyNames().indexOf(name);
		boolean alreadyExists = (index > -1);
		if (!alreadyExists) {
			getQuantifiedPropertyNames().add(name);
			quantifiedProperties.add(value);
		} else
			quantifiedProperties.set(index, value);
		return alreadyExists;
	}

	@Override
	@DoNotShowInTable
	public float getQuantifiedProperty(String name) {
		int index = getQuantifiedPropertyNames().indexOf(name);
		if (index < 0)
			throw new IllegalArgumentException("Property " + name + " does not exist in list "
					+ Utils.printStringArray(getQuantifiedPropertyNames()));
		return quantifiedProperties.get(index);
	}

	@Override
	@MethodToGetColumnNames(value = "getQuantifiedPropertyNames")
	public List<Float> getQuantifiedProperties() {
		return quantifiedProperties;
	}

	@Override
	public void setQuantifiedProperties(List<Float> qp) {
		quantifiedProperties = qp;
	}

}
