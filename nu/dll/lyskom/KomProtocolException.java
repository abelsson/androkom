/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;

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
