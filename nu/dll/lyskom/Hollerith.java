/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;

public class Hollerith extends KomToken implements java.io.Serializable {

    public Hollerith() {
	setContents(null);
    }

    public Hollerith(String s) {
	try {
	    setContents(s.getBytes("ISO-8859-1"));
	} catch (java.io.UnsupportedEncodingException e) {
	    throw new RuntimeException("Unsupported encoding: " + e.getMessage());
	}
    }
    
    public Hollerith(byte[] b) {
	setContents(b);
    }

    public String toString() {
	return "HOLLERITH(" + getContents().length +
	    "):\""+new String(getContents()) + "\"";
    }

    public String getContentString() {
	try {
	    return new String(getContents(), "ISO-8859-1");
	} catch (java.io.UnsupportedEncodingException e) {
	    throw new RuntimeException("Unsupported encoding: " + e.getMessage());
	}
    }

    public byte[] toNetwork() {
	try {
	    String prefixString = contents.length + "H";
	    byte[] prefixBytes = prefixString.getBytes("ISO-8859-1");
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
