/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;
import java.util.*;

public class RpcHeap {
    static int DEBUG = 1;
    public static Vector rpcCallStack;

    public RpcHeap() {
	rpcCallStack = new Vector();
    }
		      
    public void purgeRpcCall(RpcCall r) {
	if (rpcCallStack == null || rpcCallStack.size() == 0)
	    return;
	rpcCallStack.removeElement((Object) r);
    }

    public void purgeRpcCall(int wid) {
	if (rpcCallStack == null || rpcCallStack.size() == 0)
	    return;

	for (Enumeration e = rpcCallStack.elements(); e.hasMoreElements();) {
	    RpcCall r = (RpcCall) e.nextElement();
	    if (r.getId() == wid) rpcCallStack.removeElement((Object) r);
	}
    }

    public void addRpcCall(RpcCall r) {
	rpcCallStack.addElement((Object) r);
    }

    public RpcCall getRpcCall(int wid, boolean reqReply) {
	if (rpcCallStack == null || rpcCallStack.size() == 0)
	    return null;

	for (Enumeration e = rpcCallStack.elements(); e.hasMoreElements();) {
	    Object o = e.nextElement();
	    if (DEBUG>0) {
		if (!(o instanceof RpcCall)) {
		    throw new RuntimeException("Non-RpcCall object in rpcCallStack: "+o.toString());
		}
	    }
	    RpcCall r = (RpcCall) o;
	    if (r.getId() == wid && reqReply == (r.getReply() != null))
		return r;

	}
	return null;
    }
}
