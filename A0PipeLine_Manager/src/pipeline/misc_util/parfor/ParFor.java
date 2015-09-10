/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.misc_util.parfor;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import pipeline.misc_util.PluginRuntimeException;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.plugins.BasePipelinePlugin;

public class ParFor {
	private int startIndex, endIndex;
	private int nIterations;
	private ExecutorService threadPool;

	private ProgressReporter progressReporter;
	private boolean stopAllUponException;

	private String name = "";

	public ParFor(int startIndex, int endIndex, ProgressReporter progressBar, ExecutorService threadPool,
			boolean stopAllUponException) {
		this(startIndex, endIndex, progressBar, threadPool, stopAllUponException, Integer.MAX_VALUE);
	}

	public ParFor(int startIndex, int endIndex, ProgressReporter progressBar, ExecutorService threadPool,
			boolean stopAllUponException, int maxNThreads) {
		this.startIndex = startIndex;
		this.endIndex = endIndex;
		nIterations = endIndex - startIndex + 1;
		this.threadPool = threadPool;
		this.progressReporter = progressBar;
		this.stopAllUponException = stopAllUponException;

		int useNThreads = (int) (nProc * 1.5);
		int activeThreads = ((ThreadPoolExecutor) threadPool).getActiveCount();
		if (activeThreads > nProc)
			useNThreads = 2;
		int result = Math.min(Math.min(nIterations, useNThreads), maxNThreads);
		if ((workers == null) || (workers.length != result))
			workers = new ILoopWorker[result];
	}

	public ParFor(String name, int startIndex, int endIndex, ProgressReporter progressBar, boolean stopAllUponException) {
		this(startIndex, endIndex, progressBar, BasePipelinePlugin.threadPool, stopAllUponException);
		if (name != null)
			setName(name);
	}

	private static int nProc = Runtime.getRuntime().availableProcessors();
	private ILoopWorker[] workers = null;
	private int workerArrayIndex = 0;

	public int getNThreads() {
		return workers.length;
	}

	private void checkNotStarted() {
		if (started)
			throw new IllegalStateException("Illegal operation when ParFor already started");
	}

	public void setNThreads(int n) {
		checkNotStarted();
		workers = new ILoopWorker[n];
	}

	public void addLoopWorker(ILoopWorker task) {
		checkNotStarted();
		workers[workerArrayIndex] = task;
		workerArrayIndex++;
	}

	private transient final AtomicInteger index = new AtomicInteger();

	private static final int increment = 1;

	private List<?>[] partialResults;
	private Future<?>[] futures;

	private volatile boolean abort;
	private volatile Exception e;
	private volatile boolean clientWillGetExceptions;
	private volatile boolean printedMissingExceptionWarning;

	private volatile boolean started = false;
	private Object doneSemaphore = new Object();
	private volatile boolean done = false;

	public List<Object> run(boolean block) throws InterruptedException {
		return run(block, -1);
	}

	public List<Object> runNonBlocking() {
		try {
			return run(false, -1);
		} catch (InterruptedException e) {
			throw new IllegalStateException(e);
		}
	}

	volatile Thread parentThread;

