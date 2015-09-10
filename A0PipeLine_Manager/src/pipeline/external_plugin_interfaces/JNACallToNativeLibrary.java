/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.external_plugin_interfaces;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOHyperstack;
import pipeline.data.IPluginIOImage;
import pipeline.data.IPluginIOStack;
import pipeline.data.ImageAccessor;
import pipeline.data.PluginIOCells;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.misc_util.BasicROI;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.SettableBoolean;
import pipeline.misc_util.SimpleImageDimensions;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.plugins.SpecialDimPlugin;

import com.sun.jna.Callback;
import com.sun.jna.CallbackThreadInitializer;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;

// Needs to be reinstantiated if dimensions of slices change
// Warning: the garbage collector might need to be manually called at regular intervals
// If there are lots of allocated transferBuffers, we might need to expand the heap size with set
// sun.nio.MaxDirectMemorySize
// upon VM startup.
/**
 * Implements a link to a plugin in a dynamic library.
 */
public class JNACallToNativeLibrary extends LinkToExternalProgram {

	public interface SetPixelHook {
		public void run(int index, ByteBuffer buffer);
	}

	private SetPixelHook setPixelHook;

	public void setSetPixelHook(SetPixelHook hook) {
		setPixelHook = hook;
	}

	private static boolean suppressDestinationNotFoundWarning = true;

	/**
	 * Raised when a plugin designates an input or output image (to access its pixels or metadata), but the string
	 * used to specify the image does not match anything the pipeline has a record of. This could be either because
	 * the name is wrong, or because the relevant images have not been created by other processing steps (the latter
	 * should not be the case, as that error should be caught by the pipeline before calling the dynamic library
	 * plugin).
	 *
	 */
	public class ImageNotFound extends Exception {
		public ImageNotFound(String string) {
			super(string);
		}

		private static final long serialVersionUID = 1L;
	}

	/**
	 * 
	 * @param hashMap
	 * @param key
	 * @return ImagePlus corresponding to String key
	 */
	private static IPluginIO getImagePlusFromHashMap(Map<String, IPluginIO> hashMap, String key) {
		Object image = hashMap.get(key);
		if (image == null)
			return null;
		return (IPluginIO) image;
	}

	/**
	 * Searches for an ImagePlus stored under key "name", in registered inputs and outputs of the current processing
	 * step.
	 * 
	 * @param name
	 *            null allowed
	 * @param searchInputFirst
	 *            If true search for key "name" within inputs of processing step first, then outputs.
	 * @return Never null
	 * @throws ImageNotFound
	 */
	private IPluginIO getAuxInputOrOutputImp(String name, boolean searchInputFirst) throws ImageNotFound {
		IPluginIO result;
		Map<String, IPluginIO> usedHashMap = null;
		ArrayList<String> foundNames = new ArrayList<>(10);
		if (searchInputFirst) {
			usedHashMap = sources;
			foundNames.addAll(usedHashMap.keySet());
			result = getImagePlusFromHashMap(usedHashMap, name);
			if (result == null) {
				usedHashMap = destinations;
				result = getImagePlusFromHashMap(usedHashMap, name);
				foundNames.addAll(usedHashMap.keySet());
			}
		} else {
			usedHashMap = destinations;
			result = getImagePlusFromHashMap(usedHashMap, name);
			foundNames.addAll(usedHashMap.keySet());
			if (result == null) {
				usedHashMap = sources;
				result = getImagePlusFromHashMap(usedHashMap, name);
				foundNames.addAll(usedHashMap.keySet());
			}
		}
		if (result == null)
			throw new ImageNotFound("Could not find key " + name + " among " + Utils.printStringArray(foundNames)
					+ "; input HashMap is " + sources + "\n");
		return result;
	}

	// In the future, extend functionality to deal with the case where the object retrieved from the HashMap
	// is a String that gives a path to a file on disk
	/**
	 * Searches for slice "slice" of ImagePlus stored under key "name", in registered inputs and outputs of the current
	 * processing step.
	 * 
	 * @param name
	 * @param slice
	 *            Index of the slice; indexing begins at 0
	 * @param searchInputFirst
	 *            If true search for key "name" within inputs of processing step first, then outputs.
	 * @return 1D float array corresponding to all the pixels in slice
	 * @throws ImageNotFound
	 */
	private float[] getAuxInputOrOutputPixels(String name, int slice, boolean searchInputFirst) throws ImageNotFound {
		IPluginIOHyperstack image = (IPluginIOHyperstack) getAuxInputOrOutputImp(name, searchInputFirst);
		float[] result = (float[]) image.getPixels(slice, 0, 0);
		if (result == null)
			throw new RuntimeException("Null pixel slice while looking for " + name);
		return result;
	}

	private Map<String, IPluginIO> sources;
	private Map<String, IPluginIO> destinations;

	static {
		try {
			libraryInstance = (NativeLibrary) Native.loadLibrary("segpipeline_1", NativeLibrary.class);
		} catch (UnsatisfiedLinkError e) {
			Utils.log("Native library search path is " + System.getProperty("jna.library.path"), LogLevel.ERROR);
			throw (e);
		}
		Native.setCallbackExceptionHandler(Utils.callbackExceptionHandler);
	}

