/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;
import java.util.*;

/**
 * (not really a _Heap_, but a just bunch of RpcCall objects)
 */
class RpcHeap {
    static int DEBUG = 1;
    public static List rpcCallStack;

    public RpcHeap() {
	rpcCallStack = new LinkedList();
    }
		      
    public void purgeRpcCall(RpcCall r) {
	if (rpcCallStack == null || rpcCallStack.size() == 0)
	    return;
	rpcCallStack.remove(r);
    }

    public void purgeRpcCall(int wid) {
	if (rpcCallStack == null || rpcCallStack.size() == 0)
	    return;

	for (Iterator i = rpcCallStack.iterator(); i.hasNext();) {
	    RpcCall r = (RpcCall) i.next();
	    if (r.getId() == wid) rpcCallStack.remove(r);
	}
    }

    public void addRpcCall(RpcCall r) {
	rpcCallStack.add(r);
    }

    public RpcCall getRpcCall(int wid, boolean reqReply) {
	if (rpcCallStack == null || rpcCallStack.size() == 0)
	    return null;
	for (Iterator i = rpcCallStack.iterator(); i.hasNext();) {
	    Object o = i.next();
	    if (DEBUG>0) {
		if (!(o instanceof RpcCall)) {
		    throw new RuntimeException("Non-RpcCall object in rpcCallStack: "+o.toString());
		}
	    }
	    RpcCall r = (RpcCall) o;
	    if (r.getId() == wid && reqReply == (r.getReply() != null)) {
		return r;
	    }

	}
	return null;
    }
}
