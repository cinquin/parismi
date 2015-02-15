package pipeline.data.tiff_read_write;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Vector;

import pipeline.data.ChannelInfo;
import pipeline.misc_util.FormatException;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;

/**
 * Copied from ImageJ 1.43r source, adapted for BigTIFF and LSMs and performance-tuned by Olivier Cinquin.
 * This code reads TIFFs faster than ImageJ's native TIFF reader, and reads LSMs ~10x faster than the
 * LOCI Bioformat plugin.
 * A significant performance increase when reading large file directory structure (with {@link #getTiffInfo}) was
 * achieved by
 * not using custom ImageJ caching of the stream, and just using a RandomAccessFile.
 * This class is just used to read the dimensions and offsets of files in the TIFF image.
 * Actual reading of the pixels is performed on demand by {@link TIFFFileAccessor}, which does it much
 * more quickly on large files than the TIFFDecoder from ImageJ this file was derived from.
 * This class has only been tested on float images with little metadata.
 * Decodes single and multi-image TIFF files. The LZW decompression
 * code was contributed by Curtis Rueden.
 */
class BigTiffDecoder {

	/**
	 * Copied for ij.util.Tools to remove dependency
	 * Returns a double containg the value represented by the
	 * specified <code>String</code>.
	 *
	 * @param s
	 *            the string to be parsed.
	 * @param defaultValue
	 *            the value returned if <code>s</code> does not contain a parsable double
	 * @return The double value represented by the string argument or <code>defaultValue</code> if the string does not
	 *         contain a parsable double
	 */
	private static double parseDouble(String s, double defaultValue) {
		if (s == null)
			return defaultValue;
		try {
			defaultValue = Double.parseDouble(s);
		} catch (NumberFormatException e) {
		}
		return defaultValue;
	}

	// tags
	private static final int NEW_SUBFILE_TYPE = 254;
	private static final int IMAGE_WIDTH = 256;
	private static final int IMAGE_LENGTH = 257;
	private static final int BITS_PER_SAMPLE = 258;
	private static final int COMPRESSION = 259;
	private static final int PHOTO_INTERP = 262;
	private static final int IMAGE_DESCRIPTION = 270;
	private static final int STRIP_OFFSETS = 273;
	private static final int ORIENTATION = 274;
	private static final int SAMPLES_PER_PIXEL = 277;
	private static final int ROWS_PER_STRIP = 278;
	private static final int STRIP_BYTE_COUNT = 279;
	private static final int X_RESOLUTION = 282;
	private static final int Y_RESOLUTION = 283;
	private static final int PLANAR_CONFIGURATION = 284;
	private static final int RESOLUTION_UNIT = 296;
	private static final int SOFTWARE = 305;
	private static final int DATE_TIME = 306;
	private static final int ARTEST = 315;
	private static final int HOST_COMPUTER = 316;
	private static final int PREDICTOR = 317;
	private static final int COLOR_MAP = 320;
	private static final int SAMPLE_FORMAT = 339;
	private static final int JPEG_TABLES = 347;
	private static final int METAMORPH1 = 33628;
	private static final int METAMORPH2 = 33629;
	private static final int IPLAB = 34122;
	private static final int NIH_IMAGE_HDR = 43314;
	private static final int META_DATA_BYTE_COUNTS = 50838; // private tag registered with Adobe
	private static final int META_DATA = 50839; // private tag registered with Adobe
	private static final int TIF_CZ_LSMINFO = 34412; // LSM metadata

	// constants
	public static final int UNSIGNED = 1;
	private static final int SIGNED = 2;
	private static final int FLOATING_POINT = 3;

	// field types
	private static final int SHORT = 3;
	public static final int LONG = 4;

	// metadata types
	private static final int MAGIC_NUMBER = 0x494a494a; // "IJIJ"
	private static final int INFO = 0x696e666f; // "info" (Info image property)
	private static final int LABELS = 0x6c61626c; // "labl" (slice labels)
	private static final int RANGES = 0x72616e67; // "rang" (display ranges)
	private static final int LUTS = 0x6c757473; // "luts" (channel LUTs)
	private static final int ROI = 0x726f6920; // "roi " (ROI)
	private static final int OVERLAY = 0x6f766572; // "over" (overlay)

	private String directory;
	private String name;
	private String url;
	private RandomAccessFile in;
	private boolean debugMode = true;
	public boolean littleEndian;
	private String dInfo;
	private int ifdCount;
	private int[] metaDataCounts;
	private String tiffMetadata;

	public BigTiffDecoder(String directory, String name) {
		this.directory = directory;
		this.name = name;
	}

	FileChannel fileChannel;

	public BigTiffDecoder(RandomAccessFile in, String name) {
		directory = "";
		this.name = name;
		url = "";
		this.in = in;
	}

	final int getInt() throws IOException {
		int b1 = in.read();
		int b2 = in.read();
		int b3 = in.read();
		int b4 = in.read();
		if (littleEndian)
			return ((b4 << 24) + (b3 << 16) + (b2 << 8) + (b1 << 0));
		else
			return ((b1 << 24) + (b2 << 16) + (b3 << 8) + b4);
	}

	final long getLong() throws IOException {
		long b1 = in.read();
		long b2 = in.read();
		long b3 = in.read();
		long b4 = in.read();
		long b5 = in.read();
		long b6 = in.read();
		long b7 = in.read();
		long b8 = in.read();
		if (littleEndian)
			return ((b8 << 56) + (b7 << 48) + (b6 << 40) + (b5 << 32) + (b4 << 24) + (b3 << 16) + (b2 << 8) + (b1 << 0));
		else
			return ((b1 << 56) + (b2 << 48) + (b3 << 40) + (b4 << 32) + (b5 << 24) + (b6 << 16) + (b7 << 8) + (b8 << 0));
	}

