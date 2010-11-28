/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;

/**
 * Interface used by classes that wants to be able to receive RPC events
 * asynchronously. Applications should register their listeners with the
 * addRpcEventListener() method in the Session class.
 * 
 * @see nu.dll.lyskom.Session#addRpcEventListener(RpcEventListener)
 */
public interface RpcEventListener {
	/**
	 * Called by LatteKOM when an RPC event (an RPC reply) has been received and
	 * parsed.
	 * 
	 * @see nu.dll.lyskom.RpcEvent
	 * @see nu.dll.lyskom.RpcReply
	 * @see nu.dll.lyskom.RpcCall
	 */
	public void rpcEvent(RpcEvent e);
}
