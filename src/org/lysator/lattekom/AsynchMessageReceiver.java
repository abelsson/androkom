/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package org.lysator.lattekom;

/**
 * Interface to be implemented by classes who wants themselves to be called when
 * asynchronous messages arrives from the server.
 */
public interface AsynchMessageReceiver {
	/**
	 * Called when a messages has been read and parsed by LatteKOM.
	 * 
	 * @param m
	 *            The parsed asynch message.
	 */
	public void asynchMessage(AsynchMessage m);
}
