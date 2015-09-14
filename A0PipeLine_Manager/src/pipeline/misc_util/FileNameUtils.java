/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.misc_util;

import ij.IJ;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import pipeline.misc_util.Utils.LogLevel;

/**
 * Utilities to increment file names and translate names with brace tags (that mark numbers to be incremented within a
 * string) to real
 * file names that can be used to open files.
 *
 */
public class FileNameUtils {

	public static String incrementName(String input0) {

		int openingBrace = input0.indexOf('{');
		if ((openingBrace < 0) || (Utils.isParsableToInt(input0)) || (input0.indexOf("$") == 0)) { // nothing for us to
																									// do with this
																									// string:
			// it has no incrementable numbers, it is an absolute reference, or a relative reference to another row
			return input0;
		}
		int closingBrace = input0.indexOf('}');

		if (closingBrace < 0) {
			throw new RuntimeException("Missing closing brace in file name " + input0);
		}

		String s_numberToIncrement = input0.substring(openingBrace + 1, closingBrace);
		Utils.log("extracted number " + s_numberToIncrement, LogLevel.DEBUG);
		int numberToIncrement = Utils.parseAndThrowRuntimeException(s_numberToIncrement);

		String newName =
				input0.substring(0, openingBrace) + "{" + (numberToIncrement + 1) + "}"
						+ input0.substring(closingBrace + 1, input0.length());

		return newName;
	}

	public static List<@NonNull String> getPathExpansions(@NonNull String path, boolean sortByTime) {
		List<@NonNull String> result = new ArrayList<>();

		String command = "ls -d" + (sortByTime ? "t" : "") + " " + path.replace(" ", "\\ ");
		Process shellExec;
		try {
			shellExec = Runtime.getRuntime().exec(new String[] { "bash", "-c", command });

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(shellExec.getInputStream()))) {
				String expandedPath;
				while ((expandedPath = reader.readLine()) != null) {
					result.add(compactPath(expandedPath));
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	private static void runProcess(String command, List<String> result) throws IOException, InterruptedException {
		Process shellExec = Runtime.getRuntime().exec(new String[] { "bash", "-c", command });
		BufferedReader reader = new BufferedReader(new InputStreamReader(shellExec.getInputStream()));
		String line;
		shellExec.waitFor();
		if (shellExec.exitValue() != 0) {
			Utils.log("Process " + command + "returned with error " + shellExec.exitValue(), LogLevel.DEBUG);
			return;
		}
		while ((line = reader.readLine()) != null) {
			result.add(line);
		}
	}

	static Map<String, String> userHomeDirs = new HashMap<>();
	static {
		try {
			List<String> userNameCommands = new ArrayList<>();
			if (IJ.isMacOSX()) {
				userNameCommands.add("dscl . -ls /Users");
				userNameCommands.add("dscl localhost list /LDAPv3/analyzethis.bio.uci.edu/Users");
			} else {
				userNameCommands.add("getent passwd");
			}

			String currentUser = System.getProperty("user.name");
			Utils.log("Current user is " + currentUser, LogLevel.DEBUG);

			List<String> userNames = new ArrayList<String>();

			for (String command : userNameCommands) {
				try {
					runProcess(command, userNames);
				} catch (IOException | InterruptedException e) {
					throw new RuntimeException(e);
				}
			}

			for (String line : userNames) {
				String user;
				String path;
				if (IJ.isMacOSX()) {
					if (line.startsWith("_"))
						continue;
					user = line;
					path = expandPathUsingShell("~" + user);
				} else {
					String[] userEntry = line.split(":");
					user = userEntry[0];
					path = userEntry[5];
				}
				if (path.equals("") || path.equals("/") || (!path.contains("/")) || (path.length() < 5))
					continue;
				userHomeDirs.put("~" + user, path);
				if (user.equals(currentUser))
					userHomeDirs.put("~", path);
			}
			if (!userHomeDirs.containsKey("~"))
				throw new RuntimeException("Could not identify user home directory");
		} catch (Throwable e) {
			Utils.log("Error during FileNameUtils initialization", LogLevel.INFO);
			Utils.printStack(e, LogLevel.INFO);
		}
	}

	@SuppressWarnings("null")
	public static @NonNull String compactPath(@NonNull String path) {
		for (Entry<String, String> set : userHomeDirs.entrySet()) {
			if (path.startsWith(set.getValue())) {
				return path.replaceFirst(set.getValue(), set.getKey());
			}
		}
		return path;
	}

	private static final Pattern homeDirPattern = Pattern.compile("^~[^/]*");

	@SuppressWarnings("null")
	public static @NonNull String expandPath(@NonNull String path) {

		if (path.contains("*") || path.contains("?"))
			return expandPathUsingShell(path);

		Matcher matcher = homeDirPattern.matcher(path);
		if (matcher.find()) {
			String userName = matcher.group();
			// userName=userName.substring(0, userName.length()-1);
			if (!userHomeDirs.containsKey(userName)) {
				Utils.log("Unrecognized user " + userName, LogLevel.WARNING);
				return path;
			}
			path = path.replaceFirst(userName, userHomeDirs.get(userName));
		}

		return path;
	}

	// Adapted from http://stackoverflow.com/questions/7163364/how-to-handle-in-file-paths
	public static @NonNull String expandPathUsingShell(@NonNull String path) {
		try {
			String command = "ls -dt " + path.replace(" ", "\\ ");
			Process shellExec = Runtime.getRuntime().exec(new String[] { "bash", "-c", command });

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(shellExec.getInputStream()))) {
				String expandedPath = reader.readLine();

				// Only return a new value if expansion worked.
				// We're reading from stdin. If there was a problem, it was written
				// to stderr and our result will be null.
				if (expandedPath != null) {
					path = expandedPath;
				} else {

					// It's possible that the file doesn't exist, which is fine; in that case, redo this
					// in a way the shell expands even if the file doesn't exist

					command = "echo " + path.replace(" ", "\\ ");
					shellExec = Runtime.getRuntime().exec(new String[] { "bash", "-c", command });
					try (BufferedReader reader2 = new BufferedReader(new InputStreamReader(shellExec.getInputStream()))) {
						expandedPath = reader2.readLine();
					}

					if (expandedPath != null)
						path = expandedPath;
					else {
						BufferedReader stderr = new BufferedReader(new InputStreamReader(shellExec.getErrorStream()));
						Utils.log("Error expand file path", LogLevel.DEBUG);
						String line;
						while ((line = stderr.readLine()) != null) {
							Utils.log(line, LogLevel.DEBUG);
						}
					}
				}
			}
		} catch (java.io.IOException e) {
			// Just consider it unexpandable and return original path.
			Utils.printStack(e);
		}
		return path;
	}

