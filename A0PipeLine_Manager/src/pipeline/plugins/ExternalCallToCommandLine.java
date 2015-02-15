/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins;

import ij.ImagePlus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import pipeline.ParseImageMetadata;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOImage;
import pipeline.data.NameAndFileBacking;
import pipeline.data.PluginIOCells;
import pipeline.data.SingleChannelView;
import pipeline.external_plugin_interfaces.SystemCallToExternalProgram;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;

public abstract class ExternalCallToCommandLine extends ExternalCall {

	@Override
	public void createNewLink() {
		link = new SystemCallToExternalProgram();
	}

	@Override
	public int getFlags() {
		return PLUGIN_CREATES_OUTPUT_ITSELF + ONLY_FLOAT_INPUT;
	}

	/*
	 * @Override
	 * public void createDestination(){
	 * if (!hasDisplayableOutput()) return;
	 * if (source!=null) destination= new PluginIOHyperstackViewWithImagePlus(operationName()+"_"+source.getTitle());
	 * else destination= new PluginIOHyperstackViewWithImagePlus(operationName());
	 * }
	 */

	@Override
	public void loadExtraInputArgsForEstablishment(List<String> args) {
		String[] labels = getInputLabels();
		Map<String, IPluginIO> auxInputs = pluginInputs;// pipelineCallback.getAuxInputs(ourRow);
		for (String label : labels) {
			Object auxInput;
			try {
				auxInput = auxInputs.get(label).asFile(null, true).getAbsolutePath();
			} catch (IOException | InterruptedException e) {
				throw new RuntimeException("Error getting inputs as files", e);
			}
			String auxInputAsString = null;
			if (auxInput == null) {
				Utils.log("did not find input " + label + "; passing 0 instead", LogLevel.DEBUG);
				auxInput = "0";
			}
			// FIXME Obsolete code
			if (auxInput instanceof ImagePlus) {
				// If we're fining an ImagePlus instead of a file path, we need to check if the contents of the
				// image are already on disk, and create a new file if not. We then pass a string corresponding
				// to that filename.
				// TODO For now we are assuming that we want the first channel in the stack; we should lift that
				// restriction, perhaps by passing a SingleChannelView instead of ImagePlus

				NameAndFileBacking c = ParseImageMetadata.extractChannelNamesAndFileStore((ImagePlus) auxInput);
				if (c.filePaths[0] == null) {// the output of targetRow has not been stored to disk
					Boolean programNeedsToReloadInput = new Boolean(true);
					String storedInputName =
							(new SingleChannelView((ImagePlus) auxInput, 1, null))
									.getStoreFile(programNeedsToReloadInput);
					c.filePaths[0] = storedInputName;
				}
				auxInputAsString = c.filePaths[0];
			} else if (auxInput instanceof String) {
				// this should already by a path to a file on disk
				auxInputAsString = (String) auxInput;
			} else {
				throw new RuntimeException(
						"Auxiliary input is neither a string nor an ImagePlus; don't know how to deal with it");
			}
			args.add(auxInputAsString);
		}
	}

	@Override
	public void loadExtraOutputArgs(List<String> args, String firstOutputName) {
		String[] labels = getOutputLabels();
		Map<String, IPluginIO> auxOutputs = pluginOutputs;// pipelineCallback.getAuxOutputs(ourRow);
		// auxOutputs.clear();
		for (String label : labels) {
			// String newName=firstOutputName+"_"+i;
			// Object auxOutput=auxOutputs.get(labels[i]).asFile().getAbsolutePath();
			// auxOutputs.put(labels[i],newName);
			// args.add(newName);
			if (auxOutputs.get(label) instanceof IPluginIOImage)
				try {
					args.add(auxOutputs.get(label).asFile(null, true).getAbsolutePath());
				} catch (IOException | InterruptedException e) {
					throw new RuntimeException("Error getting outputs as files", e);
				}
		}
	}

	void loadOutputProtobuf(List<String> args) {
		String[] labels = getOutputLabels();
		Map<String, IPluginIO> auxOutputs = pluginOutputs;// pipelineCallback.getAuxOutputs(ourRow);
		// auxOutputs.clear();
		for (String label : labels) {
			// String newName=firstOutputName+"_"+i;
			// Object auxOutput=auxOutputs.get(labels[i]).asFile().getAbsolutePath();
			// auxOutputs.put(labels[i],newName);
			// args.add(newName);
			if (auxOutputs.get(label) instanceof PluginIOCells)
				try {
					args.add(auxOutputs.get(label).asFile(null, true).getAbsolutePath());
				} catch (IOException | InterruptedException e) {
					throw new RuntimeException("Error getting output protobuf as files", e);
				}
		}
	}

