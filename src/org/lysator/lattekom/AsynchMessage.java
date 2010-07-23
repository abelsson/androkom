/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
// -*- Mode: Java; c-basic-offset: 4 -*-
package org.lysator.lattekom;

import java.util.Date;

/**
 * This class represents an asynchronous message received from the server. See
 * the LysKOM Protocol A specification and the <tt>Asynch</tt> interface for
 * messages and their contents that may be received.
 * 
 * @see nu.dll.lyskom.Asynch
 */
public class AsynchMessage {

	KomToken[] parameters;
	Date arrival;
	AsynchMessage(KomToken[] parameters) {
		this.parameters = parameters;
		arrival = new Date();
	}

	/**
	 * Returns all the data contained in this asynchronous message, except the
	 * message number.
	 * 
	 * @see nu.dll.lyskom.AsynchMessage#getNumber()
	 */
	public KomToken[] getParameters() {
		if (parameters.length < 3)
			return new KomToken[]{};
		KomToken[] reqParams = new KomToken[parameters.length - 2];
		for (int i = 2; i < parameters.length; i++) {
			reqParams[i - 2] = parameters[i];
		}
		return reqParams;
	}

	/**
	 * Returns the message number. See the Asynch interface for a list of
	 * possible numbers.
	 * 
	 * @see nu.dll.lyskom.Asynch
	 */
	public int getNumber() {
		return parameters[1].intValue();
	}

	/**
	 * Returns a java.util.Date object representing the time of when this
	 * message was received by the client.
	 */
	public Date getArrivalTime() {
		return arrival;
	}

	/**
	 * Returns a String representation.
	 */
	public String toString() {
		StringBuffer blaj = new StringBuffer("AsynchMessage: <#" + getNumber()
				+ "> ");

		if (parameters == null || parameters.length == 0) {
			blaj.append("empty");
			return blaj.toString();
		}

		for (int i = 0; i < parameters.length; i++)
			blaj.append("<" + parameters[i].toString() + ">");

		return blaj.toString();
	}

}
