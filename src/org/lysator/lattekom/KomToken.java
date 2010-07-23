/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
// -*- Mode: Java; c-basic-offset: 4 -*-

package org.lysator.lattekom;

import java.io.Serializable;
import java.util.Arrays;
import java.io.UnsupportedEncodingException;

/**
 * A KomToken represents an object to be sent to or has been received from the
 * server. It is generally a serie of bytes.
 */
public class KomToken implements Serializable {
	/**
	 * A "primitive" LysKOM token, such as INT32
	 */
	public final static int PRIMITIVE = 0;
	/**
	 * A "complex" LysKOM token.
	 */
	public final static int COMPL = 1;
	/**
	 * An array of KomToken objects
	 */
	public final static int ARRAY = 2;
	/**
	 * A Hollerith
	 */
	public final static int HOLLERITH = 3;

	int type = PRIMITIVE; // obsolete?
	byte[] contents = null;

	private static final int DEBUG = 0;

	boolean eol = false; // indicates last token on line

	/**
	 * Creates an empty KomToken object.
	 */
	public KomToken() {
		type = COMPL;
	}

	/**
	 * Returns <tt>true</tt> if the supplied object is of type KomToken, and
	 * it's contents are equal to this, <i>or</i> if it is a String which,
	 * converted to the default charset, equals this object's contents,
	 * <i>or</i> if it is an Integer, which, when converted to a Protocol A
	 * representation of its value, is equal to this object's contents.
	 */
	// FIXME: untested
	public boolean equals(Object o) {
		try {
			String charset = Session.defaultServerEncoding;
			if (this instanceof Hollerith) {
				charset = ((Hollerith) this).getCharset();
			}
			if (o instanceof String || o instanceof KomToken) {
				byte[] theirdata;
				if (o instanceof String)
					theirdata = ((String) o).getBytes(charset);
				else
					theirdata = ((KomToken) o).getContents();

				return Arrays.equals(contents, theirdata);
			}
			if (o instanceof Integer) {
				return equals(new Hollerith(o.toString()));
			}
			return super.equals(o);
		} catch (UnsupportedEncodingException ex1) {
			throw new RuntimeException("Unsupported encoding", ex1);
		}
	}

	/**
	 * Returns <tt>true</tt> if this KomToken is the last token in a command
	 * received from the server.
	 */
	public boolean isEol() {
		return eol;
	}

	protected void setEol(boolean b) {
		eol = b;
	}

	/**
	 * Constructs a simple KomToken object representing the supplied integer
	 * value.
	 */
	public KomToken(int i) {
		contents = (i + "").getBytes();
	}

	public KomToken(boolean b) {
		contents = new byte[]{b ? (byte) '1' : (byte) '0'};
	}

	/**
	 * Constructs a simple KomToken object containing the supplied bytes.
	 */
	public KomToken(byte[] b) {
		contents = b;
	}

	/**
	 * Converts the supplied string into bytes according to the default
	 * encoding, using the result as the contents for this KomToken
	 */
	public KomToken(String s) {
		try {
			contents = s.getBytes(Session.defaultServerEncoding);
		} catch (UnsupportedEncodingException ex1) {
			throw new RuntimeException("Unsupported encoding: "
					+ ex1.getMessage());
		}
	}

	/**
	 * Attempts to parse the contents of this KomToken into an integer value
	 * using a radix of 10.
	 */
	public int intValue() {
		if (contents == null || contents.length == 0)
			throw new RuntimeException(
					"intValue() invoked on token with zero length data");
		try {
			return Integer.parseInt(new String(contents));
		} catch (NumberFormatException ex) {
			// Is this a programming error or runtime error? Hmmm?
			throw new RuntimeException("Error parsing " + new String(contents)
					+ " to int");
		}
	}

	public String toString() {
		String ktype = "TOKEN";
		if (this instanceof Hollerith)
			return ((Hollerith) this).toString();
		else if (type == COMPL)
			ktype = "COMPL";
		return ktype + ":\"" + new String(getContents()) + "\""
				+ (isEol() ? "(EOL)" : "");
	}

	/**
	 * Constructs a byte-array suitable for sending to the LysKOM server.
	 */
	byte[] toNetwork() {

		// uuh.. but this can't happen...?
		if (this instanceof Hollerith)
			return ((Hollerith) this).toNetwork();

		if (this instanceof KomTokenArray)
			return ((KomTokenArray) this).toNetwork();

		return getContents();
	}

	/**
	 * Returns the contents of this KomToken
	 */
	public byte[] getContents() {
		return contents;
	}

	/**
	 * Sets the contents of this KomToken
	 */
	public void setContents(byte[] c) {
		contents = c;
	}

	protected boolean isEmpty() {
		return contents == null || contents.length == 0;
	}

	/**
	 * Returns the type of this KomToken
	 */
	public int getType() {
		return type;
	}

	public int getDebugLevel() {
		return DEBUG;
	}
}