	/**
	 * Constructor to get an instance that can be used for repeated calls to a plugin in a dynamic library.
	 * 
	 * @param input
	 *            Input image
	 * @param output
	 *            Output image
	 * @param libraryName
	 *            Name of the dynamic library, *without* "lib" prefix and platform-dependent suffix (e.g. without
	 *            ".dylib" for Mac OS X)
	 * @param sources2
	 * @param destinations2
	 */
	public JNACallToNativeLibrary(final IPluginIOStack input, final IPluginIOStack output, String libraryName,
			Map<String, IPluginIO> sources2, Map<String, IPluginIO> destinations2) {// PipelineCallback
																					// pipelineCallback){
		super();
		if (!libraryName.equals("segpipeline_1"))
			throw new IllegalArgumentException();
		Utils.log("Created new JNACallToNativeLibrary", LogLevel.DEBUG);
		this.sources = sources2;
		this.destinations = destinations2;

		inputImage = input;
		outputImage = output;

		callback.getMoreWork = new GetMoreWork() {
			@Override
			public final String[] invoke() {
				Utils.log("Entering getMoreWork", LogLevel.VERBOSE_DEBUG);
				try {
					interruptPlugin = false;
					if (!keepAlive) {
						Utils.log("--- Returning null from getMoreWork because keepAlive=false", LogLevel.VERBOSE_DEBUG);
						isAlive = false;
						return null;
					} else {
						synchronized (isComputingSemaphore) {
							while ((!terminateFlag) && (workQueue.size() == 0)) {
								lastActiveTime = System.currentTimeMillis();
								isComputing = false;
								isComputingSemaphore.notifyAll();

								try {
									Utils.log("--- Sleeping in getMoreWork", LogLevel.VERBOSE_DEBUG);
									isComputingSemaphore.wait();
									Utils.log("--- Work queue waking up", LogLevel.VERBOSE_VERBOSE_DEBUG);
								} catch (InterruptedException e) {
									Utils.log("--- Returning null from getMoreWork because interrupted",
											LogLevel.VERBOSE_DEBUG);
									interrupt();
									isAlive = false;
									return null;
								}
							}
							if (terminateFlag) {
								Utils.log("--- Returning null from getMoreWork because terminateFlag=true in " + this,
										LogLevel.VERBOSE_DEBUG);
								terminateFlag = false;
								isComputing = false;
								isComputingSemaphore.notifyAll();
								return null;
							}
							interruptPlugin = false;
							// isComputing=true;
							Utils.log("--- Retrieving work queue in getMoreWork", LogLevel.VERBOSE_DEBUG);
							workQueue.add(new String(new byte[] { 0 }));// terminating null string for the plugin
							// to know when to stop reading arguments
							// pad with a few nulls in case the plugin tries to access arguments that haven't been
							// loaded
							workQueue.add(null);
							workQueue.add(null);
							workQueue.add(null);
							workQueue.add(null);
							workQueue.add(null);
							String[] workArguments = workQueue.toArray(new String[0]);
							workQueue.clear();
							Utils.log("--- Returning work arguments in getMoreWork "
									+ Utils.printStringArray(workArguments), LogLevel.VERBOSE_DEBUG);
							return workArguments;// not sure if it's necessary to convert to array

						}
					}
				} catch (Exception e) {
					Utils.printStack(e);
					returnValue = 1;
					isAlive = false;
					return null;
				}
			}
		};

		callback.getDimensions = inputOrOutputName -> {
			try {
				if (inputOrOutputName == null)
					throw new RuntimeException("Null inputOrOutputName in GetDimensions");
				// If we were passed a name, need to find a reference in auxiliary inputs or outputs
				// For now, assume that the image will already be open and have its pixels available.
				// In the future, make it possible to load things on demand, for example when we're just
				// given a path to a TIFF on disk.
				return getImageDimensions((IPluginIOHyperstack) getAuxInputOrOutputImp(inputOrOutputName, true));
			} catch (Exception e) {
				returnValue = 1;
				Utils.printStack(e);
				return null;
			}
		};

		callback.getDimensionsByRef = (inputOrOutputName, x, y, z, t, c) -> {
			try {
				if (inputOrOutputName == null) {
					throw new RuntimeException("Null inputOrOutputName in GetDimensions");
				}
				// If we were passed a name, need to find a reference in auxiliary inputs or outputs
				// For now, assume that the image will already be open and have its pixels available.
				// In the future, make it possible to load things on demand, for example when we're just
				// given a path to a TIFF on disk.
				IPluginIO io = getAuxInputOrOutputImp(inputOrOutputName, true);
				if (io instanceof IPluginIOHyperstack) {
					SimpleImageDimensions dim = getImageDimensions((IPluginIOHyperstack) io);
					x.setValue(dim.width);
					y.setValue(dim.height);
					z.setValue(dim.depth);
					t.setValue(dim.time);
					c.setValue(dim.channels);
				} else if (io instanceof PluginIOCells) {
					x.setValue(((PluginIOCells) io).getWidth());
					y.setValue(((PluginIOCells) io).getHeight());
					z.setValue(((PluginIOCells) io).getDepth());
					t.setValue(1);
					c.setValue(1);
				} else
					throw new RuntimeException("Cannot read dimensions of object " + inputOrOutputName + "" + io);
				return 0;
			} catch (Exception e) {
				Utils.printStack(e);
				returnValue = 1;
				return 1;
			}

		};

		callback.setDimensions =
				(inputOrOutputName, x, y, z, t, c) -> {
					try {
						IPluginIOHyperstack target = null;
						if ((inputOrOutputName == null) || inputOrOutputName.equals("Default destination"))
							target = outputImage;
						if ((target == null) && inputOrOutputName.equals("Default source"))
							target = inputImage;
						if (target == null)
							target = (IPluginIOHyperstack) getAuxInputOrOutputImp(inputOrOutputName, false);
						if (!(target instanceof ImageAccessor)) {
							Utils.log("Set dimensions call not supported on PluginIO " + target + " whose name is "
									+ target.getName(), LogLevel.WARNING);
							return 1;
						}
						((ImageAccessor) target).setDimensions(x, y, z, c, t);
						return 0;
					} catch (Exception e) {
						Utils.printStack(e);
						returnValue = 1;
						return 1;
					}
				};

		callback.getPixels = (index, inputOrOutputName, roi, cachePolicy) -> {
			try {
				// Utils.log("getPixels callback on slice "+index+" of "+inputOrOutputName,LogLevel.VERBOSE_VERBOSE_DEBUG);
				transferBuffer.clear();
				if (roi == null) {
					if ((inputOrOutputName == null) || inputOrOutputName.equals("Default source")) {
						if (inputImage.getPixelType() == PixelType.BYTE_TYPE) {
							byteBuffer.clear();
							byteBuffer.put(inputImage.getPixels(index, (byte) 0));
						} else {
							transferBuffer.put(inputImage.getPixels(index, 0.0f));
						}
						// TODO Handle transfer of short pixels
					}
					// If we were passed a name, need to find a reference in auxiliary inputs or outputs
					// For now, assume that the image will already be open and have its pixels available.
					// In the future, make it possible to load things on demand, for example when we're just
					// given a path to a TIFF on disk.
					else {
						PixelType pType =
								((IPluginIOImage) getAuxInputOrOutputImp(inputOrOutputName, true)).getPixelType();
						if (pType == PixelType.FLOAT_TYPE) {
							float[] pixels = getAuxInputOrOutputPixels(inputOrOutputName, index, true);
							transferBuffer.put(pixels);
						} else if (pType == PixelType.BYTE_TYPE) {
							byte[] pixels =
									((IPluginIOStack) getAuxInputOrOutputImp(inputOrOutputName, true)).getPixels(0,
											(byte) 0);
							byteBuffer.clear();
							byteBuffer.put(pixels);
						}

					}
				} else {
					float[] source;
					if ((inputOrOutputName == null) || inputOrOutputName.equals("Default source"))
						source = inputImage.getPixels(index, 0.0f);
					else
						source = getAuxInputOrOutputPixels(inputOrOutputName, index, true);
					float[] transferBufferCopy = new float[(roi.x1 - roi.x0) * (roi.y1 - roi.y0)];
					int i = 0;
					// TODO we're assuming that all images have the same width
					for (int x = roi.x0; x <= roi.x1; x++) {
						for (int y = roi.y0; y <= roi.y1; y++) {
							transferBufferCopy[i++] = source[x + y * inputImage.getWidth()];
						}
					}
					transferBuffer.put(transferBufferCopy, 0, transferBufferCopy.length);
				}
			} catch (Exception e) {
				returnValue = 1;
				Utils.printStack(e);
			}
		};

		callback.setPixels = (index, inputOrOutputName, roi, cachePolicy, multiThreadedIO) -> {
			try {
				if (transferBuffer == null) {
					// This can happen if the C plugin keeps going when it should have interrupted
				interruptPlugin = true;
				Utils.log("Null transfer buffer; C plugin probably keeps going after being interrupted", LogLevel.ERROR);
				return;
			}
			// Utils.log("top level write slice",LogLevel.VERBOSE_DEBUG);

			// Utils.log("setPixels callback on slice "+index+" of "+inputOrOutputName,LogLevel.VERBOSE_VERBOSE_DEBUG);
			if (roi == null) {
				if (setPixelHook != null) {
					setPixelHook.run(index, byteBuffer);
					byteBuffer.clear();
				}
				transferBuffer.clear();
				byteBuffer.clear();
				if (((outputImage != null) && outputImage.isSupportsWritingToPixels())
						|| ((inputOrOutputName != null) && (!(getAuxInputOrOutputImp(inputOrOutputName, false) instanceof ImageAccessor)))) {
					/*
					 * Utils.log("write slice",LogLevel.VERBOSE_DEBUG);
					 * for (int i=0;i<byteBuffer.capacity();i++){
					 * if (byteBuffer.get(i)>0)
					 * Utils.log("Found non-0 at: "+i+" value "+byteBuffer.get(i),LogLevel.VERBOSE_VERBOSE_VERBOSE_DEBUG
					 * );
					 * }
					 */

					if ((inputOrOutputName == null) || inputOrOutputName.equals("Default destination")) {
						if (outputImage.getPixelType() == PixelType.FLOAT_TYPE)
							transferBuffer.get(outputImage.getPixels(index, 0.0f));
						else
							byteBuffer.get(outputImage.getPixels(index, (byte) 0));
					}
					// If we were passed a name, need to find a reference in auxiliary inputs or outputs
					// For now, assume that the image will already be open and have its pixels available.
					// In the future, make it possible to load things on demand, for example when we're just
					// given a path to a TIFF on disk.
					else
						transferBuffer.get(getAuxInputOrOutputPixels(inputOrOutputName, index, false));
				} else {
					if (setPixelHook != null) {
						setPixelHook.run(index, byteBuffer);
						byteBuffer.clear();
					}
					ImageAccessor image = null;
					if ((outputImage != null)
							&& ((inputOrOutputName == null) || inputOrOutputName.equals("Default destination")))
						image = (ImageAccessor) outputImage;
					else
						image = ((ImageAccessor) getAuxInputOrOutputImp(inputOrOutputName, false));
					image.dumpLittleEndianFloatBufferIntoSlice(byteBuffer, index);
				}
			} else {
				transferBuffer.clear();
				float[] destination;
				if ((inputOrOutputName == null) || inputOrOutputName.equals("Default destination"))
					destination = outputImage.getPixels(index, 0.0f);
				else
					destination = getAuxInputOrOutputPixels(inputOrOutputName, index, true);
				float[] transferBufferCopy = new float[(roi.x1 - roi.x0) * (roi.y1 - roi.y0)];
				transferBuffer.get(transferBufferCopy, 0, transferBufferCopy.length);
				int i = 0;
				// TODO we're assuming that all images have the same width
				for (int x = roi.x0; x <= roi.x1; x++) {
					for (int y = roi.y0; y <= roi.y1; y++) {
						destination[x + y * inputImage.getWidth()] = transferBufferCopy[i++];
					}
				}
			}
		} catch (ImageNotFound e) {
			if (!suppressDestinationNotFoundWarning) {
				Utils.printStack(e);
			}
		} catch (Exception e) {
			Utils.log("Error while trying to write slice " + index + "; output stack has " + outputImage.getDepth(),
					LogLevel.ERROR);
			Utils.printStack(e, LogLevel.ERROR);
			returnValue = 1;
		}
	}	;

		callback.setPixel = (index, inputOrOutputName, x, y, cachePolicy, multiThreadedIO, value) -> {
			try {
				if (outputImage.isSupportsWritingToPixels()) {
					if ((inputOrOutputName == null) || inputOrOutputName.equals("Default destination"))
						outputImage.setPixelValue(x, y, index, value);
					// If we were passed a name, need to find a reference in auxiliary inputs or outputs
					// For now, assume that the image will already be open and have its pixels available.
					// In the future, make it possible to load things on demand, for example when we're just
					// given a path to a TIFF on disk.
				else
					getAuxInputOrOutputPixels(inputOrOutputName, index, false)[x + y * inputImage.getWidth()] = value;
			} else {
				ImageAccessor image = null;
				if ((inputOrOutputName == null) || inputOrOutputName.equals("Default destination"))
					image = ((ImageAccessor) outputImage);
				else
					image = (ImageAccessor) getAuxInputOrOutputImp(inputOrOutputName, false);
				SettableBoolean changesNeedExplicitSaving = new SettableBoolean();
				float[] pixels =
						(float[]) image.getReferenceToPixelZSlice(index, cachePolicy, true, changesNeedExplicitSaving);
				pixels[x + y * inputImage.getWidth()] = value;
				if (changesNeedExplicitSaving.value)
					throw new RuntimeException("Change saving not implemented in set pixel");
			}
		} catch (Exception e) {
			Utils.printStack(e);
			returnValue = 1;
		}
	}	;

		callback.getProtobufMetadata = (inputOrOutputName, buffer, dataSize) -> {
			try {
				byte[] protobuf;
				if (inputOrOutputName == null) {
					Utils.log("*** Passing an image as protobuf", LogLevel.WARNING);
					protobuf = input.asProtobufBytes();
				} else {
					IPluginIO imp = getAuxInputOrOutputImp(inputOrOutputName, true);
					if (imp instanceof PluginIOCells)
						if (((PluginIOCells) imp).getWidth() == 0)
							Utils.log("*** Passing width of 0 to plugin", LogLevel.WARNING);
					// TODO IGNORING MULTIPLE CHANNELS FOR NOW
				protobuf = imp.asProtobufBytes();

				/*
				 * SegDirectory segDir=SegDirectory.parseFrom(protobuf);
				 * Utils.log("Sending back protobuf with width "+segDir.getImageDimx()+" and number of entries="+segDir.
				 * getProtobufInfoCount()+
				 * " last entry id: "+segDir.getProtobufInfo(segDir.getProtobufInfoCount()-1).getIdx(),LogLevel.
				 * VERBOSE_DEBUG);
				 */

			}

			// FileOutputStream fos = new FileOutputStream("last_protobuf_passed_to_native_library.bin");
			// fos.write(protobuf);
			// fos.close();
			int sizeToWrite = protobuf.length;
			if (sizeToWrite > dataSize.getValue())
				throw new RuntimeException("Protobuf array too small: need to write " + protobuf.length
						+ " but buffer allocated by dynamic library is only " + dataSize.getValue());
			buffer.write(0L, protobuf, 0, sizeToWrite);
			/*
			 * for (int i=0;i<sizeToWrite;i++){
			 * if (buffer.getByte(i)!=protobuf[i])
			 * throw new RuntimeException("Written buffer does not match input");
			 * }
			 */
			dataSize.setValue(sizeToWrite);
			Utils.log("passing back " + sizeToWrite + " bytes of protobuf file", LogLevel.DEBUG);
		} catch (Exception e) {
			Utils.printStack(e);
			returnValue = 1;
			dataSize.setValue(-1);
			return;
		}
	}	;

		callback.setProtobufMetadata = (metadata, dataSize, inputOrOutputName) -> {
			try {
				byte[] dataAsBytes = metadata.getByteArray(0, dataSize);
				Utils.log("read " + dataSize + " bytes", LogLevel.VERBOSE_DEBUG);

				// Encode with Base64 so the binary protobuf data can be stored with the rest of the
				// pipeline metadata as XML
				// Need to deal with the situation where metadata is not stored in imp ImageDescription but directly in
				// table
				if (inputOrOutputName == null) {
					input.setProperty("Protobuf", dataAsBytes);
				} else {
					IPluginIO imp = getAuxInputOrOutputImp(inputOrOutputName, false);
					// TODO IGNORING MULTIPLE CHANNELS FOR NOW
					imp.setProperty("Protobuf", dataAsBytes);
				}
				// FileOutputStream fos = new FileOutputStream("last_protobuf_passed_from_native_library.bin");
				// fos.write(dataAsBytes);
				// fos.close();
			} catch (Exception e) {
				Utils.printStack(e);
			}
		};

		callback.shouldInterrupt = () -> {
			boolean result = Thread.interrupted() || interruptPlugin || terminateFlag;
			if (result) {
				Utils.log("Telling C plugin to interrupt", LogLevel.DEBUG);
				interruptPlugin = false;
			}
			return result;
		};

		callback.progressReport = progressOutOf100 -> {
			try {
				progress.setValueThreadSafe(progressOutOf100);
			} catch (Exception e) {
				Utils.printStack(e);
			}
		};

		callback.progressSetIndeterminate = indeterminate -> {
			try {
				progress.setIndeterminate(indeterminate);
			} catch (Exception e) {
				Utils.printStack(e);
			}
		};

		callback.log = Utils::log;

		callback.printCharacters = (array, size) -> Utils.log(array.getString(size), LogLevel.CRITICAL);

		callback.freeGetMoreWork = work -> {
			// don't do anything; this callback is only meant to do something on the C command-line side
			};

		callback.logThreshold = Utils.logLevelThreshold;

	}

