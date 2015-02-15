/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline;

import ij.ImagePlus;

import java.io.BufferedReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.xml.sax.InputSource;

import pipeline.data.NameAndFileBacking;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;

/**
 * OBSOLETE
 * Utility functions to parse and update XML pipeline metadata.
 *
 */
public class ParseImageMetadata {

	/**
	 * Update the node nodeToUpdate (most commonly "ProcessingSteps") in doc with the corresponding node contained in s.
	 * Leave everything else (e.g. channels) alone.
	 * 
	 * @param doc
	 * @param nodeToUpdate
	 * @param s
	 */
	static void setPipelineProcessingMetadata(Document doc, String nodeToUpdate, Element s) {
		// look for ProcessingSteps in parsed xml, and replace them with the
		// ones we've been passed

		Element root;
		if (doc.hasRootElement()) {
			root = doc.getRootElement();
			root.removeChild(nodeToUpdate);
			root.addContent(s.getChild(nodeToUpdate).detach());
		} else {
			root = new Element("PipelineMetadata");
			doc.addContent(root);
			root.addContent(s.getChild(nodeToUpdate).detach());
		}
	}

	/**
	 * Extracts pipeline metadata from the ImageJ metadata associated with the ImagePlus.
	 * Tries to ignore the parts of the string that are not part of the pipeline metadata.
	 * 
	 * @param imp
	 *            ImagePlus associated with metadata of interest
	 * @return String metadata (possibly empty)
	 */
	public static String getMetaData(ImagePlus imp) {
		synchronized (imp) {
			StringBuffer xmlToParse = new StringBuffer(100000);

			String metadata = ((String) imp.getProperty("Info"));
			if (metadata != null) {
				// try to look for XML and replace it with the new one in string s
				// look for a block delimited by BEGIN PIPELINE XML AND END PIPELINE XML

				BufferedReader reader = new BufferedReader(new StringReader(metadata));
				String str;
				boolean foundBeginning = false, doneWithXML = false;
				try {
					while ((str = reader.readLine()) != null) {
						if ((!doneWithXML) && foundBeginning) {
							// we are reading lines that belong to the XML we're getting rid of
							if (str.equals("END PIPELINE XML"))
								doneWithXML = true;
							else {
								xmlToParse.append(str);
								continue;
							}
						} else {
							if (str.equals("BEGIN PIPELINE XML")) {
								foundBeginning = true;
							}
						}
					}
				} catch (Exception e) {
					Utils.log("Failed to parse metada in" + imp.getTitle(), LogLevel.ERROR);
					Utils.printStack(e);
				}
			}
			return xmlToParse.toString();
		}
	}

	/**
	 * Converts the document to a string and sets the metadata associated with the ImagePlus.
	 * Attempts to replace pipeline metadata if it is already present.
	 * 
	 * @param doc
	 * @param imp
	 */
	public static void setImageMetadata(Document doc, ImagePlus imp) {
		// The parsing code is a duplicate of that in parseImageMetadata
		// we do this because this allows us to preserve stuff in the metadata that's not our XML
		synchronized (imp) {
			StringBuffer stuffToKeep = new StringBuffer(100000), xmlToParse = new StringBuffer(100000);

			String metadata = ((String) imp.getProperty("Info"));
			if (metadata != null) {
				// try to look for XML and replace it with the new one in string s
				// look for a block delimited by BEGIN PIPELINE XML AND END PIPELINE XML

				BufferedReader reader = new BufferedReader(new StringReader(metadata));
				String str;
				boolean foundBeginning = false, doneWithXML = false;
				try {
					while ((str = reader.readLine()) != null) {
						if ((!doneWithXML) && foundBeginning) {
							// we are reading lines that belong to the XML we're getting rid of
							if (str.equals("END PIPELINE XML"))
								doneWithXML = true;
							else {
								xmlToParse.append(str);
								continue;
							}
						} else {
							if (str.equals("BEGIN PIPELINE XML")) {
								foundBeginning = true;
							} else {
								stuffToKeep.append(str);
								stuffToKeep.append('\n');
							}
						}
					}
				} catch (Exception e) {
					Utils.log("Failed to parse metada in" + imp.getTitle(), LogLevel.ERROR);
					Utils.printStack(e);
				}
			}
			StringWriter sw = new StringWriter();
			XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
			String newXML;
			try {
				outputter.output(doc, sw);
				newXML = sw.toString();
			} catch (Exception e) {
				Utils.log("Problem printing XML to image metadata in setPipelineProcessingMetadata", LogLevel.ERROR);
				Utils.printStack(e);
				newXML = "";
			}

			imp.setProperty("Info", "BEGIN PIPELINE XML\n" + newXML + "\nEND PIPELINE XML\n" + stuffToKeep);
		}
	}

