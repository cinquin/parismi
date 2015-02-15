/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins;

import java.util.ArrayList;
import java.util.List;

import pipeline.PipelineCallback;
import pipeline.PreviewType;
import pipeline.data.IPluginIOStack;
import pipeline.external_plugin_interfaces.JNACallToNativeLibrary;
import pipeline.external_plugin_interfaces.LinkToExternalProgram;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.AbstractParameter;

public abstract class ExternalCall extends ThreeDPlugin implements AuxiliaryInputOutputPlugin {

	@Override
	public void cleanup() {
		if (link != null)
			link.terminate(false);
		input = null;
		output = null;
	}

	protected abstract String getCommandName();

	/**
	 * OBSOLETE. This is now reserved for future use.
	 * 
	 * @return "32" if a 32-bit TIFF (i.e. regular TIFF) should be created, or "64" if
	 *         a 64-bit TIFF (i.e. BigTIFF) should be created.
	 */
	@SuppressWarnings("static-method")
	protected String getTIFFType() {
		return "32";
	}

	public boolean onlyComputePreview = false;

	protected AbstractParameter param1 = null;
	protected AbstractParameter param2 = null;

	@Override
	public AbstractParameter[] getParameters() {
		AbstractParameter[] paramArray = { param1, param2 };
		return paramArray;
	}

	@Override
	public void setParameters(AbstractParameter[] param) {
		if (param == null)
			return;
		param1 = param[0];
		param2 = param[1];
	}

	protected String[] getExtraParams() {
		if ((param1 == null) && (param2 == null))
			return new String[] {};
		if (param1 == null)
			return param2.toString().split("\t");
		if (param2 == null)
			return param1.toString().split("\t");
		return Utils.concatenateArrays(param1.toString().split("\t"), param2.toString().split("\t"));
	}

	protected static void padWithDummyInputFileNames(List<String> args) {
		while (args.size() < 5) {
			args.add("0");
		}
	}

	static void padWithDummyOutputFileNames(List<String> args) {
		while (args.size() < 6) {
			args.add("0");
		}
	}

	@Override
	public String[] getInputLabels() {
		return new String[0];
	}

	@Override
	public String[] getOutputLabels() {
		return new String[0];
	}

	@SuppressWarnings("static-method")
	public String[] getExtraParamsForFirstRun() {
		return new String[0];
	}

	@SuppressWarnings("static-method")
	public String[] getExtraParamsForStdinLoop() {
		return new String[0];
	}

	@SuppressWarnings("static-method")
	public String[] inputsToHideFromExtraArgs() {
		return new String[0];
	}

	public abstract void loadExtraInputArgsForEstablishment(List<String> args);

	protected void extraArgsForRunLoop(List<String> args, boolean firstLoop) {
	}

	public abstract void loadExtraOutputArgs(List<String> args, String firstOutputName);

	private int lastLinkEstablishmentCause;

	protected abstract String getFirstArg();

	protected abstract List<String> getInputArgs();

	protected abstract List<String> getOutputArgs();

	protected abstract void createNewLink();

	protected void reestablishLink(int linkEstablishmentCause) throws InterruptedException {
		if ((link != null))
			return;// &&(linkEstablishmentCause==lastLinkEstablishmentCause)
		link = pipelineCallback.getExternalProgram(ourRow);
		if ((link != null) && link.stillAlive())
			return;
		lastLinkEstablishmentCause = linkEstablishmentCause;
		/*
		 * if ((input==null)&&(output==null)){
		 * throw new RuntimeException("Input and output have not been set in row "+ourRow+
		 * "; maybe click processed before step was first run");
		 * }
		 */

		createNewLink();
		pipelineCallback.setExternalProgram(ourRow, link);
		List<String> args = new ArrayList<>(15);
		args.add(getFirstArg());
		args.addAll(getInputArgs());
		args.add(getCommandName());
		args.add(getTIFFType());// write a 32-bit or 64-bit TIFF (BigTIFF format)
		args.addAll(getOutputArgs());

		String[] extraParams = getExtraParams();
		if (extraParams != null) {
			for (String extraParam : extraParams) {
				args.add(extraParam);
			}
		}

		Utils.log("Reestablished link with arguments " + Utils.printStringArray(args), LogLevel.DEBUG);
		if (this instanceof SpecialDimPlugin) {
			link.establish(args, (SpecialDimPlugin) this);
		} else {
			link.establish(args, null);
		}
	}

