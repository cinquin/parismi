package pipeline.misc_util.drag_and_drop;

import java.io.Serializable;

import pipeline.parameters.DropProcessor;

public class DropProcessorPassThrough implements DropProcessor, Serializable {

	private static final long serialVersionUID = -643922708855421451L;

	@Override
	public Object process(Object o) {
		return o;
	}

}