	private static SimpleImageDimensions getImageDimensions(IPluginIOHyperstack stack) {
		SimpleImageDimensions dim = new SimpleImageDimensions();
		dim.width = stack.getWidth();
		dim.height = stack.getHeight();
		Utils.log("Returning image info to C plugin: width=" + dim.width + ", height=" + dim.height,
				LogLevel.VERBOSE_VERBOSE_VERBOSE_DEBUG);
		dim.depth = stack.getDepth();
		dim.time = 1;
		dim.channels = stack.getnChannels();
		return dim;
	}

	private transient IPluginIOStack inputImage, outputImage;

	/**
	 * Used to move slices of float pixels back and forth between Java and the dynamic library. transferBuffer is not
	 * allocated directly but is just a view of byteBuffer.
	 * If one day it becomes possible to create a float [] view of the buffer (not implemented as of Sun's JVM 1.6), it
	 * would make sense to allocate the whole stack "directly" (outside of the Java heap) so the dynamic library
	 * can freely read and write to it without locking things down with GetPrimitiveArrayCritical.
	 */
	private transient FloatBuffer transferBuffer;

	/**
	 * Buffer allocated "directly" outside of the Java heap, used for efficient transfers by JNA that do not require
	 * copying the data.
	 */
	private transient ByteBuffer byteBuffer;

