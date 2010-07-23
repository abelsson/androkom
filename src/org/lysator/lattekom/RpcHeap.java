/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package org.lysator.lattekom;

import java.util.*;
import java.util.Map.Entry;

/**
 * Stores all pending RPC calls
 */
class RpcHeap {
    static int DEBUG = 1;

    /**
     * All RPC calls are stored until they are explicitly purged (as done by
     * Session.waitFor()) or their age is the RPC timeout value times 60.
     */
    long maxAge = Session.rpcTimeout * 60;
    Map<Integer, RpcCall> rpcCalls;
    Map<Integer, Long> ages;

    public RpcHeap() {
        rpcCalls = new HashMap<Integer, RpcCall>();
        ages = new HashMap<Integer, Long>();
    }

    protected void gc() {
        synchronized (rpcCalls) {
            for (Iterator<Entry<Integer, Long>> i = ages.entrySet().iterator(); i
                    .hasNext();) {
                Map.Entry<Integer, Long> entry = i.next();
                if (((Long) entry.getValue()).longValue() > maxAge) {
                    i.remove();
                    rpcCalls.remove(entry.getKey());
                }
            }
        }
    }

    public void purgeRpcCall(RpcCall r) {
        purgeRpcCall(r.getId());
    }

    public void purgeRpcCall(int wid) {
        if (rpcCalls == null)
            return;

        synchronized (rpcCalls) {
            rpcCalls.remove(new Integer(wid));
            ages.remove(new Integer(wid));
        }
    }

    public void addRpcCall(RpcCall r) {
        synchronized (rpcCalls) {
            rpcCalls.put(new Integer(r.getId()), r);
            ages.put(new Integer(r.getId()),
                    new Long(System.currentTimeMillis()));
        }
    }

    public RpcCall getRpcCall(int wid, boolean reqReply) {
        synchronized (rpcCalls) {
            RpcCall r = (RpcCall) rpcCalls.get(new Integer(wid));
            if (r != null && reqReply == (r.getReply() != null))
                return r;
        }
        return null;
    }

    public RpcCall getRpcCall(Collection<Integer> ids, boolean reqReply) {
        // XXX: will deadlock occur if someone locks on rpcCalls
        // and ids (in that order) and then calls this method?
        if (rpcCalls == null)
            return null;

        synchronized (ids) {
            Iterator<Integer> idIterator = ids.iterator();
            while (idIterator.hasNext()) {
                Object idObj = idIterator.next();
                int id;
                if (idObj instanceof RpcCall)
                    id = ((RpcCall) idObj).getId();
                else
                    id = ((Integer) idObj).intValue();

                RpcCall r = getRpcCall(id, reqReply);
                if (r != null)
                    return r;
            }
        }
        return null;
    }
}
