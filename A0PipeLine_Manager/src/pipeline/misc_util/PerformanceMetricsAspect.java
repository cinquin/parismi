package pipeline.misc_util;

// Modified from http://wiki.aurionproject.org/display/AurionMain/Performance+Instrumentation+using+AOP+with+AspectJ

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import pipeline.misc_util.Utils.LogLevel;

@Aspect
public class PerformanceMetricsAspect {

	private static String entryMsgPrefix = "PerformanceMetricsAspect: entering method";
	private static String exitMsgPrefix = "PerformanceMetricsAspect: exiting method";

	@Around("within(pipeline..*) && execution( * *(..)) && !within(PerformanceMetricsAspect) && !within(pipeline.misc_util.Utils)")
	public static
			Object log(ProceedingJoinPoint call) throws Throwable {

		long startTime = System.currentTimeMillis();

		trace(true, call, null, 0);

		Object point = call.proceed();

		long endTime = System.currentTimeMillis();
		long executionTime = (endTime - startTime);
		if (executionTime > 200) {
			Utils.log("More than 500ms [ " + call.toShortString() + " executionTime : " + executionTime + "]",
					LogLevel.INFO);
		}

		trace(false, call, point, executionTime);

		return point;
	}

	public static void trace(boolean entry, ProceedingJoinPoint call, Object retVal, long time) {
		try {
			if (entry) {
				Utils.log(entryMsgPrefix + " [" + call.toShortString() + "] with param : {"
						+ (call.getArgs().length > 0 ? call.getArgs()[0] : "") + "}", LogLevel.DEBUG);
			} else {
				Utils.log(exitMsgPrefix + " [" + call.toShortString() + "with return as: {" + String.valueOf(retVal)
						+ "} [executionTime : " + time + "]", LogLevel.DEBUG);
			}

		} catch (Exception ignore) {
		}
	}

}