	private boolean keepAlive;

	/**
	 * Thread that makes the call to the dynamic library. We create a new thread so the "run" call can return
	 * while the dynamic library computes, and stays alive, poised for more computations that do not require
	 * the input data to be read in slice by slice all over again.
	 */
	private transient Thread callThread;

	private volatile transient boolean isAlive = false;

	/**
	 * Set to true when the pipeline requested the current computation be cancelled; read by the shouldInterrupt
	 * callback
	 * the dynamic library makes at regular intervals to check whether it should cancel its current computation.
	 * The plugin should stay alive and block at a getMoreWork() call.
	 */
	private volatile transient boolean interruptPlugin = false;
	/**
	 * Set to true if we want to terminate the call to the dynamic library once it is done with its current computation.
	 * This is checked by the call thread at regular intervals; if true, the getMoreWork() call returns with null
	 * so the dynamic library knows to terminate.
	 */
	private volatile transient boolean terminateFlag = false;

	/**
	 * Used to notify that the library has finished its computations.
	 */
	private transient Object isComputingSemaphore;

	private transient static NativeLibrary libraryInstance;

	/**
	 * Blocks until work is available, and returns a list of C-style strings containing arguments.
	 * Returns NULL if the plugin should terminate.
	 */
	public static interface GetMoreWork extends Callback {
		String[] invoke();
	}