	final int getShort() throws IOException {
		int b1 = in.read();
		int b2 = in.read();
		if (littleEndian)
			return ((b2 << 8) + b1);
		else
			return ((b1 << 8) + b2);
	}

	final long readLong() throws IOException {
		if (littleEndian)
			return (getInt() & 0xffffffffL) + ((long) getInt() << 32);
		else
			return ((long) getInt() << 32) + (getInt() & 0xffffffffL);
		// return
		// in.read()+(in.read()<<8)+(in.read()<<16)+(in.read()<<24)+(in.read()<<32)+(in.read()<<40)+(in.read()<<48)+(in.read()<<56);
	}

	final double readDouble() throws IOException {
		return Double.longBitsToDouble(readLong());
	}

	public boolean bigTIFF;

	private int entrySize;

	long OpenImageFileHeader() throws IOException, FormatException {
		// Open 8-byte Image File Header at start of file.
		// Returns the offset in bytes to the first IFD or -1
		// if this is not a valid tiff file.
		int byteOrder = in.readShort();
		if (byteOrder == 0x4949) // "II"
			littleEndian = true;
		else if (byteOrder == 0x4d4d) // "MM"
			littleEndian = false;
		else {
			in.close();
			throw new FormatException("Corrupt TIFF header: can't read endianness " + byteOrder);
		}
		int magicNumber = getShort(); // 42 or 42
		bigTIFF = (magicNumber == 43);
		if ((!bigTIFF) && (magicNumber != 42))
			throw new FormatException("Unrecognized TIFF magic number: " + magicNumber);
		long offset;
		if (!bigTIFF) {
			offset = getInt() & 0xffffffffL;
			if (offset < 0)
				throw new FormatException("Negative offset");
			entrySize = 12;
		} else {
			Utils.log("Found bigTIFF", LogLevel.VERBOSE_DEBUG);
			int offsetSize = getShort();
			if (offsetSize != 8)
				throw new FormatException("Unsupported offset size in BigTIFF file: " + offsetSize);
			Utils.log("bytesize of offsets " + offsetSize, LogLevel.VERBOSE_DEBUG);
			int zero = getShort();
			if (zero != 0)
				throw new FormatException("Found BigTIFF file but not the expected 0 constant at offset 6: " + zero);
			offset = getLong();
			if (offset < 0)
				throw new FormatException("Negative offset");
			Utils.log("First offset in bigtiff is " + offset, LogLevel.VERBOSE_DEBUG);
			entrySize = 20;
		}
		return offset;
	}

	long getValue(int fieldType, long count) throws IOException {
		long value = 0;
		if (fieldType == SHORT && count == 1) {
			value = getShort();
			getShort();// unused
			if (bigTIFF)
				getInt();
		} else {
			if (bigTIFF)
				value = getLong();
			else
				value = getInt();
		}
		return value;
	}

	void getColorMap(long offset, BareBonesFileInfoLongOffsets fi) throws IOException {
		byte[] colorTable16 = new byte[768 * 2];
		long saveLoc = in.getFilePointer();
		in.seek(offset);
		in.readFully(colorTable16);
		in.seek(saveLoc);
		fi.lutSize = 256;
		fi.reds = new byte[256];
		fi.greens = new byte[256];
		fi.blues = new byte[256];
		int j = 0;
		if (littleEndian)
			j++;
		int sum = 0;
		for (int i = 0; i < 256; i++) {
			fi.reds[i] = colorTable16[j];
			sum += fi.reds[i];
			fi.greens[i] = colorTable16[512 + j];
			sum += fi.greens[i];
			fi.blues[i] = colorTable16[1024 + j];
			sum += fi.blues[i];
			j += 2;
		}
		if (sum != 0)
			fi.fileType = BareBonesFileInfoLongOffsets.COLOR8;
	}

	byte[] getString(int count, long offset) throws IOException {
		count--; // skip null byte at end of string
		if (count <= 0)
			return null;
		byte[] bytes = new byte[count];
		long saveLoc = in.getFilePointer();
		in.seek(offset);
		in.readFully(bytes);
		in.seek(saveLoc);
		return bytes;
	}

	/**
	 * Save the image description in the specified BareBonesFileInfoLongOffsets. ImageJ
	 * saves spatial and density calibration data in this string. For
	 * stacks, it also saves the number of images to avoid having to
	 * decode an IFD for each image.
	 */
	private void saveImageDescription(byte[] description, BareBonesFileInfoLongOffsets fi) {
		String id = new String(description);
		if (debugMode)
			Utils.log("Image description: " + id, LogLevel.VERBOSE_DEBUG);
		if (!id.startsWith("ImageJ"))
			saveMetadata(getName(IMAGE_DESCRIPTION), id);
		if (id.length() < 7)
			return;
		fi.description = id;
		int index1 = id.indexOf("images=");
		if (index1 > 0) {
			int index2 = id.indexOf("\n", index1);
			if (index2 > 0) {
				String images = id.substring(index1 + 7, index2);
				int n = (int) parseDouble(images, 0.0);
				if (n > 1)
					fi.nImages = n;
			}
		}
	}

	public void saveMetadata(String name, String data) {
		if (data == null)
			return;
		String str = name + ": " + data + "\n";
		if (tiffMetadata == null)
			tiffMetadata = str;
		else
			tiffMetadata += str;
	}

