/**
 *
 */
package nu.dll.lyskom;

import java.io.InputStream;
import java.io.IOException;

/**
 * A HollerithStream provides an InputStream from which the contents of a
 * Hollerith sent from the server can be read.
 * 
 * It's main purpose is to enable effecient handling of very large texts, where
 * it is not desirable to allocate the entire text contents in memory.
 * 
 * The provided InputStream is a direct interface to the LysKOM server stream,
 * which means that care must be taken not to interfere with the regular
 * protocol parsing.
 * 
 * When a HollerithStream has been retreived, all further reads from the server
 * are blocked until the object's setExhausted() method has been called,
 * signalling to the server reader that the stream has been read and that it may
 * continue normal parsing.
 * 
 * The InputStream returned by getStream() MUST NOT be closed through its
 * close() method. Also it MUST NOT be read beyond the size of the
 * HollerithStream as reported by the HollerithStream object's getSize() method.
 * If an application does not want to read the entire contents of the Hollerith,
 * it MUST call the cancel() method of the HollerithStream in order to let the
 * rest of the contents to be discarded.
 * 
 * @author Rasmus Sten
 */
public class HollerithStream extends Hollerith {
	private static final long serialVersionUID = -5690560997150845158L;

	protected InputStream stream;
	protected InputStream wrappedStream = null;

	int size;
	boolean exhausted = false;
	protected HollerithStream(InputStream is, int size, String charset) {
		super();
		this.size = size;
		setCharset(charset);
		this.stream = is;
	}

	/**
	 * Returns the size of the represented hollerith.
	 */
	public int getSize() {
		return size;
	}

	protected void setSize(int size) {
		this.size = size;
	}

	/**
	 * Returns an InputStream from which the contents can be read.
	 * 
	 * The returned InputStream must be treated carefully in order not to bring
	 * other server I/O out of sync. All other server read operations are
	 * blocked until the stream has been read and marked as such by calling
	 * setExhausted(), or closed.
	 * 
	 * If an application wants to abort reading of the InputStream, it _must_
	 * call close() in order to discard the remaining data and exhaust this
	 * HollerithStream.
	 * 
	 * Failure in closing the InputStream or calling setExhausted() or cancel()
	 * WILL cause LatteKOM to block indefinitely.
	 * 
	 * Calling setExhausted() prematurely (before the entire contents of the
	 * hollerith has been read) WILL cause LatteKOM to loose track of the server
	 * stream and corrupt subsequent incoming data.
	 * 
	 * This method may not be called once the stream has been exhausted.
	 * 
	 * @throws IllegalStateException
	 *             if the stream has already been marked as exhausted
	 */
	public InputStream getStream() throws IOException {
		if (isExhausted()) {
			throw new IllegalStateException("HollerithStream already exhausted");
		}
		if (wrappedStream == null) {
			return wrappedStream = new HollerithInputStream(this);
		} else {
			return wrappedStream;
		}
	}

	/**
	 * Equal to cancel(0).
	 * 
	 * Convenience method to cancel a stream from which nothing has been read.
	 */
	public void cancel() throws IOException {
		cancel(0);
	}

	/**
	 * Discards any remaining bytes in the InputStream, then marks the stream as
	 * exhausted.
	 * 
	 */
	public void cancel(int offset) throws IOException {
		for (int i = 0; i < getSize() - offset; i++)
			stream.read();

		setExhausted();
	}

	/**
	 * Marks the stream as exhausted.
	 * 
	 * LatteKOM will resume regular server I/O as soon as this method has been
	 * called. The application MUST NOT perform any further I/O on the supplied
	 * InputStream after this method has been called.
	 */
	public void setExhausted() {
		synchronized (this) {
			Debug.println(this + ".setExhausted()");
			exhausted = true;
			notifyAll();
		}
	}

	/**
	 * Returns true if the provided InputStream is exhausted.
	 */
	public boolean isExhausted() {
		return exhausted;
	}

	/**
	 * Returnes a byte-array containing all data in the hollerith.
	 * 
	 * This method may only be called once, since it reads and exhausts the
	 * InputStream. If it is called on an already exhausted stream,
	 * IllegalStateException will be thrown.
	 */
	public byte[] getContents() {
		if (isExhausted())
			throw new IllegalStateException(
					"Cannot call getContents() twice on a HollerithStream");

		System.err
				.println("Warning: getContents() called on a HollerithStream object");
		byte[] buffer = new byte[size];
		try {
			stream.read(buffer, 0, size);
		} catch (IOException ex1) {
			throw new RuntimeException("while reading HollerithStream", ex1);
		}
		setExhausted();
		return buffer;
	}

	public String toString() {
		return "[" + getClass().getName() + ": stream " + stream.toString()
				+ ", " + getSize() + " bytes]";
	}
}
