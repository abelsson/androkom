package nu.dll.lyskom;

import java.io.*;

class HollerithInputStream extends InputStream {
    HollerithStream hs;
    InputStream is;

    int readCount = 0;

    public HollerithInputStream(HollerithStream hs) throws IOException {
	this.hs = hs;
	this.is = hs.stream;
    }

    public int read() throws IOException {
	if (readCount >= hs.getSize()) {
	    hs.setExhausted();
	    return -1;
	}
	int v = is.read();
	readCount++;
	Debug.println(this + ".read() (" + (hs.getSize()-readCount) + ")");
	return v;
    }

    public int read(byte[] b) throws IOException {
	Debug.println(this + ".read(byte[])");
	return read(b, 0, b.length);
    }

    public int read(byte[] b, int offset, int len) throws IOException {
	if (hs.isExhausted()) return -1;
	int bytesLeft = hs.getSize() - readCount;
	if (bytesLeft == 0) {
	    hs.setExhausted();
	    return -1;
	}
	if (bytesLeft < len) {
	    len = bytesLeft;
	}
	int bytesRead = is.read(b, offset, len);
	if (bytesRead > -1) readCount += bytesRead;

	if (readCount >= hs.getSize()) hs.setExhausted();
	Debug.println(this + ".read(byte[], int, int) (" + (hs.getSize()-readCount) + ")");
	return bytesRead;
    }

    public long skip(long n) throws IOException {
	Debug.println(this + ".skip(long)");
	int bytesLeft = hs.getSize() - readCount;
	if (bytesLeft < n) {
	    n = bytesLeft;
	}
	long skipped = is.skip(n);
	readCount += skipped;
	if (readCount >= hs.getSize()) hs.setExhausted();
	return skipped;
    }

    public int available() throws IOException {
	Debug.println(this + ".available()");
	int isAvailable = is.available();
	int bytesLeft = hs.getSize() - readCount;
	if (isAvailable > bytesLeft) isAvailable = bytesLeft;
	return isAvailable;
    }

    public void close() throws IOException {
	Debug.println(this + ".close()");
	hs.cancel(hs.getSize() - readCount);
    }

    public void mark(int limit) {}

    public void reset() throws IOException {
	throw new IOException("mark/reset not supported by this stream");
    }
    
    public boolean markSupported() {
	return false;
    }
    
}
