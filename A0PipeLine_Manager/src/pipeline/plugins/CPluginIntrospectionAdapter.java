/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins;

import pipeline.misc_util.IntrospectionParameters;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.ParameterListener;

public abstract class CPluginIntrospectionAdapter extends ExternalCallToLibrary {
	@Override
	public ParameterListener[] getParameterListeners() {
		return new ParameterListener[] { getParameterListenersAsSplit() };
	}

	@Override
	public void setParameters(AbstractParameter[] params) {
		IntrospectionParameters.setParameters(this, params);
		initializeParams();
	}

	protected CPluginIntrospectionAdapter() {
		initializeParams();
	}

	protected void initializeParams() {
		param1 = getParametersAsSplit();
	}
}
