/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
// -*- Mode: Java; c-basic-offset: 4 -*-
package nu.dll.lyskom;

import java.io.*;
import java.util.Enumeration;
import java.util.List;

public class KomTokenArray extends KomToken {

    static int DEBUG = 10;
    KomToken[] objects;
    int length;

    /** constructors **/

    public KomTokenArray(int length) { // empty array
	this.length = length;
	objects = new KomToken[0];
    }
    public KomTokenArray(int length, String[] s) {
	this.length = length;
	objects = new KomToken[s.length];
	for (int i=0;i<s.length;i++)
	    objects[i] = new KomToken(s[i]);
    }
    public KomTokenArray(int length, int[] n) {
	this.length = length;
	objects = new KomToken[n.length];
	for (int i=0;i<n.length;i++)
	    objects[i] = new KomToken(n[i]);
    }

    public KomTokenArray(int length, KomToken[] o) {
	this.length = length;
	objects = o;
	contents = toNetwork();
    }

    public KomTokenArray(int length, Tokenizable[] o) {
	this.length = length;
	objects = new KomToken[o.length];
	for (int i=0; i < o.length; i++) {
	    objects[i] = o[i].toToken();
	}
	contents = toNetwork();
    }

    public KomTokenArray(int length, List tokenList) {
	this.length = length;
	objects = new KomToken[tokenList.size()];
	for (int i=0; i < objects.length; i++) {
	    objects[i] = ((Tokenizable) tokenList.get(i)).toToken();
	}
	contents = toNetwork();
    }

    /** end of constructors **/

    /** Enumeration implementation **/

    // 990915: removed

    /** end of Enumeration **/

    public int[] intValues() {
	int[] vals = new int[objects.length];
	for (int i=0; i<objects.length; i++) 
	    vals[i] = objects[i].toInteger();
	return vals;
    }

    // todo: rename
    // returns: KomToken-array-of-arrays
    public static KomToken[][] split(KomTokenArray komarray) {		
	int length = komarray.getLength();
	KomToken[] objects = komarray.getTokens();
	if (DEBUG > 0) {
	    Debug.println("split(): length: "+length+" actual: "+
			       objects.length);
	}
	if (length == objects.length)
	    return null;
	int sublength = objects.length / length;
	if (DEBUG>0) Debug.println("split(): sublength: "+sublength);
	KomToken[][] splitted = new KomToken[length][sublength];
	int ocount = 0;
	for (int i=0;i<length;i++) {
	    if (DEBUG>0) Debug.print("split(): assigning to #"+i+
					    ", ocount "+ocount);
	    KomToken[] subarray = new KomToken[sublength];
	    for (int j=0;j<sublength;j++)
		subarray[j] = objects[ocount++];

	    if (DEBUG>0) Debug.print("-"+ (ocount-1) + "\n");
	    splitted[i] = subarray;
	}
	return splitted;
    }

    public int getLength() {
	return length;
    }

    public KomToken[] getTokens() {
	return objects;
    }

    public String toString() {
	return "ARRAY("+objects.length+"):"+new String(toNetwork());
    }

    public byte[] toNetwork() {
	// FIXME: Should not use strings at all here, might destroy
	// data when String.getBytes() applies system encoding.
	StringBuffer buff = new StringBuffer();
	buff.append(length + " {");
	for (int i=0 ; i < objects.length ; i++) {
	    buff.append(" ");
	    if (objects[i] instanceof KomTokenArray) {
		KomTokenArray foo = (KomTokenArray) objects[i];
		buff.append(new String(((KomTokenArray) objects[i]).toNetwork()));
	    } else if (objects[i] instanceof Hollerith) {
		buff.append(new String(((Hollerith) objects[i]).toNetwork()));
	    } else {
		buff.append(new String(objects[i].toNetwork()));
	    }
	}
	buff.append(" }");
	return buff.toString().getBytes();
    }
		

}