	void decodeNIHImageHeader(int offset, BareBonesFileInfoLongOffsets fi) throws IOException {
		long saveLoc = in.getFilePointer();

		in.seek(offset + entrySize);
		int version = in.readShort();

		in.seek(offset + 160);
		double scale = in.readDouble();
		if (version > 106 && scale != 0.0) {
			fi.pixelWidth = 1.0 / scale;
			fi.pixelHeight = fi.pixelWidth;
		}

		// spatial calibration
		in.seek(offset + 172);
		int units = in.readShort();
		if (version <= 153)
			units += 5;
		switch (units) {
			case 5:
				fi.unit = "nanometer";
				break;
			case 6:
				fi.unit = "micrometer";
				break;
			case 7:
				fi.unit = "mm";
				break;
			case 8:
				fi.unit = "cm";
				break;
			case 9:
				fi.unit = "meter";
				break;
			case 10:
				fi.unit = "km";
				break;
			case 11:
				fi.unit = "inch";
				break;
			case 12:
				fi.unit = "ft";
				break;
			case 13:
				fi.unit = "mi";
				break;
			default:
				Utils.log("Unknown unit type " + units, LogLevel.WARNING);
		}

		// density calibration
		in.seek(offset + 182);
		int fitType = in.read();
		in.read();// unused
		int nCoefficients = in.readShort();
		if (fitType == 11) {
			fi.calibrationFunction = 21; // Calibration.UNCALIBRATED_OD
			fi.valueUnit = "U. OD";
		} else if (fitType >= 0 && fitType <= 8 && nCoefficients >= 1 && nCoefficients <= 5) {
			switch (fitType) {
				case 0:
					fi.calibrationFunction = 0;
					break; // Calibration.STRAIGHT_LINE
				case 1:
					fi.calibrationFunction = 1;
					break; // Calibration.POLY2
				case 2:
					fi.calibrationFunction = 2;
					break; // Calibration.POLY3
				case 3:
					fi.calibrationFunction = 3;
					break; // Calibration.POLY4
				case 5:
					fi.calibrationFunction = 4;
					break; // Calibration.EXPONENTIAL
				case 6:
					fi.calibrationFunction = 5;
					break; // Calibration.POWER
				case 7:
					fi.calibrationFunction = 6;
					break; // Calibration.LOG
				case 8:
					fi.calibrationFunction = 10;
					break; // Calibration.RODBARD2 (NIH Image)
				default:
					Utils.log("Unknown unit type " + fitType, LogLevel.WARNING);
			}
			fi.coefficients = new double[nCoefficients];
			for (int i = 0; i < nCoefficients; i++) {
				fi.coefficients[i] = in.readDouble();
			}
			in.seek(offset + 234);
			int size = in.read();
			StringBuffer sb = new StringBuffer();
			if (size >= 1 && size <= 16) {
				for (int i = 0; i < size; i++)
					sb.append((char) (in.read()));
				fi.valueUnit = new String(sb);
			} else
				fi.valueUnit = " ";
		}

		in.seek(offset + 260);
		int nImages = in.readShort();
		if (nImages >= 2 && (fi.fileType == FileInfoLongOffsets.GRAY8 || fi.fileType == FileInfoLongOffsets.COLOR8)) {
			fi.nImages = nImages;
			fi.pixelDepth = in.readFloat(); // SliceSpacing
			in.readShort(); // CurrentSlice unused
			fi.frameInterval = in.readFloat();
			// ij.IJ.write("fi.pixelDepth: "+fi.pixelDepth);
		}

		in.seek(offset + 272);
		float aspectRatio = in.readFloat();
		if (version > 140 && aspectRatio != 0.0)
			fi.pixelHeight = fi.pixelWidth / aspectRatio;

		in.seek(saveLoc);
	}

	void dumpTag(int tag, long count, int value, BareBonesFileInfoLongOffsets fi) {
		String name = getName(tag);
		String cs = ", count=" + count;
		// Utils.log(cs,LogLevel.VERBOSE_DEBUG);
		String tagAsString = tag + ", \"" + name + "\", value=" + value + cs;
		// dInfo += "    " +tagAsString+ "\n";

		if (debugMode)
			// Utils.log("--- Debug info so far "+dInfo+"\n ------------",LogLevel.VERBOSE_DEBUG);
			Utils.log("    " + tagAsString + "\n", LogLevel.VERBOSE_DEBUG);
		// ij.IJ.log(tag + ", \"" + name + "\", value=" + value + cs + "\n");
	}

	static String getName(int tag) {
		String name;
		switch (tag) {
			case NEW_SUBFILE_TYPE:
				name = "NewSubfileType";
				break;
			case IMAGE_WIDTH:
				name = "ImageWidth";
				break;
			case IMAGE_LENGTH:
				name = "ImageLength";
				break;
			case STRIP_OFFSETS:
				name = "StripOffsets";
				break;
			case ORIENTATION:
				name = "Orientation";
				break;
			case PHOTO_INTERP:
				name = "PhotoInterp";
				break;
			case IMAGE_DESCRIPTION:
				name = "ImageDescription";
				break;
			case BITS_PER_SAMPLE:
				name = "BitsPerSample";
				break;
			case SAMPLES_PER_PIXEL:
				name = "SamplesPerPixel";
				break;
			case ROWS_PER_STRIP:
				name = "RowsPerStrip";
				break;
			case STRIP_BYTE_COUNT:
				name = "StripByteCount";
				break;
			case X_RESOLUTION:
				name = "XResolution";
				break;
			case Y_RESOLUTION:
				name = "YResolution";
				break;
			case RESOLUTION_UNIT:
				name = "ResolutionUnit";
				break;
			case SOFTWARE:
				name = "Software";
				break;
			case DATE_TIME:
				name = "DateTime";
				break;
			case ARTEST:
				name = "Artest";
				break;
			case HOST_COMPUTER:
				name = "HostComputer";
				break;
			case PLANAR_CONFIGURATION:
				name = "PlanarConfiguration";
				break;
			case COMPRESSION:
				name = "Compression";
				break;
			case PREDICTOR:
				name = "Predictor";
				break;
			case COLOR_MAP:
				name = "ColorMap";
				break;
			case SAMPLE_FORMAT:
				name = "SampleFormat";
				break;
			case JPEG_TABLES:
				name = "JPEGTables";
				break;
			case NIH_IMAGE_HDR:
				name = "NIHImageHeader";
				break;
			case META_DATA_BYTE_COUNTS:
				name = "MetaDataByteCounts";
				break;
			case META_DATA:
				name = "MetaData";
				break;
			case TIF_CZ_LSMINFO:
				name = "LSMMetaData";
				break;
			default:
				name = tag + " (unknown tag type)";
				break;
		}
		return name;
	}

