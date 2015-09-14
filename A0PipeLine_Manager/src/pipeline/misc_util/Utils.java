/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.misc_util;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.FileInfo;
import ij.io.FileOpener;
import ij.io.TiffDecoder;
import ij.plugin.FileInfoVirtualStack;
import ij.text.TextPanel;

import java.awt.Color;
import java.awt.Frame;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.nio.channels.ClosedByInterruptException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import ncsa.util.ReaderTokenizer;

import org.apache.commons.lang3.text.WordUtils;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.jfree.data.xy.XYSeries;
import org.xml.sax.InputSource;

import pipeline.A0PipeLine_Manager;
import pipeline.data.IPluginIOHyperstack;
import pipeline.data.IPluginIOStack;
import pipeline.data.PluginIOCells;
import pipeline.data.PluginIOImage.PixelType;

import com.sun.jna.Callback;
import com.sun.jna.Callback.UncaughtExceptionHandler;
import com.thoughtworks.xstream.XStream;

/**
 * Various utilities, used mainly by the pipeline and parameters.
 *
 */
public class Utils {

	public static final String fileNameSeparator = "/";

	public static final String DEFAULT_CHANNEL_NAME_WHEN_ONLY_1_CHANNEL = "Ch 0"; // changing this will break some
																					// things

	public static final Color COLOR_FOR_EVEN_ROWS = new Color(255, 255, 200);
	public static final Color COLOR_FOR_SELECTED_ROWS = new Color(200, 200, 255);
	public static final Color COLOR_FOR_ODD_ROWS = new Color(230, 230, 255);

	public static final int MAXIMAL_BUTTON_HEIGHT = 15;

	public static int logLevelThreshold = 7;

	public static final String[] LogLevelNames = new String[] { "CRITICAL", "ERROR", "WARNING", "INFO", "DEBUG" };

	public static final class LogLevel {
		public static final int CRITICAL = 0, ERROR = 1, WARNING = 2, INFO = 3, DEBUG = 4;
	}

	private static FileWriter writer;

	public static boolean skipListsInTableTextExport = true;

	private static boolean failedToOpenLogFile = false;// will be set to true if e.g. log file is not writable

	private static String logFileName = null;

	private static boolean prependTime = false;

	public static void setPrependTime(boolean on) {
		Utils.prependTime = on;
	}

