package pipeline.parameters;

import java.io.Serializable;

public interface DropProcessor extends Serializable {
	Object process(Object o);
}