	/**
	 * True if the file we're decoding is an LSM file (guessed based on the existence of the TIF_CZ_LSMINFO tag).
	 */
	private boolean lsm = false;

	/**
	 * Last pixel offset that was read from metadata. Used to work around LSM 32-bit limitation.
	 */
	private long lastReadPixelOffset = 0;

	/**
	 * True if a pixel offset > 2^31 has been seen.
	 */
	private boolean seenLargeOffset = false;

	private long lsmPixelOffsetBase = 0;

	double getRational(long loc) throws IOException {
		// THIS CODE IS PROBABLY WRONG; IT SHOULD PROBABLY BE READING A DOUBLE
		Utils.log("Get rational is probably not doing what it should", LogLevel.DEBUG);
		long saveLoc = in.getFilePointer();
		in.seek(loc);
		int numerator = getInt();
		int denominator = getInt();
		in.seek(saveLoc);
		// System.out.println("numerator: "+numerator);
		// System.out.println("denominator: "+denominator);
		if (denominator != 0)
			return (double) numerator / denominator;
		else
			return 0.0;
	}

	@SuppressWarnings("unused")
	BareBonesFileInfoLongOffsets OpenIFD() throws IOException, FormatException {
		// Get Image File Directory data
		int tag, fieldType, value;
		int count;
		long nEntries;
		if (bigTIFF)
			nEntries = getLong();
		else
			nEntries = getShort();
		if (nEntries < 1 || nEntries > 1000)
			throw new FormatException("Aberrant number of IFD entries: " + nEntries);
		ifdCount++;
		BareBonesFileInfoLongOffsets fi = new BareBonesFileInfoLongOffsets();
		for (int i = 0; i < nEntries; i++) {
			tag = getShort();
			fieldType = getShort();
			if (bigTIFF)
				count = new Long(getLong()).intValue();
			else
				count = getInt();
			long lvalue = getValue(fieldType, count);// OFFSET TO TAG DATA
			value = new Long(lvalue).intValue();
			// if (debugMode && ifdCount<30) dumpTag(tag, count, value, fi);
			if (debugMode)
				Utils.log(i + "/" + nEntries + " " + getName(tag) + ", count=" + count + ", value=" + value,
						LogLevel.VERBOSE_DEBUG);
			// if (tag==0) return null;
			switch (tag) {
				case IMAGE_WIDTH:
					fi.width = value;
					fi.intelByteOrder = littleEndian;
					break;
				case IMAGE_LENGTH:
					fi.height = value;
					break;
				case STRIP_OFFSETS:
					if (count == 1) {
						fi.stripOffsets = new long[] { lvalue };
						// Utils.log("Read a single strip offset of "+value,LogLevel.VERBOSE_VERBOSE_DEBUG);
					} else {
						long saveLoc = in.getFilePointer();
						in.seek(lvalue);
						fi.stripOffsets = new long[count];
						for (int c = 0; c < count; c++) {
							if (bigTIFF)
								fi.stripOffsets[c] = getLong();
							else
								fi.stripOffsets[c] = getInt() & 0xffffffffL;
							// Utils.log("Read one of a series of strip offsets of "+fi.stripOffsets[c],LogLevel.VERBOSE_VERBOSE_DEBUG);
						}
						in.seek(saveLoc);
					}

					if (seenLargeOffset && lsm && (fi.stripOffsets[0] < lastReadPixelOffset - 1000000000)) {
						Utils.log("Offsets went back: from " + lastReadPixelOffset + " to " + fi.stripOffsets[0],
								LogLevel.VERBOSE_VERBOSE_DEBUG);
						// assuming offsets are stored in order, we've just passed a 2**32 threshold
						lsmPixelOffsetBase += 4294967296L;// 2**32
					}

					lastReadPixelOffset = fi.stripOffsets[0];

					if (lsm)
						for (int j = 0; j < fi.stripOffsets.length; j++) {
							fi.stripOffsets[j] += lsmPixelOffsetBase;
						}

					fi.offset = count > 0 ? fi.stripOffsets[0] : value & 0xffffffffL;
					if (fi.offset < 0)
						throw new FormatException("Negative offset");
					if (count > 1 && fi.stripOffsets[count - 1] < fi.stripOffsets[0])
						fi.offset = fi.stripOffsets[count - 1];
					if (fi.offset < 0)
						throw new FormatException("Negative offset");

					if (fi.offset > 2147483648L) {
						seenLargeOffset = true;
						// Utils.log("Detected large offset",LogLevel.VERBOSE_VERBOSE_DEBUG);
					}

					// Utils.log("Finally set the fi.offset to "+fi.offset,7);
					break;
				case STRIP_BYTE_COUNT:
					if (count == 1)
						fi.stripLengths = new long[] { value };
					else {
						long saveLoc = in.getFilePointer();
						in.seek(lvalue);
						fi.stripLengths = new long[count];
						for (int c = 0; c < count; c++) {
							if (bigTIFF)
								fi.stripLengths[c] = getLong();
							else
								fi.stripLengths[c] = getInt();
						}
						in.seek(saveLoc);
					}
					break;
				case PHOTO_INTERP:
					// xx fi.whiteIsZero = value==0;
					break;
				case BITS_PER_SAMPLE:
					if (count == 1) {
						if (value == 8)
							fi.fileType = BareBonesFileInfoLongOffsets.GRAY8;
						else if (value == 16)
							fi.fileType = BareBonesFileInfoLongOffsets.GRAY16_UNSIGNED;
						else if (value == 32)
							fi.fileType = BareBonesFileInfoLongOffsets.GRAY32_INT;
						else if (value == 12)
							fi.fileType = BareBonesFileInfoLongOffsets.GRAY12_UNSIGNED;
						else if (value == 1)
							fi.fileType = BareBonesFileInfoLongOffsets.BITMAP;
						else
							error("Unsupported BitsPerSample: " + value);
					} else {// if (count==3) { //FIXME We're assuming that all channels in the file have the same bit
							// depth
						// That might not always be the case
						long saveLoc = in.getFilePointer();
						in.seek(lvalue);
						int bitDepth = getShort(); // IS THIS OK WITH BIGTIFF?
						if (!(bitDepth == 8 || bitDepth == 16))
							error("ImageJ can only open 8 and 16 bit/channel RGB images (" + bitDepth + ")");
						if (bitDepth == 16)
							fi.fileType = BareBonesFileInfoLongOffsets.RGB48;
						in.seek(saveLoc);
					}
					break;
				case SAMPLES_PER_PIXEL:
					fi.samplesPerPixel = value;
					if (value == 3 && fi.fileType != FileInfoLongOffsets.RGB48)
						fi.fileType =
								fi.fileType == FileInfoLongOffsets.GRAY16_UNSIGNED ? FileInfoLongOffsets.RGB48
										: FileInfoLongOffsets.RGB;
					break;
				case ROWS_PER_STRIP:
					fi.rowsPerStrip = value;
					break;
				case X_RESOLUTION:
					double xScale = getRational(value);
					if (xScale != 0.0)
						fi.pixelWidth = 1.0 / xScale;
					break;
				case Y_RESOLUTION:
					double yScale = getRational(value);
					if (yScale != 0.0)
						fi.pixelHeight = 1.0 / yScale;
					break;
				case RESOLUTION_UNIT:
					if (value == 1 && fi.unit == null)
						fi.unit = " ";
					else if (value == 2) {
						if (fi.pixelWidth == 1.0 / 72.0) {
							fi.pixelWidth = 1.0;
							fi.pixelHeight = 1.0;
						} else
							fi.unit = "inch";
					} else if (value == 3)
						fi.unit = "cm";
					break;
				case PLANAR_CONFIGURATION: // 1=chunky, 2=planar
					if (value == 2 && fi.fileType == FileInfoLongOffsets.RGB48)
						fi.fileType = BareBonesFileInfoLongOffsets.GRAY16_UNSIGNED;
					else if (value == 2 && fi.fileType == FileInfoLongOffsets.RGB)
						fi.fileType = BareBonesFileInfoLongOffsets.RGB_PLANAR;
					else if (value == 1 && fi.samplesPerPixel == 4)
						fi.fileType = BareBonesFileInfoLongOffsets.ARGB;
					else if (value != 2 && !((fi.samplesPerPixel == 1) || (fi.samplesPerPixel == 3))) {
						String msg = "Unsupported SamplesPerPixel: " + fi.samplesPerPixel;
						error(msg);
					}
					break;
				case COMPRESSION:
					if (value == 5) // LZW compression
						fi.compression = BareBonesFileInfoLongOffsets.LZW;
					else if (value == 32773) // PackBits compression
						fi.compression = BareBonesFileInfoLongOffsets.PACK_BITS;
					else if (value != 1 && value != 0 && !(value == 7 && fi.width < 500)) {
						// don't abort with Spot camera compressed (7) thumbnails
						// otherwise, this is an unknown compression type
						fi.compression = BareBonesFileInfoLongOffsets.COMPRESSION_UNKNOWN;
						error("ImageJ cannot open TIFF files " + "compressed in this fashion (" + value + ")");
					}
					break;
				case SOFTWARE:
				case DATE_TIME:
				case HOST_COMPUTER:
				case ARTEST:
					if (ifdCount == 1) {
						byte[] bytes = getString(count, lvalue);
						String s = bytes != null ? new String(bytes) : null;
						saveMetadata(getName(tag), s);
					}
					break;
				case PREDICTOR:
					if (value == 2 && fi.compression == FileInfoLongOffsets.LZW)
						fi.compression = BareBonesFileInfoLongOffsets.LZW_WITH_DIFFERENCING;
					break;
				case COLOR_MAP:
					if (count == 768 && fi.fileType == FileInfoLongOffsets.GRAY8)
						getColorMap(lvalue, fi);
					break;
				case SAMPLE_FORMAT:
					if (fi.fileType == FileInfoLongOffsets.GRAY32_INT && value == FLOATING_POINT)
						fi.fileType = BareBonesFileInfoLongOffsets.GRAY32_FLOAT;
					if (fi.fileType == FileInfoLongOffsets.GRAY16_UNSIGNED) {
						if (value == SIGNED)
							fi.fileType = BareBonesFileInfoLongOffsets.GRAY16_SIGNED;
						if (value == FLOATING_POINT)
							error("ImageJ cannot open 16-bit float TIFFs");
					}
					break;
				case JPEG_TABLES:
					if (fi.compression == FileInfoLongOffsets.JPEG)
						error("Cannot open JPEG-compressed TIFFs with separate tables");
					break;
				case IMAGE_DESCRIPTION:
					if (ifdCount == 1) {
						byte[] s = getString(count, lvalue);
						if (debugMode)
							Utils.log("Image description is\n" + new String(s), LogLevel.VERBOSE_DEBUG);
						if (s != null)
							saveImageDescription(s, fi);
					}
					break;
				case ORIENTATION:
					fi.nImages = 0; // file not created by ImageJ so look at all the IFDs
					break;
				case METAMORPH1:
				case METAMORPH2:
					if ((name.indexOf(".STK") > 0 || name.indexOf(".stk") > 0)
							&& fi.compression == FileInfoLongOffsets.COMPRESSION_NONE) {
						if (tag == METAMORPH2)
							fi.nImages = count;
						else
							fi.nImages = 9999;
					}
					break;
				case IPLAB:
					fi.nImages = value;
					break;
				case NIH_IMAGE_HDR:
					if (count == 256)
						decodeNIHImageHeader(value, fi);
					break;
				case TIF_CZ_LSMINFO:
					lsm = true;
					Utils.log("Detected LSM file", LogLevel.VERBOSE_VERBOSE_DEBUG);
					long saveLoc2 = in.getFilePointer();
					in.seek(lvalue);
					int magicNumber = getInt();
					if (magicNumber != 0x00300494C && magicNumber != 0x00400494C) {
						throw new FormatException("Unrecognized TIF_CZ_LSMINFO magic number for LSM file "
								+ magicNumber);
					}
					for (int r = 0; r < 3; r++) { // read and discard 3 ints
						getInt();
					}
					int nSlices = getInt();
					int nChannels = getInt();
					int nTimePoints = getInt();
					fi.nTimePoints = nTimePoints;
					int DataType = getInt();
					int s32ThumbnailX = getInt();
					int s32ThumbnailY = getInt();
					ByteBuffer b2 = ByteBuffer.allocate(3 * 8);// to read 3 doubles
					b2.order(ByteOrder.LITTLE_ENDIAN);
					byte[] b2Array = b2.array();
					for (int b = 0; b < b2Array.length; b++) {
						b2Array[b] = in.readByte();
					}

					fi.pixelWidth = b2.getDouble() * 1.0E6d;
					fi.pixelHeight = b2.getDouble() * 1.0E6d;
					fi.pixelDepth = b2.getDouble() * 1.0E6d;

					for (int r = 0; r < 6; r++) { // read and discard 3 64 bit floats
						getInt();
					}

					int scanType = getShort();
					/**
					 * Scan type
					 * 0 - normal x-y-z-scan
					 * 1 - z-Scan (x-z-plane)
					 * 2 - line scan
					 * 3 - time series x-y
					 * 4 - time series x-z (release 2.0 or later)
					 * 5 - time series ���Mean of ROIs��� (release 2.0 or later)
					 * 6 - time series x-y-z (release 2.3 or later)
					 * 7 - spline scan (release 2.5 or later)
					 * 8 - spline plane x-z (release 2.5 or later)
					 * 9 - time series spline plane x-z (release 2.5 or later)
					 * 10 - point mode (release 3.0)
					 */

					int spectralScan = getShort();
					int dataType = getInt();
					int u32OffsetVectorOverlay = getInt();
					int u32OffsetInputLut = getInt();
					int u32OffsetOutputLut = getInt();
					int u32OffsetChannelColors = getInt();

					b2.clear();
					b2.order(ByteOrder.LITTLE_ENDIAN);
					for (int b = 0; b < 8; b++) { // read a 64-bit float
						b2Array[b] = in.readByte();
					}
					fi.timeInterval = b2.getDouble();

					int u32OffsetChannelDataTypes = getInt();
					int u32OffsetScanInformation = getInt();
					// See page 47 in LSM specs

					int subBlockRecording = 0x010000000;
					final int subBlockDetectionChannels = 0x060000000;
					final int[] subBlockTypes =
							new int[] { 0x010000000, 0x030000000, 0x050000000, 0x020000000, 0x040000000, 0x060000000,
									0x070000000, 0x080000000, 0x090000000, 0x0A0000000, 0x0B0000000, 0x0C0000000,
									0x0D0000000, 0x011000000, 0x012000000, 0x013000000, 0x014000000 };
					int subBlockEnd = 0x0FFFFFFFF;

					ArrayList<ChannelInfo> channelInfo = new ArrayList<>();

					if (u32OffsetScanInformation > 0) { // Scan information is present; let's analyze it
						in.seek(u32OffsetScanInformation);

						boolean foundRotationRecording = false;
						boolean foundDetectionChannels = false;

						int subBlockDepth = 0;

						while (true) {// ((!foundRotationRecording)||(!foundDetectionChannels))
							int u32Entry = getInt();
							if (u32Entry == subBlockEnd)
								subBlockDepth--;
							else if (Utils.indexOf(subBlockTypes, u32Entry) > -1)
								subBlockDepth++;
							if (subBlockDepth == 0)
								break;

							int u32Type = getInt();
							int u32Size = getInt();
							if (u32Entry == 0x010000034) {
								foundRotationRecording = true;
								double rotation = readDouble();
								fi.rotation = rotation;
								in.skipBytes(u32Size - 8);
							} else if (u32Entry == 0x070000022) {
								double wavelengthStart = readDouble();
								Utils.log("Found wavelength start: " + wavelengthStart, LogLevel.DEBUG);
								ChannelInfo info = new ChannelInfo();
								info.getDetectionRanges().add((int) wavelengthStart);
								channelInfo.add(info);
								in.skipBytes(u32Size - 8);
							} else if (u32Entry == 0x070000023) {
								double wavelengthEnd = readDouble();
								Utils.log("Found wavelength end: " + wavelengthEnd, LogLevel.DEBUG);
								channelInfo.get(channelInfo.size() - 1).getDetectionRanges().add((int) wavelengthEnd);
								in.skipBytes(u32Size - 8);
							} else if (u32Entry == 0x090000003) {
								double illuminationWavelength = readDouble();
								Utils.log("Found illumination wavelength: " + illuminationWavelength, LogLevel.DEBUG);
								// TODO record in metadata
								in.skipBytes(u32Size - 8);
							} else if (u32Entry == 0x070000026) {
								String s = readNullTerminatedString();
								Utils.log("Dye name: " + s, LogLevel.DEBUG);
								in.skipBytes(u32Size - (s.length() + 1));
							} else {
								in.skipBytes(u32Size);
							}
						}

						fi.channelInfo = channelInfo.toArray(new ChannelInfo[] {});

						if (!foundRotationRecording) {
							Utils.log("Did not find rotation in LSM metadata", LogLevel.WARNING);
						}
					}
					in.seek(saveLoc2);
					break;
				case NEW_SUBFILE_TYPE:
					if (lvalue == 1)
						fi.isThumbnail = true;
					break;
				case META_DATA_BYTE_COUNTS:
					long saveLoc = in.getFilePointer();
					in.seek(lvalue);
					metaDataCounts = new int[count];
					for (int c = 0; c < count; c++)
						metaDataCounts[c] = getInt();// THIS IS INTERNAL IMAGEJ FORMAT AND SHOULD NOT BE UPDATED FOR
														// BIGTIFF (RIGHT?)
					in.seek(saveLoc);
					break;
				case META_DATA:
					getMetaData(lvalue, fi);
					break;
				default:
					if (tag > 10000 && tag < 32768 && ifdCount > 1)
						return null;
			}
		}
		/*
		 * xx
		 * fi.fileFormat = BareBonesFileInfoLongOffsets.TIFF;
		 * fi.fileName = name;
		 * fi.directory = directory;
		 * if (url!=null)
		 * fi.url = url;
		 */
		return fi;
	}