	public static @NonNull String removeIncrementationMarks(@NonNull String input) {
		// remove "{" and "}" so the caller can open the file
		// A SIMPLER WAY OF DOING IT WOULD BE input.replaceAll("\\{|\\}", "");

		if (Utils.isParsableToInt(input) || input.indexOf("$") == 0) {
			return input;
		}

		int openingBrace = input.indexOf('{');

		/*
		 * if ((openingBrace<0)||(Utils.isParsableToInt(input))||(input.indexOf("$")==0)){ //nothing for us to do with
		 * this string:
		 * //it has no incrementable numbers, it is an absolute reference, or a relative reference to another row
		 * return input;
		 * }
		 */

		String newName = input;

		if (openingBrace > 0) {
			int closingBrace = input.indexOf('}');
			if (closingBrace < 0) {
				throw new RuntimeException("Missing closing brace in file name " + input);
			}
			newName =
					input.substring(0, openingBrace) + input.substring(openingBrace + 1, closingBrace)
							+ input.substring(closingBrace + 1, input.length());
		}

		if (newName.startsWith("~") || (newName.indexOf('*') > -1))
			newName = expandPath(newName);

		return newName;
	}

	@SuppressWarnings("null")
	public static @NonNull String compactPath(@NonNull File file) {
		return compactPath(file.getAbsolutePath());
	}

	public static @NonNull String getShortNameFromPath(String path, int maxLength) {
		int end = path.contains(".") ? path.lastIndexOf(".") : path.length();
		path = path.substring(0, end);
		if (path.length() > maxLength)
			return "..." + path.substring(path.length() - maxLength);
		else
			return path;
	}

	public static @Nullable File chooseFile(String message, int dialogType) {
		FileDialog dialog = new FileDialog(new Frame(), message, dialogType);
		dialog.setVisible(true);
		String filePath = dialog.getDirectory();
		if (filePath == null)
			return null;
		filePath += dialog.getFile();
		return new File(filePath);
	}
}
