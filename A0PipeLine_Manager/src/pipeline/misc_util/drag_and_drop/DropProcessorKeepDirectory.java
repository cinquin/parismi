package pipeline.misc_util.drag_and_drop;

import java.io.File;

import pipeline.misc_util.FileNameUtils;
import pipeline.parameters.DropProcessor;

public class DropProcessorKeepDirectory implements DropProcessor {
	@Override
	public Object process(Object o) {
		String s = (String) o;
		if (new File(s).isDirectory())
			return FileNameUtils.compactPath(s);
		s = FileNameUtils.compactPath(s);
		int lastIndex = s.lastIndexOf("/");
		if (lastIndex == -1)
			return null;
		return s.substring(0, lastIndex);
	}
}
