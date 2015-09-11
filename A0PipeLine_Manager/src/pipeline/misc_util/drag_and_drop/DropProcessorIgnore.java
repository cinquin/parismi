package pipeline.misc_util.drag_and_drop;

import java.io.Serializable;

import pipeline.parameters.DropProcessor;

public class DropProcessorIgnore implements DropProcessor, Serializable {
	private static final long serialVersionUID = -5695125370107552237L;

	@Override
	public Object process(Object o) {
		return null;
	}
}