	/**
	 * Returns the dimensions of input or output stack specified by inputOrOutputName.
	 * Returns null if input or output could not be found.
	 */
	public static interface GetDimensions extends Callback {
		SimpleImageDimensions invoke(String inputOrOutputName);
	}

	/**
	 * Copy the contents of transferBuffer (passed as first argument to "run") to output slice of index sliceIndex
	 * (indexing begins at 0). The output is automatically allocated by the Java end. If inputOrOutputName is null,
	 * write to the default output of the plugin. If not null, write to the designated stack.
	 * If roi is not null, only the rectangle it designates is copied.
	 * If multiThreaded IO is 1, writing to disk will be handled by the Java pipeline in a separate thread, and the
	 * call to setPixels will return before the data is actually written. This should lead to a significant performance
	 * gain if
	 * a sufficiently long computation is performed by the C plugin between each call to setPixels. If all slices are
	 * written in a burst, use multithreadedIO=0 to avoid incurring the cost of synchronization.
	 * cachePolicy settings are defined in ImageAccessor: NO_CACHE_PREFERENCE, CACHE_PIXELS, DONT_CACHE_PIXELS.
	 */
	public static interface SetPixels extends Callback {
		void invoke(int sliceIndex, String inputOrOutputName, BasicROI roi, int cachePolicy, int multithreadedIO);
	}

