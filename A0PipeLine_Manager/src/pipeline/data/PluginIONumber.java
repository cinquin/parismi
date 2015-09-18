package pipeline.data;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

public class PluginIONumber extends PluginIO implements Serializable {

	private static final long serialVersionUID = 8990718186623267453L;

	public PluginIONumber(@NonNull String name) {
		super(name);
	}

	@Override
	public @NonNull File asFile(@Nullable File saveTo, boolean useBigTIFF) throws IOException, InterruptedException {
		throw new RuntimeException("Unimplemented");
	}

	public Number number;
	
	@Override
	public boolean defaultToNoSaving() {
		return true;
	}
	
}
