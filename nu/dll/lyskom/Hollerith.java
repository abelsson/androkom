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
	// Ehm. (string.length+"H"+string).getBytes() is probably easier,
	// but may cause encoding problem (String.getBytes()).
	byte[] b = getContents();
	String realLen = b.length + "";
	int holLength = b.length + realLen.length() + 1;
	realLen = realLen + "H";
	byte[] argh = new byte[holLength];
	int i = 0;
	for (; i < realLen.length(); i++) {
	    argh[i] = (byte) realLen.charAt(i);
	}
	for (int j = 0; j < b.length; j++) {
	    argh[j+i] = b[j];
	}
	return argh;
    }

}
  