	public static interface SetPixel extends Callback {
		void invoke(int sliceIndex, String inputOrOutputName, int x, int y, int cachePolicy, int multithreadedIO,
				float value);
	}

	/**
	 * Copy the contents of input slice sliceIndex to transferBuffer (passed as first argument to "run").
	 * Indexing begins at 0. If inputOrOutputName is null, read from the default input of the plugin. If not null,
	 * read from the designated stack.
	 * If roi is not null, only the rectangle it designates is copied.
	 * cachePolicy settings are defined in ImageAccessor: NO_CACHE_PREFERENCE, CACHE_PIXELS, DONT_CACHE_PIXELS.
	 */
	public static interface GetPixels extends Callback {
		void invoke(int index, String inputOrOutputName, BasicROI roi, int cachePolicy);
	}

	/**
	 * Returns protobuf metadata associated with the input; note that the data can have null characters, and does not
	 * behave as a
	 * proper C string, which is why we're using a structure that contains size information. If inputOrOutputName is
	 * null,
	 * read from the default input of the plugin. If not null, read from the designated stack.
	 */

	public static interface GetProtobufMetadata extends Callback {
		void invoke(String inputOrOutputName, Pointer buffer, IntByReference bufferSize);
	}

	/**
	 * Set protobuf metadata associated with the output. If inputOrOutputName is null,
	 * write to the default output of the plugin. If not null, write to the designated stack.
	 */
	public static interface SetProtobufMetadata extends Callback {
		void invoke(Pointer metadata, int dataSize, String inputOrOutputName);
	}

	/**
	 * Returns true if the user or pipeline would like the plugin to interrupt its current computation, and perform a
	 * new
	 * getMoreWork() call. This should be polled at regular intervals (e.g. after computing each slice of a stack).
	 */
	public static interface ShouldInterrupt extends Callback {
		boolean invoke();
	}

	/**
	 * Set the progress displayed to the user to doneOutOf100 (between 0 and 100). It is crucial that this is called
	 * at regular intervals to give good feedback to the user. If it is not called often enough for tasks that take a
	 * long
	 * time to complete, the user might think that the program is stuck.
	 */
	public static interface ProgressReport extends Callback {
		void invoke(int progress);
	}

	/**
	 * Sets the GUI progress bar to the indeterminate mode (where it does not show a specific progress value) if
	 * parameter is true.
	 */
	public static interface ProgressSetIndeterminate extends Callback {
		void invoke(boolean indeterminate);
	}

	/**
	 * Log the passed string to the ImageJ log window. Note that anything printed directly to standard output or error
	 * will not
	 * be visible to the user.
	 * Log levels:
	 * 0: critical
	 * 1: error
	 * 2: warning
	 * 3: info
	 * 4: debug
	 * 5: verbose debug
	 * 6: verbose verbose debug
	 * 7: .....
	 */

	public static interface Log extends Callback {
		void invoke(String message, int logLevel);
	}

	public static interface PrintCharacters extends Callback {
		void invoke(Pointer message, int size);
	}

	public static interface GetDimensionsByRef extends Callback {
		int invoke(String inputOrOutputName, IntByReference x, IntByReference y, IntByReference z, IntByReference t,
				IntByReference c);
	}

	public static interface SetDimensions extends Callback {
		int invoke(String inputOrOutputName, int x, int y, int z, int t, int c);
	}

	public static interface FreeGetMoreWork extends Callback {
		void invoke(String[] work);
	}

	/**
	 * This structure contains the callback functions that the library plugin can use to get its input pixels, store its
	 * output pixels,
	 * and more generally communicate with the pipeline.
	 * DO NOT edit or change the ordering of this structure without updating the C structure to exactly match it (and
	 * other structures
	 * defined for plugins written in any other language).
	 * Otherwise the program will likely crash and take down the JVM along with it, without generating any informative
	 * error messages.
	 */
	public class CallbackFunctions extends Structure implements Callback {
		public CallbackFunctions() {
			Native.setCallbackThreadInitializer(this, new CallbackThreadInitializer(false));
		}

