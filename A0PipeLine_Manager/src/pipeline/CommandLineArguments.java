package pipeline;

import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;

class CommandLineArguments {

	@Parameter(names = "-logLevel", description = "1 for only critical messages, 8 for maximal verbosity",
			required = false)
	int logLevel = 2;

	@Parameter(names = "-suppressSaveTable",
			description = "-1 for default behavior (which is no saving for singleRun),"
					+ " 1 to force suppression, 0 to never suppress", required = false)
	int suppressSaveTable = -1;

	@Parameter(names = "-logFile", description = "Path to file to which to append log messages", required = false)
	String logFile = "pipeline_log.txt";

	@Parameter(description = "singleRun: single run on .xml file whose path follows\n"
			+ "batchRun: batch run on .xml file whose path follows\n" + "worms: simulation of mutation accumulation\n"
			+ "groovy: open Groovy console", required = true)
	List<String> action = new ArrayList<>();

	@Parameter(names = "-help", help = true)
	boolean help;
}