	private String readNullTerminatedString() throws IOException {
		StringBuffer buf = new StringBuffer(200);
		byte b = in.readByte();
		while (b != 0) {
			b = in.readByte();
			buf.append(b);
		}
		return buf.toString();
	}

	void getMetaData(long loc, BareBonesFileInfoLongOffsets fi) throws IOException {
		if (metaDataCounts == null || metaDataCounts.length == 0)
			return;
		// int maxTypes = 10;
		long saveLoc = in.getFilePointer();
		in.seek(loc);
		// int n = metaDataCounts.length;
		int hdrSize = metaDataCounts[0];
		if (hdrSize < 12 || hdrSize > 804) {
			in.seek(saveLoc);
			return;
		}
		int magicNumber = getInt();
		if (magicNumber != MAGIC_NUMBER) // "IJIJ"
		{
			in.seek(saveLoc);
			return;
		}
		if (debugMode)
			Utils.log("*****Found the magic number for metadata", LogLevel.VERBOSE_DEBUG);
		int nTypes = (hdrSize - 4) / 8;
		int[] types = new int[nTypes];
		int[] counts = new int[nTypes];

		if (debugMode)
			Utils.log(dInfo += "Metadata:", LogLevel.VERBOSE_VERBOSE_VERBOSE_DEBUG);
		int extraMetaDataEntries = 0;
		for (int i = 0; i < nTypes; i++) {
			types[i] = getInt();
			counts[i] = getInt();
			if (types[i] < 0xffffff)
				extraMetaDataEntries += counts[i];
			if (debugMode) {
				String id = "";
				if (types[i] == INFO)
					id = " (Info property)";
				if (types[i] == LABELS)
					id = " (slice labels)";
				if (types[i] == RANGES)
					id = " (display ranges)";
				if (types[i] == LUTS)
					id = " (luts)";
				if (types[i] == ROI)
					id = " (roi)";
				if (types[i] == OVERLAY)
					id = " (overlay)";
				Utils.log("Type " + i + "=" + Integer.toHexString(types[i]) + " " + counts[i] + id + "\n",
						LogLevel.VERBOSE_DEBUG);
			}
		}
		fi.metaDataTypes = new int[extraMetaDataEntries];
		fi.metaData = new byte[extraMetaDataEntries][];
		int start = 1;
		int eMDindex = 0;
		for (int i = 0; i < nTypes; i++) {
			if (types[i] == INFO)
				getInfoProperty(start, fi);
			else if (types[i] == LABELS)
				getSliceLabels(start, start + counts[i] - 1, fi);
			else if (types[i] == RANGES) {
				getDisplayRanges(start, fi);

			} else if (types[i] == LUTS) {
				getLuts(start, start + counts[i] - 1, fi);

			} else if (types[i] == ROI) {
				getRoi(start, fi);
			} else if (types[i] == OVERLAY) {
				getOverlay(start, start + counts[i] - 1, fi);
			} else if (types[i] < 0xffffff) {
				for (int j = start; j < start + counts[i]; j++) {
					int len = metaDataCounts[j];
					fi.metaData[eMDindex] = new byte[len];
					in.readFully(fi.metaData[eMDindex], 0, len);
					fi.metaDataTypes[eMDindex] = types[i];
					eMDindex++;
				}
			} else
				skipUnknownType(start, start + counts[i] - 1);
			start += counts[i];
		}
		if (debugMode)
			Utils.log("*****Done with metadata", LogLevel.VERBOSE_DEBUG);
		in.seek(saveLoc);
	}

