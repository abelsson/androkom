/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
// -*- Mode: Java; c-basic-offset: 4 -*-
package nu.dll.lyskom;

import java.util.Vector;
import java.util.Enumeration;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
/**
 * This class is used to do the actual RPC Calls to the server. Look
 * at the fields in the RPC class to find out what calls are
 * available, and in the Protocol A documentation to see what they do
 */
public class RpcCall implements Rpc {

    static final int DEBUG = 3;

    int id, number;
    RpcReply reply;

    Vector parameters;
    Vector aux;

    public RpcCall(int id, int number) {
	this.id = id;
	this.number = number;
	parameters = new Vector();
	aux = new Vector();
    }
    public int getId() {
	return id;
    }
    public int getOp() {
	return number;
    }
    public void setReply(RpcReply r) {
	reply = r;
	if (!r.getSuccess()) {
	    Debug.println("RPC call #" + id + " failed with error #" +
			       r.parameters[0].toInteger());
	}
    }
    public RpcReply getReply() {
	return reply;
    }

    public KomToken getParameter(int n) {
	return (KomToken) parameters.elementAt(n);
    }

    public Enumeration getParameterElements() {
	return parameters.elements();
    }

    public void addAux(Object o) {
	aux.addElement(o);
    }
    
    public Object getAux(int n) {
	if (n < 0 || n > aux.size()-1)
	    return null;

	return aux.elementAt(n);
    }

    public Enumeration getAuxElements() {
	return aux.elements();
    }
    
    public RpcCall add(Hollerith h) {
	return add((KomToken) h);
    }

    public RpcCall add(int i) {
	return add(new KomToken(i));
    }

    public RpcCall add(KomTokenArray k) {
	return add((KomToken) k);
    }

    public RpcCall add(KomToken k) {
	parameters.addElement((Object) k);
	return this;
    }
    
    public RpcCall add(String s) {
	parameters.addElement(new KomToken(s));
	return this;
    }
    public String toString() {
	return "RpcCall(id: "+id+"; op: "+number+"; parameters: " + 
	    parameters.size() + ")";
    }

    public byte[] toNetwork() {
	try {
	    ByteArrayOutputStream stream = new ByteArrayOutputStream();
	    stream.write((id+" ").getBytes());
	    stream.write((number+"").getBytes());
	    for (Enumeration e = parameters.elements(); e.hasMoreElements();) {
		stream.write(' ');
		stream.write(((KomToken) e.nextElement()).toNetwork());
	    }
	    stream.write('\n');
	    return stream.toByteArray();
	} catch (IOException ex) {
	    Debug.println("RpcCall.toNetwork(): "+ex.getMessage());
	    System.exit(-101);
	    return null;
	}
    }

    public void writeNetwork(OutputStream output)
    throws IOException {	
	output.write(toNetwork());
	if (DEBUG>2)
	    Debug.println("Wrote: "+new String(toNetwork()));
    }
    
}

