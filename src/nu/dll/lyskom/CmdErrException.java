/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;

/**
 * This is a relic actually belonging to the nu.dll.app.test package.
 */
public class CmdErrException extends Exception {
	private static final long serialVersionUID = 8210711432550088007L;

	public CmdErrException(String s) {
		super(s);
	}
}
