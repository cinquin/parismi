package pipeline.misc_util.drag_and_drop;

import pipeline.parameters.DropProcessor;

public class DropProcessorIgnore implements DropProcessor {
	@Override
	public Object process(Object o) {
		return null;
	}
}