	void getInfoProperty(int first, BareBonesFileInfoLongOffsets fi) throws IOException {
		int len = metaDataCounts[first];
		byte[] buffer = new byte[len];
		in.readFully(buffer, 0, len);
		len /= 2;
		char[] chars = new char[len];
		if (littleEndian) {
			for (int j = 0, k = 0; j < len; j++)
				chars[j] = (char) (buffer[k++] & 255 + ((buffer[k++] & 255) << 8));
		} else {
			for (int j = 0, k = 0; j < len; j++)
				chars[j] = (char) ((((buffer[k++]) & 255 + 0) << 8) + ((buffer[k++]) & 255));
		}
		fi.info = new String(chars);
	}

	void getSliceLabels(int first, int last, BareBonesFileInfoLongOffsets fi) throws IOException {
		fi.sliceLabels = new String[last - first + 1];
		int index = 0;
		byte[] buffer = new byte[metaDataCounts[first]];
		for (int i = first; i <= last; i++) {
			int len = metaDataCounts[i];
			if (len > 0) {
				if (len > buffer.length)
					buffer = new byte[len];
				in.readFully(buffer, 0, len);
				len /= 2;
				char[] chars = new char[len];
				if (littleEndian) {
					for (int j = 0, k = 0; j < len; j++)
						chars[j] = (char) (buffer[k++] & 255 + ((buffer[k++] & 255) << 8));
				} else {
					for (int j = 0, k = 0; j < len; j++)
						chars[j] = (char) (((buffer[k++] & 0xFF) << 8) + (buffer[k++] & 0xFF));
				}
				fi.sliceLabels[index++] = new String(chars);
				// ij.IJ.log(i+"  "+fi.sliceLabels[i-1]+"  "+len);
			} else
				fi.sliceLabels[index++] = null;
		}
	}