	/**
	 * Update the node named nodeToUpdate (most commonly "ProcessingSteps") in imp metadata with the corresponding node
	 * contained in s
	 * leave everything else (e.g. channels) alone
	 * 
	 * @param imp
	 * @param nodeToUpdate
	 * @param s
	 */
	public static void setPipelineProcessingMetadata(ImagePlus imp, String nodeToUpdate, Element s) {

		synchronized (imp) {
			// The parsing code is a duplicate of that in parseImageMetadata
			// we do this because this allows us to preserve stuff in the metadata that's not our XML
			String metadata = ((String) imp.getProperty("Info"));
			if (metadata == null) {
				imp.setProperty("Info", "BEGIN PIPELINE XML\n" + s + "\nEND PIPELINE XML");
				return;
			}
			// try to look for XML and replace it with the new one in string s
			// look for a block delimited by BEGIN PIPELINE XML AND END PIPELINE XML

			BufferedReader reader = new BufferedReader(new StringReader(metadata));
			String str;
			StringBuffer xmlToParse = new StringBuffer(100000);
			boolean foundBeginning = false, doneWithXML = false;
			StringBuffer stuffToKeepBuffer = new StringBuffer(100000);
			try {
				while ((str = reader.readLine()) != null) {
					if ((!doneWithXML) && foundBeginning) {
						// we are reading lines that belong to the XML we're getting rid of
						if (str.equals("END PIPELINE XML"))
							doneWithXML = true;
						else {
							xmlToParse.append(str);
							continue;
						}
					} else {
						if (str.equals("BEGIN PIPELINE XML")) {
							foundBeginning = true;
						} else {
							stuffToKeepBuffer.append(str);
							stuffToKeepBuffer.append('\n');
						}
					}
				}
			} catch (Exception e) {
				Utils.log("Failed to parse metada in" + imp.getTitle(), LogLevel.ERROR);
				Utils.printStack(e);
			}
			Document doc;
			// xmlToParse should contain xml we created and we know how to parse

			try {
				SAXBuilder builder = new SAXBuilder();
				doc = builder.build(new InputSource(new StringReader(xmlToParse.toString())));
			} catch (Exception e) {
				Utils.log("Failed to parse " + xmlToParse, LogLevel.ERROR);
				doc = new Document();
			}

			setPipelineProcessingMetadata(doc, nodeToUpdate, s);

			StringWriter sw = new StringWriter();
			XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
			String newXML;
			try {
				outputter.output(doc, sw);
				newXML = sw.toString();
			} catch (Exception e) {
				Utils.log("Problem printing XML to image metadata in setPipelineProcessingMetadata", LogLevel.ERROR);
				Utils.printStack(e);
				newXML = "";
			}

			imp.setProperty("Info", "BEGIN PIPELINE XML\n" + newXML + "\nEND PIPELINE XML\n" + stuffToKeepBuffer);
		}
	}

	/**
	 * Retrieves a channel property from ImagePlus metadata, or null if it absent.
	 * Leave everything else alone.
	 * 
	 * @param imp
	 * @param channelName
	 *            If null, return the info for the first channel found in the image
	 * @param propertyName
	 * @return XML property
	 */
	@SuppressWarnings("unchecked")
	public static String getChannelInfo(ImagePlus imp, String channelName, String propertyName) {

		synchronized (imp) {
			Document metaData = parseMetadata(imp);

			if (!metaData.hasRootElement()) {
				return null;
			}
			Element pipelineMetadata = metaData.getRootElement();
			Element channels = pipelineMetadata.getChild("Channels");
			List<Element> channelList;
			channelList = channels.getChildren();
			Element ourChannel = null;
			try {
				boolean found = false;
				for (Element aChannelList : channelList) {
					ourChannel = aChannelList;
					if ((channelName == null) || ourChannel.getChildText("Name").equals(channelName)) {
						found = true;
						break;
					}
				}
				if (!found)
					return null;
			} catch (Exception e) {
				Utils.log("Exception while extracting property " + propertyName
						+ " from parsed xml in updateChannelInfo", LogLevel.ERROR);
				Utils.printStack(e);
				return null;
			}

			return ourChannel.getChildText(propertyName);
		}
	}

