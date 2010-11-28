/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;

import java.io.Serializable;

/**
 * This class represents the LysKOM data type BITSTRING, which is a serie of
 * ones and zeroes making up a vector of boolean "flags". It can thus be seen as
 * an array of <tt>boolean</tt> values.
 */
public class Bitstring extends KomToken implements Serializable {
	private static final long serialVersionUID = 1L;
	public static int ITEM_SIZE = 1;

	/**
	 * Creates an empty Bitstring
	 */
	public Bitstring() {
		super();
	}

	/**
	 * Creates a Bitstring out of the contents in the supplied KomToken
	 * 
	 * @param bits
	 *            the KomToken object containing BITSTRING data
	 */
	public Bitstring(KomToken bits) {
		setContents(bits.getContents());
	}

	/**
	 * Creates a Bitstring out of the contents in the supplied String
	 * 
	 * @param s
	 *            the String object containing BITSTRING data
	 */
	public Bitstring(String s) {
		this(new KomToken(s));
	}

	/**
	 * Creates a Bitstring out of an <tt>boolean[]</tt>.
	 * 
	 * @param bits
	 *            an array of <tt>boolean</tt>
	 */
	public Bitstring(boolean[] bits) {
		byte[] bs = new byte[bits.length];
		for (int i = 0; i < bs.length; i++)
			bs[i] = (byte) (bits[i] ? '1' : '0');

		setContents(bs);
	}

	/**
	 * Returns the status at a specific position in this Bitstring.
	 * 
	 * @param i
	 *            the position to look at
	 */
	public boolean getBitAt(int i) {
		return (getContents()[i] == '1') ? true : false;
	}

	/**
	 * Sets the value of a bit at a specific position in this Bitstring.
	 * 
	 * @param i
	 *            the position to change
	 * @param value
	 *            the new value
	 */
	public void setBitAt(int i, boolean value) {
		byte[] contents = getContents();
		contents[i] = (byte) (value ? '1' : '0');
		setContents(contents);
	}

	/**
	 * Returns a <tt>boolean[]</tt> equal the bits' representation in this
	 * Bitstring.
	 */
	public boolean[] getBits() {
		byte[] cont = getContents();
		boolean[] bs = new boolean[cont.length];
		for (int i = 0; i < bs.length; i++)
			bs[i] = ((cont[i] == '1') ? true : false);

		return bs;
	}

	static Bitstring createFrom(int offset, KomToken[] array) {
		return new Bitstring(array[offset]);
	}

	public String toString() {
		return "BITSTRING:" + new String(getContents());
	}

}
