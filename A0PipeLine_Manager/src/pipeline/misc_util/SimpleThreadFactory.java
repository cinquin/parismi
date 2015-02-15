package pipeline.misc_util;

import java.util.concurrent.ThreadFactory;

public class SimpleThreadFactory implements ThreadFactory {
	int priority;

	public SimpleThreadFactory(int priority) {
		this.priority = priority;
	}

	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(r);
		t.setPriority(priority);
		return t;
	}
}
