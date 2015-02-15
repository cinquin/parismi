package pipeline.misc_util.drag_and_drop;

import java.io.File;

import pipeline.misc_util.FileNameUtils;
import pipeline.parameters.DropProcessor;

public class DropProcessorKeepFileName implements DropProcessor {
	@Override
	public Object process(Object o) {
		String s = (String) o;
		if (new File(s).isDirectory())
			return null;
		s = FileNameUtils.compactPath(s);
		int lastIndex = s.lastIndexOf("/");
		if (lastIndex == -1)
			return o;
		return s.substring(lastIndex + 1, s.length());
	}
}
