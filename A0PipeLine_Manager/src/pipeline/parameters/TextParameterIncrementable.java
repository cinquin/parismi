package pipeline.parameters;

import pipeline.FileNameIncrementable;
import pipeline.misc_util.FileNameUtils;

public class TextParameterIncrementable extends TextParameter implements FileNameIncrementable {

	private static final long serialVersionUID = -7556188714472425122L;

	public TextParameterIncrementable(String name, String explanation, String initial_value, boolean editable,
			ParameterListener listener, Object creatorReference) {
		super(name, explanation, initial_value, editable, listener, creatorReference);
	}

	@Override
	public void incrementFileName() {
		value = FileNameUtils.incrementName(value);
		fireValueChanged(false, true, true);
	}

	@Override
	public void prefixFileName(String prefix) {
		value = prefix + value;
		fireValueChanged(false, true, false);
	}
}
