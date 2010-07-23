/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package org.lysator.lattekom;

/**
 * Exception that will be thrown by LatteKOM methods that do server I/O and
 * encounters data that it can't understand, or it seems that the server can't
 * understand what we're telling it. Might contain a nested exception.
 * 
 * @see nu.dll.lyskom.KomProtocolException#getException()
 */
public class KomProtocolException extends RuntimeException {

	Exception nestedException = null;

	public KomProtocolException(String s, Exception e) {
		super(s);
		nestedException = e;
	}

	public KomProtocolException(Exception e) {
		super(e.getMessage());
		nestedException = e;
	}

	public KomProtocolException(String s) {
		super(s);
	}

	public Exception getException() {
		return nestedException;
	}
}
