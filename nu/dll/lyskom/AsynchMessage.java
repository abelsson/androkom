/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
// -*- Mode: Java; c-basic-offset: 4 -*-
package nu.dll.lyskom;

public class AsynchMessage extends Asynch {

    KomToken[] parameters;
    int number = -1;
    
    public AsynchMessage(KomToken[] p) {
	if (p.length > 1) {

	    /** this entire block is a SAN check only **/
	    byte[] stoken = p[0].getContents();
	    if (stoken.length == 0)
		throw new KomProtocolException("empty asynch size token " + p[0]);
	    int suppSize = -1;
	    try {
		byte[] ntoken = new byte[stoken.length-1];
		System.arraycopy(stoken, 1, ntoken, 0, ntoken.length);
		suppSize = Integer.parseInt(new String(ntoken));
		if (suppSize != p.length-2)
		    throw new KomProtocolException("supplied parameter count (" + suppSize +
						   ") differs from counted (" + (p.length-2) + ")");

		number = Integer.parseInt(new String(p[1].getContents()));

	    } catch (NumberFormatException ex) {
		throw new KomProtocolException(ex);
	    }


	    parameters = new KomToken[p.length-2];
	    System.arraycopy(p, 2, parameters, 0, parameters.length);
	} else {
	    /** this is actually an error condition */
	    Debug.println("AsynchMessage: length is zero or one");
	    parameters = new KomToken[0];
	}
    }

    public int getNumber() {
	return number;
    }

    public KomToken[] getParameters() {
	return parameters;
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
