package pipeline.data.tiff_read_write;

import ij.VirtualStack;
import ij.io.FileInfo;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.misc_util.parfor.ParFor;

/**
 * Writes a raw image described by a FileInfo object to an RandomAccessFile.
 * Adapted from ImageJ source 1.43 to use NIO and direct buffers, and to have
 * a buffering mechanism (which is not completely finished). This buffering mechanism
 * is useful because some plugins write output as they go instead of all at once,
 * and not blocking on disk access makes for higher throughput.
 * In some tests, write throughput was 2-3x higher that ImageJ's native TIFF writer.
 * */
class ImageWriterNIO {
	private static class BufferPair {
		ByteBuffer byteBuffer;
		Buffer typedBuffer;
	}

	private FileInfo fi;

	private static final int queueCapacity = 2;
	private BlockingDeque<BufferPair> freeBuffers = new LinkedBlockingDeque<BufferPair>(queueCapacity);
	private BlockingQueue<BufferPair> pendingWrites = new ArrayBlockingQueue<>(queueCapacity, true);

	public long timeWaitingForData = 0;
	public long timeInIOThreadNotWaiting = 0;
	public long timeWaitingForBufferWrite = 0;
	public long timeMessingWithArray = 0;

	private FileChannel outChannel;

	private ParFor parFor;

	public void waitForIOToFinish() throws IOException {
		if (ioException != null)
			throw ioException;
		if (throwable != null)
			ParFor.UnsafeHolder.unsafe.throwException(throwable);
		int nRetrievedBuffers = 0;
		while (nRetrievedBuffers < queueCapacity) {
			try {
				freeBuffers.poll(Long.MAX_VALUE, TimeUnit.DAYS);
				nRetrievedBuffers++;
			} catch (InterruptedException e) {
				Utils.log("waitForIOToFinish interrupted while waiting for writer thread", LogLevel.INFO);
				break;
			}
		}
		if (ioException != null)
			throw ioException;
		if (throwable != null)
			ParFor.UnsafeHolder.unsafe.throwException(throwable);
		// outChannel.force(true);
	}

	private volatile boolean allDone = false;

	private void checkAllocate(BufferPair bp) {
		if (bp.byteBuffer == null) {
			BufferPair allocated = allocateBuffer();
			bp.byteBuffer = allocated.byteBuffer;
			bp.typedBuffer = allocated.typedBuffer;
		}
	}

