/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;

public class RpcReply {
    boolean good;
    int id;
    KomToken[] parameters;

    RpcFailure exception;

    final static int DEBUG = 255;

    public RpcReply(boolean good, int id, KomToken[] parameters) {
	this.good = good;
	this.id = id;
	this.parameters = parameters;
	if (!good) exception = new RpcFailure(this, null);
    }

    public boolean getSuccess() {
	return good;
    }

    public RpcFailure getException() {
	return exception;
    }

    public int getId() {
	return id;
    }

    public KomToken[] getParameters() {
	return parameters;
    }

    public String toString() {
	return "RpcReply(id: "+id+"; OK: "+good+"; parameters: "+parameters.length+")";
    }

}
