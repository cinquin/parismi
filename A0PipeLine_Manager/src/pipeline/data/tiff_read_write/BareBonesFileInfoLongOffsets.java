package pipeline.data.tiff_read_write;

import pipeline.data.ChannelInfo;

/**
 * This class consists of public fields that describe an image file.
 * Copied from ImageJ. Changed by Olivier Cinquin for offsets to be Long instead of int, to support BigTIFF.
 */
public class BareBonesFileInfoLongOffsets implements Cloneable {

	/** 8-bit unsigned integer (0-255). */
	public static final int GRAY8 = 0;

	/**
	 * 16-bit signed integer (-32768-32767). Imported signed images
	 * are converted to unsigned by adding 32768.
	 */
	public static final int GRAY16_SIGNED = 1;

	/** 16-bit unsigned integer (0-65535). */
	public static final int GRAY16_UNSIGNED = 2;

	/**
	 * 32-bit signed integer. Imported 32-bit integer images are
	 * converted to floating-point.
	 */
	public static final int GRAY32_INT = 3;

	/** 32-bit floating-point. */
	public static final int GRAY32_FLOAT = 4;

	/** 8-bit unsigned integer with color lookup table. */
	public static final int COLOR8 = 5;

	/** 24-bit interleaved RGB. Import/export only. */
	private static final int RGB = 6;

	/** 24-bit planer RGB. Import only. */
	public static final int RGB_PLANAR = 7;

	/** 1-bit black and white. Import only. */
	public static final int BITMAP = 8;

	/** 32-bit interleaved ARGB. Import only. */
	public static final int ARGB = 9;

	/** 24-bit interleaved BGR. Import only. */
	private static final int BGR = 10;

	/**
	 * 32-bit unsigned integer. Imported 32-bit integer images are
	 * converted to floating-point.
	 */
	private static final int GRAY32_UNSIGNED = 11;

	/** 48-bit interleaved RGB. */
	public static final int RGB48 = 12;

	/** 12-bit unsigned integer (0-4095). Import only. */
	public static final int GRAY12_UNSIGNED = 13;

	/** 24-bit unsigned integer. Import only. */
	private static final int GRAY24_UNSIGNED = 14;

	/** 32-bit interleaved BARG (MCID). Import only. */
	private static final int BARG = 15;

	/** 64-bit floating-point. Import only. */
	private static final int GRAY64_FLOAT = 16;

	/** 48-bit planar RGB. Import only. */
	private static final int RGB48_PLANAR = 17;

	/** 32-bit interleaved ABGR. Import only. */
	private static final int ABGR = 18;

	// File formats
	public static final int UNKNOWN = 0;
	public static final int RAW = 1;
	public static final int TIFF = 2;
	public static final int GIF_OR_JPG = 3;
	public static final int FITS = 4;
	public static final int BMP = 5;
	public static final int DICOM = 6;
	public static final int ZIP_ARCHIVE = 7;
	public static final int PGM = 8;
	public static final int IMAGEIO = 9;

	// Compression modes
	public static final int COMPRESSION_UNKNOWN = 0;
	public static final int COMPRESSION_NONE = 1;
	public static final int LZW = 2;
	public static final int LZW_WITH_DIFFERENCING = 3;
	public static final int JPEG = 4;
	public static final int PACK_BITS = 5;

	/* File format (TIFF, GIF_OR_JPG, BMP, etc.). Used by the File/Revert command */
	// public int fileFormat;

	/* File type (GRAY8, GRAY_16_UNSIGNED, RGB, etc.) */
	public int fileType;

	// public String fileName;
	// public String directory;
	// public String url;
	public int width;
	public int height;
	public long offset = 0; // Use getOffset() to read
	public int nImages;
	public int nTimePoints;
	public int gapBetweenImages;
	// public boolean whiteIsZero;
	public boolean intelByteOrder;
	public int compression;
	public long[] stripOffsets;
	public long[] stripLengths;
	public int rowsPerStrip;
	public int lutSize;
	public byte[] reds;
	public byte[] greens;
	public byte[] blues;
	// public Object pixels;
	// public String debugInfo;
	public String[] sliceLabels;
	public String info;
	// public InputStream inputStream;
	// public VirtualStack virtualStack;

