package pipeline.misc_util.drag_and_drop;

import java.io.File;
import java.io.Serializable;
import java.util.Objects;

import pipeline.misc_util.FileNameUtils;
import pipeline.parameters.DropProcessor;

public class DropProcessorKeepDirectory implements DropProcessor, Serializable {

	private static final long serialVersionUID = 1162036822913013776L;

	@Override
	public Object process(Object o) {
		String s = (String) o;
		Objects.requireNonNull(s);
		if (new File(s).isDirectory())
			return FileNameUtils.compactPath(s);
		s = FileNameUtils.compactPath(s);
		int lastIndex = s.lastIndexOf("/");
		if (lastIndex == -1)
			return null;
		return s.substring(0, lastIndex);
	}
}
