/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;

public class Bitstring extends KomToken {

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

    public boolean[] getBits() {
	byte[] cont = getContents();
	boolean[] bs = new boolean[cont.length];
	for (int i=0;i<bs.length;i++)
	    bs[i] = ((cont[i] == '1') ? true : false);

	return bs;
    }
    public String toString() {
	return "BITSTRING:"+new String(getContents());
    }

}	