	private BufferPair allocateBuffer() {
		BufferPair bufferPair = new BufferPair();

		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bufferSize);
		byteBuffer.order(java.nio.ByteOrder.nativeOrder());
		bufferPair.byteBuffer = byteBuffer;
		switch (bytesPerPixel) {
			case 1:
				bufferPair.typedBuffer = byteBuffer;
				break;
			case 2:
				bufferPair.typedBuffer = byteBuffer.asShortBuffer();
				break;
			case 4:
				bufferPair.typedBuffer = byteBuffer.asFloatBuffer();
				break;
			default:
				throw new IllegalArgumentException("Unhandled pixel depth " + bytesPerPixel);
		}
		return bufferPair;
	}

	public void close() throws IOException {
		allDone = true;
		parFor.interrupt();
		if (ioException != null)
			throw ioException;
		if (throwable != null)
			ParFor.UnsafeHolder.unsafe.throwException(throwable);
	}

	volatile IOException ioException = null;
	volatile Throwable throwable = null;

	public ImageWriterNIO(FileInfo fi, int bytesPerPixel, RandomAccessFile out) {
		this.fi = fi;
		outChannel = out.getChannel();
		// for buffer allocation
		bufferSize = fi.width * fi.height * bytesPerPixel;
		this.bytesPerPixel = bytesPerPixel;

		for (int i = 0; i < queueCapacity; i++) {
			freeBuffers.add(new BufferPair());
		}

		final int nThreads = 1;
		// TODO Not set up for multiple writer threads at this point as
		// there is only 1 outChannel

		parFor = new ParFor("ImageWriter tasks", 0, nThreads - 1, null, true);
		parFor.addLoopWorker((index, threadIndex) -> {
			while (!allDone) {
				BufferPair bufferPair = null;
				try {
					bufferPair = pendingWrites.poll(Long.MAX_VALUE, TimeUnit.DAYS);
					outChannel.write(bufferPair.byteBuffer);
					// Add free buffer in first position so that if there is no need for
					// write buffering (for example if only 1 slice is written, or if there
					// is a large delay for the computation of every slice)
					// only 1 DirectArray gets allocated.
				} catch (InterruptedException e) {
					Utils.log("Writer thread interrupted", LogLevel.DEBUG);
					break;
				} catch (IOException e) {
					ioException = e;
					Utils.printStack(e);
					allDone = true;
					parFor.interrupt();
				} catch (Throwable e) {
					Utils.log("Unhandled throwable in writer thread", LogLevel.ERROR);
					Utils.printStack(e);
					throwable = e;
				} finally {
					if (bufferPair != null)
						freeBuffers.addFirst(bufferPair);
				}
			}
			return null;
		});

		parFor.runNonBlocking();

	}

	@SuppressWarnings({ "static-method", "unused" })
	void write8BitImage(byte[] pixels) throws IOException {
		throw new RuntimeException("Unimplemented");
		/*
		int bytesWritten = 0;
		int size = fi.width * fi.height;
		int count = 8192;

		while (bytesWritten < size) {
			if ((bytesWritten + count) > size)
				count = size - bytesWritten;
			// System.out.println(bytesWritten + " " + bufferSize + " " + size);
			out.write(pixels, bytesWritten, count);
			bytesWritten += count;
			showProgress((double) bytesWritten / size);
		}
		*/
	}

	@SuppressWarnings({ "static-method", "unused" })
	void write8BitStack(Object[] stack) throws IOException {
		throw new RuntimeException("Unimplemented");
		/*
		showProgressBar = false;
		for (int i = 0; i < fi.nImages; i++) {
			IJ.showStatus("Writing: " + (i + 1) + "/" + fi.nImages);
			write8BitImage((byte[]) stack[i]);
			IJ.showProgress((double) (i + 1) / fi.nImages);
		} */
	}

	@SuppressWarnings({ "static-method", "unused" })
	void write8BitVirtualStack(VirtualStack virtualStack) throws IOException {
		throw new RuntimeException("Unimplemented");
		/*
		showProgressBar = false;
		boolean flip = "FlipTheseImages".equals(fi.fileName);
		for (int i = 1; i <= fi.nImages; i++) {
			IJ.showStatus("Writing: " + i + "/" + fi.nImages);
			ImageProcessor ip = virtualStack.getProcessor(i);
			if (flip)
				ip.flipVertical();
			byte[] pixels = (byte[]) ip.getPixels();
			write8BitImage(pixels);
			IJ.showProgress((double) i / fi.nImages);
		}*/
	}

	@SuppressWarnings({ "static-method", "unused" })
	void write16BitImage(short[] pixels) throws IOException {
		throw new RuntimeException("Unimplemented");
		/*
		int bytesWritten = 0;
		int size = fi.width * fi.height * 2;
		int count = 8192;
		byte[] buffer = new byte[count];

		while (bytesWritten < size) {
			if ((bytesWritten + count) > size)
				count = size - bytesWritten;
			int j = bytesWritten / 2;
			int value;
			if (fi.intelByteOrder)
				for (int i = 0; i < count; i += 2) {
					value = pixels[j];
					buffer[i] = (byte) value;
					buffer[i + 1] = (byte) (value >>> 8);
					j++;
				}
			else
				for (int i = 0; i < count; i += 2) {
					value = pixels[j];
					buffer[i] = (byte) (value >>> 8);
					buffer[i + 1] = (byte) value;
					j++;
				}
			out.write(buffer, 0, count);
			bytesWritten += count;
			showProgress((double) bytesWritten / size);
		} */
	}

	@SuppressWarnings({ "static-method", "unused" })
	void write16BitStack(Object[] stack) throws IOException {
		throw new RuntimeException("Unimplemented");
		/*
		showProgressBar = false;
		for (int i = 0; i < fi.nImages; i++) {
			IJ.showStatus("Writing: " + (i + 1) + "/" + fi.nImages);
			write16BitImage((short[]) stack[i]);
			IJ.showProgress((double) (i + 1) / fi.nImages);
		}
		*/
	}

	@SuppressWarnings({ "static-method", "unused" })
	void write16BitVirtualStack(VirtualStack virtualStack) throws IOException {
		throw new RuntimeException("Unimplemented");
		/*
		showProgressBar = false;
		boolean flip = "FlipTheseImages".equals(fi.fileName);
		for (int i = 1; i <= fi.nImages; i++) {
			IJ.showStatus("Writing: " + i + "/" + fi.nImages);
			ImageProcessor ip = virtualStack.getProcessor(i);
			if (flip)
				ip.flipVertical();
			short[] pixels = (short[]) ip.getPixels();
			write16BitImage(pixels);
			IJ.showProgress((double) i / fi.nImages);
		}
		*/
	}

	@SuppressWarnings({ "static-method", "unused" })
	void writeRGB48Image(Object[] stack) throws IOException {
		throw new RuntimeException("Unimplemented");
		/*
		short[] r = (short[]) stack[0];
		short[] g = (short[]) stack[1];
		short[] b = (short[]) stack[2];
		int count = fi.width * 6;
		byte[] buffer = new byte[count];
		for (int line = 0; line < fi.height; line++) {
			int index2 = 0;
			int index1 = line * fi.width;
			int value;
			if (fi.intelByteOrder) {
				for (int i = 0; i < fi.width; i++) {
					value = r[index1];
					buffer[index2++] = (byte) value;
					buffer[index2++] = (byte) (value >>> 8);
					value = g[index1];
					buffer[index2++] = (byte) value;
					buffer[index2++] = (byte) (value >>> 8);
					value = b[index1];
					buffer[index2++] = (byte) value;
					buffer[index2++] = (byte) (value >>> 8);
					index1++;
				}
			} else {
				for (int i = 0; i < fi.width; i++) {
					value = r[index1];
					buffer[index2++] = (byte) (value >>> 8);
					buffer[index2++] = (byte) value;
					value = g[index1];
					buffer[index2++] = (byte) (value >>> 8);
					buffer[index2++] = (byte) value;
					value = b[index1];
					buffer[index2++] = (byte) (value >>> 8);
					buffer[index2++] = (byte) value;
					index1++;
				}
			}
			out.write(buffer, 0, count);
		}
		*/
	}

	private int bytesPerPixel;
	private int bufferSize;

	/**
	 * The caller is responsible for ensuring that buffer is of the right size, and that slices are
	 * sequentially written.
	 * 
	 * @param buffer
	 * @param ignoredPosition
	 *            offset in the file; IGNORED FOR NOW
	 * @throws IOException
	 * @throws InterruptedException
	 */
	final public int dumpBufferAtPosition(ByteBuffer buffer, long ignoredPosition) throws IOException,
			InterruptedException {
		if (ioException != null)
			throw ioException;
		BufferPair bufferPair = null;
		try {
			bufferPair = freeBuffers.pollFirst(Long.MAX_VALUE, TimeUnit.DAYS);
			checkAllocate(bufferPair);
			ByteBuffer byteBuffer2 = bufferPair.byteBuffer;
			byteBuffer2.clear();
			byteBuffer2.put(buffer);
			byteBuffer2.flip();
			buffer.clear();
			pendingWrites.add(bufferPair);
			bufferPair = null;
		} finally {
			if (bufferPair != null)
				freeBuffers.add(bufferPair);
		}
		return buffer.capacity();
	}

	private void writeImage(Object pixels) throws InterruptedException, IOException {
		if (ioException != null)
			throw ioException;
		BufferPair bufferPair = null;
		try {
			bufferPair = freeBuffers.pollFirst(Long.MAX_VALUE, TimeUnit.DAYS);
			checkAllocate(bufferPair);
			bufferPair.typedBuffer.clear();
			if (pixels instanceof float[]) {
				((FloatBuffer) bufferPair.typedBuffer).put((float[]) pixels);
			} else if (pixels instanceof short[]) {
				((ShortBuffer) bufferPair.typedBuffer).put((short[]) pixels);
			} else if (pixels instanceof byte[]) {
				bufferPair.byteBuffer.put((byte[]) pixels);
			} else
				throw new IllegalArgumentException("Unsupported pixel type " + pixels);
			bufferPair.byteBuffer.clear();
			pendingWrites.add(bufferPair);
			bufferPair = null;
		} finally {
			if (bufferPair != null)
				freeBuffers.add(bufferPair);
		}
	}

	@SuppressWarnings({ "static-method", "unused" })
	private void writeFloatStack(Object[] stack) throws InterruptedException, IOException {
		throw new RuntimeException("Unimplemented");
		/*
		showProgressBar = false;
		for (int i = 0; i < fi.nImages; i++) {
			IJ.showStatus("Writing: " + (i + 1) + "/" + fi.nImages);
			writeImage(stack[i]);
			IJ.showProgress((double) (i + 1) / fi.nImages);
		} */
	}

	@SuppressWarnings({ "static-method", "unused" })
	private void writeFloatVirtualStack(VirtualStack virtualStack) throws InterruptedException, IOException {
		throw new RuntimeException("Unimplemented");
		/*
		showProgressBar = false;
		boolean flip = "FlipTheseImages".equals(fi.fileName);
		for (int i = 1; i <= fi.nImages; i++) {
			IJ.showStatus("Writing: " + i + "/" + fi.nImages);
			ImageProcessor ip = virtualStack.getProcessor(i);
			if (flip)
				ip.flipVertical();
			float[] pixels = (float[]) ip.getPixels();
			writeImage(pixels);
			IJ.showProgress((double) i / fi.nImages);
		} */
	}

	@SuppressWarnings({ "static-method", "unused" })
	void writeRGBImage(int[] pixels) throws IOException {
		throw new RuntimeException("Unimplemented");
		/*
		int bytesWritten = 0;
		int size = fi.width * fi.height * 3;
		int count = fi.width * 24;
		byte[] buffer = new byte[count];
		while (bytesWritten < size) {
			if ((bytesWritten + count) > size)
				count = size - bytesWritten;
			int j = bytesWritten / 3;
			for (int i = 0; i < count; i += 3) {
				buffer[i] = (byte) (pixels[j] >> 16); // red
				buffer[i + 1] = (byte) (pixels[j] >> 8); // green
				buffer[i + 2] = (byte) pixels[j]; // blue
				j++;
			}
			out.write(buffer, 0, count);
			bytesWritten += count;
			showProgress((double) bytesWritten / size);
		}
		*/
	}

	@SuppressWarnings({ "static-method", "unused" })
	void writeRGBStack(Object[] stack) throws IOException {
		throw new RuntimeException("Unimplemented");
		/*
		showProgressBar = false;
		for (int i = 0; i < fi.nImages; i++) {
			IJ.showStatus("Writing: " + (i + 1) + "/" + fi.nImages);
			writeRGBImage((int[]) stack[i]);
			IJ.showProgress((double) (i + 1) / fi.nImages);
		} */
	}

	@SuppressWarnings({ "static-method", "unused" })
	void writeRGBVirtualStack(VirtualStack virtualStack) throws IOException {
		throw new RuntimeException("Unimplemented");
		/*
		showProgressBar = false;
		boolean flip = "FlipTheseImages".equals(fi.fileName);
		for (int i = 1; i <= fi.nImages; i++) {
			IJ.showStatus("Writing: " + i + "/" + fi.nImages);
			ImageProcessor ip = virtualStack.getProcessor(i);
			if (flip)
				ip.flipVertical();
			int[] pixels = (int[]) ip.getPixels();
			writeRGBImage(pixels);
			IJ.showProgress((double) i / fi.nImages);
		} */
	}

	/**
	 * Writes the image to the specified RandomAccessFile.
	 * The RandomAccessFile is not closed. The fi.pixels field
	 * must contain the image data. If fi.nImages>1
	 * then fi.pixels must be a 2D array, for example an
	 * array of images returned by ImageStack.getImageArray()).
	 * The fi.offset field is ignored.
	 * 
	 * @throws InterruptedException
	 */
	public final void write() throws IOException, InterruptedException {
		if (fi.pixels == null && fi.virtualStack == null)
			throw new IOException("ImageWriterNIO: fi.pixels==null");
		if (fi.nImages > 1 && fi.virtualStack == null && !(fi.pixels instanceof Object[]))
			throw new IOException("ImageWriterNIO: fi.pixels not a stack");
		switch (fi.fileType) {
			case FileInfo.GRAY8:
			case FileInfo.COLOR8:
				if (fi.nImages > 1 && fi.virtualStack != null)
					write8BitVirtualStack(fi.virtualStack);
				else if (fi.nImages > 1)
					write8BitStack((Object[]) fi.pixels);
				else
					writeImage(fi.pixels);
				break;
			case FileInfo.GRAY16_SIGNED:
			case FileInfo.GRAY16_UNSIGNED:
				if (fi.nImages > 1 && fi.virtualStack != null)
					write16BitVirtualStack(fi.virtualStack);
				else if (fi.nImages > 1)
					write16BitStack((Object[]) fi.pixels);
				else
					writeImage(fi.pixels);
				break;
			case FileInfo.RGB48:
				writeRGB48Image((Object[]) fi.pixels);
				break;
			case FileInfo.GRAY32_FLOAT:
				if (fi.nImages > 1 && fi.virtualStack != null)
					writeFloatVirtualStack(fi.virtualStack);
				else if (fi.nImages > 1)
					writeFloatStack((Object[]) fi.pixels);
				else
					writeImage(fi.pixels);
				break;
			case FileInfo.RGB:
				if (fi.nImages > 1 && fi.virtualStack != null)
					writeRGBVirtualStack(fi.virtualStack);
				else if (fi.nImages > 1)
					writeRGBStack((Object[]) fi.pixels);
				else
					writeRGBImage((int[]) fi.pixels);
				break;
			default:
		}
	}

}
