/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
// -*- Mode: Java; c-basic-offset: 4 -*-
package nu.dll.lyskom;

import nu.dll.io.SpyInputStream;

import java.io.*;
import java.net.ProtocolException;
import java.util.Vector;
import java.util.Enumeration;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;


class KomTokenReader {

    final static int DEBUG = Integer.getInteger("lattekom.debug-parser", 0).intValue();
    static {
	if (DEBUG > 0) {
	    Debug.println("KomTokenReader debug level: " + DEBUG);
	}
    }

    private InputStream input;
    private Session session;

    boolean lastByteWasEol = false;

    KomToken lastToken = null;

    public KomTokenReader(InputStream i, Session session) {
	if (DEBUG > 3) {
	    try {
		input = new SpyInputStream(i, new FileOutputStream("/lyskom-trace.log"));
	    } catch (IOException ex1) {
		throw new RuntimeException("I/O error adding snoop stream", ex1);
	    }
	} else {
	    input = i;
	}
	this.session = session;
    }

    public void close() throws IOException {
	input.close();
    }

    protected static KomTokenArray readArray(KomToken prefix, InputStream is, String charset)
    throws IOException {
	if (prefix == null) {
	    throw new IllegalArgumentException("KomTokenReader.readArray(): " +
					       "prefix was null");
	}
	int length = prefix.intValue();
	List v = new LinkedList();
	KomToken beforeLast = null;
        KomToken last = readToken(null, -1, is, charset);

	byte[] lspre = last.getContents();
	boolean arrayCompleted = lspre[0] == '}';
	while (!arrayCompleted) {
	    if (lspre != null && lspre.length != 0 && lspre[0] == '{') {
		v.add(readArray(beforeLast, is, charset));
	    } else {
		v.add(last);
	    }
	    
	    beforeLast = last;
	    last = readToken(last, -1, is, charset);

	    lspre = last.getContents();
	    arrayCompleted = lspre != null && lspre.length != 0 && lspre[0] == '}';
	}
	KomToken[] arr = new KomToken[v.size()];
	Iterator vIter = v.iterator();
	for (int i=0;i<v.size();i++) {
	    arr[i] = (KomToken) vIter.next();
	}
	if (DEBUG>1) Debug.println("Array end ("+arr.length+")");

	KomTokenArray ktArr = new KomTokenArray(length, arr);
	ktArr.setEol(last.isEol());
	return ktArr;
    }

    public KomToken[] readLine()
    throws ProtocolException, IOException {
	Vector v = new Vector();
	while (!lastByteWasEol)
	    v.addElement(readToken());

	lastByteWasEol = false;

	KomToken[] foo = new KomToken[v.size()];

	Enumeration e = v.elements();
	for (int i=0;i<foo.length;i++)
	    foo[i] = (KomToken) e.nextElement();

	return foo;
    }

    protected KomToken readToken() 
    throws IOException, ProtocolException {
	return readToken(-1);
    }

    protected static KomToken readToken(InputStream is, String charset)
    throws IOException, ProtocolException {
	return readToken(null, -1, is, charset);
    }

    protected static KomToken readToken(KomToken _lastToken, int hollerithLimit, InputStream is, String charset)
    throws IOException, ProtocolException {
	ByteArrayOutputStream os = new ByteArrayOutputStream(32);
	boolean readMore = true;
	int arrlen = -1;
	byte b = 0;
	byte lastB = 0;
	boolean wasEol = false;
	KomToken token = null;
	if (Debug.ENABLED && hollerithLimit > -1) {
	    Debug.println("readToken(" + hollerithLimit + ")");
	}
	boolean tokenCompleted = false;
	while (!tokenCompleted) {
	    lastB = b;
	    b = (byte) is.read();
	    //Debug.println(new String((char) b));
	    if (b == '\n')
		wasEol = true;
	    else
		wasEol = false;
	    
	    switch ((int)b) {
	    case -1:
		// if EOF is reached and there are bytes in the buffer,
		// treat it as an end-of-token. If the buffer is empty
		// and no token has been created, return null.
		wasEol = true;
		tokenCompleted = true;
		if (token == null && os.size() == 0) {
		    break;
		}
	    case '\n':
		wasEol = true;
	    case ' ':
		if (lastB == b)
		    break;

		// this is leading whitespace which can be ignored
		if (token == null && os.size() == 0) { 
		    break;
		}

		if (token == null) {
		    token = new KomToken(os.toByteArray());
		}
		tokenCompleted = true;
		break;
	    case '*':
		arrlen = (_lastToken != null ? _lastToken.intValue() : -1);
		token = (KomToken) new KomTokenArray(arrlen);
		break;
	    case '{':
		is.read(); // eat up leading space
		token = (KomToken) readArray(_lastToken, is, charset);
		wasEol = token.isEol();
		tokenCompleted = true;
		break;
	    case 'H':
		try {
		    arrlen = Integer.parseInt(new String(os.toByteArray(), charset));
		} catch (NumberFormatException x) {
		    throw(new KomProtocolException("Bad hollerith \""+
						   new String(os.toByteArray()) + "\"?"));
		}
		if (hollerithLimit != -1 && arrlen > hollerithLimit) {
		    if (Debug.ENABLED)
			Debug.println("Returning HollerithStream of " + arrlen + " bytes");
		    token = new HollerithStream(is, arrlen, charset);
		    tokenCompleted = true;
		    break;
		}
		byte[] hstring = new byte[arrlen];
		int bytesRead = readFill(is, hstring);
		if (bytesRead != hstring.length) {
		    throw new IOException("Expected " + hstring.length +
					  " bytes in hollerith, got " +
					  bytesRead);
		}
		if (is.read() == '\n') { // eat trailing space/cr
		    wasEol = true;
		} else {
		    wasEol = false;
		}
	        token = new Hollerith(hstring, charset);
		tokenCompleted = true;
		break;
	    default:
		os.write(b);
	    }
	}
	
	// only change EOL flag if it isn't alreay set
	if (token != null && !token.isEol()) token.setEol(wasEol);
	if (DEBUG > 2) {
	    if (token != null) {
		Debug.println("readToken(): " + token + (token.isEol() ? " (END OF LINE)" : ""));
	    } else {
		Debug.println("readToken(): EOF");
	    }
	}
	return token;
    }

    private static int readFill(InputStream is, byte[] data)
    throws IOException {
	int bytesRead = 0;
	int offset = 0;
	int totalBytes = 0;
	while (totalBytes < data.length && bytesRead != -1) {
	    bytesRead = is.read(data, totalBytes, data.length-totalBytes);
	    totalBytes += bytesRead;
	    if (Debug.ENABLED) {
		Debug.println("read " + bytesRead + " bytes into buffer " + 
			      "(total " + totalBytes + ", expecting " + 
			      data.length + ")");
	    }
	}
	return totalBytes;
    }

    protected KomToken readToken(int hollerithLimit)
    throws IOException, ProtocolException {
	KomToken token = readToken(lastToken, hollerithLimit, input, session.getServerEncoding());
	lastToken = token;
	lastByteWasEol = token.isEol();
	return token;
    }
}