	protected IPluginIOStack input;
	protected IPluginIOStack output;
	protected LinkToExternalProgram link;

	/**
	 * Called once the external program is done running.
	 * This is meant to be overridden by subclasses of ExternalCall, for example to update
	 * GUI displays other than the output image.
	 */
	protected void postRunUpdates() {
	}

	/**
	 * Called every time the external program returns from the piece of work it got from getMoreWork.
	 * This is meant to be overridden by subclasses of ExternalCall, for example to update
	 * GUI displays other than the output image.
	 */
	public void postGetMoreWorkUpdates() {
	}

	protected JNACallToNativeLibrary.SetPixelHook externalCallPixelHook;

	@Override
	public void runChannel(final IPluginIOStack input, final IPluginIOStack output, final ProgressReporter p,
			final PreviewType previewType, boolean inputHasChanged) throws InterruptedException {
		this.input = input;
		this.output = output;

		link = pipelineCallback.getExternalProgram(ourRow);

		if ((link != null) && inputHasChanged) { // if the input has changed, just kill the program
			Utils.log("Input has changed; restarting local link", LogLevel.DEBUG);
			link.terminate(true);
			pipelineCallback.setExternalProgram(ourRow, null);
			link = null;
		}

		boolean linkFreshlyEstablished = (link == null);

		// TODO REMOVE MouseEventPlugin.PROGRAM_LAUNCHED_IN_RESPONSE_TO_PIPELINE==lastLinkEstablishmentCause
		// (which is currently always true; and reestablishLink ignores it anyway)
		if ((link == null) || (MouseEventPlugin.PROGRAM_LAUNCHED_IN_RESPONSE_TO_PIPELINE == lastLinkEstablishmentCause)) {
			reestablishLink(MouseEventPlugin.PROGRAM_LAUNCHED_IN_RESPONSE_TO_PIPELINE);
			linkFreshlyEstablished = true;
		}

		// write the argument the program needs to do to its standard input

		List<String> extraInputArgs = new ArrayList<>(5);
		extraArgsForRunLoop(extraInputArgs, linkFreshlyEstablished);
		String doLoopParameter;
		if (pipelineCallback.keepCProgramAlive(ourRow)) {
			doLoopParameter = "1 ";
		} else {
			doLoopParameter = "0 ";
		}
		String[] args =
				Utils.concatenateArrays(Utils.concatenateArrays(new String[] { doLoopParameter, "-1" }, extraInputArgs
						.toArray(new String[0])), getExtraParams());
		// For now, tell program not to quit after running (hence "0"), and to output the whole set of results
		// rather than just a slice preview (hence "-1")
		Utils.log("Writing to stdin: " + Utils.printStringArray(args, " ") + "\n", LogLevel.DEBUG);
		((JNACallToNativeLibrary) link).setSetPixelHook(externalCallPixelHook);

		if (this instanceof SpecialDimPlugin) {
			link.run(args, true, true, p, (SpecialDimPlugin) this);
		} else {
			link.run(args, true, true, p, null);
		}

		if (Thread.interrupted())
			throw new InterruptedException();

		postRunUpdates();
	}

	@Override
	public String operationName() {
		return "Abstract external call";
	}

	@Override
	public String version() {
		return "1.0";
	}

	public ExternalCall(PipelineCallback listener) {
		pipelineCallback = listener;
	}

	public ExternalCall() {
	}

	@SuppressWarnings("static-method")
	public boolean hasDisplayableOutput() {
		return true;
	}

}
