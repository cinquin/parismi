/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.parameters;

import pipeline.A0PipeLine_Manager.TableSelectionDemo;

public class PipelineParameter extends TextParameter {

	private static final long serialVersionUID = -8407408547269107115L;
	private transient TableSelectionDemo pipeline;

	public TableSelectionDemo getPipeline() {
		return pipeline;
	}

	public void setPipeline(TableSelectionDemo pipeline) {
		this.pipeline = pipeline;
	}

	public PipelineParameter(String name, String explanation, String initial_value, boolean editable,
			ParameterListener listener) {
		super(name, explanation, initial_value, editable, listener, null);
	}

}
