/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;

/**
 * Represents a failed RPC call.
 * <br>
 * <b>Note:</b> although this call is a <tt>RuntimeException</tt> now, this might
 * change in the future to enforce more strict error handling of RPC calls.
 */
public class RpcFailure extends RuntimeException {
    RpcReply reply;
    int errorCode = 0;
    int errorStatus = 0;

    RpcFailure(RpcReply r, String s) {
	super(s);
	reply = r;
	if (!r.getSuccess()) {
	    errorCode = r.getParameters()[0].toInteger();
	    errorStatus = r.getParameters()[1].toInteger();
	}
    }

    /**
     * Returns the <tt>RpcReply</tt> object representing the actual RPC reply.
     */
    public RpcReply getReply() {
	return reply;
    }

    /**
     * Returns the error code returned by the server.
     */
    public int getError() {
	return errorCode;
    }

    /**
     * Returns the error status returned by the server.
     */
    public int getErrorStatus() {
	return errorStatus;
    }

    /**
     * Returns a string containing the error code and status.
     */ 
    public String getMessage() {
	return "RPC error " + errorCode + ", status " + errorStatus;
    }
    

}
