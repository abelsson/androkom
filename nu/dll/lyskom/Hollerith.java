/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
// -*- Mode: Java; c-basic-offset: 4 -*-
package nu.dll.lyskom;

public class Hollerith extends KomToken {

    public Hollerith() {
	setContents(null);
    }

    public Hollerith(String s) {
	setContents(s.getBytes());
    }
    
    public Hollerith(byte[] b) {
	setContents(b);
    }

    public String toString() {
	return "HOLLERITH(" + getContents().length +
	    "):\""+new String(getContents()) + "\"";
    }

    public String getContentString() {
	return new String(getContents());
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
  