	public double pixelWidth = 1.0;
	public double pixelHeight = 1.0;
	public double pixelDepth = 1.0;
	public String unit;
	public int calibrationFunction;
	public double[] coefficients;
	public String valueUnit;
	public double frameInterval;
	public String description;
	// Use <i>longOffset</i> instead of <i>offset</i> when offset>2147483647.
	// public long longOffset; // Use getOffset() to read
	// Extra metadata to be stored in the TIFF header
	public int[] metaDataTypes; // must be < 0xffffff
	public byte[][] metaData;
	public double[] displayRanges;
	public byte[][] channelLuts;
	public byte[] roi;
	public byte[][] overlay;
	public int samplesPerPixel;

	public boolean isThumbnail;

	public double timeInterval;

	public double rotation;

	public ChannelInfo[] channelInfo;

	/** Creates a FileInfo object with all of its fields set to their default value. */
	public BareBonesFileInfoLongOffsets() {
		// assign default values
		// fileFormat = UNKNOWN;
		fileType = GRAY8;
		// fileName = null;//"Untitled";
		// directory = null;//"";
		// url = null;//"";
		nImages = 1;
		// compression = COMPRESSION_NONE;
		// samplesPerPixel = 1;
	}

	/** Returns the offset as a long. */
	public final long getOffset() {
		return offset;
	}

	/** Returns the number of bytes used per pixel. */
	public int getBytesPerPixel() {
		switch (fileType) {
			case GRAY8:
			case COLOR8:
			case BITMAP:
				return 1;
			case GRAY16_SIGNED:
			case GRAY16_UNSIGNED:
				return 2;
			case GRAY32_INT:
			case GRAY32_UNSIGNED:
			case GRAY32_FLOAT:
			case ARGB:
			case GRAY24_UNSIGNED:
			case BARG:
			case ABGR:
				return 4;
			case RGB:
			case RGB_PLANAR:
			case BGR:
				return 3;
			case RGB48:
			case RGB48_PLANAR:
				return 6;
			case GRAY64_FLOAT:
				return 8;
			default:
				return 0;
		}
	}

	@Override
	public String toString() {
		return
		// "name=" + fileName
		// + ", dir=" + directory
		// + ", url=" + url
		", width=" + width + ", height=" + height + ", nImages=" + nImages + ", type=" + getType()
		// + ", format=" + fileFormat
				+ ", offset=" + getOffset()
				// + ", whiteZero=" + (whiteIsZero?"t":"f")
				+ ", Intel=" + (intelByteOrder ? "t" : "f");
		// + ", lutSize=" + lutSize
		// + ", comp=" + compression
		// + ", ranges=" + (displayRanges!=null?""+displayRanges.length/2:"null")
		// + ", samples=" + samplesPerPixel;
	}

	public String getType() {
		switch (fileType) {
			case GRAY8:
				return "byte";
			case GRAY16_SIGNED:
				return "short";
			case GRAY16_UNSIGNED:
				return "ushort";
			case GRAY32_INT:
				return "int";
			case GRAY32_UNSIGNED:
				return "uint";
			case GRAY32_FLOAT:
				return "float";
			case COLOR8:
				return "byte+lut";
			case RGB:
				return "RGB";
			case RGB_PLANAR:
				return "RGB(p)";
			case RGB48:
				return "RGB48";
			case BITMAP:
				return "bitmap";
			case ARGB:
				return "ARGB";
			case ABGR:
				return "ABGR";
			case BGR:
				return "BGR";
			case BARG:
				return "BARG";
			case GRAY64_FLOAT:
				return "double";
			case RGB48_PLANAR:
				return "RGB48(p)";
			default:
				return "";
		}
	}

	@Override
	public synchronized Object clone() {
		BareBonesFileInfoLongOffsets newFi = new BareBonesFileInfoLongOffsets();
		newFi.width = width;
		newFi.height = height;
		newFi.nImages = nImages;
		newFi.fileType = fileType;
		newFi.offset = offset;
		newFi.intelByteOrder = intelByteOrder;
		newFi.metaData = metaData;
		newFi.gapBetweenImages = gapBetweenImages;
		newFi.stripOffsets = stripOffsets;
		newFi.stripLengths = stripLengths;
		newFi.samplesPerPixel = samplesPerPixel;
		newFi.description = description;
		newFi.pixelDepth = pixelDepth;
		newFi.pixelHeight = pixelHeight;
		newFi.pixelWidth = pixelWidth;
		newFi.isThumbnail = isThumbnail;
		newFi.nTimePoints = nTimePoints;
		newFi.timeInterval = timeInterval;
		newFi.rotation = rotation;
		newFi.channelInfo = channelInfo;
		return newFi;
	}

}
