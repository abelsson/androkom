/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package org.lysator.lattekom;

/**
 * Thrown by the Selection class when an application tries to access a
 * non-existant key.
 */
public class NoSuchKeyException extends RuntimeException {
	public NoSuchKeyException() {
		super();
	}
	public NoSuchKeyException(String s) {
		super(s);
	}
}
