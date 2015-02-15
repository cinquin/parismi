package processing_utilities.pcurves.Debug;

import java.awt.Event;

final public class DebugEvent extends Event {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public final static int STARTED_DEBUG_THREAD = 10000;
	public final static int FINISHED_DEBUG_THREAD = 10001;
	public final static int ITERATE_DEBUG_THREAD = 10002;
	public final static int RESET_DEBUG_THREAD = 10003;

	public DebugEvent(Object target, int id, Object arg) {
		super(target, id, arg);
	}
}