	@SuppressWarnings({ "rawtypes" })
	public List<Object> run(boolean block, final int nToCompleteBeforeReturn) throws InterruptedException {

		try {
			checkNotStarted();
			started = false;

			parentThread = Thread.currentThread();

			clientWillGetExceptions = block || (nToCompleteBeforeReturn != -1);
			printedMissingExceptionWarning = false;

			partialResults = new ArrayList<?>[workers.length];
			for (int i = 0; i < partialResults.length; i++) {
				partialResults[i] = new ArrayList<Object>();
			}
			Runnable[] runnables = new Runnable[workers.length];
			futures = new Future[workers.length];
			abort = false;
			e = null;
			final float progressMultiplyingFactor = 100.0f / nIterations;
			final Thread parentThread = Thread.currentThread();
			final AtomicInteger nCompleted = new AtomicInteger(0);
			index.set(startIndex);

			for (int i = 0; i < workers.length; i++) {
				final int finalI = i;
				runnables[i] = new Runnable() {

					private long lastGUIUpdate = 0;
					private int modulo = 1;

					private final int workerID = finalI;
					final ProgressReporter localProgress = progressReporter;

					@Override
					public final void run() {
						try {

							Thread.currentThread().setName(name + workerID);

							final ILoopWorker localWorker = workers[workerID] == null ? workers[0] : workers[workerID];
							@SuppressWarnings("unchecked")
							final List<Object> localResults = (List<Object>) partialResults[workerID];

							int sliceModulo = 0;

							for (int n = index.getAndAdd(increment); n <= endIndex; n = index.getAndAdd(increment)) {
								if (abort)
									return;
								// Thread.yield();
								Object result = localWorker.run(n, workerID);
								if (result != null)
									localResults.add(result);

								if ((localProgress != null) && (sliceModulo++ == modulo)) {
									long currentTime = System.currentTimeMillis();
									if (lastGUIUpdate > 0) {
										long timeLag = currentTime - lastGUIUpdate;
										if (Math.abs(timeLag) > 3000) {
											modulo = Math.max((int) (modulo * 0.7), 1);
										} else if (Math.abs(timeLag) < 500) {
											modulo = Math.min(Integer.MAX_VALUE / 2, (int) (modulo * 1.3));
										}
									}
									lastGUIUpdate = currentTime;
									sliceModulo = 0;
									int ourProgress = (int) ((n - startIndex) * progressMultiplyingFactor);
									if (ourProgress > localProgress.getValue())
										localProgress.setValueThreadSafe(ourProgress);
									// not perfect but minimizes synchronization
								}
							}
						} catch (Exception e1) {
							if (!clientWillGetExceptions) {
								Utils.log(name + "Exception will probably not propagate up to ParFor owner",
										LogLevel.INFO);
								Utils.printStack(e1);
							}

							if (stopAllUponException && !abort) {
								abort = true;
								synchronized (ParFor.this) {
									if (e == null) {
										e = e1;
										synchronized (doneSemaphore) {
											doneSemaphore.notifyAll();
										}
									} else if (!printedMissingExceptionWarning) {
										printedMissingExceptionWarning = true;
										Utils.log(name
												+ "Multiple exceptions caught in ParFor(1); only 1 will be rethrown",
												LogLevel.ERROR);
									}
								}
								Utils.log(name + "Aborting ParFor run", LogLevel.INFO);
								if (clientWillGetExceptions && parentThread != null)
									parentThread.interrupt();
							}
						} finally {
							if (nToCompleteBeforeReturn != -1) {
								int completed = nCompleted.incrementAndGet();
								if (completed == nToCompleteBeforeReturn) {
									synchronized (nCompleted) {
										nCompleted.notifyAll();
									}
								}
							}
							Thread.currentThread().setName("");
						}
					}
				};
				futures[i] = threadPool.submit(runnables[i], 0);
			}

			if (!block && nToCompleteBeforeReturn == -1) {
				return null;
			}

			if (nToCompleteBeforeReturn != -1) {
				synchronized (nCompleted) {
					while (nCompleted.get() < nToCompleteBeforeReturn && (!abort)) {
						try {
							nCompleted.wait();
						} catch (InterruptedException e1) {
							if (!abort) {
								abort = true;
								synchronized (ParFor.this) {
									if (e == null) {
										e = e1;
										synchronized (doneSemaphore) {
											doneSemaphore.notifyAll();
										}
									}
								}
								Utils.log(name + "Aborting ParFor run", LogLevel.INFO);
								for (Future future : futures) {
									future.cancel(true);
								}
							}
						}
					}
				}
			} else {
				getAllFutures();
			}
			done = true;

			if (!abort)
				clientWillGetExceptions = false;
			if (e != null) {
				if (e instanceof InterruptedException)
					throw ((InterruptedException) e);
				UnsafeHolder.unsafe.throwException(e);
				throw new IllegalStateException("Should have thrown an exception already");
			}

			if (block) {
				List<Object> aggregatedResults = new ArrayList<>();
				for (int i = 0; i < futures.length; i++) {
					aggregatedResults.addAll(partialResults[i]);
				}
				return aggregatedResults;
			} else {
				return null;
			}
		} finally {
			if (!abort)
				clientWillGetExceptions = false;
			parentThread = null;
			synchronized (doneSemaphore) {
				doneSemaphore.notifyAll();
			}
		}
	}

	private void getAllFutures() {
		for (int i = 0; i < futures.length && (!abort); i++) {
			try {
				futures[i].get();
			} catch (InterruptedException | ExecutionException e1) {
				if (!abort) {
					abort = true;
					synchronized (ParFor.this) {
						if (e == null) {
							e = e1;
							synchronized (doneSemaphore) {
								doneSemaphore.notifyAll();
							}
						} else
							Utils.log(name + "Multiple exceptions caught in ParFor(2); only 1 will be rethrown",
									LogLevel.ERROR);
					}
					Utils.log(name + "Aborting ParFor run", LogLevel.INFO);
				}
				for (Future<?> future : futures) {
					future.cancel(true);
				}
			}
		}
	}

	public void waitForCompletion() throws InterruptedException {
		clientWillGetExceptions = true;
		try {
			if (!done) {
				getAllFutures();
			}
		} finally {
			if (abort) {
				synchronized (doneSemaphore) {
					while (e == null) {
						doneSemaphore.wait();
					}
				}
			}
			if (e != null) {
				if (e instanceof InterruptedException)
					throw ((InterruptedException) e);
				PluginRuntimeException repackaged = new PluginRuntimeException(e.getMessage(), e, false);
				repackaged.unmaskable = true;
				throw repackaged;
				// UnsafeHolder.unsafe.throwException(e);
				// throw new IllegalStateException();
			}
		}
	}

	// From http://robaustin.wikidot.com/rethrow-exceptions
	public static class UnsafeHolder {
		public static final sun.misc.Unsafe unsafe;

		static {
			try {
				Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
				field.setAccessible(true);
				unsafe = (sun.misc.Unsafe) field.get(null);
			} catch (Exception e) {
				throw new AssertionError(e);
			}
		}
	}

	public void interrupt() {
		for (Future<?> future : futures) {
			if (future != null)
				future.cancel(true);
		}
	}

	public void setName(String name) {
		this.name = name + ": ";
	}

}