	/**
	 * Update the information relative to how channel channelName was computed (Node tagged "Transform")
	 * or update temporary file backing (Node tagged "FileBacking") .
	 * Leave everything else alone.
	 * 
	 * @param imp
	 * @param channelName
	 *            If null, take the first channel found in the parsed XML metadata
	 * @param propertyName
	 * @param channelInfo
	 */
	@SuppressWarnings("unchecked")
	public static void updateChannelInfo(ImagePlus imp, String channelName, String propertyName, String channelInfo) {

		synchronized (imp) {
			Document metaData = parseMetadata(imp);

			if (!metaData.hasRootElement()) {
				throw new RuntimeException("No root element in updateChannelInfo");
			}
			Element pipelineMetadata = metaData.getRootElement();
			Element channels = pipelineMetadata.getChild("Channels");
			List<Element> channelList;
			channelList = channels.getChildren();
			Element ourChannel = null;
			try {
				boolean found = false;
				for (Element aChannelList : channelList) {
					ourChannel = aChannelList;
					if ((channelName == null) || ourChannel.getChildText("Name").equals(channelName)) {
						found = true;
						break;
					}
				}
				if (!found)
					throw new RuntimeException("channel " + channelName + " not found in updateChannelInfo");
			} catch (Exception e) {
				Utils.log("Exception while extracting names from parsed xml in updateChannelInfo", LogLevel.ERROR);
				Utils.printStack(e);
				return;
			}

			// Element oldTransform=ourChannel.getChild("Transform");
			ourChannel.removeChild(propertyName);
			ourChannel.addContent(new Element(propertyName).setText(channelInfo));

			// NodeList channelNode=metaData.getElementsByTagName(channelName);
			// channelNode.item(0).setUserData("Transform",channelInfo, null);

			StringWriter sw = new StringWriter();
			XMLOutputter outputter = new XMLOutputter();
			String xmlString;
			try {
				outputter.output(metaData, sw);
				xmlString = sw.toString();
			} catch (Exception e) {
				Utils.log("Problem printing XML to image metadata in updateChannelInfo", LogLevel.ERROR);
				Utils.printStack(e);
				xmlString = "";
			}

			imp.setProperty("Info", "BEGIN PIPELINE XML\n" + xmlString + "\nEND PIPELINE XML\n");
		}
	}

	/**
	 * Retrieves the pipeline metadata associated with the ImagePlus, and parses it to a Document.
	 * If there is not metadata or parsing fails, returns an empty Document.
	 * 
	 * @param imp
	 * @return Document containing the metadata
	 */
	public static Document parseMetadata(ImagePlus imp) {
		if (imp == null)
			return new Document();
		synchronized (imp) {
			try {

				String metadata = ((String) imp.getProperty("Info"));
				if (metadata == null)
					return new Document();
				// try to look for XML and parse it
				// look for a block delimited by BEGIN PIPELINE XML AND END PIPELINE XML

				BufferedReader reader = new BufferedReader(new StringReader(metadata));
				String str;
				StringBuffer xmlToParse = new StringBuffer(20000);
				boolean foundBeginning = false;
				try {
					while ((str = reader.readLine()) != null) {
						if (foundBeginning) {
							// we are reading lines that belong to the XML we'll parse later
							if (!str.equals("END PIPELINE XML")) {
								xmlToParse.append(str);
								xmlToParse.append('\n');
							} else
								break;
						} else {
							if (str.equals("BEGIN PIPELINE XML")) {
								foundBeginning = true;
							}
						}
					}
				} catch (Exception e) {
					Utils.log("Failed to find any metada in " + imp.getTitle(), LogLevel.ERROR);
					Utils.printStack(e);
					return new Document();
				}

				Document doc;
				// xmlToParse should contain xml we created and we know how to parse

				try {
					SAXBuilder builder = new SAXBuilder();
					doc = builder.build(new InputSource(new StringReader(xmlToParse.toString())));
				} catch (Exception e) {
					Utils.log("Failed to parse " + xmlToParse, LogLevel.ERROR);
					return new Document();
				}

				return doc;
			} catch (Exception e) {
				Utils.log("Failed to parse image metada C " + imp.getTitle(), LogLevel.ERROR);
				Utils.printStack(e);

				return null;
			}
		}
	}

