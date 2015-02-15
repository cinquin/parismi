package pipeline.misc_util;

public class PluginRuntimeException extends RuntimeException {

	private static final long serialVersionUID = 2322046583890237646L;

	private boolean displayUserDialog;

	public boolean unmaskable;

	public PluginRuntimeException(String message, boolean displayUserDialog) {
		super(message);
		this.displayUserDialog = displayUserDialog;
	}

	public PluginRuntimeException(String message, Throwable cause, boolean displayUserDialog) {
		super(message, cause);
		this.displayUserDialog = displayUserDialog;
	}

	public PluginRuntimeException(Throwable cause, boolean displayUserDialog) {
		super(cause);
		this.displayUserDialog = displayUserDialog;
	}

	public boolean getDisplayUserDialog() {
		return displayUserDialog;
	}
}
