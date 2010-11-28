package nu.dll.io;

import java.io.*;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;

/**
 * The SpyInputStream and SpyOutputStream classes are Input- and OutputStream
 * filters that enabled "spying" on input and output streams by attaching
 * OutputStream objects as "spies", to which, whenever data is written to or
 * read from the underlying stream, exactly the same data is written to all
 * currently attached spy output streams.
 * <p>
 * The eavesdroppers can be attached and removed through the addSpy() and
 * removeSpy() methods, or by the two-args constructor which takes an output
 * stream to dump the data to as the second argument. An arbitrary number of spy
 * streams can be attached, but it inevitably affects the performance, since
 * read() and write() methods blocks until all data has been written to all
 * eavesdropping output streams.
 * <p>
 * The data is written to the attached eavesdroppers using the same methods by
 * which the SpyStream object is called, or, in the case of SpyInputStream, each
 * read() call triggers it's eavesdropper's corresponding write() call.
 * <p>
 * This class also contains the functionality to print out debug information
 * every <i>n</i> bytes, where <i>n</i> is set by the setStatusLimit(int) method
 * (defaults to 100 bytes). It also keeps track of the total number of bytes
 * passed through the underlying stream. This is useful to debug the efficiency
 * of bulk write() calls, for example.
 * 
 */
public class SpyInputStream extends FilterInputStream {
    private final static long initTime = System.currentTimeMillis();
    private long byteCount = 0;
    private List<OutputStream> spies = new LinkedList<OutputStream>();
    private long statusLimit = 100;
    private boolean autoStatus = false;

    /**
     * Constructs a SpyInputStream with no eavesdropping output stream.
     * 
     * @param in
     *            the underlying InputStream from which to read data from
     */
    public SpyInputStream(InputStream in) {
        super(in);
    }

    /**
     * Constructs a SpyInputStream with one eavesdropping output stream.
     * 
     * @param in
     *            the underlying InputStream from which to read data from
     * @param out
     *            an OutputStream to write all read data to
     */
    public SpyInputStream(InputStream in, OutputStream out) {
        super(in);
        spies.add(out);
    }

    /**
     * Adds an OutputStream as an eavesdropper to this stream.
     * 
     * @param spy
     *            an OutputStream to write all read data to
     */
    public void addSpy(OutputStream spy) {
        synchronized (spies) {
            spies.add(spy);
        }
    }

    /**
     * Removes the supplied OutputStream from the list of eavesdroppers.
     * 
     * @param spy
     *            the OutputStream to be removed
     */
    public void removeSpy(OutputStream spy) {
        synchronized (spies) {
            spies.remove(spy);
        }
    }

    /**
     * Tells this SpyInputStream to automatically print status messages about
     * the total number of bytes written, to stdout, every <i>n</i> bytes passed
     * through the underlying input stream.
     * 
     * @param B
     *            if <code>true</code>, debug data will be written
     * @see #setStatusLimit(long)
     */
    public void setAutoStatus(boolean B) {
        autoStatus = B;
    }

    /**
     * Sets the treshold of the number of bytes to pass before automatically
     * writing debug info to stdout.
     * 
     * @param I
     *            the number of bytes
     * @see #setAutoStatus(boolean)
     */
    public void setStatusLimit(long I) {
        statusLimit = I;
    }

    /**
     * Writes the byte 'b' to all eavesdropping output streams.
     */
    private void writeToSpies(int b) throws IOException {
        synchronized (spies) {
            Iterator<OutputStream> i = spies.iterator();
            while (i.hasNext()) {
                ((OutputStream) i.next()).write(b);
            }
        }
    }

    /**
     * Calls <code>write(byte[], int, int)</code> on all eavesdropping output
     * streams.
     */
    private void writeToSpies(byte[] b, int off, int len) throws IOException {
        synchronized (spies) {
            Iterator<OutputStream> i = spies.iterator();
            while (i.hasNext()) {
                ((OutputStream) i.next()).write(b, off, len);
            }
        }
    }

    /**
     * Reads one byte from the underlying input stream, incrementing the byte
     * count by one if successful, and writes the byte to all eavesdropping
     * output stream. Perhaps writes debug info to stdout if
     * <code>autoStatus</code> is <code>true</code>
     * 
     * @see #setAutoStatus(boolean)
     * @see #setStatusLimit(long)
     * @see java.io.InputStream#read()
     */
    public int read() throws IOException {
        int b = super.read();
        if (b != -1)
            byteCount++;
        if (spies.size() > 0)
            writeToSpies(b);
        if (autoStatus)
            writeStatusMaybe();
        return b;
    }

    /**
     * Reads <code>len</code> bytes from the underlying input stream, placing
     * them into the supplied byte array <code>b</code> at the supplied offset
     * <code>off</code>, according the general contract of
     * <code>java.io.InputStream.read(byte[], int, int)</code>. Additionally,
     * increments the byte count by the number of bytes actually read, and
     * writes the bytes to all eavesdropping output stream. Perhaps writes debug
     * info to stdout if <code>autoStatus</code> is <code>true</code>
     * 
     * @see #setAutoStatus(boolean)
     * @see #setStatusLimit(long)
     * @see java.io.InputStream#read(byte[], int, int)
     */
    public int read(byte[] b, int off, int len) throws IOException {
        int c = in.read(b, off, len);
        byteCount += c;
        if (spies.size() > 0)
            writeToSpies(b, off, len);
        if (autoStatus)
            writeStatusMaybe();
        return c;
    }

    private long lastStatusBytes = 0;

    /**
     * If the number of bytes read since the last call to this method exceeds
     * the value of <code>statusLimit</code> (set by
     * <code>setStatusLimit()</code>), debug data containing the number of bytes
     * totally read and and since last call is written to
     * <code>System.out</code>.
     * 
     * @see #setStatusLimit(long)
     * @see #setAutoStatus(boolean)
     */
    public void writeStatusMaybe() {
        long sinceLast = byteCount - lastStatusBytes;
        if (sinceLast > statusLimit) {
            trace(byteCount + " bytes read (" + sinceLast + " since last)");
            lastStatusBytes = byteCount;
        }
    }

    /**
     * Writes the total number of bytes read to <code>System.out</code>
     * 
     * @see #getCount()
     */
    void writeStatus() {
        trace(byteCount + " bytes read.");
    }

    /**
     * Returns the total number of bytes passed through the underlying
     * OutputStream.
     */
    public long getCount() {
        return byteCount;
    }

    private long lastByteCount = 0;

    /**
     * Returns the number of bytes read since the last call to this method.
     */
    public long getCountOff() {
        long r = byteCount - lastByteCount;
        lastByteCount = byteCount;
        return r;
    }

    /**
     * Closes the underlying input stream, but not any eavesdropping output
     * streams.
     */
    public void close() throws IOException {
        if (autoStatus)
            writeStatus();
        super.close();
    }

    /**
     * If <code>autoStatus</code>, write the byte count upon object
     * finalization.
     */
    public void finalize() {
        if (autoStatus)
            writeStatus();
    }

    protected static void trace(String s) {
        System.out.println((System.currentTimeMillis() - initTime) + "\t"
                + Thread.currentThread().getName() + ": " + s);
    }

}