		public GetMoreWork getMoreWork;
		public GetDimensions getDimensions;
		public GetDimensionsByRef getDimensionsByRef;
		public SetDimensions setDimensions;
		public SetPixels setPixels;
		public SetPixel setPixel;
		public GetPixels getPixels;
		public GetProtobufMetadata getProtobufMetadata;
		public SetProtobufMetadata setProtobufMetadata;
		public ShouldInterrupt shouldInterrupt;
		public ProgressReport progressReport;
		public ProgressSetIndeterminate progressSetIndeterminate;
		public Log log;
		public PrintCharacters printCharacters;
		public final int pipelineVersion = 1;
		public FreeGetMoreWork freeGetMoreWork;
		public int logThreshold;

		@SuppressWarnings("rawtypes")
		@Override
		protected List getFieldOrder() {
			return Arrays.asList("getMoreWork", "getDimensions", "getDimensionsByRef", "setDimensions", "setPixels",
					"setPixel", "getPixels", "getProtobufMetadata", "setProtobufMetadata", "shouldInterrupt",
					"progressReport", "progressSetIndeterminate", "log", "printCharacters", "pipelineVersion",
					"freeGetMoreWork", "logThreshold");
		}
	}

	public interface NativeLibrary extends Library {
		public int run(ByteBuffer byteBuffer, SimpleImageDimensions dim, CallbackFunctions callbacks, String... arg);
	}

	/**
	 * List of String arguments to be passed to the dynamic library next time it calls getMoreWork().
	 * The call thread waits on this object to know when it should wake up because more work has become available.
	 */
	transient private ArrayList<String> workQueue = new ArrayList<>();

	private transient ProgressReporter progress;

	private transient CallbackFunctions callback = new CallbackFunctions();

	private int returnValue;

	@Override
	public final void establish(final List<String> programNameAndArguments, SpecialDimPlugin dimensionAccessor)
			throws InterruptedException {
		returnValue = 0;
		transferBuffer = null;
		if ((callThread != null) && callThread.isAlive())
			throw new RuntimeException("Thread exists: JNA call already established");

		if ((outputImage != null) && (!(outputImage.getPixelType() == PixelType.FLOAT_TYPE))) {
			Utils.log("External library called on a non-float image", LogLevel.INFO);
		}

		final int width, height, depth;
		final PixelType pixel;
		int nPixelsInSlice = 0;
		if (dimensionAccessor != null) {
			width = dimensionAccessor.getOutputWidth(inputImage);
			height = dimensionAccessor.getOutputHeight(inputImage);
			depth = dimensionAccessor.getOutputDepth(inputImage);
			pixel = dimensionAccessor.getOutputPixelType(inputImage);
			if (width * height == 0) {
				throw new IllegalArgumentException("Dimensions of input image not read correctly: their product is 0.");
			}
			nPixelsInSlice = width * height;
		} else if (inputImage != null) {
			width = inputImage.getWidth();
			height = inputImage.getHeight();
			depth = inputImage.getDepth();
			pixel = inputImage.getPixelType();
			if (width * height == 0) {
				throw new IllegalArgumentException("Dimensions of input image not read correctly: their product is 0.");
			}
		} else {
			width = 1;
			height = 1;
			pixel = null;
			depth = 1;
		}

		// Take the biggest number of pixels we can find to set the size of the transfer buffer
		if (inputImage != null && inputImage.getWidth() * inputImage.getHeight() > nPixelsInSlice) {
			nPixelsInSlice = inputImage.getWidth() * inputImage.getHeight();
		} else {
			nPixelsInSlice = Math.max(nPixelsInSlice, width * height);
		}

		final int pixelSize = pixel == PixelType.BYTE_TYPE ? 1 : 4;

		byteBuffer = ByteBuffer.allocateDirect(pixelSize * nPixelsInSlice);
		byteBuffer.order(java.nio.ByteOrder.nativeOrder());
		transferBuffer = byteBuffer.asFloatBuffer();

		if (isComputingSemaphore == null)
			isComputingSemaphore = new Object();

		if (inputImage != null)
			inputImage.computePixelArray();
		if (outputImage != null)
			outputImage.computePixelArray();

		isAlive = true;
		interruptPlugin = false;
		keepAlive = true;

		final ArrayList<String> nullTerminatedArguments = new ArrayList<>(programNameAndArguments);
		nullTerminatedArguments.add(new String(new byte[] { 0 }));

		callThread = new Thread("Thread for library call " + Utils.printStringArray(programNameAndArguments)) {
			@Override
			public void run() {
				try {
					SimpleImageDimensions dim = new SimpleImageDimensions();
					dim.width = width;
					dim.height = height;
					dim.depth = depth;
					dim.time = 1;
					if (byteBuffer == null)
						throw new RuntimeException("Null byteBuffer");
					callback.logThreshold = Utils.logLevelThreshold;
					Utils.log("Calling run function of external library for "
							+ Utils.printStringArray(programNameAndArguments), LogLevel.DEBUG);
					int result =
							libraryInstance.run(byteBuffer, dim, callback, nullTerminatedArguments
									.toArray(new String[0]));
					returnValue += result;
					Utils.log("-------- External library call returned", LogLevel.VERBOSE_DEBUG);
				} catch (Exception e) {
					Utils.printStack(e);
					returnValue = 1;
				} finally {
					isAlive = false;
					isComputing = false;
					synchronized (isComputingSemaphore) {
						isComputingSemaphore.notifyAll();
					}					
				}
			}
		};
		callThread.start();
	}