	/*
	 * protected String getStoredOutputFileName(){
	 * String storedOutputName=null;
	 * SingleChannelView scv=null;
	 * if (hasDisplayableOutput()){
	 * if (destination instanceof PluginIOHyperstackViewWithImagePlus){
	 * PluginIOHyperstackViewWithImagePlus extImp=null;
	 * extImp=(PluginIOHyperstackViewWithImagePlus) destination;
	 * scv=extImp.singleChannelView;
	 * scv.createNewFileBackingName("name_prefix",".tiff");
	 * 
	 * storedOutputName=extImp.singleChannelView.getStoreFile(new Boolean(true));
	 * } else { //assume we have a CompositeImageWithMetadata
	 * CompositeImageWithMetadata extImp;
	 * extImp=(CompositeImageWithMetadata) destination;
	 * scv=extImp.singleChannelView;
	 * scv.createNewFileBackingName("name_prefix",".tiff");
	 * 
	 * storedOutputName=extImp.singleChannelView.getStoreFile(new Boolean(true));
	 * }
	 * Utils.log("created output file name "+storedOutputName);
	 * }
	 * return storedOutputName;
	 * }
	 * 
	 * protected SingleChannelView getSingleChannelView(){
	 * SingleChannelView scv=null;
	 * if (hasDisplayableOutput()){
	 * if (destination instanceof PluginIOHyperstackViewWithImagePlus){
	 * PluginIOHyperstackViewWithImagePlus extImp=null;
	 * extImp=(PluginIOHyperstackViewWithImagePlus) destination;
	 * scv=extImp.singleChannelView;
	 * scv.createNewFileBackingName("name_prefix",".tiff");
	 * 
	 * } else { //assume we have a CompositeImageWithMetadata
	 * CompositeImageWithMetadata extImp;
	 * extImp=(CompositeImageWithMetadata) destination;
	 * scv=extImp.singleChannelView;
	 * scv.createNewFileBackingName("name_prefix",".tiff");
	 * }
	 * }
	 * return scv;
	 * }
	 */

	@Override
	public String getFirstArg() {
		return "plugins/segpipeline_1";
	}

	@Override
	public List<String> getInputArgs() {
		ArrayList<String> inputArgs = new ArrayList<>(5);
		String storedInputName;
		try {
			storedInputName = input.asFile(null, true).getAbsolutePath();
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException("Error getting inputs as files", e);
		}
		Utils.log("input backing is " + storedInputName, LogLevel.DEBUG);
		// if input.getStoreFile is empty, the channelview will create a file
		// and copy its contents to that file
		// if the file already exists, it will try to determine whether it has been
		// modified since the last save (reading the modification and save fields in the ImagePlus metadata)
		inputArgs.add(storedInputName);
		loadExtraInputArgsForEstablishment(inputArgs);
		padWithDummyInputFileNames(inputArgs);
		return inputArgs;
	}

	@Override
	public List<String> getOutputArgs() {
		ArrayList<String> outputArgs = new ArrayList<>(6);
		String storedOutputName;
		try {
			storedOutputName = getOutput().asFile(null, true).getAbsolutePath();
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException("Error getting outputs as files", e);
		}// getStoredOutputFileName();
		if (storedOutputName != null)
			outputArgs.add(storedOutputName);
		loadExtraOutputArgs(outputArgs, storedOutputName);
		padWithDummyOutputFileNames(outputArgs);
		loadOutputProtobuf(outputArgs);
		return outputArgs;
	}

	@Override
	public void postRunUpdates() {
		// NOW LET THE SHELL SHOW ALL OUTPUTS, EVEN IF THEY'RE NOT RESIDENT IN IMAGEPLUS PIXELS
		// (EG FILE ON DISK THAT'S ONLY NOW PRESENT TO BE DISPLAYED)
		/*
		 * if (hasDisplayableOutput()){
		 * SingleChannelView scv=getSingleChannelView();
		 * String fileName=scv.getStoreFile(new Boolean(true));
		 * File file=new File(fileName);
		 * 
		 * try {
		 * Utils.openVirtualTiff(file, destination,true);
		 * } catch (ImageOpenFailed e) {
		 * Utils.printStack(e);
		 * }
		 * / *
		 * //TiffDecoder td = new TiffDecoder(fi.directory, fi.fileName);
		 * TiffDecoder td = new TiffDecoder(file.getParent(), file.getName());
		 * 
		 * FileInfo[] fiArray=null;
		 * try {fiArray = td.getTiffInfo();}
		 * catch (IOException e) {
		 * String msg = e.getMessage();
		 * if (msg==null||msg.equals("")) msg = ""+e;
		 * Utils.displayError("TiffDecoder", msg);
		 * throw new RuntimeException("Error reading output of external program");
		 * }
		 * if (fiArray==null || fiArray.length==0) {
		 * Utils.displayError("Virtual Stack", "This does not appear to be a TIFF stack; not displaying");
		 * throw new RuntimeException("Error reading output of external program");
		 * }
		 * 
		 * 
		 * FileInfoVirtualStack fivs=new FileInfoVirtualStack();
		 * FileOpener.setSilentMode(true);
		 * fivs.info=fiArray;
		 * fivs.openButDontShow(destination);* /
		 * destination.show();
		 * //destination.updateAndDraw();
		 * }
		 */
	}
}
