/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
// -*- Mode: Java; c-basic-offset: 4 -*-
package nu.dll.lyskom;

import java.net.ProtocolException;
import java.util.Vector;
import java.util.Enumeration;
import java.io.*;

/** NB! Rename this class! **/
public class MessageListener
implements Runnable {

    private final static int DEBUG = 0;

    Vector rpcReceivers = new Vector(1);
    Vector asynchReceivers = new Vector(1);

    Session session;

    boolean asynch = false;

    public MessageListener(Session session) {
	this.session = session;
    }

    public void setAsynch(boolean wantAsynch) {
	if (wantAsynch) {
	    if (!asynch) {
		asynch = true;
		new Thread(this, "LyskomAsynchReadThread").start();
	    }
	} else {
	    // TODO: change from synchronous to asynch mode and vice versa?

	    // this 
	    asynch = false;
	}
    }

    // cannot throw exceptions from here, use callback error handling?
   public void run() {

	while(asynch) {
	    KomToken[] row;
	    try {
		row = session.getKomTokenReader().readLine();
	    } catch (ProtocolException ex) {
		throw(new KomProtocolException(ex.getMessage()));
	    } catch (IOException ex) {
		throw new KomProtocolException(ex.getMessage());
	    }
	    if (row.length == 0) {
		Debug.println("Got: Empty row, skipping");
		continue;
	    }
	    String descriptor = new String(row[0].toNetwork());
	    if (descriptor.startsWith("=") ||
		descriptor.startsWith("%")) { // RPC reply
		if (descriptor.startsWith("%%")) {
		    StringBuffer dbgs = new StringBuffer();
		    for (int i=0;i<row.length;i++)
			dbgs.append(" " +
					 new String(row[i].toNetwork()));
		    dbgs.append(" ("+row.length+" elements)");
		    throw(new KomProtocolException(dbgs.toString()));
		}
			    
		boolean good = (descriptor.startsWith("=") ? true : false);
		/*if (descriptor.startsWith("=")) good = true;
		  else good = false;*/
		int id;
		try {
		    id = Integer.parseInt(descriptor.substring(1));
		} catch (NumberFormatException ex) {
		    throw new KomProtocolException("Bad RPC ID: " +
						   descriptor);
		}
		KomToken[] params = new KomToken[row.length-1];
		for (int i=0;i<params.length;i++)
		    params[i] = row[i+1];

		// notify listeners...
		for(Enumeration e = rpcReceivers.elements();
		    e.hasMoreElements();)
		    ((RpcReplyReceiver)
		     e.nextElement()).rpcReply(new RpcReply(good, id, params));
	    } else if (descriptor.startsWith(":")) { // Asynch message
		for(Enumeration e = asynchReceivers.elements();
		    e.hasMoreElements();) {
		    AsynchMessageReceiver rcvr = (AsynchMessageReceiver) e.nextElement();
		    Debug.println("dispatching asynch message to " + rcvr);
		    rcvr.asynchMessage(new AsynchMessage(row));
		}
	    }
	}
	System.err.println("MessageListener.run() finishing");
   }

    private KomToken[] stripFirstToken(KomToken[] tokens) {
	KomToken[] rtoks = new KomToken[tokens.length-1];
	for (int i=0;i<rtoks.length;i++)
	    rtoks[i] = tokens[i+1];
	return rtoks;
    }
    


    /** add/remove event receivers **/
    public boolean removeRpcReplyReceiver(RpcReplyReceiver r) {
	return rpcReceivers.removeElement((Object) r);
    }
	    
    public void addRpcReplyReceiver(RpcReplyReceiver r) {
	rpcReceivers.addElement((Object) r);
    }

    public void addAsynchMessageReceiver(AsynchMessageReceiver a) {
	asynchReceivers.addElement((Object) a);
    }

    public boolean removeAsynchMessageReceiver(AsynchMessageReceiver a) {
	return asynchReceivers.removeElement((Object) a);
    }

}
