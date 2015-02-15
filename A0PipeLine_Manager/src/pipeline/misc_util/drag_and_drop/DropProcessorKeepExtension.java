package pipeline.misc_util.drag_and_drop;

import pipeline.parameters.DropProcessor;

public class DropProcessorKeepExtension implements DropProcessor {
	@Override
	public Object process(Object o) {
		String s = (String) o;
		int lastIndex = s.lastIndexOf(".");
		if (lastIndex == -1)
			return null;
		return s.substring(lastIndex, s.length());
	}

}