	@Override
	public void interrupt() {
		Utils.log("Interrupting C plugin", LogLevel.DEBUG);
		interruptPlugin = true;
	}

	/**
	 * Returns as soon as the plugin signals that it is done with its current computation by calling getMoreWork()
	 */
	private void waitUntilComputationDone() {
		Utils.log("Waiting for computation to be done", LogLevel.VERBOSE_DEBUG);
		synchronized (isComputingSemaphore) {
			while ((isComputing) && (isAlive)) {
				try {
					isComputingSemaphore.wait();
				} catch (InterruptedException e) {
					interrupt();
				}
			}
		}
		Utils.log("Computation done", LogLevel.VERBOSE_DEBUG);
	}

	private void waitUntilDead() {
		Utils.log("Waiting for plugin link to be dead", LogLevel.VERBOSE_DEBUG);
		synchronized (isComputingSemaphore) {
			while (callThread != null && callThread.isAlive()) {
				try {
					// Need a timeout because we don't get a notification from
					// isComputingSemaphore when callThread finishes and dies
					// It's probably not useful to use isComputingSemaphore here
					isComputingSemaphore.wait(500);
				} catch (InterruptedException e) {
					interrupt();
				}
			}
		}
		Utils.log("Plugin link now dead", LogLevel.VERBOSE_DEBUG);
	}

	@Override
	public final void run(String[] arguments, boolean keepAlive, boolean blockUntilDone, ProgressReporter progress,
			SpecialDimPlugin dimensionAccessor) {
		if ((arguments == null) || (arguments.length == 0))
			throw new IllegalArgumentException("Need at least one argument to pass to external library");
		this.progress = progress;
		interruptPlugin = false;
		this.keepAlive = keepAlive;

		if (callThread == null) {
			throw new IllegalStateException("Null call thread");
		}
		if (!callThread.isAlive()) {
			throw new IllegalStateException("Call thread is dead");
		}

		callback.logThreshold = Utils.logLevelThreshold;

		Utils.log("Waiting for computing semaphore " + Utils.printStringArray(arguments), LogLevel.VERBOSE_DEBUG);

		synchronized (isComputingSemaphore) {
			Utils.log("Done waiting for computing semaphore " + Utils.printStringArray(arguments),
					LogLevel.VERBOSE_DEBUG);
			for (String argument : arguments) {
				workQueue.add(argument);
			}
			isComputing = true;
			if ((callThread == null) || !callThread.isAlive()) {
				throw new IllegalStateException("Worker thread not alive");
			}
			Utils.log("Notifying worker thread " + Utils.printStringArray(arguments), LogLevel.VERBOSE_DEBUG);
			isComputingSemaphore.notifyAll();
		}

		// If not using getMoreWork, computation will be performed directly in response to establish() call

		if (blockUntilDone) {
			waitUntilComputationDone();
		}

		if (!keepAlive) {
			Utils.log("Terminating worker thread for external library call", LogLevel.VERBOSE_DEBUG);
			terminateFlag = true;
			synchronized (isComputingSemaphore) {
				isComputingSemaphore.notifyAll();
			}
			Utils.log("Terminated external library", LogLevel.VERBOSE_DEBUG);
		} else {
			Utils.log("Not terminating worker thread for external library call", LogLevel.VERBOSE_DEBUG);
		}

		// Reset returnValue to 0 for next call; not safe to do at start of this method because
		// run might already have completed with an error (if it did not use getMoreWork)
		int saveReturnValue = returnValue;
		returnValue = 0;
		if (saveReturnValue != 0)
			throw new RuntimeException("External plugin returned " + saveReturnValue);

	}

	@Override
	public final boolean stillAlive() {
		return (isAlive && (callThread != null) && callThread.isAlive());
	}

	@Override
	public final void terminate(boolean blockUntilTerminated) {
		blockUntilTerminated = true;
		terminateFlag = true;

		if (!stillAlive()) {
			Utils.log("Plugin to terminate is not running", LogLevel.DEBUG);
		} else {
			synchronized (isComputingSemaphore) {
				isComputingSemaphore.notifyAll();
			}
			if (blockUntilTerminated) {
				waitUntilDead();
			}
		}
		inputImage = null;// to release the memory
		outputImage = null;// to release the memory
		sources = null;
		destinations = null;
		transferBuffer = null;
		byteBuffer = null;
		callThread = null;

		// callback=null;//THIS IS ONLY SAFE IF FROM THIS POINT PLUGIN
		// NEVER USES THE CALLBACK AGAIN
		progress = null;
		workQueue = null;
	}

	// @SuppressWarnings("deprecation")
	@Override
	public final void terminateForcibly() {
		terminate(true);
	}

}
