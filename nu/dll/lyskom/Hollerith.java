/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;

import java.io.Serializable;

/**
 * Class representing the LysKOM datatype Hollerith. A Hollerith represents a byte vector
 * of arbitrary length containing arbitrary byte values. It is used for all handling of
 * strings in LysKOM.
 */
public class Hollerith extends KomToken implements Serializable {

    String charset = Session.defaultServerEncoding;

    /**
     * Constructs an empty Hollerith
     */
    public Hollerith() {
	setContents(null);
    }

    public Hollerith(String s) {
	this(s, Session.defaultServerEncoding);
    }

    public Hollerith(byte[] buf, String charset) {
	setCharset(charset);
	setContents(buf);
    }

    /**
     * Construct a Hollertith by converting the supplied string into
     * bytes according to the given charset.
     */
    public Hollerith(String s, String charset) {
	try {
	    setCharset(charset);
	    setContents(s.getBytes(charset));
	} catch (java.io.UnsupportedEncodingException e) {
	    throw new RuntimeException("Unsupported encoding: " + e.getMessage());
	}
    }

    protected void setCharset(String c) {
	charset = c;
    }

    protected String getCharset() {
	return charset;
    }
    
    /**
     * Constructs a Hollerith containing the supplied bytes
     */
    public Hollerith(byte[] b) {
	setContents(b);
    }

    public String toString() {
	return "HOLLERITH(" + getContents().length +
	    "):\""+new String(getContents()) + "\"";
    }

    /**
     * Returns this Holleriths contents as a String, translated according to the server's encoding.
     */
    public String getContentString() {
	try {
	    return new String(getContents(), getCharset());
	} catch (java.io.UnsupportedEncodingException e) {
	    throw new RuntimeException("Unsupported encoding: " + e.getMessage());
	}
    }

    /**
     * Constructs a byte-array representation of this Hollerith suitable 
     * for sending to the server, over the network (into the sea...).
     * <br>
     * A Hollerith is a prefix being the string length and the characted 'H',
     * followed by the string data.
     */
    public byte[] toNetwork() {
	try {
	    String prefixString = contents.length + "H";
	    byte[] prefixBytes = prefixString.getBytes("us-ascii");
	    byte[] contents = getContents();
	    byte[] output = new byte[contents.length + prefixBytes.length];
	    
	    System.arraycopy(prefixBytes, 0, output, 0, prefixBytes.length);
	    System.arraycopy(contents, 0, output, prefixBytes.length, contents.length);
	    
	    return output;
	} catch (java.io.UnsupportedEncodingException e) {
	    throw new RuntimeException("Unsupported encoding: " + e.getMessage());
	}
    }
}
