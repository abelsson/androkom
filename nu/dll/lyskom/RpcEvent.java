/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;

public class RpcEvent {

    RpcCall rpcCall;
    RpcReply rpcReply;
    int op;
    int id;
    Session source;
    Object object;
    
    public RpcEvent(Session source, RpcCall rpcCall) {
	this(source, rpcCall, null);
    }

    public RpcEvent(Session source, RpcCall rpcCall, Object object) {
	this.source = source;
	this.rpcCall = rpcCall;
	op = rpcCall.getOp();
	id = rpcCall.getId();
	this.object = object;
	rpcReply = rpcCall.getReply();
    }

    public String toString() {
	return "RpcEvent: id="+id+", op="+op;
    }

    public Session getSource() {
	return source;
    }

    public Object getObject() {
	return object;
    }

    public int getOp() {
	return op;
    }

    public int getId() {
	return id;
    }

    public RpcCall getCall() {
	return rpcCall;
    }

    public RpcReply getReply() {
	return rpcReply;
    }

    public boolean getSuccess() {
	return rpcReply.getSuccess();
    }
}