	/**
	 * Return a list of channel names in the ImagePlus. First look in the metadata and go by that if possible.
	 * If not, check if ImagePlus is a hyperstack; if it is, generate a generic list of names.
	 * If ImagePlus is not a hyperstack, just create a single generic name.
	 * 
	 * @param imp
	 * @return Array of channel names, in the same order as they appear in the ImageJ structure.
	 * @see #extractChannelNamesAndFileStore
	 */
	@SuppressWarnings("unchecked")
	public static String[] extractChannelNames(ImagePlus imp) {

		// if it's not,
		// doc.getDocumentElement().normalize();
		if (imp == null)
			return new String[0];
		synchronized (imp) {
			Document doc = parseMetadata(imp);
			Element documentRoot = doc.hasRootElement() ? doc.getRootElement() : null;
			Element channels;
			List<Element> channelList;

			if (documentRoot == null) {// we need to create a root
				channels = null;
				channelList = null;
			} else {
				channels = documentRoot.getChild("Channels");
				if (channels == null)
					channelList = null;
				else
					channelList = channels.getChildren();
			}

			if ((channelList == null) || (channelList.size() == 0)) {
				// nothing to be found in metadata
				// just examine the image and make generic names
				if (!imp.isHyperStack())
					return new String[] { Utils.DEFAULT_CHANNEL_NAME_WHEN_ONLY_1_CHANNEL };// was "Only channel"

				// we now know we're dealing with a hyperstack
				int nChannels = imp.getNChannels();
				String[] names = new String[nChannels];
				for (int i = 0; i < nChannels; i++) {
					names[i] = "Ch " + i;
				}
				return names;
			}

			try {
				String[] nameArray = new String[channelList.size()];
				int index = 0;
				for (Element e : channelList) {
					nameArray[index] = e.getChildText("Name");
					index++;
				}

				return nameArray;
			} catch (Exception e) {
				Utils.log("Exception while extracting names from parsed xml", LogLevel.ERROR);
				Utils.printStack(e);
				return null;
			}
		}
	}

	/**
	 * Return a list of channel names in the ImagePlus, and information about whether any of the channels
	 * have a copy in a temporary single-channel TIFF. First look in the metadata and go by that if possible.
	 * If not, check if ImagePlus is a hyperstack; if it is, generate a generic list of names.
	 * If ImagePlus is not a hyperstack, just create a single generic name.
	 * 
	 * @param imp
	 * @return Array of channel names, in the same order as they appear in the ImageJ structure.
	 * @see #extractChannelNames
	 */
	@SuppressWarnings("unchecked")
	public static NameAndFileBacking extractChannelNamesAndFileStore(ImagePlus imp) {
		// first look in the metadata and go by that if possible
		// if not, check if imp is a hyperstack; if it is, generate a generic list of names
		// if it's not, just create a single generic name
		// doc.getDocumentElement().normalize();
		synchronized (imp) {
			NameAndFileBacking result = new NameAndFileBacking();

			Document doc = parseMetadata(imp);
			Element documentRoot = doc.hasRootElement() ? doc.getRootElement() : null;
			Element channels;
			List<Element> channelList;

			if (documentRoot == null) {// we need to create a root
				channels = null;
				channelList = null;
			} else {
				channels = documentRoot.getChild("Channels");
				if (channels == null)
					channelList = null;
				else
					channelList = channels.getChildren();
			}

			if ((channelList == null) || (channelList.size() == 0)) {
				// nothing to be found in metadata
				// just examine the image and make generic names
				if (!imp.isHyperStack()) {
					result.channelNames = new String[] { "Only channel" };
					result.filePaths = new String[] { null };
					result.timesStored = new long[] { 0 };// make sure the [non-existent] file is seen as stale
					result.timesModified = new long[] { System.currentTimeMillis() };
					return result;
				}

				// we now know we're dealing with a hyperstack
				int nChannels = imp.getNChannels();
				String[] names = new String[nChannels];
				String[] filePaths = new String[nChannels];
				long[] timesStored = new long[nChannels];
				long[] timesModified = new long[nChannels];
				for (int i = 0; i < nChannels; i++) {
					names[i] = "Ch " + i;
					timesStored[i] = 0;// already initialized to 0; just for the sake of clarity
					timesModified[i] = System.currentTimeMillis();
				}
				result.channelNames = names;
				result.filePaths = filePaths;
				result.timesStored = timesStored;
				result.timesModified = timesModified;
				return result;
			}

			try {
				String[] names = new String[channelList.size()];
				String[] filePaths = new String[channelList.size()];
				long[] timesStored = new long[channelList.size()];
				long[] timesModified = new long[channelList.size()];
				int index = 0;
				for (Element e : channelList) {
					names[index] = e.getChildText("Name");
					filePaths[index] = e.getChildText("FileBacking");
					String lastStorageString = e.getChildText("LastStorageTime");
					if (lastStorageString == null) {
						Utils.log("Missing last storage time for channel " + names[index], LogLevel.WARNING);
					} else
						timesStored[index] = Long.parseLong(lastStorageString);
					String lastModificationString = e.getChildText("LastModificationTime");
					if (lastModificationString == null) {
						Utils.log("Missing last modification time for channel " + names[index]
								+ "; WARNING: ASSUMING IT HAS NOT BEEN MODIFIED", LogLevel.WARNING);
					} else
						timesModified[index] = Long.parseLong(lastModificationString);
					index++;
				}

				result.channelNames = names;
				result.filePaths = filePaths;
				result.timesStored = timesStored;
				result.timesModified = timesModified;
				return result;
			} catch (Exception e) {
				Utils.log("Exception while extracting names from parsed xml", LogLevel.ERROR);
				Utils.printStack(e);
				return null;
			}
		}
	}

