/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
// -*- Mode: Java; c-basic-offset: 4 -*-
package nu.dll.lyskom;

import java.io.*;
import java.net.ProtocolException;
import java.util.Vector;
import java.util.Enumeration;

class KomTokenReader {

    final static int DEBUG = Integer.getInteger("lattekom.debug-parser", 0).intValue();
    static {
	if (DEBUG > 0) {
	    Debug.println("KomTokenReader debug lever: " + DEBUG);
	}
    }

    private InputStream input;
    private Session session;

    boolean lastByteWasEol = false;

    KomToken lastToken = null;

    public KomTokenReader(InputStream i, Session session) {
	input = i;
	this.session = session;
    }

    public void close() throws IOException {
	input.close();
    }

    public KomTokenArray readArray()
    throws IOException {
	if (lastToken == null) {
	    throw new KomProtocolException("KomTokenReader.readArray(): " +
					   "lastToken was null");
	}
	int length = lastToken.toInteger();
	Vector v = new Vector();
        KomToken last = readToken();
	String lspre = new String(last.toNetwork());
	while (!lspre.equals("}")) {
	    if (lspre.equals("{"))
		v.addElement((Object) readArray());
	    else		      
		v.addElement((Object) last);

	    last = readToken(); lspre = new String(last.toNetwork());
	}
	KomToken[] arr = new KomToken[v.size()];
	Enumeration e = v.elements();
	for (int i=0;i<v.size();i++) {
	    arr[i] = (KomToken) e.nextElement();	    
	}
	if (DEBUG>1) Debug.println("Array end ("+arr.length+")");
	return new KomTokenArray(length, arr);
    }

    StringBuffer debugBuffer;
    public KomToken[] readLine()
    throws ProtocolException, IOException {
	if (Debug.ENABLED) debugBuffer = new StringBuffer();
	Vector v = new Vector();
	while (!lastByteWasEol)
	    v.addElement(readToken());

	lastByteWasEol = false;

	KomToken[] foo = new KomToken[v.size()];

	Enumeration e = v.elements();
	for (int i=0;i<foo.length;i++)
	    foo[i] = (KomToken) e.nextElement();

	if (Debug.ENABLED) Debug.println("RCV: " + debugBuffer.toString());
	return foo;
    }

    /**
     * @deprecated TODO: remove all references to this method asap.
     */
    private void read(InputStream in, byte[] buffer)
    throws IOException {
	for (int i=0;i<buffer.length;i++) {
	    byte b = (byte) in.read();
	    //if (b == -1) throw(new IOException("End of Stream"));
	    buffer[i] = b;
	}
	if (debugBuffer != null) debugBuffer.append(new String(buffer) + " ");
    }

    protected KomToken readToken() 
    throws IOException, ProtocolException {
	return readToken(-1);
    }

    protected KomToken readToken(int hollerithLimit)
    throws IOException, ProtocolException {
	ByteArrayOutputStream os = new ByteArrayOutputStream(32);
	boolean readMore = true;
	int arrlen = -1;
	byte b = 0;
	byte lastB = 0;
	if (Debug.ENABLED && hollerithLimit > -1) {
	    Debug.println("readToken(" + hollerithLimit + ")");
	}
	boolean tokenCompleted = false;
	while (!tokenCompleted) {
	    lastB = b;
	    b = (byte) input.read();
	    if (debugBuffer != null) debugBuffer.append((char) b);
	    //Debug.println(new String((char) b));
	    if (b == '\n')
		lastByteWasEol = true;
	    else
		lastByteWasEol = false;
	    
	    switch ((int)b) {
		//case -1:
		//throw(new IOException("End of stream"));
	    case '\n':
	    case ' ':
		if (lastB == b)
		    break;

		lastToken = new KomToken(os.toByteArray());
		tokenCompleted = true;
		break;
	    case '*':
		arrlen = (lastToken != null ? lastToken.toInteger() : -1);
		lastToken = (KomToken) new KomTokenArray(arrlen);
		tokenCompleted = true;
		break;
	    case '{':
		input.read(); // eat up leading space
		lastToken = (KomToken) readArray();
		tokenCompleted = true;
		break;
	    case 'H':
		try {
		    arrlen = Integer.parseInt(session.toString(os.toByteArray()));
		} catch (NumberFormatException x) {
		    throw(new KomProtocolException("Bad hollerith \""+
						   new String(os.toByteArray()) + "\"?"));
		}
		if (hollerithLimit != -1 && arrlen > hollerithLimit) {
		    if (Debug.ENABLED)
			Debug.println("Returning HollerithStream of " + arrlen + " bytes");
		    lastToken = new HollerithStream(input, arrlen, session.getServerEncoding());
		    tokenCompleted = true;
		    break;
		}
		byte[] hstring = new byte[arrlen];
		read(input, hstring);
		if (input.read() == '\n') { // eat trailing space/cr
		    lastByteWasEol = true;
		} else {
		    lastByteWasEol = false;
		}
	        lastToken = new Hollerith(hstring, session.getServerEncoding());
		tokenCompleted = true;
		break;
	    default:
		os.write(b);
	    }
	}
	if (DEBUG > 2) {
	    Debug.println("readToken(): " + lastToken + (lastByteWasEol ? " (END OF LINE)" : ""));
	}
	return lastToken;
    }
}



