/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;

public class RpcFailure extends RuntimeException {
    RpcReply reply;
    int errorCode = 0;
    int errorStatus = 1;
    public RpcFailure(RpcReply r, String s) {
	super(s);
	reply = r;
	if (!r.getSuccess()) {
	    errorCode = r.getParameters()[0].toInteger();
	    errorStatus = r.getParameters()[0].toInteger();
	}
    }

    public RpcReply getReply() {
	return reply;
    }

    public int getError() {
	return errorCode;
    }
    public int getErrorStatus() {
	return errorStatus;
    }
    

}