	/**
	 * Add new channel names to a metadata Document, and return the result as an XML String.
	 * 
	 * @param doc
	 *            Should not be null, but can be empty
	 * @param channelName
	 * @return metadata with added channel names as a String
	 * @see #addChannelNamesToNewImp
	 */
	public static String addChannelNameToXML(Document doc, String[] channelName) {
		// Node newChannel=doc.createElement("Channel");//was Node
		Element[] newChannel = new Element[channelName.length];
		for (int i = 0; i < newChannel.length; i++) {
			newChannel[i] = new Element("Channel");
			newChannel[i].addContent(new Element("Name").addContent(channelName[i]));
			newChannel[i].addContent(new Element("Derivation").addContent("plugin description will go here"));
		}
		// no need to add details about processing yet; we'll do that while processing
		// Node newNode=doc.importNode(newChannel,true);
		// doc.insertBefore(newNode, null);
		// Node root;

		Element documentRoot = doc.hasRootElement() ? doc.getRootElement() : null;
		Element channels;
		if (documentRoot == null) {// we need to create a root
			documentRoot = new Element("PipelineMetadata");
			doc.addContent(documentRoot);
			channels = new Element("Channels");
			documentRoot.addContent(channels);
		} else {
			channels = documentRoot.getChild("Channels");
			if (channels == null) {
				channels = new Element("Channels");
				documentRoot.addContent(channels);
			}
			// check if the name already exists
			for (int i = 0; i < newChannel.length; i++) {
				if (channels.getChild(channelName[i]) != null)
					throw new RuntimeException("Node name " + channelName[i] + " already exists");
			}
			/*
			 * NodeList nodeLst=root.getChildNodes();
			 * for (int i=0;i<nodeLst.getLength();i++){
			 * if ((nodeLst.item(i).getNodeType()==Node.ELEMENT_NODE) && ((Element)
			 * nodeLst.item(i)).getAttribute("Name").equals(channelName)){
			 * throw new RuntimeException ("Node name already exists");
			 * }
			 * }
			 */
		}
		for (Element element : newChannel) {
			channels.addContent(element);
		}

		StringWriter sw = new StringWriter();
		XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
		String xmlString;
		try {
			outputter.output(doc, sw);
			xmlString = sw.toString();
		} catch (Exception e) {
			Utils.log("Problem printing XML to image metadata in addBlankChannel", LogLevel.ERROR);
			Utils.printStack(e);
			xmlString = "";
		}

		return "BEGIN PIPELINE XML\n" + xmlString + "END PIPELINE XML\n";
	}

	/**
	 * Add new channel names to the metadata found in oldImp, and store the result in the metadata
	 * associated with newImp.
	 * 
	 * @param oldImp
	 * @param newImp
	 * @param nameArray
	 * @see #addChannelNameToXML
	 */
	public static void addChannelNamesToNewImp(ImagePlus oldImp, ImagePlus newImp, String[] nameArray) {
		synchronized (oldImp) {
			synchronized (newImp) {
				// copy old metadata (excluding channel names) over from oldImp
				Document doc = ParseImageMetadata.parseMetadata(oldImp);
				if (!doc.hasRootElement()) {
					// there was no metadata of ours in the image we're adding a channel to

					String image1_info = (String) oldImp.getProperty("Info");
					newImp.setProperty("Info", addChannelNameToXML(doc, nameArray) + image1_info);
				} else {
					// remove channels if they exist
					Element documentRoot = doc.getRootElement();
					Element channels = documentRoot.getChild("Channels");
					if (channels != null) {
						documentRoot.removeContent(channels);
					}
					newImp.setProperty("Info", addChannelNameToXML(doc, nameArray));// image1_info was here
				}
				if (ParseImageMetadata.extractChannelNames(newImp).length != newImp.getNChannels()) {
					throw new RuntimeException("Mismatch between number of channels in image newImp="
							+ newImp.getTitle() + " channels=" + newImp.getNChannels() + " and in metadata="
							+ ParseImageMetadata.extractChannelNames(newImp).length + " names="
							+ Utils.printStringArray(ParseImageMetadata.extractChannelNames(newImp)));
				}
			}
		}
	}
}