	void getDisplayRanges(int first, BareBonesFileInfoLongOffsets fi) throws IOException {
		int n = metaDataCounts[first] / 8;
		fi.displayRanges = new double[n];
		for (int i = 0; i < n; i++)
			fi.displayRanges[i] = readDouble();
	}

	void getLuts(int first, int last, BareBonesFileInfoLongOffsets fi) throws IOException {
		fi.channelLuts = new byte[last - first + 1][];
		int index = 0;
		for (int i = first; i <= last; i++) {
			int len = metaDataCounts[i];
			fi.channelLuts[index] = new byte[len];
			in.readFully(fi.channelLuts[index], 0, len);
			index++;
		}
	}

	void getRoi(int first, BareBonesFileInfoLongOffsets fi) throws IOException {
		int len = metaDataCounts[first];
		fi.roi = new byte[len];
		in.readFully(fi.roi, 0, len);
	}

	void getOverlay(int first, int last, BareBonesFileInfoLongOffsets fi) throws IOException {
		fi.overlay = new byte[last - first + 1][];
		int index = 0;
		for (int i = first; i <= last; i++) {
			int len = metaDataCounts[i];
			fi.overlay[index] = new byte[len];
			in.readFully(fi.overlay[index], 0, len);
			index++;
		}
	}

