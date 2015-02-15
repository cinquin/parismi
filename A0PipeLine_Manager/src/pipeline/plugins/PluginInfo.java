package pipeline.plugins;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface PluginInfo {
	boolean displayToUser() default true;

	int displayToExpertLevel() default 0;

	public int DEBUG = 3;

	boolean obsolete() default false;

	String suggestedReplacement() default "";
}
