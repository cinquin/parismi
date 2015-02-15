package pipeline.data.video;

import java.text.SimpleDateFormat;
import java.util.Date;

public class VideoFrameInfo {

	public static final long timeOrigin = 1372316827000l;

	public static final ThreadLocal<SimpleDateFormat> dateFormatter = new ThreadLocal<SimpleDateFormat>() {
		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat("E dd MMM yy HH:mm:ss");
		}
	};

	public static boolean hasMagicNumber(Object pixels) {
		long header;
		if (pixels instanceof short[])
			header = readUnsignedInt((short[]) pixels, 0);
		else if (pixels instanceof byte[])
			header = readUnsignedInt((byte[]) pixels, 0);
		else {
			return false;
		}
		return (header == magicNumber);
	}

	public static long readUnsignedInt(byte[] pixels, int offset) {
		long result = 0;
		for (int b = 0; b < 4; b++)
			result += (pixels[b + offset] & 0xFF) << 8 * b;
		return result;
	}

	public static void writeUnsignedInt(byte[] pixels, int offset, long i) {
		for (int b = 0; b < 4; b++)
			pixels[b + offset] = (byte) (i >>> 8 * b);
	}

	public static long readUnsignedInt(short[] pixels, int offset) {
		long result = 0;
		for (int b = 0; b < 2; b++)
			result += (pixels[b + offset] & 0xFFFF) << 16 * b;
		return result;
	}

	public static void writeUnsignedInt(short[] pixels, int offset, long i) {
		for (int b = 0; b < 2; b++)
			pixels[b + offset] = (short) (i >>> 16 * b);
	}

	public static long readUnsignedInt(float[] pixels, int offset) {
		long result = 0;
		for (int b = 0; b < 2; b++)
			result += (((short) pixels[b + offset]) & 0xFFFF) << 16 * b;
		return result;
	}

	public static void writeUnsignedInt(float[] pixels, int offset, long i) {
		for (int b = 0; b < 2; b++)
			pixels[b + offset] = (short) (i >>> 16 * b);
	}

	public int movementScore;
	public long timeMs;
	public long rebasedTimeMs;
	private long extraMicroSeconds;

	public static int stampLengthInBytes = 16;
	private static final long magicNumber = readUnsignedInt(new byte[] { 'C', 'i', 'n', 'q' }, 0);

	public void write(byte[] pixels) {
		writeUnsignedInt(pixels, 0, magicNumber);
		writeUnsignedInt(pixels, 4, (timeMs - extraMicroSeconds / 1000) / 1000);
		writeUnsignedInt(pixels, 8, movementScore);
		writeUnsignedInt(pixels, 12, extraMicroSeconds);
	}

	public void write(short[] pixels) {
		writeUnsignedInt(pixels, 0, magicNumber);
		writeUnsignedInt(pixels, 2, (timeMs - extraMicroSeconds / 1000) / 1000);
		writeUnsignedInt(pixels, 4, movementScore);
		writeUnsignedInt(pixels, 6, extraMicroSeconds);
	}

	public void write(float[] pixels) {
		writeUnsignedInt(pixels, 0, magicNumber);
		writeUnsignedInt(pixels, 2, (timeMs - extraMicroSeconds / 1000) / 1000);
		writeUnsignedInt(pixels, 4, movementScore);
		writeUnsignedInt(pixels, 6, extraMicroSeconds);
	}

	public void write(Object pixels) {
		if (pixels instanceof byte[])
			write((byte[]) pixels);
		else if (pixels instanceof short[])
			write((short[]) pixels);
		else if (pixels instanceof float[])
			write((float[]) pixels);
		else
			throw new IllegalArgumentException();
	}

	public VideoFrameInfo(Object pixels) throws MissingMagicNumber {

		byte[] bytes;
		if (pixels instanceof byte[])
			bytes = (byte[]) pixels;
		else if (pixels instanceof short[]) {
			bytes = new byte[stampLengthInBytes];
			for (int i = 0; i < stampLengthInBytes / 4; i++) {
				long r = readUnsignedInt((short[]) pixels, i * 2);
				writeUnsignedInt(bytes, i * 4, r);
			}
		} else if (pixels instanceof float[]) {
			bytes = new byte[stampLengthInBytes];
			for (int i = 0; i < stampLengthInBytes / 4; i++) {
				long r = readUnsignedInt((float[]) pixels, i * 2);
				writeUnsignedInt(bytes, i * 4, r);
			}
		} else
			throw new MissingMagicNumber("Unsupported pixel type for magic numbers");

		if (!hasMagicNumber(bytes)) {
			throw new MissingMagicNumber("Frame does not have magic number indicating presence of time stamp");
		}

		timeMs = readUnsignedInt(bytes, 4) * 1000;
		movementScore = (int) readUnsignedInt(bytes, 8);

		extraMicroSeconds = readUnsignedInt(bytes, 12);
		if (Math.abs(extraMicroSeconds) > 2000000) {
			throw new IllegalArgumentException("Too high value of tv_usec");
		}

		timeMs += extraMicroSeconds / 1000;
		rebasedTimeMs = timeMs - timeOrigin;

	}

	@Override
	public String toString() {
		return dateFormatter.get().format((new Date(timeMs))) + " (" + timeMs + "); movement: " + movementScore;
	}

	public static void copyTimeStamp(Object pixels, Object pixels2) throws MissingMagicNumber {
		VideoFrameInfo frameInfo = new VideoFrameInfo(pixels);

		frameInfo.write(pixels2);
	}
}
