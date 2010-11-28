package nu.dll.lyskom.io;

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
public class SpyOutputStream extends FilterOutputStream {
    private long byteCount = 0;
    private List<OutputStream> spies = new LinkedList<OutputStream>();
    private long statusLimit = 100;
    private boolean autoStatus = false;

    public SpyOutputStream(OutputStream out) {
        super(out);
    }

    public SpyOutputStream(OutputStream out1, OutputStream out2) {
        super(out1);
        spies.add(out2);
    }

    public void addSpy(OutputStream spy) {
        synchronized (spies) {
            spies.add(spy);
        }
    }

    public void removeSpy(OutputStream spy) {
        synchronized (spies) {
            spies.remove(spy);
        }
    }

    public void setAutoStatus(boolean B) {
        autoStatus = B;
    }

    private void writeToSpies(int b) throws IOException {
        synchronized (spies) {
            Iterator<OutputStream> i = spies.iterator();
            while (i.hasNext()) {
                ((OutputStream) i.next()).write(b);
            }
        }
    }

    private void writeToSpies(byte[] b, int off, int len) throws IOException {
        synchronized (spies) {
            Iterator<OutputStream> i = spies.iterator();
            while (i.hasNext()) {
                ((OutputStream) i.next()).write(b, off, len);
            }
        }
    }

    public void write(int b) throws IOException {
        super.write(b);
        byteCount++;
        if (spies.size() > 0)
            writeToSpies(b);
        if (autoStatus)
            writeStatusMaybe();
    }

    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
        byteCount += len;
        if (spies.size() > 0)
            writeToSpies(b, off, len);
        if (autoStatus)
            writeStatusMaybe();
    }

    private long lastStatusBytes = 0;

    public void writeStatusMaybe() {
        long sinceLast = byteCount - lastStatusBytes;
        if (sinceLast > statusLimit) {
            trace(byteCount + " bytes written (" + sinceLast + " since last)");
            lastStatusBytes = byteCount;
        }
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

    public void writeStatus() {
        trace(byteCount + " bytes written.");
    }

    public long getCount() {
        return byteCount;
    }

    long lastByteCount = 0;

    public long getCountOff() {
        long r = byteCount - lastByteCount;
        lastByteCount = byteCount;
        return r;
    }

    public void close() throws IOException {
        if (autoStatus)
            writeStatus();
        super.close();
    }

    protected static void trace(String s) {
        SpyInputStream.trace(s);
    }

    public void finalize() {
        if (autoStatus)
            writeStatus();
    }

}