	void error(String message) throws IOException {
		if (in != null)
			in.close();
		throw new IOException(message);
	}

	void skipUnknownType(int first, int last) throws IOException {
		byte[] buffer = new byte[metaDataCounts[first]];
		for (int i = first; i <= last; i++) {
			int len = metaDataCounts[i];
			if (len > buffer.length)
				buffer = new byte[len];
			in.readFully(buffer, 0, len);
		}
	}

	public void enableDebugging() {
		debugMode = true;
	}

	public BareBonesFileInfoLongOffsets[] getTiffInfo() throws IOException, FormatException {
		long ifdOffset;
		Vector<BareBonesFileInfoLongOffsets> info;

		if (in == null)
			in = new RandomAccessFile(new File(directory, name), "r");
		info = new Vector<>();
		ifdOffset = OpenImageFileHeader();
		if (ifdOffset < 0L) {
			in.close();
			Utils.log("Negative TIFF file offset", LogLevel.ERROR);
			return null;
		}
		if (debugMode)
			dInfo = "\n  " + name + ": opening\n";
		while (ifdOffset > 0L) {
			in.seek(ifdOffset);
			BareBonesFileInfoLongOffsets fi = OpenIFD();
			if (fi != null) {
				Utils.log("Retrieved a fi with offsets " + fi.offset, LogLevel.VERBOSE_VERBOSE_VERBOSE_DEBUG);
				info.addElement(fi);
				if (bigTIFF)
					ifdOffset = getLong();
				else
					ifdOffset = getInt() & 0xffffffffL;
			} else
				ifdOffset = 0L;
			if (debugMode && ifdCount < 30)
				// dInfo += "  nextIFD=" + ifdOffset + "\n";
				if (debugMode)
					Utils.log("  nextIFD=" + ifdOffset + "\n", LogLevel.VERBOSE_VERBOSE_VERBOSE_DEBUG);
			if (fi != null) {
				if (fi.nImages > 1) { // ignore extra IFDs in ImageJ and NIH Image stacks
					ifdOffset = 0L;
					Utils.log("Ignoring extra IFDs", LogLevel.VERBOSE_VERBOSE_VERBOSE_DEBUG);
				}
			}
		}
		if (info.size() == 0) {
			in.close();
			return null;
		} else {
			BareBonesFileInfoLongOffsets[] fi = new BareBonesFileInfoLongOffsets[info.size()];
			info.copyInto(fi);
			// if (debugMode) fi[0].debugInfo = dInfo;
			if (url != null) {
				in.seek(0);
				// fi[0].inputStream = null;//was in; does this need to be fixed?
			} else
				in.close();
			if (fi[0].info == null)
				fi[0].info = tiffMetadata;
			return fi;
		}
	}

}
