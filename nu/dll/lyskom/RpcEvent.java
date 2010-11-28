/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;

/**
 * This call represents an RPC event; i.e. that an RPC reply has been received
 * from the server.
 * 
 */
public class RpcEvent {

	RpcCall rpcCall;
	RpcReply rpcReply;
	int op;
	int id;
	Session source;
	Object object;

	RpcEvent(Session source, RpcCall rpcCall) {
		this(source, rpcCall, null);
	}

	RpcEvent(Session source, RpcCall rpcCall, Object object) {
		this.source = source;
		this.rpcCall = rpcCall;
		op = rpcCall.getOp();
		id = rpcCall.getId();
		this.object = object;
		rpcReply = rpcCall.getReply();
	}

	/**
	 * Returns a String containing information about this object.
	 */
	public String toString() {
		return "RpcEvent: id=" + id + ", op=" + op;
	}

	/**
	 * Returns the Session object that initiated this event.
	 */
	public Session getSource() {
		return source;
	}

	/**
	 * Returns an arbitrary object that has been associated with this event.
	 * 
	 * @deprecated I don't even know what this was good for in the first place
	 */
	public Object getObject() {
		return object;
	}

	/**
	 * Returns the operation (RPC request call) that initiated this event.
	 */
	public int getOp() {
		return op;
	}

	/**
	 * Returns the ID (RPC reference number) of the call that initiated this
	 * event.
	 */
	public int getId() {
		return id;
	}

	/**
	 * Returns the RpcCall object that represents the call of which this is a
	 * reply to.
	 */
	public RpcCall getCall() {
		return rpcCall;
	}

	/**
	 * Returns the RpcReply object that represents the reply received from the
	 * server.
	 */
	public RpcReply getReply() {
		return rpcReply;
	}

	/**
	 * Returns <tt>true</tt> if the server indicated that the call was
	 * successful.
	 */
	public boolean getSuccess() {
		return rpcReply.getSuccess();
	}
}
