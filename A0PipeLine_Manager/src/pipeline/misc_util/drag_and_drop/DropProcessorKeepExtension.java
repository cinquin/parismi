package pipeline.misc_util.drag_and_drop;

import java.io.Serializable;

import pipeline.parameters.DropProcessor;

public class DropProcessorKeepExtension implements DropProcessor, Serializable {
	private static final long serialVersionUID = 7324664680484851906L;

	@Override
	public Object process(Object o) {
		String s = (String) o;
		int lastIndex = s.lastIndexOf(".");
		if (lastIndex == -1)
			return null;
		return s.substring(lastIndex, s.length());
	}

}
