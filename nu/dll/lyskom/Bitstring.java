/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;

import java.io.Serializable;

public class Bitstring extends KomToken implements Serializable {
    public static int ITEM_SIZE = 1;
    
    public Bitstring() {
	super();
    }

    // this is, uh, like, clone'n'cast.
    public Bitstring(KomToken bits) {
	setContents(bits.getContents());
    }

    public Bitstring(String s) {
	this(new KomToken(s));
    }

    public Bitstring(boolean[] bits) {
	byte[] bs = new byte[bits.length];
	for (int i=0;i<bs.length;i++)
	    bs[i] = (byte) (bits[i] ? '1' : '0');

	setContents(bs);
    }

    public boolean getBitAt(int i) {
	return (getContents()[i] == '1') ? true : false;
    }

    public void setBitAt(int i, boolean value) {
	byte[] contents = getContents();
	contents[i] = (byte) (value ? '1' : '0');
	setContents(contents);
    }

    public boolean[] getBits() {
	byte[] cont = getContents();
	boolean[] bs = new boolean[cont.length];
	for (int i=0;i<bs.length;i++)
	    bs[i] = ((cont[i] == '1') ? true : false);

	return bs;
    }
    
    public static Bitstring createFrom(int offset, KomToken[] array) {
    	return new Bitstring(array[offset]);
    }
    
    public String toString() {
	return "BITSTRING:"+new String(getContents());
    }

}	
