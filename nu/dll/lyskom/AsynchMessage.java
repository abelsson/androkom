/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
// -*- Mode: Java; c-basic-offset: 4 -*-
package nu.dll.lyskom;

import java.util.Date;

public class AsynchMessage extends Asynch {

    KomToken[] parameters;
    Date arrival;
    public AsynchMessage(KomToken[] parameters) {
	this.parameters = parameters;
	arrival = new Date();
    }

    // only return elements 2 and up (first is length, second is message no.)
    public KomToken[] getParameters() {
	if (parameters.length < 3) return new KomToken[] {};
	KomToken[] reqParams = new KomToken[parameters.length-2];
	for (int i=2; i < parameters.length; i++) {
	    reqParams[i-2] = parameters[i];
	}
	return reqParams;
    }
    
    public int getNumber() {
	return parameters[1].toInt();
    }

    public Date getArrivalTime() {
	return arrival;
    }

    public String toString() {
	StringBuffer blaj = new StringBuffer("AsynchMessage: <#" + number + "> ");

	if (parameters == null || parameters.length == 0) {
	    blaj.append("empty");
	    return blaj.toString();
	}

	for (int i=0;i<parameters.length;i++)
	    blaj.append("<"+parameters[i].toString()+">");

	return blaj.toString();
    }

}
