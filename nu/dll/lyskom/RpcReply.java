/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;

/**
 * Represents an RPC reply received from the server.
 */
public class RpcReply {
    boolean good;
    int id;
    KomToken[] parameters;

    RpcFailure exception;

    final static int DEBUG = 255;

    RpcReply(boolean good, int id, KomToken[] parameters) {
	this.good = good;
	this.id = id;
	this.parameters = parameters;
	if (!good) exception = new RpcFailure(this, null);
    }

    /**
     * Returns <tt>true</tt> if the server indicated that the initiating call was successful.
     */    
    public boolean getSuccess() {
	return good;
    }

    /**
     * If the call was unsuccessful, returns an <tt>RpcFailure</tt> object containing the error status,
     * otherwise <tt>null</tt>.
     * @see nu.dll.lyskom.RpcFailure
     */ 
    public RpcFailure getException() {
	return exception;
    }

    /**
     * Returns the RPC reference number of the call which this is a reply to.
     */
    public int getId() {
	return id;
    }

    /**
     * Return an array of KomToken objects containing all reply data.
     */
    public KomToken[] getParameters() {
	return parameters;
    }

    /**
     * Returns a string containing information about this object.
     */
    public String toString() {
	return "RpcReply(id: "+id+"; OK: "+good+"; parameters: "+parameters.length+")";
    }

}
