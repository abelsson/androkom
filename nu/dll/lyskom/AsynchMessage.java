/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
// -*- Mode: Java; c-basic-offset: 4 -*-
package nu.dll.lyskom;

public class AsynchMessage extends Asynch {

    KomToken[] parameters;
    
    public AsynchMessage(KomToken[] parameters) {
	this.parameters = parameters;
    }

    public KomToken[] getParameters() {
	return parameters;
    }

    public String toString() {
	if (parameters == null) return "empty";
	StringBuffer blaj = new StringBuffer("AsynchMessage: ");
	for (int i=0;i<parameters.length;i++)
	    blaj.append("<"+parameters[i].toString()+">");
	return blaj.toString();
    }

}
