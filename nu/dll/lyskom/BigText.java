package nu.dll.lyskom;

import java.io.InputStream;
import java.io.IOException;
/**
 * (EXPERIMENTAL)
 *
 * A "big" text, intended to efficiently handle large messages such
 * as binary files, often seen in mail attachments.
 *
 * The contents of a BigText is never stored in memory. Rather, the
 * BigText objects stores a reference of the Session that contains
 * the data, and retreives it on demand.
 *
 * Care must be taken when handling the HollerithStream objects
 * returned by this class, since they provide a direct interface
 * to the server, and may as such screw up server I/O if not
 * treated properly. See the HollerithStream API documentation for
 * more information.
 *
 * An application that uses the big-text feature of LatteKOM should
 * test with instanceof whether a text is a Text or a BigText object.
 * If it is a BigText, it should use getBodyStream() instead of
 * getBody() in order to read out the texts contents.
 *
 * To enable the big-text feature, set the system property
 * "lyskom.big-text" to "true" (defaults to false). LatteKOM will then
 * use the values of the system properties "lyskom.big-text-limit"
 * (default 51200 bytes) and "lyskom.big-text-head" (default 100 bytes)
 * to determine the maximum text size for RAM-allocatable texts, and the
 * text truncation limit, respectively.
 *
 * If a text exceeds the big-text-limit, a BigText stub object will be
 * created, containing only the first big-text-head bytes. The default
 * value of big-text-head is usually sufficient to extract the subject,
 * and in most aspects, a BigText object can be treated as a regular 
 * Text. Care should be taken not to call getContents() of a BigText
 * object, since it will defeat it's purpose of memory efficiency, and
 * instead place an extra burden on both client and server, as the
 * entire text contents will be requested from the server on each call
 * and mapped into memory, but never cached.
 *
 * @see nu.dll.lyskom.HollerithStream
 * @author Rasmus Sten
 */
public class BigText extends Text {
    Session session;

    protected BigText(Session session, int textNo) {
	this.textNo = textNo;
	this.session = session;
    }


    /**
     * Returns a HollerithStream with the body of this text.
     *
     * @see nu.dll.lyskom.HollerithStream
     */
    public HollerithStream getBodyStream() 
    throws IOException, RpcFailure {
	HollerithStream hs = session.getTextStream(getNo(), 0, getStat().getSize());
	InputStream is = hs.getStream();
	int count = 0;

	for (int b = is.read(); b != '\n' && b != -1; b = is.read()) {
	    count++;
	}

	hs.setSize(hs.getSize()-count);
	return hs;
    }

    /**
     * Returns the entire contents (subject + "\n" + body) of this text as a stream.
     *
     * @see nu.dll.lyskom.HollerithStream
     */
    public HollerithStream getContentStream()
    throws IOException, RpcFailure {
	return session.getTextStream(getNo(), 0, getStat().getSize());
    }

    public InputStream getInputStream() throws IOException {
	try {
	    return getBodyStream().getStream();
	} catch (RpcFailure ex1) {
	    if (Debug.ENABLED) ex1.printStackTrace();
	    throw new IOException("Stream error:" + ex1);
	}
    }

    /**
     * Returns a byte array containing this texts entire contents.
     *
     * Note that this is very inefficient when dealing with very large
     * "texts".
     */
    public byte[] getContents() {
	try {
	    Debug.println("Warning: getContents() called on BigText");
	    byte[] bytes = new byte[getStat().getSize()];
	    HollerithStream hs = getContentStream();
	    int count = KomTokenReader.readFill(hs.getStream(), bytes);
	    if (count != bytes.length) {
		Debug.println("BigText.getContents(): Warning: " + 
			      count + " != " + bytes.length);
	    }
	    hs.setExhausted();
	    return bytes;
	} catch (IOException ex1) {
	    throw new RuntimeException("I/O error while retreiving hollerith stream", ex1);
	}
    }

}