	private static final ThreadLocal<SimpleDateFormat> dateFormatter = new ThreadLocal<SimpleDateFormat>() {
		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat("E dd MMM yy HH:mm:ss");
		}
	};

	public static void setLogFile(String logFile) {
		if (writer != null) {
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			writer = null;
			failedToOpenLogFile = false;
		}
		logFileName = logFile;
		if (logFile == null) {
			failedToOpenLogFile = true;
		}
	}

	private static String logPrefix = null;

	public static void setLogPrefix(String prefix) {
		Utils.logPrefix = prefix;
	}

	private static boolean copyLogMessagesToStdOut = true;

	public static void setCopyLogMessagesToStdOut(boolean on) {
		Utils.copyLogMessagesToStdOut = on;
	}

	private static void log(final String message, int logLevel, boolean suppressTime) {
		if (suppressLog || (logLevel > logLevelThreshold))
			return;

		final String message2 = logPrefix != null ? logPrefix + message : message;

		final String finalMessage =
				prependTime && !suppressTime ? dateFormatter.get().format((new Date())) + " " + message2 : message2;

		try {
			if ((!failedToOpenLogFile) && (writer == null)) {
				if (logFileName == null) {
					System.err.println("Warning: log file name not set; using default");
					logFileName = A0PipeLine_Manager.getBaseDir() + "/pipeline_log.txt";
				}
				try {
					writer = new FileWriter(new File(logFileName), true);
				} catch (Exception e) {
					failedToOpenLogFile = true;
					if (headless) {
						System.err.println("Exception while trying to open log file: " + e);
					} else {
						IJ.log("Exception while trying to open log file: " + e);
					}
				}
			}
			if (writer != null) {
				writer.write(finalMessage + "\n");
				writer.flush();
			} else {
				copyLogMessagesToStdOut = true;
			}
		} catch (IOException e) {
			if (headless) {
				System.err.println("Exception while logging " + finalMessage);
			} else {
				IJ.log("Exception while logging " + finalMessage);
			}
			Utils.printStack(e);
		}

		if (copyLogMessagesToStdOut) {
			if (headless) {
				System.err.println(finalMessage);
			} else
				SwingUtilities.invokeLater(() -> {
					IJ.log(finalMessage);
					if (logLevel <= LogLevel.WARNING) {
						try {
							Field logPanelField = IJ.class.getDeclaredField("logPanel");
							logPanelField.setAccessible(true);
							TextPanel panel = (TextPanel) logPanelField.get(null);
							((Frame) panel.getParent()).toFront();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
		}

	}

	/**
	 * Log the passed string to the ImageJ log window.
	 * 
	 * @param logLevel
	 *            Only log if logLevel smaller than current threshold.
	 *            Log levels:
	 *            0: critical
	 *            1: error
	 *            2: warning
	 *            3: info
	 *            4: debug
	 *            5: verbose debug
	 *            6: verbose verbose debug
	 *            7: .....
	 */
	public final static void log(final String message, int logLevel) {
		log(message, logLevel, false);
	}

	public static class ImageOpenFailed extends Exception {

		private static final long serialVersionUID = 1L;

		public ImageOpenFailed(String s) {
			super(s);
		}

		public ImageOpenFailed(String s, Throwable cause) {
			super(s, cause);
		}
	}

	public static void openVirtualTiff(File file, ImagePlus image, boolean displayErrorMessage) throws ImageOpenFailed {
		TiffDecoder td = new TiffDecoder(file.getParent(), file.getName());

		@SuppressWarnings("null")
		String shortName = FileNameUtils.compactPath(file.getAbsolutePath());

		FileInfo[] fiArray = null;
		try {
			fiArray = td.getTiffInfo();
		} catch (IOException e) {
			if (displayErrorMessage)
				Utils.displayMessage("Error opening tiff " + shortName, false, LogLevel.ERROR);
			throw new ImageOpenFailed("Error opening virtual TIFF file " + shortName, e);
		}

		if (fiArray == null || fiArray.length == 0) {
			if (displayErrorMessage)
				Utils.displayMessage("Error opening " + shortName + ": this does not appear to be a valid TIFF stack",
						true, LogLevel.ERROR);
			throw new ImageOpenFailed("Error opening virtual TIFF file " + shortName);
		}

		FileInfoVirtualStack fivs = new FileInfoVirtualStack();
		FileOpener.setSilentMode(true);
		fivs.info = fiArray;
		fivs.openButDontShow(image);
	}

	public static String chopOffString(String s, int maxLength) {
		if (s == null)
			return "";
		if (s.length() <= maxLength) {
			return s;
		} else {
			return s.substring(0, min(maxLength, s.length())) + "...";
		}
	}

	public static String chopOffStringBeginning(String s, int maxLength) {
		if (s == null)
			return "";
		if (s.length() <= maxLength) {
			return s;
		} else {
			return "..." + s.substring(max(s.length() - maxLength, 0));
		}
	}

	public static int min(int a, int b) {
		if (a < b)
			return a;
		else
			return b;
	}

	public static int max(int a, int b) {
		if (a > b)
			return a;
		else
			return b;
	}

	// From http://forums.thedailywtf.com/forums/p/2806/72054.aspx#72054
	public static String encodeHTML(String s) {
		StringBuffer out = new StringBuffer("<html>");
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c > 127 || c == '"' || c == '<' || c == '>') {
				out.append("&#" + (int) c + ";");
			} else {
				out.append(c);
			}
		}
		out.append("</html>");
		return out.toString();
	}

	public static void displayMessage(final String s, boolean logMessage, final int logLevel) {
		Utils.log("Displaying message: " + s, LogLevel.ERROR);
		if ((!headless) && (!suppressWarningPopups))
			SwingUtilities.invokeLater(() -> {
				JLabel textLabel = new JLabel(encodeHTML(WordUtils.wrap(s, 50)).replace("\n", "<br>\n"));
				int level;
				if (logLevel >= LogLevel.INFO)
					level = JOptionPane.INFORMATION_MESSAGE;
				else if (logLevel == LogLevel.WARNING)
					level = JOptionPane.WARNING_MESSAGE;
				else
					level = JOptionPane.ERROR_MESSAGE;
				JOptionPane optionPane = new JOptionPane();
				optionPane.setMessageType(level);
				optionPane.setMessage(textLabel);
				JDialog dialog = optionPane.createDialog("Pipeline message");
				dialog.setAlwaysOnTop(true);
				dialog.setVisible(true);
				// JOptionPane.showMessageDialog(null, textLabel,"Pipeline message",level);
				});
	}

	/**
	 * True if pipeline is not attached to a GUI.
	 */
	public static boolean headless;

	public static boolean suppressLog;

	private static boolean VNCScrollWheel = false;

	public static UncaughtExceptionHandler callbackExceptionHandler = new UncaughtExceptionHandler() {

		@Override
		public void uncaughtException(Callback arg0, Throwable arg1) {
			Utils.log("Uncaught exception in JNA callback " + arg0.toString(), LogLevel.ERROR);
			Utils.printStack(arg1);
		}
	};

	public static boolean suppressWarningPopups = false;

	public static float getMouseWheelClickFactor() {
		if (VNCScrollWheel)
			return 0.1f;
		else
			return 1f;
	}

	public static void add(float[] a, float[] b) {
		for (int i = 0; i < a.length; i++) {
			a[i] = a[i] + b[i];
		}
	}

	public static void subtract(float[] a, float[] b) {
		for (int i = 0; i < a.length; i++) {
			a[i] = a[i] - b[i];
		}
	}

	public static int indexOfMin(float[] a) {
		float min = Float.MAX_VALUE;
		int min_index = -1;
		for (int i = 0; i < a.length; i++) {
			if (a[i] < min) {
				min = a[i];
				min_index = i;
			}
		}
		return min_index;
	}

	public static int indexOfMax(float[] a) {
		float max = -Float.MAX_VALUE;
		int max_index = -1;
		for (int i = 0; i < a.length; i++) {
			if (a[i] > max) {
				max = a[i];
				max_index = i;
			}
		}
		return max_index;
	}

	public static float[] arrayAbs(float[] a) {
		float[] b = new float[a.length];
		for (int i = 0; i < a.length; i++) {
			b[i] = Math.abs(a[i]);
		}
		return b;
	}

	public static float findMax(float[] a) {
		float max = 0;
		for (float element : a) {
			if (element > max)
				max = element;
		}
		return max;
	}

	public static float findMax(short[] a) {
		float max = 0;
		for (short element : a) {
			if (((element & 0xffff)) > max)
				max = (element & 0xffff);
		}
		return max;
	}

	public static float findMax(byte[] a) {
		float max = 0;
		for (byte element : a) {
			if (((element & 0xff)) > max)
				max = (element & 0xff);
		}
		return max;
	}

	private static void findMinMax(byte[] a, MinMax m) {
		float max = -Float.MAX_VALUE;
		float min = Float.MAX_VALUE;
		for (byte element : a) {
			if (((element & 0xff)) > max)
				max = (element & 0xff);
			if (((element & 0xff)) < min)
				min = (element & 0xff);
		}
		m.max = max;
		m.min = min;
	}

	private static void findMinMax(short[] a, MinMax m) {
		float max = -Float.MAX_VALUE;
		float min = Float.MAX_VALUE;
		for (short element : a) {
			if (((element & 0xff)) > max)
				max = (element & 0xffff);
			if (((element & 0xff)) < min)
				min = (element & 0xffff);
		}
		m.max = max;
		m.min = min;
	}

	private static void findMinMax(float[] a, MinMax m) {
		float max = -Float.MAX_VALUE;
		float min = Float.MAX_VALUE;
		for (float element : a) {
			if (element > max)
				max = element;
			if (element < min)
				min = element;
		}
		m.max = max;
		m.min = min;
	}

	public static void updateRangeInStack(ImagePlus imp) {
		float max = -Float.MAX_VALUE;
		float min = Float.MAX_VALUE;
		int nSlices = imp.getNSlices();
		int nChannels = imp.getNChannels();
		MinMax m = new MinMax();
		for (int i = 1; i <= nSlices; i++) {
			Object pixels = imp.getStack().getPixels((i - 1) * nChannels + imp.getChannel());
			float sliceMax = 0;
			float sliceMin = 0;
			if (pixels instanceof float[]) {
				findMinMax((float[]) pixels, m);
			} else if (pixels instanceof byte[]) {
				findMinMax((byte[]) pixels, m);
			} else if (pixels instanceof short[]) {
				findMinMax((short[]) pixels, m);
			}
			sliceMax = m.max;
			sliceMin = m.min;
			if (sliceMax > max)
				max = sliceMax;
			if (sliceMin < min)
				min = sliceMin;
		}

		imp.setDisplayRange(min, max);
		imp.updateChannelAndDraw();
	}

	public static void updateRangeInRegularImp(ImagePlus imp) {
		Object pixels = imp.getProcessor().getPixels();
		MinMax m = new MinMax();
		if (pixels instanceof float[])
			findMinMax((float[]) pixels, m);
		else if (pixels instanceof byte[])
			findMinMax((byte[]) pixels, m);
		else if (pixels instanceof short[])
			findMinMax((short[]) pixels, m);
		imp.setDisplayRange(m.min, m.max);
		imp.updateChannelAndDraw();
	}

	public static String[] concatenateArrays(String[] array1, String[] array2) {
		if (array1 == null)
			return array2.clone();
		if (array2 == null)
			return array1.clone();
		String[] newArray = new String[array1.length + array2.length];
		System.arraycopy(array1, 0, newArray, 0, array1.length);
		System.arraycopy(array2, 0, newArray, array1.length, array2.length);
		return newArray;
	}

	public static int[] arraySubSlice(int[] array, int start, int stop) {
		int[] newArray = new int[stop - start + 1];
		for (int i = start; i <= stop; i++) {
			newArray[i - start] = array[i - start];
		}
		return newArray;
	}

	public static int[] indexesOf(String[] masterList, String[] elementsToSelect) {
		int[] result = new int[elementsToSelect.length];

		int resultIndex = 0;
		for (String element : elementsToSelect) {
			result[resultIndex] = Utils.indexOf(masterList, element);
			if (result[resultIndex] == -1) {
				Utils.log("Channel " + element + " not present in " + printStringArray(masterList), LogLevel.DEBUG);
			} else
				resultIndex++;
		}

		if (resultIndex < elementsToSelect.length) {
			// we didn't find some channels, so need to resize the array
			int[] newResult = new int[resultIndex];
			for (int i = 0; i < resultIndex; i++) {
				newResult[i] = result[i];
			}
			result = newResult;
		}

		return result;
	}

	private static float max(Object o) {
		if (o instanceof float[]) {
			float the_max = Float.MIN_VALUE;
			float[] fa = (float[]) o;
			for (float element : fa) {
				if (element > the_max)
					the_max = element;
			}
			return the_max;
		} else if (o instanceof short[]) {
			int the_max = Integer.MIN_VALUE;
			short[] fa = (short[]) o;
			for (short element : fa) {
				if ((element & 0xffff) > the_max)
					the_max = (element & 0xffff);
			}
			return the_max;
		} else if (o instanceof byte[]) {
			int the_max = Integer.MIN_VALUE;
			byte[] fa = (byte[]) o;
			for (byte element : fa) {
				if ((element & 0xff) > the_max)
					the_max = (element & 0xff);
			}
			return the_max;
		} else
			throw new RuntimeException("Pixel type not handled in max");
	}

	private static float min(Object o) {

		if (o instanceof float[]) {
			float the_min = Float.MAX_VALUE;
			float[] fa = (float[]) o;
			for (float element : fa) {
				if (element < the_min)
					the_min = element;
			}
			return the_min;
		} else if (o instanceof short[]) {
			int the_min = Integer.MAX_VALUE;
			short[] fa = (short[]) o;
			for (short element : fa) {
				if ((element & 0xffff) < the_min)
					the_min = (element & 0xffff);
			}
			return the_min;
		} else if (o instanceof byte[]) {
			int the_min = Integer.MAX_VALUE;
			byte[] fa = (byte[]) o;
			for (byte element : fa) {
				if ((element & 0xff) < the_min)
					the_min = (element & 0xff);
			}
			return the_min;
		} else
			throw new RuntimeException("Pixel type not handled in min");
	}

	public static float getMin(IPluginIOStack source) {
		if (source == null) {
			Utils.log("Source stack in null in getStackMax", LogLevel.WARNING);
			return 0;
		}
		float the_min = Float.MAX_VALUE;
		for (int i = 0; i < source.getDimensions().depth; i++) {
			float slice_min = (min(source.getPixels(i)));
			if (slice_min < the_min)
				the_min = slice_min;
		}
		return the_min;
	}

	public static float getMax(IPluginIOStack source) {
		if (source == null) {
			Utils.log("Source stack in null in getStackMax", LogLevel.WARNING);
			return 0;
		}
		float max = Float.MIN_VALUE;
		for (int i = 0; i < source.getDimensions().depth; i++) {
			float sliceMax = (max(source.getPixels(i)));
			if (sliceMax > max)
				max = sliceMax;
		}
		return max;
	}

	public static float getStackMax(ImagePlus source) {
		if (source == null) {
			Utils.log("Source stack in null in getStackMax", LogLevel.INFO);
			return 0;
		}
		ImageStack stack = source.getStack();
		float max = Float.MIN_VALUE;
		for (int i = 1; i <= stack.getSize(); i++) {
			float sliceMax = (max(stack.getProcessor(i).getPixels()));
			if (sliceMax > max)
				max = sliceMax;
		}
		return max;
	}

	public static float getStackMin(ImagePlus source) {
		if (source == null) {
			Utils.log("Source stack in null in getStackMin", LogLevel.INFO);
			return 0;
		}
		ImageStack stack = source.getStack();
		float min = Float.MAX_VALUE;
		for (int i = 1; i <= stack.getSize(); i++) {
			float sliceMin = (float) stack.getProcessor(i).getMin();
			if (sliceMin < min)
				min = sliceMin;
		}
		return min;
	}

	public static String printIntArray(int[] a) {
		StringBuilder r = new StringBuilder();
		for (int element : a) {
			r.append(element).append(", ");
		}
		return r.toString();
	}

	public static String printStringArray(String[] a) {
		StringBuilder r = new StringBuilder();
		for (String element : a) {
			r.append(element).append(", ");
		}
		return r.toString();
	}

	public static String printObjectArray(Object[] a) {
		StringBuilder r = new StringBuilder();
		for (Object element : a) {
			r.append(element).append(", ");
		}
		return r.toString();
	}

	public static String printStringArray(String[] a, String delimiter) {
		StringBuilder r = new StringBuilder();
		for (String element : a) {
			r.append(element).append(delimiter);
		}
		return r.toString();
	}

	public static String printStringArray(List<String> a) {
		StringBuilder r = new StringBuilder();
		for (String anA : a) {
			r.append(anA).append(", ");
		}
		return r.toString();
	}

	public static int indexOf(String[] a, String b) {
		if (a == null || b == null)
			return -1;
		for (int i = 0; i < a.length; i++) {
			if (b.equals(a[i]))
				return i;
		}
		return -1;
	}

	public static int indexOf(int[] a, int b) {
		if (a == null)
			return -1;
		for (int i = 0; i < a.length; i++) {
			if (b == a[i])
				return i;
		}
		return -1;
	}

	static public boolean isParsableToInt(String i) {
		try {
			Integer.parseInt(i);
			return true;
		} catch (NumberFormatException nfe) {
			return false;
		}
	}

	static public boolean isParsableToFloat(String i) {
		try {
			Float.parseFloat(i);
			return true;
		} catch (NumberFormatException nfe) {
			return false;
		}
	}

	static public int parseAndThrowRuntimeException(String i) {
		try {
			return Integer.parseInt(i);
		} catch (NumberFormatException nfe) {
			throw new RuntimeException("String " + i + " not parsable to int");
		}

	}

	static public void printStack(Throwable e, int logLevel) {
		if (suppressLog)
			return;
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);

		Utils.log("\n" + "\n" + sw.toString(), logLevel, true);// "\n"+"\n"+e.getMessage()+"\n\n"+e.toString()+
	}

	public static boolean causedByInterruption(Throwable e) {
		if (e instanceof PluginRuntimeException && ((PluginRuntimeException) e).unmaskable)
			return false;
		while (e != null) {
			if (e instanceof InterruptedException || e instanceof ClosedByInterruptException)
				return true;
			e = e.getCause();
		}
		return false;
	}

	static public void printStack(Throwable e) {
		printStack(e, LogLevel.CRITICAL);
	}

	static public int nSlices(ImagePlus imp) {
		boolean isHyperStack = imp.isHyperStack();

		int nSlices, nChannels;
		if (isHyperStack) {
			nChannels = imp.getNChannels();
			nSlices = imp.getStackSize() / nChannels;
		} else {
			nChannels = 1;
			nSlices = imp.getStackSize();
		}
		return nSlices;
	}

	static public String objectToXMLString(Object o) {
		XStream workonStream = new XStream(A0PipeLine_Manager.reflectionProvider);

		String w = null;
		if (o instanceof XYSeries)
			try {
				w = workonStream.toXML(((XYSeries) o).clone());// To remove listener that messes up deserialization
			} catch (CloneNotSupportedException e) {
				Utils.printStack(e);
			}
		else
			w = workonStream.toXML(o);

		return w;
	}

	static public Content objectToXML(Object o) {
		XStream workonStream = new XStream();
		String w = workonStream.toXML(o);
		// parse the XML string for insertion into our tree; that's a bit wasteful
		// is it possible to get an element instead of a string out of XStream?

		Document doc = null;
		try {
			SAXBuilder builder = new SAXBuilder();
			doc = builder.build(new InputSource(new StringReader(w)));
		} catch (Exception e) {
			// Utils.printStack(e);
			log("Failed to parse WORK_ON_CHANNEL_FIELD" + w, LogLevel.ERROR);
		}

		return (doc == null) ? null : doc.getRootElement().detach();

	}

	/**
	 * Reads a text file formatted by the GSL histogram outputting functions:
	 * range[0] range[1] bin[0]
	 * range[1] range[2] bin[1]
	 * range[2] range[3] bin[2]
	 * ....
	 * range[n-1] range[n] bin[n-1]
	 * 
	 * Writes the results as a pair of XY values into an XYSeries object from FreeChart,
	 * taking the lower end of the range for each pair
	 * 
	 * @param fileName
	 *            The path to the text file to read the data from
	 * @param series
	 *            The series object to write the results into
	 */
	public static void readHistogramIntoXYSeries(String fileName, XYSeries series) {
		final int nColumns = 3;
		// From http://www.java2s.com/Code/Java/File-Input-Output/ReadingNumbersfromaTextFile.htm
		try (Reader r = new BufferedReader(new FileReader(fileName))) {

			StreamTokenizer stok = new StreamTokenizer(r);
			stok.parseNumbers();
			int currentColumn = 0;
			double range0 = 0;
			stok.nextToken();

			while (stok.ttype != StreamTokenizer.TT_EOF) {
				if (stok.ttype == StreamTokenizer.TT_NUMBER) {
					if (currentColumn == 0) {
						range0 = stok.nval;
					} else if (currentColumn == 2) {
						series.add(range0, stok.nval);
					}
				} else
					Utils.log("Nonnumber while reading histogram from file " + fileName + ": " + stok.sval,
							LogLevel.ERROR);
				stok.nextToken();
				currentColumn++;
				if (currentColumn == nColumns)
					currentColumn = 0;
			}
		} catch (FileNotFoundException e) {
			throw new RuntimeException("Could not find file " + fileName + " from which to read histogram", e);
		} catch (IOException e) {
			printStack(e);
		}

	}

	/**
	 * Reads a text file formatted by the GSL histogram outputting functions:
	 * range[0] range[1] bin[0]
	 * range[1] range[2] bin[1]
	 * range[2] range[3] bin[2]
	 * ....
	 * range[n-1] range[n] bin[n-1]
	 * 
	 * Writes the results as a pair of XY values into an XYSeries object from FreeChart,
	 * taking the lower end of the range for each pair
	 * 
	 * @param plot
	 *            The plot produced by a plugin
	 * @param series
	 *            The series object to write the results into
	 */
	public static void readHistogramIntoXYSeries(PluginIOCells plot, XYSeries series) {
		final int nColumns = 3;
		// From http://www.java2s.com/Code/Java/File-Input-Output/ReadingNumbersfromaTextFile.htm
		byte[] protobufArray = (byte[]) plot.getProperty("Protobuf");
		if (protobufArray == null)
			return;
		Reader r = new StringReader(new String(protobufArray));
		StreamTokenizer stok = new StreamTokenizer(r);
		stok.parseNumbers();
		int currentColumn = 0;
		double range0 = 0;
		try {
			stok.nextToken();

			while (stok.ttype != StreamTokenizer.TT_EOF) {
				if (stok.ttype == StreamTokenizer.TT_NUMBER) {
					if (currentColumn == 0) {
						range0 = stok.nval;
					} else if (currentColumn == 2) {
						series.add(range0, stok.nval);
					}
				} else
					Utils.log("Nonnumber while reading histogram from string " + plot.getProperty("Protobuf") + ": "
							+ stok.sval, LogLevel.ERROR);
				stok.nextToken();
				currentColumn++;
				if (currentColumn == nColumns)
					currentColumn = 0;
			}
		} catch (IOException e) {
			printStack(e);
			return;
		}

	}

	public static String printPixelTypes(PixelType[] pixelTypes) {
		if (pixelTypes == null)
			return "";
		StringBuffer b = new StringBuffer(200);
		for (int i = 0; i < pixelTypes.length; i++) {
			b.append(pixelTypes[i]);
			if (i < pixelTypes.length - 1)
				b.append(", ");
		}
		return b.toString();
	}

	public static double mean(double[] array) {
		if (array.length == 0)
			throw new IllegalArgumentException("Cannot compute mean of empty array");
		double result = 0;
		for (double d : array) {
			result += d;
		}
		return result / array.length;
	}

	public static double median(double[] array) {
		if (array.length == 0)
			throw new IllegalArgumentException("Cannot compute median of empty array");
		double[] sortedArray = array.clone();
		Arrays.sort(sortedArray);
		return sortedArray[array.length / 2];
	}

	public static double stdDev(double[] array, Double mean) {
		if (array.length == 0)
			throw new IllegalArgumentException("Cannot compute stdev of empty array");
		double result = 0;
		if (mean == null)
			mean = mean(array);
		double localMean = mean;
		for (double d : array) {
			result += (d - localMean) * (d - localMean);
		}
		return Math.sqrt(result / array.length);
	}

	public static double confidence(double level, int n, double stdDev) {
		double multiplier = 0;
		if (level == 0.05) {
			multiplier = 1.96;
		} else if (level == 0.16) {
			multiplier = 1.405;
		} else
			throw new RuntimeException("Unimplemented confidence level");
		return multiplier * stdDev / (n);
	}

	public static float getMin(IPluginIOHyperstack imageSource) {
		float the_min = Float.MAX_VALUE;
		for (IPluginIOStack channel : imageSource.getChannels().values()) {
			float channelMinimum = getMin(channel);
			if (channelMinimum < the_min)
				the_min = channelMinimum;
		}
		return the_min;
	}

	public static float getMax(IPluginIOHyperstack imageSource) {
		float the_max = Float.MIN_VALUE;
		for (IPluginIOStack channel : imageSource.getChannels().values()) {
			float channelMaximum = getMax(channel);
			if (channelMaximum > the_max)
				the_max = channelMaximum;
		}
		return the_max;
	}

	public static void setVNCSettings(boolean selected) {
		VNCScrollWheel = selected;
	}

	public static void main(String[] args) throws IOException {// findStepsInProfile
		File profileFile = new File(args[0]);
		int columnIndex = Integer.parseInt(args[1]);
		int xColumnIndex = Integer.parseInt(args[2]);
		int nColumns = Integer.parseInt(args[3]);
		float windowLength = Float.parseFloat(args[4]);

		// FIXME Code duplicated in RegisterVideoLabelTimes plugin. Create a parsing class.
		try (Reader r = new BufferedReader(new FileReader(profileFile))) {

			ReaderTokenizer stok = new ReaderTokenizer(r);

			stok.parseNumbers();
			int currentColumn = 0;
			// Skip column headers
			for (int i = 0; i < nColumns; i++) {
				stok.nextToken();
			}
			stok.nextToken();

			// FloatList profileList=new ArrayFloatList(100000); //Time in seconds
			// FloatList xPositionList=new ArrayFloatList(100000); //Time in seconds

			ArrayList<float[]> readings = new ArrayList<>();

			int currentIndex = 0;
			readings.add(new float[2]);
			while (stok.ttype != ReaderTokenizer.TT_EOF) {

				if (stok.ttype == ReaderTokenizer.TT_NUMBER) {
					float f = (float) stok.nval;
					if (currentColumn == columnIndex) {
						readings.get(currentIndex)[1] = f;
						if (f < 0) {
							Utils.log("Read negative profile", LogLevel.ERROR);
						}
					} else if (currentColumn == xColumnIndex) {
						readings.get(currentIndex)[0] = f;
						if (f < 0) {
							Utils.log("Read negative profile", LogLevel.ERROR);
						}
					}
				} else
					throw new RuntimeException("Unexpected read from detected cell file " + stok.sval + " of type "
							+ stok.ttype);
				stok.nextToken();
				currentColumn++;
				if (currentColumn == nColumns) {
					currentColumn = 0;
					currentIndex++;
					readings.add(new float[2]);
				}
			}

			readings.remove(readings.size() - 1);

			float[][] readingsArray = readings.toArray(new float[][] { { 1, 2 } });

			Arrays.sort(readingsArray, (o1, o2) -> Float.compare(o1[0], o2[0]));

			float[] steps = minMaxSlidingWindow(readingsArray, windowLength);

			for (int i = 0; i < steps.length; i++) {
				System.out.println((readingsArray[i][0] + windowLength * 0.5) + "\t" + steps[i]);
			}
		} catch (FileNotFoundException e) {
			throw new RuntimeException("Could not find output file to read times from", e);
		}
	}

	private static float[] minMaxSlidingWindow(float[][] numbers, float windowLength) {
		int windowStart = 0;

		float[] result = new float[numbers.length];
		int arrayIndex = 0;
		float globalMin = Float.MAX_VALUE;

		for (float[] number : numbers) {
			if (number[1] < globalMin)
				globalMin = number[1];
		}

		while (arrayIndex < numbers.length - 1) {
			int effectiveEnd = windowStart;
			while ((numbers[effectiveEnd][0] - numbers[windowStart][0] < windowLength)
					&& (effectiveEnd < numbers.length - 1))
				effectiveEnd++;

			float max = -Float.MAX_VALUE;
			float min = Float.MAX_VALUE;

			for (int i = windowStart; i < effectiveEnd; i++) {
				if (numbers[i][1] > max)
					max = numbers[i][1];
				if (numbers[i][1] < min)
					min = numbers[i][1];
			}

			if (min > globalMin)
				result[arrayIndex] = (max - min) / (min - globalMin);
			else
				result[arrayIndex] = Float.NaN;

			windowStart++;
			arrayIndex++;
		}

		return result;
	}

	public static Color colorFromString(String colorName) {
		Color result = null;
		try {
			Field field = Class.forName("java.awt.Color").getField(colorName.toLowerCase());
			result = (Color) field.get(null);
		} catch (Exception e) {
			Utils.log("Error trying to parse color " + colorName, LogLevel.ERROR);
		}
		return result;
	}

	public static double enhanceMin(double min) {
		return min - Math.abs(min) * 0.01;
	}

	public static double enhanceMax(double max) {
		return max + Math.abs(max) * 0.01;
	}

}
